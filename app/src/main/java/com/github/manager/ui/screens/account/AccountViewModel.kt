package com.github.manager.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.manager.data.local.TokenManager
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            gitHubRepository.getAuthenticatedUser()
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(user = user, isLoading = false)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAll()
        }
    }

    fun switchAccount(newToken: String) {
        viewModelScope.launch {
            tokenManager.clearAll()
            tokenManager.saveToken(newToken)
            loadProfile()
        }
    }
}
