package moe.lyniko.keepaliver.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import moe.lyniko.keepaliver.KeepAliverApp
import moe.lyniko.keepaliver.ui.applist.ActivityListScreen
import moe.lyniko.keepaliver.ui.applist.AppListScreen
import moe.lyniko.keepaliver.ui.editor.EditorScreen
import moe.lyniko.keepaliver.ui.editor.EditorViewModel
import moe.lyniko.keepaliver.ui.main.MainScreen
import moe.lyniko.keepaliver.ui.main.MainViewModel
import moe.lyniko.keepaliver.ui.settings.SettingsScreen
import moe.lyniko.keepaliver.ui.settings.SettingsViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    val app = LocalContext.current.applicationContext as KeepAliverApp

    NavHost(
        navController = navController,
        startDestination = Screen.Main.route,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Screen.Main.route) {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(app.repository)
            )
            MainScreen(
                viewModel = viewModel,
                onNavigateToEditor = { id ->
                    navController.navigate(Screen.Editor.createRoute(id))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("entryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong("entryId") ?: -1L
            val viewModel: EditorViewModel = viewModel(
                factory = EditorViewModel.Factory(app, app.repository, app.settingsStore, entryId)
            )

            // Handle picker results from AppList/ActivityList
            val savedStateHandle = backStackEntry.savedStateHandle
            LaunchedEffect(savedStateHandle) {
                savedStateHandle.get<String>("picked_package")?.let { pkg ->
                    viewModel.updateTargetPackage(pkg)
                    savedStateHandle.remove<String>("picked_package")
                }
            }
            LaunchedEffect(savedStateHandle) {
                savedStateHandle.get<String>("picked_class")?.let { cls ->
                    viewModel.updateTargetClass(cls)
                    savedStateHandle.remove<String>("picked_class")
                }
            }

            EditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onPickPackage = {
                    navController.navigate(Screen.AppList.createRoute())
                },
                onPickActivity = { pkg ->
                    navController.navigate(Screen.ActivityList.createRoute(pkg))
                }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.settingsStore)
            )
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AppList.route) {
            AppListScreen(
                onNavigateBack = { navController.popBackStack() },
                onAppPicked = { packageName ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_package", packageName)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ActivityList.route,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            ActivityListScreen(
                packageName = packageName,
                onNavigateBack = { navController.popBackStack() },
                onActivityPicked = { pkg, cls ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_package", pkg)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("picked_class", cls)
                    navController.popBackStack()
                }
            )
        }
    }
}
