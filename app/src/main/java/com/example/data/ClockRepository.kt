package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first

class ClockRepository(private val clockDao: ClockDao) {

    val allAlarms: Flow<List<Alarm>> = clockDao.getAllAlarms()
    val allCities: Flow<List<WorldCity>> = clockDao.getAllCities()

    suspend fun getAlarmById(id: Int): Alarm? = clockDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: Alarm): Long = clockDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = clockDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = clockDao.deleteAlarm(alarm)

    suspend fun insertCity(city: WorldCity): Long = clockDao.insertCity(city)

    suspend fun deleteCity(city: WorldCity) = clockDao.deleteCity(city)

    // Seed default cities if empty
    suspend fun seedDefaultCitiesIfEmpty() {
        val currentCities = clockDao.getAllCities().first()
        if (currentCities.isEmpty()) {
            val defaults = listOf(
                WorldCity(nameAr = "مكة المكرمة", nameEn = "Mecca", timezoneId = "Asia/Riyadh", isAddedByUser = false),
                WorldCity(nameAr = "القدس الشريف", nameEn = "Jerusalem", timezoneId = "Asia/Jerusalem", isAddedByUser = false),
                WorldCity(nameAr = "القاهرة", nameEn = "Cairo", timezoneId = "Africa/Cairo", isAddedByUser = false),
                WorldCity(nameAr = "دبي", nameEn = "Dubai", timezoneId = "Asia/Dubai", isAddedByUser = false),
                WorldCity(nameAr = "بغداد", nameEn = "Baghdad", timezoneId = "Asia/Baghdad", isAddedByUser = false),
                WorldCity(nameAr = "تونس", nameEn = "Tunis", timezoneId = "Africa/Tunis", isAddedByUser = false),
                WorldCity(nameAr = "الرباط", nameEn = "Rabat", timezoneId = "Africa/Casablanca", isAddedByUser = false),
                WorldCity(nameAr = "إسطنبول", nameEn = "Istanbul", timezoneId = "Europe/Istanbul", isAddedByUser = false),
                WorldCity(nameAr = "لندن", nameEn = "London", timezoneId = "Europe/London", isAddedByUser = false),
                WorldCity(nameAr = "باريس", nameEn = "Paris", timezoneId = "Europe/Paris", isAddedByUser = false),
                WorldCity(nameAr = "نيويورك", nameEn = "New York", timezoneId = "America/New_York", isAddedByUser = false),
                WorldCity(nameAr = "طوكيو", nameEn = "Tokyo", timezoneId = "Asia/Tokyo", isAddedByUser = false)
            )
            clockDao.insertCities(defaults)
        }
    }
}
