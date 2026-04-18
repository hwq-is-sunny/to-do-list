package com.campus.todo.ui.screens.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.R
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.ui.AppViewModelFactory

@Composable
fun AddCandidateScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
    vm: AddCandidateViewModel = viewModel(factory = factory)
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    val mockSources = remember {
        listOf(
            SourceKind.WECHAT to context.getString(R.string.add_candidate_source_wechat),
            SourceKind.QQ to context.getString(R.string.add_candidate_source_qq),
            SourceKind.ZHIHUISHU to context.getString(R.string.add_candidate_source_zhihuishu),
            SourceKind.CHAOXING to context.getString(R.string.add_candidate_source_chaoxing),
            SourceKind.TJU_PORTAL to context.getString(R.string.add_candidate_source_portal),
            SourceKind.MANUAL to context.getString(R.string.add_candidate_source_manual)
        )
    }
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(mockSources[0].first) }
    var label by remember { mutableStateOf(mockSources[0].second) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF060A19), Color(0xFF0A1022), Color(0xFF070B1A))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleActionIcon(
                    icon = Icons.Outlined.ArrowBack,
                    onClick = onBack
                )
                Text(
                    text = stringResource(R.string.add_candidate_title),
                    color = Color(0xFFF2F5FC),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(modifier = Modifier.width(40.dp))
            }

            Text(
                text = stringResource(R.string.add_candidate_heading),
                color = Color(0xFFF1A364),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                stringResource(R.string.add_candidate_description),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Color(0xFFB9C2D8)
            )

            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF161D2F))
                        .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(22.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.add_candidate_source_label),
                                color = Color(0xFF9EA9C4),
                                fontSize = 12.sp
                            )
                            Text(
                                text = label,
                                color = Color(0xFFF0F4FF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFC8D0E6)
                        )
                    }
                }
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

            TextField(
                value = text,
                onValueChange = {
                    text = it
                    aiError = null
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.add_candidate_placeholder),
                        color = Color(0xFF8E9AB5),
                        fontSize = 14.sp
                    )
                },
                minLines = 8,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF151C2E),
                    unfocusedContainerColor = Color(0xFF151C2E),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color(0xFFF0F4FF),
                    unfocusedTextColor = Color(0xFFF0F4FF),
                    cursorColor = Color(0xFFF0F4FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(24.dp))
            )
            aiError?.let {
                Text(
                    it,
                    color = Color(0xFFFF8D8D),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            SecondaryActionButton(
                onClick = {
                    if (text.isBlank()) {
                        aiError = context.getString(R.string.add_candidate_input_required)
                        return@SecondaryActionButton
                    }
                    aiLoading = true
                    aiError = null
                    vm.aiAssist(
                        text,
                        onOk = { s ->
                            aiLoading = false
                            val merged = buildString {
                                s.title?.let { append(context.getString(R.string.add_candidate_ai_title_prefix)).append(it).append('\n') }
                                s.courseHint?.let { append(context.getString(R.string.add_candidate_ai_course_prefix)).append(it).append('\n') }
                                s.dueAtEpoch?.let { append(context.getString(R.string.add_candidate_ai_due_prefix)).append(it).append('\n') }
                                s.taskType?.let { append(context.getString(R.string.add_candidate_ai_type_prefix)).append(it.name).append('\n') }
                                s.urgency?.let { append(context.getString(R.string.add_candidate_ai_urgency_prefix)).append(it.name).append('\n') }
                                append('\n').append(context.getString(R.string.add_candidate_ai_original_prefix)).append('\n').append(text)
                            }
                            text = merged
                        },
                        onErr = { msg ->
                            aiLoading = false
                            aiError = msg.ifBlank { context.getString(R.string.add_candidate_ai_failed) }
                        }
                    )
                },
                enabled = !aiLoading,
                icon = Icons.Outlined.AutoAwesome,
                label = if (aiLoading) {
                    stringResource(R.string.add_candidate_ai_loading)
                } else {
                    stringResource(R.string.add_candidate_ai_action)
                }
            ) {
                if (aiLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFEAF0FF)
                    )
                }
            }
            PrimaryGradientButton(
                onClick = {
                    if (text.isNotBlank()) {
                        vm.submit(text, selected) { id -> onCreated(id) }
                    }
                },
                enabled = text.isNotBlank() && !aiLoading,
                label = stringResource(R.string.add_candidate_submit)
            ) {
                if (aiLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x2EFFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFE8EDFA),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SecondaryActionButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    extraContent: @Composable RowScope.() -> Unit = {}
) {
    val bg = if (enabled) Color(0xFF1A2237) else Color(0xFF131A2A)
    val fg = if (enabled) Color(0xFFEAF0FF) else Color(0xFF7C879F)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        extraContent()
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = fg,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PrimaryGradientButton(
    onClick: () -> Unit,
    enabled: Boolean,
    label: String,
    extraContent: @Composable RowScope.() -> Unit = {}
) {
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(Color(0xFFF39C58), Color(0xFFE66CD0)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF5B5F70), Color(0xFF4A4E5E)))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(brush)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        extraContent()
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
