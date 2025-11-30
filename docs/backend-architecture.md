# DouyinBackend 后端架构与实现细节

> 对应前端 `ApiClient` 的 `http://localhost:8080/` 服务，主要为抖音关注页提供分页用户数据与状态更新。

---

## 1. 项目与技术栈概览

- **工程路径**：`backend/DouyinBackend/DouyinBackend`
- **构建工具**：Maven（`pom.xml`）
- **主要技术栈**：
  - Spring Boot 3（`spring-boot-starter-web`）
  - JDBC（`spring-boot-starter-jdbc`）
  - Redis（`spring-boot-starter-data-redis`）
  - Jackson（Spring 默认 JSON 序列化）
  - 另外引入了 `spring-ai-starter-vector-store-redis`，但当前这个关注列表场景里主要用到的是 Redis 作为缓存。
- **Java 版本**：17

### 1.1 启动类

- **`com.mtj.DouyinBackend.DouyinBackendApplication`**
  - 标准 Spring Boot 启动入口：
    - `@SpringBootApplication`
    - `public static void main(String[] args) { SpringApplication.run(DouyinBackendApplication.class, args); }`
  - 启动后会：
    - 创建 Web 容器（默认 8080 端口，与前端 `BASE_URL` 对齐）。
    - 扫描并加载 `star` 包下的 Controller / Service / Repository / 数据初始化组件。

---

## 2. 包结构与分层

主包：`com.mtj.DouyinBackend`

- **根包**
  - `DouyinBackendApplication`：启动类。
- **子包 `star`**：本项目核心业务全部集中在这里：
  - API 层：`StarUserController`
  - 业务层：`StarUserService`
  - 数据访问层：`StarUserRepository`
  - 实体模型：`StarUser`
  - DTO / 请求体 / 响应体：
    - `StarUserDto`
    - `StarUserPageResponse`
    - `UpdateStarUserRequestBody`
  - 启动数据初始化：`StarUserDataInitializer`

这一套基本就是典型的 **Controller → Service → Repository → DB** 分层结构，再加上一层 **DTO** 来与前端的数据模型对齐，同时有 **Redis 缓存** 在 Service 层做性能优化。

---

## 3. API 层：StarUserController

**类路径**：`star/StarUserController.java`

- 类注解：
  - `@RestController`
  - `@RequestMapping("/api/stars")`
- 注入：
  - 构造器注入 `StarUserService`。

### 3.1 分页查询接口：GET /api/stars

```http
GET /api/stars?page={page}&size={size}
```

- 方法签名：
  - `public StarUserPageResponse list(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "10") int size)`
- 行为：
  - 将请求参数转交给 `service.getPage(page, size)`。
  - 返回值是 `StarUserPageResponse`，由 Spring 自动序列化为 JSON 给前端。

> 注意：在 Service 层会固定 `pageSize = 10`，即使前端传了别的 `size`，最终仍然按 10 条一页处理。

### 3.2 更新用户接口：PATCH /api/stars/{id}

```http
PATCH /api/stars/{id}
Content-Type: application/json

{
  "specialFollow": true/false,
  "followed": true/false,
  "remark": "..."
}
```

- 方法签名：
  - `public ResponseEntity<Void> update(@PathVariable("id") long id, @RequestBody UpdateStarUserRequestBody body)`
- 行为：
  - 调用 `service.updateUser(id, body)` 执行更新逻辑。
  - 返回 `204 No Content`。

这两个接口 **刚好对应前端的 Retrofit 接口 `StarUserApi`**：
- 前端 `StarUserApi.list(page, size)` → 这里的 `GET /api/stars`
- 前端 `StarUserApi.updateUser(id, body)` → 这里的 `PATCH /api/stars/{id}`

---

## 4. 业务层：StarUserService

**类路径**：`star/StarUserService.java`

- 注解：`@Service`
- 依赖：
  - `StarUserRepository`：操作数据库。
  - `StringRedisTemplate`：操作 Redis 缓存分页结果。
  - `ObjectMapper`：手动把 `StarUserPageResponse` 序列化/反序列化成 JSON 存进 Redis。

### 4.1 分页查询核心逻辑：getPage(page, size)

核心步骤：

1. **参数校正**：
   - `page < 0` 时强制置为 0。
   - `size` 强制改为 `10`（作业要求）。

2. **构造缓存 Key**：
   - `cacheKey = "star_page:" + page + ":" + size`。

3. **尝试从 Redis 读取缓存**：
   - `StarUserPageResponse cached = readFromCache(cacheKey);`
   - 若非空，直接返回缓存结果。

4. **访问数据库**：
   - `long total = repository.countAll();`
   - 若 `total == 0`，返回空列表；否则：
     - `List<StarUser> users = repository.findPage(page, size);`

5. **转换为 DTO 并封装响应体**：
   - `StarUserPageResponse response = new StarUserPageResponse();`
   - 设置 `total/page/size`。
   - `response.setContent(mapToDto(users));`

