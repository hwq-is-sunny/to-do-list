package com.campus.todo.ui.screens.calendar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.Task
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.ui.components.DeepCard
import com.campus.todo.ui.components.SectionHeader
import com.campus.todo.ui.components.SoftCard
import com.campus.todo.util.MinuteParse
import com.campus.todo.util.TimeUtils
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    factory: AppViewModelFactory,
    vm: CalendarViewModel = viewModel(factory = factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val df = DateTimeFormatter.ofPattern("M/d E")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日历 · 周视图") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { vm.shiftWeek(-1) }) {
                        Icon(Icons.Default.ChevronLeft, "上一周")
                    }
                    IconButton(onClick = { vm.shiftWeek(1) }) {
                        Icon(Icons.Default.ChevronRight, "下一周")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DeepCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("你的训练天数 → 这里替换为校园周视图", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.days.forEach { cell ->
                    val selected = cell.date == state.selected
                    FilterChip(
                        selected = selected,
                        onClick = { vm.selectDate(cell.date) },
                        label = { Text(df.format(cell.date)) }
                    )
                }
            }

            val selectedCell = state.days.find { it.date == state.selected }
                ?: state.days.firstOrNull()
            if (selectedCell == null) return@Column
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SectionHeader("课表")
                }
                if (selectedCell.slots.isEmpty()) {
                    item {
                        SoftCard { Text("这一天没有课表节次。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                } else {
                    items(selectedCell.slots, key = { it.id }) { slot ->
                        val name = state.coursesById[slot.courseId]?.name ?: "课程"
                        SoftCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay)}–${
                                        MinuteParse.formatMinuteOfDay(slot.endMinuteOfDay)
                                    }  ${slot.location ?: ""}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader("有截止时间的待办")
                }
                if (selectedCell.tasks.isEmpty()) {
                    item {
                        SoftCard { Text("这天没有待办截止。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                } else {
                    items(selectedCell.tasks, key = { it.id }) { t ->
                        TaskCardWeek(t, state.coursesById[t.courseId ?: 0L]?.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCardWeek(t: Task, course: String?) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(t.title, style = MaterialTheme.typography.titleSmall)
            val due = t.dueAtEpoch?.let { TimeUtils.formatEpoch(it) } ?: "无截止"
            Text(
                listOfNotNull(course, due).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
