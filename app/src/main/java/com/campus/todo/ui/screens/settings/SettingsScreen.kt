package com.campus.todo.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.ui.components.DeepCard
import com.campus.todo.ui.components.SectionHeader
import com.campus.todo.ui.components.SoftCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    factory: AppViewModelFactory,
    onLogout: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = factory)
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val username by vm.username.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            DeepCard {
                Text("简洁、低焦虑：只做普通/重要/紧急提醒，不做完成率和排行。")
            }
            SectionHeader("账号")
            SoftCard {
                Text(
                    if (username.isNotBlank()) "当前用户：$username" else "未登录",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedButton(
                onClick = { vm.logout(onLogout) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录")
            }
            SectionHeader("通知权限")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                SoftCard {
                    Text(
                        if (granted) "通知权限已开启" else "需要通知权限才能提醒你临近截止的事项",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (!granted) {
                    Button(onClick = {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) {
                        Text("请求通知权限")
                    }
                }
            } else {
                SoftCard {
                    Text(
                        "当前系统版本会自动展示提醒通知。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SectionHeader("数据")
            SoftCard {
                Text(
                    "数据仅存本机，未接教务与 IM 真实接口。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
