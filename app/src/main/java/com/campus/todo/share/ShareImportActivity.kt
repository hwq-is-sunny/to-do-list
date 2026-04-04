package com.campus.todo.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.campus.todo.CampusTodoApp
import com.campus.todo.MainActivity
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.ui.theme.CampusTodoTheme
import kotlinx.coroutines.launch

/**
 * Share Target 占位：第二轮可在此接入真实解析 / 深链路由。
 * 当前将分享文本以 [SourceKind.SHARE_IMPORT_STUB] 写入候选箱，并跳转主界面打开详情。
 */
class ShareImportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CampusTodoApp
        val raw = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()

        setContent {
            CampusTodoTheme {
                val preview = remember(raw) { raw.ifEmpty { "（未收到文本，请从支持分享的应用重试）" } }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text("分享导入（占位）", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "第二轮将完善解析规则与去重。现在可先写入候选箱并手动确认。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = preview,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分享内容预览") },
                        minLines = 4
                    )
                    Button(
                        onClick = {
                            if (raw.isNotBlank()) {
                                lifecycleScope.launch {
                                    val id = app.repository.ingestRawText(raw, SourceKind.SHARE_IMPORT_STUB)
                                    startActivity(
                                        Intent(this@ShareImportActivity, MainActivity::class.java)
                                            .putExtra(MainActivity.EXTRA_OPEN_CANDIDATE, id)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    )
                                    finish()
                                }
                            }
                        },
                        enabled = raw.isNotBlank()
                    ) {
                        Text("写入候选箱")
                    }
                    TextButton(onClick = { finish() }) { Text("关闭") }
                }
            }
        }
    }
}
