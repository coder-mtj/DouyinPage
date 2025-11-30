# 客户端 App 与后端服务整体架构与数据流

> 这一篇是把前面三份文档串起来看：
> - 客户端：`client-architecture.md` / `client-details.md`
> - 后端：`backend-architecture.md`
> 这里重点讲 **前后端是如何配合** 的，以及 **数据在两边之间怎么流动**。

---

## 1. 整体架构概览

### 1.1 分层视图

从上到下，可以分成四层：

1. **UI 层（Android 客户端）**  
   - Activity / Fragment / RecyclerView / BottomSheet。  
   - 典型类：`MainActivity`、`FollowingActivity`、`FollowingFragment`、`MoreOptionsBottomSheet`、`FollowingAdapter`。

2. **客户端数据与网络层**  
   - 本地模型 & 本地缓存 & 网络访问。  
   - 模型：`User`（app 内部使用）。  
   - 本地缓存：`UserStorage`（SharedPreferences + JSON）。  
   - 网络：`ApiClient`（Retrofit + OkHttp） + `StarUserApi`（Retrofit 接口）。

3. **后端服务层（Spring Boot）**  
   - Web API + 业务逻辑 + 缓存 + 数据访问。  
   - 控制器：`StarUserController`。  
   - 业务：`StarUserService`（分页、缓存、初始化）。  
   - 缓存：Redis（`StringRedisTemplate`）。

4. **数据存储层**  
   - 数据库（`star_user` 表） + Redis。  
   - 数据访问：`StarUserRepository`（JdbcTemplate + SQL）。  
   - 实体：`StarUser`。

### 1.2 通信协议与地址

- **协议**：HTTP + JSON。
- **前端 Base URL**：
  - `ApiClient` 中：`BASE_URL = "http://localhost:8080/"`。
- **主要后端接口**：
  - `GET  /api/stars`：分页查询关注列表。  
  - `PATCH /api/stars/{id}`：更新单个用户（特别关注 / 关注状态 / 备注）。

客户端通过 Retrofit 把 Java 接口 `StarUserApi` 映射成 HTTP 请求，后端由 Spring Boot Controller 接收并处理。

---

## 2. 关键数据模型的对应关系

### 2.1 客户端模型：User

- 字段：
  - `id: int`
  - `name: String`
  - `avatar: int`（drawable 资源 id）
  - `isVerified: boolean`
  - `isFollowed: boolean`
  - `douyinId: String`
  - `isSpecialFollow: boolean`
  - `remark: String`

### 2.2 后端实体与 DTO

- **数据库 / 后端内部实体：`StarUser`**
  - 字段：`id, name, avatarIndex, verified, followed, douyinId, specialFollow, remark`。
- **返回给客户端的 DTO：`StarUserDto`**
  - 字段与 `StarUser` 一致，只是类型都为 `Long/Integer/Boolean/String`。
- **分页响应体：`StarUserPageResponse`**
  - `content: List<StarUserDto>`
  - `total: long` 总记录数。
  - `page: int` 当前页号。
  - `size: int` 页面大小（强制 10）。

### 2.3 更新请求体：UpdateStarUserRequestBody

- 前端发送 PATCH 请求的 body 对应：
  - `specialFollow: Boolean`
  - `followed: Boolean`
  - `remark: String`

### 2.4 DTO ↔ User 的映射

在客户端的 `FollowingFragment` 中：

- 从后端得到 `StarUserDto` 后，调用 `mapRemoteUsers(List<StarUserDto>)`：
  - 使用 `dto.getAvatarIndex()` 通过 `getAvatarResId()` 映射到对应的 `R.drawable.avatar_X`。  
  - 其他字段直接复制：`id/name/verified/followed/douyinId/specialFollow/remark`。  
  - 生成本地 `User` 列表供 RecyclerView 渲染。

---

## 3. 数据流一：关注列表分页加载

### 3.1 时序总览

从用户打开 App，到看到关注列表的一整条链路：

1. **系统启动 App**
   - 启动 `MainActivity`。
   - `MainActivity` 立即 `startActivity(FollowingActivity)` 并 `finish()` 自己。

2. **进入 FollowingActivity**
   - 使用 `ActivityFollowingBinding` 加载 `activity_following.xml`。  
   - 设置 `TabLayout` + `ViewPager2` + `FollowingPagerAdapter`。  
   - 默认选中「关注」tab，对应 `FollowingFragment`。

3. **FollowingFragment 初始化**
   - `onCreateView`：inflate `fragment_following.xml`，绑定 `binding`。  
   - `onViewCreated`：调用 `setupRecyclerView()` 和 `setupFragmentResultListener()`。

4. **RecyclerView 与 Adapter 安装**
   - `setupRecyclerView()` 中：
     - 初始化 `users = new ArrayList<>()`。
     - 创建 `FollowingAdapter`，传入：
       - 用户列表 `users`。
       - `OnMoreClickListener`（点击三点打开 BottomSheet）。
       - `OnUserChangedListener`（本地关注状态修改时回调，用于保存 + 调后端）。
     - 设置布局管理器为 `LinearLayoutManager`（纵向列表）。
     - 设置滚动监听，在列表滑到底部时触发 `loadNextPage()`。
     - 最后调用 `loadUsersFromBackendOrLocal()` 启动第一次加载。

