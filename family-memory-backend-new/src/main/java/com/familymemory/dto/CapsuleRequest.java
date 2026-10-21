package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 珍藏封存请求，开启日期决定正文可读时间。
 */
@Data
public class CapsuleRequest {
    /**
     * 标题，最长120字。
     */
    @NotBlank(message = "请输入胶囊标题")
    private String title;
    /**
     * 正文内容。
     */
    @NotBlank(message = "请输入胶囊内容")
    private String content;
    /**
     * 收信人称呼。
     */
    private String recipient;
    /**
     * 封面资源地址。
     */
    private String coverUrl;
    /**
     * 约定开启日期，不参与星海归档。
     */
    @NotNull(message = "请选择开启日期")
    private LocalDate openDate;
}
