package com.familymemory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 家庭记忆后端应用启动入口。
 */
@MapperScan("com.familymemory.mapper")
@SpringBootApplication
public class FamilyMemoryApplication {
    /**
     * 启动家庭记忆后端服务。
     * @param args 应用启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FamilyMemoryApplication.class, args);
    }
}
