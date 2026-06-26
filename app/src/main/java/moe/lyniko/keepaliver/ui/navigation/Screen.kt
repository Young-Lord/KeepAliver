package moe.lyniko.keepaliver.ui.navigation

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Editor : Screen("editor/{entryId}") {
        fun createRoute(entryId: Long) = "editor/$entryId"
    }
    object Settings : Screen("settings")
    object AppList : Screen("appList") {
        fun createRoute() = "appList"
    }
    object ActivityList : Screen("activityList?packageName={packageName}") {
        fun createRoute(packageName: String) = "activityList?packageName=$packageName"
    }
}
