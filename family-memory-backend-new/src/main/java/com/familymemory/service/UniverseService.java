package com.familymemory.service;

import com.familymemory.common.BusinessException;
import com.familymemory.entity.UniverseSourceType;
import com.familymemory.dto.*;
import com.familymemory.mapper.UniverseIndexMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

/** 星海读服务：概要只读计数，日期和搜索只读分页索引，详情交给原业务接口。 */
@Service
public class UniverseService {
    /** 星海索引持久层。 */
    private final UniverseIndexMapper mapper;
    /** 家庭成员权限服务。 */
    private final FamilyService familyService;

    /**
     * 初始化星海只读服务。
     * @param mapper 星海索引持久层
     * @param familyService 家庭成员权限服务
     */
    public UniverseService(UniverseIndexMapper mapper, FamilyService familyService) {
        this.mapper = mapper;
        this.familyService = familyService;
    }

    /**
     * 一次返回有内容的年月日及记忆、珍藏数量，不拉取逐条业务内容。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 按年份降序、月份及日期升序组织的轻量概要
     */
    @Transactional(readOnly = true)
    public UniverseOverviewResponse overview(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        UniverseOverviewResponse result = new UniverseOverviewResponse();
        Map<Integer, UniverseOverviewResponse.YearSummary> years = new TreeMap<>(Collections.reverseOrder());
        Map<Integer, Map<Integer, UniverseOverviewResponse.MonthSummary>> months = new HashMap<>();
        for (UniverseDayCount row : mapper.dailyCounts(familyId)) {
            LocalDate date = row.getNodeDate();
            UniverseOverviewResponse.YearSummary year = years.computeIfAbsent(date.getYear(), value -> {
                UniverseOverviewResponse.YearSummary item = new UniverseOverviewResponse.YearSummary();
                item.setYear(value); return item;
            });
            UniverseOverviewResponse.MonthSummary month = months.computeIfAbsent(date.getYear(), value -> new TreeMap<>())
                .computeIfAbsent(date.getMonthValue(), value -> {
                    UniverseOverviewResponse.MonthSummary item = new UniverseOverviewResponse.MonthSummary();
                    item.setMonth(value); return item;
                });
            UniverseOverviewResponse.DaySummary day = new UniverseOverviewResponse.DaySummary();
            day.setDate(date); day.setDay(date.getDayOfMonth());
            day.setMemoryCount(row.getMemoryCount()); day.setCapsuleCount(row.getCapsuleCount());
            month.getDays().add(day);
            month.setMemoryCount(month.getMemoryCount() + row.getMemoryCount());
            month.setCapsuleCount(month.getCapsuleCount() + row.getCapsuleCount());
            year.setMemoryCount(year.getMemoryCount() + row.getMemoryCount());
            year.setCapsuleCount(year.getCapsuleCount() + row.getCapsuleCount());
            result.setMemoryCount(result.getMemoryCount() + row.getMemoryCount());
            result.setCapsuleCount(result.getCapsuleCount() + row.getCapsuleCount());
        }
        for (UniverseOverviewResponse.YearSummary year : years.values()) {
            year.setMonths(new ArrayList<>(months.get(year.getYear()).values()));
            for (UniverseOverviewResponse.MonthSummary month : year.getMonths()) {
                month.getDays().sort(Comparator.comparing(UniverseOverviewResponse.DaySummary::getDate));
            }
        }
        result.setYears(new ArrayList<>(years.values()));
        return result;
    }

