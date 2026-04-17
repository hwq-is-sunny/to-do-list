package com.campus.todo.ui.screens.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.TimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CandidateDetailScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    vm: CandidateDetailViewModel = viewModel(factory = factory)
) {
    val cand by vm.candidate.collectAsStateWithLifecycle()
    val courses by vm.courses.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var courseId by remember { mutableLongStateOf(-1L) }
    var dueEpoch by remember { mutableLongStateOf(0L) }
    var hasDue by remember { mutableStateOf(false) }
    var taskType by remember { mutableStateOf(TaskType.OTHER) }
    var urgency by remember { mutableStateOf(UrgencyLevel.NORMAL) }
    var showDate by remember { mutableStateOf(false) }
    var showIgnoreConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(cand?.id) {
        val c = cand ?: return@LaunchedEffect
        title = c.parsedTitle ?: c.rawText.take(120)
        dueEpoch = c.parsedDueAtEpoch ?: 0L
        hasDue = c.parsedDueAtEpoch != null
        taskType = c.suggestedTaskType ?: TaskType.OTHER
        urgency = c.suggestedUrgency ?: UrgencyLevel.NORMAL
        courseId = matchCourseId(courses, c.parsedCourseHint) ?: -1L
    }

    LaunchedEffect(courses, cand?.parsedCourseHint) {
        val c = cand ?: return@LaunchedEffect
        val m = matchCourseId(courses, c.parsedCourseHint)
        if (m != null) courseId = m
    }

    val c = cand
    if (c == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080C1A)),
            contentAlignment = Alignment.Center
        ) {
            Text("加载中...", color = Color(0xFFD9E0F3))
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF060A19), Color(0xFF0A1022), Color(0xFF070B1A))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleActionIcon(icon = Icons.Outlined.ArrowBack, onClick = onBack)
                Text(
                    text = "Confirm Candidate",
                    color = Color(0xFFF2F5FC),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(modifier = Modifier.width(40.dp))
            }

            Text("Raw snippet", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFFF3A060))
            InfoCard(text = c.rawText)

            StyledInput(value = title, onValueChange = { title = it }, placeholder = "Title")
            SelectionField(
                label = "Course",
                value = courses.find { it.id == courseId }?.name ?: "不关联课程",
                options = listOf("不关联课程") + courses.map { it.name },
                onSelect = { name -> courseId = courses.find { it.name == name }?.id ?: -1L }
            )
            SelectionField(
                label = "Type",
                value = typeLabel(taskType),
                options = TaskType.entries.map(::typeLabel),
                onSelect = { selectedLabel ->
                    taskType = TaskType.entries.first { typeLabel(it) == selectedLabel }
                }
            )
            SelectionField(
                label = "Urgency",
                value = urgencyLabel(urgency),
                options = UrgencyLevel.entries.map(::urgencyLabel),
                onSelect = { selectedLabel ->
                    urgency = UrgencyLevel.entries.first { urgencyLabel(it) == selectedLabel }
                }
            )

            DateField(
                hasDue = hasDue,
                dueEpoch = dueEpoch,
                onOpen = { showDate = true },
                onClear = { hasDue = false }
            )

            HintText("保存草稿后，若设置了截止日期，会在临近时提醒你确认并加入正式待办。")

            SecondaryButton(
                onClick = {
                    vm.updateDraft(
                        title = title,
                        courseHint = courses.find { it.id == courseId }?.name,
                        dueAtEpoch = if (hasDue) dueEpoch else null,
                        taskType = taskType,
                        urgency = urgency
                    ) { }
                },
                label = "保存草稿"
            )
            PrimaryButton(
                onClick = {
                    vm.confirm(
                        title = title,
                        courseId = courseId.takeIf { it >= 0 },
                        dueAtEpoch = if (hasDue) dueEpoch else null,
                        taskType = taskType,
                        urgency = urgency,
                        onDone = onBack
                    )
                },
                label = "确认并进正式待办"
            )
            SecondaryButton(
                onClick = { showIgnoreConfirm = true },
                label = "忽略此条",
                warning = true
            )
        }
    }

    if (showDate) {
        DatePickerOverlay(
            initial = dueEpoch.takeIf { hasDue },
            onDismiss = { showDate = false },
            onPick = { selectedEpoch ->
                dueEpoch = selectedEpoch
                hasDue = true
                showDate = false
            }
        )
    }

    if (showIgnoreConfirm) {
        ConfirmOverlay(
            title = "忽略此条候选？",
            description = "忽略后该事项会从候选箱移除。",
            onDismiss = { showIgnoreConfirm = false },
            onConfirm = {
                showIgnoreConfirm = false
                vm.ignore(onBack)
            }
        )
    }
}

