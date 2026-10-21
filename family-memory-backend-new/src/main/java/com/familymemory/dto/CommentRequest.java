package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 记忆评论发布请求。
 */
@Data
public class CommentRequest {
    /**
     * 正文内容。
     */
    @NotBlank(message = "回应不能为空")
    @Size(max = 500, message = "回应最多 500 个字")
    private String content;
}
