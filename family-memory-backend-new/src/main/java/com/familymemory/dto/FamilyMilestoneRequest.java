package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class FamilyMilestoneRequest {
    @NotBlank(message = "请输入大事记标题")
    @Size(max = 120, message = "标题不能超过120个字")
    private String title;

    @Size(max = 500, message = "备注不能超过500个字")
    private String description;

    @NotNull(message = "请选择日期")
    private LocalDate eventDate;

    private Boolean recurringYearly = false;

    /** PRIVATE: 仅自己；FAMILY: 全家；SPECIFIC: 指定家人。 */
    private String visibilityScope = "PRIVATE";

    private List<Long> viewerUserIds = new ArrayList<>();
}
