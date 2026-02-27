# 家庭记忆应用 - API 接口文档

## 概述

- **基础路径**: `/api/v1`
- **数据格式**: JSON
- **认证方式**: JWT Token (Header: `Authorization: Bearer <token>`)
- **字符编码**: UTF-8

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 响应码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权/Token无效 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 一、认证模块 (Auth)

### 1.1 用户注册

**POST** `/api/v1/auth/register`

**请求参数**:
```json
{
  "username": "string",    // 用户名，3-20位
  "password": "string",    // 密码，6-20位
  "nickname": "string",    // 昵称，2-20位
  "phone": "string"        // 手机号（可选）
}
```

**响应**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "testuser",
    "nickname": "测试用户"
  }
}
```

### 1.2 用户登录

**POST** `/api/v1/auth/login`

**请求参数**:
```json
{
  "username": "string",
  "password": "string"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "testuser",
      "nickname": "测试用户",
      "avatar": "👨"
    }
  }
}
```

### 1.3 获取当前用户信息

**GET** `/api/v1/auth/me`

**Headers**: `Authorization: Bearer <token>`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "testuser",
    "nickname": "测试用户",
    "avatar": "👨",
    "phone": "13800138000",
    "currentFamily": {
      "id": 1,
      "name": "温馨小家"
    }
  }
}
```

---

## 二、家庭模块 (Family)

### 2.1 创建家庭

**POST** `/api/v1/families`

**Headers**: `Authorization: Bearer <token>`

**请求参数**:
```json
{
  "name": "string",           // 家庭名称
  "description": "string"     // 家庭描述（可选）
}
```

**响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "name": "温馨小家",
    "description": "我们幸福的一家人"
  }
}
```

### 2.2 获取家庭信息

**GET** `/api/v1/families/{familyId}`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "温馨小家",
    "description": "我们幸福的一家人",
    "memberCount": 4,
    "memoryCount": 128,
    "photoCount": 3582,
    "recordDays": 1204
  }
}
```

### 2.3 获取家庭成员列表

**GET** `/api/v1/families/{familyId}/members`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "nickname": "爸爸",
      "role": "一家之主",
      "avatar": "👨",
      "color": "bg-blue-100",
      "isAdmin": true
    },
    {
      "id": 2,
      "userId": 2,
      "nickname": "妈妈",
      "role": "超人妈妈",
      "avatar": "👩",
      "color": "bg-pink-100",
      "isAdmin": true
    }
  ]
}
```

### 2.4 生成邀请码

**POST** `/api/v1/families/{familyId}/invite-codes`

**请求参数**:
```json
{
  "expireHours": 24,    // 过期时间（小时），默认24
  "maxUses": 1          // 最大使用次数，默认1
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "code": "ABCD1234",
    "expireAt": "2024-01-16T10:30:00",
    "inviteLink": "https://family-memory.app/join/ABCD1234"
  }
}
```

### 2.5 通过邀请码加入家庭

**POST** `/api/v1/families/join`

**请求参数**:
```json
{
  "inviteCode": "ABCD1234",
  "nickname": "string",     // 在家庭中的昵称
  "role": "string"          // 家庭角色（可选）
}
```

**响应**:
```json
{
  "code": 200,
  "message": "加入成功",
  "data": {
    "familyId": 1,
    "familyName": "温馨小家"
  }
}
```

---

## 三、记忆模块 (Memory)

### 3.1 获取记忆列表

**GET** `/api/v1/families/{familyId}/memories`

**查询参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码，默认1 |
| size | int | 每页数量，默认10 |
| year | string | 年份筛选，如 "2023" |
| type | string | 类型筛选：photo/video/note |
| keyword | string | 关键词搜索 |

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "page": 1,
    "size": 10,
    "list": [
      {
        "id": 1,
        "type": "photo",
        "title": "宝宝第一次叫爸爸",
        "description": "今天晚饭后...",
        "date": "2023-10-15",
        "year": "2023",
        "author": "妈妈",
        "authorId": 2,
        "avatar": "👩",
        "location": "温馨的家",
        "images": [
          "https://..."
        ],
        "likes": 4,
        "tags": ["成长", "第一次"],
        "isLiked": false
      }
    ]
  }
}
```

### 3.2 获取记忆详情

**GET** `/api/v1/memories/{memoryId}`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "type": "photo",
    "title": "宝宝第一次叫爸爸",
    "description": "今天晚饭后，小宝突然看着爸爸清晰地喊了一声"爸爸"...",
    "date": "2023-10-15",
    "year": "2023",
    "author": "妈妈",
    "authorId": 2,
    "avatar": "👩",
    "location": "温馨的家",
    "images": [
      "https://..."
    ],
    "audioUrl": null,
    "audioDuration": null,
    "likes": 4,
    "tags": ["成长", "第一次"],
    "isLiked": false,
    "createdAt": "2023-10-15T18:30:00"
  }
}
```

### 3.3 创建记忆

**POST** `/api/v1/families/{familyId}/memories`

**Content-Type**: `multipart/form-data`

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 记忆标题 |
| description | string | 否 | 记忆描述 |
| type | string | 是 | 类型：photo/video/note |
| memoryDate | string | 是 | 记忆日期，格式：YYYY-MM-DD |
| location | string | 否 | 位置信息 |
| tags | string | 否 | 标签，逗号分隔 |
| files | File[] | 否 | 图片/视频文件，最多9个 |
| audio | File | 否 | 语音文件 |

**响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 5,
    "title": "新的记忆"
  }
}
```

