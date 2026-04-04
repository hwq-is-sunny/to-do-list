package com.campus.todo.ui.screens.courses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilterChip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.TimetableSlot
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.MinuteParse
import com.campus.todo.util.TimeUtils

private val DAY_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    vm: CourseDetailViewModel = viewModel(factory = factory)
) {
    val detail by vm.detail.collectAsStateWithLifecycle()
    var showSlot by remember { mutableStateOf(false) }
    var dayIndex by remember { mutableIntStateOf(0) }
    var startText by remember { mutableStateOf("08:00") }
    var endText by remember { mutableStateOf("09:40") }
    var loc by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.course?.name ?: "课程详情") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSlot = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加上课时间")
            }
        }
    ) { padding ->
        val d = detail
        if (d == null) {
            Text("加载中…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("课表节次", style = MaterialTheme.typography.titleMedium)
            }
            if (d.slots.isEmpty()) {
                item {
                    Text(
                        "还没有排课时间。点右下角添加一节。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(d.slots, key = { it.id }) { slot ->
                    SlotRow(slot, onDelete = { vm.deleteSlot(slot.id) })
                }
            }

            item {
                Text("进行中的待办", style = MaterialTheme.typography.titleMedium)
            }
            if (d.tasks.isEmpty()) {
                item {
                    Text(
                        "暂无。从候选箱确认任务时可绑定本课程。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(d.tasks, key = { it.id }) { t ->
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(t.title, style = MaterialTheme.typography.titleSmall)
                            t.dueAtEpoch?.let {
                                Text("截止 ${TimeUtils.formatEpoch(it)}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { vm.markTaskDone(t.id) }) {
                                Text("标记已完成")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSlot) {
        AlertDialog(
            onDismissRequest = { showSlot = false },
            title = { Text("添加上课时间") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("星期", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DAY_LABELS.size) { i ->
                            val selected = dayIndex == i
                            FilterChip(
                                selected = selected,
                                onClick = { dayIndex = i },
                                label = { Text(DAY_LABELS[i]) }
                            )
                        }
                    }
                    OutlinedTextField(startText, { startText = it }, label = { Text("开始 HH:mm") }, singleLine = true)
                    OutlinedTextField(endText, { endText = it }, label = { Text("结束 HH:mm") }, singleLine = true)
                    OutlinedTextField(loc, { loc = it }, label = { Text("教室（可选）") }, singleLine = true)
                    OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sm = MinuteParse.parseMinuteOfDay(startText)
                        val em = MinuteParse.parseMinuteOfDay(endText)
                        if (sm != null && em != null && em > sm) {
                            vm.addSlot(
                                dayOfWeek = dayIndex + 1,
                                startMin = sm,
                                endMin = em,
                                location = loc.ifBlank { null },
                                note = note.ifBlank { null }
                            ) { showSlot = false }
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSlot = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SlotRow(slot: TimetableSlot, onDelete: () -> Unit) {
    Card {
        ListItem(
            headlineContent = {
                Text(
                    "${DAY_LABELS.getOrNull(slot.dayOfWeek - 1) ?: "周${slot.dayOfWeek}"}  " +
                        "${MinuteParse.formatMinuteOfDay(slot.startMinuteOfDay)}–${
                            MinuteParse.formatMinuteOfDay(slot.endMinuteOfDay)
                        }"
                )
            },
            supportingContent = {
                Text(listOfNotNull(slot.location, slot.note).joinToString(" · ").ifEmpty { " " })
            },
            trailingContent = {
                TextButton(onClick = onDelete) { Text("删除") }
            }
        )
    }
}
