package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.ClockDatabase
import com.example.data.ClockRepository
import com.example.data.WorldCity
import com.example.utils.AlarmScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

enum class ClockTab {
    CLOCK, ALARM, STOPWATCH, TIMER
}

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClockRepository
    val allAlarms: StateFlow<List<Alarm>>
    val allCities: StateFlow<List<WorldCity>>

    // Active screen navigation
    private val _currentTab = MutableStateFlow(ClockTab.CLOCK)
    val currentTab: StateFlow<ClockTab> = _currentTab

    init {
        val clockDao = ClockDatabase.getDatabase(application).clockDao()
        repository = ClockRepository(clockDao)

        allAlarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCities = repository.allCities.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default world cities on launch
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDefaultCitiesIfEmpty()
        }
    }

    fun selectTab(tab: ClockTab) {
        _currentTab.value = tab
    }

    // ==========================================
    // ALARMS HANDLERS
    // ==========================================
    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            
            // Register or Cancel in AlarmManager
            withContext(Dispatchers.Main) {
                if (updated.isEnabled) {
                    AlarmScheduler.scheduleAlarm(getApplication(), updated)
                } else {
                    AlarmScheduler.cancelAlarm(getApplication(), updated)
                }
            }
        }
    }

    fun saveAlarm(hour: Int, minute: Int, label: String, repeatDays: Set<Int>, isVibrate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val repeatStr = repeatDays.sorted().joinToString(",")
            val alarm = Alarm(
                hour = hour,
                minute = minute,
                label = label.ifEmpty { "منبه" },
                repeatDays = repeatStr,
                isVibrate = isVibrate,
                isEnabled = true
            )
            val insertedId = repository.insertAlarm(alarm)
            val savedAlarm = alarm.copy(id = insertedId.toInt())
            
            withContext(Dispatchers.Main) {
                AlarmScheduler.scheduleAlarm(getApplication(), savedAlarm)
            }
        }
    }

    fun updateAlarmFull(id: Int, hour: Int, minute: Int, label: String, repeatDays: Set<Int>, isVibrate: Boolean, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val repeatStr = repeatDays.sorted().joinToString(",")
            val alarm = Alarm(
                id = id,
                hour = hour,
                minute = minute,
                label = label.ifEmpty { "منبه" },
                repeatDays = repeatStr,
                isVibrate = isVibrate,
                isEnabled = isEnabled
            )
            repository.updateAlarm(alarm)
            
            withContext(Dispatchers.Main) {
                if (alarm.isEnabled) {
                    AlarmScheduler.scheduleAlarm(getApplication(), alarm)
                } else {
                    AlarmScheduler.cancelAlarm(getApplication(), alarm)
                }
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlarm(alarm)
            withContext(Dispatchers.Main) {
                AlarmScheduler.cancelAlarm(getApplication(), alarm)
            }
        }
    }


    // ==========================================
    // WORLD CITIES HANDLERS
    // ==========================================
    fun addCity(nameAr: String, nameEn: String, timezoneId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val city = WorldCity(
                nameAr = nameAr,
                nameEn = nameEn,
                timezoneId = timezoneId,
                isAddedByUser = true
            )
            repository.insertCity(city)
        }
    }

    fun deleteCity(city: WorldCity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCity(city)
        }
    }


    // ==========================================
    // STOPWATCH LOGIC
    // ==========================================
    private val _stopwatchTime = MutableStateFlow(0L)
    val stopwatchTime: StateFlow<Long> = _stopwatchTime

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning

    private val _stopwatchLaps = MutableStateFlow<List<Long>>(emptyList())
    val stopwatchLaps: StateFlow<List<Long>> = _stopwatchLaps

    private var stopwatchJob: Job? = null
    private var startRealTime = 0L
    private var accumulatedTime = 0L

    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        startRealTime = SystemClock.elapsedRealtime()
        
        stopwatchJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startRealTime
                _stopwatchTime.value = accumulatedTime + elapsed
                delay(10)
            }
        }
    }

    fun pauseStopwatch() {
        if (!_isStopwatchRunning.value) return
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        accumulatedTime += SystemClock.elapsedRealtime() - startRealTime
        _stopwatchTime.value = accumulatedTime
    }

    fun recordLap() {
        val currentTotal = _stopwatchTime.value
        val list = _stopwatchLaps.value.toMutableList()
        list.add(0, currentTotal) // Insert at top
        _stopwatchLaps.value = list
    }

    fun resetStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        accumulatedTime = 0L
        startRealTime = 0L
        _stopwatchTime.value = 0L
        _stopwatchLaps.value = emptyList()
    }


    // ==========================================
    // countdown TIMER LOGIC
    // ==========================================
    private val _timerRemaining = MutableStateFlow(0L) // in milliseconds
    val timerRemaining: StateFlow<Long> = _timerRemaining

    private val _timerTotal = MutableStateFlow(0L) // total in milliseconds
    val timerTotal: StateFlow<Long> = _timerTotal

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning

    private val _showTimerCompletion = MutableStateFlow(false)
    val showTimerCompletion: StateFlow<Boolean> = _showTimerCompletion

    private var timerJob: Job? = null
    private var timerEndRealTime = 0L

    fun startTimer(hours: Int, minutes: Int, seconds: Int) {
        if (_isTimerRunning.value) return
        
        val totalMs = ((hours * 3600) + (minutes * 60) + seconds) * 1000L
        if (totalMs <= 0L) return

        _timerTotal.value = totalMs
        _timerRemaining.value = totalMs
        _isTimerRunning.value = true
        _showTimerCompletion.value = false

        startTimerLoop()
    }

    fun resumeTimer() {
        if (_isTimerRunning.value || _timerRemaining.value <= 0L) return
        _isTimerRunning.value = true
        startTimerLoop()
    }

    private fun startTimerLoop() {
        timerEndRealTime = SystemClock.elapsedRealtime() + _timerRemaining.value
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val remaining = timerEndRealTime - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    _timerRemaining.value = 0L
                    _isTimerRunning.value = false
                    _showTimerCompletion.value = true
                    triggerTimerCompletionAlert()
                    cancel()
                } else {
                    _timerRemaining.value = remaining
                    delay(50)
                }
            }
        }
    }

    fun pauseTimer() {
        if (!_isTimerRunning.value) return
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerRemaining.value = maxOf(0L, timerEndRealTime - SystemClock.elapsedRealtime())
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerRemaining.value = 0L
        _timerTotal.value = 0L
        _showTimerCompletion.value = false
    }

    fun dismissTimerAlert() {
        _showTimerCompletion.value = false
        stopTimerSound()
    }

    private var timerMediaPlayer: MediaPlayer? = null
    private var timerVibrator: Vibrator? = null

    private fun triggerTimerCompletionAlert() {
        // Play notification/alarm sound in a simple loop or once
        scopePlayTimerSound()
        showTimerNotification()
    }

    private fun scopePlayTimerSound() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (timerMediaPlayer == null) {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    timerMediaPlayer = MediaPlayer().apply {
                        setDataSource(getApplication(), alertUri!!)
                        isLooping = false
                        prepare()
                        start()
                    }
                }
                
                // Vibrate
                timerVibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    timerVibrator?.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    timerVibrator?.vibrate(1000)
                }
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    private fun stopTimerSound() {
        try {
            timerMediaPlayer?.stop()
            timerMediaPlayer?.release()
            timerMediaPlayer = null
            timerVibrator?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun showTimerNotification() {
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "timer_completed_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات مؤقت الساعة"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(getApplication(), channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("انتهى المؤقت!")
            .setContentText("لقد دوت الساعة لانتهاء فترة المؤقت.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(8888, notification)
    }

    override fun onCleared() {
        stopwatchJob?.cancel()
        timerJob?.cancel()
        stopTimerSound()
        super.onCleared()
    }
}
