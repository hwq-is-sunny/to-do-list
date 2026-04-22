package com.campus.todo.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.R
import com.campus.todo.data.settings.LlmProvider
import com.campus.todo.data.settings.ReminderMethod
import com.campus.todo.notification.CampusNotificationListenerService
import com.campus.todo.ui.AppViewModelFactory

@Composable
fun SettingsScreen(
    factory: AppViewModelFactory,
    onLogout: () -> Unit,
) {
    val vm: SettingsViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val state by vm.state.collectAsStateWithLifecycle()
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showTomatoDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var pendingPermissionTarget by remember { mutableStateOf<NotificationPermissionTarget?>(null) }
    var reminderDraftMethod by remember(state.settings.reminderMethod) {
        mutableStateOf(state.settings.reminderMethod)
    }
    var reminderDraftRingtone by remember(state.settings.ringtoneUri) {
        mutableStateOf(state.settings.ringtoneUri)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationListenerOn by remember {
        mutableStateOf(CampusNotificationListenerService.isListenerEnabled(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationListenerOn = CampusNotificationListenerService.isListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(
            context,
            if (granted) "通知权限已开启" else "通知权限未开启，提醒可能无法显示",
            Toast.LENGTH_SHORT
        ).show()
        when (pendingPermissionTarget) {
            NotificationPermissionTarget.TASK_REMINDER -> vm.setTaskReminderEnabled(granted)
            NotificationPermissionTarget.TOMATO_FOCUS -> vm.setTomatoFocusEnabled(granted)
            null -> Unit
        }
        pendingPermissionTarget = null
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val picked: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            picked?.let { reminderDraftRingtone = it.toString() }
        }
    }

    fun ensureNotificationPermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted()
            return
        }
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onGranted()
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleActionIcon(
                        icon = Icons.Outlined.ArrowBack,
                        onClick = { activity?.onBackPressedDispatcher?.onBackPressed() }
                    )
                    Text(
                        text = "设置",
                        color = Color(0xFFF1F5FF),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                    CircleActionIcon(
                        icon = Icons.Outlined.Logout,
                        onClick = {
                            showLogoutConfirmDialog = true
                        }
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2A3042), Color(0xFF202738))
                            )
                        )
                        .border(1.dp, Color(0x2CFFFFFF), RoundedCornerShape(26.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TopFeatureChip(
                            icon = Icons.Outlined.Language,
                            title = "语言",
                            onClick = { Toast.makeText(context, "当前版本仅支持中文", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f)
                        )
                        TopFeatureChip(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "智能规划",
                            onClick = { showAiDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        TopFeatureChip(
                            icon = Icons.Outlined.Timer,
                            title = "番茄专注",
                            onClick = { showTomatoDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SettingsGroupCard {
                    SettingsArrowRow(
                        title = "昵称",
                        trailingText = state.settings.nickname.ifBlank { "同学" },
                        onClick = { showNicknameDialog = true }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "语言",
                        trailingText = "中文",
                        onClick = { Toast.makeText(context, "当前版本仅支持中文", Toast.LENGTH_SHORT).show() }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "智能规划",
                        trailingText = aiPlanningLabel(state.settings),
                        onClick = { showAiDialog = true }
                    )
                }
            }

            item {
                Text(
                    text = "提醒设置",
                    color = Color(0xFFF08B4A),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                SettingsGroupCard {
                    SettingsSwitchRow(
                        title = "任务提醒通知",
                        checked = state.settings.taskReminderEnabled && state.settings.notificationsEnabled,
                        onCheckedChange = {
                            if (it) {
                                pendingPermissionTarget = NotificationPermissionTarget.TASK_REMINDER
                                ensureNotificationPermission { vm.setTaskReminderEnabled(true) }
                            } else {
                                vm.setTaskReminderEnabled(false)
                            }
                        }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "默认提醒方式与铃声",
                        trailingText = reminderMethodLabel(state.settings.reminderMethod),
                        onClick = {
                            reminderDraftMethod = state.settings.reminderMethod
                            reminderDraftRingtone = state.settings.ringtoneUri
                            showReminderDialog = true
                        }
                    )
                    GroupDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_notification_import),
                        subtitle = stringResource(R.string.settings_notification_import_hint),
                        checked = state.settings.notificationImportEnabled,
                        onCheckedChange = vm::setNotificationImportEnabled
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = stringResource(R.string.settings_notification_listener_open),
                        trailingText = if (notificationListenerOn) {
                            stringResource(R.string.settings_notification_listener_status_on)
                        } else {
                            stringResource(R.string.settings_notification_listener_status_off)
                        },
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        }
                    )
                }
            }

            item {
                Text(
                    text = "番茄专注",
                    color = Color(0xFFF08B4A),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                SettingsGroupCard {
                    SettingsSwitchRow(
                        title = "开启专注休息提醒",
                        checked = state.settings.tomatoFocusEnabled,
                        onCheckedChange = {
                            if (it) {
                                pendingPermissionTarget = NotificationPermissionTarget.TOMATO_FOCUS
                                ensureNotificationPermission { vm.setTomatoFocusEnabled(true) }
                            } else {
                                vm.setTomatoFocusEnabled(false)
                            }
                        }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "提醒间隔",
                        trailingText = "${state.settings.tomatoFocusIntervalMinutes} 分钟",
                        onClick = { showTomatoDialog = true }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "立即测试一次专注提醒",
                        onClick = {
                            vm.triggerTomatoFocusTest()
                            Toast.makeText(context, "测试提醒已发送", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Text(
                    text = "日历视图",
                    color = Color(0xFFF08B4A),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                SettingsGroupCard {
                    SettingsSwitchRow(
                        title = "显示周数",
                        checked = state.settings.displayWeekCount,
                        onCheckedChange = vm::setDisplayWeekCount
                    )
                }
            }
        }
    }

    if (showNicknameDialog) {
        NicknameDialog(
            currentNickname = state.settings.nickname,
            onDismiss = { showNicknameDialog = false },
            onSave = { nickname ->
                vm.setNickname(nickname)
                showNicknameDialog = false
                Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAiDialog) {
        AiConfigDialog(
            currentEnabled = state.settings.aiParseEnabled,
            currentProvider = state.settings.llmProvider,
            currentBaseUrl = state.settings.normalizedAiBaseUrl,
            currentApiKey = state.settings.aiApiKey,
            currentModel = state.settings.normalizedAiModel,
            currentTimeoutSeconds = state.settings.normalizedTimeoutSeconds,
            onDismiss = { showAiDialog = false },
            onSave = { enabled, provider, baseUrl, apiKey, model, timeout ->
                vm.saveAiConfig(enabled, provider, baseUrl, apiKey, model, timeout)
                Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
                showAiDialog = false
            }
        )
    }

    if (showTomatoDialog) {
        TomatoFocusDialog(
            currentInterval = state.settings.tomatoFocusIntervalMinutes,
            onDismiss = { showTomatoDialog = false },
            onConfirm = { minutes ->
                vm.setTomatoFocusInterval(minutes)
                showTomatoDialog = false
            }
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            currentMethod = reminderDraftMethod,
            currentRingtoneUri = reminderDraftRingtone,
            onDismiss = { showReminderDialog = false },
            onMethodChange = { reminderDraftMethod = it },
            onRingtoneChange = { reminderDraftRingtone = it },
            onPickRingtone = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    val existingUri = reminderDraftRingtone.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    putExtra(
                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        existingUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    )
                }
                ringtonePickerLauncher.launch(intent)
            },
            onSave = { method, ringtoneUri ->
                vm.setReminderMethod(method)
                vm.setRingtoneUri(ringtoneUri)
                Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
                showReminderDialog = false
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        vm.logout(onDone = onLogout)
                    }
                ) {
                    Text("确认退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("取消")
                }
            },
            title = { Text("确认退出登录") },
            text = { Text("确定要退出当前登录状态吗？") }
        )
    }
}

@Composable
private fun NicknameDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var nickname by remember(currentNickname) { mutableStateOf(currentNickname) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(nickname.trim()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = { Text("昵称") },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it.take(20) },
                label = { Text("用于首页问候语展示") },
                singleLine = true
            )
        }
    )
}

@Composable
private fun AiConfigDialog(
    currentEnabled: Boolean,
    currentProvider: String,
    currentBaseUrl: String,
    currentApiKey: String,
    currentModel: String,
    currentTimeoutSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (Boolean, LlmProvider, String, String, String, Int) -> Unit
) {
    var enabled by remember(currentEnabled) { mutableStateOf(currentEnabled) }
    var provider by remember(currentProvider) {
        mutableStateOf(
            runCatching { LlmProvider.valueOf(currentProvider.trim().uppercase()) }
                .getOrElse {
                    if (currentProvider.equals("deepseek", true) || currentProvider.equals("remote", true)) {
                        LlmProvider.DEEPSEEK
                    } else {
                        LlmProvider.CUSTOM
                    }
                }
        )
    }
    var baseUrl by remember(currentBaseUrl) { mutableStateOf(currentBaseUrl) }
    var apiKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var model by remember(currentModel) { mutableStateOf(currentModel) }
    var timeoutText by remember(currentTimeoutSeconds) { mutableStateOf(currentTimeoutSeconds.toString()) }
    var showApiKey by remember { mutableStateOf(false) }
    val timeout = timeoutText.toIntOrNull()?.coerceIn(10, 120) ?: 45
    val canSave = if (!enabled) true else baseUrl.startsWith("http") && model.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = { onSave(enabled, provider, baseUrl, apiKey, model, timeout) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = { Text("智能规划") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "在本地保存接口配置；开启后，部分智能解析能力会使用这里的网址、密钥与模型。",
                    fontSize = 13.sp,
                    color = Color(0xFF6C7285)
                )
                SelectableRow(
                    selected = enabled,
                    onClick = { enabled = !enabled },
                    title = "启用智能规划"
                )
                SelectableRow(
                    selected = provider == LlmProvider.DEEPSEEK,
                    onClick = { provider = LlmProvider.DEEPSEEK },
                    title = "提供商：深度求索（DeepSeek）"
                )
                SelectableRow(
                    selected = provider == LlmProvider.CUSTOM,
                    onClick = { provider = LlmProvider.CUSTOM },
                    title = "提供商：自定义"
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("接口地址") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("接口密钥") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Icon(
                            imageVector = if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.clickable { showApiKey = !showApiKey }
                        )
                    }
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型名称") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = timeoutText,
                    onValueChange = { timeoutText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("请求超时时间（秒）") },
                    singleLine = true
                )
                if (!canSave) {
                    Text(
                        text = "启用智能规划时需填写有效的接口网址和模型名称。",
                        color = Color(0xFFB96D6D),
                        fontSize = 12.sp
                    )
                }
            }
        }
    )
}

