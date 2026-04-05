package com.campus.todo.ui.screens.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                title = {
                    Column {
                        Text("Hi，同学", style = MaterialTheme.typography.titleLarge)
                        Text(
                            TimeUtils.formatDateToday(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("今日概览", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MiniMetricStat("今日课程", "${state.timetableSlots.size}", "门")
                            MiniMetricStat("今日截止", "${state.tasksDueToday.size}", "项")
                            MiniMetricStat("临近截止", "${state.upcomingTasks.size}", "项")
                        }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(cn, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay)}–${
                                        MinuteParse.formatMinuteOfDay(slot.endMinuteOfDay)
                                    }  ·  ${slot.location ?: "教室待定"}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            // Time indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("今日待办", "${state.tasksDueToday.size} 项")
            }
            if (state.tasksDueToday.isEmpty()) {
                item {
                    SoftCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "今日没有截止的待办，轻松一点也没问题。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.tasksDueToday, key = { it.id }) { t ->
                    SwipeableTaskRow(
                        task = t,
                        courseName = state.coursesById[t.courseId ?: -1]?.name,
                        onComplete = { vm.markTaskDone(t.id) },
                        onDelete = { vm.deleteTask(t.id) }
                    )
                }
            }

            item {
                SectionHeader("即将截止", "未来 7 天内")
            }
            if (state.upcomingTasks.isEmpty()) {
                item {
                    SoftCard {
                        Text(
                            "暂无。确认候选箱中的事项即可添加。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(state.upcomingTasks.take(5), key = { "upcoming_${it.id}" }) { t ->
                    SwipeableTaskRow(
                        task = t,
                        courseName = state.coursesById[t.courseId ?: -1]?.name,
                        onComplete = { vm.markTaskDone(t.id) },
                        onDelete = { vm.deleteTask(t.id) }
                    )
                }
                if (state.upcomingTasks.size > 5) {
                    item {
                        SoftCard {
                            Text(
                                "还有 ${state.upcomingTasks.size - 5} 项即将截止...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader("快速添加")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenAddCandidate,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("导入待办文本", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "从通知、聊天记录中快速添加",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            Icons.Outlined.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMetricStat(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            "$label $unit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskRow(
    task: Task,
    courseName: String?,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onComplete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFFE53935)
                    else -> Color.Transparent
                },
                label = "swipe_color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                Icon(
                    imageVector = when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.CheckCircle
                        else -> Icons.Default.Delete
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        TaskRow(task, courseName)
    }
}

@Composable
private fun TaskRow(task: Task, courseName: String?) {
    val urgencyColor = when (task.urgency) {
        UrgencyLevel.URGENT -> Color(0xFFE53935)
        UrgencyLevel.IMPORTANT -> Color(0xFFFF9800)
        UrgencyLevel.NORMAL -> MaterialTheme.colorScheme.primary
    }

    SoftCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                // Urgency badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(urgencyColor.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        urgencyLabel(task.urgency),
                        style = MaterialTheme.typography.labelSmall,
                        color = urgencyColor
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Task type chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        taskTypeLabel(task),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                courseName?.let {
                    Text(
                        "· $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            task.dueAtEpoch?.let { epoch ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "截止: ${TimeUtils.formatEpoch(epoch)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
    UrgencyLevel.NORMAL -> "普通"
    UrgencyLevel.IMPORTANT -> "重要"
    UrgencyLevel.URGENT -> "紧急"
}
