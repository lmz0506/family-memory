package com.familymemory;

import com.familymemory.common.BusinessException;
import com.familymemory.dto.*;
import com.familymemory.mapper.UniverseIndexMapper;
import com.familymemory.service.FamilyService;
import com.familymemory.service.UniverseService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 不连接真实数据库的星海查询契约测试。 */
class UniverseServiceTest {
    /** 测试索引Mapper替身。 */
    private final UniverseIndexMapper mapper = mock(UniverseIndexMapper.class);
    /** 测试家庭权限替身。 */
    private final FamilyService families = mock(FamilyService.class);
    /** 被测星海查询服务。 */
    private final UniverseService service = new UniverseService(mapper, families);

    /**
     * 校验概要仅包含有内容的日期，并分别累计记忆和珍藏。
     */

    @Test void overviewGroupsOnlyOccupiedDatesAndSeparatesSources() {
        when(mapper.dailyCounts(1L)).thenReturn(Arrays.asList(row("2026-08-07", 3, 1), row("2023-01-01", 1, 0), row("2026-06-03", 0, 2)));
        UniverseOverviewResponse result = service.overview(1L, 2L);
        assertEquals(4, result.getMemoryCount()); assertEquals(3, result.getCapsuleCount());
        assertEquals(2026, result.getYears().get(0).getYear());
        assertEquals(6, result.getYears().get(0).getMonths().get(0).getMonth());
        assertEquals(2, result.getYears().get(0).getMonths().size());
        verify(families).requireMember(1L, 2L); verify(mapper).dailyCounts(1L); verifyNoMoreInteractions(mapper);
    }
    /**
     * 校验未授权家庭在执行SQL前被拒绝。
     */
    @Test void unauthorizedFamilyNeverQueriesIndex() {
        when(families.requireMember(2L, 2L)).thenThrow(BusinessException.forbidden("非家庭成员"));
        assertThrows(BusinessException.class, () -> service.overview(2L, 2L));
        assertThrows(BusinessException.class, () -> service.search(2L, 2L, "标题", null, 1, 24));
        verifyNoInteractions(mapper);
    }
    /**
     * 校验搜索特殊字符转义及服务端分页上限。
     */
    @Test void searchEscapesWildcardsAndBoundsPagination() {
        String pattern = "%a!%!_!!%";
        when(mapper.countItems(1L, null, "CAPSULE", pattern)).thenReturn(51L);
        when(mapper.listItems(1L, null, "CAPSULE", pattern, 0L, 50)).thenReturn(Collections.nCopies(50, new UniverseItemResponse()));
        UniversePageResponse result = service.search(1L, 2L, " a%_! ", "capsule", 1, 10000);
        assertEquals(50, result.getSize()); assertTrue(result.isHasMore());
        verify(mapper).listItems(1L, null, "CAPSULE", pattern, 0L, 50);
    }
    /**
     * 校验日期精确筛选、非法来源和非法页码。
     */
    @Test void dayUsesExactDateAndRejectsUnsupportedSource() {
        LocalDate date = LocalDate.of(2026, 8, 7);
        when(mapper.countItems(1L, date, null, null)).thenReturn(0L);
        assertTrue(service.day(1L, 2L, date, null, 1, 24).getItems().isEmpty());
        verify(mapper, never()).listItems(anyLong(), any(), any(), any(), anyLong(), anyInt());
        assertThrows(BusinessException.class, () -> service.search(1L, 2L, "", "photo", 1, 24));
        assertThrows(BusinessException.class, () -> service.day(1L, 2L, date, null, 0, 24));
    }
    /** 验证按年游标查询的范围、批量上限和下一批游标，不执行总数统计。 */
    @Test void yearBatchUsesBoundedCursorAndExclusiveNextYear() {
        UniverseItemResponse first = new UniverseItemResponse(); first.setIndexId(11L);
        UniverseItemResponse second = new UniverseItemResponse(); second.setIndexId(17L);
        UniverseItemResponse extra = new UniverseItemResponse(); extra.setIndexId(20L);
        when(mapper.yearBatch(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 9L, 3))
            .thenReturn(Arrays.asList(first, second, extra));
        UniverseBatchResponse result = service.year(1L, 2L, 2026, 9L, 2);
        assertEquals(2, result.getItems().size()); assertEquals(17L, result.getNextAfterId()); assertTrue(result.isHasMore());
        when(mapper.yearBatch(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 17L, 201)).thenReturn(Collections.emptyList());
        UniverseBatchResponse empty = service.year(1L, 2L, 2026, 17L, 10000);
        assertEquals(17L, empty.getNextAfterId()); assertFalse(empty.isHasMore());
        verify(mapper, never()).countItems(anyLong(), any(), any(), any());
    }

    /** 验证按年预加载仍先校验家庭权限，并拒绝非法年份和负游标。 */
    @Test void yearBatchRejectsInvalidParametersAndUnauthorizedFamily() {
        when(families.requireMember(2L, 2L)).thenThrow(BusinessException.forbidden("非家庭成员"));
        assertThrows(BusinessException.class, () -> service.year(2L, 2L, 2026, 0L, 120));
        assertThrows(BusinessException.class, () -> service.year(1L, 2L, 10000, 0L, 120));
        assertThrows(BusinessException.class, () -> service.year(1L, 2L, 2026, -1L, 120));
        verifyNoInteractions(mapper);
    }
    /**
     * 构造一天的双来源统计测试行。
     * @param date 测试归档日期，格式yyyy-MM-dd
     * @param memory 测试记忆数量
     * @param capsule 测试珍藏数量
     * @return 测试对象或测试依赖
     */
    private UniverseDayCount row(String date, long memory, long capsule) {
        UniverseDayCount row = new UniverseDayCount(); row.setNodeDate(LocalDate.parse(date));
        row.setMemoryCount(memory); row.setCapsuleCount(capsule); return row;
    }
}
