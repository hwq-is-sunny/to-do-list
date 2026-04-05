package com.campus.todo.ui.screens.inbox

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.ui.components.SectionHeader
import com.campus.todo.ui.components.SoftCard
import com.campus.todo.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    factory: AppViewModelFactory,
    onOpenCandidate: (Long) -> Unit,
    onAdd: () -> Unit,
    vm: InboxViewModel = viewModel(factory = factory)
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<Long>()) }

    // Exit selection mode when list changes
    val currentIds = state.candidates.map { it.id }.toSet()
    if (selectionMode && selectedItems.any { it !in currentIds }) {
        selectedItems = selectedItems intersect currentIds
        if (selectedItems.isEmpty()) selectionMode = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text("${selectedItems.size} 已选")
                    } else {
                        Column {
                            Text("候选箱")
                            if (state.totalCount > 0) {
                                Text(
                                    "${state.totalCount} 条待处理",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedItems = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    }
                },
                actions = {
                    if (selectionMode && selectedItems.isNotEmpty()) {
                        IconButton(onClick = {
                            vm.ignoreCandidates(selectedItems.toList())
                            selectedItems = emptySet()
                            selectionMode = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "忽略")
                        }
                        IconButton(onClick = {
                            vm.deleteCandidates(selectedItems.toList())
                            selectedItems = emptySet()
                            selectionMode = false
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (selectionMode) MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.background,
                    titleContentColor = if (selectionMode) MaterialTheme.colorScheme.onPrimaryContainer
                                         else MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
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
                SectionHeader("待确认事项", "向左滑动确认，右滑忽略")
            }
            if (state.candidates.isEmpty()) {
                item {
                    SoftCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "暂无候选。从「今日」或此处手动导入通知文本。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(state.candidates, key = { _, c -> c.id }) { index, c ->
                    val isSelected = c.id in selectedItems
                    SelectableCandidateRow(
                        candidate = c,
                        isSelected = isSelected,
                        onClick = {
                            if (selectionMode) {
                                selectedItems = if (isSelected) {
                                    selectedItems - c.id
                                } else {
                                    selectedItems + c.id
                                }
                                if (selectedItems.isEmpty()) selectionMode = false
                            } else {
                                onOpenCandidate(c.id)
                            }
                        },
                        onLongClick = {
                            selectionMode = true
                            selectedItems = setOf(c.id)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableCandidateRow(
    candidate: CandidateItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
         else MaterialTheme.colorScheme.surface,
        label = "selection_bg"
    )

    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Parsed title or raw text
                Text(
                    candidate.parsedTitle ?: candidate.rawText.take(60),
                    style = MaterialTheme.typography.titleSmall
                )

                // Source and date info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Source chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            sourceLabel(candidate.sourceKind),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Parsed due date
                    candidate.parsedDueAtEpoch?.let { epoch ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                TimeUtils.formatRelativeDate(epoch),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Confidence note
                candidate.confidenceNote?.let { note ->
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun sourceLabel(s: com.campus.todo.data.db.entity.SourceKind) = when (s) {
    com.campus.todo.data.db.entity.SourceKind.WECHAT -> "微信"
    com.campus.todo.data.db.entity.SourceKind.QQ -> "QQ"
    com.campus.todo.data.db.entity.SourceKind.ZHIHUISHU -> "智慧树"
    com.campus.todo.data.db.entity.SourceKind.CHAOXING -> "学习通"
    com.campus.todo.data.db.entity.SourceKind.TJU_PORTAL -> "办公网"
    com.campus.todo.data.db.entity.SourceKind.TIMETABLE -> "课表"
    com.campus.todo.data.db.entity.SourceKind.MANUAL -> "手动"
    com.campus.todo.data.db.entity.SourceKind.MOCK -> "模拟"
    com.campus.todo.data.db.entity.SourceKind.SHARE_IMPORT_STUB -> "分享"
}
