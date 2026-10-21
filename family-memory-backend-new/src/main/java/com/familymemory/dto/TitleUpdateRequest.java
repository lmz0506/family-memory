package com.familymemory.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 记忆或珍藏的标题修改请求，不接受日期变更。 */
@Data
public class TitleUpdateRequest {
    /** 新标题，去除首尾空白后写入业务表及星海索引，最长120字。 */
    @NotBlank(message = "请输入标题")
    @Size(max = 120, message = "标题不能超过120字")
    private String title;
}
