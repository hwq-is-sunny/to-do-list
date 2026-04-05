package com.campus.todo.ui.screens.courses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.Course
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.ui.components.SectionHeader
import com.campus.todo.ui.components.SoftCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(
    factory: AppViewModelFactory,
    onCourseClick: (Long) -> Unit,
    vm: CourseListViewModel = viewModel(factory = factory)
) {
    val courses by vm.courses.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    var menuCourseId by remember { mutableStateOf<Long?>(null) }
    var editTarget by remember { mutableStateOf<Course?>(null) }
    var editName by remember { mutableStateOf("") }
    var editCode by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Course?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课程") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "添加课程")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionHeader("课程列表", "点课程进入详情；右侧菜单可改名或删除") }
            items(courses, key = { it.id }) { c ->
                SoftCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCourseClick(c.id) }
                                .padding(end = 4.dp)
                        ) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall)
                            c.code?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Box {
                            IconButton(onClick = { menuCourseId = c.id }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "菜单")
                            }
                            DropdownMenu(
                                expanded = menuCourseId == c.id,
                                onDismissRequest = { menuCourseId = null }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("修改名称") },
                                    onClick = {
                                        menuCourseId = null
                                        editTarget = c
                                        editName = c.name
                                        editCode = c.code.orEmpty()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("删除课程") },
                                    onClick = {
                                        menuCourseId = null
                                        deleteTarget = c
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("新建课程") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("课程名") }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("课号（可选）") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            vm.addCourse(name, code.ifBlank { null }) { id ->
                                showAdd = false
                                name = ""
                                code = ""
                                onCourseClick(id)
                            }
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            }
        )
    }

    val et = editTarget
    if (et != null) {
        AlertDialog(
            onDismissRequest = { editTarget = null },
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
                            vm.updateCourse(et.id, editName, editCode.ifBlank { null }) {
                                editTarget = null
                            }
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("取消") }
            }
        )
    }

    val dt = deleteTarget
    if (dt != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除课程") },
            text = { Text("确定删除「${dt.name}」及该课程下所有节次吗？关联待办将保留并取消课程关联。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteCourse(dt.id) { deleteTarget = null }
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}
