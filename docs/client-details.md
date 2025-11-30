# DouyinPage 客户端实现细节

> 重点讲：页面如何启动、如何跳转、如何获取后端服务、如何刷新 / 分页加载，以及 BottomSheet 状态回传。

---

## 1. 应用启动流程（MainActivity）

### 1.1 启动入口

- Android 系统根据 `AndroidManifest.xml` 中配置的 `LAUNCHER` Activity 启动应用，一般就是 `MainActivity`。
- `MainActivity` 代码（简化逻辑）：
  - `onCreate()` 中：
    - `setContentView(R.layout.activity_main);`
    - `startActivity(new Intent(this, FollowingActivity.class));`
    - `finish();`

### 1.2 关键点说明

- **占位 Activity**：
  - `MainActivity` 本身不承担业务 UI，只负责把用户引导到核心页面 `FollowingActivity`。
- **返回栈行为**：
  - 调用 `finish()` 后，用户从 `FollowingActivity` 点返回键时不会再回到 `MainActivity`，而是直接退出应用或回到桌面。

---

## 2. 页面跳转与 Fragment 导航

### 2.1 Activity 之间的跳转

- 从 `MainActivity` → `FollowingActivity`：
  - 使用 `Intent`：`startActivity(new Intent(this, FollowingActivity.class));`
- 如果后续需要从其他 Activity 再跳回来，也都是通过 `Intent` + `startActivity` 的方式。

### 2.2 FollowingActivity 内部的 Tab 导航

- `FollowingActivity` 使用 `ViewPager2 + TabLayout` 实现四个 Tab：
  - 创建 `FollowingPagerAdapter adapter = new FollowingPagerAdapter(this);`
  - `binding.viewPager.setAdapter(adapter);`
- 使用 `TabLayoutMediator` 绑定 Tab 与页面标题：
  - `position 0` → "互关"
  - `position 1` → "关注"（默认选中）
  - `position 2` → "粉丝"
  - `position 3` → "朋友"
- 设置默认页：`binding.viewPager.setCurrentItem(1);`

### 2.3 Fragment 的创建与切换

- `FollowingPagerAdapter.createFragment(position)` 决定每个 Tab 对应的 Fragment：
  - `0`：`EmptyFragment.newInstance("暂无互关")`
  - `1`：`new FollowingFragment()`（真正的关注列表）
  - `2`：`EmptyFragment.newInstance("暂无粉丝")`
  - `3`：`EmptyFragment.newInstance("暂无朋友")`
- 用户左右滑动或点击 Tab 时，`ViewPager2` 切换当前 Fragment，`FragmentStateAdapter` 负责 Fragment 的生命周期管理和复用。

---

## 3. 网络层：如何获取后端服务

### 3.1 Retrofit 客户端（ApiClient）

- `ApiClient` 负责统一创建 Retrofit + OkHttp 实例：
  - `BASE_URL = "http://localhost:8080/"`。
  - `createClient()`：
    - 创建 `OkHttpClient`，添加 `HttpLoggingInterceptor`，日志级别为 `BASIC`。
  - `createRetrofit()`：
    - `new Retrofit.Builder()`
      - `.baseUrl(BASE_URL)`
      - `.client(createClient())`
      - `.addConverterFactory(GsonConverterFactory.create())`（使用 Gson 解析 JSON）。
  - `getStarUserApi()`：
    - 双重检查锁 (`double-checked locking`) 创建单例 `StarUserApi` 实例。

### 3.2 网络接口定义（StarUserApi）

- 使用 Retrofit 注解声明 HTTP 接口：
  - `@GET("api/stars")`
    - `Call<StarUserPageResponse> list(@Query("page") int page, @Query("size") int size);`
    - 作用：分页获取明星关注列表，后端返回 JSON，被解析为 `StarUserPageResponse`。
  - `@PATCH("api/stars/{id}")`
    - `Call<Void> updateUser(@Path("id") int id, @Body UpdateStarUserRequestBody body);`
    - 作用：更新指定用户（是否关注、是否特别关注、备注）。

