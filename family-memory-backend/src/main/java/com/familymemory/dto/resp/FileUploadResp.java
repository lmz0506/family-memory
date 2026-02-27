package com.familymemory.dto.resp;

import lombok.Data;

/**
 * 文件上传响应
 */
@Data
public class FileUploadResp {

    private Long id;

    /**
     * 相对路径，用于存储到数据库（如 /files/memory/2026/02/04/xxx.jpg）
     */
    private String url;

    /**
     * 完整URL，用于前端直接访问（如 http://localhost:8080/api/v1/files/memory/2026/02/04/xxx.jpg）
     */
    private String fullUrl;

    private String thumbnailUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
}
