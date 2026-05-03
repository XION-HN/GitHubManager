package com.github.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.manager.ui.screens.auth.AuthScreen
import com.github.manager.ui.screens.auth.AuthViewModel
import com.github.manager.ui.screens.repo.RepoDetailScreen
import com.github.manager.ui.screens.repo.RepoListScreen
import com.github.manager.ui.screens.repo.RepoListViewModel

object Routes {
    const val AUTH = "auth"
    const val REPO_LIST = "repoList"
    const val REPO_DETAIL = "repoDetail/{owner}/{repo}"

    fun repoDetail(owner: String, repo: String) = "repoDetail/$owner/$repo"
}

@Composable
fun GitHubNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.REPO_LIST) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }

        composable(Routes.REPO_LIST) {
            RepoListScreen(
                onRepoClick = { owner, repo ->
                    navController.navigate(Routes.repoDetail(owner, repo))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.REPO_DETAIL,
            arguments = listOf(
                navArgument("owner") { type = NavType.StringType },
                navArgument("repo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val owner = backStackEntry.arguments?.getString("owner") ?: ""
            val repo = backStackEntry.arguments?.getString("repo") ?: ""
            RepoDetailScreen(
                owner = owner,
                repo = repo,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
