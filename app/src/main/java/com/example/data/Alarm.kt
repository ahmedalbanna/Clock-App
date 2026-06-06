package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "منبه",
    val isEnabled: Boolean = true,
    val repeatDays: String = "", // Comma-separated: e.g. "2,3,4,5,6" (Monday to Friday, using Calendar.MONDAY = 2, etc.)
    val isVibrate: Boolean = true,
    val isSnoozed: Boolean = false
) : Serializable {
    
    // Check if alarm repeats on any day
    val isRepeating: Boolean
        get() = repeatDays.isNotEmpty()

    // Decodes repeatDays into a set of integers
    fun getRepeatDaysSet(): Set<Int> {
        if (repeatDays.isEmpty()) return emptySet()
        return repeatDays.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }
}