### 3.3 在页面中如何拿到 API 实例

- 在 `FollowingFragment` 中：
  - 成员字段：`private StarUserApi starUserApi;`
  - 初始化方式：
    - 在首次加载时：
      - 如果 `starUserApi == null`，就调用 `ApiClient.getStarUserApi()`。

总体流程：**UI → 调用 `ApiClient.getStarUserApi()` → 获得 Retrofit 创建的 `StarUserApi` 实例 → 通过接口方法发起 HTTP 请求**。

---

## 4. 关注列表的加载、分页与刷新

### 4.1 初始加载：loadUsersFromBackendOrLocal()

- 在 `FollowingFragment.setupRecyclerView()` 的末尾调用：
  - `loadUsersFromBackendOrLocal();`
- 逻辑：
  - 确保 `starUserApi` 已初始化。
  - 重置分页相关字段：
    - `currentPage = -1;`
    - `hasMore = true;`
    - `totalCount = 0L;`
  - 调用 `loadPage(0, true);` 加载第 0 页数据（并在成功时清空旧数据）。

### 4.2 分页加载：滚动监听 + loadNextPage()

- 在 RecyclerView 上添加 `OnScrollListener`：
  - 如果：
    - `dy > 0`（向下滚动）
    - `!recyclerView.canScrollVertically(1)`（不能再向下滚动了，到了底部）
    - `hasMore && !isLoading`
  - 则调用 `loadNextPage();`
- `loadNextPage()` 计算下一页下标：
  - `int nextPage = currentPage + 1;`
  - 调用 `loadPage(nextPage, false);`

### 4.3 核心加载逻辑：loadPage(page, clearBefore)

- 确保 `starUserApi` 不为空。
- 设置 `isLoading = true;` 防止重复加载。
- `Call<StarUserPageResponse> call = starUserApi.list(page, PAGE_SIZE);`
- `call.enqueue(new Callback<StarUserPageResponse>() { ... })` 异步发送请求。

#### onResponse 成功回调

- 判空：`response.isSuccessful() && response.body() != null`。
- 从 `body` 中取：
  - `List<StarUserDto> dtos = body.getContent();`
  - 把后台 DTO 转成本地 `User` 模型：`List<User> mapped = mapRemoteUsers(dtos);`
- 更新本地列表：
  - 如 `clearBefore == true`：
    - 清空 `users` 列表。
    - `users.addAll(mapped);`
    - `adapter.notifyDataSetChanged();`
  - 否则增量添加：
    - 记录旧大小 `oldSize = users.size();`
    - `users.addAll(mapped);`
    - 使用 `notifyItemRangeInserted(oldSize, mapped.size());` 提升刷新性能。
- 更新分页状态：
  - `totalCount = body.getTotal();`
  - `currentPage = page;`
  - `hasMore = !mapped.isEmpty() && (long) (currentPage + 1) * PAGE_SIZE < totalCount;`
- 持久化与 UI 更新：
  - `persistUsers();` → 调用 `UserStorage.saveUsers()` 保存本地。
  - `updateFollowingCount();` → 设置顶部「我的关注(x人)」。
- 最后记得：`isLoading = false;`

#### onFailure 失败回调

- 打日志：`Log.e(TAG, "loadUsersFromBackendOrLocal: failed", t);`
- 弹 Toast：`Toast.makeText(requireContext(), "网络异常，无法加载关注列表", Toast.LENGTH_SHORT).show();`
- `isLoading = false;`

> 当前实现：网络失败时只是提示错误，并**没有自动回退到 `UserStorage` 的本地缓存**，如需离线体验，可以在失败分支中手动调用 `UserStorage.loadUsers()` 并更新列表。

### 4.4 DTO → User 的映射：mapRemoteUsers()

