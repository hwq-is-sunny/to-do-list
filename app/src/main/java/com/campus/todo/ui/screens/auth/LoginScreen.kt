package com.campus.todo.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.ui.AppViewModelFactory
import com.campus.todo.ui.components.DeepCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    factory: AppViewModelFactory,
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = viewModel(factory = factory)
) {
    var username by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录") },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DeepCard {
                Text(
                    "数据保存在本机。输入昵称即可开始使用（演示用本地会话，非云端账号）。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("昵称 / 学号") },
                singleLine = true,
                isError = error != null
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (username.isBlank()) {
                        error = "请输入昵称或学号"
                    } else {
                        vm.login(username) { onLoggedIn() }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("进入应用")
            }
        }
    }
}
