package com.familymemory.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建家庭请求
 */
@Data
public class CreateFamilyReq {

    @NotBlank(message = "家庭名称不能为空")
    private String name;

    private String description;
}