- 遍历 `StarUserDto` 列表：
  - 过滤掉 `null` 和 `id == null` 的条目。
- 头像资源：
  - 根据 `dto.getAvatarIndex()`，通过 `getAvatarResId()` 映射到 `R.drawable.avatar_x`。
- 创建 `User`：
  - `id`、`name`、`avatarResId`、`verified`、`followed`、`douyinId`、`specialFollow`、`remark`。
- 返回 `List<User>`，直接附加到 `users` 列表中。

---

## 5. 本地缓存：UserStorage

### 5.1 保存用户列表：saveUsers()

- 输入：`Context context, List<User> users`。
- 步骤：
  1. 构造 `JSONArray array = new JSONArray();`
  2. 遍历每个 `User`：
     - 用 `JSONObject obj = new JSONObject();` 填充各字段（`id/name/avatar/verified/followed/douyinId/special/remark`）。
     - `array.put(obj);`
  3. 获取 `SharedPreferences`：`context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)`。
  4. `.edit().putString(KEY_USERS, array.toString()).apply();` 异步提交。

### 5.2 读取用户列表：loadUsers()

- 从 `SharedPreferences` 取出 JSON 字符串，如果为空直接返回空列表。
- 否则：
  - 构造 `JSONArray array = new JSONArray(json);`
  - 遍历下标 0..length-1：
    - `JSONObject obj = array.getJSONObject(i);`
    - 使用 `optXxx` 系列方法安全读取各字段，构造 `new User(...)`，加入列表。
- 返回 `List<User>`。

### 5.3 清空缓存：clear()

- `edit().remove(KEY_USERS).apply();`。

### 5.4 在页面中的使用

- 在 `FollowingFragment`：
  - 每次用户状态更新（点击关注按钮 / 在 BottomSheet 改备注 / 取消关注）后：
    - 调用 `persistUsers();` → `UserStorage.saveUsers(requireContext(), users);`
- 如需离线读取，可以在网络失败时增加调用 `UserStorage.loadUsers()` 恢复本地数据。

---

## 6. BottomSheet 交互与状态回传

### 6.1 打开 BottomSheet：FollowingFragment → MoreOptionsBottomSheet

- 在 `FollowingFragment.setupRecyclerView()` 中创建 `FollowingAdapter` 时：
  - 传入 `OnMoreClickListener`：
    - 当用户点击单行的 `moreButton` 时，回调 `onMoreClick(user)`。
- 回调实现：
  - 调用 `showMoreOptionsBottomSheet(user);`
  - 内部：
    - `MoreOptionsBottomSheet bottomSheet = MoreOptionsBottomSheet.newInstance(user);`
    - `bottomSheet.show(getParentFragmentManager(), "MoreOptions");`

### 6.2 在 BottomSheet 内部处理 UI 与状态

- 通过 `newInstance(User user)` 把 `User` 序列化后放进 `Bundle` 作为 `ARG_USER`。
- 在 `onViewCreated()` 中：
  - 从 `getArguments()` 里还原出 `User user`。
  - 初始化：
    - `isSpecialFollow = user.isSpecialFollow();`
    - `isFollowed = user.isFollowed();`
    - `remark = user.getRemark();`
- 设置各个控件：
  - `setupUserInfo()`：展示用户名、抖音号、备注，处理「复制抖音号」按钮。
  - `setupSpecialFollowSection()`：
    - `SwitchMaterial` 根据 `isSpecialFollow` 设置选中状态。
    - 切换时更新 `isSpecialFollow` 并调用 `emitResult(false)` 通知外部。
  - `setupRemarkSection()`：
    - 点击备注区域弹出 `AlertDialog` + `EditText`，保存后更新 `remark` 和界面，再调用 `emitResult(false)`。
  - `setupCancelFollow()`：
    - 点击「取消关注」时：`isFollowed = false; emitResult(true);`。
  - `setupCloseButton()`：
    - 点击关闭时 `emitResult(true);`

