# Requirements Document

## Introduction

本文档定义了将 Android 端关注列表功能从本地数据存储改造为使用后端 API 的需求。系统将通过 RESTful API 从 Spring Boot 后端获取明星用户数据（1000+ 条），支持分页加载和状态更新同步。

## Glossary

- **Android Client**: Android 应用客户端，负责展示用户界面和处理用户交互
- **Backend Server**: Spring Boot 后端服务器，提供 RESTful API 接口
- **StarUser**: 明星用户数据模型，包含用户基本信息和关注状态
- **Retrofit**: Android 端使用的 HTTP 客户端库
- **OkHttp**: 底层 HTTP 网络库
- **Gson**: JSON 序列化/反序列化库
- **Redis**: 后端使用的缓存数据库
- **SQLite**: 后端使用的持久化数据库
- **Pagination**: 分页机制，每次请求返回固定数量的数据
- **DTO**: Data Transfer Object，用于网络传输的数据对象
- **BASE_URL**: 后端服务器的基础 URL 地址

## Requirements

### Requirement 1

**User Story:** 作为 Android 开发者，我希望在应用中集成网络层依赖，以便能够通过 HTTP 请求与后端服务器通信。

#### Acceptance Criteria

1. WHEN the Android Client builds THEN the system SHALL include Retrofit, OkHttp, and Gson dependencies in the app module
2. WHEN the Android Client initializes the network layer THEN the system SHALL configure BASE_URL as http://10.0.2.2:8080/ for Android emulator
3. WHEN the Android Client creates Retrofit instance THEN the system SHALL configure Gson converter for JSON serialization
4. WHEN the Android Client makes HTTP requests THEN the system SHALL use OkHttp as the underlying HTTP client

### Requirement 2

**User Story:** 作为 Android 开发者，我希望定义网络 API 接口，以便能够调用后端的明星用户数据接口。

#### Acceptance Criteria

1. WHEN the Android Client defines API interface THEN the system SHALL create a method for GET /api/stars with page and size parameters
2. WHEN the Android Client defines API interface THEN the system SHALL create a method for PATCH /api/stars/{id} with request body
3. WHEN the Android Client calls GET /api/stars THEN the system SHALL receive a paginated response containing StarUser list and total count
4. WHEN the Android Client calls PATCH /api/stars/{id} THEN the system SHALL send updated followed status, specialFollow status, and remark fields

### Requirement 3

**User Story:** 作为 Android 开发者，我希望创建网络数据模型，以便能够正确映射后端返回的 JSON 数据。

#### Acceptance Criteria

1. WHEN the Android Client receives JSON response THEN the system SHALL deserialize it into StarUserDto with fields: id, name, avatarIndex, verified, followed, douyinId, specialFollow, remark
2. WHEN the Android Client converts StarUserDto to User model THEN the system SHALL map avatarIndex to corresponding drawable resource ID
3. WHEN avatarIndex value is N THEN the system SHALL map it to R.drawable.avatar_N resource
4. WHEN the Android Client serializes update request THEN the system SHALL create JSON with followed, specialFollow, and remark fields

### Requirement 4

**User Story:** 作为用户，我希望应用启动时从后端加载明星用户列表，以便看到最新的关注数据。

#### Acceptance Criteria

1. WHEN FollowingFragment initializes THEN the system SHALL request GET /api/stars with page=0 and appropriate size parameter
2. WHEN the Backend Server returns StarUser data THEN the Android Client SHALL convert each StarUserDto to User model
3. WHEN the Android Client receives total count THEN the system SHALL display it in the format "我的关注(X人)"
4. WHEN the Android Client loads data successfully THEN the system SHALL populate RecyclerView with converted User list
5. WHEN the Android Client fails to load data THEN the system SHALL display an error message to the user

### Requirement 5

**User Story:** 作为用户，我希望点击关注按钮时能够同步更新到后端，以便我的关注状态能够持久化保存。

#### Acceptance Criteria

1. WHEN a user clicks the follow button THEN the Android Client SHALL immediately update the UI to reflect the new state
2. WHEN the UI updates THEN the Android Client SHALL asynchronously call PATCH /api/stars/{id} with the new followed status
3. WHEN the PATCH request succeeds THEN the system SHALL maintain the updated UI state
4. WHEN the PATCH request fails THEN the system SHALL revert the UI state and notify the user

### Requirement 6

**User Story:** 作为用户，我希望在底部菜单中更新特别关注和备注时能够同步到后端，以便这些设置能够跨设备保存。

#### Acceptance Criteria

1. WHEN a user toggles special follow switch THEN the Android Client SHALL immediately update the UI
2. WHEN special follow state changes THEN the Android Client SHALL asynchronously call PATCH /api/stars/{id} with the new specialFollow status
3. WHEN a user saves a remark THEN the Android Client SHALL immediately update the UI
4. WHEN remark is saved THEN the Android Client SHALL asynchronously call PATCH /api/stars/{id} with the new remark text
5. WHEN the PATCH request fails THEN the system SHALL notify the user and maintain the previous state

### Requirement 7

**User Story:** 作为开发者，我希望保留现有的 UserStorage 类，以便将来可以实现本地缓存功能。

#### Acceptance Criteria

1. WHEN the Android Client refactors data loading THEN the system SHALL keep UserStorage class in the codebase
2. WHEN the Android Client loads data THEN the system SHALL not call UserStorage.createDefaultUsers() method
3. WHEN the Android Client updates user state THEN the system SHALL optionally persist to SharedPreferences for offline support

### Requirement 8

**User Story:** 作为开发者，我希望后端能够提供 1000+ 条明星用户数据，以便测试分页加载功能。

#### Acceptance Criteria

1. WHEN the Backend Server initializes THEN the system SHALL generate at least 1000 mock StarUser records
2. WHEN the Backend Server receives pagination request THEN the system SHALL return exactly the requested page size of records
3. WHEN the Backend Server stores data THEN the system SHALL use SQLite for persistence and Redis for caching
4. WHEN the Backend Server returns paginated data THEN the system SHALL include total count in the response

### Requirement 9

**User Story:** 作为开发者，我希望配置灵活的 BASE_URL，以便支持模拟器和真机测试。

#### Acceptance Criteria

1. WHEN the Android Client runs on emulator THEN the system SHALL use BASE_URL http://10.0.2.2:8080/
2. WHEN the Android Client runs on physical device THEN the system SHALL support configuration of LAN IP address
3. WHEN BASE_URL is configured THEN the system SHALL apply it to all Retrofit API calls
4. WHEN network configuration changes THEN the system SHALL allow easy modification of BASE_URL constant
