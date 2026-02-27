package com.familymemory.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件URL处理工具
 * 数据库存储相对路径，返回给前端时拼接完整URL
 */
@Component
public class FileUrlHelper {

    private static String baseUrl;

    @Value("${file.base-url}")
    public void setBaseUrl(String url) {
        FileUrlHelper.baseUrl = url;
    }

    /**
     * 将相对路径转换为完整URL
     * 如：/files/memory/2026/02/04/xxx.jpg -> http://localhost:8080/api/v1/files/memory/2026/02/04/xxx.jpg
     */
    public static String toFullUrl(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }
        // 如果已经是完整URL则直接返回
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        // 去掉开头的 /files 因为 baseUrl 已经包含了
        if (relativePath.startsWith("/files/")) {
            relativePath = relativePath.substring(7); // 去掉 "/files/"
        }
        return baseUrl + "/" + relativePath;
    }

    /**
     * 将完整URL转换为相对路径用于存储
     * 如：http://localhost:8080/api/v1/files/memory/2026/02/04/xxx.jpg -> /files/memory/2026/02/04/xxx.jpg
     */
    public static String toRelativePath(String fullUrl) {
        if (!StringUtils.hasText(fullUrl)) {
            return null;
        }
        // 如果已经是相对路径则直接返回
        if (fullUrl.startsWith("/files/")) {
            return fullUrl;
        }
        // 提取相对路径
        if (fullUrl.contains("/files/")) {
            int index = fullUrl.indexOf("/files/");
            return fullUrl.substring(index);
        }
        return fullUrl;
    }
}
