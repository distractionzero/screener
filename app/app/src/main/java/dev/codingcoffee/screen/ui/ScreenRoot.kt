package dev.codingcoffee.screen.ui

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.codingcoffee.screen.app

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    ACTIVITY("activity", "Activity", Icons.Rounded.History),
    RULES("rules", "Rules", Icons.Rounded.Tune),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings),
}

@Composable
fun ScreenRoot() {
    val app = LocalContext.current.app
    val settings by app.settings.collectAsStateWithLifecycle()
    val roleHeld = rememberRoleHeld()
    val loaded = settings ?: return

    LaunchedEffect(roleHeld) {
        if (roleHeld && !loaded.onboarded) app.settingsStore.setOnboarded()
    }

    if (loaded.onboarded || roleHeld) MainNav() else OnboardingScreen()
}

private fun ruleRoute(id: String? = null, pattern: String? = null, name: String? = null): String =
    "rule?id=${Uri.encode(id.orEmpty())}&pattern=${Uri.encode(pattern.orEmpty())}&name=${Uri.encode(name.orEmpty())}"

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (Tab.entries.any { it.route == route }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.ACTIVITY.route,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(Tab.ACTIVITY.route) {
                ActivityScreen(onOpenEntry = { nav.navigate("log/${Uri.encode(it)}") })
            }
            composable(Tab.RULES.route) {
                RulesScreen(onEdit = { nav.navigate(ruleRoute(id = it)) }, onAdd = { nav.navigate(ruleRoute()) })
            }
            composable(Tab.SETTINGS.route) { SettingsScreen() }
            composable("log/{id}") { entry ->
                LogDetailScreen(
                    id = entry.arguments?.getString("id"),
                    onBack = { nav.popBackStack() },
                    onCreateRule = { pattern, name -> nav.navigate(ruleRoute(pattern = pattern, name = name)) },
                )
            }
            composable(
                "rule?id={id}&pattern={pattern}&name={name}",
                arguments = listOf("id", "pattern", "name").map {
                    navArgument(it) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                },
            ) { entry ->
                val args = entry.arguments
                RuleEditScreen(
                    id = args?.getString("id")?.ifEmpty { null },
                    initialPattern = args?.getString("pattern"),
                    initialName = args?.getString("name"),
                    onDone = { nav.popBackStack() },
                )
            }
        }
    }
}
