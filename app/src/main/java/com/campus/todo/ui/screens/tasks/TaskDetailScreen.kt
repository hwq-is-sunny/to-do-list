package com.campus.todo.ui.screens.tasks

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.data.db.entity.Task
import com.campus.todo.data.db.entity.TaskStatus
import com.campus.todo.data.db.entity.TaskType
import com.campus.todo.data.db.entity.UrgencyLevel
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.util.TimeUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun TaskDetailScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
) {
    val vm: TaskDetailViewModel = viewModel(factory = factory)
    val task by vm.task.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("") }
    var taskType by remember { mutableStateOf(TaskType.OTHER) }
    var urgency by remember { mutableStateOf(UrgencyLevel.NORMAL) }

    LaunchedEffect(task?.id, task?.updatedAtEpoch) {
        val t = task ?: return@LaunchedEffect
        title = t.title
        description = t.description.orEmpty()
        location = t.location.orEmpty()
        dueDateText = t.dueAtEpoch?.let { TimeUtils.formatDate(it) }.orEmpty()
        taskType = t.taskType
        urgency = t.urgency
    }

    val t = task
    if (t == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080C1A)),
            contentAlignment = Alignment.Center
        ) {
            Text("加载中…", color = Color(0xFFD9E0F3))
        }
        return
    }

    if (t.status == TaskStatus.DONE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF060A19), Color(0xFF0A1022))))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleBack(onBack)
                    Text("任务已完成", color = Color(0xFFF2F5FC), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.width(40.dp))
                }
                Text("该任务已标记完成，可删除记录或返回。", color = Color(0xFFB9C2D8), fontSize = 14.sp)
                TextButton(onClick = {
                    vm.delete { onBack() }
                }) { Text("删除记录", color = Color(0xFFFFB4B4)) }
                TextButton(onClick = onBack) { Text("返回", color = Color(0xFFEAF0FF)) }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF060A19), Color(0xFF0A1022), Color(0xFF070B1A))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleBack(onBack)
                Text("任务详情", color = Color(0xFFF2F5FC), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.width(40.dp))
            }

            FieldLabel("标题")
            DarkTextField(title, { title = it }, "必填")

            FieldLabel("备注")
            DarkTextField(description, { description = it }, "可选")

            FieldLabel("地点")
            DarkTextField(location, { location = it }, "可选")

            FieldLabel("截止日期（留空表示无，例 2026-04-22）")
            DarkTextField(dueDateText, { dueDateText = it }, "例如 2026-04-22")

            TypeRow(taskType, { taskType = it })
            UrgencyRow(urgency, { urgency = it })

            PrimaryBtn("保存修改") {
                val due = dueDateText.trim().takeIf { it.isNotBlank() }?.let { d ->
                    runCatching {
                        val date = LocalDate.parse(d)
                        date.atTime(LocalTime.of(23, 59)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.getOrNull()
                }
                vm.save(
                    t.copy(
                        title = title.trim(),
                        description = description.trim().ifBlank { null },
                        location = location.trim().ifBlank { null },
                        dueAtEpoch = due,
                        reminderAtEpoch = due,
                        taskType = taskType,
                        category = taskType.name,
                        urgency = urgency
                    )
                ) {
                    Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                }
            }
            SecondaryBtn("标记完成") {
                vm.markDone { onBack() }
            }
            SecondaryBtn("删除任务") {
                vm.delete { onBack() }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = Color(0xFFDCE3F6), fontSize = 13.sp)
}

@Composable
private fun DarkTextField(value: String, onChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = Color(0xFF6B758E), fontSize = 14.sp) },
        singleLine = placeholder.contains("备注").not() && placeholder.contains("标题"),
        minLines = if (placeholder.contains("备注")) 3 else 1,
        shape = RoundedCornerShape(20.dp),
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
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(20.dp))
    )
}

@Composable
private fun TypeRow(selected: TaskType, onSelect: (TaskType) -> Unit) {
    FieldLabel("类型")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskType.values().toList().chunked(4).forEach { chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                chunk.forEach { type ->
                    val on = selected == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (on) Color(0xFFF08B4A) else Color(0x131E2A45))
                            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(16.dp))
                            .clickable { onSelect(type) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            typeLabelZh(type),
                            color = if (on) Color.White else Color(0xFFE8EEFB),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgencyRow(selected: UrgencyLevel, onSelect: (UrgencyLevel) -> Unit) {
    FieldLabel("优先级")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        UrgencyLevel.values().forEach { u ->
            val on = selected == u
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) Color(0xFFF08B4A) else Color(0x131E2A45))
                    .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(16.dp))
                    .clickable { onSelect(u) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    when (u) {
                        UrgencyLevel.NORMAL -> "普通"
                        UrgencyLevel.IMPORTANT -> "重要"
                        UrgencyLevel.URGENT -> "紧急"
                    },
                    color = if (on) Color.White else Color(0xFFE8EEFB),
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun typeLabelZh(type: TaskType): String = when (type) {
    TaskType.HOMEWORK -> "作业"
    TaskType.EXAM -> "考试"
    TaskType.SIGN_IN -> "签到"
    TaskType.CLASS -> "课程"
    TaskType.ANNOUNCEMENT -> "通知"
    TaskType.PERSONAL -> "个人"
    TaskType.OTHER -> "其他"
}

@Composable
private fun PrimaryBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFF39C58), Color(0xFFE66CD0))))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1A2237))
            .border(1.dp, Color(0x2AFFFFFF), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFFEAF0FF), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CircleBack(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x2EFFFFFF), CircleShape)
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.ArrowBack, contentDescription = null, tint = Color(0xFFE8EDFA), modifier = Modifier.size(20.dp))
    }
}
