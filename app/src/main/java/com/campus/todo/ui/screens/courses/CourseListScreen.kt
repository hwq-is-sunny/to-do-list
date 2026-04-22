package com.campus.todo.ui.screens.courses

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.ui.AppViewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CourseListScreen(
    factory: AppViewModelFactory,
    onCourseClick: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    vm: CourseListViewModel = viewModel(factory = factory)
) {
    val courses by vm.courses.collectAsStateWithLifecycle()
    val slots by vm.timetableSlots.collectAsStateWithLifecycle()
    val tasks by vm.pendingTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var keyword by rememberSaveable { mutableStateOf("") }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val currentMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val monthLabel = currentMonth.format(DateTimeFormatter.ofPattern("yyyy年M月"))

    val scheduleByDay = remember(currentMonth, slots, tasks, courses) {
        buildScheduleFromData(currentMonth, slots, tasks, courses)
    }
    val detailItems = remember(scheduleByDay, selectedDate, keyword) {
        val all = scheduleByDay[selectedDate].orEmpty()
        if (keyword.isBlank()) all else all.filter {
            it.title.contains(keyword, ignoreCase = true) ||
                it.subtitle.contains(keyword, ignoreCase = true)
        }
    }
    val dateMarkers = remember(scheduleByDay) { scheduleByDay.keys.toSet() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF060A19), Color(0xFF091022), Color(0xFF070B1A))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TopActionRow(
                    keyword = keyword,
                    onKeywordChange = { keyword = it },
                    onDone = {
                        val hint = keyword.ifBlank { "（显示全部）" }
                        Toast.makeText(
                            context,
                            "已按关键词筛选：$hint",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
            item {
                MonthCalendarSection(
                    month = currentMonth,
                    monthLabel = monthLabel,
                    selectedDate = selectedDate,
                    markers = dateMarkers,
                    onSelectDate = { selectedEpochDay = it.toEpochDay() }
                )
            }
            item {
                CourseListOverviewCard(
                    courseNames = remember(courses) {
                        courses.sortedBy { it.name }.map { it.name }
                    }
                )
            }
            item {
                DetailPanel(
                    date = selectedDate,
                    details = detailItems,
                    onOpenCourse = onCourseClick,
                    onOpenTask = onOpenTask
                )
            }
        }
    }
}

