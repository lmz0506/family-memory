package com.familymemory.service;

import com.familymemory.dto.resp.FileUploadResp;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传单个文件
     */
    FileUploadResp uploadFile(MultipartFile file, String category, Long userId);

    /**
     * 批量上传文件
     */
    List<FileUploadResp> uploadFiles(MultipartFile[] files, String category, Long userId);
}
