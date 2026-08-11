package com.familymemory.service;

import com.familymemory.common.BusinessException;
import com.familymemory.entity.StoredFile;
import com.familymemory.mapper.StoredFileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path root;
    private final String publicPath;
    private final StoredFileMapper mapper;

    public FileStorageService(@Value("${app.upload.directory}") String directory,
                              @Value("${app.upload.public-path}") String publicPath, StoredFileMapper mapper) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        this.publicPath = publicPath;
        this.mapper = mapper;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录", e);
        }
    }

    public Map<String, Object> store(MultipartFile file, String category, Long userId) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择文件");
        }
        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String stored = UUID.randomUUID().toString().replace("-", "") + extension.toLowerCase();
        Path target = root.resolve(stored).normalize();
        if (!target.startsWith(root)) {
            throw BusinessException.badRequest("文件名不合法");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败");
        }
        String url = "/api/v1" + publicPath + "/" + stored;
        StoredFile record = new StoredFile();
        record.setUserId(userId);
        record.setOriginalName(original);
        record.setStoredName(stored);
        record.setFilePath(target.toString());
        record.setFileUrl(url);
        record.setFileSize(file.getSize());
        record.setFileType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        record.setCategory(StringUtils.hasText(category) ? category : "memory");
        record.setCreatedAt(LocalDateTime.now());
        mapper.insert(record);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", record.getId());
        map.put("url", url);
        map.put("fileName", original);
        map.put("fileSize", file.getSize());
        map.put("fileType", record.getFileType());
        return map;
    }
}
