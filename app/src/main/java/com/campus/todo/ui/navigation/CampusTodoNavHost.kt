package com.campus.todo.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.campus.todo.ui.screens.auth.LoginScreen
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

    var initialStart by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        initialStart = NavRoutes.TODAY
    }

    if (initialStart == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDest = initialStart!!

    var pendingOpenCandidate by remember { mutableStateOf(openCandidateId) }
    LaunchedEffect(pendingOpenCandidate, startDest) {
        if (startDest != NavRoutes.TODAY) return@LaunchedEffect
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
                PremiumBottomBar(
                    currentRoute = route,
                    onNavigate = { destination ->
                        navController.navigate(destination) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCenterAction = { navController.navigate(NavRoutes.ADD_CANDIDATE) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.LOGIN) {
                LoginScreen(
                    factory = activityFactory,
                    onLoggedIn = {
                        navController.navigate(NavRoutes.TODAY) {
                            popUpTo(NavRoutes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.TODAY) {
                TodayScreen(
                    factory = activityFactory,
                    onOpenInbox = {
                        navController.navigate(NavRoutes.INBOX) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
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
                SettingsScreen(
                    factory = activityFactory,
                    onLogout = {
                        navController.navigate(NavRoutes.LOGIN) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
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
                    onCourseDeleted = { navController.popBackStack() },
                    vm = viewModel(entry, factory = entryFactory)
                )
            }
        }
    }
}

@Composable
private fun PremiumBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCenterAction: () -> Unit
) {
    val barShape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(20.dp, barShape, clip = false)
                .clip(barShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xDE232A3F), Color(0xD1121729))
                    )
                )
                .border(1.dp, Color(0x26FFFFFF), barShape)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BottomIcon(
                        selected = currentRoute == NavRoutes.TODAY,
                        icon = Icons.Outlined.Home,
                        onClick = { onNavigate(NavRoutes.TODAY) }
                    )
                    BottomIcon(
                        selected = currentRoute == NavRoutes.CALENDAR,
                        icon = Icons.Outlined.CalendarMonth,
                        onClick = { onNavigate(NavRoutes.CALENDAR) }
                    )
                }
                Spacer(modifier = Modifier.width(74.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BottomIcon(
                        selected = currentRoute == NavRoutes.COURSES,
                        icon = Icons.Outlined.MenuBook,
                        onClick = { onNavigate(NavRoutes.COURSES) }
                    )
                    BottomIcon(
                        selected = currentRoute == NavRoutes.SETTINGS,
                        icon = Icons.Outlined.Settings,
                        onClick = { onNavigate(NavRoutes.SETTINGS) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-20).dp)
                .size(68.dp)
                .shadow(16.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFB56A), Color(0xFFF1873F))
                    )
                )
                .border(1.5.dp, Color(0x4DFFFFFF), CircleShape)
                .clickable(onClick = onCenterAction),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "新增",
                tint = Color.White,
                modifier = Modifier.size(29.dp)
            )
        }
    }
}

@Composable
private fun BottomIcon(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val tint = if (selected) Color(0xFFFF9D58) else Color(0xFFD6DDEF)
    val selectedBackground = Brush.verticalGradient(
        listOf(Color(0x26FFAF72), Color(0x10FFAF72))
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(46.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0x14FFAA69) else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (selected) selectedBackground else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(if (selected) 22.dp else 21.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 14.dp else 0.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) Color(0x66FFA25C) else Color.Transparent)
        )
    }
}
