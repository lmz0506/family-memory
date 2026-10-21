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

/**
 * 文件上传接口，接收文件并返回资源访问地址。
 */
@RestController
@RequestMapping("/uploads")
public class FileController {
    /**
     * 本地文件存储业务，使用随机存储名称并记录上传信息。
     */
    private final FileStorageService service;

    /**
     * 初始化 FileController 的依赖。
     * @param service 本地文件存储业务，使用随机存储名称并记录上传信息
     */
    public FileController(FileStorageService service) {
        this.service = service;
    }

    /**
     * 上传文件并返回资源访问地址及文件信息。
     * @param file 用户上传的文件
     * @param category 文件业务分类，默认memory记忆，可扩展
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping
    public ApiResult<Map<String, Object>> upload(@RequestParam MultipartFile file, @RequestParam(defaultValue =
            "memory") String category) {
        return ApiResult.ok("上传成功", service.store(file, category, UserContext.require()));
    }
}
