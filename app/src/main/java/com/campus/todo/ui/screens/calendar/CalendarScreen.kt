package com.campus.todo.ui.screens.calendar

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.MinuteParse
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private val CalendarMenuContainerColor = Color(0xFF131B2E)
private val CalendarMenuTextColor = Color(0xFFF2F5FC)
private val CalendarMenuBorderColor = Color(0x24FFFFFF)

@Composable
fun CalendarScreen(
    factory: AppViewModelFactory,
    onOpenTask: (Long) -> Unit,
    onOpenCourse: (Long) -> Unit,
) {
    val vm: CalendarViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    var filterMode by rememberSaveable { mutableStateOf(CalendarFilterMode.ALL) }
    var sortDescending by rememberSaveable { mutableStateOf(false) }
    var compactView by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedTimelineId by rememberSaveable { mutableStateOf<String?>(null) }
    val rawTimeline = buildTimelineItems(
        slots = state.selectedDaySlots,
        tasks = state.selectedDayTasks,
        coursesById = state.coursesById
    )
    val filteredTimeline = when (filterMode) {
        CalendarFilterMode.ALL -> rawTimeline
        CalendarFilterMode.COURSES_ONLY -> rawTimeline.filter { it.itemType == TimelineItemType.COURSE }
        CalendarFilterMode.TASKS_ONLY -> rawTimeline.filter { it.itemType == TimelineItemType.TASK }
    }
    val timeline = if (sortDescending) {
        filteredTimeline.sortedByDescending { it.sortMinute }
    } else {
        filteredTimeline.sortedBy { it.sortMinute }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF060A19),
                        Color(0xFF0A1022),
                        Color(0xFF070B1A)
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CalendarHeader(
                    monthTitle = state.monthTitle,
                    menuExpanded = menuExpanded,
                    onPreviousMonth = vm::previousMonth,
                    onNextMonth = vm::nextMonth,
                    onMenuToggle = { menuExpanded = !menuExpanded },
                    onDismissMenu = { menuExpanded = false },
                    onMenuAction = { action ->
                        menuExpanded = false
                        when (action) {
                            CalendarMenuAction.FILTER -> {
                                filterMode = filterMode.next()
                            }
                            CalendarMenuAction.SORT -> {
                                sortDescending = !sortDescending
                            }
                            CalendarMenuAction.TOGGLE_VIEW -> {
                                compactView = !compactView
                            }
                        }
                    }
                )
            }
            item {
                CalendarDateStrip(
                    days = state.dateStrip,
                    selected = state.selected,
                    displayWeekCount = state.displayWeekCount,
                    onDateClick = vm::selectDate
                )
            }
            item {
                TimelineContainer(
                    items = timeline,
                    compactView = compactView,
                    selectedItemId = selectedTimelineId,
                    onItemOpen = { item ->
                        selectedTimelineId = item.id
                        item.taskId?.let { onOpenTask(it); return@TimelineContainer }
                        item.courseId?.let { onOpenCourse(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    monthTitle: String,
    menuExpanded: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMenuToggle: () -> Unit,
    onDismissMenu: () -> Unit,
    onMenuAction: (CalendarMenuAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(34.dp))
            Text(
                text = "日程",
                color = Color(0xFFF2F5FC),
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
            Box {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x24FFFFFF), CircleShape)
                        .clickable(onClick = onMenuToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreHoriz,
                        contentDescription = "更多",
                        tint = Color(0xFFE7EBF7),
                        modifier = Modifier.size(20.dp)
                    )
                }
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        surface = CalendarMenuContainerColor
                    )
                ) {
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = onDismissMenu,
                        offset = DpOffset(x = 6.dp, y = 8.dp),
                        modifier = Modifier
                            .width(136.dp)
                            .border(1.dp, CalendarMenuBorderColor, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { MenuActionText("筛选") },
                            onClick = { onMenuAction(CalendarMenuAction.FILTER) },
                            colors = MenuDefaults.itemColors(
                                textColor = CalendarMenuTextColor,
                                leadingIconColor = CalendarMenuTextColor,
                                trailingIconColor = CalendarMenuTextColor,
                                disabledTextColor = CalendarMenuTextColor,
                                disabledLeadingIconColor = CalendarMenuTextColor,
                                disabledTrailingIconColor = CalendarMenuTextColor
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        )
                        DropdownMenuItem(
                            text = { MenuActionText("排序") },
                            onClick = { onMenuAction(CalendarMenuAction.SORT) },
                            colors = MenuDefaults.itemColors(
                                textColor = CalendarMenuTextColor,
                                leadingIconColor = CalendarMenuTextColor,
                                trailingIconColor = CalendarMenuTextColor,
                                disabledTextColor = CalendarMenuTextColor,
                                disabledLeadingIconColor = CalendarMenuTextColor,
                                disabledTrailingIconColor = CalendarMenuTextColor
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        )
                        DropdownMenuItem(
                            text = { MenuActionText("切换视图") },
                            onClick = { onMenuAction(CalendarMenuAction.TOGGLE_VIEW) },
                            colors = MenuDefaults.itemColors(
                                textColor = CalendarMenuTextColor,
                                leadingIconColor = CalendarMenuTextColor,
                                trailingIconColor = CalendarMenuTextColor,
                                disabledTextColor = CalendarMenuTextColor,
                                disabledLeadingIconColor = CalendarMenuTextColor,
                                disabledTrailingIconColor = CalendarMenuTextColor
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        )
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MonthSwitchIcon(icon = Icons.Outlined.ChevronLeft, onClick = onPreviousMonth)
            Text(
                text = monthTitle,
                color = Color(0xFFF2914E),
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
            MonthSwitchIcon(icon = Icons.Outlined.ChevronRight, onClick = onNextMonth)
        }
    }
}

@Composable
private fun MonthSwitchIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x24FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFE7EBF7),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun MenuActionText(text: String) {
    Text(
        text = text,
        color = CalendarMenuTextColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun CalendarDateStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    displayWeekCount: Boolean,
    onDateClick: (LocalDate) -> Unit
) {
    val locale = Locale.getDefault()
    val weekFields = WeekFields.of(locale)
    val listState = rememberLazyListState()
    LaunchedEffect(days, selected) {
        val index = days.indexOf(selected).coerceAtLeast(0)
        listState.scrollToItem(index)
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 0.dp, end = 16.dp)
    ) {
        items(days) { day ->
            val selectedState = day == selected
            val outer = if (selectedState) Color(0xFFF0F2F6) else Color(0x22FFFFFF)
            val textColor = if (selectedState) Color(0xFF1A2133) else Color(0xFFD8DFEF)
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(outer)
                    .border(1.dp, if (selectedState) Color(0x30FFFFFF) else Color(0x18FFFFFF), RoundedCornerShape(20.dp))
                    .clickable { onDateClick(day) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selectedState) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF68A46)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${day.dayOfMonth}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "${day.dayOfMonth}",
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = shortWeekLabel(day),
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                if (displayWeekCount) {
                    val weekNumber = day.get(weekFields.weekOfWeekBasedYear())
                    Text(
                        text = "${weekNumber}周",
                        color = textColor.copy(alpha = 0.72f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineContainer(
    items: List<TimelineVisualItem>,
    compactView: Boolean,
    selectedItemId: String?,
    onItemOpen: (TimelineVisualItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E1528), Color(0xFF090F20))
                )
            )
            .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(28.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "时间轴",
                color = Color(0xFFF2F5FC),
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (items.isEmpty()) {
                Text(
                    text = "当天暂无课表时段与待办任务。",
                    color = Color(0xFF9AA5BF),
                    fontSize = 14.sp
                )
            }
            items.forEachIndexed { index, item ->
                TimelineRow(
                    item = item,
                    isLast = index == items.lastIndex,
                    compactView = compactView,
                    selected = item.id == selectedItemId,
                    onClick = { onItemOpen(item) }
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineVisualItem,
    isLast: Boolean,
    compactView: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(72.dp)
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = item.timeMark,
                color = Color(0xFFF0F4FF),
                fontSize = if (compactView) 16.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Start
            )
            if (item.subTime.isNotBlank()) {
                Text(
                    text = item.subTime,
                    color = Color(0xFF98A2BA),
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )
            }
        }
        Column(
            modifier = Modifier
                .width(12.dp)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0x80FFFFFF))
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(1.dp)
                    .height(if (isLast) 38.dp else if (compactView) 66.dp else 78.dp)
                    .background(Color(0x26FFFFFF))
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x22FFFFFF))
            )
            CourseCard(
                item = item,
                compactView = compactView,
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun CourseCard(
    item: TimelineVisualItem,
    compactView: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        item.cardColor.copy(alpha = 0.96f),
                        item.cardColor.copy(alpha = 0.88f)
                    )
                )
            )
            .border(
                width = if (selected) 1.2.dp else 1.dp,
                color = if (selected) Color(0x66FFFFFF) else Color(0x1AFFFFFF),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compactView) 11.dp else 12.dp,
                vertical = if (compactView) 8.dp else 10.dp
            )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compactView) 2.dp else 4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = if (compactView) 7.dp else 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.tag,
                    color = item.tagColor,
                    fontSize = if (compactView) 10.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = item.title,
                color = Color(0xFFFCFEFF),
                fontSize = if (compactView) 15.sp else 17.sp,
                lineHeight = if (compactView) 18.sp else 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                color = Color(0xFFEAF0FF),
                fontSize = if (compactView) 11.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.location,
                color = Color(0xFFD6DDF0),
                fontSize = if (compactView) 10.sp else 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class TimelineVisualItem(
    val id: String,
    val timeMark: String,
    val subTime: String,
    val title: String,
    val subtitle: String,
    val location: String,
    val tag: String,
    val tagColor: Color,
    val cardColor: Color,
    val itemType: TimelineItemType,
    val sortMinute: Int,
    val taskId: Long? = null,
    val courseId: Long? = null,
)

