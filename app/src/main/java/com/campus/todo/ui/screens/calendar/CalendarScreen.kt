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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.MinuteParse
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun CalendarScreen(
    factory: AppViewModelFactory,
    vm: CalendarViewModel = viewModel(factory = factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var filterMode by rememberSaveable { mutableStateOf(CalendarFilterMode.ALL) }
    var sortDescending by rememberSaveable { mutableStateOf(false) }
    var compactView by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val rawTimeline = buildTimelineItems(
        selectedDate = state.selected,
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item {
                CalendarHeader(
                    monthTitle = state.monthTitle,
                    menuExpanded = menuExpanded,
                    onMenuToggle = { menuExpanded = !menuExpanded },
                    onDismissMenu = { menuExpanded = false },
                    onMenuAction = { action ->
                        menuExpanded = false
                        when (action) {
                            "筛选" -> {
                                filterMode = filterMode.next()
                            }
                            "排序" -> {
                                sortDescending = !sortDescending
                            }
                            "切换视图" -> {
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
                TimelineContainer(items = timeline, compactView = compactView)
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    monthTitle: String,
    menuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onDismissMenu: () -> Unit,
    onMenuAction: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(40.dp))
            Text(
                text = "Calendar",
                color = Color(0xFFF2F5FC),
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x30FFFFFF), CircleShape)
                        .clickable(onClick = onMenuToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreHoriz,
                        contentDescription = "更多",
                        tint = Color(0xFFE7EBF7),
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = onDismissMenu,
                    offset = DpOffset(x = 6.dp, y = 8.dp),
                    shape = RoundedCornerShape(22.dp),
                    containerColor = Color(0xCC182137),
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(136.dp)
                ) {
                    DropdownMenuItem(
                        text = { MenuActionText("筛选") },
                        onClick = { onMenuAction("筛选") },
                        colors = MenuDefaults.itemColors(
                            textColor = Color(0xFFEAF0FF),
                            leadingIconColor = Color(0xFFEAF0FF),
                            trailingIconColor = Color(0xFFEAF0FF)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    )
                    DropdownMenuItem(
                        text = { MenuActionText("排序") },
                        onClick = { onMenuAction("排序") },
                        colors = MenuDefaults.itemColors(
                            textColor = Color(0xFFEAF0FF),
                            leadingIconColor = Color(0xFFEAF0FF),
                            trailingIconColor = Color(0xFFEAF0FF)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    )
                    DropdownMenuItem(
                        text = { MenuActionText("切换视图") },
                        onClick = { onMenuAction("切换视图") },
                        colors = MenuDefaults.itemColors(
                            textColor = Color(0xFFEAF0FF),
                            leadingIconColor = Color(0xFFEAF0FF),
                            trailingIconColor = Color(0xFFEAF0FF)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    )
                }
            }
        }
        Text(
            text = monthTitle,
            color = Color(0xFFF2914E),
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun MenuActionText(text: String) {
    Text(
        text = text,
        color = Color(0xFFEAF0FF),
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
    LazyRow(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 0.dp, end = 16.dp)
    ) {
        items(days) { day ->
            val selectedState = day == selected
            val outer = if (selectedState) Color(0xFFF0F2F6) else Color(0x31FFFFFF)
            val textColor = if (selectedState) Color(0xFF1A2133) else Color(0xFFE1E7F4)
            Column(
                modifier = Modifier
                    .width(58.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(outer)
                    .border(1.dp, if (selectedState) Color(0x30FFFFFF) else Color(0x24FFFFFF), RoundedCornerShape(24.dp))
                    .clickable { onDateClick(day) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (selectedState) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF68A46)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${day.dayOfMonth}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "${day.dayOfMonth}",
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (displayWeekCount) {
                    val weekNumber = day.get(weekFields.weekOfWeekBasedYear())
                    Text(
                        text = if (locale.language.startsWith("zh", ignoreCase = true)) {
                            "${weekNumber}周"
                        } else {
                            "W$weekNumber"
                        },
                        color = textColor.copy(alpha = 0.72f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineContainer(items: List<TimelineVisualItem>, compactView: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0E1528), Color(0xFF090F20))
                )
            )
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "All Tasks",
                color = Color(0xFFF2F5FC),
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold
            )
            items.forEachIndexed { index, item ->
                TimelineRow(
                    item = item,
                    isLast = index == items.lastIndex,
                    compactView = compactView
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(item: TimelineVisualItem, isLast: Boolean, compactView: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(108.dp)
                .padding(top = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = item.timeMark,
                color = Color(0xFFF0F4FF),
                fontSize = if (compactView) 24.sp else 30.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Start
            )
            if (item.subTime.isNotBlank()) {
                Text(
                    text = item.subTime,
                    color = Color(0xFF98A2BA),
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )
            }
        }
        Column(
            modifier = Modifier
                .width(16.dp)
                .padding(top = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0x80FFFFFF))
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .width(1.dp)
                    .height(if (isLast) 52.dp else if (compactView) 98.dp else 132.dp)
                    .background(Color(0x2EFFFFFF))
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x22FFFFFF))
            )
            CourseCard(item = item, compactView = compactView)
        }
    }
}

@Composable
private fun CourseCard(item: TimelineVisualItem, compactView: Boolean) {
    val shape = RoundedCornerShape(22.dp)
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
            .border(1.dp, Color(0x1AFFFFFF), shape)
            .clickable { }
            .padding(
                horizontal = if (compactView) 12.dp else 14.dp,
                vertical = if (compactView) 10.dp else 14.dp
            )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compactView) 3.dp else 5.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(horizontal = if (compactView) 8.dp else 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = item.tag,
                    color = item.tagColor,
                    fontSize = if (compactView) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = item.title,
                color = Color(0xFFFCFEFF),
                fontSize = if (compactView) 24.sp else 34.sp,
                lineHeight = if (compactView) 26.sp else 35.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.subtitle,
                color = Color(0xFFEAF0FF),
                fontSize = if (compactView) 13.sp else 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.location,
                color = Color(0xFFD6DDF0),
                fontSize = if (compactView) 12.sp else 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class TimelineVisualItem(
    val timeMark: String,
    val subTime: String,
    val title: String,
    val subtitle: String,
    val location: String,
    val tag: String,
    val tagColor: Color,
    val cardColor: Color,
    val itemType: TimelineItemType,
    val sortMinute: Int
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

private fun buildTimelineItems(
    selectedDate: LocalDate,
    slots: List<TimetableSlot>,
    tasks: List<Task>,
    coursesById: Map<Long, Course>
): List<TimelineVisualItem> {
    if (slots.isNotEmpty() || tasks.isNotEmpty()) {
        val slotItems = slots.mapIndexed { index, slot ->
            val course = coursesById[slot.courseId]
            val color = course?.colorArgb?.let { Color(it) } ?: demoPalette[index % demoPalette.size]
            TimelineVisualItem(
                timeMark = MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay),
                subTime = "${MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay)}-${MinuteParse.formatMinuteOfDay(slot.endMinuteOfDay)}",
                title = course?.name ?: "Course",
                subtitle = slot.note ?: "Teaching slot",
                location = slot.location ?: "D.2B Building of BME",
                tag = course?.code ?: "#EC${92000 + index}",
                tagColor = Color(0xFF6A5878),
                cardColor = color,
                itemType = TimelineItemType.COURSE,
                sortMinute = slot.startMinuteOfDay
            )
        }
        val taskItems = tasks.mapIndexed { index, task ->
            val minute = task.dueAtEpoch?.toMinuteOfDay() ?: (17 * 60 + 30 + index * 25)
            val mark = MinuteParse.formatMinuteOfDay(minute)
            TimelineVisualItem(
                timeMark = mark,
                subTime = "$mark Due",
                title = task.title,
                subtitle = "Task deadline",
                location = "From todo list",
                tag = "#TSK${1000 + index}",
                tagColor = Color(0xFF6A5878),
                cardColor = demoPalette[(index + 1) % demoPalette.size],
                itemType = TimelineItemType.TASK,
                sortMinute = minute
            )
        }
        return slotItems + taskItems
    }

    val seed = selectedDate.dayOfMonth % 3
    val list = when (seed) {
        0 -> listOf(
            mockItem("8:00", "8:30-9:30", "Project Managment", "Johnathon O'Connor", "D.2B Building of BME", "#EC936D24", demoPalette[0]),
            mockItem("13:00", "14:30-15:00", "Eesign Thinking", "Johnathon O'Connor", "D.2B Building of BME", "#EC920025", demoPalette[1]),
            mockItem("15:00", "15:10-16:40", "History of UX design", "Assignment Deadline", "Online submission", "#EC901100", demoPalette[2])
        )
        1 -> listOf(
            mockItem("8:00", "8:00-9:10", "Display Design", "Studio task", "Art Building A3", "#EC771200", demoPalette[2]),
            mockItem("13:00", "13:20-14:40", "Music Appreciation", "Prof. Eris", "Hall B1", "#EC882233", demoPalette[3]),
            mockItem("15:00", "15:20-16:20", "Tea Art Appreciation", "Class Meeting", "Room C2", "#EC553311", demoPalette[0])
        )
        else -> listOf(
            mockItem("8:00", "8:10-9:20", "Half-leng portrait sketch", "Chinese art history", "Sketch Studio", "#EC991122", demoPalette[1]),
            mockItem("13:00", "13:40-14:50", "Fine-line Sketching", "Critique session", "North Building", "#EC100245", demoPalette[0]),
            mockItem("15:00", "15:10-16:00", "Basics of Marketing", "Class Cancelled", "Notice board", "#EC777004", demoPalette[3])
        )
    }
    return list
}

private fun mockItem(
    time: String,
    subTime: String,
    title: String,
    subtitle: String,
    location: String,
    tag: String,
    color: Color
): TimelineVisualItem {
    val sortMinute = timeTextToMinute(time)
    return TimelineVisualItem(
        timeMark = time,
        subTime = subTime,
        title = title,
        subtitle = subtitle,
        location = location,
        tag = tag,
        tagColor = Color(0xFF765A83),
        cardColor = color,
        itemType = TimelineItemType.COURSE,
        sortMinute = sortMinute
    )
}

private val demoPalette = listOf(
    Color(0xFFB3D584),
    Color(0xFFB7AEF7),
    Color(0xFF63D0D0),
    Color(0xFF8ED9A9)
)

private fun Long.toMinuteOfDay(): Int {
    val localTime = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    return localTime.hour * 60 + localTime.minute
}

private fun timeTextToMinute(time: String): Int {
    return MinuteParse.parseMinuteOfDay(time) ?: 0
}
