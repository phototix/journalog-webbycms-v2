package com.journalog.app.navigation

sealed class NavRoutes(val route: String) {
    data object Auth : NavRoutes("auth")
    data object Feed : NavRoutes("feed")
    data object Explore : NavRoutes("explore")
    data object Create : NavRoutes("create")
    data object Messenger : NavRoutes("messenger")
    data object Profile : NavRoutes("profile/{username}") {
        fun createRoute(username: String) = "profile/$username"
    }
    data object Notifications : NavRoutes("notifications")
    data object Settings : NavRoutes("settings")
    data object PostDetail : NavRoutes("post/{postId}") {
        fun createRoute(postId: Int) = "post/$postId"
    }
    data object Conversation : NavRoutes("conversation/{userId}/{userName}") {
        fun createRoute(userId: Int, userName: String) = "conversation/$userId/$userName"
    }
}
