package com.campus.todo.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.R
import com.campus.todo.data.settings.AppLocaleManager
import com.campus.todo.data.settings.AppLanguage
import com.campus.todo.data.settings.ReminderMethod
import com.campus.todo.ui.AppViewModelFactory

@Composable
fun SettingsScreen(
    factory: AppViewModelFactory,
    onLogout: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = factory)
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val state by vm.state.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var showTomatoDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderDraftMethod by remember(state.settings.reminderMethod) {
        mutableStateOf(state.settings.reminderMethod)
    }
    var reminderDraftRingtone by remember(state.settings.ringtoneUri) {
        mutableStateOf(state.settings.ringtoneUri)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(
            context,
            context.getString(
                if (granted) R.string.notification_permission_granted
                else R.string.notification_permission_denied
            ),
            Toast.LENGTH_SHORT
        ).show()
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

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
                        text = stringResource(R.string.settings_title),
                        color = Color(0xFFF1F5FF),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                    CircleActionIcon(
                        icon = Icons.Outlined.Logout,
                        onClick = {
                            vm.logout(onDone = onLogout)
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
                            title = stringResource(R.string.settings_language_title),
                            onClick = { showLanguageDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        TopFeatureChip(
                            icon = Icons.Outlined.AutoAwesome,
                            title = stringResource(R.string.settings_ai_planning_title),
                            onClick = { showAiDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        TopFeatureChip(
                            icon = Icons.Outlined.Timer,
                            title = stringResource(R.string.settings_tomato_focus_title),
                            onClick = { showTomatoDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SettingsGroupCard {
                    SettingsArrowRow(
                        title = stringResource(R.string.settings_language_title),
                        trailingText = languageLabel(state.currentLanguage),
                        onClick = { showLanguageDialog = true }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = stringResource(R.string.settings_ai_planning_title),
                        trailingText = state.settings.normalizedAiModel,
                        onClick = { showAiDialog = true }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_schedule_reminder_section),
                    color = Color(0xFFF08B4A),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                SettingsGroupCard {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_task_reminder_toggle),
                        checked = state.settings.taskReminderEnabled,
                        onCheckedChange = {
                            if (it) requestNotificationPermissionIfNeeded()
                            vm.setTaskReminderEnabled(it)
                        }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = stringResource(R.string.settings_reminder_method_title),
                        trailingText = reminderMethodLabel(state.settings.reminderMethod),
                        onClick = {
                            reminderDraftMethod = state.settings.reminderMethod
                            reminderDraftRingtone = state.settings.ringtoneUri
                            showReminderDialog = true
                        }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_tomato_focus_title),
                    color = Color(0xFFF08B4A),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                SettingsGroupCard {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_tomato_focus_enable),
                        checked = state.settings.tomatoFocusEnabled,
                        onCheckedChange = {
                            if (it) requestNotificationPermissionIfNeeded()
                            vm.setTomatoFocusEnabled(it)
                        }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = stringResource(R.string.settings_tomato_focus_interval_title),
                        trailingText = stringResource(
                            R.string.settings_interval_minutes_value,
                            state.settings.tomatoFocusIntervalMinutes
                        ),
                        onClick = { showTomatoDialog = true }
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_calendar_view_section),
                    color = Color(0xFFF08B4A),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                SettingsGroupCard {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_display_week_count),
                        checked = state.settings.displayWeekCount,
                        onCheckedChange = vm::setDisplayWeekCount
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            selected = state.currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onConfirm = { language ->
                showLanguageDialog = false
                vm.setLanguage(language) {
                    activity?.let { AppLocaleManager.applyToActivity(it, language.tag) }
                }
            }
        )
    }

    if (showAiDialog) {
        AiConfigDialog(
            currentBaseUrl = state.settings.normalizedAiBaseUrl,
            currentApiKey = state.settings.aiApiKey,
            currentModel = state.settings.normalizedAiModel,
            onDismiss = { showAiDialog = false },
            onSave = { baseUrl, apiKey, model ->
                vm.saveAiConfig(baseUrl, apiKey, model)
                Toast.makeText(context, context.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, context.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                showReminderDialog = false
            }
        )
    }
}

@Composable
private fun LanguageDialog(
    selected: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (AppLanguage) -> Unit
) {
    var temp by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(temp) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = { Text(stringResource(R.string.settings_language_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    SelectableRow(
                        selected = temp == language,
                        onClick = { temp = language },
                        title = when (language) {
                            AppLanguage.CHINESE -> stringResource(R.string.language_option_chinese)
                            AppLanguage.ENGLISH -> stringResource(R.string.language_option_english)
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun AiConfigDialog(
    currentBaseUrl: String,
    currentApiKey: String,
    currentModel: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var baseUrl by remember(currentBaseUrl) { mutableStateOf(currentBaseUrl) }
    var apiKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var model by remember(currentModel) { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(baseUrl, apiKey, model) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = { Text(stringResource(R.string.settings_ai_planning_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.settings_ai_planning_description),
                    fontSize = 13.sp,
                    color = Color(0xFF6C7285)
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text(stringResource(R.string.settings_ai_base_url)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.settings_ai_api_key)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(stringResource(R.string.settings_ai_model)) },
                    singleLine = true
                )
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
    val options = listOf(15, 20, 25, 30, 45, 60)
    var selected by remember(currentInterval) { mutableIntStateOf(currentInterval) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = { Text(stringResource(R.string.settings_tomato_focus_interval_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { minutes ->
                    SelectableRow(
                        selected = selected == minutes,
                        onClick = { selected = minutes },
                        title = stringResource(R.string.settings_interval_minutes_value, minutes)
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
                        if (currentMethod == ReminderMethod.VIBRATE) "" else currentRingtoneUri
                    )
                }
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = { Text(stringResource(R.string.settings_reminder_method_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectableRow(
                    selected = currentMethod == ReminderMethod.SOUND,
                    onClick = { onMethodChange(ReminderMethod.SOUND) },
                    title = stringResource(R.string.reminder_method_sound)
                )
                SelectableRow(
                    selected = currentMethod == ReminderMethod.VIBRATE,
                    onClick = {
                        onMethodChange(ReminderMethod.VIBRATE)
                        onRingtoneChange("")
                    },
                    title = stringResource(R.string.reminder_method_vibrate)
                )
                if (currentMethod == ReminderMethod.SOUND) {
                    TextButton(onClick = onPickRingtone) {
                        Text(stringResource(R.string.settings_choose_ringtone))
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
                        text = stringResource(R.string.settings_vibrate_mode_hint),
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFFECF1FF),
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
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
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.CHINESE -> stringResource(R.string.language_option_chinese)
    AppLanguage.ENGLISH -> stringResource(R.string.language_option_english)
}

@Composable
private fun reminderMethodLabel(method: ReminderMethod): String = when (method) {
    ReminderMethod.SOUND -> stringResource(R.string.reminder_method_sound)
    ReminderMethod.VIBRATE -> stringResource(R.string.reminder_method_vibrate)
}

private fun ringtoneLabel(context: android.content.Context, uriString: String): String {
    if (uriString.isBlank()) return context.getString(R.string.settings_ringtone_default)
    return runCatching {
        val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))
        ringtone?.getTitle(context)
    }.getOrNull().orEmpty().ifBlank {
        context.getString(R.string.settings_ringtone_default)
    }
}