@Composable
private fun TopActionRow(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = keyword,
                    onValueChange = onKeywordChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "请输入关键词搜索",
                            color = Color(0xFF96A1BA),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFFD0D9ED),
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0x101B243A),
                        unfocusedContainerColor = Color(0x101B243A),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFFE9EEFB),
                        unfocusedTextColor = Color(0xFFE9EEFB),
                        cursorColor = Color(0xFFE9EEFB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Color(0x4F7F8DD1), RoundedCornerShape(22.dp))
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF39C58), Color(0xFFE66CD0))))
                    .clickable(onClick = onDone)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "完成",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MonthCalendarSection(
    month: YearMonth,
    monthLabel: String,
    selectedDate: LocalDate,
    markers: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "月度课程",
            color = Color(0xFFE9EEFB),
            fontSize = 36.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = monthLabel,
            color = Color(0xFFF18D4A),
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")
            weekdays.forEach {
                Text(text = it, color = Color(0xFFB7C0D8), fontSize = 17.sp, modifier = Modifier.width(40.dp))
            }
        }

        val cells = remember(month) { monthCalendarCells(month) }
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { day ->
                    val date = day.date
                    if (date == null) {
                        Spacer(modifier = Modifier.size(40.dp))
                    } else {
                        val isSelected = date == selectedDate
                        val hasMarker = markers.contains(date)
                        val isToday = date == LocalDate.now()
                        val textColor = if (isSelected) Color.White else Color(0xFFC7CFE4)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> Color(0xFFF48A45)
                                        isToday -> Color(0xFFDE72C4)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (!isSelected && hasMarker) 1.dp else 0.dp,
                                    color = if (hasMarker) Color(0xFFF2914E) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onSelectDate(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${date.dayOfMonth}",
                                color = textColor,
                                fontSize = 19.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseListOverviewCard(courseNames: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2E3450), Color(0xFF232838))))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "已添加课程（来自数据库）",
                color = Color(0xFFE9EEFB),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (courseNames.isEmpty()) {
                Text(
                    text = "暂无课程。请点底部加号 →「导入课表」手动录入或图片识别，保存后即显示在此。",
                    color = Color(0xFFB7C0D8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            } else {
                Text(
                    text = "共 ${courseNames.size} 门",
                    color = Color(0xFFF18D4A),
                    fontSize = 13.sp
                )
                courseNames.forEach { name ->
                    Text(
                        text = "· $name",
                        color = Color(0xFFD0D9ED),
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailPanel(
    date: LocalDate,
    details: List<DayDetail>,
    onOpenCourse: (Long) -> Unit,
    onOpenTask: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF787A86), Color(0xFF696C78)))
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(62.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x742A2A2A))
            )
            Text(
                text = date.format(
                    DateTimeFormatter.ofPattern("M月d日", Locale.getDefault())
                ),
                color = Color(0xFFF7F9FF),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (details.isEmpty()) {
                Text(
                    text = "当天暂无日程",
                    color = Color(0xFFDEE5F8),
                    fontSize = 14.sp
                )
            } else {
                details.forEach { detail ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x1FFFFFFF))
                            .clickable {
                                when {
                                    detail.taskId != null -> onOpenTask(detail.taskId)
                                    detail.courseId > 0 -> onOpenCourse(detail.courseId)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.width(74.dp)) {
                            Text(detail.start, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(detail.end, color = Color(0xFFDDE3F3), fontSize = 13.sp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(detail.dotColor)
                                )
                                Text(
                                    detail.title,
                                    color = Color(0xFFF7FAFF),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(detail.subtitle, color = Color(0xFFE1E8FA), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class CalendarCell(val date: LocalDate?)

private data class DayDetail(
    val start: String,
    val end: String,
    val title: String,
    val subtitle: String,
    val dotColor: Color,
    val courseId: Long = -1,
    val taskId: Long? = null
)

private fun monthCalendarCells(month: YearMonth): List<CalendarCell> {
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()
    val cells = mutableListOf<CalendarCell>()
    repeat(leading) { cells += CalendarCell(null) }
    for (day in 1..daysInMonth) {
        cells += CalendarCell(month.atDay(day))
    }
    while (cells.size < 42) cells += CalendarCell(null)
    return cells
}

private fun buildScheduleFromData(
    month: YearMonth,
    slots: List<TimetableSlot>,
    tasks: List<Task>,
    courses: List<Course>
): Map<LocalDate, List<DayDetail>> {
    if (slots.isEmpty() && tasks.isEmpty()) return emptyMap()
    val zone = ZoneId.systemDefault()
    val courseMap = courses.associateBy { it.id }
    val details = mutableMapOf<LocalDate, MutableList<DayDetail>>()

    val first = month.atDay(1)
    val last = month.atEndOfMonth()
    var day = first
    while (!day.isAfter(last)) {
        val daySlots = slots.filter { it.dayOfWeek == day.dayOfWeek.value }
        if (daySlots.isNotEmpty()) {
            val mapped = daySlots.map { slot ->
                val course = courseMap[slot.courseId]
                DayDetail(
                    start = minuteLabel(slot.startMinuteOfDay),
                    end = minuteLabel(slot.endMinuteOfDay),
                    title = course?.name ?: "课程",
                    subtitle = slot.location ?: "教室待定",
                    dotColor = Color(course?.colorArgb ?: 0xFFB5A9F8.toInt()),
                    courseId = slot.courseId,
                    taskId = null
                )
            }
            details.getOrPut(day) { mutableListOf() }.addAll(mapped)
        }
        day = day.plusDays(1)
    }

    tasks.forEach { task ->
        val due = task.dueAtEpoch ?: return@forEach
        val date = Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
        if (date.month != month.month || date.year != month.year) return@forEach
        val time = Instant.ofEpochMilli(due).atZone(zone).toLocalTime()
        details.getOrPut(date) { mutableListOf() }.add(
            DayDetail(
                start = String.format("%02d:%02d", time.hour, time.minute),
                end = "截止",
                title = task.title,
                subtitle = task.courseName ?: task.description ?: "任务",
                dotColor = when (task.urgency) {
                    com.campus.todo.data.db.entity.UrgencyLevel.URGENT -> Color(0xFFF2914E)
                    com.campus.todo.data.db.entity.UrgencyLevel.IMPORTANT -> Color(0xFFE769CC)
                    com.campus.todo.data.db.entity.UrgencyLevel.NORMAL -> Color(0xFF6FD3D0)
                },
                courseId = -1,
                taskId = task.id
            )
        )
    }

    return details.mapValues { (_, value) -> value.sortedBy { it.start } }
}

private fun minuteLabel(minuteOfDay: Int): String {
    val hour = (minuteOfDay / 60).coerceIn(0, 23)
    val minute = (minuteOfDay % 60).coerceIn(0, 59)
    return String.format("%02d:%02d", hour, minute)
}
