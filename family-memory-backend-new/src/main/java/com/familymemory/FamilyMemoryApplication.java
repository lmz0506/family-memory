package com.familymemory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.familymemory.mapper")
@SpringBootApplication
public class FamilyMemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(FamilyMemoryApplication.class, args);
    }
}
