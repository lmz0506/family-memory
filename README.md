# 家庭记忆应用 (Family Memory)

一个温馨的家庭记忆记录应用，帮助家人共同记录和分享生活中的美好时刻。

## 项目结构

```
family-memory/
├── front/                    # Vue 3 前端项目
├── family-memory-backend/    # Spring Boot 后端项目
└── doc/                      # 项目文档
    ├── api-documentation.md  # API 接口文档
    └── database-design.sql   # 数据库设计文档
```

## 技术栈

### 前端
- Vue 3 + TypeScript
- Vite
- Pinia (状态管理)
- Vue Router
- Tailwind CSS
- Axios

### 后端
- Spring Boot 3.2
- MyBatis-Plus
- MySQL 8.0
- JWT 认证
- Spring Security

## 快速开始

### 1. 数据库配置

```bash
# 登录 MySQL
mysql -u root -p

# 执行数据库初始化脚本
source doc/database-design.sql
```

### 2. 后端启动

```bash
cd family-memory-backend

# 修改数据库配置 (src/main/resources/application.yml)
# spring.datasource.url
# spring.datasource.username
# spring.datasource.password

# Maven 打包运行
mvn spring-boot:run
```

后端服务将运行在 http://localhost:8080

### 3. 前端启动

```bash
cd front

# 安装依赖
npm install

# 开发模式运行
npm run dev
```

前端应用将运行在 http://localhost:5173

## 测试账号

数据库初始化后可使用以下测试账号：

| 用户名 | 密码 | 角色 |
|--------|------|------|
| dad | 123456 | 爸爸 |
| mom | 123456 | 妈妈 |
| baby | 123456 | 小宝 |
| grandpa | 123456 | 爷爷 |

## 主要功能

### 温馨界面 (Home)
- 搜索记忆
- 那年今日 - 显示历年今日的记忆
- 最新动态 - 显示最新的家庭记忆

### 时光轴 (Timeline)
- 按年份筛选记忆
- 时间线展示家庭记忆

### 添加记忆
- 文字记录
- 照片/视频上传
- 语音录制
- 日期选择
- 位置标记

### 珍藏 (Vault)
- 时光胶囊 - 写给未来的信
- 设置开启日期

### 家人 (Family)
- 家庭成员管理
- 邀请家人加入
- 家庭统计数据

## API 文档

详细的 API 文档请查看 [doc/api-documentation.md](doc/api-documentation.md)

## 数据库设计

数据库设计文档请查看 [doc/database-design.sql](doc/database-design.sql)

### 主要数据表

| 表名 | 说明 |
|------|------|
| fm_user | 用户表 |
| fm_family | 家庭表 |
| fm_family_member | 家庭成员关系表 |
| fm_memory | 记忆表 |
| fm_memory_media | 记忆媒体表 |
| fm_memory_tag | 记忆标签表 |
| fm_memory_like | 记忆点赞表 |
| fm_time_capsule | 时光胶囊表 |
| fm_invite_code | 邀请码表 |
| fm_file_upload | 文件上传记录表 |

## 项目配置

### 前端环境变量

```env
# .env.development
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### 后端配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/family_memory
    username: root
    password: root

jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时

file:
  upload-dir: ./upload
  base-url: http://localhost:8080/api/v1/files
```

## 开发说明

### 前端开发规范
- 使用 Vue 3 Composition API
- 使用 TypeScript 进行类型检查
- 使用 Pinia 进行状态管理
- 组件文件使用 PascalCase 命名

### 后端开发规范
- 遵循 RESTful API 设计规范
- 使用 MyBatis-Plus 进行数据库操作
- 统一响应格式 `{ code, message, data }`
- 使用逻辑删除

## License

MIT
