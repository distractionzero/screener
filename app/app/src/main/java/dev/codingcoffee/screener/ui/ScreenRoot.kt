package dev.codingcoffee.screener.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.codingcoffee.screener.app
import kotlinx.coroutines.launch

private enum class Tab(val label: String, val icon: ImageVector) {
    ACTIVITY("Activity", Icons.Rounded.History),
    RULES("Rules", Icons.Rounded.Tune),
    STATS("Stats", Icons.Rounded.BarChart),
    SETTINGS("Settings", Icons.Rounded.Settings),
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

    NavHost(navController = nav, startDestination = "tabs") {
        composable("tabs") { Tabs(nav) }
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
        composable("privacy") { PrivacyPolicyScreen(onBack = { nav.popBackStack() }) }
        composable("licenses") { LicensesScreen(onBack = { nav.popBackStack() }) }
    }
}

@Composable
private fun Tabs(nav: NavHostController) {
    val pager = rememberPagerState { Tab.entries.size }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pager.currentPage != 0) {
        scope.launch { pager.animateScrollToPage(0) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = pager.currentPage == i,
                        onClick = { scope.launch { pager.animateScrollToPage(i) } },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pager,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) { page ->
            when (Tab.entries[page]) {
                Tab.ACTIVITY -> ActivityScreen(onOpenEntry = { nav.navigate("log/${Uri.encode(it)}") })
                Tab.STATS -> StatsScreen()
                Tab.RULES -> RulesScreen(
                    onEdit = { nav.navigate(ruleRoute(id = it)) },
                    onAdd = { nav.navigate(ruleRoute()) },
                )
                Tab.SETTINGS -> SettingsScreen(
                    onOpenPrivacy = { nav.navigate("privacy") },
                    onOpenLicenses = { nav.navigate("licenses") },
                )
            }
        }
    }
}
