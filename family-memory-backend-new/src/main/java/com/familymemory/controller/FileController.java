package com.familymemory.controller;

import com.familymemory.common.ApiResult;
import com.familymemory.security.UserContext;
import com.familymemory.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/uploads")
public class FileController {
    private final FileStorageService service;

    public FileController(FileStorageService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResult<Map<String, Object>> upload(@RequestParam MultipartFile file, @RequestParam(defaultValue =
            "memory") String category) {
        return ApiResult.ok("上传成功", service.store(file, category, UserContext.require()));
    }
}
