package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class CapsuleRequest {
    @NotBlank(message = "请输入胶囊标题")
    private String title;
    @NotBlank(message = "请输入胶囊内容")
    private String content;
    private String recipient;
    private String coverUrl;
    @NotNull(message = "请选择开启日期")
    private LocalDate openDate;
}
