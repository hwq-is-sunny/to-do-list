package com.campus.todo.ui.screens.settings

import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.ui.AppViewModelFactory

@Composable
fun SettingsScreen(
    factory: AppViewModelFactory,
    onLogout: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = factory)
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var autoSync by rememberSaveable { mutableStateOf(true) }
    var meetingReminder by rememberSaveable { mutableStateOf(true) }
    var displayWeekCount by rememberSaveable { mutableStateOf(true) }

    val onFeedback: (String) -> Unit = { text ->
        Toast.makeText(context, "$text 功能后续接入", Toast.LENGTH_SHORT).show()
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
                        text = "Settings",
                        color = Color(0xFFF1F5FF),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                    CircleActionIcon(
                        icon = Icons.Outlined.Menu,
                        onClick = { onFeedback("菜单") }
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
                            icon = Icons.Outlined.Palette,
                            title = "Theme Colors",
                            onClick = { onFeedback("Theme Colors") },
                            modifier = Modifier.weight(1f)
                        )
                        TopFeatureChip(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "AI Planning",
                            onClick = { onFeedback("AI Planning") },
                            modifier = Modifier.weight(1f)
                        )
                        TopFeatureChip(
                            icon = Icons.Outlined.Timer,
                            title = "Tomato Focus",
                            onClick = { onFeedback("Tomato Focus") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SettingsGroupCard {
                    SettingsArrowRow(
                        title = "Schedule Import and Management",
                        onClick = { onFeedback("Schedule Import and Management") }
                    )
                    GroupDivider()
                    SettingsSwitchRow(
                        title = "Automatic schedule synchronization",
                        checked = autoSync,
                        onCheckedChange = { autoSync = it }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "On the computer and ipad access to schedule",
                        onClick = { onFeedback("Desktop and iPad schedule access") }
                    )
                }
            }

            item {
                Text(
                    text = "Schedule reminder",
                    color = Color(0xFFF08B4A),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                SettingsGroupCard {
                    SettingsArrowRow(
                        title = "Default reminder time",
                        onClick = { onFeedback("Default reminder time") }
                    )
                    GroupDivider()
                    SettingsSwitchRow(
                        title = "Meeting schedule alarm reminder",
                        checked = meetingReminder,
                        onCheckedChange = { meetingReminder = it }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "Default reminder method and ringtone",
                        onClick = { onFeedback("Default reminder method and ringtone") }
                    )
                }
            }

            item {
                Text(
                    text = "Calendar view",
                    color = Color(0xFFF08B4A),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            item {
                SettingsGroupCard {
                    SettingsArrowRow(
                        title = "Other calendars",
                        trailingText = "Chinese Lunar Calender",
                        onClick = { onFeedback("Other calendars") }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "Time zone priority",
                        trailingText = "Closed",
                        onClick = { onFeedback("Time zone priority") }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "The beginning of the week",
                        trailingText = "Regional default",
                        onClick = { onFeedback("The beginning of the week") }
                    )
                    GroupDivider()
                    SettingsSwitchRow(
                        title = "Display the number of weeks",
                        checked = displayWeekCount,
                        onCheckedChange = { displayWeekCount = it }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "Moon view selection",
                        trailingText = "Default view",
                        onClick = { onFeedback("Moon view selection") }
                    )
                    GroupDivider()
                    SettingsArrowRow(
                        title = "Festival Management",
                        onClick = { onFeedback("Festival Management") }
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
