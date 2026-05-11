package com.github.manager.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.manager.data.model.Repository
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private var username: String = ""
    private var currentPage = 1
    private val perPage = 30

    fun loadUser(username: String) {
        if (username == this.username && _uiState.value.user != null) return
        this.username = username
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            gitHubRepository.getUserProfile(username)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(user = user, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
            loadReposInternal()
        }
    }

    private suspend fun loadReposInternal() {
        currentPage = 1
        gitHubRepository.getUserRepos(username, page = currentPage)
            .onSuccess { repos ->
                _uiState.value = _uiState.value.copy(repos = repos, hasMore = repos.size >= perPage)
            }
            .onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
    }

    fun loadMoreRepos() {
        if (!_uiState.value.hasMore) return
        viewModelScope.launch {
            currentPage++
            gitHubRepository.getUserRepos(username, page = currentPage)
                .onSuccess { repos ->
                    _uiState.value = _uiState.value.copy(
                        repos = _uiState.value.repos + repos,
                        hasMore = repos.size >= perPage
                    )
                }
                .onFailure { currentPage-- }
        }
    }
}
