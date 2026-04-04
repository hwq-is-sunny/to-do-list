package com.campus.todo.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.campus.todo.ui.components.DeepCard
import com.campus.todo.ui.components.MiniMetric
import com.campus.todo.ui.components.SectionHeader
import com.campus.todo.ui.components.SoftCard
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
                title = { Text("Hi，同学") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
                DeepCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Today Overview", style = MaterialTheme.typography.titleMedium)
                        MiniMetric("今日课程", "${state.timetableSlots.size} 门")
                        MiniMetric("今日截止", "${state.tasksDueToday.size} 项")
                        MiniMetric("临近截止", "${state.upcomingTasks.size} 项")
                    }
                }
            }
            item { SectionHeader("今天的课程") }
            if (state.timetableSlots.isEmpty()) {
                item {
                    SoftCard {
                        Text(
                            "暂无课表。到「课程」里添加课程和节次即可出现在这里。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.timetableSlots, key = { it.id }) { slot ->
                    val cn = state.coursesById[slot.courseId]?.name ?: "未命名课程"
                    SoftCard {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(cn, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay)}–${
                                    MinuteParse.formatMinuteOfDay(slot.endMinuteOfDay)
                                }  ·  ${slot.location ?: "教室待定"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader("今日待办")
            }
            if (state.tasksDueToday.isEmpty()) {
                item {
                    SoftCard {
                        Text(
                            "今日没有截止的待办，轻松一点也没问题。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.tasksDueToday, key = { it.id }) { t ->
                    TaskRow(t, state.coursesById[t.courseId ?: -1]?.name)
                }
            }

            item {
                SectionHeader("临近截止", "未来 7 天内（不含今天）")
            }
            if (state.upcomingTasks.isEmpty()) {
                item {
                    SoftCard {
                        Text(
                            "暂无。需要时从候选箱确认事项即可。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.upcomingTasks, key = { it.id }) { t ->
                    TaskRow(t, state.coursesById[t.courseId ?: -1]?.name)
                }
            }

            item {
                SectionHeader("候选箱", "先确认，再进入正式待办")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenAddCandidate,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.AddCircle, contentDescription = null)
                        Text("手动 / 模拟导入一条文本", style = MaterialTheme.typography.titleSmall)
                        Text("进入候选箱，确认后再提醒", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("立即添加", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, courseName: String?) {
    SoftCard {
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
