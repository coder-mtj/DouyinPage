# DouyinPage 客户端架构总览

> 不含 `backend` 目录，只看 Android 客户端（Java + Layout）部分。

---

## 1. 包结构与职责划分

根包：`com.mtj.douyinpage`

- **根包（入口）**
  - `MainActivity`
    - 应用启动入口 Activity。
    - 加载 `activity_main.xml`，立即通过 `Intent` 跳到 `FollowingActivity`，然后 `finish()` 自己。

- **ui 包：`com.mtj.douyinpage.ui`**
  - `FollowingActivity`
    - 顶部栏 + `TabLayout` + `ViewPager2` 容器页面。
    - 使用 `ActivityFollowingBinding` 绑定 `activity_following.xml`。
    - 负责：
      - 顶部返回按钮（`finish()` 当前 Activity）。
      - 创建 `FollowingPagerAdapter`，管理 4 个 Tab 页面。
      - 使用 `TabLayoutMediator` 把 `TabLayout` 与 `ViewPager2` 联动。
  - `FollowingFragment`
    - 「关注」列表的核心页面。
    - 使用 `FragmentFollowingBinding` 绑定 `fragment_following.xml`。
    - 负责：
      - 管理 `RecyclerView` + `FollowingAdapter`。
      - 调用后端接口加载分页数据，并映射为本地 `User` 列表。
      - 监听滚动触底，做分页加载。
      - 通过 `UserStorage` 做本地持久化。
      - 打开 `MoreOptionsBottomSheet` 处理更多操作，并通过 FragmentResult 接收回传的修改结果。
  - `EmptyFragment`
    - 空状态页面，使用 `FragmentEmptyBinding` 绑定 `fragment_empty.xml`。
    - 通过 `newInstance(message)` 工厂方法传入不同提示语，用于「互关 / 粉丝 / 朋友」三个 Tab。
  - `MoreOptionsBottomSheet`
    - `BottomSheetDialogFragment` 底部弹窗。
    - 使用 `BottomSheetMoreOptionsBinding` 绑定 `bottom_sheet_more_options.xml`。
    - 展示单个用户的更多操作：特别关注开关、备注编辑、取消关注、复制抖音号等。
    - 通过 `FragmentManager.setFragmentResult` 向列表页面回传用户状态（是否特别关注 / 是否已关注 / 备注）。

- **adapter 包：`com.mtj.douyinpage.adapter`**
  - `FollowingPagerAdapter`
    - 继承 `FragmentStateAdapter`，服务于 `ViewPager2`。
    - 根据 position 创建不同 Fragment：
      - 0：`EmptyFragment.newInstance("暂无互关")`
      - 1：`FollowingFragment`（关注列表）
      - 2：`EmptyFragment.newInstance("暂无粉丝")`
      - 3：`EmptyFragment.newInstance("暂无朋友")`
  - `FollowingAdapter`
    - `RecyclerView.Adapter`，列表项 `ViewHolder` 使用 `ItemFollowingBinding` 绑定 `item_following.xml`。
    - 负责：
      - 绑定 `User` 模型到单行视图（头像、名称、认证标识、特别关注标签、关注按钮等）。
      - 处理关注按钮点击，切换 `isFollowed` 并更新样式。
      - 处理「三点」更多按钮点击，通过回调把对应 `User` 交回外部（Fragment 再去弹出底部菜单）。
      - 用户信息变更后，通过回调通知外部进行持久化和同步到后端。

- **model 包：`com.mtj.douyinpage.model`**
  - `User`
    - 关注列表中单个用户的数据模型，实现 `Serializable`。
    - 字段：`id`、`name`、`avatar`(drawable id)、`isVerified`、`isFollowed`、`douyinId`、`isSpecialFollow`、`remark`。
    - 提供多种构造函数 + Getter/Setter。

- **data 包：`com.mtj.douyinpage.data`**
  - `UserStorage`
    - 负责本地持久化用户列表（只在客户端内部使用）。
    - 基于 `SharedPreferences` + JSON 数组：
      - `saveUsers(Context, List<User>)`：把 `User` 列表序列化成 JSON 字符串保存。
      - `loadUsers(Context)`：从本地 JSON 还原为 `User` 列表。
      - `clear(Context)`：清空本地缓存（调试用途）。

- **远程数据层：`com.mtj.douyinpage.data.remote`**
  - `ApiClient`
    - 单例 Retrofit 客户端创建器。
    - 内部：
      - `BASE_URL = "http://localhost:8080/"`（指向后端服务）。
      - 创建 `OkHttpClient`，加 `HttpLoggingInterceptor` 输出基础网络日志。
      - 创建全局唯一 `StarUserApi` 实例：`getStarUserApi()`。
  - `StarUserApi`
    - Retrofit 接口定义：
      - `@GET("api/stars") list(page, size)`：分页获取明星用户列表。
      - `@PATCH("api/stars/{id}") updateUser(id, body)`：更新单个用户的关注相关状态。
  - 其它 DTO / Response / RequestBody 类
    - `StarUserDto`、`StarUserPageResponse`、`UpdateStarUserRequestBody` 等（从 `FollowingFragment` 引用可见）。
    - 用于网络层与后端 JSON 协议的映射，再由 `FollowingFragment` 负责转换成 UI 使用的 `User` 模型。

---

## 2. 页面与 Layout 的整体关系

### 2.1 Activity & Fragment 与 XML 对应关系

- `MainActivity` ↔ `activity_main.xml`
  - 主要是启动时的占位布局，随后立即跳转到 `FollowingActivity` 并结束自身。

