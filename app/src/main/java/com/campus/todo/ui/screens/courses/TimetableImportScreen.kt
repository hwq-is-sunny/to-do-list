package com.campus.todo.ui.screens.courses

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.todo.R
import com.campus.todo.ui.AppViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun TimetableImportScreen(
    factory: AppViewModelFactory,
    onBack: () -> Unit,
) {
    val vm: TimetableImportViewModel = viewModel(factory = factory)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(ImportMode.MANUAL) }
    val drafts = remember { mutableStateListOf<TimetableDraft>() }

    var courseName by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf(1) }
    var startAt by remember { mutableStateOf("08:00") }
    var endAt by remember { mutableStateOf("09:40") }
    var startPeriod by remember { mutableStateOf("") }
    var endPeriod by remember { mutableStateOf("") }
    var startWeek by remember { mutableStateOf("1") }
    var endWeek by remember { mutableStateOf("20") }
    var weekMode by remember { mutableStateOf(WeekMode.EVERY) }
    val requiredCourseTypeLabel = stringResource(R.string.timetable_import_course_type_required)
    var courseType by remember { mutableStateOf(requiredCourseTypeLabel) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var ocrLoading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        selectedImageUri = uri
        ocrLoading = true
        scope.launch {
            val result = vm.recognizeTextFromImage(context, uri)
            ocrLoading = false
            result.onSuccess { text ->
                ocrText = text
                val parsed = vm.parseTextToDrafts(text)
                drafts.clear()
                drafts.addAll(parsed)
                if (parsed.isEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.timetable_import_image_no_result),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.timetable_import_image_result_count, parsed.size),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.onFailure {
                Toast.makeText(
                    context,
                    it.message ?: context.getString(R.string.timetable_import_image_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
        Column(
            modifier = Modifier
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
                CircleActionButton(
                    icon = Icons.Outlined.ArrowBack,
                    onClick = onBack
                )
                Text(
                    text = stringResource(R.string.timetable_import_title),
                    color = Color(0xFFF2F5FC),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(modifier = Modifier.size(40.dp))
            }

            ModeTabs(selected = mode, onSelect = { mode = it })

            when (mode) {
                ImportMode.MANUAL -> {
                    ImportInputField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = stringResource(R.string.timetable_import_course_name)
                    )
                    ImportInputField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = stringResource(R.string.timetable_import_teacher)
                    )
                    DaySelector(
                        selected = dayOfWeek,
                        onSelected = { dayOfWeek = it }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ImportInputField(
                            value = startAt,
                            onValueChange = { startAt = it },
                            label = stringResource(R.string.timetable_import_start_time),
                            modifier = Modifier.weight(1f)
                        )
                        ImportInputField(
                            value = endAt,
                            onValueChange = { endAt = it },
                            label = stringResource(R.string.timetable_import_end_time),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ImportInputField(
                            value = startPeriod,
                            onValueChange = { startPeriod = it },
                            label = stringResource(R.string.timetable_import_start_period),
                            modifier = Modifier.weight(1f)
                        )
                        ImportInputField(
                            value = endPeriod,
                            onValueChange = { endPeriod = it },
                            label = stringResource(R.string.timetable_import_end_period),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ImportInputField(
                            value = startWeek,
                            onValueChange = { startWeek = it },
                            label = stringResource(R.string.timetable_import_start_week),
                            modifier = Modifier.weight(1f)
                        )
                        ImportInputField(
                            value = endWeek,
                            onValueChange = { endWeek = it },
                            label = stringResource(R.string.timetable_import_end_week),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WeekModeSelector(
                        selected = weekMode,
                        onSelected = { weekMode = it }
                    )
                    CourseTypeSelector(
                        selected = courseType,
                        onSelected = { courseType = it }
                    )
                    ImportInputField(
                        value = location,
                        onValueChange = { location = it },
                        label = stringResource(R.string.timetable_import_location)
                    )
                    PrimaryActionButton(
                        label = stringResource(R.string.timetable_import_add_course),
                        onClick = {
                            vm.buildManualDraft(
                                courseName = courseName,
                                teacher = teacher,
                                location = location,
                                dayOfWeek = dayOfWeek,
                                startPeriodText = startPeriod,
                                endPeriodText = endPeriod,
                                startTimeText = startAt,
                                endTimeText = endAt,
                                startWeek = startWeek,
                                endWeek = endWeek,
                                weekMode = weekMode,
                                courseType = courseType
                            ).onSuccess { draft ->
                                drafts.add(draft)
                                courseName = ""
                                teacher = ""
                                location = ""
                                startPeriod = ""
                                endPeriod = ""
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.timetable_import_added_preview),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: context.getString(R.string.timetable_import_invalid),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                ImportMode.IMAGE -> {
                    Text(
                        text = stringResource(R.string.timetable_import_image_mode_hint),
                        color = Color(0xFFAEB8D2),
                        fontSize = 13.sp
                    )
                    PrimaryActionButton(
                        label = stringResource(R.string.timetable_import_pick_image),
                        onClick = { imagePicker.launch("image/*") }
                    )
                    if (ocrLoading) {
                        Text(
                            text = stringResource(R.string.timetable_import_image_processing),
                            color = Color(0xFFDDE4F7),
                            fontSize = 14.sp
                        )
                    }
                    if (selectedImageUri != null) {
                        Text(
                            text = stringResource(R.string.timetable_import_image_selected),
                            color = Color(0xFFAEB8D2),
                            fontSize = 13.sp
                        )
                    }
                    if (ocrText.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.timetable_import_ocr_text_title),
                            color = Color(0xFFF2F5FC),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = ocrText.take(360),
                            color = Color(0xFFC9D2EA),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (drafts.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.timetable_import_preview_title),
                    color = Color(0xFFF2F5FC),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                drafts.forEachIndexed { index, draft ->
                    DraftEditorCard(
                        draft = draft,
                        onUpdate = { updated -> drafts[index] = updated },
                        onDelete = { drafts.removeAt(index) }
                    )
                }
                PrimaryActionButton(
                    label = stringResource(R.string.timetable_import_confirm),
                    onClick = {
                        vm.importDrafts(drafts) { ok, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (ok) {
                                drafts.clear()
                                onBack()
                            }
                        }
                    }
                )
            }
        }
    }
}

private enum class ImportMode {
    MANUAL,
    IMAGE
}

@Composable
private fun ModeTabs(
    selected: ImportMode,
    onSelect: (ImportMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ImportModeChip(
            text = stringResource(R.string.timetable_import_mode_manual),
            selected = selected == ImportMode.MANUAL,
            onClick = { onSelect(ImportMode.MANUAL) }
        )
        ImportModeChip(
            text = stringResource(R.string.timetable_import_mode_image),
            selected = selected == ImportMode.IMAGE,
            onClick = { onSelect(ImportMode.IMAGE) }
        )
    }
}

@Composable
private fun CourseTypeSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf(
        stringResource(R.string.timetable_import_course_type_required),
        stringResource(R.string.timetable_import_course_type_elective),
        stringResource(R.string.timetable_import_course_type_custom)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.timetable_import_course_type),
            color = Color(0xFFDCE3F6),
            fontSize = 13.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                ImportModeChip(
                    text = option,
                    selected = selected == option,
                    onClick = { onSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun WeekModeSelector(
    selected: WeekMode,
    onSelected: (WeekMode) -> Unit
) {
    val options = listOf(
        WeekMode.EVERY to stringResource(R.string.timetable_import_week_mode_every),
        WeekMode.ODD to stringResource(R.string.timetable_import_week_mode_odd),
        WeekMode.EVEN to stringResource(R.string.timetable_import_week_mode_even)
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.timetable_import_week_mode),
            color = Color(0xFFDCE3F6),
            fontSize = 13.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (mode, label) ->
                ImportModeChip(
                    text = label,
                    selected = selected == mode,
                    onClick = { onSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun ImportModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFFF08B4A) else Color(0x131E2A45))
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFFE8EEFB),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DaySelector(
    selected: Int,
    onSelected: (Int) -> Unit
) {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            val dayValue = index + 1
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected == dayValue) Color(0xFFF08B4A) else Color(0x131E2A45))
                    .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onSelected(dayValue) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (selected == dayValue) Color.White else Color(0xFFDCE3F6),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DraftEditorCard(
    draft: TimetableDraft,
    onUpdate: (TimetableDraft) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x101B243A))
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ImportInputField(
            value = draft.courseName,
            onValueChange = { onUpdate(draft.copy(courseName = it)) },
            label = stringResource(R.string.timetable_import_course_name)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImportInputField(
                value = draft.location,
                onValueChange = { onUpdate(draft.copy(location = it)) },
                label = stringResource(R.string.timetable_import_location),
                modifier = Modifier.weight(1f)
            )
            ImportInputField(
                value = draft.teacher,
                onValueChange = { onUpdate(draft.copy(teacher = it)) },
                label = stringResource(R.string.timetable_import_teacher),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ImportInputField(
                value = draft.startWeek.toString(),
                onValueChange = { v -> onUpdate(draft.copy(startWeek = v.toIntOrNull() ?: draft.startWeek)) },
                label = stringResource(R.string.timetable_import_start_week),
                modifier = Modifier.weight(1f)
            )
            ImportInputField(
                value = draft.endWeek.toString(),
                onValueChange = { v -> onUpdate(draft.copy(endWeek = v.toIntOrNull() ?: draft.endWeek)) },
                label = stringResource(R.string.timetable_import_end_week),
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x22ED6B6B))
                .clickable(onClick = onDelete)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.timetable_import_remove_item),
                color = Color(0xFFFFD6D6),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ImportInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF151C2E),
            unfocusedContainerColor = Color(0xFF151C2E),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color(0xFFF0F4FF),
            unfocusedTextColor = Color(0xFFF0F4FF),
            cursorColor = Color(0xFFF0F4FF),
            focusedLabelColor = Color(0xFFD1DAF1),
            unfocusedLabelColor = Color(0xFFA5B0CB)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(20.dp))
    )
}

@Composable
private fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFF39C58), Color(0xFFE66CD0))))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CircleActionButton(
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
