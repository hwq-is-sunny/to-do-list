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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.R
import com.campus.todo.data.db.entity.Course
import com.campus.todo.ui.AppViewModelFactory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CourseListScreen(
    factory: AppViewModelFactory,
    onCourseClick: (Long) -> Unit,
    vm: CourseListViewModel = viewModel(factory = factory)
) {
    val courses by vm.courses.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val today = remember { LocalDate.now() }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var keyword by rememberSaveable { mutableStateOf("") }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val currentMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val monthLabel = remember(currentMonth, locale) {
        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }

    val scheduleByDay = remember(currentMonth) { demoSchedule(currentMonth) }
    val detailItems = remember(scheduleByDay, selectedDate, keyword) {
        val all = scheduleByDay[selectedDate].orEmpty()
        if (keyword.isBlank()) all else all.filter {
            it.title.contains(keyword, ignoreCase = true) ||
                it.subtitle.contains(keyword, ignoreCase = true)
        }
    }
    val dateMarkers = remember(scheduleByDay) { scheduleByDay.keys.toSet() }

    val sourceCourses = if (courses.isNotEmpty()) courses else demoCourses()
    val requiredCourses = sourceCourses.take(4)
    val electiveCourses = sourceCourses.drop(4).ifEmpty { sourceCourses.takeLast(4) }

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
                    onBack = { Toast.makeText(context, "返回功能后续接入", Toast.LENGTH_SHORT).show() },
                    onDone = { Toast.makeText(context, "Done 功能后续接入", Toast.LENGTH_SHORT).show() },
                    onRefresh = { Toast.makeText(context, "刷新功能后续接入", Toast.LENGTH_SHORT).show() },
                    onAdd = { Toast.makeText(context, "新增功能后续接入", Toast.LENGTH_SHORT).show() }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CourseCategoryCard(
                        title = stringResource(R.string.courses_required_title),
                        items = requiredCourses.map { it.name },
                        tint = Brush.verticalGradient(listOf(Color(0xFFC4B8FF), Color(0xFFABA0F2))),
                        modifier = Modifier.weight(1f)
                    )
                    CourseCategoryCard(
                        title = stringResource(R.string.courses_elective_title),
                        items = electiveCourses.map { it.name },
                        tint = Brush.verticalGradient(listOf(Color(0xFF72E0DD), Color(0xFF54C7D2))),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                DetailPanel(
                    date = selectedDate,
                    details = detailItems,
                    onOpenCourse = onCourseClick
                )
            }
        }
    }
}

@Composable
private fun TopActionRow(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onRefresh: () -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(icon = Icons.Outlined.ArrowBack, onClick = onBack)
            Box(modifier = Modifier.weight(1f)) {
                TextField(
                    value = keyword,
                    onValueChange = onKeywordChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.courses_search_placeholder),
                            color = Color(0xFF96A1BA),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color(0xFFD0D9ED)
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
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(R.string.common_done),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircleIconButton(icon = Icons.Outlined.Refresh, onClick = onRefresh)
                CircleIconButton(icon = Icons.Outlined.Add, onClick = onAdd)
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
            text = stringResource(R.string.courses_monthly_title),
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
            val dayLabels = listOf(7, 1, 2, 3, 4, 5, 6).map { day ->
                java.time.DayOfWeek.of(day)
                    .getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault())
            }
            dayLabels.forEach {
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
private fun CourseCategoryCard(
    title: String,
    items: List<String>,
    tint: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(tint)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Color(0xFF182038), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            items.take(4).forEach { name ->
                Text(
                    text = name,
                    color = Color(0xFF21304D),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DetailPanel(
    date: LocalDate,
    details: List<DayDetail>,
    onOpenCourse: (Long) -> Unit
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
                text = date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())),
                color = Color(0xFFF7F9FF),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (details.isEmpty()) {
                Text(
                    text = stringResource(R.string.courses_no_schedule),
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
                            .clickable { if (detail.courseId > 0) onOpenCourse(detail.courseId) }
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

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x12FFFFFF))
            .border(1.dp, Color(0x30FFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE8EDFA), modifier = Modifier.size(20.dp))
    }
}

private data class CalendarCell(val date: LocalDate?)

private data class DayDetail(
    val start: String,
    val end: String,
    val title: String,
    val subtitle: String,
    val dotColor: Color,
    val courseId: Long = -1
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

private fun demoSchedule(month: YearMonth): Map<LocalDate, List<DayDetail>> {
    val d1 = month.atDay(4.coerceAtMost(month.lengthOfMonth()))
    val d2 = month.atDay(6.coerceAtMost(month.lengthOfMonth()))
    val d3 = month.atDay(12.coerceAtMost(month.lengthOfMonth()))
    val d4 = month.atDay(14.coerceAtMost(month.lengthOfMonth()))
    val d5 = month.atDay(20.coerceAtMost(month.lengthOfMonth()))
    val d6 = month.atDay(22.coerceAtMost(month.lengthOfMonth()))
    val d7 = month.atDay(25.coerceAtMost(month.lengthOfMonth()))
    val d8 = month.atDay(26.coerceAtMost(month.lengthOfMonth()))
    val d9 = month.atDay(27.coerceAtMost(month.lengthOfMonth()))

    return mapOf(
        d1 to listOf(DayDetail("08:00", "17:00", "Assigment Deadline", "History of ux design", Color(0xFFE769CC))),
        d2 to listOf(DayDetail("17:00", "19:30", "Class Cancelled", "Basics of Marketing", Color(0xFF9BD45F))),
        d3 to listOf(DayDetail("09:00", "11:00", "Philosophy of Art", "Room B-201", Color(0xFF6FD3D0))),
        d4 to listOf(DayDetail("13:00", "15:00", "Display design", "A3 Studio", Color(0xFFB5A9F8))),
        d5 to listOf(DayDetail("14:30", "16:00", "Sketch Review", "Design Workshop", Color(0xFFF2914E))),
        d6 to listOf(DayDetail("19:00", "21:00", "Basketball Tarining", "SO33 Sport room 07", Color(0xFFF2914E))),
        d7 to listOf(
            DayDetail("08:00", "17:00", "Assigment Deadline", "History of ux design", Color(0xFFE769CC)),
            DayDetail("17:00", "19:30", "Class Cancelled", "Basics of Marketing", Color(0xFF9BD45F))
        ),
        d8 to listOf(DayDetail("19:00", "21:00", "Basketball Tarining", "SO33 Sport room 07", Color(0xFFF2914E))),
        d9 to listOf(DayDetail("09:00", "11:00", "Tea Art Appreciation", "Hall C3", Color(0xFF6FD3D0)))
    )
}

private fun demoCourses(): List<Course> = listOf(
    Course(id = 1, name = "Chinese are history", code = "ART101"),
    Course(id = 2, name = "Still life sketching", code = "ART202"),
    Course(id = 3, name = "Half-leng portrait sketch", code = "ART212"),
    Course(id = 4, name = "Fine-line Sketching", code = "ART301"),
    Course(id = 5, name = "Music appreciation", code = "MUS121"),
    Course(id = 6, name = "Display design", code = "DES204"),
    Course(id = 7, name = "Appreciation of calligr", code = "ART330"),
    Course(id = 8, name = "Tea Art Appreciation", code = "ART402")
)
