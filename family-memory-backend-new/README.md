# 拾光 · 家庭记忆后端

基于 Spring Boot 2.7、MyBatis-Plus、MySQL 和 JWT 的独立后端服务。

## 启动

1. 执行 `database/schema.sql` 初始化数据库。
2. 按需配置环境变量 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和 `JWT_SECRET`。
3. 运行：

```bash
mvn spring-boot:run
```

服务默认地址为 `http://localhost:8080/api/v1`。测试账号和初始化数据见数据库脚本。
