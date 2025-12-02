# DouyinPage

## 项目
这是我照着抖音里“关注”页面写的一个 Android Demo，主要就是打开之后直接跳到一个关注列表。界面里有：
- 顶部有个返回按钮
- TabLayout + ViewPager2，对应互关/关注/粉丝/朋友四个标签
- RecyclerView 显示一堆明星的头像和状态
- 点右边的小三点会弹出底部菜单

### 技术杂记
随便列一些：
1. Android 11 (minSdk 24, target 36)
2. SharedPreferences + JSON 储存用户列表
3. RecyclerView + Adapter 列表，TabLayoutMediator 做联动

```
MainActivity -> FollowingActivity -> Fragments
```
就是这条路线，MainActivity 只负责跳转。

#### 如何运行：
- Android Studio 打开工程
- Gradle Sync 等它转圈
- 接个模拟器或者手机，Run app

## 目录
app/
- java/com/mtj/douyinpage/... 大部分是Activity Fragment Adapter
- res/layout 关注页面相关 XML
- drawable 头像资源

## 后端说明（补充）

项目后端放在 `backend/DouyinBackend/DouyinBackend`，是一个轻量的 Spring Boot 服务，主要用于提供明星（star user）相关的 API：

- 技术栈：Spring Boot + SQLite（默认）
- 可选：Redis 缓存（配置在 `application.properties` 中，默认 `localhost:6379`）

主要接口：
- GET `/api/stars?page={page}&size={size}`：分页获取明星列表（服务端默认 page=0，size 在服务端被固定为 10）。
- PATCH `/api/stars/{id}`：更新某个明星的 `specialFollow`、`followed` 与 `remark` 字段，成功返回 204。

快速运行（后端）：

Windows PowerShell：
```powershell
cd backend\DouyinBackend\DouyinBackend
.\mvnw.cmd spring-boot:run
```

启动后后端默认监听 `http://localhost:8080/`。如果你要在 Android 模拟器中访问，请将客户端 `ApiClient.BASE_URL` 设置为 `http://10.0.2.2:8080/`（或使用宿主机局域网 IP）。

数据库与初始化：
- 数据库使用 SQLite，文件名为 `douyin-following.db`（在后端运行目录生成）。
- 项目自带 `schema.sql`，如果数据量不足，服务会自动生成 1000 条 mock 数据用于开发调试。

```
docs/technical-overview.md
```