### 6.3 通过 FragmentResult 回传数据

- `emitResult(boolean dismissAfter)`：
  - 构造 `Bundle bundle`：
    - `RESULT_USER_ID`：用户 id。
    - `RESULT_SPECIAL`：是否特别关注。
    - `RESULT_FOLLOWED`：是否关注。
    - `RESULT_REMARK`：备注。
  - `getParentFragmentManager().setFragmentResult(RESULT_KEY, bundle);`
  - 如果 `dismissAfter == true`，则 `dismiss()` 关闭 BottomSheet。
- 在 `onDismiss()` 中也会调用一次 `emitResult(false)`，确保修改不会丢失。

### 6.4 FollowingFragment 接收并更新列表

- 在 `setupFragmentResultListener()` 中注册监听：
  - `getParentFragmentManager().setFragmentResultListener(MoreOptionsBottomSheet.RESULT_KEY, this, (requestKey, result) -> { ... })`
- 回调中：
  1. 获取 `userId`，遍历 `users` 找到对应的 `User`。
  2. 读出 `RESULT_SPECIAL / RESULT_FOLLOWED / RESULT_REMARK` 三个字段，更新该用户对象。
  3. `adapter.notifyItemChanged(i);` 刷新该行 UI。
  4. `persistUsers();` 持久化本地。
  5. `updateUserOnBackend(target);` 调用后端同步修改。

---

## 7. 用户交互到后端的完整链路示例

以「点击列表中的关注按钮」为例：

1. 用户点击列表行的 `followButton`。
2. `FollowingAdapter.ViewHolder.bind(User user)` 中的点击监听触发：
   - `user.setFollowed(!user.isFollowed());`
   - 调整按钮文案和背景颜色（"关注" ↔ "已关注"）。
   - 调用 `onUserChanged.onUserChanged(user);`
3. `FollowingFragment` 在创建 Adapter 时传入的 `OnUserChangedListener` 被调用：
   - `persistUsers();`（保存到 `SharedPreferences`）。
   - `updateUserOnBackend(changedUser);`（调用 `StarUserApi.updateUser`）。
4. 后端成功返回后，如果 HTTP 状态码正常，什么都不做；失败则写日志。

> 特别关注开关、备注修改、取消关注的链路类似，只是从 BottomSheet 触发，然后通过 FragmentResult 更新列表，再复用 `persistUsers()` 和 `updateUserOnBackend()`。

---

## 8. ViewBinding 使用小结

- 每个布局 XML 都生成一个对应的 Binding 类：
  - `activity_main.xml` → `ActivityMainBinding`（当前主要通过 `setContentView` 使用）。
  - `activity_following.xml` → `ActivityFollowingBinding`。
  - `fragment_following.xml` → `FragmentFollowingBinding`。
  - `fragment_empty.xml` → `FragmentEmptyBinding`。
  - `item_following.xml` → `ItemFollowingBinding`。
  - `bottom_sheet_more_options.xml` → `BottomSheetMoreOptionsBinding`。
- 使用规范：
  - Activity：
    - `binding = XxxBinding.inflate(getLayoutInflater());`
    - `setContentView(binding.getRoot());`
  - Fragment：
    - 在 `onCreateView()` 中 inflate，`return binding.getRoot();`
  - RecyclerView.ViewHolder：
    - 在 `onCreateViewHolder()` 中 `ItemFollowingBinding.inflate(...)`，用 binding.getRoot() 作为 itemView。
  - BottomSheetDialogFragment：
    - 同 Fragment，用 Binding inflate。

这样整个客户端从启动、跳转、获取后端服务、刷新 / 分页加载，到 BottomSheet 的状态回传，都围绕这几层：

- UI（Activity / Fragment / BottomSheet）
- Adapter（ViewPager2 + RecyclerView）
- Model（User）
- Data（UserStorage + data.remote）

之间的责任分明、调用链路清晰。
