# 拾光 · 家庭记忆前端

Vue 3 + TypeScript + Pinia + Vue Router + Tailwind CSS 实现的全新家庭记忆应用。

## 本地运行

```bash
npm install
npm run dev
```

开发服务器会将 `/api` 代理到 `http://localhost:8080`。也可通过 `VITE_API_BASE_URL` 指定后端地址。

## 主要功能

- 登录、注册与家庭初始化
- 创建家庭、邀请码加入和成员展示
- 记忆发布、图片上传、时间轴、搜索和年份筛选
- 那年今日、点赞、家人回应
- 时光胶囊创建和倒计时
- 桌面三栏与移动端底部导航响应式布局
- 演示图片全部内置于 `public/assets/images`，运行时不依赖远程图片或在线字体
