package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ClockViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StopwatchScreen(viewModel: ClockViewModel) {
    val totalTimeMs by viewModel.stopwatchTime.collectAsState()
    val isRunning by viewModel.isStopwatchRunning.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()

    // Formatting MM:SS:CC
    val minutes = (totalTimeMs / 60000) % 60
    val seconds = (totalTimeMs / 1000) % 60
    val centiseconds = (totalTimeMs / 10) % 100
    val timeString = String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Arabic Header
        Text(
            text = "ساعة الإيقاف",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Right
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sweeping mechanical stopwatch dial
        StopwatchDial(totalTimeMs = totalTimeMs)

        Spacer(modifier = Modifier.height(24.dp))

        // Large Digital Display with a customized premium font styling
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.testTag("stopwatch_digital_timer")
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            Button(
                onClick = { viewModel.resetStopwatch() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .shadow(4.dp, CircleShape)
                    .testTag("stopwatch_reset_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "إعادة ضبط")
            }

            // Start/Pause Button (Neon active ripple)
            Button(
                onClick = {
                    if (isRunning) viewModel.pauseStopwatch() else viewModel.startStopwatch()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(80.dp)
                    .shadow(12.dp, CircleShape)
                    .testTag("stopwatch_play_pause_btn")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "إيقاف مؤقت" else "تشغيل",
                    modifier = Modifier.size(36.dp)
                )
            }

            // Lap / Split Button
            Button(
                onClick = { viewModel.recordLap() },
                enabled = isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .shadow(4.dp, CircleShape)
                    .testTag("stopwatch_lap_btn")
            ) {
                Icon(Icons.Default.Flag, contentDescription = "تسجيل دورة")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))

        // Registered Laps
        if (laps.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "الدورات والـ Splits المسجلة ستظهر هنا.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(laps) { index, lapTime ->
                    val lapMinutes = (lapTime / 60000) % 60
                    val lapSeconds = (lapTime / 1000) % 60
                    val lapCs = (lapTime / 10) % 100
                    val lapTimeFormatted = String.format("%02d:%02d.%02d", lapMinutes, lapSeconds, lapCs)
                    
                    val lapNumber = laps.size - index

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("lap_item_$lapNumber")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Section: Time
                            Text(
                                text = lapTimeFormatted,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )

                            // Right Section: Lap Index
                            Text(
                                text = "الدورة $lapNumber",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StopwatchDial(totalTimeMs: Long) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val neutralColor = MaterialTheme.colorScheme.onBackground
    val activeDialColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .size(180.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        activeDialColor,
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val radius = width / 2f
            val center = Offset(width / 2f, height / 2f)

            // Draw circular ticking grid lines
            for (i in 0 until 60) {
                val angle = i * 6 * Math.PI / 180f
                val length = if (i % 5 == 0) 10.dp.toPx() else 5.dp.toPx()
                val thickness = if (i % 5 == 0) 3.dp.toPx() else 1.dp.toPx()
                val tickColor = if (i % 5 == 0) primaryColor else neutralColor.copy(alpha = 0.3f)

                val startX = center.x + (radius - length) * cos(angle).toFloat()
                val startY = center.y + (radius - length) * sin(angle).toFloat()
                val endX = center.x + radius * cos(angle).toFloat()
                val endY = center.y + radius * sin(angle).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round
                )
            }

            // Sweeping needle representing stopwatch progress
            // 60 seconds make a full 360 sweep
            val secondsElapsed = (totalTimeMs % 60000) / 1000f
            val sweepAngle = (secondsElapsed * 6 - 90) * Math.PI / 180f
            val needleLength = radius * 0.85f

            drawLine(
                color = primaryColor,
                start = center,
                end = Offset(
                    center.x + needleLength * cos(sweepAngle).toFloat(),
                    center.y + sweepAngle.sinAngle().toFloat()
                ),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center Pin Glow
            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = center
            )
        }
    }
}

// Ext helper inline to calculate safe math values
private fun Double.sinAngle(): Double = sin(this)
