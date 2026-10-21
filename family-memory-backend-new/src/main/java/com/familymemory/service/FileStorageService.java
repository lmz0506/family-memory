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

/**
 * 本地文件存储业务，使用随机存储名称并记录上传信息。
 */
@Service
public class FileStorageService {
    /**
     * 规范化后的本地上传目录。
     */
    private final Path root;
    /**
     * 上传文件公共访问路径配置app.upload.public-path。
     */
    private final String publicPath;
    /**
     * 文件上传记录持久层，对应表：fm_file_upload。
     */
    private final StoredFileMapper mapper;

    /**
     * 初始化文件存储依赖，并创建不存在的上传目录。
     * @param directory 本地上传目录配置
     * @param publicPath 上传资源公共访问前缀
     * @param mapper 文件上传记录持久层，对应表：fm_file_upload
     */
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

    /**
     * 校验并保存上传文件，记录上传元数据后返回访问地址。
     * @param file 用户上传的文件
     * @param category 文件业务分类，默认memory记忆，可扩展
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
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
        // 返回字段 id：记录ID，来源表：fm_file_upload.id。
        map.put("id", record.getId());
        // 返回字段 url：上传文件资源访问地址。
        map.put("url", url);
        // 返回字段 fileName：原始上传文件名。
        map.put("fileName", original);
        // 返回字段 fileSize：文件大小，单位字节。
        map.put("fileSize", file.getSize());
        // 返回字段 fileType：文件MIME类型。
        map.put("fileType", record.getFileType());
        return map;
    }
}
