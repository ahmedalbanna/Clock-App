package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.ClockDatabase
import com.example.data.ClockRepository
import com.example.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot detected. Rescheduling alarms...")
            val goAsync = goAsync()
            
            val db = ClockDatabase.getDatabase(context.applicationContext)
            val repository = ClockRepository(db.clockDao())

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = repository.allAlarms.first()
                    for (alarm in alarms) {
                        if (alarm.isEnabled) {
                            AlarmScheduler.scheduleAlarm(context, alarm)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error while rescheduling alarms on boot", e)
                } finally {
                    goAsync.finish()
                }
            }
        }
    }
}
