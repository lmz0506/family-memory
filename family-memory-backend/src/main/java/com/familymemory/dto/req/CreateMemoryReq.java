package com.familymemory.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建记忆请求
 */
@Data
public class CreateMemoryReq {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "类型不能为空")
    private String type;

    @NotNull(message = "日期不能为空")
    private String memoryDate;

    private String location;

    private List<String> tags;

    private List<String> mediaUrls;

    private String audioUrl;

    private Integer audioDuration;
}
