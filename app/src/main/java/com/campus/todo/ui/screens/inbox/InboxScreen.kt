package com.campus.todo.ui.screens.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MarkunreadMailbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.CandidateItem
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.TimeUtils

@Composable
fun InboxScreen(
    factory: AppViewModelFactory,
    onOpenCandidate: (Long) -> Unit,
    onAdd: () -> Unit,
    vm: InboxViewModel = viewModel(factory = factory)
) {
    val list by vm.candidates.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060A19), Color(0xFF0A1022), Color(0xFF070B1A))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Inbox",
                            color = Color(0xFFF2F5FC),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "先确认，再进入正式待办并安排提醒",
                            color = Color(0xFFAFB9D3),
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(0xFFF5A15C), Color(0xFFF08038))))
                            .clickable(onClick = onAdd),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "添加", tint = Color.White)
                    }
                }
            }
            if (list.isEmpty()) {
                item {
                    EmptyInboxCard(onAdd = onAdd)
                }
            } else {
                items(list, key = { it.id }) { c ->
                    CandidateRow(
                        c = c,
                        modifier = Modifier.clickable { onOpenCandidate(c.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(c: CandidateItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF171E31), Color(0xFF121A2B)))
            )
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0x1AFFFFFF)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkunreadMailbox,
                        contentDescription = null,
                        tint = Color(0xFFF59A53),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    text = sourceLabel(c.sourceKind),
                    color = Color(0xFFC2CCE4),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color(0x64E6ECFC),
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = c.parsedTitle ?: c.rawText.take(80),
                color = Color(0xFFF4F8FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = c.rawText.take(120),
                color = Color(0xA9D3DCF1),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            c.parsedDueAtEpoch?.let {
                Text(
                    text = "识别截止: ${TimeUtils.formatEpoch(it)}",
                    color = Color(0xFFE8A56B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyInboxCard(onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF161E31), Color(0xFF111827))
                )
            )
            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x16FFFFFF)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inbox,
                    contentDescription = null,
                    tint = Color(0xFFB7C2DD)
                )
            }
            Text(
                text = "暂无候选事项",
                color = Color(0xFFF2F6FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "可从首页或这里导入一段通知文本，先入候选箱再确认。",
                color = Color(0xFFB7C1D9),
                fontSize = 13.sp
            )
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF49E59), Color(0xFFE56CD0))))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "导入到候选箱",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
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
