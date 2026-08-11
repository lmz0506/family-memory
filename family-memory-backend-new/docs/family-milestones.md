# 家庭大事记提醒 API

基础路径为 `/api/v1`，所有接口都需要登录。

## 可见范围

- `PRIVATE`：仅创建者可见，也是新增时的默认值。
- `FAMILY`：当前家庭的所有成员可见。
- `SPECIFIC`：创建者与 `viewerUserIds` 中指定的家庭成员可见。

指定人员必须属于当前家庭。家庭管理员不会自动获得其他成员私密提醒的访问权限，提醒也只有创建者可以修改或删除。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/families/{familyId}/milestones` | 获取当前用户有权查看的提醒 |
| POST | `/families/{familyId}/milestones` | 新增提醒 |
| PUT | `/milestones/{milestoneId}` | 创建者修改提醒 |
| DELETE | `/milestones/{milestoneId}` | 创建者删除提醒 |

新增或修改请求示例：

```json
{
  "title": "重要日子",
  "description": "提前准备",
  "eventDate": "2026-09-20",
  "recurringYearly": true,
  "visibilityScope": "SPECIFIC",
  "viewerUserIds": [2, 3]
}
```

已有数据库需执行 `database/migrations/20260810_family_milestone.sql`，全新数据库可直接使用 `database/schema.sql`。
