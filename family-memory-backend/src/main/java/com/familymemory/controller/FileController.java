package com.familymemory.controller;

import com.familymemory.common.Result;
import com.familymemory.dto.resp.FileUploadResp;
import com.familymemory.service.FileService;
import com.familymemory.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传单个文件
     */
    @PostMapping("/upload")
    public Result<FileUploadResp> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "memory") String category) {
        Long userId = UserContext.getUserId();
        return Result.success("上传成功", fileService.uploadFile(file, category, userId));
    }

    /**
     * 批量上传文件
     */
    @PostMapping("/upload/batch")
    public Result<List<FileUploadResp>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "category", defaultValue = "memory") String category) {
        Long userId = UserContext.getUserId();
        return Result.success("上传成功", fileService.uploadFiles(files, category, userId));
    }
}
