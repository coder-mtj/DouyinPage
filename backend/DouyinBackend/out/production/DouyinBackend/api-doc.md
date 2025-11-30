# Douyin Backend API 文档

## 基本信息

- **Base URL（后端本机）**：`http://localhost:8080`
- **Base URL（Android 模拟器访问）**：`http://10.0.2.2:8080`
- 所有接口均返回 JSON，编码为 UTF-8。

当前模块只对外暴露与“我的关注（明星列表）”相关的接口。

---

## 1. 分页查询明星列表

- **URL**：`GET /api/stars`
- **说明**：
  - 从服务器分页获取“我的关注”列表数据。
  - 服务端固定每页返回 **10 条数据**，无论客户端传入的 `size` 为多少，都会在服务端被限制为 10。

### 请求参数

- **Query 参数**：
  - `page`：`int`，页码，从 **0** 开始。
  - `size`：`int`，每页条数；服务端会强制调整为 10，仅用于保持与前端 Retrofit 接口兼容。

### 返回结果

```json
{
  "content": [
    {
      "id": 1,
      "name": "周杰伦",
      "avatarIndex": 1,
      "verified": true,
      "followed": true,
      "douyinId": "JayChou001",
      "specialFollow": true,
      "remark": ""
    }
    // ... 共 10 条
  ],
  "total": 1200,
  "page": 0,
  "size": 10
}
```

### 字段说明

- **顶层字段**：
  - `content`：`StarUserDto[]`，当前页的明星列表。
  - `total`：`long`，所有明星的总数量（当前实现约 1200 条）。
  - `page`：`int`，当前页码（0 基）。
  - `size`：`int`，当前页大小（固定为 10）。

- **StarUserDto 字段**（与 Android 端 `StarUserDto` 一致）：
  - `id`：`long`，用户唯一 ID。
  - `name`：`string`，展示名称（如 “周杰伦 1”）。
  - `avatarIndex`：`int`，头像序号，范围 1~15；前端通过该索引映射到本地 `R.drawable.avatar_x` 资源。
  - `verified`：`boolean`，是否认证。
  - `followed`：`boolean`，是否已关注，一般为 `true`。
  - `douyinId`：`string`，抖音号（示例：`"star0001"`）。
  - `specialFollow`：`boolean`，是否特别关注。
  - `remark`：`string`，备注名称，可为空串。

### 典型调用示例

- 第一页：`GET /api/stars?page=0&size=10`
- 第二页：`GET /api/stars?page=1&size=10`

---

## 2. 更新单个明星的关注状态

- **URL**：`PATCH /api/stars/{id}`
- **说明**：
  - 更新指定明星的“特别关注 / 关注状态 / 备注”信息。
  - 该接口会修改 SQLite 中的记录，并清理分页缓存，下次分页查询会返回最新数据。

### 路径参数

- `id`：`long`，要更新的明星用户 ID。

### 请求体（JSON）

```json
{
  "specialFollow": true,
  "followed": true,
  "remark": "特别关注"
}
```

- 字段说明：
  - `specialFollow`：`boolean`，是否特别关注。
  - `followed`：`boolean`，是否仍然关注（前端点击“关注 / 已关注”按钮会触发修改）。
  - `remark`：`string`，备注文本，可为空字符串。

### 返回结果

- **状态码**：`204 No Content`
- **响应体**：无内容。

### 典型调用示例

```http
PATCH /api/stars/1
Content-Type: application/json

{
  "specialFollow": true,
  "followed": true,
  "remark": "特别关注"
}
```

---

## 3. 其他说明

- **分页与总数**：
  - 启动时如果数据库中没有数据，服务会自动生成约 1200 条 mock 明星数据插入 SQLite（基于 15 位明星扩展而来）。
  - `total` 字段即为数据库中 `star_user` 表的总记录数。
- **Redis 缓存**：
  - 若本机启动了 Redis，分页结果会按页缓存到 Redis 中，key 形如：`star_users:page:{page}:{size}`。
  - 如果 Redis 不可用，服务会自动降级，仅使用 SQLite，不影响接口返回。
