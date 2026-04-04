package com.campus.todo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.CampusTodoApp
import com.campus.todo.MainActivity
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.ui.screens.calendar.CalendarScreen
import com.campus.todo.ui.screens.courses.CourseDetailScreen
import com.campus.todo.ui.screens.courses.CourseListScreen
import com.campus.todo.ui.screens.inbox.AddCandidateScreen
import com.campus.todo.ui.screens.inbox.CandidateDetailScreen
import com.campus.todo.ui.screens.inbox.InboxScreen
import com.campus.todo.ui.screens.settings.SettingsScreen
import com.campus.todo.ui.screens.today.TodayScreen

@Composable
fun CampusTodoNavHost(
    app: CampusTodoApp,
    openCandidateId: Long?
) {
    val navController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity
    val activityFactory = remember(activity) {
        AppViewModelFactory(activity, activity.intent.extras, app)
    }

    var pendingOpenCandidate by remember { mutableStateOf(openCandidateId) }
    LaunchedEffect(pendingOpenCandidate) {
        val id = pendingOpenCandidate ?: return@LaunchedEffect
        navController.navigate(NavRoutes.candidate(id)) {
            launchSingleTop = true
        }
        pendingOpenCandidate = null
        activity.intent.removeExtra(MainActivity.EXTRA_OPEN_CANDIDATE)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val showBottomBar = route in setOf(
        NavRoutes.TODAY,
        NavRoutes.CALENDAR,
        NavRoutes.COURSES,
        NavRoutes.INBOX,
        NavRoutes.SETTINGS
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == NavRoutes.TODAY,
                        onClick = {
                            navController.navigate(NavRoutes.TODAY) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                        label = { Text("今日") }
                    )
                    NavigationBarItem(
                        selected = route == NavRoutes.CALENDAR,
                        onClick = {
                            navController.navigate(NavRoutes.CALENDAR) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
                        label = { Text("日历") }
                    )
                    NavigationBarItem(
                        selected = route == NavRoutes.COURSES,
                        onClick = {
                            navController.navigate(NavRoutes.COURSES) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.MenuBook, contentDescription = null) },
                        label = { Text("课程") }
                    )
                    NavigationBarItem(
                        selected = route == NavRoutes.INBOX,
                        onClick = {
                            navController.navigate(NavRoutes.INBOX) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.Inbox, contentDescription = null) },
                        label = { Text("候选") }
                    )
                    NavigationBarItem(
                        selected = route == NavRoutes.SETTINGS,
                        onClick = {
                            navController.navigate(NavRoutes.SETTINGS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text("设置") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.TODAY,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.TODAY) {
                TodayScreen(
                    factory = activityFactory,
                    onOpenAddCandidate = { navController.navigate(NavRoutes.ADD_CANDIDATE) }
                )
            }
            composable(NavRoutes.CALENDAR) {
                CalendarScreen(factory = activityFactory)
            }
            composable(NavRoutes.COURSES) {
                CourseListScreen(
                    factory = activityFactory,
                    onCourseClick = { id -> navController.navigate(NavRoutes.course(id)) }
                )
            }
            composable(NavRoutes.INBOX) {
                InboxScreen(
                    factory = activityFactory,
                    onOpenCandidate = { id -> navController.navigate(NavRoutes.candidate(id)) },
                    onAdd = { navController.navigate(NavRoutes.ADD_CANDIDATE) }
                )
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(factory = activityFactory)
            }
            composable(NavRoutes.ADD_CANDIDATE) {
                AddCandidateScreen(
                    factory = activityFactory,
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.popBackStack()
                        navController.navigate(NavRoutes.candidate(id))
                    }
                )
            }
            composable(
                NavRoutes.CANDIDATE,
                arguments = listOf(navArgument("candidateId") { type = NavType.LongType })
            ) { entry ->
                val entryFactory = remember(entry) {
                    AppViewModelFactory(entry, entry.arguments, app)
                }
                CandidateDetailScreen(
                    factory = entryFactory,
                    onBack = { navController.popBackStack() },
                    vm = viewModel(entry, factory = entryFactory)
                )
            }
            composable(
                NavRoutes.COURSE,
                arguments = listOf(navArgument("courseId") { type = NavType.LongType })
            ) { entry ->
                val entryFactory = remember(entry) {
                    AppViewModelFactory(entry, entry.arguments, app)
                }
                CourseDetailScreen(
                    factory = entryFactory,
                    onBack = { navController.popBackStack() },
                    vm = viewModel(entry, factory = entryFactory)
                )
            }
        }
    }
}
