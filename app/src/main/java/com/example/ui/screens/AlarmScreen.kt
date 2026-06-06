package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Alarm
import com.example.ui.ClockViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(viewModel: ClockViewModel) {
    val alarms by viewModel.allAlarms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAlarmForEdit by remember { mutableStateOf<Alarm?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arab heading
            Text(
                text = "المنبه",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Right
            )

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "لا يوجد منبهات",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "لا توجد منبهات مجدولة.\nاضغط على الزر لإضافة منبه جديد.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms) { alarm ->
                        AlarmItem(
                            alarm = alarm,
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onDelete = { viewModel.deleteAlarm(alarm) },
                            onClick = { selectedAlarmForEdit = alarm }
                        )
                    }
                }
            }
        }

        // Add alarm FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_alarm_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة منبه")
        }
    }

    if (showAddDialog) {
        AddEditAlarmDialog(
            onDismiss = { showAddDialog = false },
            onSave = { hr, min, lbl, days, vib ->
                viewModel.saveAlarm(hr, min, lbl, days, vib)
                showAddDialog = false
            }
        )
    }

    if (selectedAlarmForEdit != null) {
        val editing = selectedAlarmForEdit!!
        AddEditAlarmDialog(
            alarmToEdit = editing,
            onDismiss = { selectedAlarmForEdit = null },
            onSave = { hr, min, lbl, days, vib ->
                viewModel.updateAlarmFull(editing.id, hr, min, lbl, days, vib, editing.isEnabled)
                selectedAlarmForEdit = null
            }
        )
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    // Standard Format (Hour 12 with AM/PM Arabized)
    val amPmStr = if (alarm.hour < 12) "ص" else "م"
    val displayHour = when {
        alarm.hour == 0 -> 12
        alarm.hour > 12 -> alarm.hour - 12
        else -> alarm.hour
    }
    val timeFormatted = String.format("%02d:%02d", displayHour, alarm.minute)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("alarm_item_${alarm.id}")
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right section: Toggle Switch + info
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("alarm_switch_${alarm.id}")
            )

            // Middle section: Name and Days (RTL style layout alignment)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (alarm.isVibrate) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "هزاز",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        ),
                        textAlign = TextAlign.Right
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                // Render week days indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val daysArabic = listOf(
                        Pair(7, "س"), // Sat
                        Pair(6, "ج"), // Fri
                        Pair(5, "خ"), // Thu
                        Pair(4, "ر"), // Wed
                        Pair(3, "ث"), // Tue
                        Pair(2, "ن"), // Mon
                        Pair(1, "ح")  // Sun
                    )
                    daysArabic.forEach { (calDay, literal) ->
                        val isDaySelected = alarm.getRepeatDaysSet().contains(calDay)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDaySelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = literal,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Left sections: Large active time representation + delete
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف المنبه",
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = amPmStr,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmDialog(
    alarmToEdit: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, repeatDays: Set<Int>, isVibrate: Boolean) -> Unit
) {
    var hourInput by remember { mutableStateOf(alarmToEdit?.hour?.toString() ?: "07") }
    var minuteInput by remember { mutableStateOf(alarmToEdit?.minute?.toString() ?: "30") }
    var labelInput by remember { mutableStateOf(alarmToEdit?.label ?: "") }
    var selectedDays by remember { mutableStateOf(alarmToEdit?.getRepeatDaysSet() ?: emptySet()) }
    var isVibrate by remember { mutableStateOf(alarmToEdit?.isVibrate ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (alarmToEdit == null) "إضافة منبه جديد" else "تعديل المنبه",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Hour and minute selectors side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minute Field
                    OutlinedTextField(
                        value = minuteInput,
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all { it.isDigit() }) {
                                minuteInput = input
                            }
                        },
                        label = { Text("الدقيقة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Hour Field
                    OutlinedTextField(
                        value = hourInput,
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all { it.isDigit() }) {
                                hourInput = input
                            }
                        },
                        label = { Text("الساعة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Label Text Field Input
                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text("اسم أو تسمية المنبه (مثال: الاستيقاظ للعمل)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Repeat weekday selector blocks
                Text(
                    text = "أيام التكرار أسبوعياً",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Right
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val daysArabic = listOf(
                        Pair(7, "س"), // Sat
                        Pair(6, "ج"), // Fri
                        Pair(5, "خ"), // Thu
                        Pair(4, "ر"), // Wed
                        Pair(3, "ث"), // Tue
                        Pair(2, "ن"), // Mon
                        Pair(1, "ح")  // Sun
                    )

                    daysArabic.forEach { (calDay, literal) ->
                        val isSelected = selectedDays.contains(calDay)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        selectedDays - calDay
                                    } else {
                                        selectedDays + calDay
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = literal,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isVibrate,
                        onCheckedChange = { isVibrate = it }
                    )
                    Text(
                        text = "تفيل الهزاز عند التنبيه",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val hr = hourInput.toIntOrNull() ?: 0
                            val min = minuteInput.toIntOrNull() ?: 0
                            val boundedHr = hr.coerceIn(0, 23)
                            val boundedMin = min.coerceIn(0, 59)
                            onSave(boundedHr, boundedMin, labelInput, selectedDays, isVibrate)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حفظ")
                    }
                }
            }
        }
    }
}
