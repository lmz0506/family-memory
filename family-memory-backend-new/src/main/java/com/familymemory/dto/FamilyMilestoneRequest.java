package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 家庭提醒创建或修改请求。
 */
@Data
public class FamilyMilestoneRequest {
    /**
     * 标题，最长120字。
     */
    @NotBlank(message = "请输入大事记标题")
    @Size(max = 120, message = "标题不能超过120个字")
    private String title;

    /**
     * 说明文本。
     */
    @Size(max = 500, message = "备注不能超过500个字")
    private String description;

    /**
     * 提醒原始日期，来源表：fm_family_milestone.event_date。
     */
    @NotNull(message = "请选择日期")
    private LocalDate eventDate;

    /**
     * 每年重复标志，false一次性提醒，true每年重复。
     */

    private Boolean recurringYearly = false;

    /** PRIVATE: 仅自己；FAMILY: 全家；SPECIFIC: 指定家人。 */
    private String visibilityScope = "PRIVATE";

    /**
     * 指定可见用户ID列表，来源表：fm_user.id，成员必须属于当前家庭。
     */

    private List<Long> viewerUserIds = new ArrayList<>();
}
