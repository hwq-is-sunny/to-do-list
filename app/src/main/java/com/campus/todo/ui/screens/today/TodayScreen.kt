package com.campus.todo.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.TimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val HeaderSubtle = Color(0xFFD4DAEB)
private val HeaderStrong = Color(0xFFF5F7FD)
private val AccentOrange = Color(0xFFF68A46)
private val AccentPink = Color(0xFFE86ACF)
private val AccentPurple = Color(0xFF9F9EFF)
private val CardBase = Color(0xFF171D31)
private val CardMuted = Color(0xFF101628)

/** 问候语等与「校园场景」一致的展示时区（北京时间）。 */
private val BeijingZoneId: ZoneId = ZoneId.of("Asia/Shanghai")

private data class HomeTaskVisual(
    val id: Long,
    val title: String,
    val subtitle: String,
    val timeLabel: String,
)

private enum class HomeMode {
    EVERYDAY,
    WEEKLY
}

@Composable
fun TodayScreen(
    factory: AppViewModelFactory,
    onOpenInbox: () -> Unit,
    onOpenAddCandidate: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val vm: TodayViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsStateWithLifecycle(initialValue = TodayUiState(
        nickname = "",
        dayOfWeek = LocalDate.now().dayOfWeek.value,
        timetableSlots = emptyList(),
        coursesById = emptyMap(),
        tasksDueToday = emptyList(),
        upcomingTasks = emptyList(),
        allPending = emptyList(),
        allTasks = emptyList()
    ))
    val today = remember { LocalDate.now() }
    val thisMonth = remember(today) { YearMonth.from(today) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val targetMonth = remember(thisMonth, monthOffset) { thisMonth.plusMonths(monthOffset.toLong()) }
    val monthLabel = remember(targetMonth) {
        targetMonth.format(DateTimeFormatter.ofPattern("yyyy年M月"))
    }
    val defaultSelectedDate = remember(targetMonth, today, thisMonth) {
        if (targetMonth == thisMonth) today else targetMonth.atDay(1)
    }
    val dateStrip = remember(targetMonth) {
        (1..targetMonth.lengthOfMonth()).map { targetMonth.atDay(it) }
    }

    var selectedEpochDay by rememberSaveable { mutableLongStateOf(defaultSelectedDate.toEpochDay()) }
    LaunchedEffect(targetMonth) { selectedEpochDay = defaultSelectedDate.toEpochDay() }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    var mode by rememberSaveable { mutableStateOf(HomeMode.EVERYDAY) }
    val snackHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val taskSubtitle = "任务"
    val displayName = displayNameForGreeting(state.nickname)
    val greetingText = greetingByTime(LocalTime.now(BeijingZoneId))
    val dayStartSel = remember(selectedDate) { TimeUtils.startOfDayEpoch(selectedDate) }
    val dayEndSel = remember(selectedDate) { TimeUtils.endOfDayEpoch(selectedDate) }
    val weekRangeStart = remember(dateStrip) { TimeUtils.startOfDayEpoch(dateStrip.first()) }
    val weekRangeEnd = remember(dateStrip) { TimeUtils.endOfDayEpoch(dateStrip.last()) }

    val pendingForSelection = remember(state.allPending, mode, selectedDate, dateStrip) {
        if (mode == HomeMode.EVERYDAY) {
            state.allPending.filter { it.dueAtEpoch?.toLocalDate() == selectedDate }
        } else {
            val start = TimeUtils.startOfDayEpoch(dateStrip.first())
            val end = TimeUtils.endOfDayEpoch(dateStrip.last())
            state.allPending.filter { due ->
                val epoch = due.dueAtEpoch
                epoch != null && epoch in start..end
            }
        }
    }

    val doneForSelection = remember(state.allTasks, mode, dayStartSel, dayEndSel, weekRangeStart, weekRangeEnd) {
        if (mode == HomeMode.EVERYDAY) {
            state.allTasks.filter { t ->
                t.status == TaskStatus.DONE &&
                    t.completedAtEpoch != null &&
                    t.completedAtEpoch in dayStartSel..dayEndSel
            }
        } else {
            state.allTasks.filter { t ->
                t.status == TaskStatus.DONE &&
                    t.completedAtEpoch != null &&
                    t.completedAtEpoch in weekRangeStart..weekRangeEnd
            }
        }
    }

    val taskItems = remember(pendingForSelection, taskSubtitle) {
        pendingForSelection.sortedBy { it.dueAtEpoch ?: Long.MAX_VALUE }.map { task ->
            HomeTaskVisual(
                id = task.id,
                title = task.title,
                subtitle = taskSubtitle,
                timeLabel = task.dueAtEpoch?.let { TimeUtils.formatEpoch(it).substringAfter(" ") } ?: "--:--",
            )
        }
    }

    val priorityItems = remember(pendingForSelection, taskSubtitle) {
        pendingForSelection
            .sortedWith(compareByDescending<Task> { it.urgency.ordinal }.thenBy { it.dueAtEpoch ?: Long.MAX_VALUE })
            .take(4)
            .map { task ->
                HomeTaskVisual(
                    id = task.id,
                    title = task.title,
                    subtitle = taskSubtitle,
                    timeLabel = task.dueAtEpoch?.let { TimeUtils.formatEpoch(it).substringAfter(" ") } ?: "--:--",
                )
            }
    }

    val doneCount = doneForSelection.size
    val pendingCount = pendingForSelection.size
    val totalForStats = (pendingCount + doneCount).coerceAtLeast(1)
    val barHeightsDone = remember(doneCount) { List(5) { i -> (8 + (doneCount + i) * 3).coerceAtMost(28).dp } }
    val barHeightsPending = remember(pendingCount) { List(5) { i -> (8 + (pendingCount + i) * 3).coerceAtMost(28).dp } }

    var editingTask by remember { mutableStateOf<HomeTaskVisual?>(null) }
    var editingTitle by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF060A19), Color(0xFF0A1124), Color(0xFF060B1D))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GreetingHeader(
                    greeting = greetingText,
                    name = displayName,
                    monthLabel = monthLabel,
                    onPrev = { monthOffset -= 1 },
                    onNext = { monthOffset += 1 }
                )
            }
            item {
                DateStrip(
                    items = dateStrip,
                    selected = selectedDate,
                    onSelected = { selectedEpochDay = it.toEpochDay() }
                )
            }
            item {
                ModeTabs(
                    selected = mode,
                    onSelect = { mode = it }
                )
            }
            item {
                HeroOverviewCard(
                    date = selectedDate,
                    pendingCount = pendingCount,
                    doneCount = doneCount,
                    onCheckNow = {
                        scope.launch {
                            snackHost.showSnackbar(
                                message = "当前视图：待办 ${pendingCount} 项，已完成 ${doneCount} 项。"
                            )
                        }
                    }
                )
            }
            item {
                val moduleHeight = 276.dp
                val rightGap = 10.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(moduleHeight),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PriorityPanel(
                        tasks = priorityItems,
                        onOpenTask = onOpenTask,
                        onMarkDone = { id -> vm.markDone(id) },
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.8f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(rightGap)
                    ) {
                        StatsPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            title = "已完成",
                            detail = "$doneCount / $totalForStats 项",
                            accent = Brush.linearGradient(listOf(AccentPink, AccentOrange)),
                            bars = barHeightsDone,
                            showProgressCircle = true
                        )
                        StatsPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            title = "待处理",
                            detail = "$pendingCount 项",
                            accent = Brush.linearGradient(listOf(AccentPurple, AccentOrange)),
                            bars = barHeightsPending,
                            showProgressCircle = false
                        )
                    }
                }
            }
            item {
                Text(
                    text = "全部任务",
                    color = HeaderStrong,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF0D1426), Color(0xFF0A0F20))))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(28.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        if (taskItems.isEmpty()) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "当前视图下暂无待办。",
                                    color = Color(0xFF9AA5BF),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "打开候选箱",
                                    color = Color(0xFFF08B4A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { onOpenInbox() }
                                )
                                Text(
                                    text = "导入候选项",
                                    color = Color(0xFFF08B4A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { onOpenAddCandidate() }
                                )
                            }
                        } else {
                            taskItems.take(6).forEach { task ->
                                AllTaskItem(
                                    item = task,
                                    onOpen = { onOpenTask(task.id) },
                                    onMarkDone = { vm.markDone(task.id) },
                                    onEdit = {
                                        editingTask = task
                                        editingTitle = task.title
                                    },
                                    onDelete = { vm.deleteTask(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 94.dp)
        )
    }

    if (editingTask != null) {
        AlertDialog(
            onDismissRequest = { editingTask = null },
            title = { Text("编辑任务标题", color = Color(0xFFEAF0FF)) },
            text = {
                TextField(
                    value = editingTitle,
                    onValueChange = { editingTitle = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1B2338),
                        unfocusedContainerColor = Color(0xFF1B2338),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFFEAF0FF),
                        unfocusedTextColor = Color(0xFFEAF0FF),
                        cursorColor = Color(0xFFEAF0FF)
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val task = editingTask ?: return@TextButton
                        vm.renameTask(task.id, editingTitle)
                        editingTask = null
                    }
                ) {
                    Text(text = "保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTask = null }) {
                    Text(text = "取消")
                }
            },
            containerColor = Color(0xFF131B2E)
        )
    }
}

