package com.familymemory;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证初始化脚本中演示密码摘要与约定密码一致，不连接数据库。 */
class DatabaseSeedTest {
    /** 校验BCrypt演示密码摘要，防止种子账号无法登录。 */
    @Test
    void demoPasswordHashMatchesDocumentedPassword() {
        String hash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        assertTrue(new BCryptPasswordEncoder().matches("123456", hash));
    }
}
