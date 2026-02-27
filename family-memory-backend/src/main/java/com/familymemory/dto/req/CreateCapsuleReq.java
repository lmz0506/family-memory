package com.familymemory.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建时光胶囊请求
 */
@Data
public class CreateCapsuleReq {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @NotNull(message = "开启日期不能为空")
    private String openDate;
}
