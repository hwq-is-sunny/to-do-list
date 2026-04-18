package com.campus.todo.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.R
import com.campus.todo.data.db.entity.Task
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.TimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

private val PageBackground = Color(0xFF070B1A)
private val HeaderSubtle = Color(0xFFD7DDED)
private val HeaderStrong = Color(0xFFF5F7FD)
private val AccentOrange = Color(0xFFF68A46)
private val AccentPink = Color(0xFFE86ACF)
private val AccentPurple = Color(0xFF9F9EFF)
private val CardBase = Color(0xFF171D31)
private val CardMuted = Color(0xFF101628)

private data class HomeTaskVisual(
    val id: Long,
    val title: String,
    val subtitle: String,
    val timeLabel: String
)

@Composable
fun TodayScreen(
    factory: AppViewModelFactory,
    onOpenInbox: () -> Unit,
    onOpenAddCandidate: () -> Unit,
    vm: TodayViewModel = viewModel(factory = factory)
) {
    val locale = Locale.getDefault()
    val state by vm.state.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val dateStrip = remember(today) { (0..6).map { today.plusDays(it.toLong()) } }

    val realtimeTasks = remember(state.allPending, selectedEpochDay) {
        state.allPending
            .filter { it.dueAtEpoch?.toLocalDate() == selectedDate }
            .sortedBy { it.dueAtEpoch ?: Long.MAX_VALUE }
            .map { t ->
                HomeTaskVisual(
                    id = t.id,
                    title = t.title,
                    subtitle = "",
                    timeLabel = t.dueAtEpoch?.let { TimeUtils.formatEpoch(it).substringAfter(" ") } ?: "--:--"
                )
            }
    }
    val taskItems = if (realtimeTasks.isNotEmpty()) realtimeTasks else mockTasksForDate(selectedDate)
    val priorityItems = taskItems.take(5)
    val completedCount = max(1, taskItems.size / 3)
    val inProgressCount = max(1, taskItems.size - completedCount)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF060A19),
                        Color(0xFF0A1124),
                        Color(0xFF060B1D)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GreetingHeader(
                    name = "Maria",
                    monthLabel = selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
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
                ModeTabs()
            }
            item {
                HeroOverviewCard(
                    date = selectedDate,
                    count = taskItems.size,
                    onCheckNow = onOpenInbox
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PriorityPanel(
                        tasks = priorityItems,
                        modifier = Modifier.weight(1.14f)
                    )
                    Column(
                        modifier = Modifier.weight(0.86f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsPanel(
                            title = stringResource(R.string.today_completed_title),
                            detail = stringResource(R.string.today_completed_detail, completedCount, taskItems.size),
                            accent = Brush.linearGradient(listOf(AccentPink, AccentOrange)),
                            bars = listOf(24.dp, 12.dp, 26.dp, 9.dp, 22.dp)
                        )
                        StatsPanel(
                            title = stringResource(R.string.today_in_progress_title),
                            detail = stringResource(R.string.today_in_progress_detail, inProgressCount),
                            accent = Brush.linearGradient(listOf(AccentPurple, AccentOrange)),
                            bars = listOf(10.dp, 22.dp, 14.dp, 20.dp, 11.dp)
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.calendar_all_tasks_title),
                    color = HeaderStrong,
                    fontSize = 37.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0D1426), Color(0xFF0A0F20))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0x22FFFFFF),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        taskItems.take(5).forEach { task ->
                            AllTaskItem(
                                item = task,
                                onClick = onOpenInbox
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun GreetingHeader(name: String, monthLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.today_greeting),
                color = HeaderSubtle,
                fontSize = 35.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 38.sp
            )
            Text(
                text = name,
                color = HeaderStrong,
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 50.sp,
                letterSpacing = (-0.4).sp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoftIconCircle(Icons.Outlined.ChevronLeft)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFB7A9FF), Color(0xFF978CFC))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 9.dp)
            ) {
                Text(
                    text = monthLabel,
                    color = Color(0xFF241C48),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            SoftIconCircle(Icons.Outlined.ChevronRight)
        }
    }
}

