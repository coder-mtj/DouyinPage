# DouyinPage

## 项目
这是我照着抖音里“关注”页面并以ai辅助写的一个 Android Demo，主要就是打开之后直接跳到一个关注列表。界面里有：
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

## 其他
* 数据都是写死的，第一次启动会塞一堆默认明星