@Composable
private fun TomatoFocusDialog(
    currentInterval: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val options = listOf(15, 25, 30, 45, 60)
    var selected by remember(currentInterval) { mutableIntStateOf(currentInterval) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = { Text("提醒间隔") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { minutes ->
                    SelectableRow(
                        selected = selected == minutes,
                        onClick = { selected = minutes },
                        title = "${minutes} 分钟"
                    )
                }
            }
        }
    )
}

@Composable
private fun ReminderDialog(
    currentMethod: ReminderMethod,
    currentRingtoneUri: String,
    onDismiss: () -> Unit,
    onMethodChange: (ReminderMethod) -> Unit,
    onRingtoneChange: (String) -> Unit,
    onPickRingtone: () -> Unit,
    onSave: (ReminderMethod, String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        currentMethod,
                        if (currentMethod == ReminderMethod.VIBRATE || currentMethod == ReminderMethod.SILENT) {
                            ""
                        } else {
                            currentRingtoneUri
                        }
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        title = { Text("默认提醒方式与铃声") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectableRow(
                    selected = currentMethod == ReminderMethod.SOUND,
                    onClick = { onMethodChange(ReminderMethod.SOUND) },
                    title = "声音"
                )
                SelectableRow(
                    selected = currentMethod == ReminderMethod.VIBRATE,
                    onClick = {
                        onMethodChange(ReminderMethod.VIBRATE)
                        onRingtoneChange("")
                    },
                    title = "震动提醒"
                )
                SelectableRow(
                    selected = currentMethod == ReminderMethod.SOUND_AND_VIBRATE,
                    onClick = { onMethodChange(ReminderMethod.SOUND_AND_VIBRATE) },
                    title = "声音 + 震动"
                )
                SelectableRow(
                    selected = currentMethod == ReminderMethod.SILENT,
                    onClick = {
                        onMethodChange(ReminderMethod.SILENT)
                        onRingtoneChange("")
                    },
                    title = "静默提醒"
                )
                if (currentMethod == ReminderMethod.SOUND || currentMethod == ReminderMethod.SOUND_AND_VIBRATE) {
                    TextButton(onClick = onPickRingtone) {
                        Text("选择铃声")
                    }
                    Text(
                        text = ringtoneLabel(
                            context = context,
                            uriString = currentRingtoneUri
                        ),
                        color = Color(0xFF6C7285),
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = if (currentMethod == ReminderMethod.SILENT) {
                            "静默模式下不会播放声音，也不会震动。"
                        } else {
                            "震动模式下不会播放铃声。"
                        },
                        color = Color(0xFF6C7285),
                        fontSize = 13.sp
                    )
                }
            }
        }
    )
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = title,
            fontSize = 15.sp,
            color = Color(0xFF1C2230)
        )
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
private fun TopFeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1AFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFF2914E),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = title,
            color = Color(0xFFE8EEFB),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF232B3E), Color(0xFF1A2234))
                )
            )
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
private fun SettingsArrowRow(
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFFECF1FF),
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        trailingText?.let {
            Text(
                text = it,
                color = Color(0xA9E0E7FA),
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color(0x8FE7EDFD),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFECF1FF),
                fontSize = 16.sp
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color(0xFF9AA4BD),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFDAD8FF),
                checkedTrackColor = Color(0xFF7E74D8),
                uncheckedThumbColor = Color(0xFFB7BFD3),
                uncheckedTrackColor = Color(0xFF404A63),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun GroupDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 8.dp)
            .background(Color(0x1EFFFFFF))
    )
}

