package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 记忆发布请求，包含发生日期、正文及媒体资源地址。
 */
@Data
public class MemoryRequest {
    /**
     * 标题，最长120字。
     */
    @NotBlank(message = "请输入记忆标题")
    private String title;
    /**
     * 正文内容。
     */
    private String content;
    /**
     * 记忆类型，photo照片，video视频，note文字，audio声音。
     */
    private String type = "photo";
    /**
     * 记忆发生日期，发布后固定，来源表：fm_memory.memory_date。
     */
    @NotNull(message = "请选择发生日期")
    private LocalDate memoryDate;
    /**
     * 发生地点。
     */
    private String location;
    /**
     * 音频资源地址。
     */
    private String audioUrl;
    /**
     * 音频时长，单位秒。
     */
    private Integer audioDuration;
    /**
     * 媒体资源地址列表，按请求顺序展示。
     */
    private List<String> mediaUrls;
    /**
     * 标签名称列表。
     */
    private List<String> tags;
}