    /**
     * 年月日概要载入后，在后台按年份分批读取真实星点索引。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param year 归档年份，范围1至9998
     * @param afterId 排他索引游标，来源表：fm_universe_index.id，首次为0
     * @param size 批量大小，限制为1至200
     * @return 当前年份轻量批次及下一批游标，不读取正文或媒体
     */
    @Transactional(readOnly = true)
    public UniverseBatchResponse year(Long familyId, Long userId, int year, long afterId, int size) {
        familyService.requireMember(familyId, userId);
        if (year < 1 || year > 9998) throw BusinessException.badRequest("年份必须在1至9998之间");
        if (afterId < 0) throw BusinessException.badRequest("索引游标不能小于0");
        int boundedSize = Math.min(200, Math.max(1, size));
        List<UniverseItemResponse> rows = mapper.yearBatch(familyId, LocalDate.of(year, 1, 1),
            LocalDate.of(year + 1, 1, 1), afterId, boundedSize + 1);
        boolean hasMore = rows.size() > boundedSize;
        List<UniverseItemResponse> items = hasMore ? new ArrayList<>(rows.subList(0, boundedSize)) : rows;
        UniverseBatchResponse response = new UniverseBatchResponse();
        response.setYear(year); response.setItems(items); response.setHasMore(hasMore);
        response.setNextAfterId(items.isEmpty() ? afterId : items.get(items.size() - 1).getIndexId());
        return response;
    }

    /**
     * 兼容旧版日期分页读取；新版星海使用按年份后台预加载。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param date 精确归档日期
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏；null 表示全部
     * @param page 页码，从1开始
     * @param size 分页大小，限制为1至50
     * @return 当天分页索引，不包含正文
     */
    @Transactional(readOnly = true)
    public UniversePageResponse day(Long familyId, Long userId, LocalDate date, String sourceType, int page, int size) {
        familyService.requireMember(familyId, userId);
        if (date == null) throw BusinessException.badRequest("请选择日期");
        return page(familyId, date, normalizeType(sourceType), null, page, size);
    }

    /**
     * 在当前家庭的索引标题内搜索，返回有界分页及每条结果的日期路径。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param keyword 标题关键词，最多120字；空串表示按来源分页浏览
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏；null 表示全部
     * @param page 页码，从1开始
     * @param size 分页大小，限制为1至50
     * @return 当前页匹配索引及匹配总数，不搜索封存正文
     */
    @Transactional(readOnly = true)
    public UniversePageResponse search(Long familyId, Long userId, String keyword, String sourceType, int page, int size) {
        familyService.requireMember(familyId, userId);
        String text = keyword == null ? "" : keyword.trim();
        if (text.length() > 120) throw BusinessException.badRequest("搜索词不能超过120字");
        String pattern = text.isEmpty() ? null : "%" + text.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return page(familyId, null, normalizeType(sourceType), pattern, page, size);
    }

    /**
     * 构造只读分页响应。调用者必须已校验家庭权限。
     * @param familyId 已授权家庭ID，来源表：fm_family.id
     * @param date 日期筛选；null 表示全部日期
     * @param sourceType 已规范化来源类型；null 表示全部
     * @param pattern 已转义标题模式；null 表示不过滤标题
     * @param page 页码，从1开始
     * @param size 请求分页大小
     * @return 总数、页码、实际分页大小及当前页条目
     */
    private UniversePageResponse page(Long familyId, LocalDate date, String sourceType, String pattern, int page, int size) {
        if (page < 1) throw BusinessException.badRequest("页码必须大于0");
        int boundedSize = Math.min(50, Math.max(1, size));
        long total = mapper.countItems(familyId, date, sourceType, pattern);
        long offset = (long) (page - 1) * boundedSize;
        UniversePageResponse response = new UniversePageResponse();
        response.setTotal(total); response.setPage(page); response.setSize(boundedSize);
        response.setItems(offset >= total ? Collections.emptyList() : mapper.listItems(familyId, date, sourceType, pattern, offset, boundedSize));
        response.setHasMore(offset + response.getItems().size() < total);
        return response;
    }

    /**
     * 规范化并限制来源值，避免把原业务的媒体类型误当来源类型。
     * @param value MEMORY 记忆，CAPSULE 珍藏，空值表示全部
     * @return 大写来源类型或null
     */
    public static String normalizeType(String value) {
        return UniverseSourceType.normalize(value);
    }
}
