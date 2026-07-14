package com.journalog.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("Feed", NavRoutes.Feed.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("Explore", NavRoutes.Explore.route, Icons.Filled.Explore, Icons.Outlined.Explore),
    BottomNavItem("Messages", NavRoutes.Messenger.route, Icons.Filled.Chat, Icons.Outlined.Chat),
    BottomNavItem("Profile", NavRoutes.Profile.route, Icons.Filled.Person, Icons.Outlined.Person),
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    profileUsername: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = when (item.route) {
                NavRoutes.Profile.route -> currentRoute?.startsWith("profile") == true
                else -> currentRoute == item.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    val route = if (item.route == NavRoutes.Profile.route) {
                        NavRoutes.Profile.createRoute(profileUsername)
                    } else {
                        item.route
                    }
                    onItemSelected(route)
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }
}