private enum class TimelineItemType {
    COURSE,
    TASK
}

private enum class CalendarFilterMode {
    ALL,
    COURSES_ONLY,
    TASKS_ONLY;

    fun next(): CalendarFilterMode = when (this) {
        ALL -> COURSES_ONLY
        COURSES_ONLY -> TASKS_ONLY
        TASKS_ONLY -> ALL
    }
}

private enum class CalendarMenuAction {
    FILTER,
    SORT,
    TOGGLE_VIEW
}

private fun buildTimelineItems(
    slots: List<TimetableSlot>,
    tasks: List<Task>,
    coursesById: Map<Long, Course>
): List<TimelineVisualItem> {
    val slotItems = slots.mapIndexed { index, slot ->
        val course = coursesById[slot.courseId]
        val color = course?.colorArgb?.let { Color(it) } ?: demoPalette[index % demoPalette.size]
        TimelineVisualItem(
            id = "slot-${slot.id}",
            timeMark = MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay),
            subTime = "${MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay)}-${MinuteParse.formatMinuteOfDay(slot.endMinuteOfDay)}",
            title = course?.name ?: "课程",
            subtitle = slot.note?.takeIf { it.isNotBlank() } ?: "课表",
            location = slot.location ?: "—",
            tag = course?.code ?: "课程",
            tagColor = Color(0xFF6A5878),
            cardColor = color,
            itemType = TimelineItemType.COURSE,
            sortMinute = slot.startMinuteOfDay,
            taskId = null,
            courseId = slot.courseId
        )
    }
    val taskItems = tasks.mapIndexed { index, task ->
        val minute = taskTimelineMinute(task)
        val mark = MinuteParse.formatMinuteOfDay(minute)
        TimelineVisualItem(
            id = "task-${task.id}",
            timeMark = mark,
            subTime = "$mark · ${taskTypeSubtitle(task.taskType)}",
            title = task.title,
            subtitle = task.courseName ?: "待办",
            location = task.location ?: "—",
            tag = "任务",
            tagColor = Color(0xFF6A5878),
            cardColor = demoPalette[(index + 1) % demoPalette.size],
            itemType = TimelineItemType.TASK,
            sortMinute = minute,
            taskId = task.id,
            courseId = null
        )
    }
    return (slotItems + taskItems).sortedBy { it.sortMinute }
}

private fun taskTypeSubtitle(type: TaskType): String = when (type) {
    TaskType.HOMEWORK -> "作业"
    TaskType.EXAM -> "考试"
    TaskType.SIGN_IN -> "签到"
    TaskType.CLASS -> "课程"
    TaskType.ANNOUNCEMENT -> "通知"
    TaskType.PERSONAL -> "个人"
    TaskType.OTHER -> "待办"
}

private fun taskTimelineMinute(task: Task): Int {
    val z = ZoneId.systemDefault()
    task.startAtEpoch?.let {
        val t = Instant.ofEpochMilli(it).atZone(z).toLocalTime()
        return t.hour * 60 + t.minute
    }
    task.dueAtEpoch?.let {
        val t = Instant.ofEpochMilli(it).atZone(z).toLocalTime()
        return t.hour * 60 + t.minute
    }
    return 18 * 60
}

private val demoPalette = listOf(
    Color(0xFFB3D584),
    Color(0xFFB7AEF7),
    Color(0xFF63D0D0),
    Color(0xFF8ED9A9)
)

private fun shortWeekLabel(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    else -> "周日"
}
