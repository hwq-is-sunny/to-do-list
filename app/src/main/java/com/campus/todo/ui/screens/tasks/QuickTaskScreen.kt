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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.ui.AppViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun QuickTaskScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
    onCreated: (Long) -> Unit,
) {
    val vm: QuickTaskViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var dueText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF060A19), Color(0xFF0A1022), Color(0xFF070B1A))))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x2EFFFFFF), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = Color(0xFFE8EDFA), modifier = Modifier.size(20.dp))
                }
                Text("新建任务", color = Color(0xFFF2F5FC), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Box(Modifier.width(40.dp))
            }
            Text("标题", color = Color(0xFFDCE3F6), fontSize = 13.sp)
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("要做什么", color = Color(0xFF6B758E)) },
                singleLine = true,
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
            Text("截止日（可选，格式 2026-04-21）", color = Color(0xFFDCE3F6), fontSize = 13.sp)
            TextField(
                value = dueText,
                onValueChange = { dueText = it },
                placeholder = { Text("留空则无截止日", color = Color(0xFF6B758E)) },
                singleLine = true,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF39C58), Color(0xFFE66CD0))))
                    .clickable {
                        val due = dueText.trim().takeIf { it.isNotBlank() }?.let { d ->
                            runCatching {
                                val date = LocalDate.parse(d)
                                date.atTime(LocalTime.of(23, 59)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            }.getOrNull()
                        }
                        if (title.isBlank()) {
                            Toast.makeText(context, "请填写标题", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        if (dueText.isNotBlank() && due == null) {
                            Toast.makeText(context, "日期格式应为 2026-04-21（年-月-日）", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        vm.create(title, due) { id -> onCreated(id) }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("保存", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
