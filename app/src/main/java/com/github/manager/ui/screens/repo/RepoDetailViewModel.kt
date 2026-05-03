package com.github.manager.ui.screens.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.manager.data.model.*
import com.github.manager.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepoDetailUiState(
    val repo: Repository? = null,
    val commits: List<Commit> = emptyList(),
    val issues: List<Issue> = emptyList(),
    val pullRequests: List<PullRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentTab: Int = 0
)

@HiltViewModel
class RepoDetailViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoDetailUiState())
    val uiState: StateFlow<RepoDetailUiState> = _uiState.asStateFlow()

    private var owner: String = ""
    private var repoName: String = ""

    fun init(owner: String, repo: String) {
        this.owner = owner
        this.repoName = repo
        loadRepo()
        loadCommits()
    }

    fun loadRepo() {
        viewModelScope.launch {
            gitHubRepository.getRepository(owner, repoName)
                .onSuccess { repo ->
                    _uiState.value = _uiState.value.copy(repo = repo)
                }
        }
    }

    fun loadCommits() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            gitHubRepository.getCommits(owner, repoName)
                .onSuccess { commits ->
                    _uiState.value = _uiState.value.copy(commits = commits, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun loadIssues() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            gitHubRepository.getIssues(owner, repoName)
                .onSuccess { issues ->
                    val realIssues = issues.filter { it.pullRequest == null }
                    _uiState.value = _uiState.value.copy(issues = realIssues, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun loadPullRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            gitHubRepository.getPullRequests(owner, repoName)
                .onSuccess { prs ->
                    _uiState.value = _uiState.value.copy(pullRequests = prs, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun onTabChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
        when (index) {
            0 -> loadCommits()
            1 -> loadIssues()
            2 -> loadPullRequests()
        }
    }

    fun starRepo() {
        viewModelScope.launch {
            gitHubRepository.starRepository(owner, repoName)
            loadRepo()
        }
    }

    fun forkRepo() {
        viewModelScope.launch {
            gitHubRepository.forkRepository(owner, repoName)
        }
    }

    fun deleteRepo() {
        viewModelScope.launch {
            gitHubRepository.deleteRepository(owner, repoName)
        }
    }

    fun createIssue(title: String, body: String?) {
        viewModelScope.launch {
            gitHubRepository.createIssue(owner, repoName, title, body)
            loadIssues()
        }
    }
}
