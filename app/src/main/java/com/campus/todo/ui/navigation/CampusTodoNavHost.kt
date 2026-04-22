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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.campus.todo.R
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.ui.screens.auth.LoginScreen
import com.campus.todo.ui.screens.calendar.CalendarScreen
import com.campus.todo.ui.screens.courses.CourseDetailScreen
import com.campus.todo.ui.screens.courses.CourseListScreen
import com.campus.todo.ui.screens.courses.TimetableImportScreen
import com.campus.todo.ui.screens.inbox.AddCandidateScreen
import com.campus.todo.ui.screens.inbox.CandidateDetailScreen
import com.campus.todo.ui.screens.inbox.InboxScreen
import com.campus.todo.ui.screens.settings.SettingsScreen
import com.campus.todo.ui.screens.tasks.QuickTaskScreen
import com.campus.todo.ui.screens.tasks.TaskDetailScreen
import com.campus.todo.ui.screens.today.TodayScreen

@OptIn(ExperimentalMaterial3Api::class)
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
        initialStart = if (app.sessionStore.isLoggedIn()) {
            NavRoutes.TODAY
        } else {
            NavRoutes.LOGIN
        }
    }

    if (initialStart == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDest = initialStart ?: return
    var showQuickCreateSheet by remember { mutableStateOf(false) }
    val loggedIn by app.sessionStore.loggedInFlow.collectAsState(initial = false)

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

    LaunchedEffect(loggedIn, route) {
        if (loggedIn && route == NavRoutes.LOGIN) {
            navController.navigate(NavRoutes.TODAY) {
                popUpTo(NavRoutes.LOGIN) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
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
                            popUpTo(NavRoutes.TODAY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCenterAction = { showQuickCreateSheet = true }
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
                    onLoggedIn = { }
                )
            }
            composable(NavRoutes.TODAY) { entry ->
                val todayFactory = remember(entry) {
                    AppViewModelFactory(entry, entry.arguments, app)
                }
                TodayScreen(
                    factory = todayFactory,
                    onOpenInbox = {
                        navController.navigate(NavRoutes.INBOX) {
                            popUpTo(NavRoutes.TODAY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenAddCandidate = { navController.navigate(NavRoutes.ADD_CANDIDATE) },
                    onOpenTask = { id -> navController.navigate(NavRoutes.task(id)) }
                )
            }
            composable(NavRoutes.CALENDAR) { entry ->
                val calFactory = remember(entry) {
                    AppViewModelFactory(entry, entry.arguments, app)
                }
                CalendarScreen(
                    factory = calFactory,
                    onOpenTask = { id -> navController.navigate(NavRoutes.task(id)) },
                    onOpenCourse = { id -> navController.navigate(NavRoutes.course(id)) }
                )
            }
            composable(NavRoutes.COURSES) {
                CourseListScreen(
                    factory = activityFactory,
                    onCourseClick = { id -> navController.navigate(NavRoutes.course(id)) },
                    onOpenTask = { id -> navController.navigate(NavRoutes.task(id)) }
                )
            }
            composable(NavRoutes.TIMETABLE_IMPORT) { entry ->
                val importFactory = remember(entry) {
                    AppViewModelFactory(entry, entry.arguments, app)
                }
                TimetableImportScreen(
                    factory = importFactory,
                    onBack = { navController.popBackStack() }
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
            composable(NavRoutes.ADD_CANDIDATE) { entry ->
                val entryFactory = remember(entry) {
                    AppViewModelFactory(entry, entry.arguments, app)
                }
                AddCandidateScreen(
                    factory = entryFactory,
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.popBackStack()
                        navController.navigate(NavRoutes.candidate(id))
                    }
                )
            }
            composable(NavRoutes.QUICK_TASK) { entry ->
                val qf = remember(entry) {
                    AppViewModelFactory(entry, entry.arguments, app)
                }
                QuickTaskScreen(
                    factory = qf,
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.popBackStack()
                        navController.navigate(NavRoutes.task(id))
                    }
                )
            }
            composable(
                NavRoutes.TASK,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) { entry ->
                val tf = remember(entry) {
                    AppViewModelFactory(entry, entry.arguments, app)
                }
                TaskDetailScreen(
                    factory = tf,
                    onBack = { navController.popBackStack() }
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

    if (showQuickCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickCreateSheet = false },
            containerColor = Color(0xFF121A2D)
        ) {
            QuickActionRow(
                title = stringResource(R.string.quick_action_new_task),
                onClick = {
                    showQuickCreateSheet = false
                    navController.navigate(NavRoutes.QUICK_TASK)
                }
            )
            QuickActionRow(
                title = stringResource(R.string.quick_action_import_notice),
                onClick = {
                    showQuickCreateSheet = false
                    navController.navigate(NavRoutes.ADD_CANDIDATE)
                }
            )
            QuickActionRow(
                title = stringResource(R.string.quick_action_import_timetable),
                onClick = {
                    showQuickCreateSheet = false
                    navController.navigate(NavRoutes.TIMETABLE_IMPORT)
                }
            )
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun QuickActionRow(
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFFEAF0FF)
        )
    }
}

@Composable
private fun PremiumBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onCenterAction: () -> Unit
) {
    val barShape = RoundedCornerShape(38.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .shadow(24.dp, barShape, clip = false)
                .clip(barShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xD92A324A), Color(0xCC171D30))
                    )
                )
                .border(1.dp, Color(0x28FFFFFF), barShape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(Color(0x22FFFFFF))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Spacer(modifier = Modifier.width(84.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                .offset(y = (-12).dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x331F2539))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp)
                .size(64.dp)
                .shadow(18.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFB66C), Color(0xFFF08940))
                    )
                )
                .border(1.2.dp, Color(0x52FFFFFF), CircleShape)
                .clickable(onClick = onCenterAction),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "新增",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
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
    val tint = if (selected) Color(0xFFFFA05A) else Color(0xFFD9E0F1)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(48.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) Color(0x1B11182B) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Color(0x2DFFFFFF) else Color.Transparent,
                shape = RoundedCornerShape(17.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (selected) {
                        Brush.verticalGradient(listOf(Color(0x20FFB273), Color(0x08FFB273)))
                    } else {
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(if (selected) 21.dp else 20.dp)
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 13.dp else 0.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) Color(0x70FFA460) else Color.Transparent)
        )
    }
}
