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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.ui.AppViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCandidateScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    vm: AddCandidateViewModel = viewModel(factory = factory)
) {
    var text by remember { mutableStateOf("") }
    val mockSources = remember {
        listOf(
            SourceKind.WECHAT to "微信（模拟）",
            SourceKind.QQ to "QQ（模拟）",
            SourceKind.ZHIHUISHU to "智慧树（模拟）",
            SourceKind.CHAOXING to "学习通（模拟）",
            SourceKind.TJU_PORTAL to "天大办公网（模拟）",
            SourceKind.MANUAL to "手动输入"
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(mockSources[0].first) }
    var label by remember { mutableStateOf(mockSources[0].second) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导入到候选箱") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("关闭") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "粘贴通知、作业要求等文本即可。系统只做轻度识别，最终以你在详情页的修改为准。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box {
                OutlinedTextField(
                    value = label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("来源（占位）") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { expanded = true }) { Text("选择来源") }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    mockSources.forEach { (kind, lb) ->
                        DropdownMenuItem(
                            text = { Text(lb) },
                            onClick = {
                                selected = kind
                                label = lb
                                expanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("内容") },
                minLines = 6
            )
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        vm.submit(text, selected) { id -> onCreated(id) }
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text("生成候选")
            }
        }
    }
}
