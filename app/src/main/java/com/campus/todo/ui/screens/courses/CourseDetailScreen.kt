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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.campus.todo.ui.components.SectionHeader
import com.campus.todo.ui.components.SoftCard
import com.campus.todo.util.MinuteParse
import com.campus.todo.util.TimeUtils

private val DAY_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onCourseDeleted: () -> Unit,
    vm: CourseDetailViewModel = viewModel(factory = factory)
) {
    val detail by vm.detail.collectAsStateWithLifecycle()
    var showSlot by remember { mutableStateOf(false) }
    var dayIndex by remember { mutableIntStateOf(0) }
    var startText by remember { mutableStateOf("08:00") }
    var endText by remember { mutableStateOf("09:40") }
    var loc by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var slotError by remember { mutableStateOf<String?>(null) }

    var menuOpen by remember { mutableStateOf(false) }
    var showEditCourse by remember { mutableStateOf(false) }
    var showDeleteCourse by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.course?.name ?: "课程详情") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("修改课程名称") },
                            onClick = {
                                menuOpen = false
                                editName = detail?.course?.name.orEmpty()
                                editCode = detail?.course?.code.orEmpty()
                                showEditCourse = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除整门课程") },
                            onClick = {
                                menuOpen = false
                                showDeleteCourse = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                slotError = null
                showSlot = true
            }) {
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
                SectionHeader("课表节次")
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
                SectionHeader("进行中的待办")
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
                    SoftCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    OutlinedTextField(startText, { startText = it; slotError = null }, label = { Text("开始时间（如 08:00）") }, singleLine = true)
                    OutlinedTextField(endText, { endText = it; slotError = null }, label = { Text("结束时间（如 09:40）") }, singleLine = true)
                    OutlinedTextField(loc, { loc = it }, label = { Text("教室（可选）") }, singleLine = true)
                    OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, singleLine = true)
                    slotError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val err = MinuteParse.timeRangeValidationError(startText, endText)
                        if (err != null) {
                            slotError = err
                            return@TextButton
                        }
                        val sm = MinuteParse.parseMinuteOfDay(startText)
                        val em = MinuteParse.parseMinuteOfDay(endText)
                        if (sm == null || em == null) {
                            slotError = "时间格式无效，请输入 时:分（如 08:00）"
                            return@TextButton
                        }
                        vm.addSlot(
                            dayOfWeek = dayIndex + 1,
                            startMin = sm,
                            endMin = em,
                            location = loc.ifBlank { null },
                            note = note.ifBlank { null }
                        ) {
                            showSlot = false
                            slotError = null
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSlot = false }) { Text("取消") }
            }
        )
    }

    if (showEditCourse) {
        AlertDialog(
            onDismissRequest = { showEditCourse = false },
            title = { Text("修改课程") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editName, { editName = it }, label = { Text("课程名") }, singleLine = true)
                    OutlinedTextField(editCode, { editCode = it }, label = { Text("课号（可选）") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank()) {
                            vm.updateCourse(editName, editCode.ifBlank { null }) {
                                showEditCourse = false
                            }
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditCourse = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteCourse) {
        AlertDialog(
            onDismissRequest = { showDeleteCourse = false },
            title = { Text("删除课程") },
            text = {
                Text("将删除本课程及所有课表节次；关联待办会保留，但不再关联到本课程。确定吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteCourse {
                            showDeleteCourse = false
                            onCourseDeleted()
                        }
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCourse = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SlotRow(slot: TimetableSlot, onDelete: () -> Unit) {
    SoftCard {
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
