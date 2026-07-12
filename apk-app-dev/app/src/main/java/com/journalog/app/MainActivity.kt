package com.journalog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.journalog.app.core.common.TokenManager
import com.journalog.app.core.designsystem.JournalogTheme
import com.journalog.app.core.network.ApiClient
import com.journalog.app.feature.auth.AuthScreen
import com.journalog.app.feature.feed.FeedScreen
import com.journalog.app.feature.explore.ExploreScreen
import com.journalog.app.feature.create.CreateScreen
import com.journalog.app.feature.messenger.MessengerScreen
import com.journalog.app.feature.messenger.ConversationScreen
import com.journalog.app.feature.profile.ProfileScreen
import com.journalog.app.feature.notifications.NotificationsScreen
import com.journalog.app.feature.settings.SettingsScreen
import com.journalog.app.navigation.BottomNavBar
import com.journalog.app.navigation.NavRoutes
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)

        setContent {
            JournalogTheme(darkTheme = false) {
                MainContent(tokenManager)
            }
        }
    }
}

@Composable
fun MainContent(tokenManager: TokenManager) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var isLoggedIn by remember { mutableStateOf(false) }
    var currentUsername by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val token = tokenManager.getToken()
        if (!token.isNullOrEmpty()) {
            ApiClient.setToken(token)
            isLoggedIn = true
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            tokenManager.usernameFlow.collect { username ->
                if (username != null) currentUsername = username
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = isLoggedIn && currentRoute != null && !currentRoute.startsWith("auth")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    profileUsername = currentUsername,
                    onItemSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) NavRoutes.Feed.route else NavRoutes.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Auth.route) {
                AuthScreen(
                    tokenManager = tokenManager,
                    onLoggedIn = {
                        isLoggedIn = true
                        navController.navigate(NavRoutes.Feed.route) {
                            popUpTo(NavRoutes.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.Feed.route) {
                FeedScreen(
                    onPostClick = { postId ->
                        navController.navigate(NavRoutes.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { username ->
                        navController.navigate(NavRoutes.Profile.createRoute(username))
                    }
                )
            }

            composable(NavRoutes.Explore.route) {
                ExploreScreen(
                    onPostClick = { postId ->
                        navController.navigate(NavRoutes.PostDetail.createRoute(postId))
                    },
                    onProfileClick = { username ->
                        navController.navigate(NavRoutes.Profile.createRoute(username))
                    }
                )
            }

            composable(NavRoutes.Create.route) {
                CreateScreen()
            }

            composable(NavRoutes.Messenger.route) {
                MessengerScreen(
                    onConversationClick = { userId, userName ->
                        navController.navigate(NavRoutes.Conversation.createRoute(userId, userName))
                    }
                )
            }

            composable(
                NavRoutes.Profile.route,
                arguments = listOf(navArgument("username") { type = NavType.StringType })
            ) { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username") ?: ""
                ProfileScreen(
                    username = username,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(NavRoutes.Settings.route) },
                    onNotificationsClick = { navController.navigate(NavRoutes.Notifications.route) },
                    onPostClick = { postId ->
                        navController.navigate(NavRoutes.PostDetail.createRoute(postId))
                    }
                )
            }

            composable(NavRoutes.Notifications.route) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }

            composable(NavRoutes.Settings.route) {
                SettingsScreen(
                    tokenManager = tokenManager,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        scope.launch {
                            tokenManager.clearSession()
                            ApiClient.setToken(null)
                            isLoggedIn = false
                            navController.navigate(NavRoutes.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                NavRoutes.PostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.IntType })
            ) {
                PostDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(
                NavRoutes.Conversation.route,
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                val userName = backStackEntry.arguments?.getString("userName") ?: ""
                ConversationScreen(
                    userId = userId,
                    userName = userName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PostDetailScreen(onBack: () -> Unit) {
    com.journalog.app.feature.feed.PostDetailScreen(onBack = onBack)
}
