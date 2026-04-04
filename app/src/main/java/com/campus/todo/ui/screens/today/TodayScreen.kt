package com.campus.todo.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.MinuteParse
import com.campus.todo.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    factory: AppViewModelFactory,
    onOpenAddCandidate: () -> Unit,
    vm: TodayViewModel = viewModel(factory = factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今日") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "今天的课程",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (state.timetableSlots.isEmpty()) {
                item {
                    Text(
                        "暂无课表。到「课程」里添加课程和节次即可出现在这里。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.timetableSlots, key = { it.id }) { slot ->
                    val cn = state.coursesById[slot.courseId]?.name ?: "未命名课程"
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        ListItem(
                            headlineContent = { Text(cn) },
                            supportingContent = {
                                Text(
                                    "${MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay)}–${
                                        MinuteParse.formatMinuteOfDay(slot.endMinuteOfDay)
                                    }  ·  ${slot.location ?: "教室待定"}"
                                )
                            }
                        )
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "今日待办",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (state.tasksDueToday.isEmpty()) {
                item {
                    Text(
                        "今日没有截止的待办，轻松一点也没问题。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.tasksDueToday, key = { it.id }) { t ->
                    TaskRow(t, state.coursesById[t.courseId ?: -1]?.name)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "临近截止",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "未来 7 天内（不含仅今天已列在上方）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.upcomingTasks.isEmpty()) {
                item {
                    Text(
                        "暂无。需要时从候选箱确认事项即可。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.upcomingTasks, key = { it.id }) { t ->
                    TaskRow(t, state.coursesById[t.courseId ?: -1]?.name)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "候选箱",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "从通知里识别的事项会先出现在候选箱，避免误进清单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(
                    onClick = onOpenAddCandidate,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    ListItem(
                        headlineContent = { Text("手动 / 模拟导入一条文本") },
                        supportingContent = { Text("进入候选箱，确认后再提醒") }
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, courseName: String?) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title, style = MaterialTheme.typography.titleSmall)
            val sub = buildString {
                courseName?.let { append(it).append(" · ") }
                append(taskTypeLabel(task))
                append(" · ")
                append(urgencyLabel(task.urgency))
                task.dueAtEpoch?.let {
                    append(" · 截止 ")
                    append(TimeUtils.formatEpoch(it))
                }
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun taskTypeLabel(t: Task) = when (t.taskType) {
    com.campus.todo.data.db.entity.TaskType.HOMEWORK -> "作业"
    com.campus.todo.data.db.entity.TaskType.EXAM -> "考试"
    com.campus.todo.data.db.entity.TaskType.SIGN_IN -> "签到"
    com.campus.todo.data.db.entity.TaskType.CLASS -> "上课"
    com.campus.todo.data.db.entity.TaskType.ANNOUNCEMENT -> "通知"
    com.campus.todo.data.db.entity.TaskType.PERSONAL -> "个人"
    com.campus.todo.data.db.entity.TaskType.OTHER -> "其他"
}

private fun urgencyLabel(u: UrgencyLevel) = when (u) {
    UrgencyLevel.NORMAL -> "普通提醒"
    UrgencyLevel.IMPORTANT -> "重要"
    UrgencyLevel.URGENT -> "紧急"
}