- `FollowingActivity` ↔ `activity_following.xml`（通过 `ActivityFollowingBinding`）
  - 核心控件：
    - 顶部返回按钮 `backButton`。
    - 顶部 Tab 区域 `tabLayout`。
    - 内容区 `viewPager`（`ViewPager2`）。

- `FollowingFragment` ↔ `fragment_following.xml`（通过 `FragmentFollowingBinding`）
  - 核心控件：
    - 关注数量文案 `followingCountText`（显示「我的关注(x人)」）。
    - `RecyclerView`：展示用户列表。
    - 可能还有加载中视图 / 空视图等（具体以 XML 为准）。

- `EmptyFragment` ↔ `fragment_empty.xml`（通过 `FragmentEmptyBinding`）
  - 非常简单：一个文本 `emptyText` 显示「暂无互关 / 暂无粉丝 / 暂无朋友 / 暂无内容」等提示。

- `MoreOptionsBottomSheet` ↔ `bottom_sheet_more_options.xml`（通过 `BottomSheetMoreOptionsBinding`）
  - 底部弹出层，核心控件：
    - 用户名 `userNameText`。
    - 抖音号展示 + 复制按钮 `userDouyinId` / `copyDouyinButton`。
    - 特别关注开关 `specialFollowSwitch`。
    - 备注区域 `remarkSection` + `remarkValue`。
    - 取消关注按钮 `btnCancelFollow`。
    - 关闭按钮 `closeButton`。

- `FollowingAdapter.ViewHolder` ↔ `item_following.xml`（通过 `ItemFollowingBinding`）
  - 单个关注用户的一行：
    - `userAvatar`：头像。
    - `userName`：显示备注或昵称。
    - `verifiedIcon`：是否认证。
    - `specialTag`：是否特别关注。
    - `followButton`：关注 / 已关注按钮，点击切换状态。
    - `moreButton`：右侧三点按钮，点击弹出 `MoreOptionsBottomSheet`。

### 2.2 ViewBinding 的使用方式

项目大量使用 **ViewBinding**，代替 `findViewById`：

- Activity 中：
  - `ActivityFollowingBinding.inflate(getLayoutInflater())` → `setContentView(binding.getRoot())`。
- Fragment 中：
  - `FragmentFollowingBinding.inflate(inflater, container, false)` → 返回 `binding.getRoot()`。
- RecyclerView.ViewHolder 中：
  - 在 `onCreateViewHolder` 里直接使用 `ItemFollowingBinding.inflate(...)` 创建 Item 视图。
- BottomSheet 中：
  - `BottomSheetMoreOptionsBinding.inflate(inflater, container, false)`。

优点：
- 视图 ID 都是强类型字段，编译期检查，避免手写字符串 ID 出错。
- 少写大量 `findViewById`，代码更简洁。

---

## 3. 典型用户视图层级

结合上述组件，用户实际看到的是这样的一条链路：

1. **应用启动**
   - 系统通过 `AndroidManifest` 指定的 `MainActivity` 拉起应用进程。
2. **MainActivity**
   - 加载 `activity_main.xml`。
   - 立即 `startActivity(new Intent(this, FollowingActivity.class));` 跳到关注页面。
   - `finish()` 自己，不保留在返回栈中。
3. **FollowingActivity**
   - 加载 `activity_following.xml`。
   - 顶部显示标题 + 返回按钮，返回键 `finish()` 当前 Activity。
   - 创建 `FollowingPagerAdapter`，配置 `TabLayout` + `ViewPager2`，形成「互关 / 关注 / 粉丝 / 朋友」四个 Tab。
   - 默认选中第 1 个 Tab（关注）。
4. **FollowingFragment（关注 Tab）**
   - 加载 `fragment_following.xml`，展示：
     - 顶部「我的关注(x人)」。
     - 下方 `RecyclerView` 列表。
   - 第一次进入时触发从后端加载数据（通过 `StarUserApi`）。
5. **RecyclerView + FollowingAdapter**
   - 每一行用 `item_following.xml` 渲染一个 `User`：头像、名称、认证、特别关注标识、关注按钮、更多按钮。
   - `Follow` 按钮直接在本地切换 `User.isFollowed` 并更新样式，同时通知 Fragment 持久化 & 通知后端。
   - 「更多」按钮则弹出 `MoreOptionsBottomSheet`。
6. **MoreOptionsBottomSheet**
   - 加载 `bottom_sheet_more_options.xml`。
   - 用户可在此修改：特别关注、备注、取消关注、复制抖音号等。
   - 操作结果通过 FragmentResult 回传到 `FollowingFragment`，驱动列表更新和后端同步。

---

## 4. 架构风格小结

- **UI 层**：`Activity + Fragment + RecyclerView + BottomSheet`，配合 ViewBinding 管理布局。
- **适配器层**：`FollowingPagerAdapter` 管理 Tab/Fragment，`FollowingAdapter` 管理列表项视图和交互。
- **模型 & 本地数据层**：`User` 表示业务实体，`UserStorage` 负责 SharedPreferences 持久化。
- **远程数据层**：`ApiClient + StarUserApi + DTO/Response`，基于 Retrofit + OkHttp 调用本地后端服务（默认 `http://localhost:8080/`）。
- **事件与状态流**：
  - UI 交互（点击按钮 / 滚动） → 更新 `User` 或发起网络请求。
  - 更新结果写回 `users` 列表 → `RecyclerView.Adapter` 通知刷新界面。
  - 同步到本地 (`UserStorage`) 和后端 (`StarUserApi`)，保证刷新后仍能保持状态一致。

这份文档只讲「整体构架」和布局关系，具体的「如何启动页面、页面跳转、如何获取后端服务、分页加载与刷新、BottomSheet 状态回传」等流程，会在 `client-details.md` 中详细展开。
