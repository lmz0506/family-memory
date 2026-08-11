package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fm_file_upload")
public class StoredFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String originalName;
    private String storedName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private String category;
    private LocalDateTime createdAt;
}
