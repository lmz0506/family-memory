package com.familymemory.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.resp.FileUploadResp;
import com.familymemory.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件服务实现
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.base-url}")
    private String baseUrl;

    @Override
    public FileUploadResp uploadFile(MultipartFile file, String category, Long userId) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException("无法识别文件类型");
        }

        // 允许的文件类型
        if (!contentType.startsWith("image/") && !contentType.startsWith("video/") && !contentType.startsWith("audio/")) {
            throw new BusinessException("不支持的文件类型");
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null ? FileUtil.extName(originalFilename) : "bin";
        String newFilename = IdUtil.fastSimpleUUID() + "." + ext;

        // 按日期分目录存储
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = category + "/" + datePath + "/" + newFilename;

        // 使用绝对路径，避免 transferTo 使用 Tomcat 临时目录
        Path absoluteUploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        File destFile = absoluteUploadDir.resolve(relativePath).toFile();

        // 创建目录
        FileUtil.mkParentDirs(destFile);

        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }

        // 构建响应 - 存储相对路径，便于数据迁移
        String storagePath = "/files/" + relativePath;

        FileUploadResp resp = new FileUploadResp();
        resp.setUrl(storagePath);  // 相对路径，用于存储到数据库
        resp.setFullUrl(baseUrl + "/" + relativePath);  // 完整URL，用于前端直接使用
        resp.setFileName(originalFilename);
        resp.setFileSize(file.getSize());
        resp.setFileType(contentType);

        return resp;
    }

    @Override
    public List<FileUploadResp> uploadFiles(MultipartFile[] files, String category, Long userId) {
        if (files == null || files.length == 0) {
            throw new BusinessException("文件不能为空");
        }

        if (files.length > 9) {
            throw new BusinessException("最多同时上传9个文件");
        }

        List<FileUploadResp> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadFile(file, category, userId));
        }

        return results;
    }
}
