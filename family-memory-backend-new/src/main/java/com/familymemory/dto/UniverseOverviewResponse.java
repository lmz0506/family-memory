package com.familymemory.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 星海首次加载的年月日统计，只返回有内容的日期，不返回逐条内容。 */
@Data
public class UniverseOverviewResponse {
    /** 当前家庭有效记忆总数。 */
    private long memoryCount;
    /** 当前家庭有效珍藏总数，每份珍藏仅按封存日期计算一次。 */
    private long capsuleCount;
    /** 有内容的年份，按年份降序。 */
    private List<YearSummary> years = new ArrayList<>();

    /** 一个年份的两类内容计数及月份统计。 */
    @Data
    public static class YearSummary {
        /** 四位年份。 */
        private int year;
        /** 该年有效记忆数量。 */
        private long memoryCount;
        /** 该年有效珍藏数量。 */
        private long capsuleCount;
        /** 有内容的月份，按月份升序。 */
        private List<MonthSummary> months = new ArrayList<>();
    }

    /** 一个月份的两类内容计数及日期统计。 */
    @Data
    public static class MonthSummary {
        /** 月份，1至12。 */
        private int month;
        /** 该月有效记忆数量。 */
        private long memoryCount;
        /** 该月有效珍藏数量。 */
        private long capsuleCount;
        /** 有内容的日期，按日期升序。 */
        private List<DaySummary> days = new ArrayList<>();
    }

    /** 一天的计数。前端点击后再查询当天分页内容。 */
    @Data
    public static class DaySummary {
        /** 完整归档日期，格式 yyyy-MM-dd。 */
        private LocalDate date;
        /** 月内日期，1至31。 */
        private int day;
        /** 当天有效记忆数量。 */
        private long memoryCount;
        /** 当天有效珍藏数量。 */
        private long capsuleCount;
    }
}