@Composable
private fun SoftIconCircle(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0x11FFFFFF))
            .border(1.dp, Color(0x25FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFF1F4FF),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun DateStrip(
    items: List<LocalDate>,
    selected: LocalDate,
    onSelected: (LocalDate) -> Unit
) {
    val locale = Locale.getDefault()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { date ->
            val selectedState = date == selected
            val bg = if (selectedState) Color(0xFFF3F4F7) else Color(0xFFE5E8EF)
            val text = if (selectedState) Color(0xFF1B2133) else Color(0xFF5F667A)
            Column(
                modifier = Modifier
                    .width(54.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(bg)
                    .clickable { onSelected(date) }
                    .padding(horizontal = 6.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (selectedState) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AccentOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Text(
                        text = "${date.dayOfMonth}",
                        color = text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                    color = text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModeTabs() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.today_everyday),
                color = AccentOrange,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(130.dp)
                    .height(2.dp)
                    .background(AccentOrange)
            )
        }
        Text(
            text = stringResource(R.string.today_weekly),
            color = Color(0xFFB8BED2),
            fontSize = 34.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HeroOverviewCard(
    date: LocalDate,
    count: Int,
    onCheckNow: () -> Unit
) {
    val locale = Locale.getDefault()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFF8B078),
                        Color(0xFFC5C5FF),
                        Color(0xFFF6B8D5)
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(34.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x0FFFFFFF), Color(0x22FFFFFF))
                    )
                )
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${date.dayOfMonth} ${date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)}",
                color = Color(0xFF646070),
                fontSize = 14.sp
            )
            Text(
                text = stringResource(R.string.today_ai_analysis),
                color = Color(0xFF6D6874),
                fontSize = 18.sp
            )
            Text(
                text = stringResource(R.string.today_task_summary, count),
                color = Color.White,
                fontSize = 44.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xD9FFFFFF))
                .clickable(onClick = onCheckNow)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.today_check_now),
                color = Color(0xFF5F5C67),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PriorityPanel(tasks: List<HomeTaskVisual>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFC4EE8B), Color(0xFFABD277))
                )
            )
            .padding(horizontal = 14.dp, vertical = 15.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.today_priority_task),
                color = Color(0xFF182011),
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (tasks.isEmpty()) {
                Text(text = stringResource(R.string.today_no_task), color = Color(0xFF304023))
            } else {
                tasks.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0xFF4B5F36), CircleShape)
                                .background(if (index == 0) Color(0xFF2F3B20) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == 0) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(AccentOrange)
                                )
                            }
                        }
                        Text(
                            text = item.title,
                            color = Color(0xFF1B2A11),
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPanel(
    title: String,
    detail: String,
    accent: Brush,
    bars: List<androidx.compose.ui.unit.Dp>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardMuted)
            .border(1.dp, Color(0x23FFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(9.dp)
                            .height(h)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent)
                    )
                }
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 25.sp,
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
private fun AllTaskItem(item: HomeTaskVisual, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBase)
            .border(1.dp, Color(0x16FFFFFF), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.timeLabel,
            color = Color(0xFFC4CADC),
            fontSize = 16.sp,
            modifier = Modifier.width(58.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                color = Color(0xFF8B94AA),
                fontSize = 14.sp
            )
        }
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = null,
            tint = Color(0xFFDDE2F0),
            modifier = Modifier.size(21.dp)
        )
    }
}

private fun mockTasksForDate(date: LocalDate): List<HomeTaskVisual> {
    val seed = date.dayOfYear % 3
    return when (seed) {
        0 -> listOf(
            HomeTaskVisual(1001, "Design meeting", "UI Strategy", "08:00"),
            HomeTaskVisual(1002, "Prototype review", "Team Sync", "10:30"),
            HomeTaskVisual(1003, "Write summary", "Product notes", "16:30")
        )
        1 -> listOf(
            HomeTaskVisual(1004, "Marketing huddle", "Campaign room", "09:20"),
            HomeTaskVisual(1005, "Design assets export", "Priority", "12:10"),
            HomeTaskVisual(1006, "Wrap up report", "Status update", "18:00")
        )
        else -> listOf(
            HomeTaskVisual(1007, "Onboarding call", "Client success", "11:00"),
            HomeTaskVisual(1008, "Roadmap alignment", "Sprint board", "14:00"),
            HomeTaskVisual(1009, "Weekly plan", "Planning", "19:10")
        )
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