6. **写入缓存**：
   - `writeToCache(cacheKey, response);`

7. 返回 `response`。

#### 4.1.1 Redis 缓存实现

- `readFromCache(key)`：
  - 若 `redisTemplate == null` 直接返回 null（便于在不配置 Redis 的情况下也能正常跑）。
  - 使用 `opsForValue().get(key)` 读字符串。
  - 使用 `objectMapper.readValue(value, StarUserPageResponse.class)` 反序列化。
  - 捕获：
    - `RedisConnectionFailureException`
    - `DataAccessException`
    - `JsonProcessingException`
  - 发生异常时返回 null，相当于 **静默降级为无缓存模式**。

- `writeToCache(key, value)`：
  - 同样先判空 `redisTemplate`。
  - `objectMapper.writeValueAsString(value)` 序列化为 JSON，再 `opsForValue().set(key, json)` 写入。
  - 捕获同样的异常并忽略（保证业务不因缓存失败而中断）。

#### 4.1.2 DTO 映射：mapToDto(List<StarUser>)

- 遍历每个 `StarUser`：
  - 构造新的 `StarUserDto`：
    - `id/name/avatarIndex/verified/followed/douyinId/specialFollow/remark` 一一对应。
- 返回 `List<StarUserDto>`，供 Controller 返回给前端。

### 4.2 更新用户逻辑：updateUser(id, body)

- 调用仓储层：
  - `repository.updateUser(id, body.getSpecialFollow(), body.getFollowed(), body.getRemark());`
- 缓存处理：
  - `evictPageCache();`
  - 实现：从 Redis 中找到所有匹配 `"star_page:*"` 的 Key 并删除。
  - 若 Redis 不可用或出错，异常被吞掉，不影响主流程。

### 4.3 初始化数据：initDataIfNeeded()

- 用于项目启动时注入初始数据，避免数据库空表：
  - 查询当前总数：`count = repository.countAll();`
  - 若 `count >= 1000`，认为数据足够，直接返回。
  - 否则：
    - 调 `generateMockUsers(1000)` 生成 1000 条「明星用户」数据。
    - `repository.insertBatch(users);`
    - 调用 `evictPageCache()` 清除缓存。

#### 4.3.1 数据生成规则：generateMockUsers(count)

- 基础名字列表：周杰伦、王心凌、张韶涵、张靓颖、五月天、林俊杰、薛之谦、刘德华、杨幂、赵丽颖、邓紫棋、陈奕迅、易烊千玺、王俊凯、蔡依林。
- 循环生成：
  - `id = i + 1`。
  - `name = baseName + " " + round`（模拟同一明星多次出现）。
  - `avatarIndex`：循环取 1~15。
  - `verified`：`(i % 3) != 0`（大部分是已认证）。
  - `followed`：`true`。
  - `douyinId`：`"star" + String.format("%04d", id)`。
  - `specialFollow`：`(i % 10) == 0`（每 10 个中有 1 个特别关注）。
  - `remark`：空串。

### 4.4 启动时自动初始化：StarUserDataInitializer

- 类注解：
  - `@Component`
  - `@Order(1)`
  - 实现 `CommandLineRunner`。
- 在 `run(String... args)` 中调用：
  - `service.initDataIfNeeded();`
- 这意味着：**每次后端启动时，会自动检查数据库记录数，不足 1000 条就自动塞满**，确保前端分页有数据可看。

---

## 5. 数据访问层：StarUserRepository

**类路径**：`star/StarUserRepository.java`

- 注解：`@Repository`
- 依赖：
  - `JdbcTemplate`：Spring 的 JDBC 封装，用来执行 SQL。
- 内部：
  - 定义了一个 `RowMapper<StarUser>` 将结果集映射到 `StarUser`。

### 5.1 RowMapper 映射规则

从 SQL 查询结果中读取字段：
- `id` → `user.setId(rs.getLong("id"));`
- `name` → `setName(rs.getString("name"));`
- `avatar_index` → `setAvatarIndex(rs.getInt("avatar_index"));`
- `verified` → 整型转 Boolean：`rs.getInt("verified") != 0;`
- `followed` → 同上。
- `douyin_id` → `setDouyinId(rs.getString("douyin_id"));`
- `special_follow` → 整型转 Boolean；
- `remark` → 文本备注。

### 5.2 核心方法

- **统计总数**：`countAll()`
  - SQL：`SELECT COUNT(*) FROM star_user`。
  - 返回 `long`，空结果时返回 `0L`。

- **分页查询**：`findPage(int page, int size)`
  - 计算偏移量：`offset = page * size`。
  - SQL：
    - `SELECT id, name, avatar_index, verified, followed, douyin_id, special_follow, remark FROM star_user ORDER BY id LIMIT ? OFFSET ?`。
  - 使用前面定义的 `rowMapper` 映射结果到 `List<StarUser>`。

