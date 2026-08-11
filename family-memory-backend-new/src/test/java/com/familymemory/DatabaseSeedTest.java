package com.familymemory;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSeedTest {
    @Test
    void demoPasswordHashMatchesDocumentedPassword() {
        String hash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";
        assertTrue(new BCryptPasswordEncoder().matches("123456", hash));
    }
}
