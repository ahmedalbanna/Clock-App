package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Alarm
import com.example.data.ClockDatabase
import com.example.data.ClockRepository
import com.example.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: ClockRepository

    companion object {
        private const val TAG = "AlarmService"
        const val CHANNEL_ID = "alarms_ringing_channel"
        const val NOTIFICATION_ID = 9999

        const val ACTION_START_ALARM = "START_ALARM"
        const val ACTION_DISMISS = "DISMISS_ALARM"
        const val ACTION_SNOOZE = "SNOOZE_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        val db = ClockDatabase.getDatabase(applicationContext)
        repository = ClockRepository(db.clockDao())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        Log.d(TAG, "onStartCommand: action=$action, alarmId=$alarmId")

        when (action) {
            ACTION_START_ALARM -> {
                if (alarmId != -1) {
                    startRinging(alarmId)
                } else {
                    stopSelf()
                }
            }
            ACTION_DISMISS -> {
                dismissAlarm(alarmId)
            }
            ACTION_SNOOZE -> {
                snoozeAlarm(alarmId)
            }
        }

        return START_STICKY
    }

    private fun startRinging(alarmId: Int) {
        scope.launch {
            val alarm = repository.getAlarmById(alarmId) ?: return@launch
            
            // Create ringing notification first to satisfy Foreground Service requirement
            createNotificationChannel()
            val notification = buildRingingNotification(alarm)
            
            // Start Foreground
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Play Alarm Sound
            try {
                if (mediaPlayer == null) {
                    var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    if (alarmUri == null) {
                        alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    }
                    if (alarmUri == null) {
                        alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                    
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(this@AlarmService, alarmUri!!)
                        isLooping = true
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start media player", e)
            }

            // Vibrate
            if (alarm.isVibrate) {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                
                val pattern = longArrayOf(0, 500, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }

            // If it's a one-off (non-repeating), toggle isEnabled to false in Room database
            if (!alarm.isRepeating) {
                val updated = alarm.copy(isEnabled = false)
                repository.updateAlarm(updated)
            } else {
                // Repeating alarm: reschedule for next occurrence
                AlarmScheduler.scheduleAlarm(this@AlarmService, alarm)
            }
        }
    }

    private fun dismissAlarm(alarmId: Int) {
        stopRingingAndSelf()
    }

    private fun snoozeAlarm(alarmId: Int) {
        if (alarmId != -1) {
            scope.launch {
                val alarm = repository.getAlarmById(alarmId) ?: return@launch
                
                // Re-schedule this specific alarm 5 minutes later
                val cal = Calendar.getInstance()
                cal.add(Calendar.MINUTE, 5)
                
                val snoozeAlarm = alarm.copy(
                    id = alarmId + 100000, // Unique temporary snooze ID
                    hour = cal.get(Calendar.HOUR_OF_DAY),
                    minute = cal.get(Calendar.MINUTE),
                    label = "غفوة: ${alarm.label}",
                    repeatDays = "", // No repeat for snooze temporary alarms
                    isEnabled = true
                )
                
                AlarmScheduler.scheduleAlarm(this@AlarmService, snoozeAlarm)
                Log.d(TAG, "Snoozed alarm $alarmId to temporary snooze alarm ${snoozeAlarm.id} at ${snoozeAlarm.hour}:${snoozeAlarm.minute}")
            }
        }
        stopRingingAndSelf()
    }

    private fun stopRingingAndSelf() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration", e)
        }

        stopForeground(true)
        stopSelf()
    }

    private fun buildRingingNotification(alarm: Alarm): Notification {
        val fullscreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_RINGING_DIALOG", true)
            putExtra("RINGING_ALARM_ID", alarm.id)
        }
        
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            alarm.id,
            fullscreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
            putExtra("ALARM_ID", alarm.id)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            alarm.id,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("ALARM_ID", alarm.id)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            alarm.id,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeString = String.format("%02d:%02d", alarm.hour, alarm.minute)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("المنبه يرن! $timeString")
            .setContentText(alarm.label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_notification_clear_all, "إيقاف", dismissPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "غفوة (5 د)", snoozePendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات المنبه الفعالة"
            val descriptionText = "يُعرض هذا الإشعار عند رنين المنبه لتتمكن من إيقافه أو تأجيله."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setSound(null, null) // handled manually through foreground service audio play
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopRingingAndSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