- **批量插入**：`insertBatch(List<StarUser> users)`
  - SQL：
    - `INSERT INTO star_user (id, name, avatar_index, verified, followed, douyin_id, special_follow, remark) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`。
  - 使用 `jdbcTemplate.batchUpdate` 批量写入，提高初始化效率。

- **更新单个用户**：`updateUser(long id, Boolean specialFollow, Boolean followed, String remark)`
  - SQL：
    - `UPDATE star_user SET special_follow = ?, followed = ?, remark = ? WHERE id = ?`。
  - 将 Boolean 转为 0/1 存储。

> 可见，这里没有使用 JPA，而是直接用 **JdbcTemplate + 手写 SQL**，结构非常直接清晰，便于教学和理解。

---

## 6. 模型与 DTO

### 6.1 实体模型：StarUser

- 用于 **数据库层内部使用**，字段：
  - `id: Long`
  - `name: String`
  - `avatarIndex: Integer`
  - `verified: Boolean`
  - `followed: Boolean`
  - `douyinId: String`
  - `specialFollow: Boolean`
  - `remark: String`
- Service 层从 `StarUser` 映射到 `StarUserDto`，再返回给前端。

### 6.2 返回 DTO：StarUserDto

- 字段和前端 `User` 模型基本对应，只是头像使用 `avatarIndex`：
  - `id`
  - `name`
  - `avatarIndex`（前端再映射到 `R.drawable.avatar_x`）
  - `verified`
  - `followed`
  - `douyinId`
  - `specialFollow`
  - `remark`

### 6.3 分页响应体：StarUserPageResponse

- 字段：
  - `content: List<StarUserDto>` 当前页数据。
  - `total: long` 总记录数。
  - `page: int` 当前页号。
  - `size: int` 当前页大小（被固定为 10）。

### 6.4 更新请求体：UpdateStarUserRequestBody

- 字段：
  - `specialFollow: Boolean`
  - `followed: Boolean`
  - `remark: String`
- 与前端传的 JSON 一一对应，直接交给 Service → Repository 更新数据库。

---

## 7. 前后端整体链路（从 App 到 DB 再到缓存）

以「前端关注页加载第 N 页」为例：

1. **前端 `FollowingFragment` 调用 Retrofit**：
   - `starUserApi.list(page, PAGE_SIZE)`（始终以 10 条为一页）。

2. **请求到达后端 Controller**：
   - `StarUserController.list(page, size)` 收到 `GET /api/stars?page=...&size=...`。
   - 调用 `StarUserService.getPage(page, size)`。

3. **Service 尝试读取 Redis 缓存**：
   - 构造 `cacheKey = "star_page:" + page + ":" + size`。
   - 若 Redis 有值，则直接反序列化返回，**不会访问数据库**。

4. **若缓存未命中**：
   - `StarUserRepository.countAll()` 查询总数。
   - `StarUserRepository.findPage(page, size)` 用 `LIMIT & OFFSET` 查询当前页。
   - Service 把 `StarUser` 列表转成 `StarUserDto` 列表，封装进 `StarUserPageResponse`。
   - 写入 Redis 缓存。

5. **Controller 返回 JSON**：
   - Spring Boot 自动把 `StarUserPageResponse` 转成 JSON 返回给客户端。

6. **前端接收并展示**：
   - 前端将 `StarUserDto` → 本地 `User` 模型，映射 `avatarIndex` 到对应 `R.drawable.avatar_x`。
   - RecyclerView 列表刷新 UI。

类似地，「前端修改关注状态或特别关注 / 备注」的完整链路是：

1. 前端点击按钮或在 BottomSheet 修改 → 调用 `StarUserApi.updateUser(id, body)` 发 PATCH 请求。
2. 后端 `StarUserController.update(id, body)` → `StarUserService.updateUser(id, body)`。
3. Service 通过 Repository 执行 `UPDATE star_user SET ... WHERE id = ?`。
4. 成功后 `evictPageCache()` 清除 Redis 中所有分页缓存 Key（保证下次查询拿到最新数据）。
5. 前端本地同时已更新 UI 和本地缓存，下一次重新加载时与后端保持一致。

---

## 8. 小结

- 后端项目是一个 **结构非常干净的 Spring Boot + JDBC + Redis** 服务：
  - 一条典型的 **Controller → Service → Repository → DB** 调用链。
  - 使用 DTO + 响应体与前端解耦，字段设计完全贴合前端 `User` 模型。
  - 使用 Redis 做分页结果缓存，通过更新操作时统一清空相关缓存，保证数据新鲜度。
  - 启动时自动初始化 1000 条模拟数据，前端一接就有内容可展示。
- 这个文档描述的是后端的「架构 + 实现细节」，和前端的 `client-architecture.md` / `client-details.md` 一起看，可以完整理解 **从 App UI 到数据库与缓存** 的全链路。
