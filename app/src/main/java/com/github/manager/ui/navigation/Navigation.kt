package com.github.manager.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.manager.ui.screens.account.AccountScreen
import com.github.manager.ui.screens.auth.AuthScreen
import com.github.manager.ui.screens.auth.AuthViewModel
import com.github.manager.ui.screens.repo.RepoDetailScreen
import com.github.manager.ui.screens.repo.RepoListScreen

object Routes {
    const val AUTH = "auth"
    const val REPO_LIST = "repoList"
    const val ACCOUNT = "account"
    const val REPO_DETAIL = "repoDetail/{owner}/{repo}"

    fun repoDetail(owner: String, repo: String) = "repoDetail/$owner/$repo"
}

private const val ANIM_DURATION = 350

@Composable
fun GitHubNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated && navController.currentDestination?.route == Routes.AUTH) {
            navController.navigate(Routes.REPO_LIST) {
                popUpTo(Routes.AUTH) { inclusive = true }
            }
        } else if (!uiState.isAuthenticated && navController.currentDestination?.route != Routes.AUTH) {
            navController.navigate(Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 3 },
                animationSpec = tween(ANIM_DURATION)
            ) + fadeIn(animationSpec = tween(ANIM_DURATION))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(ANIM_DURATION)
            ) + fadeOut(animationSpec = tween(ANIM_DURATION))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(ANIM_DURATION)
            ) + fadeIn(animationSpec = tween(ANIM_DURATION))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 3 },
                animationSpec = tween(ANIM_DURATION)
            ) + fadeOut(animationSpec = tween(ANIM_DURATION))
        }
    ) {
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
                onAccountClick = {
                    navController.navigate(Routes.ACCOUNT)
                },
                onLogout = {
                    authViewModel.logout()
                }
            )
        }

        composable(Routes.ACCOUNT) {
            AccountScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
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
