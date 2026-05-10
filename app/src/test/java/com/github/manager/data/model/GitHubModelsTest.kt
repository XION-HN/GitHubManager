package com.github.manager.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GitHubModelsTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    @Test
    fun `User deserialization with snake_case json`() {
        val json = """{
            "id": 1,
            "login": "octocat",
            "name": "Octocat",
            "avatar_url": "https://github.com/avatar.png",
            "html_url": "https://github.com/octocat",
            "bio": "GitHub mascot",
            "public_repos": 10,
            "public_gists": 5,
            "followers": 100,
            "following": 0,
            "created_at": "2024-01-01T00:00:00Z",
            "updated_at": "2024-06-01T00:00:00Z"
        }"""

        val adapter = moshi.adapter(User::class.java)
        val user = adapter.fromJson(json)!!

        assertEquals(1L, user.id)
        assertEquals("octocat", user.login)
        assertEquals("Octocat", user.name)
        assertEquals("https://github.com/avatar.png", user.avatarUrl)
        assertEquals(10, user.publicRepos)
        assertEquals(100, user.followers)
    }

    @Test
    fun `User default values`() {
        val json = """{"id": 2, "login": "test"}"""
        val adapter = moshi.adapter(User::class.java)
        val user = adapter.fromJson(json)!!

        assertNull(user.name)
        assertEquals("", user.avatarUrl)
        assertEquals(0, user.publicRepos)
        assertEquals(0, user.followers)
    }

    @Test
    fun `Repository deserialization`() {
        val json = """{
            "id": 1,
            "name": "hello-world",
            "full_name": "octocat/hello-world",
            "owner": {"id": 1, "login": "octocat"},
            "description": "My first repo",
            "private": false,
            "fork": false,
            "language": "Kotlin",
            "stargazers_count": 42,
            "forks_count": 7,
            "open_issues_count": 3,
            "default_branch": "main",
            "topics": ["kotlin", "android"]
        }"""

        val adapter = moshi.adapter(Repository::class.java)
        val repo = adapter.fromJson(json)!!

        assertEquals("hello-world", repo.name)
        assertEquals("octocat/hello-world", repo.fullName)
        assertEquals("Kotlin", repo.language)
        assertEquals(42, repo.stargazersCount)
        assertEquals(listOf("kotlin", "android"), repo.topics)
        assertFalse(repo.private)
    }

    @Test
    fun `Issue deserialization with pull_request ref`() {
        val json = """{
            "id": 1,
            "number": 42,
            "title": "Bug report",
            "state": "open",
            "pull_request": {"url": "https://api.github.com/pulls/42", "html_url": "https://github.com/pull/42"}
        }"""

        val adapter = moshi.adapter(Issue::class.java)
        val issue = adapter.fromJson(json)!!

        assertEquals(42, issue.number)
        assertNotNull(issue.pullRequest)
        assertEquals("https://api.github.com/pulls/42", issue.pullRequest?.url)
    }

    @Test
    fun `Issue without pull_request is null`() {
        val json = """{"id": 1, "number": 1, "title": "Test", "state": "open"}"""
        val adapter = moshi.adapter(Issue::class.java)
        val issue = adapter.fromJson(json)!!

        assertNull(issue.pullRequest)
    }

    @Test
    fun `PullRequest deserialization`() {
        val json = """{
            "id": 1,
            "number": 10,
            "title": "Feature PR",
            "state": "open",
            "head": {"ref": "feature-branch", "sha": "abc123"},
            "base": {"ref": "main", "sha": "def456"},
            "draft": false,
            "mergeable": true
        }"""

        val adapter = moshi.adapter(PullRequest::class.java)
        val pr = adapter.fromJson(json)!!

        assertEquals(10, pr.number)
        assertEquals("feature-branch", pr.head.ref)
        assertEquals("main", pr.base.ref)
        assertFalse(pr.draft)
        assertTrue(pr.mergeable!!)
    }

    @Test
    fun `CreateRepoRequest serialization`() {
        val request = CreateRepoRequest(name = "test-repo", description = "A test repo", private = true, autoInit = true)
        val adapter = moshi.adapter(CreateRepoRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"name\":\"test-repo\""))
        assertTrue(json.contains("\"private\":true"))
        assertTrue(json.contains("\"auto_init\":true"))
    }

    @Test
    fun `CreateIssueRequest serialization`() {
        val request = CreateIssueRequest(title = "Bug", body = "Description", labels = listOf("bug"))
        val adapter = moshi.adapter(CreateIssueRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"title\":\"Bug\""))
        assertTrue(json.contains("\"labels\""))
    }

    @Test
    fun `WorkflowRun deserialization`() {
        val json = """{
            "id": 100,
            "name": "CI",
            "head_branch": "main",
            "head_sha": "abc123",
            "status": "completed",
            "conclusion": "success",
            "workflow_id": 1
        }"""

        val adapter = moshi.adapter(WorkflowRun::class.java)
        val run = adapter.fromJson(json)!!

        assertEquals(100L, run.id)
        assertEquals("completed", run.status)
        assertEquals("success", run.conclusion)
        assertEquals("main", run.headBranch)
    }

    @Test
    fun `SearchResult deserialization`() {
        val json = """{
            "total_count": 100,
            "incomplete_results": false,
            "items": [{"id": 1, "name": "kotlin", "full_name": "jetbrains/kotlin", "owner": {"id": 1, "login": "jetbrains"}}]
        }"""

        val adapter = moshi.adapter(SearchResult::class.java)
        val result = adapter.fromJson(json)!!

        assertEquals(100, result.totalCount)
        assertFalse(result.incompleteResults)
        assertEquals(1, result.items?.size)
    }

    @Test
    fun `UserSearchResult deserialization`() {
        val json = """{
            "total_count": 50,
            "incomplete_results": false,
            "items": [{"id": 1, "login": "octocat"}]
        }"""

        val adapter = moshi.adapter(UserSearchResult::class.java)
        val result = adapter.fromJson(json)!!

        assertEquals(50, result.totalCount)
        assertEquals(1, result.items?.size)
        assertEquals("octocat", result.items?.first()?.login)
    }

    @Test
    fun `RepoContent deserialization`() {
        val json = """{
            "name": "src",
            "path": "src",
            "sha": "abc123",
            "size": 0,
            "type": "dir",
            "download_url": null,
            "html_url": "https://github.com/tree/main/src"
        }"""

        val adapter = moshi.adapter(RepoContent::class.java)
        val content = adapter.fromJson(json)!!

        assertEquals("src", content.name)
        assertEquals("dir", content.type)
        assertNull(content.downloadUrl)
    }

    @Test
    fun `UpdateIssueRequest serialization with partial fields`() {
        val request = UpdateIssueRequest(state = "closed")
        val adapter = moshi.adapter(UpdateIssueRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"state\":\"closed\""))
        assertFalse(json.contains("\"title\""))
    }

    @Test
    fun `MergePRRequest default merge method`() {
        val request = MergePRRequest()
        assertEquals("merge", request.mergeMethod)
        assertNull(request.sha)
        assertNull(request.commitTitle)
    }

    @Test
    fun `Release deserialization`() {
        val json = """{
            "id": 1,
            "tag_name": "v1.0.0",
            "name": "First Release",
            "body": "Release notes",
            "draft": false,
            "prerelease": false,
            "assets": [{"id": 1, "name": "app.apk", "content_type": "application/apk", "size": 1024, "browser_download_url": "https://example.com/app.apk", "download_count": 50}]
        }"""

        val adapter = moshi.adapter(Release::class.java)
        val release = adapter.fromJson(json)!!

        assertEquals("v1.0.0", release.tagName)
        assertEquals("First Release", release.name)
        assertEquals(1, release.assets?.size)
        assertEquals(50, release.assets?.first()?.downloadCount)
    }

    @Test
    fun `Branch deserialization`() {
        val json = """{
            "name": "main",
            "commit": {"sha": "abc123", "url": "https://api.github.com/commits/abc123"},
            "protected": true
        }"""

        val adapter = moshi.adapter(Branch::class.java)
        val branch = adapter.fromJson(json)!!

        assertEquals("main", branch.name)
        assertEquals("abc123", branch.commit.sha)
        assertTrue(branch.protected)
    }

    @Test
    fun `CachedData stores timestamp`() {
        val data = CachedData(data = "test", timestamp = 1000L)
        assertEquals("test", data.data)
        assertEquals(1000L, data.timestamp)
    }

    @Test
    fun `WorkflowDispatchRequest serialization`() {
        val request = WorkflowDispatchRequest(ref = "main")
        val adapter = moshi.adapter(WorkflowDispatchRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"ref\":\"main\""))
    }

    @Test
    fun `WorkflowListResponse deserialization`() {
        val json = """{
            "total_count": 2,
            "workflows": [
                {"id": 1, "name": "CI", "path": ".github/workflows/ci.yml", "state": "active"},
                {"id": 2, "name": "CD", "path": ".github/workflows/cd.yml", "state": "active"}
            ]
        }"""

        val adapter = moshi.adapter(WorkflowListResponse::class.java)
        val response = adapter.fromJson(json)!!

        assertEquals(2, response.totalCount)
        assertEquals(2, response.workflows?.size)
        assertEquals("CI", response.workflows?.first()?.name)
    }

    @Test
    fun `IssueComment deserialization`() {
        val json = """{
            "id": 1,
            "body": "Great fix!",
            "user": {"id": 1, "login": "commenter"},
            "created_at": "2024-01-01T00:00:00Z"
        }"""

        val adapter = moshi.adapter(IssueComment::class.java)
        val comment = adapter.fromJson(json)!!

        assertEquals(1L, comment.id)
        assertEquals("Great fix!", comment.body)
        assertEquals("commenter", comment.user?.login)
    }

    @Test
    fun `Commit deserialization`() {
        val json = """{
            "sha": "abc123def456",
            "commit": {
                "author": {"name": "John", "email": "john@test.com", "date": "2024-01-15T10:30:00Z"},
                "message": "feat: add new feature"
            },
            "author": {"id": 1, "login": "john"},
            "html_url": "https://github.com/owner/repo/commit/abc123def456"
        }"""

        val adapter = moshi.adapter(Commit::class.java)
        val commit = adapter.fromJson(json)!!

        assertEquals("abc123def456", commit.sha)
        assertEquals("feat: add new feature", commit.commit.message)
        assertEquals("John", commit.commit.author.name)
        assertEquals("john@test.com", commit.commit.author.email)
        assertEquals("john", commit.author?.login)
    }

    @Test
    fun `Commit deserialization with minimal fields`() {
        val json = """{"sha": "minimal123"}"""
        val adapter = moshi.adapter(Commit::class.java)
        val commit = adapter.fromJson(json)!!

        assertEquals("minimal123", commit.sha)
        assertNull(commit.author)
    }

    @Test
    fun `CommitAuthor deserialization`() {
        val json = """{"name": "Jane", "email": "jane@test.com", "date": "2024-03-01T00:00:00Z"}"""
        val adapter = moshi.adapter(CommitAuthor::class.java)
        val author = adapter.fromJson(json)!!

        assertEquals("Jane", author.name)
        assertEquals("jane@test.com", author.email)
        assertEquals("2024-03-01T00:00:00Z", author.date)
    }

    @Test
    fun `CommitDetail deserialization`() {
        val json = """{
            "author": {"name": "Dev", "email": "dev@test.com", "date": "2024-01-01"},
            "message": "fix: critical bug"
        }"""
        val adapter = moshi.adapter(CommitDetail::class.java)
        val detail = adapter.fromJson(json)!!

        assertEquals("fix: critical bug", detail.message)
        assertEquals("Dev", detail.author.name)
    }

    @Test
    fun `Owner deserialization with all fields`() {
        val json = """{
            "id": 42,
            "login": "octocat",
            "avatar_url": "https://avatars.githubusercontent.com/u/42",
            "html_url": "https://github.com/octocat"
        }"""
        val adapter = moshi.adapter(Owner::class.java)
        val owner = adapter.fromJson(json)!!

        assertEquals(42L, owner.id)
        assertEquals("octocat", owner.login)
        assertEquals("https://avatars.githubusercontent.com/u/42", owner.avatarUrl)
        assertEquals("https://github.com/octocat", owner.htmlUrl)
    }

    @Test
    fun `Owner deserialization with minimal fields`() {
        val json = """{"id": 1, "login": "test"}"""
        val adapter = moshi.adapter(Owner::class.java)
        val owner = adapter.fromJson(json)!!

        assertEquals("test", owner.login)
        assertEquals("", owner.avatarUrl)
    }

    @Test
    fun `SearchResult with null items`() {
        val json = """{"total_count": 0, "incomplete_results": false, "items": null}"""
        val adapter = moshi.adapter(SearchResult::class.java)
        val result = adapter.fromJson(json)!!

        assertEquals(0, result.totalCount)
        assertNull(result.items)
    }

    @Test
    fun `UserSearchResult with null items`() {
        val json = """{"total_count": 0, "incomplete_results": false, "items": null}"""
        val adapter = moshi.adapter(UserSearchResult::class.java)
        val result = adapter.fromJson(json)!!

        assertEquals(0, result.totalCount)
        assertNull(result.items)
    }

    @Test
    fun `WorkflowRun with null conclusion for in-progress run`() {
        val json = """{
            "id": 200,
            "name": "Build",
            "head_branch": "develop",
            "head_sha": "def456",
            "status": "in_progress",
            "conclusion": null,
            "workflow_id": 5
        }"""
        val adapter = moshi.adapter(WorkflowRun::class.java)
        val run = adapter.fromJson(json)!!

        assertEquals(200L, run.id)
        assertEquals("in_progress", run.status)
        assertNull(run.conclusion)
        assertEquals("develop", run.headBranch)
    }

    @Test
    fun `WorkflowRunsResponse deserialization`() {
        val json = """{
            "total_count": 2,
            "workflow_runs": [
                {"id": 1, "name": "CI", "status": "completed", "conclusion": "success"},
                {"id": 2, "name": "CD", "status": "in_progress", "conclusion": null}
            ]
        }"""
        val adapter = moshi.adapter(WorkflowRunsResponse::class.java)
        val response = adapter.fromJson(json)!!

        assertEquals(2, response.totalCount)
        assertEquals(2, response.workflowRuns?.size)
        assertNull(response.workflowRuns?.get(1)?.conclusion)
    }

    @Test
    fun `RepoContent with base64 content`() {
        val json = """{
            "name": "main.kt",
            "path": "src/main.kt",
            "sha": "abc123",
            "size": 256,
            "type": "file",
            "encoding": "base64",
            "content": "ZnVuYyBtYWluKCk=",
            "download_url": "https://raw.githubusercontent.com/owner/repo/main/src/main.kt",
            "html_url": "https://github.com/owner/repo/blob/main/src/main.kt"
        }"""
        val adapter = moshi.adapter(RepoContent::class.java)
        val content = adapter.fromJson(json)!!

        assertEquals("main.kt", content.name)
        assertEquals("file", content.type)
        assertEquals("base64", content.encoding)
        assertEquals("ZnVuYyBtYWluKCk=", content.content)
        assertNotNull(content.downloadUrl)
    }

    @Test
    fun `Release with null assets`() {
        val json = """{
            "id": 5,
            "tag_name": "v0.1.0",
            "name": "Alpha",
            "body": "Early version",
            "draft": false,
            "prerelease": true,
            "assets": null
        }"""
        val adapter = moshi.adapter(Release::class.java)
        val release = adapter.fromJson(json)!!

        assertEquals("v0.1.0", release.tagName)
        assertTrue(release.prerelease)
        assertNull(release.assets)
    }

    @Test
    fun `PullRequestRef deserialization`() {
        val json = """{
            "url": "https://api.github.com/repos/owner/repo/pulls/10",
            "html_url": "https://github.com/owner/repo/pull/10"
        }"""
        val adapter = moshi.adapter(PullRequestRef::class.java)
        val ref = adapter.fromJson(json)!!

        assertEquals("https://api.github.com/repos/owner/repo/pulls/10", ref.url)
        assertEquals("https://github.com/owner/repo/pull/10", ref.htmlUrl)
    }

    @Test
    fun `PRBranch deserialization`() {
        val json = """{"ref": "feature-x", "sha": "abc789", "label": "owner:feature-x"}"""
        val adapter = moshi.adapter(PRBranch::class.java)
        val branch = adapter.fromJson(json)!!

        assertEquals("feature-x", branch.ref)
        assertEquals("abc789", branch.sha)
        assertEquals("owner:feature-x", branch.label)
    }

    @Test
    fun `Label deserialization`() {
        val json = """{"id": 10, "name": "bug", "color": "fc2929", "description": "Something isn't working"}"""
        val adapter = moshi.adapter(Label::class.java)
        val label = adapter.fromJson(json)!!

        assertEquals(10L, label.id)
        assertEquals("bug", label.name)
        assertEquals("fc2929", label.color)
        assertEquals("Something isn't working", label.description)
    }

    @Test
    fun `WorkflowRunResponseWrapper deserialization`() {
        val json = """{
            "workflow_run": {"id": 300, "name": "Deploy", "status": "queued", "conclusion": null, "workflow_id": 10}
        }"""
        val adapter = moshi.adapter(WorkflowRunResponseWrapper::class.java)
        val wrapper = adapter.fromJson(json)!!

        assertNotNull(wrapper.workflowRun)
        assertEquals(300L, wrapper.workflowRun?.id)
        assertEquals("queued", wrapper.workflowRun?.status)
    }

    @Test
    fun `WorkflowRunResponseWrapper with null run`() {
        val json = """{"workflow_run": null}"""
        val adapter = moshi.adapter(WorkflowRunResponseWrapper::class.java)
        val wrapper = adapter.fromJson(json)!!

        assertNull(wrapper.workflowRun)
    }

    @Test
    fun `BranchCommit deserialization`() {
        val json = """{"sha": "abc123", "url": "https://api.github.com/repos/owner/repo/commits/abc123"}"""
        val adapter = moshi.adapter(BranchCommit::class.java)
        val commit = adapter.fromJson(json)!!

        assertEquals("abc123", commit.sha)
        assertEquals("https://api.github.com/repos/owner/repo/commits/abc123", commit.url)
    }

    @Test
    fun `ReleaseAsset deserialization`() {
        val json = """{
            "id": 55,
            "name": "app-release.apk",
            "content_type": "application/vnd.android.package-archive",
            "size": 2048000,
            "browser_download_url": "https://github.com/owner/repo/releases/download/v1.0/app.apk",
            "download_count": 150
        }"""
        val adapter = moshi.adapter(ReleaseAsset::class.java)
        val asset = adapter.fromJson(json)!!

        assertEquals(55L, asset.id)
        assertEquals("app-release.apk", asset.name)
        assertEquals(2048000L, asset.size)
        assertEquals(150, asset.downloadCount)
    }

    @Test
    fun `Repository serialization round trip`() {
        val repo = Repository(
            id = 1, name = "test-repo", fullName = "user/test-repo",
            owner = Owner(1, "user"), description = "A test repo",
            language = "Kotlin", stargazersCount = 42, topics = listOf("kotlin", "android")
        )
        val adapter = moshi.adapter(Repository::class.java)
        val json = adapter.toJson(repo)
        val roundTrip = adapter.fromJson(json)!!

        assertEquals(repo.id, roundTrip.id)
        assertEquals(repo.name, roundTrip.name)
        assertEquals(repo.fullName, roundTrip.fullName)
        assertEquals(repo.description, roundTrip.description)
        assertEquals(repo.language, roundTrip.language)
        assertEquals(repo.stargazersCount, roundTrip.stargazersCount)
        assertEquals(repo.topics, roundTrip.topics)
    }

    @Test
    fun `User serialization round trip`() {
        val user = User(id = 1, login = "testuser", name = "Test", avatarUrl = "https://avatar.png", publicRepos = 5, followers = 20)
        val adapter = moshi.adapter(User::class.java)
        val json = adapter.toJson(user)
        val roundTrip = adapter.fromJson(json)!!

        assertEquals(user.id, roundTrip.id)
        assertEquals(user.login, roundTrip.login)
        assertEquals(user.name, roundTrip.name)
        assertEquals(user.avatarUrl, roundTrip.avatarUrl)
        assertEquals(user.publicRepos, roundTrip.publicRepos)
        assertEquals(user.followers, roundTrip.followers)
    }

    @Test
    fun `Issue serialization round trip`() {
        val issue = Issue(id = 1, number = 42, title = "Bug report", state = "open", body = "Description")
        val adapter = moshi.adapter(Issue::class.java)
        val json = adapter.toJson(issue)
        val roundTrip = adapter.fromJson(json)!!

        assertEquals(issue.id, roundTrip.id)
        assertEquals(issue.number, roundTrip.number)
        assertEquals(issue.title, roundTrip.title)
        assertEquals(issue.state, roundTrip.state)
        assertEquals(issue.body, roundTrip.body)
    }
}
