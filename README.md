# GitHub Manager

一个管理 GitHub 主页的安卓应用，使用 Kotlin + Jetpack Compose 构建。

## 功能

- **Token 登录** - 使用 GitHub Personal Access Token 安全登录
- **存储库管理** - 浏览、创建、删除、Star、Fork 存储库
- **提交查看** - 查看任意存储库的提交历史
- **Issue 管理** - 查看、创建 Issue
- **Pull Request** - 查看 PR 列表和状态
- **Star 管理** - 切换查看自己/Star 的存储库

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository Pattern |
| 依赖注入 | Hilt |
| 网络 | Retrofit + OkHttp + Moshi |
| 本地存储 | DataStore Preferences |
| 导航 | Navigation Compose |
| 图片加载 | Coil |

## 项目结构

```
app/src/main/java/com/github/manager/
├── GitHubManagerApp.kt          # Application
├── MainActivity.kt              # 入口 Activity
├── data/
│   ├── api/GitHubApiService.kt  # Retrofit API 接口定义
│   ├── local/TokenManager.kt    # Token 持久化存储
│   ├── model/GitHubModels.kt    # 数据模型
│   └── repository/GitHubRepository.kt  # 仓库层（封装API调用）
├── di/NetworkModule.kt          # Hilt 网络依赖注入
└── ui/
    ├── navigation/Navigation.kt # 导航图
    ├── screens/
    │   ├── auth/                # 登录页
    │   └── repo/                # 存储库列表+详情页
    └── theme/                   # Material 3 主题
```

## 快速开始

### 1. 生成 GitHub Token

1. 打开 GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 点击 "Generate new token"
3. 勾选所需权限：
   - `repo` (完整存储库控制)
   - `read:org`
   - `gist`
4. 生成并复制 Token

### 2. 构建项目

```bash
# 使用 Android Studio 打开项目，或命令行构建：
./gradlew assembleDebug
```

### 3. 运行

在 Android Studio 中点击 Run，或：

```bash
./gradlew installDebug
```

## GitHub API 版本

使用 GitHub REST API v2022-11-28，Base URL: `https://api.github.com/`

## Token 所需权限

| 权限 | 用途 |
|------|------|
| `repo` | 读写存储库、Issue、PR |
| `read:org` | 读取组织信息 |
| `delete_repo` | 删除存储库 |

## 后续扩展方向

- [ ] 搜索存储库和用户
- [ ] 查看文件内容/代码浏览
- [ ] 通知提醒
- [ ] 深色模式自定义
- [ ] 离线缓存（Room Database）
- [ ] 下拉刷新 + 分页加载
