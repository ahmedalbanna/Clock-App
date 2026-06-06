package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.WorldCity
import com.example.ui.ClockViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(viewModel: ClockViewModel) {
    val cities by viewModel.allCities.collectAsState()
    
    var showAddCityDialog by remember { mutableStateOf(false) }
    var tickingTime by remember { mutableStateOf(Calendar.getInstance()) }

    // Ticking coroutine for smooth analog hands and exact digital display
    LaunchedEffect(Unit) {
        while (true) {
            tickingTime = Calendar.getInstance()
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant Arabized title
            Text(
                text = "الساعة العالمية",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Right
            )

            // Analog Clock Design
            Spacer(modifier = Modifier.height(16.dp))
            AnalogClock(time = tickingTime)
            Spacer(modifier = Modifier.height(24.dp))

            // Current Digital Time Local
            val dfTime = SimpleDateFormat("hh:mm:ss a", Locale("ar"))
            val dfDate = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
            
            Text(
                text = dfTime.format(tickingTime.time),
                style = MaterialTheme.typography.displayMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Text(
                text = dfDate.format(tickingTime.time),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Active user cities list
            val userCities = cities.filter { it.isAddedByUser }
            if (userCities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم تقم بإضافة مدن مفضلة بعد.\nاضغط على الزر لإضافة مدينة.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(userCities) { city ->
                        WorldCityItem(city = city, onDelete = { viewModel.deleteCity(city) })
                    }
                }
            }
        }

        // Add City floating action button
        FloatingActionButton(
            onClick = { showAddCityDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_city_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة مدينة")
        }
    }

    if (showAddCityDialog) {
        AddCityDialog(
            onDismiss = { showAddCityDialog = false },
            onAddCity = { nameAr, nameEn, tzId ->
                viewModel.addCity(nameAr, nameEn, tzId)
                showAddCityDialog = false
            }
        )
    }
}

@Composable
fun AnalogClock(time: Calendar) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val neutralColor = MaterialTheme.colorScheme.onBackground
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .size(200.dp)
            .shadow(16.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
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

            // Draw clock background dial ticks
            for (i in 0 until 12) {
                val angle = i * 30 * Math.PI / 180f
                val startX = center.x + (radius - 12.dp.toPx()) * cos(angle).toFloat()
                val startY = center.y + (radius - 12.dp.toPx()) * sin(angle).toFloat()
                val endX = center.x + (radius - 4.dp.toPx()) * cos(angle).toFloat()
                val endY = center.y + (radius - 4.dp.toPx()) * sin(angle).toFloat()
                
                // Draw bold ticks at 12, 3, 6, 9
                val strokeW = if (i % 3 == 0) 4.dp.toPx() else 2.dp.toPx()
                val tickColor = if (i % 3 == 0) primaryColor else neutralColor.copy(alpha = 0.5f)
                
                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }

            // Get hours, minutes, seconds
            val seconds = time.get(Calendar.SECOND)
            val minutes = time.get(Calendar.MINUTE)
            val hours = time.get(Calendar.HOUR_OF_DAY)

            // 1. Hour hand (Shortest, thickest)
            val hourAngle = ((hours % 12) * 30 + minutes * 0.5 - 90) * Math.PI / 180f
            val hourHandLength = radius * 0.5f
            drawLine(
                color = neutralColor,
                start = center,
                end = Offset(
                    center.x + hourHandLength * cos(hourAngle).toFloat(),
                    center.y + hourHandLength * sin(hourAngle).toFloat()
                ),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 2. Minute hand (Medium length, medium thickness)
            val minuteAngle = (minutes * 6 - 90) * Math.PI / 180f
            val minuteHandLength = radius * 0.7f
            drawLine(
                color = neutralColor.copy(alpha = 0.8f),
                start = center,
                end = Offset(
                    center.x + minuteHandLength * cos(minuteAngle).toFloat(),
                    center.y + minuteHandLength * sin(minuteAngle).toFloat()
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 3. Second hand (Longest, thinnest, colored primary)
            val secondAngle = (seconds * 6 - 90) * Math.PI / 180f
            val secondHandLength = radius * 0.85f
            drawLine(
                color = primaryColor,
                start = center,
                end = Offset(
                    center.x + secondHandLength * cos(secondAngle).toFloat(),
                    center.y + secondHandLength * sin(secondAngle).toFloat()
                ),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center pin
            drawCircle(
                color = primaryColor,
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = neutralColor,
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
fun WorldCityItem(city: WorldCity, onDelete: () -> Unit) {
    var cityTime by remember { mutableStateOf("") }
    var cityDateDiff by remember { mutableStateOf("") }

    // Fast ticking
    LaunchedEffect(Unit) {
        while (true) {
            val tz = TimeZone.getTimeZone(city.timezoneId)
            val loc = Locale("ar")
            val cal = Calendar.getInstance(tz)
            
            val sdf = SimpleDateFormat("hh:mm a", loc)
            sdf.timeZone = tz
            cityTime = sdf.format(cal.time)

            // Calculate timezone offset difference relative to systemTimeZone
            val systemTz = TimeZone.getDefault()
            val offsetDiffMs = tz.getOffset(System.currentTimeMillis()) - systemTz.getOffset(System.currentTimeMillis())
            val offsetDiffHours = offsetDiffMs / (1000 * 60 * 60)
            
            cityDateDiff = when {
                offsetDiffHours == 0 -> "نفس توقيتك"
                offsetDiffHours > 0 -> "متقدم بـ $offsetDiffHours ساعة"
                else -> "متأخر بـ ${Math.abs(offsetDiffHours)} ساعة"
            }
            delay(1000)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("city_card_${city.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
            // Right Section (City Names & offset) RTL layout
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف المدينة",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = city.nameAr,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = cityDateDiff,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Left Section (Time)
            Text(
                text = cityTime,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityDialog(onDismiss: () -> Unit, onAddCity: (String, String, String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Complete world Arabic catalogs
    val databaseOfCities = listOf(
        Triple("مكة المكرمة", "Mecca", "Asia/Riyadh"),
        Triple("الرياض", "Riyadh", "Asia/Riyadh"),
        Triple("القاهرة", "Cairo", "Africa/Cairo"),
        Triple("القدس الشريف", "Jerusalem", "Asia/Jerusalem"),
        Triple("دبي", "Dubai", "Asia/Dubai"),
        Triple("بيروت", "Beirut", "Asia/Beirut"),
        Triple("بغداد", "Baghdad", "Asia/Baghdad"),
        Triple("دمشق", "Damascus", "Asia/Damascus"),
        Triple("عمان", "Amman", "Asia/Amman"),
        Triple("جدة", "Jeddah", "Asia/Riyadh"),
        Triple("مسقط", "Muscat", "Asia/Muscat"),
        Triple("الكويت", "Kuwait", "Asia/Kuwait"),
        Triple("المنامة", "Manama", "Asia/Bahrain"),
        Triple("صنعاء", "Sanaa", "Asia/Aden"),
        Triple("تونس", "Tunis", "Africa/Tunis"),
        Triple("الجزائر", "Algiers", "Africa/Algiers"),
        Triple("الرباط", "Rabat", "Africa/Casablanca"),
        Triple("طرابلس", "Tripoli", "Africa/Tripoli"),
        Triple("الخرطوم", "Khartoum", "Africa/Khartoum"),
        Triple("غزة", "Gaza", "Asia/Gaza"),
        Triple("لندن", "London", "Europe/London"),
        Triple("باريس", "Paris", "Europe/Paris"),
        Triple("نيويورك", "New York", "America/New_York"),
        Triple("طوكيو", "Tokyo", "Asia/Tokyo"),
        Triple("سيدني", "Sydney", "Australia/Sydney"),
        Triple("موسكو", "Moscow", "Europe/Moscow"),
        Triple("روما", "Rome", "Europe/Rome"),
        Triple("برلين", "Berlin", "Europe/Berlin"),
        Triple("بكين", "Beijing", "Asia/Shanghai"),
        Triple("جاكرتا", "Jakarta", "Asia/Jakarta"),
        Triple("سنغافورة", "Singapore", "Asia/Singapore"),
        Triple("لوس أنجلوس", "Los Angeles", "America/Los_Angeles")
    )

    val filteredList = databaseOfCities.filter {
        it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إضافة مدينة عالمية",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث باسم المدينة باللغة العربية أو الإنجليزية...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    onAddCity(city.first, city.second, city.third)
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = city.first,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                )
                                Text(
                                    text = city.second,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Text(
                                text = "+ إضافة",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إلغاء")
                }
            }
        }
    }
}
