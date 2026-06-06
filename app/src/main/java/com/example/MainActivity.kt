package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.AlarmService
import com.example.ui.ClockTab
import com.example.ui.ClockViewModel
import com.example.ui.screens.AlarmScreen
import com.example.ui.screens.ClockScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.ClockTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Verify if opened from an active alarm notification
        var showRingingDialogInitially = intent?.getBooleanExtra("OPEN_RINGING_DIALOG", false) ?: false
        var ringingAlarmId = intent?.getIntExtra("RINGING_ALARM_ID", -1) ?: -1

        setContent {
            ClockTheme {
                var showRingingDialog by remember { mutableStateOf(showRingingDialogInitially) }
                val currentTab by viewModel.currentTab.collectAsState()

                // Register runtime permission check for Android 13+ notifications
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                        } catch (e: Exception) {
                            // Suppress
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        ClockBottomNavigation(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Switch active tabs
                        when (currentTab) {
                            ClockTab.CLOCK -> ClockScreen(viewModel = viewModel)
                            ClockTab.ALARM -> AlarmScreen(viewModel = viewModel)
                            ClockTab.STOPWATCH -> StopwatchScreen(viewModel = viewModel)
                            ClockTab.TIMER -> TimerScreen(viewModel = viewModel)
                        }

                        // Full-screen-like ringing dialog overlay
                        if (showRingingDialog && ringingAlarmId != -1) {
                            ActiveRingingDialog(
                                alarmId = ringingAlarmId,
                                onDismiss = {
                                    showRingingDialog = false
                                    ringingAlarmId = -1
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val showRinging = intent.getBooleanExtra("OPEN_RINGING_DIALOG", false)
        val alarmId = intent.getIntExtra("RINGING_ALARM_ID", -1)
        if (showRinging && alarmId != -1) {
            setContent {
                ClockTheme {
                    var showRingingDialog by remember { mutableStateOf(true) }
                    val currentTab by viewModel.currentTab.collectAsState()

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            ClockBottomNavigation(
                                currentTab = currentTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            when (currentTab) {
                                ClockTab.CLOCK -> ClockScreen(viewModel = viewModel)
                                ClockTab.ALARM -> AlarmScreen(viewModel = viewModel)
                                ClockTab.STOPWATCH -> StopwatchScreen(viewModel = viewModel)
                                ClockTab.TIMER -> TimerScreen(viewModel = viewModel)
                            }

                            if (showRingingDialog) {
                                ActiveRingingDialog(
                                    alarmId = alarmId,
                                    onDismiss = {
                                        showRingingDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClockBottomNavigation(
    currentTab: ClockTab,
    onTabSelected: (ClockTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .shadow(16.dp)
            .testTag("clock_bottom_nav"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        // Tab CLOCK (الساعة)
        NavigationBarItem(
            selected = currentTab == ClockTab.CLOCK,
            onClick = { onTabSelected(ClockTab.CLOCK) },
            icon = { Icon(Icons.Default.Public, contentDescription = "الساعة العالمية") },
            label = { Text("الساعة") },
            modifier = Modifier.testTag("tab_clock")
        )

        // Tab ALARM (المنبه)
        NavigationBarItem(
            selected = currentTab == ClockTab.ALARM,
            onClick = { onTabSelected(ClockTab.ALARM) },
            icon = { Icon(Icons.Default.Notifications, contentDescription = "المنبه") },
            label = { Text("المنبه") },
            modifier = Modifier.testTag("tab_alarm")
        )

        // Tab STOPWATCH (ساعة الإيقاف)
        NavigationBarItem(
            selected = currentTab == ClockTab.STOPWATCH,
            onClick = { onTabSelected(ClockTab.STOPWATCH) },
            icon = { Icon(Icons.Default.Timer, contentDescription = "ساعة الإيقاف") },
            label = { Text("ساعة الإيقاف") },
            modifier = Modifier.testTag("tab_stopwatch")
        )

        // Tab TIMER (المؤقت)
        NavigationBarItem(
            selected = currentTab == ClockTab.TIMER,
            onClick = { onTabSelected(ClockTab.TIMER) },
            icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = "المؤقت التنازلي") },
            label = { Text("المؤقت") },
            modifier = Modifier.testTag("tab_timer")
        )
    }
}

@Composable
fun ActiveRingingDialog(alarmId: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    contentDescription = "جرس المنبه",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "تنبيه المنبه النشط! 🔔",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "يرن المنبه الآن. يرجى اختيار إحدى العمليات أدناه.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Snooze Button
                    Button(
                        onClick = {
                            val ringServiceIntent = Intent(context, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_SNOOZE
                                putExtra("ALARM_ID", alarmId)
                            }
                            context.startService(ringServiceIntent)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("غفوة 5 د")
                    }

                    // Dismiss Button
                    Button(
                        onClick = {
                            val ringServiceIntent = Intent(context, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_DISMISS
                                putExtra("ALARM_ID", alarmId)
                            }
                            context.startService(ringServiceIntent)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إيقاف الجرس")
                    }
                }
            }
        }
    }
}
