package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 家庭空间创建请求。
 */
@Data
public class FamilyRequest {
    /**
     * 家庭名称。
     */
    @NotBlank(message = "请输入家庭名称")
    @Size(max = 60, message = "家庭名称最多 60 个字")
    private String name;
    /**
     * 说明文本。
     */
    @Size(max = 300, message = "家庭介绍最多 300 个字")
    private String description;
    /**
     * 封面资源地址。
     */
    private String coverUrl;
}
