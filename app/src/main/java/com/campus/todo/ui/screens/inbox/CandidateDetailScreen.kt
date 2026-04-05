package com.campus.todo.ui.screens.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.Course
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.TimeUtils
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("确认事项") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        val c = cand
        if (c == null) {
            Text("加载中…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "原始片段",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(c.rawText, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(title, { title = it }, label = { Text("标题") }, singleLine = false, minLines = 2)
            CourseDropdown(courses, courseId, onCourseId = { courseId = it })
            TypeDropdown(taskType) { taskType = it }
            UrgencyDropdown(urgency) { urgency = it }

            Text(
                if (hasDue) "截止: ${TimeUtils.formatEpoch(dueEpoch)}" else "尚未设置截止",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = { showDate = true }) { Text(if (hasDue) "更改日期" else "选择截止日期") }
            if (hasDue) {
                TextButton(onClick = { hasDue = false }) { Text("清除截止") }
            }

            Text(
                "保存草稿后，若设置了截止日期，会在临近时提醒你「确认并加入正式待办」；正式提醒仍以确认后的待办为准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = {
                    vm.updateDraft(
                        title = title,
                        courseHint = courses.find { it.id == courseId }?.name,
                        dueAtEpoch = if (hasDue) dueEpoch else null,
                        taskType = taskType,
                        urgency = urgency
                    ) { }
                }
            ) { Text("保存草稿") }

            Button(
                onClick = {
                    vm.confirm(
                        title = title,
                        courseId = courseId.takeIf { it >= 0 },
                        dueAtEpoch = if (hasDue) dueEpoch else null,
                        taskType = taskType,
                        urgency = urgency,
                        onDone = onBack
                    )
                }
            ) { Text("确认并进正式待办") }

            TextButton(
                onClick = {
                    vm.ignore(onBack)
                }
            ) { Text("忽略此条") }
        }
    }

    if (showDate) {
        val initial = if (hasDue && dueEpoch > 0) dueEpoch else System.currentTimeMillis()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = state.selectedDateMillis
                        if (millis != null) {
                            val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            dueEpoch = TimeUtils.endOfDayEpoch(ld)
                            hasDue = true
                        }
                        showDate = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun matchCourseId(courses: List<Course>, hint: String?): Long? {
    if (hint.isNullOrBlank()) return null
    return courses.firstOrNull { it.name.contains(hint) || hint.contains(it.name) }?.id
}

@Composable
private fun CourseDropdown(courses: List<Course>, selectedId: Long, onCourseId: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = courses.find { it.id == selectedId }?.name ?: "不关联课程"
    Box {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("课程") },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { expanded = true }) { Text("选择课程") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("不关联课程") },
                onClick = {
                    onCourseId(-1L)
                    expanded = false
                }
            )
            courses.forEach { co ->
                DropdownMenuItem(
                    text = { Text(co.name) },
                    onClick = {
                        onCourseId(co.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TypeDropdown(value: TaskType, onChange: (TaskType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = TaskType.entries.toList()
    Box {
        OutlinedTextField(
            value = typeLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text("类型") },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { expanded = true }) { Text("选择类型") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { t ->
                DropdownMenuItem(
                    text = { Text(typeLabel(t)) },
                    onClick = {
                        onChange(t)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UrgencyDropdown(value: UrgencyLevel, onChange: (UrgencyLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = UrgencyLevel.entries.toList()
    Box {
        OutlinedTextField(
            value = urgencyLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text("提醒级别") },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = { expanded = true }) { Text("选择级别") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { u ->
                DropdownMenuItem(
                    text = { Text(urgencyLabel(u)) },
                    onClick = {
                        onChange(u)
                        expanded = false
                    }
                )
            }
        }
    }
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