5. **前端通过 Retrofit 发起 HTTP 请求**
   - `loadUsersFromBackendOrLocal()`：
     - 确保 `starUserApi = ApiClient.getStarUserApi()`。  
     - 重置分页状态：`currentPage=-1, hasMore=true, totalCount=0`。  
     - 调用 `loadPage(0, true)`。
   - `loadPage(0, true)`：
     - 构造 `Call<StarUserPageResponse> call = starUserApi.list(page, PAGE_SIZE);`
     - `call.enqueue(new Callback<>() { ... })` 发送异步 HTTP GET：
       - `GET http://localhost:8080/api/stars?page=0&size=10`

6. **后端 Controller 接收请求**
   - `StarUserController.list(page, size)` 被调用：
     - `@GetMapping` + `@RequestParam` 解析 URL 参数。  
     - 调 `StarUserService.getPage(page, size)`。

7. **Service 层处理分页与缓存**
   - `getPage(page, size)`：
     1. **规范参数**：`page < 0` → 0，`size` 强制为 10。  
     2. **尝试从 Redis 读取缓存**：
        - key：`"star_page:" + page + ":" + size`。  
        - 有值则反序列化成 `StarUserPageResponse` 直接返回。  
     3. **未命中缓存 → 访问数据库**：
        - `total = repository.countAll();`  
        - `users = repository.findPage(page, size);`  
     4. **实体转 DTO**：
        - `mapToDto(users)` 生成 `List<StarUserDto>`。  
     5. **封装响应体 + 写缓存**：
        - 构造 `StarUserPageResponse` 设置 `total/page/size/content`。  
        - `writeToCache(cacheKey, response)` 把 JSON 写入 Redis。  
     6. 返回 `response` 给 Controller。

8. **数据库层执行 SQL**
   - `StarUserRepository.countAll()`：
     - `SELECT COUNT(*) FROM star_user`。
   - `StarUserRepository.findPage(page, size)`：
     - `SELECT ... FROM star_user ORDER BY id LIMIT ? OFFSET ?`。
   - `RowMapper` 把每一行映射为 `StarUser` 实体。

9. **响应返回到前端**
   - HTTP 响应 JSON：
     - 包含 `content`（数组）、`total`、`page`、`size`。
   - Retrofit + Gson 自动把它转换为 `StarUserPageResponse` 对象。

10. **前端映射并刷新 UI**
    - `FollowingFragment.onResponse(...)`：
      - `List<StarUserDto> dtos = body.getContent();`  
      - `List<User> mapped = mapRemoteUsers(dtos);`  
      - 如果 `clearBefore == true`，`users.clear()` 再 `users.addAll(mapped)`。  
      - 通知 Adapter 刷新：`notifyDataSetChanged()` 或 `notifyItemRangeInserted(...)`。  
      - 更新 `totalCount / currentPage / hasMore`。  
      - `persistUsers()` 把 `users` 保存到 `UserStorage`。  
      - `updateFollowingCount()` 设置「我的关注(x人)」。

此时，用户在客户端就能看到从数据库 + Redis 经过后端聚合后返回来的关注列表。

---

## 4. 数据流二：用户状态更新（关注 / 特别关注 / 备注）

主要有两种触发：
- 点击列表行上的 **关注按钮**（在 `FollowingAdapter` 里）。
- 在 **MoreOptionsBottomSheet** 中改特别关注 / 备注 / 取消关注。

### 4.1 列表中点击关注按钮

1. 用户点击某一项的 `followButton`。

2. 在 `FollowingAdapter.ViewHolder.bind(User user)` 中：
   - 本地切换状态：`user.setFollowed(!user.isFollowed());`。  
   - 更新按钮 UI：文字 & 背景颜色。  
   - 若 `onUserChanged` 回调不为空：`onUserChanged.onUserChanged(user);`。

3. 回调传回 `FollowingFragment`：
   - 在创建 Adapter 的时候，Fragment 传入了一个 `OnUserChangedListener` 实现：
     - `persistUsers();` → `UserStorage.saveUsers(context, users);` 把整个列表写到本地 SharedPreferences。  
     - `updateUserOnBackend(user);`：
       - 构造 `UpdateStarUserRequestBody`，填入 `specialFollow/followed/remark`。  
       - 调用 `starUserApi.updateUser(user.getId(), body)` 发起 PATCH 请求：
         - `PATCH http://localhost:8080/api/stars/{id}`。

4. 后端 Controller 处理 PATCH：
   - `StarUserController.update(id, body)`：
     - 将 JSON 反序列化为 `UpdateStarUserRequestBody`。  
     - 调用 `StarUserService.updateUser(id, body)`。

