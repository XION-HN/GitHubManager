package com.github.manager.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.manager.data.local.TokenManager
import com.github.manager.data.model.Repository
import com.github.manager.data.model.User
import com.github.manager.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val isRepoSearch: Boolean = true,
    val repos: List<Repository> = emptyList(),
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val totalCount: Int = 0,
    val searchHistory: List<String> = emptyList(),
    val showHistory: Boolean = true,
    val sortBy: String = "stars",
    val languageFilter: String? = null,
    val isOfflineFallback: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val gitHubRepository: GitHubRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    companion object {
        private const val MAX_HISTORY = 20
        private const val SEARCH_HISTORY_KEY = "search_history"
        private val historyEntries = mutableListOf<String>()
    }

    init {
        loadSearchHistory()
    }

    private fun loadSearchHistory() {
        viewModelScope.launch {
            val stored = tokenManager.loadCache(SEARCH_HISTORY_KEY)
            if (stored != null && stored.isNotBlank()) {
                historyEntries.clear()
                historyEntries.addAll(stored.split("||").filter { it.isNotBlank() })
                _uiState.value = _uiState.value.copy(searchHistory = historyEntries.toList())
            }
        }
    }

    private fun persistSearchHistory() {
        viewModelScope.launch {
            tokenManager.saveCache(SEARCH_HISTORY_KEY, historyEntries.joinToString("||"))
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                repos = emptyList(), users = emptyList(),
                isLoading = false, error = null, totalCount = 0,
                showHistory = true, isOfflineFallback = false
            )
            return
        }
        _uiState.value = _uiState.value.copy(showHistory = false)
        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.value = _uiState.value.copy(page = 1, hasMore = true)
            performSearch()
        }
    }

    fun toggleSearchType(isRepo: Boolean) {
        _uiState.value = _uiState.value.copy(
            isRepoSearch = isRepo, repos = emptyList(), users = emptyList(),
            page = 1, hasMore = true, totalCount = 0, isOfflineFallback = false
        )
        if (_uiState.value.query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch { performSearch() }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || !_uiState.value.hasMore) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(page = _uiState.value.page + 1)
            performSearch(append = true)
        }
    }

    fun setSortBy(sort: String) {
        _uiState.value = _uiState.value.copy(sortBy = sort)
        if (_uiState.value.query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                _uiState.value = _uiState.value.copy(page = 1, hasMore = true)
                performSearch()
            }
        }
    }

    fun setLanguageFilter(language: String?) {
        _uiState.value = _uiState.value.copy(languageFilter = language)
        if (_uiState.value.query.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch {
                _uiState.value = _uiState.value.copy(page = 1, hasMore = true)
                performSearch()
            }
        }
    }

    fun addToHistory(query: String) {
        if (query.isBlank()) return
        historyEntries.remove(query)
        historyEntries.add(0, query)
        if (historyEntries.size > MAX_HISTORY) {
            historyEntries.removeLast()
        }
        _uiState.value = _uiState.value.copy(searchHistory = historyEntries.toList())
        persistSearchHistory()
    }

    fun removeFromHistory(query: String) {
        historyEntries.remove(query)
        _uiState.value = _uiState.value.copy(searchHistory = historyEntries.toList())
        persistSearchHistory()
    }

    fun clearSearchHistory() {
        historyEntries.clear()
        _uiState.value = _uiState.value.copy(searchHistory = emptyList())
        persistSearchHistory()
    }

    fun selectHistoryItem(query: String) {
        onQueryChanged(query)
    }

    private suspend fun performSearch(append: Boolean = false) {
        val query = buildSearchQuery()
        if (query.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, isOfflineFallback = false)
        val page = _uiState.value.page
        if (_uiState.value.isRepoSearch) {
            gitHubRepository.searchRepositories(query, page = page)
                .onSuccess { result ->
                    val repos = if (append) _uiState.value.repos + (result.items ?: emptyList()) else (result.items ?: emptyList())
                    _uiState.value = _uiState.value.copy(
                        repos = repos,
                        isLoading = false,
                        totalCount = result.totalCount,
                        hasMore = repos.size < result.totalCount
                    )
                    addToHistory(_uiState.value.query.trim())
                }
                .onFailure {
                    val cachedRepos = gitHubRepository.searchReposInCache(_uiState.value.query.trim())
                    if (cachedRepos.isNotEmpty() && !append) {
                        _uiState.value = _uiState.value.copy(
                            repos = cachedRepos,
                            isLoading = false,
                            isOfflineFallback = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
                    }
                }
        } else {
            gitHubRepository.searchUsers(query, page = page)
                .onSuccess { result ->
                    val users = if (append) _uiState.value.users + (result.items ?: emptyList()) else (result.items ?: emptyList())
                    _uiState.value = _uiState.value.copy(
                        users = users,
                        isLoading = false,
                        totalCount = result.totalCount,
                        hasMore = users.size < result.totalCount
                    )
                    addToHistory(_uiState.value.query.trim())
                }
                .onFailure {
                    val cachedUsers = gitHubRepository.searchUsersInCache(_uiState.value.query.trim())
                    if (cachedUsers.isNotEmpty() && !append) {
                        _uiState.value = _uiState.value.copy(
                            users = cachedUsers,
                            isLoading = false,
                            isOfflineFallback = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(error = it.message, isLoading = false)
                    }
                }
        }
    }

    private fun buildSearchQuery(): String {
        val baseQuery = _uiState.value.query.trim()
        if (baseQuery.isBlank()) return ""
        val parts = mutableListOf(baseQuery)
        _uiState.value.languageFilter?.let { lang ->
            parts.add("language:$lang")
        }
        return parts.joinToString(" ")
    }
}