@Composable
private fun CircleActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x2EFFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color(0xFFE8EDFA), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun InfoCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF151D30))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Text(text = text, color = Color(0xFFDDE5F8), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun StyledInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        minLines = 2,
        shape = RoundedCornerShape(22.dp),
        placeholder = { Text(placeholder, color = Color(0xFF8E9AB5)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF151C2E),
            unfocusedContainerColor = Color(0xFF151C2E),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color(0xFFF0F4FF),
            unfocusedTextColor = Color(0xFFF0F4FF),
            cursorColor = Color(0xFFF0F4FF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
    )
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF151D30))
                .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(20.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = label, color = Color(0xFF9EA9C4), fontSize = 12.sp)
                    Text(
                        text = value,
                        color = Color(0xFFF0F4FF),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = Color(0xFFC8D0E6))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(18.dp),
            containerColor = Color(0xCC1A2238),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.width(240.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color(0xFFEAF0FF)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DateField(
    hasDue: Boolean,
    dueEpoch: Long,
    onOpen: () -> Unit,
    onClear: () -> Unit
) {
    val dateText = if (hasDue) "截止: ${TimeUtils.formatEpoch(dueEpoch)}" else "尚未设置截止"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF151D30))
            .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Event, null, tint = Color(0xFFD0D8EE), modifier = Modifier.size(18.dp))
                Text(dateText, color = Color(0xFFF0F4FF), fontSize = 15.sp)
            }
            if (hasDue) {
                Text(
                    "清除",
                    color = Color(0xFFF08C57),
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onClear)
                )
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(text = text, color = Color(0xFFAAB5CF), fontSize = 13.sp, lineHeight = 18.sp)
}

@Composable
private fun SecondaryButton(onClick: () -> Unit, label: String, warning: Boolean = false) {
    val bg = if (warning) Color(0xFF2A1C25) else Color(0xFF1A2237)
    val fg = if (warning) Color(0xFFF4A7B4) else Color(0xFFEAF0FF)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (warning) {
            Icon(Icons.Outlined.WarningAmber, null, tint = fg, modifier = Modifier.size(16.dp))
            Text(label, color = fg, modifier = Modifier.padding(start = 6.dp))
        } else {
            Text(label, color = fg, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PrimaryButton(onClick: () -> Unit, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFF39C58), Color(0xFFE66CD0))))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DatePickerOverlay(
    initial: Long?,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    val baseDate = remember(initial) {
        if (initial != null && initial > 0L) {
            Instant.ofEpochMilli(initial).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            LocalDate.now()
        }
    }
    val options = remember(baseDate) { (0..10).map { baseDate.plusDays(it.toLong()) } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color(0xFF161E31))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选择截止日期", color = Color(0xFFF0F4FF), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                options.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(TimeUtils.endOfDayEpoch(day)) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(day.format(DateTimeFormatter.ofPattern("M/d E", Locale.ENGLISH)), color = Color(0xFFEAF0FF))
                        Text(
                            day.format(DateTimeFormatter.ofPattern("yyyy", Locale.ENGLISH)),
                            color = Color(0x9FEAF0FF),
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    "取消",
                    color = Color(0xFFBFC8DD),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onDismiss)
                        .padding(top = 4.dp, bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfirmOverlay(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF182137))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, color = Color(0xFFF0F4FF), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = Color(0xFFB8C2DC), fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(onClick = onDismiss, label = "取消")
                    PrimaryButton(onClick = onConfirm, label = "确认")
                }
            }
        }
    }
}

private fun matchCourseId(courses: List<Course>, hint: String?): Long? {
    if (hint.isNullOrBlank()) return null
    return courses.firstOrNull { it.name.contains(hint) || hint.contains(it.name) }?.id
}

private fun typeLabel(t: TaskType) = when (t) {
    TaskType.HOMEWORK -> "作业"
    TaskType.EXAM -> "考试"
    TaskType.SIGN_IN -> "签到"
    TaskType.CLASS -> "上课"
    TaskType.ANNOUNCEMENT -> "通知"
    TaskType.PERSONAL -> "个人"
    TaskType.OTHER -> "其他"
}

private fun urgencyLabel(u: UrgencyLevel) = when (u) {
    UrgencyLevel.NORMAL -> "普通提醒"
    UrgencyLevel.IMPORTANT -> "重要"
    UrgencyLevel.URGENT -> "紧急"
}
