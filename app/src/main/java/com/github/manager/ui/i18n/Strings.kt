package com.github.manager.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class LanguageMode {
    CHINESE, ENGLISH, BILINGUAL
}

data class BilingualText(val zh: String, val en: String)

object Strings {
    val appName = BilingualText("GitHub 管理器", "GitHub Manager")
    val signIn = BilingualText("登录", "Sign In")
    val signInWithToken = BilingualText("使用个人访问令牌登录", "Sign in with your Personal Access Token")
    val personalAccessToken = BilingualText("个人访问令牌", "Personal Access Token")
    val tokenCannotBeEmpty = BilingualText("令牌不能为空", "Token cannot be empty")
    val authFailed = BilingualText("认证失败", "Authentication failed")
    val tokenHelp = BilingualText(
        "前往 设置 > 开发者设置 > 个人访问令牌\n生成具有所需权限的新令牌。",
        "Go to Settings > Developer settings > Personal access tokens\nto generate a new token with the required scopes."
    )
    val hideToken = BilingualText("隐藏令牌", "Hide token")
    val showToken = BilingualText("显示令牌", "Show token")

    val myRepos = BilingualText("我的仓库", "My Repos")
    val starred = BilingualText("已收藏", "Starred")
    val createRepo = BilingualText("创建仓库", "Create Repo")
    val account = BilingualText("账号", "Account")
    val retry = BilingualText("重试", "Retry")

    val repositoryName = BilingualText("仓库名称", "Repository Name")
    val descriptionOptional = BilingualText("描述（可选）", "Description (optional)")
    val privateRepo = BilingualText("私有", "Private")
    val create = BilingualText("创建", "Create")
    val cancel = BilingualText("取消", "Cancel")
    val createRepository = BilingualText("创建仓库", "Create Repository")

    val commits = BilingualText("提交", "Commits")
    val issues = BilingualText("问题", "Issues")
    val pullRequests = BilingualText("拉取请求", "Pull Requests")
    val branches = BilingualText("分支", "Branchs")
    val actions = BilingualText("工作流", "Actions")

    val star = BilingualText("收藏", "Star")
    val unstar = BilingualText("取消收藏", "Unstar")
    val fork = BilingualText("复刻", "Fork")
    val delete = BilingualText("删除", "Delete")
    val deleteRepo = BilingualText("删除仓库", "Delete Repository")
    val deleteRepoConfirm = BilingualText(
        "确定要删除此仓库吗？此操作不可撤销。",
        "Are you sure you want to delete this repo? This action cannot be undone."
    )

    val createIssue = BilingualText("创建问题", "Create Issue")
    val title = BilingualText("标题", "Title")
    val bodyOptional = BilingualText("内容（可选）", "Body (optional)")
    val draft = BilingualText("草稿", "Draft")

    val logout = BilingualText("退出登录", "Logout")
    val logoutConfirm = BilingualText(
        "确定要退出登录吗？你需要重新输入令牌。",
        "Are you sure you want to logout? You will need to enter your token again."
    )
    val switchAccount = BilingualText("切换账号", "Switch Account")
    val newPersonalAccessToken = BilingualText("新的个人访问令牌", "New Personal Access Token")
    val switch = BilingualText("切换", "Switch")

    val githubProfile = BilingualText("GitHub 主页", "GitHub Profile")
    val memberSince = BilingualText("注册时间", "Member Since")
    val repos = BilingualText("仓库", "Repos")
    val gists = BilingualText("Gists", "Gists")
    val followers = BilingualText("关注者", "Followers")
    val following = BilingualText("关注中", "Following")

    val defaultBranch = BilingualText("默认分支", "Default Branch")
    val currentBranch = BilingualText("当前分支", "Current Branch")
    val switchBranch = BilingualText("切换分支", "Switch Branch")
    val selectBranch = BilingualText("选择分支", "Select Branch")
    val protected = BilingualText("受保护", "Protected")

    val workflow = BilingualText("工作流", "Workflow")
    val runNumber = BilingualText("运行编号", "Run #")
    val triggerWorkflow = BilingualText("触发工作流", "Trigger Workflow")
    val reRun = BilingualText("重新运行", "Re-run")
    val cancelRun = BilingualText("取消运行", "Cancel Run")
    val monitoring = BilingualText("监控中", "Monitoring")
    val completed = BilingualText("已完成", "Completed")
    val inProgress = BilingualText("进行中", "In Progress")
    val queued = BilingualText("排队中", "Queued")
    val waiting = BilingualText("等待中", "Waiting")
    val failed = BilingualText("失败", "Failed")
    val success = BilingualText("成功", "Success")
    val cancelled = BilingualText("已取消", "Cancelled")
    val noWorkflows = BilingualText("暂无工作流", "No Workflows")
    val noRuns = BilingualText("暂无运行记录", "No Runs")
    val startedAt = BilingualText("开始时间", "Started At")
    val duration = BilingualText("耗时", "Duration")
    val refresh = BilingualText("刷新", "Refresh")
    val autoRefresh = BilingualText("自动刷新", "Auto Refresh")
    val language = BilingualText("语言", "Language")
    val chinese = BilingualText("中文", "Chinese")
    val english = BilingualText("英文", "English")
    val bilingual = BilingualText("中英双语", "Bilingual")

    val loading = BilingualText("加载中...", "Loading...")
    val error = BilingualText("错误", "Error")
    val noData = BilingualText("暂无数据", "No Data")
}

val LocalLanguageMode = staticCompositionLocalOf { LanguageMode.BILINGUAL }

val languageModeState = mutableStateOf(LanguageMode.BILINGUAL)

@Composable
fun bt(text: BilingualText): String {
    return when (languageModeState.value) {
        LanguageMode.CHINESE -> text.zh
        LanguageMode.ENGLISH -> text.en
        LanguageMode.BILINGUAL -> text.zh
    }
}

@Composable
fun BilingualLabel(text: BilingualText) {
    when (languageModeState.value) {
        LanguageMode.CHINESE -> {
            androidx.compose.material3.Text(text.zh)
        }
        LanguageMode.ENGLISH -> {
            androidx.compose.material3.Text(text.en)
        }
        LanguageMode.BILINGUAL -> {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(text.zh)
                androidx.compose.material3.Text(
                    text = text.en,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun BilingualLabelSmall(text: BilingualText) {
    when (languageModeState.value) {
        LanguageMode.CHINESE -> {
            androidx.compose.material3.Text(text.zh, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
        LanguageMode.ENGLISH -> {
            androidx.compose.material3.Text(text.en, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        }
        LanguageMode.BILINGUAL -> {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(text.zh, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                androidx.compose.material3.Text(
                    text = text.en,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
