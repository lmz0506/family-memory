package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件上传记录实体，对应表：fm_file_upload。
 */
@Data
@TableName("fm_file_upload")
public class StoredFile {
    /**
     * 主键ID，对应表：fm_file_upload.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 用户ID，来源表：fm_user.id。
     */
    private Long userId;
    /**
     * 用户上传时的原始文件名。
     */
    private String originalName;
    /**
     * 服务端生成的存储文件名。
     */
    private String storedName;
    /**
     * 文件在服务端的存储路径。
     */
    private String filePath;
    /**
     * 对外访问文件的资源地址。
     */
    private String fileUrl;
    /**
     * 文件大小，单位字节。
     */
    private Long fileSize;
    /**
     * 文件MIME类型，例如image/jpeg、video/mp4、audio/mpeg。
     */
    private String fileType;
    /**
     * 文件业务分类，默认memory记忆，可由调用方扩展。
     */
    private String category;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
