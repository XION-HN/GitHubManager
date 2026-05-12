package com.github.manager.ui.screens.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.manager.data.local.TokenManager
import com.github.manager.data.model.Repository
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepoListUiState(
    val user: User? = null,
    val repos: List<Repository> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null,
    val isStarredTab: Boolean = false,
    val starredRepos: Set<String> = emptySet(),
    val isOfflineFallback: Boolean = false
)

@HiltViewModel
class RepoListViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoListUiState())
    val uiState: StateFlow<RepoListUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val perPage = 30

    init {
        loadProfile()
        loadRepos()
        loadStarredSet()
    }

    fun loadProfile() {
        viewModelScope.launch {
            gitHubRepository.getAuthenticatedUser()
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(user = user)
                }
                .onFailure {
                    val cached = gitHubRepository.getAuthenticatedUserFromCache()
                    if (cached != null) {
                        _uiState.value = _uiState.value.copy(user = cached)
                    }
                }
        }
    }

    private fun loadStarredSet() {
        viewModelScope.launch {
            gitHubRepository.getStarredRepos()
                .onSuccess { repos ->
                    _uiState.value = _uiState.value.copy(
                        starredRepos = repos.map { it.fullName }.toSet()
                    )
                }
                .onFailure {
                    val cached = gitHubRepository.getStarredReposFromCache()
                    if (cached.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            starredRepos = cached.map { it.fullName }.toSet()
                        )
                    }
                }
        }
    }

    fun loadRepos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isOfflineFallback = false)
            currentPage = 1
            val result = if (_uiState.value.isStarredTab) {
                gitHubRepository.getStarredRepos(page = currentPage)
            } else {
                gitHubRepository.getUserRepos(page = currentPage)
            }
            result
                .onSuccess { repos ->
                    _uiState.value = _uiState.value.copy(
                        repos = repos,
                        isLoading = false,
                        hasMore = repos.size >= perPage
                    )
                }
                .onFailure { e ->
                    val cached = if (_uiState.value.isStarredTab) {
                        gitHubRepository.getStarredReposFromCache()
                    } else {
                        gitHubRepository.getUserReposFromCache()
                    }
                    if (cached.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            repos = cached,
                            isLoading = false,
                            hasMore = false,
                            isOfflineFallback = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            currentPage = 1
            val result = if (_uiState.value.isStarredTab) {
                gitHubRepository.getStarredRepos(page = currentPage)
            } else {
                gitHubRepository.getUserRepos(page = currentPage)
            }
            result
                .onSuccess { repos ->
                    _uiState.value = _uiState.value.copy(
                        repos = repos,
                        isRefreshing = false,
                        hasMore = repos.size >= perPage
                    )
                }
        .onFailure { e ->
                val cached = if (_uiState.value.isStarredTab) {
                    gitHubRepository.getStarredReposFromCache()
                } else {
                    gitHubRepository.getUserReposFromCache()
                }
                if (cached.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        repos = cached,
                        isRefreshing = false,
                        hasMore = false,
                        isOfflineFallback = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = e.message, isRefreshing = false)
                }
            }
            loadProfile()
            loadStarredSet()
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            currentPage++
            val result = if (_uiState.value.isStarredTab) {
                gitHubRepository.getStarredRepos(page = currentPage)
            } else {
                gitHubRepository.getUserRepos(page = currentPage)
            }
            result
                .onSuccess { repos ->
                    _uiState.value = _uiState.value.copy(
                        repos = _uiState.value.repos + repos,
                        isLoadingMore = false,
                        hasMore = repos.size >= perPage
                    )
                }
                .onFailure { e ->
                    currentPage--
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, error = e.message)
                }
        }
    }

    fun toggleTab(isStarred: Boolean) {
        _uiState.value = _uiState.value.copy(isStarredTab = isStarred)
        loadRepos()
    }

    fun starRepo(owner: String, repo: String) {
        val fullName = "$owner/$repo"
        _uiState.value = _uiState.value.copy(
            starredRepos = _uiState.value.starredRepos + fullName
        )
        viewModelScope.launch {
            gitHubRepository.starRepository(owner, repo)
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        starredRepos = _uiState.value.starredRepos - fullName
                    )
                }
            if (_uiState.value.isStarredTab) loadRepos()
        }
    }

    fun unstarRepo(owner: String, repo: String) {
        val fullName = "$owner/$repo"
        _uiState.value = _uiState.value.copy(
            starredRepos = _uiState.value.starredRepos - fullName
        )
        viewModelScope.launch {
            gitHubRepository.unstarRepository(owner, repo)
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        starredRepos = _uiState.value.starredRepos + fullName
                    )
                }
            if (_uiState.value.isStarredTab) loadRepos()
        }
    }

    fun forkRepo(owner: String, repo: String) {
        viewModelScope.launch {
            gitHubRepository.forkRepository(owner, repo)
            loadRepos()
        }
    }

    fun deleteRepo(owner: String, repo: String) {
        viewModelScope.launch {
            gitHubRepository.deleteRepository(owner, repo)
            loadRepos()
        }
    }

    fun createRepo(name: String, description: String?, isPrivate: Boolean) {
        viewModelScope.launch {
            gitHubRepository.createRepository(name, description, isPrivate)
            loadRepos()
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAll()
        }
    }
}