### 3.4 点赞记忆

**POST** `/api/v1/memories/{memoryId}/like`

**响应**:
```json
{
  "code": 200,
  "message": "点赞成功",
  "data": {
    "likes": 5,
    "isLiked": true
  }
}
```

### 3.5 取消点赞

**DELETE** `/api/v1/memories/{memoryId}/like`

**响应**:
```json
{
  "code": 200,
  "message": "取消点赞",
  "data": {
    "likes": 4,
    "isLiked": false
  }
}
```

### 3.6 获取那年今日记忆

**GET** `/api/v1/families/{familyId}/memories/today-in-history`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "type": "photo",
      "title": "第一次去迪士尼",
      "description": "...",
      "date": "2021-01-15",
      "year": "2021",
      "yearsAgo": 3,
      "author": "妈妈",
      "avatar": "👩",
      "images": ["..."],
      "likes": 10
    }
  ]
}
```

### 3.7 获取可用年份列表

**GET** `/api/v1/families/{familyId}/memories/years`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": ["2023", "2022", "2021", "2020", "2015"]
}
```

---

## 四、时光胶囊模块 (Time Capsule)

### 4.1 获取时光胶囊列表

**GET** `/api/v1/families/{familyId}/capsules`

**查询参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| status | string | 状态：pending(待开启)/opened(已开启)/all(全部) |

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "pendingCount": 3,
    "list": [
      {
        "id": 1,
        "title": "给十岁的小宝",
        "openDate": "2025-05-20",
        "isOpened": false,
        "creatorName": "妈妈",
        "creatorAvatar": "👩",
        "createdAt": "2023-05-20"
      }
    ]
  }
}
```

### 4.2 创建时光胶囊

**POST** `/api/v1/families/{familyId}/capsules`

**请求参数**:
```json
{
  "title": "string",       // 胶囊标题
  "content": "string",     // 胶囊内容
  "openDate": "2025-05-20" // 开启日期
}
```

**响应**:
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 4,
    "title": "给未来的自己"
  }
}
```

### 4.3 获取胶囊详情（已开启的）

**GET** `/api/v1/capsules/{capsuleId}`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "给十岁的小宝",
    "content": "亲爱的小宝，当你打开这封信的时候...",
    "openDate": "2025-05-20",
    "isOpened": true,
    "openedAt": "2025-05-20T00:00:00",
    "creatorName": "妈妈",
    "creatorAvatar": "👩",
    "createdAt": "2023-05-20T10:30:00"
  }
}
```

---

## 五、文件上传模块 (File)

### 5.1 上传文件

**POST** `/api/v1/files/upload`

**Content-Type**: `multipart/form-data`

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 文件 |
| category | string | 否 | 分类：memory/avatar/audio，默认memory |

**响应**:
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": 1,
    "url": "https://xxx.com/files/xxx.jpg",
    "thumbnailUrl": "https://xxx.com/files/xxx_thumb.jpg",
    "fileName": "xxx.jpg",
    "fileSize": 102400,
    "fileType": "image/jpeg"
  }
}
```

### 5.2 批量上传文件

**POST** `/api/v1/files/upload/batch`

**Content-Type**: `multipart/form-data`

**请求参数**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| files | File[] | 是 | 文件数组，最多9个 |
| category | string | 否 | 分类 |

**响应**:
```json
{
  "code": 200,
  "message": "上传成功",
  "data": [
    {
      "id": 1,
      "url": "https://xxx.com/files/xxx1.jpg"
    },
    {
      "id": 2,
      "url": "https://xxx.com/files/xxx2.jpg"
    }
  ]
}
```

---

## 六、统计模块 (Statistics)

### 6.1 获取家庭统计数据

**GET** `/api/v1/families/{familyId}/statistics`

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordDays": 1204,      // 记录天数
    "photoCount": 3582,      // 照片/视频数量
    "memoryCount": 128,      // 珍藏故事数量
    "memberCount": 4,        // 成员数量
    "capsuleCount": 3        // 待开启胶囊数量
  }
}
```

---

## 错误响应示例

```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null
}
```

```json
{
  "code": 401,
  "message": "Token已过期，请重新登录",
  "data": null
}
```

```json
{
  "code": 404,
  "message": "记忆不存在",
  "data": null
}
```
