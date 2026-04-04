package com.campus.todo.ui.screens.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    factory: AppViewModelFactory,
    onOpenCandidate: (Long) -> Unit,
    onAdd: () -> Unit,
    vm: InboxViewModel = viewModel(factory = factory)
) {
    val list by vm.candidates.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("候选箱") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "这里的条目需要先确认，才会进入正式待办并安排提醒。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        "暂无候选。可从「今日」或此处手动导入一段通知文本。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(list, key = { it.id }) { c ->
                    CandidateRow(c, Modifier.clickable { onOpenCandidate(c.id) })
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(c: CandidateItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(c.parsedTitle ?: c.rawText.take(60), style = MaterialTheme.typography.titleSmall)
            Text(
                "来源: ${sourceLabel(c.sourceKind)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            c.parsedDueAtEpoch?.let {
                Text("识别截止: ${TimeUtils.formatEpoch(it)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun sourceLabel(s: com.campus.todo.data.db.entity.SourceKind) = when (s) {
    com.campus.todo.data.db.entity.SourceKind.WECHAT -> "微信（模拟）"
    com.campus.todo.data.db.entity.SourceKind.QQ -> "QQ（模拟）"
    com.campus.todo.data.db.entity.SourceKind.ZHIHUISHU -> "智慧树（模拟）"
    com.campus.todo.data.db.entity.SourceKind.CHAOXING -> "学习通（模拟）"
    com.campus.todo.data.db.entity.SourceKind.TJU_PORTAL -> "办公网（模拟）"
    com.campus.todo.data.db.entity.SourceKind.TIMETABLE -> "课表"
    com.campus.todo.data.db.entity.SourceKind.MANUAL -> "手动"
    com.campus.todo.data.db.entity.SourceKind.MOCK -> "模拟"
    com.campus.todo.data.db.entity.SourceKind.SHARE_IMPORT_STUB -> "分享（占位）"
}
