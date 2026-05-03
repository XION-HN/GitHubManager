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
    val error: String? = null,
    val isStarredTab: Boolean = false
)

@HiltViewModel
class RepoListViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoListUiState())
    val uiState: StateFlow<RepoListUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadRepos()
    }

    fun loadProfile() {
        viewModelScope.launch {
            gitHubRepository.getAuthenticatedUser()
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(user = user)
                }
        }
    }

    fun loadRepos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = if (_uiState.value.isStarredTab) {
                gitHubRepository.getStarredRepos()
            } else {
                gitHubRepository.getUserRepos()
            }
            result
                .onSuccess { repos ->
                    _uiState.value = _uiState.value.copy(repos = repos, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun toggleTab(isStarred: Boolean) {
        _uiState.value = _uiState.value.copy(isStarredTab = isStarred)
        loadRepos()
    }

    fun starRepo(owner: String, repo: String) {
        viewModelScope.launch {
            gitHubRepository.starRepository(owner, repo)
            loadRepos()
        }
    }

    fun unstarRepo(owner: String, repo: String) {
        viewModelScope.launch {
            gitHubRepository.unstarRepository(owner, repo)
            loadRepos()
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