5. Service 更新 DB + 清理缓存：
   - `repository.updateUser(id, body.getSpecialFollow(), body.getFollowed(), body.getRemark())`：
     - 执行 `UPDATE star_user SET special_follow = ?, followed = ?, remark = ? WHERE id = ?`。  
   - `evictPageCache()`：
     - 用 Redis `keys("star_page:*")` 找到所有分页缓存 key。  
     - 统一删除，确保下一次分页查询都是最新数据。

6. Controller 返回 `204 No Content`。

7. 客户端此时已经：
   - 本地 `User` 状态已更新，并写入 `UserStorage`。  
   - UI 立即生效。  
   - 下一次重新加载列表时，会拿到与数据库 / Redis 一致的结果。

### 4.2 在 BottomSheet 中修改特别关注/备注/取消关注

1. 用户点击列表项右侧三点按钮 `moreButton`。

2. 在 `FollowingAdapter` 中回调 `onMoreClick(user)`，`FollowingFragment` 的回调实现：
   - `showMoreOptionsBottomSheet(user)`：
     - `MoreOptionsBottomSheet bottomSheet = MoreOptionsBottomSheet.newInstance(user);`
     - `bottomSheet.show(getParentFragmentManager(), "MoreOptions");`

3. BottomSheet 中展示并修改状态：
   - 初始状态从传入的 `User` 恢复：`isSpecialFollow/isFollowed/remark`。  
   - 用户操作：
     - 切换特别关注开关。  
     - 点击备注区域弹框修改备注。  
     - 点击「取消关注」把 `isFollowed` 设为 false。  
     - 点击关闭按钮。
   - 每次修改后调用 `emitResult(...)`：
     - 构造 Bundle，填入 `RESULT_USER_ID / RESULT_SPECIAL / RESULT_FOLLOWED / RESULT_REMARK`。  
     - 通过 `FragmentManager.setFragmentResult(RESULT_KEY, bundle)` 回传给 `FollowingFragment`。

4. `FollowingFragment` 接收 FragmentResult：
   - 在 `setupFragmentResultListener()` 中注册监听：
     - 按 `userId` 在 `users` 列表里找到对应的 `User`。  
     - 从 Bundle 中读取最新的 `specialFollow/followed/remark`，写回该 `User`。  
     - `adapter.notifyItemChanged(i);` 刷新这一行视图。  
     - `persistUsers();` 保存到 `UserStorage`。  
     - `updateUserOnBackend(target);` 再走一遍与 4.1 相同的 PATCH 流程。

5. 后端的处理与 4.1 完全一样：
   - 更新数据库 → 清理 Redis 分页缓存 → 返回 204。

---

## 5. 本地缓存（UserStorage）与服务端数据的一致性

### 5.1 本地缓存的作用

- **目的**：
  - 减少每次打开页面都完全依赖网络的体验问题。  
  - 虽然当前实现中，网络失败时并没有自动回退到本地缓存，但已经有 `UserStorage` 方便你后续扩展离线/弱网逻辑。

- **当前用法**：
  - 每次用户状态有变更时（列表点击关注 / BottomSheet 修改）：
    - `FollowingFragment.persistUsers()` 会把当前 `users` 列表写入 `SharedPreferences`。

### 5.2 一致性策略（当前实现）

- **来源**：
  - 列表初次加载：**直接以服务端为准**，覆盖本地 `users` 并保存一份最新快照。  
  - 用户交互修改：
    - 先本地更新 + 写入本地缓存。  
    - 然后 PATCH 请求同步到后端。  
    - 后端更新 DB 同时清理 Redis 缓存，下一次再 GET 就是新数据。
- 如果想更强一致：
  - 可以在 PATCH 成功回调中再触发一次当前页 `loadPage()`，保证当前页面立即刷新为后端最终状态。

---

## 6. 小结：一张脑图式理解

- **客户端**：
  - `FollowingActivity`：壳子 + Tab。  
  - `FollowingFragment`：列表页，负责调用 `StarUserApi` + 管理 `User` 列表 + 挥动 RecyclerView。  
  - `MoreOptionsBottomSheet`：修改一条数据的更多属性。  
  - 本地缓存：`UserStorage`。

- **网络层**：
  - `ApiClient` + `StarUserApi`：把 Java 调用转换成 HTTP 请求（GET / PATCH）。

- **后端**：
  - `StarUserController`：收/发 HTTP 请求与响应。  
  - `StarUserService`：分页、初始化、缓存、更新逻辑的核心。  
  - `StarUserRepository`：用 SQL 操作 `star_user` 表。  
  - Redis：缓存分页结果，加速 GET /api/stars。

- **数据流**：
  - **查询**：前端 Fragment → Retrofit → 后端 Controller → Service（Redis 缓存 + DB） → DTO/Response → 前端 `User` 列表 → RecyclerView。
  - **更新**：前端交互（按钮/BottomSheet）→ 本地 `User` 更新 + `UserStorage` → Retrofit PATCH → 后端 Controller → Service → Repository → DB → 清除 Redis 缓存 → 下次查询拿到新数据。

这份文档配合前面三篇，可以帮助你完整掌握整个 DouyinPage 项目的「前端、后端、缓存、数据库」联动关系。
