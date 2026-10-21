package com.familymemory;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.familymemory.dto.CapsuleRequest;
import com.familymemory.dto.MemoryRequest;
import com.familymemory.entity.FamilyMember;
import com.familymemory.mapper.*;
import com.familymemory.service.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 在独立H2内存库验证真实事务回滚，不连接用户数据库。 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UniverseTransactionTest.Config.class)
class UniverseTransactionTest {
    /** 仅注册内存测试所需的事务、Mapper和业务服务。 */
    @Configuration @EnableTransactionManagement
    @MapperScan("com.familymemory.mapper")
    static class Config {
        /**
         * 创建独立H2内存数据源，MySQL兼容模式。
         * @return 测试对象或测试依赖
         */
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:universe;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", ""); }
        /**
         * 创建测试事务管理器。
         * @param source 独立H2数据源
         * @return 测试对象或测试依赖
         */
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        /**
         * 创建开启下划线转驼峰的MyBatis测试会话工厂。
         * @param source 独立H2数据源
         * @return 测试对象或测试依赖
         * @throws Exception 测试会话初始化失败
         */
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean(); factory.setDataSource(source);
            MybatisConfiguration configuration = new MybatisConfiguration(); configuration.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(configuration); return factory.getObject();
        }
        /**
         * 创建仅允许测试家庭与用户的权限替身。
         * @return 测试对象或测试依赖
         */
        @Bean FamilyService familyService() { FamilyService service = mock(FamilyService.class); when(service.requireMember(1L, 2L)).thenReturn(new FamilyMember()); return service; }
        /**
         * 创建参与真实测试事务的索引同步服务。
         * @param mapper 测试索引Mapper
         * @return 测试对象或测试依赖
         */
        @Bean UniverseIndexService universeIndexService(UniverseIndexMapper mapper) { return new UniverseIndexService(mapper); }
        /**
         * 创建使用测试库Mapper的记忆业务服务。
         * @param m 测试记忆Mapper
         * @param media 测试媒体Mapper
         * @param tags 测试标签Mapper
         * @param likes 测试点赞Mapper
         * @param comments 测试评论Mapper
         * @param users 测试用户Mapper
         * @param family 家庭权限替身
         * @param index 测试索引同步服务
         * @return 测试对象或测试依赖
         */
        @Bean MemoryService memoryService(MemoryMapper m, MemoryMediaMapper media, MemoryTagMapper tags, MemoryLikeMapper likes, MemoryCommentMapper comments, UserMapper users, FamilyService family, UniverseIndexService index) { return new MemoryService(m, media, tags, likes, comments, users, family, index); }
        /**
         * 创建使用测试库Mapper的珍藏业务服务。
         * @param c 测试珍藏Mapper
         * @param users 测试用户Mapper
         * @param family 家庭权限替身
         * @param index 测试索引同步服务
         * @return 测试对象或测试依赖
         */
        @Bean CapsuleService capsuleService(TimeCapsuleMapper c, UserMapper users, FamilyService family, UniverseIndexService index) { return new CapsuleService(c, users, family, index); }
        /**
         * 创建测试星海查询服务。
         * @param mapper 测试索引Mapper
         * @param family 家庭权限替身
         * @return 测试对象或测试依赖
         */
        @Bean UniverseService universeService(UniverseIndexMapper mapper, FamilyService family) { return new UniverseService(mapper, family); }
    }
    /** 独立H2测试数据源。 */
    @Autowired DataSource source;
    /** 被测记忆服务。 */
    @Autowired MemoryService memories;
    /** 被测珍藏服务。 */
    @Autowired CapsuleService capsules;
    /** 被测星海查询服务。 */
    @Autowired UniverseService universe;
    /** 只访问独立H2内存库的JDBC工具。 */
    private JdbcTemplate jdbc;

    /**
     * 仅重建独立H2内存测试库，不操作业务数据库。
     */

    @BeforeEach void resetMemoryDatabase() { new ResourceDatabasePopulator(new ClassPathResource("universe-test-schema.sql")).execute(source); jdbc = new JdbcTemplate(source); }

    /**
     * 校验两类内容的发布、改标题和删除都同步索引，且来源ID不冲突。
     */

    @Test void publishTitleAndDeleteMaintainBothSourceTypesWithoutIdCollision() {
        Map<String, Object> memory = memories.create(1L, 2L, memoryRequest());
        CapsuleRequest request = new CapsuleRequest(); request.setTitle("未来的信"); request.setContent("尚不可见"); request.setOpenDate(LocalDate.now().plusYears(3));
        Map<String, Object> capsule = capsules.create(1L, 2L, request);
        Long memoryId = ((Number) memory.get("id")).longValue(), capsuleId = ((Number) capsule.get("id")).longValue();
        assertEquals(memoryId, capsuleId); assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM fm_universe_index WHERE deleted=0", Integer.class));
        assertEquals(LocalDate.of(2020, 2, 29), jdbc.queryForObject("SELECT node_date FROM fm_universe_index WHERE source_type='MEMORY'", LocalDate.class));
        assertEquals(jdbc.queryForObject("SELECT CAST(created_at AS DATE) FROM fm_time_capsule", LocalDate.class), jdbc.queryForObject("SELECT node_date FROM fm_universe_index WHERE source_type='CAPSULE'", LocalDate.class));
        memories.updateTitle(memoryId, 2L, "改名后的记忆"); capsules.updateTitle(capsuleId, 2L, "改名后的珍藏");
        assertEquals("改名后的记忆", jdbc.queryForObject("SELECT title FROM fm_universe_index WHERE source_type='MEMORY'", String.class));
        assertEquals(1, universe.search(1L, 2L, "珍藏", "CAPSULE", 1, 24).getTotal());
        assertEquals(2, universe.overview(1L, 2L).getMemoryCount() + universe.overview(1L, 2L).getCapsuleCount());
        assertFalse(capsule.containsKey("content"));
        memories.delete(memoryId, 2L); capsules.delete(capsuleId, 2L);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM fm_universe_index WHERE deleted=0", Integer.class));
    }
    /**
     * 删除测试库索引表模拟失败，验证两类业务发布均整体回滚。
     */
    @Test void indexFailureRollsBackMemoryAndCapsulePublishing() {
        jdbc.execute("DROP TABLE fm_universe_index");
        assertThrows(RuntimeException.class, () -> memories.create(1L, 2L, memoryRequest()));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM fm_memory", Integer.class));
        CapsuleRequest request = new CapsuleRequest(); request.setTitle("事务验证"); request.setContent("正文"); request.setOpenDate(LocalDate.now().plusDays(2));
        assertThrows(RuntimeException.class, () -> capsules.create(1L, 2L, request));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM fm_time_capsule", Integer.class));
    }
    /**
     * 校验非创建者不能修改标题及索引。
     */
    @Test void titleCannotBeChangedByAnotherCreator() {
        Long id = ((Number) memories.create(1L, 2L, memoryRequest()).get("id")).longValue();
        assertThrows(RuntimeException.class, () -> memories.updateTitle(id, 3L, "越权修改"));
        assertEquals("历史记忆", jdbc.queryForObject("SELECT title FROM fm_universe_index", String.class));
    }
    /** 使用真实Mapper验证按年日期边界、游标翻页、家庭隔离和逻辑删除过滤。 */
    @Test void yearBatchReadsOnlyMatchingYearFamilyAndActiveRows() {
        jdbc.update("INSERT INTO fm_universe_index(family_id,source_type,source_id,node_date,title,deleted) VALUES(1,'MEMORY',101,'2026-01-01','年初',0),(1,'CAPSULE',102,'2026-12-31','年末',0),(1,'MEMORY',103,'2027-01-01','下一年',0),(1,'MEMORY',104,'2026-06-01','已删除',1),(2,'MEMORY',105,'2026-06-01','别的家庭',0)");
        com.familymemory.dto.UniverseBatchResponse first = universe.year(1L, 2L, 2026, 0L, 1);
        assertEquals(1, first.getItems().size()); assertEquals("年初", first.getItems().get(0).getTitle()); assertTrue(first.isHasMore());
        com.familymemory.dto.UniverseBatchResponse second = universe.year(1L, 2L, 2026, first.getNextAfterId(), 1);
        assertEquals("年末", second.getItems().get(0).getTitle()); assertFalse(second.isHasMore());
        assertEquals("CAPSULE", second.getItems().get(0).getSourceType());
    }
    /**
     * 创建用于验证历史日期的记忆请求。
     * @return 测试对象或测试依赖
     */
    private MemoryRequest memoryRequest() { MemoryRequest request = new MemoryRequest(); request.setTitle("历史记忆"); request.setMemoryDate(LocalDate.of(2020, 2, 29)); return request; }
}
