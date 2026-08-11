package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class FamilyRequest {
    @NotBlank(message = "请输入家庭名称")
    @Size(max = 60, message = "家庭名称最多 60 个字")
    private String name;
    @Size(max = 300, message = "家庭介绍最多 300 个字")
    private String description;
    private String coverUrl;
}
