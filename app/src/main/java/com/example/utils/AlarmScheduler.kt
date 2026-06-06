package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Alarm
import com.example.receiver.AlarmReceiver
import java.util.*

object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            action = "com.example.ACTION_TRIGGER_ALARM"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateTriggerTime(alarm)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm ${alarm.id} for ${Date(triggerTime)}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Cannot schedule exact alarm", e)
            // Fallback to inexact
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_ALARM"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm ${alarm.id}")
    }

    private fun calculateTriggerTime(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.isRepeating) {
            val repeatDays = alarm.getRepeatDaysSet() // standard Calendar days: 1=Sunday, 2=Monday, ..., 7=Saturday
            val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            
            var daysUntilAlarm = -1
            // Check day by day starting from today
            for (i in 0..6) {
                val checkDay = (currentDayOfWeek + i - 1) % 7 + 1
                if (repeatDays.contains(checkDay)) {
                    if (i == 0) {
                        // Today, check if time already passed
                        if (target.after(now)) {
                            daysUntilAlarm = 0
                            break
                        }
                    } else {
                        daysUntilAlarm = i
                        break
                    }
                }
            }
            
            if (daysUntilAlarm == -1) {
                // Should not happen if non-empty, default fallback
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
            } else {
                target.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
            }
        } else {
            // One-off alarm
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return target.timeInMillis
    }
}