@Composable
private fun reminderMethodLabel(method: ReminderMethod): String = when (method) {
    ReminderMethod.SOUND -> "声音"
    ReminderMethod.VIBRATE -> "震动提醒"
    ReminderMethod.SOUND_AND_VIBRATE -> "声音 + 震动"
    ReminderMethod.SILENT -> "静默提醒"
}

@Composable
private fun aiPlanningLabel(settings: com.campus.todo.data.settings.AppSettings): String {
    if (!settings.aiParseEnabled) return "已关闭"
    val provider = when {
        settings.llmProvider.equals("deepseek", true) || settings.llmProvider.equals("remote", true) ->
            "提供商：深度求索"
        settings.llmProvider.equals("custom", true) || settings.llmProvider.isBlank() ->
            "提供商：自定义"
        else -> "提供商：${settings.llmProvider}"
    }
    return "$provider · 模型 ${settings.normalizedAiModel}"
}

private fun ringtoneLabel(context: android.content.Context, uriString: String): String {
    if (uriString.isBlank()) return "系统默认通知铃声"
    return runCatching {
        val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))
        ringtone?.getTitle(context)
    }.getOrNull().orEmpty().ifBlank {
        "系统默认通知铃声"
    }
}

private enum class NotificationPermissionTarget {
    TASK_REMINDER,
    TOMATO_FOCUS
}
