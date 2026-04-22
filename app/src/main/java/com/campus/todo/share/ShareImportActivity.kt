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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.campus.todo.CampusTodoApp
import com.campus.todo.MainActivity
import com.campus.todo.R
import com.campus.todo.data.db.entity.SourceKind
import com.campus.todo.ui.theme.CampusTodoTheme
import kotlinx.coroutines.launch

class ShareImportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as CampusTodoApp
        val raw = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()

        setContent {
            CampusTodoTheme {
                val preview = remember(raw) {
                    raw.ifEmpty { getString(R.string.share_import_missing_text) }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(stringResource(R.string.share_import_title_text), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.share_import_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = preview,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.share_import_preview_label)) },
                        minLines = 4
                    )
                    Button(
                        onClick = {
                            if (raw.isNotBlank()) {
                                lifecycleScope.launch {
                                    val id = app.repository.ingestRawText(raw, SourceKind.OTHER)
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
                        Text(stringResource(R.string.share_import_confirm))
                    }
                    TextButton(onClick = { finish() }) { Text(stringResource(R.string.common_close)) }
                }
            }
        }
    }
}
