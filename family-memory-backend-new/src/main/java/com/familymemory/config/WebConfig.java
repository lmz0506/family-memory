package com.familymemory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web配置，注册跨域规则和上传文件静态资源路径。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /**
     * 上传目录配置app.upload.directory。
     */
    @Value("${app.upload.directory}")
    private String uploadDirectory;
    /**
     * 上传文件公共访问路径配置app.upload.public-path。
     */
    @Value("${app.upload.public-path}")
    private String publicPath;

    /**
     * 配置跨域访问的路径、方法和请求头。
     * @param registry 跨域规则注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOriginPatterns("*").allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS"
        ).allowedHeaders("*").allowCredentials(true);
    }
    /**
     * 将上传资源访问前缀映射到本地存储目录。
     * @param registry 静态资源映射注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(publicPath + "/**").addResourceLocations(Paths.get(uploadDirectory).toAbsolutePath().normalize().toUri().toString());
    }
}
