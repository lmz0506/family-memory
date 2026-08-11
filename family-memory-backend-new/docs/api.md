# 拾光 API

基础路径：`/api/v1`。除登录、注册和静态文件外，请携带 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/register` | 注册并签发 Token |
| POST | `/auth/login` | 登录 |
| GET | `/auth/me` | 当前用户及家庭列表 |
| GET/POST | `/families` | 我的家庭 / 创建家庭 |
| GET | `/families/{id}` | 家庭详情与统计 |
| GET | `/families/{id}/members` | 家庭成员 |
| POST | `/families/{id}/invite-codes` | 创建邀请码 |
| POST | `/families/join` | 使用邀请码加入 |
| GET/POST | `/families/{id}/memories` | 查询 / 创建记忆 |
| GET | `/families/{id}/memories/today-in-history` | 那年今日 |
| GET | `/families/{id}/memories/years` | 可用年份 |
| GET/DELETE | `/memories/{id}` | 记忆详情 / 删除 |
| POST | `/memories/{id}/like` | 切换点赞 |
| POST | `/memories/{id}/comments` | 家人回应 |
| GET/POST | `/families/{id}/capsules` | 胶囊列表 / 创建 |
| GET | `/capsules/{id}` | 胶囊详情；到期时自动开启 |
| POST | `/uploads` | multipart 文件上传 |

所有响应采用：

```json
{ "code": 200, "message": "success", "data": {} }
```

详细请求字段以 `src/main/java/com/familymemory/dto` 下的 DTO 为准。
