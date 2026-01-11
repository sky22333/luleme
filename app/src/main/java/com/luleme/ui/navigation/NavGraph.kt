package com.luleme.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luleme.ui.screens.home.HomeScreen
import com.luleme.ui.screens.lock.LockScreen
import com.luleme.ui.screens.settings.SettingsScreen
import com.luleme.ui.screens.statistics.StatisticsScreen

@Composable
fun NavGraph(startDestination: String = Screen.Lock.route) {
    val navController = rememberNavController()

    val items = listOf(
        Screen.Home,
        Screen.Statistics,
        Screen.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show BottomBar only when not in Lock Screen
    val showBottomBar = currentDestination?.route != Screen.Lock.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    Screen.Home -> Icon(Icons.Default.Home, contentDescription = null)
                                    Screen.Statistics -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                                    Screen.Settings -> Icon(Icons.Default.Settings, contentDescription = null)
                                    else -> {}
                                }
                            },
                            label = {
                                when (screen) {
                                    Screen.Home -> Text("主页")
                                    Screen.Statistics -> Text("统计")
                                    Screen.Settings -> Text("设置")
                                    else -> {}
                                }
                            },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
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
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Lock.route) {
                LockScreen(
                    onUnlocked = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Lock.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Statistics.route) { StatisticsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