@Composable
private fun GreetingHeader(
    greeting: String,
    name: String,
    monthLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = greeting,
                color = HeaderSubtle,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 30.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = name,
                    color = HeaderStrong,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 46.sp,
                    letterSpacing = (-0.35).sp
                )
                Text(text = "\uD83D\uDC4B", fontSize = 22.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoftIconCircle(icon = Icons.Outlined.ChevronLeft, onClick = onPrev)
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFD0C8FF), Color(0xFFB9AFFF))))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = monthLabel,
                    color = Color(0xFF2F2857),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            SoftIconCircle(icon = Icons.Outlined.ChevronRight, onClick = onNext)
        }
    }
}

@Composable
private fun SoftIconCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0x10FFFFFF))
            .border(1.dp, Color(0x24FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFF1F4FF),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DateStrip(
    items: List<LocalDate>,
    selected: LocalDate,
    onSelected: (LocalDate) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(items, selected) {
        val index = items.indexOf(selected).coerceAtLeast(0)
        listState.scrollToItem(index)
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { date ->
            val selectedState = date == selected
            val bg = if (selectedState) Color(0xFFF2F4F8) else Color(0xFFE5E9F1)
            val textColor = if (selectedState) Color(0xFF1D2336) else Color(0xFF606B81)
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(bg)
                    .clickable { onSelected(date) }
                    .padding(horizontal = 6.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (selectedState) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AccentOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Text(
                        text = "${date.dayOfMonth}",
                        color = textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = weekLabel(date),
                    color = textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModeTabs(
    selected: HomeMode,
    onSelect: (HomeMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeTabItem(
            text = "每日",
            selected = selected == HomeMode.EVERYDAY,
            onClick = { onSelect(HomeMode.EVERYDAY) }
        )
        ModeTabItem(
            text = "每周",
            selected = selected == HomeMode.WEEKLY,
            onClick = { onSelect(HomeMode.WEEKLY) }
        )
    }
}

@Composable
private fun ModeTabItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = if (selected) AccentOrange else Color(0xFFA9B2C9),
            fontSize = 31.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .width(if (selected) 102.dp else 92.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) AccentOrange else Color.Transparent)
        )
    }
}

@Composable
private fun HeroOverviewCard(
    date: LocalDate,
    pendingCount: Int,
    doneCount: Int,
    onCheckNow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF8B078), Color(0xFFC6C8FF), Color(0xFFF5B6D2))
                )
            )
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(34.dp))
                .background(Brush.verticalGradient(listOf(Color(0x11FFFFFF), Color(0x28FFFFFF))))
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "${date.dayOfMonth} ${weekLabel(date)}",
                color = Color(0xFF5F5867),
                fontSize = 13.sp
            )
            Text(
                text = "今日一览",
                color = Color(0xFF6D6874),
                fontSize = 17.sp
            )
            Text(
                text = "待办 ${pendingCount} 项 · 已完成 ${doneCount} 项",
                color = Color.White,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.25).sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "紧急",
                    color = Color(0xFF5D5864),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xDAFFFFFF))
                .clickable(onClick = onCheckNow)
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = "立即查看",
                color = Color(0xFF5D5965),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PriorityPanel(
    tasks: List<HomeTaskVisual>,
    onOpenTask: (Long) -> Unit,
    onMarkDone: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardHorizontalPadding = 14.dp
    val cardVerticalPadding = 12.dp
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFC6ED90), Color(0xFFAFD47D))))
            .padding(horizontal = cardHorizontalPadding, vertical = cardVerticalPadding)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "优先任务",
                color = Color(0xFF1A2413),
                fontSize = 27.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (tasks.isEmpty()) {
                Text(
                    text = "暂无任务",
                    color = Color(0xFF304023),
                    fontSize = 13.sp
                )
            } else {
                tasks.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.title,
                            color = Color(0xFF1B2A11),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenTask(item.id) }
                        )
                        Text(
                            text = "完成",
                            color = Color(0xFF3D4E2A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMarkDone(item.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPanel(
    modifier: Modifier = Modifier,
    title: String,
    detail: String,
    accent: Brush,
    bars: List<androidx.compose.ui.unit.Dp>,
    showProgressCircle: Boolean
) {
    val cardHorizontalPadding = 14.dp
    val cardVerticalPadding = 12.dp
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CardMuted)
            .border(1.dp, Color(0x23FFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = cardHorizontalPadding, vertical = cardVerticalPadding)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.height(26.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                if (showProgressCircle) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0x50FFFFFF), CircleShape)
                            .background(Color(0x13FFFFFF))
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                bars.forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(h)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent)
                    )
                }
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                color = Color(0xFFADB6CC),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AllTaskItem(
    item: HomeTaskVisual,
    onOpen: () -> Unit,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = item.timeLabel,
            color = Color(0xFFC8CFE2),
            fontSize = 15.sp,
            modifier = Modifier.width(46.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(CardBase)
                .border(1.dp, Color(0x16FFFFFF), RoundedCornerShape(18.dp))
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    color = Color(0xFF8B94AA),
                    fontSize = 13.sp
                )
            }
            Box {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    tint = Color(0xFFDDE2F0),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { menuExpanded = true }
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("标记完成") },
                        onClick = {
                            menuExpanded = false
                            onMarkDone()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun weekLabel(date: LocalDate): String {
    return when (date.dayOfWeek.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }
}

private fun displayNameForGreeting(rawNickname: String?): String {
    val trimmed = rawNickname?.trim()
    if (trimmed.isNullOrBlank()) return "同学"
    return trimmed
}

private fun greetingByTime(now: LocalTime): String {
    val hour = now.hour
    return when {
        hour < 12 -> "早上好，"
        hour < 17 -> "下午好，"
        else -> "晚上好，"
    }
}
