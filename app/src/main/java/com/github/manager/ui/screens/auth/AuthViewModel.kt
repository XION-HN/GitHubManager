package com.github.manager.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.manager.data.local.TokenManager
import com.github.manager.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tokenManager.token.collect { savedToken ->
                if (!savedToken.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(token = savedToken)
                    validateToken(savedToken)
                } else {
                    _uiState.value = _uiState.value.copy(isAuthenticated = false)
                }
            }
        }
    }

    fun onTokenChanged(token: String) {
        _uiState.value = _uiState.value.copy(token = token, error = null)
    }

    fun login() {
        val token = _uiState.value.token.trim()
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "令牌不能为空 / Token cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            tokenManager.saveToken(token)
            validateToken(token)
        }
    }

    private suspend fun validateToken(token: String) {
        gitHubRepository.getAuthenticatedUser()
            .onSuccess { user ->
                tokenManager.saveUsername(user.login)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    error = null
                )
            }
            .onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    error = "认证失败 / Authentication failed: ${e.message}"
                )
            }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAll()
            _uiState.value = AuthUiState()
        }
    }

    fun checkAuth(): Boolean = _uiState.value.isAuthenticated
}
