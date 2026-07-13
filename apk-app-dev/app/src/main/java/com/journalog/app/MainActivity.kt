package com.journalog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
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
import com.journalog.app.core.debug.DebugOverlay
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
import com.journalog.app.feature.splash.SplashScreen
import com.journalog.app.feature.story.StoryCreateScreen
import com.journalog.app.feature.story.StoryViewerScreen
import com.journalog.app.feature.subscription.SubscriptionScreen
import com.journalog.app.navigation.BottomNavBar
import com.journalog.app.navigation.NavRoutes
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val launchToken = intent?.getStringExtra("auth_token")

        setContent {
            JournalogTheme(darkTheme = false) {
                MainContent(tokenManager, launchToken)
            }
        }
    }
}

@Composable
fun MainContent(tokenManager: TokenManager, launchToken: String? = null) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var currentUsername by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var storyViewerUserId by remember { mutableStateOf<Int?>(null) }
    var subscribeToUser by remember { mutableStateOf<com.journalog.app.data.remote.dto.UserDto?>(null) }

    LaunchedEffect(Unit) {
        tokenManager.usernameFlow.collect { username ->
            if (username != null) currentUsername = username
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != null
        && currentRoute != NavRoutes.Splash.route
        && currentRoute != NavRoutes.Auth.route
        && currentRoute != NavRoutes.Settings.route
        && currentRoute != NavRoutes.Notifications.route
        && currentRoute != NavRoutes.PostDetail.route
        && currentRoute != NavRoutes.StoryViewer.route
        && !currentRoute.startsWith("conversation")

    Box(modifier = Modifier.fillMaxSize()) {
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
            startDestination = NavRoutes.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Splash.route) {
                SplashScreen(
                    tokenManager = tokenManager,
                    authToken = launchToken,
                    onNavigateHome = {
                        scope.launch {
                            isAdmin = tokenManager.isAdmin()
                            navController.navigate(NavRoutes.Feed.route) {
                                popUpTo(NavRoutes.Splash.route) { inclusive = true }
                            }
                        }
                    },
                    onNavigateAuth = {
                        navController.navigate(NavRoutes.Auth.route) {
                            popUpTo(NavRoutes.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(NavRoutes.Auth.route) {
                AuthScreen(
                    tokenManager = tokenManager,
                    onLoggedIn = {
                        scope.launch {
                            isAdmin = tokenManager.isAdmin()
                            navController.navigate(NavRoutes.Feed.route) {
                                popUpTo(NavRoutes.Auth.route) { inclusive = true }
                            }
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
                    },
                    onStoryClick = { userId ->
                        storyViewerUserId = userId
                    },
                    onCreateStory = {
                        navController.navigate(NavRoutes.StoryCreate.route)
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
                    currentUsername = currentUsername,
                    onBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(NavRoutes.Settings.route) },
                    onNotificationsClick = { navController.navigate(NavRoutes.Notifications.route) },
                    onPostClick = { postId ->
                        navController.navigate(NavRoutes.PostDetail.createRoute(postId))
                    },
                    onSubscribeClick = { user ->
                        subscribeToUser = user
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
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId") ?: 0
                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() }
                )
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

            composable(NavRoutes.StoryCreate.route) {
                StoryCreateScreen(
                    tokenManager = tokenManager,
                    onBack = { navController.popBackStack() },
                    onStoryCreated = { navController.popBackStack() }
                )
            }
        }
    }

        storyViewerUserId?.let { userId ->
            StoryViewerScreen(
                userId = userId,
                onBack = { storyViewerUserId = null }
            )
        }

        subscribeToUser?.let { user ->
            SubscriptionScreen(
                creator = user,
                onBack = { subscribeToUser = null },
                onSubscribed = { subscribeToUser = null }
            )
        }

        DebugOverlay(isAdmin = isAdmin)
    }
}

@Composable
private fun PostDetailScreen(postId: Int, onBack: () -> Unit) {
    com.journalog.app.feature.feed.PostDetailScreen(postId = postId, onBack = onBack)
}
