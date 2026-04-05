package com.campus.todo.ui.screens.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

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
                "粘贴通知、作业要求等文本即可。可使用「AI 辅助」尝试结构化提取，最终以详情页为准。",
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
                onValueChange = {
                    text = it
                    aiError = null
                },
                label = { Text("内容") },
                minLines = 6
            )
            aiError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = {
                    if (text.isBlank()) {
                        aiError = "请先输入内容"
                        return@OutlinedButton
                    }
                    aiLoading = true
                    aiError = null
                    vm.aiAssist(
                        text,
                        onOk = { s ->
                            aiLoading = false
                            val merged = buildString {
                                s.title?.let { append("【标题】").append(it).append('\n') }
                                s.courseHint?.let { append("【课程线索】").append(it).append('\n') }
                                s.dueAtEpoch?.let { append("【截止(毫秒时间戳)】").append(it).append('\n') }
                                s.taskType?.let { append("【类型】").append(it.name).append('\n') }
                                s.urgency?.let { append("【提醒级别】").append(it.name).append('\n') }
                                append('\n').append("—— 原文 ——\n").append(text)
                            }
                            text = merged
                        },
                        onErr = { msg ->
                            aiLoading = false
                            aiError = msg
                        }
                    )
                },
                enabled = !aiLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (aiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text("AI 辅助解析（DeepSeek）")
                }
            }
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        vm.submit(text, selected) { id -> onCreated(id) }
                    }
                },
                enabled = text.isNotBlank() && !aiLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("生成候选")
            }
        }
    }
}
