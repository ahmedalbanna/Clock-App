package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.ClockViewModel

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: ClockViewModel) {
    val remainingMs by viewModel.timerRemaining.collectAsState()
    val totalMs by viewModel.timerTotal.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val showCompletionAlert by viewModel.showTimerCompletion.collectAsState()

    // Picks input states when NOT active/running
    var hourInput by remember { mutableStateOf("00") }
    var minuteInput by remember { mutableStateOf("15") }
    var secondInput by remember { mutableStateOf("00") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Core Header
        Text(
            text = "المؤقت التنازلي",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Right
        )

        Spacer(modifier = Modifier.weight(0.2f))

        // Large animated switching box: Configuration Pickers vs Circle Progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f),
            contentAlignment = Alignment.Center
        ) {
            if (totalMs <= 0L) {
                // NOT RUNNING: Render input pickers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Seconds Reel Input Card
                    TimerInputCard(
                        value = secondInput,
                        onValueChange = { input -> 
                            if (input.length <= 2 && input.all { it.isDigit() }) secondInput = input 
                        },
                        label = "ثانية",
                        testTag = "timer_second_input"
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Minutes Reel Input Card
                    TimerInputCard(
                        value = minuteInput,
                        onValueChange = { input -> 
                            if (input.length <= 2 && input.all { it.isDigit() }) minuteInput = input 
                        },
                        label = "دقيقة",
                        testTag = "timer_minute_input"
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Hours Reel Input Card
                    TimerInputCard(
                        value = hourInput,
                        onValueChange = { input -> 
                            if (input.length <= 2 && input.all { it.isDigit() }) hourInput = input 
                        },
                        label = "ساعة",
                        testTag = "timer_hour_input"
                    )
                }
            } else {
                // RUNNING/PAUSED: Render Circular progress
                val progressFraction = if (totalMs > 0) remainingMs.toFloat() / totalMs.toFloat() else 0f
                
                // Format active display string
                val totSecs = remainingMs / 1000
                val displayHrs = totSecs / 3600
                val displayMins = (totSecs % 3600) / 60
                val displaySecs = totSecs % 60
                val countdownStr = String.format("%02d:%02d:%02d", displayHrs, displayMins, displaySecs)

                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress Canvas
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    
                    val animatedProgress by animateFloatAsState(targetValue = progressFraction)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        
                        // Draw Background Track
                        drawCircle(
                            color = trackColor,
                            style = Stroke(width = strokeWidth)
                        )

                        // Draw Neon Orange (NeonAmber) active progress arc
                        drawArc(
                            color = secondaryColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Display numerical text in center
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = countdownStr,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("timer_countdown_text")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRunning) "قيد التشغيل" else "موقوف مؤقتاً",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))

        // Aligned Action Controls Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (totalMs <= 0L) {
                // START Action Button Only
                Button(
                    onClick = {
                        val hours = hourInput.toIntOrNull() ?: 0
                        val mins = minuteInput.toIntOrNull() ?: 15
                        val secs = secondInput.toIntOrNull() ?: 0
                        viewModel.startTimer(hours, mins, secs)
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .testTag("timer_start_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "ابدأ المؤقت", modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "ابدأ المؤقت التنازلي",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // RUNNING Buttons: Pause/Resume and Cancel (Stop)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel/Stop Button
                    Button(
                        onClick = { viewModel.resetTimer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .width(120.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .testTag("timer_cancel_btn")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "إلغاء", modifier = Modifier.padding(end = 4.dp))
                        Text(
                            text = "إلغاء",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    // Play/Pause toggler
                    Button(
                        onClick = {
                            if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            contentColor = if (isRunning) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .width(150.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .testTag("timer_pause_resume_btn")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "إيقاف مؤقت" else "استئناف",
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = if (isRunning) "إيقاف مؤقت" else "استئناف",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.2f))
    }

    // Timer Finished Notification Dialog alert
    if (showCompletionAlert) {
        Dialog(onDismissRequest = {}) {
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "انتهى الوقت!",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "انتهى الوقت! 🔔",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "لقد أكمل المؤقت التنازلي بنجاح الوقت المطلوب.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { viewModel.dismissTimerAlert() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("timer_dismiss_alert_btn")
                    ) {
                        Text(
                            text = "إيقاف رنين الجرس",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(80.dp)
                .height(80.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Center textField input
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(testTag),
                    singleLine = true
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
