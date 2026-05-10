package com.github.manager.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.github.manager.data.model.Repository
import com.github.manager.data.model.User
import com.github.manager.ui.i18n.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onRepoClick: (owner: String, repo: String) -> Unit,
    onUserClick: (username: String) -> Unit = {},
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showSortDialog) {
        SortDialog(
            currentSort = uiState.sortBy,
            onSortSelected = { viewModel.setSortBy(it); showSortDialog = false },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguageFilterDialog(
            currentLanguage = uiState.languageFilter,
            onLanguageSelected = { viewModel.setLanguageFilter(it); showLanguageDialog = false },
            onDismiss = { showLanguageDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = { Text(getText(I18nStrings.enterSearchQuery), style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (uiState.isRepoSearch && uiState.query.isNotBlank()) {
                        IconButton(onClick = { showSortDialog = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        IconButton(onClick = { showLanguageDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.isRepoSearch,
                    onClick = { viewModel.toggleSearchType(true) },
                    label = {
                        when (languageModeState.value) {
                            LanguageMode.CHINESE -> Text(I18nStrings.searchRepos.zh, style = MaterialTheme.typography.bodySmall)
                            LanguageMode.ENGLISH -> Text(I18nStrings.searchRepos.en, style = MaterialTheme.typography.bodySmall)
                            LanguageMode.BILINGUAL -> Column {
                                Text(I18nStrings.searchRepos.zh, fontSize = 11.sp)
                                Text(I18nStrings.searchRepos.en, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = !uiState.isRepoSearch,
                    onClick = { viewModel.toggleSearchType(false) },
                    label = {
                        when (languageModeState.value) {
                            LanguageMode.CHINESE -> Text(I18nStrings.searchUsers.zh, style = MaterialTheme.typography.bodySmall)
                            LanguageMode.ENGLISH -> Text(I18nStrings.searchUsers.en, style = MaterialTheme.typography.bodySmall)
                            LanguageMode.BILINGUAL -> Column {
                                Text(I18nStrings.searchUsers.zh, fontSize = 11.sp)
                                Text(I18nStrings.searchUsers.en, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            if (uiState.languageFilter != null && uiState.isRepoSearch) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { viewModel.setLanguageFilter(null) },
                        label = { Text(uiState.languageFilter ?: "", style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                }
            }

            if (uiState.isOfflineFallback) {
                Text(
                    text = if (languageModeState.value == LanguageMode.ENGLISH)
                        "Offline results from cache"
                    else "离线缓存结果",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            if (uiState.totalCount > 0) {
                Text(
                    text = "${uiState.totalCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            }

            if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onQueryChanged(uiState.query) }) { Text(getText(I18nStrings.retry)) }
                    }
                }
            } else if (uiState.query.isBlank() && uiState.showHistory && uiState.searchHistory.isNotEmpty()) {
                SearchHistoryList(
                    history = uiState.searchHistory,
                    onSelect = viewModel::selectHistoryItem,
                    onRemove = viewModel::removeFromHistory,
                    onClearAll = viewModel::clearSearchHistory
                )
            } else if (uiState.query.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(16.dp))
                        BilingualLabelSmall(I18nStrings.enterSearchQuery)
                    }
                }
            } else if (uiState.isLoading && uiState.repos.isEmpty() && uiState.users.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.isRepoSearch) {
                if (uiState.repos.isEmpty() && !uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BilingualLabelSmall(I18nStrings.noSearchResults)
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.repos, key = { it.id }) { repo ->
                            SearchRepoItem(repo = repo, onClick = { onRepoClick(repo.owner.login, repo.name) })
                        }
                        if (uiState.hasMore && !uiState.isLoading) {
                            item {
                                LaunchedEffect(Unit) { viewModel.loadMore() }
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                if (uiState.users.isEmpty() && !uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BilingualLabelSmall(I18nStrings.noSearchResults)
                    }
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.users, key = { it.id }) { user ->
                            SearchUserItem(user = user, onClick = { onUserClick(user.login) })
                        }
                        if (uiState.hasMore && !uiState.isLoading) {
                            item {
                                LaunchedEffect(Unit) { viewModel.loadMore() }
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryList(
    history: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val historyLabel = if (languageModeState.value == LanguageMode.ENGLISH) "Recent Searches" else "搜索历史"
            Text(historyLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClearAll) {
                val clearLabel = if (languageModeState.value == LanguageMode.ENGLISH) "Clear All" else "清除全部"
                Text(clearLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(history, key = { it }) { query ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(query) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        query,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemove(query) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortDialog(
    currentSort: String,
    onSortSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sortOptions = listOf("stars" to BilingualText("按星标", "By Stars"), "forks" to BilingualText("按复刻", "By Forks"), "updated" to BilingualText("按更新", "Recently Updated"))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { val t = if (languageModeState.value == LanguageMode.ENGLISH) "Sort By" else "排序方式"; Text(t) },
        text = {
            Column {
                sortOptions.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortSelected(value) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentSort == value, onClick = { onSortSelected(value) })
                        Spacer(modifier = Modifier.width(8.dp))
                        BilingualLabel(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { BilingualLabel(I18nStrings.cancel) } }
    )
}

@Composable
private fun LanguageFilterDialog(
    currentLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(null to BilingualText("全部语言", "All Languages")) +
            listOf("Kotlin", "Java", "Python", "JavaScript", "TypeScript", "Go", "Rust", "C++", "C", "Swift", "Dart", "Ruby", "PHP").map {
                it to BilingualText(it, it)
            }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { val t = if (languageModeState.value == LanguageMode.ENGLISH) "Filter by Language" else "按语言筛选"; Text(t) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(languages) { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(value) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentLanguage == value, onClick = { onLanguageSelected(value) })
                        Spacer(modifier = Modifier.width(8.dp))
                        BilingualLabel(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { BilingualLabel(I18nStrings.cancel) } }
    )
}

@Composable
private fun SearchRepoItem(repo: Repository, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = repo.fullName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            repo.description?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repo.language?.let { lang ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(lang, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFC107))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${repo.stargazersCount}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SearchUserItem(user: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name ?: user.login,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${user.login}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                user.bio?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${user.followers}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
