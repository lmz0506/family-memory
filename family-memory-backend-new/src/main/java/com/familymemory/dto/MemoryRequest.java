package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
public class MemoryRequest {
    @NotBlank(message = "请输入记忆标题")
    private String title;
    private String content;
    private String type = "photo";
    @NotNull(message = "请选择发生日期")
    private LocalDate memoryDate;
    private String location;
    private String audioUrl;
    private Integer audioDuration;
    private List<String> mediaUrls;
    private List<String> tags;
}
