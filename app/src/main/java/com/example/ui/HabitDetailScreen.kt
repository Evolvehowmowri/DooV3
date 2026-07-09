package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Do
import com.example.data.DoLog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitDetailScreen(
    doItem: Do,
    logs: List<DoLog>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    viewModel: DoViewModel
) {
    val persianCalendarEnabled by viewModel.persianCalendarEnabled.collectAsStateWithLifecycle()
    val usePersianScript by viewModel.usePersianScript.collectAsStateWithLifecycle()

    val categoryInfo = CategoryRegistry.getCategoryInfo(doItem.category)
    val themeColor = categoryInfo.color

    // Keep state of monthly selection for charts (e.g. Month or Year view)
    var selectedScoreRange by remember { mutableStateOf("Month") }
    var selectedHistoryRange by remember { mutableStateOf("Month") }

    // Filter logs for this doItem
    val doLogs = remember(logs, doItem.id) { logs.filter { it.doId == doItem.id } }

    // Streak calculations
    val currentStreak = remember(doLogs) { StreakCalculator.calculateStreak(doLogs, doItem.id) }
    val longestStreak = remember(doLogs) { StreakCalculator.calculateLongestStreak(doLogs, doItem.id) }
    val completionsCount = remember(doLogs) { doLogs.count { it.value > 0f } }

    // Calculate Overview stats (Score, Month change, Year change, Total)
    val score30Days = remember(doLogs) {
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var completed = 0
        var totalDaysToConsider = 0
        for (i in 0 until 30) {
            val dateStr = dateFormat.format(cal.time)
            val log = doLogs.find { it.date == dateStr }
            val isSkipped = log != null && log.value == -1f
            if (!isSkipped) {
                totalDaysToConsider++
                if (log != null) {
                    if (doItem.isMeasurable) {
                        if (log.value >= doItem.targetValue) completed++
                    } else {
                        if (log.value > 0f) completed++
                    }
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        if (totalDaysToConsider > 0) {
            ((completed / totalDaysToConsider.toFloat()) * 100).toInt()
        } else {
            0
        }
    }

    // Month-over-month completion change
    val monthChange = remember(doLogs) {
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var currentMonthCompletions = 0
        var currentMonthDays = 0
        var prevMonthCompletions = 0
        var prevMonthDays = 0

        // Current 30 days
        for (i in 0 until 30) {
            val dateStr = dateFormat.format(cal.time)
            val log = doLogs.find { it.date == dateStr }
            val isSkipped = log != null && log.value == -1f
            if (!isSkipped) {
                currentMonthDays++
                if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                    currentMonthCompletions++
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        // Prev 30 days
        for (i in 0 until 30) {
            val dateStr = dateFormat.format(cal.time)
            val log = doLogs.find { it.date == dateStr }
            val isSkipped = log != null && log.value == -1f
            if (!isSkipped) {
                prevMonthDays++
                if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                    prevMonthCompletions++
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val rate1 = if (currentMonthDays > 0) (currentMonthCompletions / currentMonthDays.toFloat()) * 100 else 0f
        val rate2 = if (prevMonthDays > 0) (prevMonthCompletions / prevMonthDays.toFloat()) * 100 else 0f
        val diff = (rate1 - rate2).toInt()
        if (diff >= 0) "+$diff%" else "$diff%"
    }

    // Year completion rate / change
    val yearRate = remember(doLogs) {
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var completed = 0
        for (i in 0 until 365) {
            val dateStr = dateFormat.format(cal.time)
            val log = doLogs.find { it.date == dateStr }
            if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                completed++
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val rate = ((completed / 365f) * 100).toInt()
        "+$rate%"
    }

    // Dynamic Progress Optimization Insights
    val progressInsight = remember(doLogs, doItem.id, doItem.isMeasurable, doItem.targetValue) {
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        var current7DaysCompletions = 0
        var prev7DaysCompletions = 0
        
        // Current 7 days
        for (i in 0 until 7) {
            val dateStr = dateFormat.format(cal.time)
            val log = doLogs.find { it.date == dateStr }
            if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                current7DaysCompletions++
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        // Prev 7 days
        for (i in 0 until 7) {
            val dateStr = dateFormat.format(cal.time)
            val log = doLogs.find { it.date == dateStr }
            if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                prev7DaysCompletions++
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        val trendType: String // "IMPROVING", "FALLING_BEHIND", "STABLE", "INACTIVE"
        val percentageDiff = current7DaysCompletions - prev7DaysCompletions
        
        if (current7DaysCompletions == 0 && prev7DaysCompletions == 0) {
            trendType = "INACTIVE"
        } else if (percentageDiff > 0) {
            trendType = "IMPROVING"
        } else if (percentageDiff < 0) {
            trendType = "FALLING_BEHIND"
        } else {
            trendType = "STABLE"
        }
        
        val title: String
        val description: String
        val recommendation: String
        val tintColor: Color
        
        when (trendType) {
            "IMPROVING" -> {
                title = "Excellent Progress!"
                description = "You've completed this habit $current7DaysCompletions times this week, compared to $prev7DaysCompletions last week. Your momentum is rising!"
                recommendation = "Optimize by riding this wave! Keep the streak alive by checking in at the same time daily."
                tintColor = Color(0xFF4CAF50) // Green
            }
            "FALLING_BEHIND" -> {
                title = "Falling Behind Slightly"
                description = "You completed this habit $current7DaysCompletions times this week, down from $prev7DaysCompletions last week."
                recommendation = "Don't let two days slip in a row. Try lowering the target or adjusting your reminder to a more mindful time."
                tintColor = Color(0xFFFF9800) // Orange/Amber
            }
            "INACTIVE" -> {
                title = "Time to Restart?"
                description = "You haven't logged any progress for this habit in the last 2 weeks."
                recommendation = "Starting is the hardest part. Just check it off once today to rebuild your psychological momentum."
                tintColor = Color(0xFFF44336) // Red
            }
            else -> { // STABLE
                title = "Steady Consistency"
                description = "You matched your pace with exactly $current7DaysCompletions completions both this week and last week."
                recommendation = "You've established a solid routine. To push your limits, try adding a small stretch goal or tracking a higher target value!"
                tintColor = Color(0xFF2196F3) // Blue
            }
        }
        
        InsightData(
            title = title,
            description = description,
            recommendation = recommendation,
            trendType = trendType,
            tintColor = tintColor,
            currentCount = current7DaysCompletions,
            prevCount = prev7DaysCompletions
        )
    }

    // Dialog layout
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Allows full screen
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header (Action bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = doItem.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 240.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Habit", tint = Color.White)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Habit", tint = HighPriorityRed)
                        }
                    }
                }

                // Sub-header Info (Frequency / Reminders / Scheduling)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Frequency", tint = themeColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (doItem.type == "RECURRING") "Recurring Habit" else "Single Target Task",
                            color = DarkTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminder", tint = themeColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = doItem.reminderTime?.let { "Reminds at $it" } ?: "Reminder Off",
                            color = DarkTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (doItem.type == "TASK" && doItem.targetDate.isNullOrBlank() && doItem.targetTime.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Scheduled", tint = themeColor, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Scheduled for: Someday",
                                color = DarkTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (doItem.targetDate != null || doItem.targetTime != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Scheduled", tint = themeColor, modifier = Modifier.size(16.dp))
                            val formattedDate = doItem.targetDate?.let { dateStr ->
                                if (persianCalendarEnabled) {
                                    val slashDate = PersianCalendarHelper.formatToPersianSlash(dateStr)
                                    if (usePersianScript) PersianCalendarHelper.toPersianDigits(slashDate) else slashDate
                                } else {
                                    dateStr
                                }
                            }
                            val dateTimeStr = buildString {
                                if (formattedDate != null) append(formattedDate)
                                if (doItem.targetTime != null) {
                                    if (isNotEmpty()) append(" ")
                                    append(doItem.targetTime)
                                }
                            }
                            Text(
                                text = "Scheduled for: $dateTimeStr",
                                color = DarkTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable details content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Tags Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories:",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        doItem.category.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { catName ->
                            val catInfo = CategoryRegistry.getCategoryInfo(catName)
                            Box(
                                modifier = Modifier
                                    .background(catInfo.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, catInfo.color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = catInfo.icon,
                                        contentDescription = null,
                                        tint = catInfo.color,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = catName,
                                        color = catInfo.color,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 1. Overview Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Overview",
                                color = themeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Circular Score Indicator
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(70.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { score30Days / 100f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = themeColor,
                                        trackColor = Color.White.copy(alpha = 0.08f),
                                        strokeWidth = 6.dp
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$score30Days%",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Score",
                                            color = DarkTextSecondary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                // Metrics Columns
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = monthChange,
                                        color = if (monthChange.startsWith("+")) themeColor else HighPriorityRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Month",
                                        color = DarkTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = yearRate,
                                        color = themeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Year",
                                        color = DarkTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = completionsCount.toString(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Total Checked",
                                        color = DarkTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Progress Optimization Insights Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = progressInsight.tintColor.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val icon = when (progressInsight.trendType) {
                                        "IMPROVING" -> Icons.Default.TrendingUp
                                        "FALLING_BEHIND" -> Icons.Default.TrendingDown
                                        "INACTIVE" -> Icons.Default.Warning
                                        else -> Icons.Default.TrendingFlat
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(progressInsight.tintColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = progressInsight.tintColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = progressInsight.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                
                                // Progress Badge
                                Box(
                                    modifier = Modifier
                                        .background(progressInsight.tintColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    val diff = progressInsight.currentCount - progressInsight.prevCount
                                    val badgeText = when {
                                        diff > 0 -> "+$diff this week"
                                        diff < 0 -> "$diff this week"
                                        progressInsight.trendType == "INACTIVE" -> "Inactive"
                                        else -> "Steady pace"
                                    }
                                    Text(
                                        text = badgeText,
                                        color = progressInsight.tintColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = progressInsight.description,
                                color = DarkTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            // Horizontal Divider via Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(GlassBorder)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Recommendation",
                                    tint = BrightAmber,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Column {
                                    Text(
                                        text = "Optimization Recommendation",
                                        color = BrightAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = progressInsight.recommendation,
                                        color = DarkTextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // 2. Score Trend Line Chart
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Score Trend",
                                    color = themeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                // Modern Range Selection Chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("Day", "Week", "Month", "Year").forEach { range ->
                                        val isSelected = selectedScoreRange == range
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) themeColor.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                                .border(1.dp, if (isSelected) themeColor else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { selectedScoreRange = range }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = range,
                                                color = if (isSelected) Color.White else DarkTextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Score Trend Custom Canvas Drawing
                            val trendPoints = remember(doLogs, selectedScoreRange, doItem.id, doItem.isMeasurable, doItem.targetValue) {
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val points = mutableListOf<Pair<String, Float>>()

                                when (selectedScoreRange) {
                                    "Day" -> {
                                        // Past 7 days (including today)
                                        val cal = Calendar.getInstance()
                                        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                                        for (i in 0 until 7) {
                                            val labelDate = cal.time
                                            val dStr = dateFormat.format(labelDate)
                                            val log = doLogs.find { it.date == dStr }
                                            val score = if (log != null) {
                                                if (doItem.isMeasurable) {
                                                    val fraction = if (doItem.targetValue > 0f) log.value / doItem.targetValue else 0f
                                                    minOf(1f, maxOf(0f, fraction)) * 100f
                                                } else {
                                                    if (log.value > 0f) 100f else 0f
                                                }
                                            } else {
                                                0f
                                            }
                                            points.add(0, Pair(dayFormat.format(labelDate), score))
                                            cal.add(Calendar.DAY_OF_YEAR, -1)
                                        }
                                    }
                                    "Week" -> {
                                        // Past 4 weeks
                                        val cal = Calendar.getInstance()
                                        for (w in 0 until 4) {
                                            var completedDays = 0f
                                            var totalDays = 0f
                                            val labelDate = cal.time
                                            for (d in 0 until 7) {
                                                val dStr = dateFormat.format(cal.time)
                                                val log = doLogs.find { it.date == dStr }
                                                val isSkipped = log != null && log.value == -1f
                                                if (!isSkipped) {
                                                    totalDays += 1f
                                                    if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                                                        completedDays += 1f
                                                    }
                                                }
                                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                            }
                                            val weekScore = if (totalDays > 0) (completedDays / totalDays) * 100f else 0f
                                            points.add(0, Pair("Wk ${4 - w}", weekScore))
                                        }
                                    }
                                    "Month" -> {
                                        // Past 6 calendar months
                                        val cal = Calendar.getInstance()
                                        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                                        for (m in 0 until 6) {
                                            val labelMonth = cal.time
                                            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                            var completedDays = 0f
                                            var totalDays = 0f
                                            for (d in 0 until maxDaysInMonth) {
                                                val dStr = dateFormat.format(cal.time)
                                                val log = doLogs.find { it.date == dStr }
                                                val isSkipped = log != null && log.value == -1f
                                                if (!isSkipped) {
                                                    totalDays += 1f
                                                    if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                                                        completedDays += 1f
                                                    }
                                                }
                                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                            }
                                            val monthScore = if (totalDays > 0) (completedDays / totalDays) * 100f else 0f
                                            points.add(0, Pair(monthFormat.format(labelMonth), monthScore))
                                        }
                                    }
                                    else -> { // "Year"
                                        // Past 12 calendar months
                                        val cal = Calendar.getInstance()
                                        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                                        for (m in 0 until 12) {
                                            val labelMonth = cal.time
                                            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                            var completedDays = 0f
                                            var totalDays = 0f
                                            for (d in 0 until maxDaysInMonth) {
                                                val dStr = dateFormat.format(cal.time)
                                                val log = doLogs.find { it.date == dStr }
                                                val isSkipped = log != null && log.value == -1f
                                                if (!isSkipped) {
                                                    totalDays += 1f
                                                    if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                                                        completedDays += 1f
                                                    }
                                                }
                                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                            }
                                            val monthScore = if (totalDays > 0) (completedDays / totalDays) * 100f else 0f
                                            points.add(0, Pair(monthFormat.format(labelMonth), monthScore))
                                        }
                                    }
                                }
                                points
                            }

                            ScoreTrendChart(points = trendPoints, themeColor = themeColor)
                        }
                    }

                    // 3. History Bar Chart Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Completions History",
                                    color = themeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                // Modern Range Selection Chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("Week", "Month", "Year").forEach { range ->
                                        val isSelected = selectedHistoryRange == range
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) themeColor.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                                .border(1.dp, if (isSelected) themeColor else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { selectedHistoryRange = range }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = range,
                                                color = if (isSelected) Color.White else DarkTextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val completionCounts = remember(doLogs, selectedHistoryRange, doItem.id, doItem.isMeasurable, doItem.targetValue) {
                                val cal = Calendar.getInstance()
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val points = mutableListOf<Pair<String, Int>>()

                                when (selectedHistoryRange) {
                                    "Week" -> {
                                        // Past 7 days (including today)
                                        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                                        for (d in 0 until 7) {
                                            val labelDate = cal.time
                                            val dStr = dateFormat.format(labelDate)
                                            val log = doLogs.find { it.date == dStr }
                                            val count = if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) 1 else 0
                                            points.add(0, Pair(dayFormat.format(labelDate), count))
                                            cal.add(Calendar.DAY_OF_YEAR, -1)
                                        }
                                    }
                                    "Month" -> {
                                        // Count per week for past 5 weeks
                                        val weekFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                                        for (w in 0 until 5) {
                                            var count = 0
                                            val labelDate = cal.time
                                            for (d in 0 until 7) {
                                                val dStr = dateFormat.format(cal.time)
                                                val log = doLogs.find { it.date == dStr }
                                                if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                                                    count++
                                                }
                                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                            }
                                            points.add(0, Pair(weekFormat.format(labelDate), count))
                                        }
                                    }
                                    else -> { // "Year"
                                        // Count per month for past 12 months
                                        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                                        for (m in 0 until 12) {
                                            val labelMonth = cal.time
                                            val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                                            var count = 0
                                            for (d in 0 until maxDaysInMonth) {
                                                val dStr = dateFormat.format(cal.time)
                                                val log = doLogs.find { it.date == dStr }
                                                if (log != null && (if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value > 0f)) {
                                                    count++
                                                }
                                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                            }
                                            points.add(0, Pair(monthFormat.format(labelMonth), count))
                                        }
                                    }
                                }
                                points
                            }

                            HistoryBarChart(points = completionCounts, themeColor = themeColor)
                        }
                    }

                    // 4. Interactive Calendar Heatmap Grid
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Calendar History",
                                color = themeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Tap any day cell to instantly toggle log status",
                                color = DarkTextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Load only the last 30 days ending today, aligned with Saturday as first day of week
                            val calendars = remember {
                                val list = mutableListOf<CalendarMonthData>()
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val startCal = Calendar.getInstance()
                                startCal.add(Calendar.DAY_OF_YEAR, -29) // 30 days total ending today
                                
                                val tempCal = Calendar.getInstance()
                                tempCal.time = startCal.time
                                val dow = tempCal.get(Calendar.DAY_OF_WEEK)
                                val paddingDays = (dow - Calendar.SATURDAY + 7) % 7
                                
                                val days = mutableListOf<CalendarDayData>()
                                // Add empty padding days to align grid to Saturday
                                for (p in 0 until paddingDays) {
                                    days.add(CalendarDayData(dateString = "", dayNumber = 0, isValid = false))
                                }
                                
                                // Add the last 30 days
                                for (i in 0..29) {
                                    val dayNum = startCal.get(Calendar.DAY_OF_MONTH)
                                    days.add(
                                        CalendarDayData(
                                            dateString = sdf.format(startCal.time),
                                            dayNumber = dayNum,
                                            isValid = true
                                        )
                                    )
                                    startCal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                                
                                list.add(CalendarMonthData(monthLabel = "Last 30 Days Tracker", days = days))
                                list
                            }

                            // Render calendars vertically
                            calendars.forEach { month ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = month.monthLabel,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    // Weekday Headers (Saturday is forced as the first day of the week)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf("Sa", "Su", "Mo", "Tu", "We", "Th", "Fr").forEach { h ->
                                            Text(
                                                text = h,
                                                color = LavenderAccent.copy(alpha = 0.8f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Days Grid (Chunked in 7s)
                                    month.days.chunked(7).forEach { week ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            week.forEach { day ->
                                                if (day.isValid) {
                                                    val log = doLogs.find { it.date == day.dateString }
                                                    val isLogged = log != null && log.value > 0f
                                                    val loggedValue = log?.value ?: 0f
                                                    val isSkipped = log != null && log.value == -1f

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .padding(2.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                if (isLogged) {
                                                                    if (doItem.isMeasurable && loggedValue < doItem.targetValue) {
                                                                        themeColor.copy(alpha = 0.35f)
                                                                    } else {
                                                                        themeColor
                                                                    }
                                                                } else if (isSkipped) {
                                                                    Color.White.copy(alpha = 0.08f)
                                                                } else {
                                                                    DarkSurfaceVariant
                                                                }
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isLogged) Color.Transparent else if (isSkipped) Color.White.copy(alpha = 0.3f) else GlassBorder,
                                                                shape = RoundedCornerShape(6.dp)
                                                            )
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (doItem.isMeasurable) {
                                                                        val newValue = if (isLogged) 0f else doItem.targetValue
                                                                        viewModel.logDoValue(doItem.id, day.dateString, newValue)
                                                                    } else {
                                                                        val newValue = if (isLogged) 0f else 1f
                                                                        viewModel.logDoValue(doItem.id, day.dateString, newValue)
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    val newValue = if (isSkipped) 0f else -1f
                                                                    viewModel.logDoValue(doItem.id, day.dateString, newValue)
                                                                }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSkipped) {
                                                            Text(
                                                                text = "-",
                                                                color = Color.White.copy(alpha = 0.8f),
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        } else {
                                                            Text(
                                                                text = day.dayNumber.toString(),
                                                                color = if (isLogged && (!doItem.isMeasurable || loggedValue >= doItem.targetValue)) Color.Black else Color.White,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                            // Fill row if incomplete week
                                            if (week.size < 7) {
                                                for (i in 0 until (7 - week.size)) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Streaks card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Streaks Analysis",
                                color = themeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            // Streak metrics rows
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(BrightAmber.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = BrightAmber, modifier = Modifier.size(16.dp))
                                        }
                                        Column {
                                            Text("Current Streak", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("Consecutive days active", color = DarkTextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Text("$currentStreak Days", color = BrightAmber, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(themeColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = themeColor, modifier = Modifier.size(16.dp))
                                        }
                                        Column {
                                            Text("Longest Streak", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("Your ultimate all-time best", color = DarkTextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Text("$longestStreak Days", color = themeColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    // 6. Day Frequency Analysis Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Weekly Frequency",
                                color = themeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Calculate count per day of week
                            val weekdayCounts = remember(doLogs) {
                                val counts = IntArray(7) // Index 0=Saturday, 1=Sunday, ...
                                val cal = Calendar.getInstance()
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                
                                doLogs.forEach { log ->
                                    if (log.value > 0f) {
                                        try {
                                            val date = sdf.parse(log.date)
                                            if (date != null) {
                                                cal.time = date
                                                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1-7 (Sun-Sat)
                                                val forceSatIndex = (dayOfWeek - Calendar.SATURDAY + 7) % 7
                                                counts[forceSatIndex]++
                                            }
                                        } catch (e: Exception) {}
                                    }
                                }
                                counts
                            }

                            val weekdayLabels = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
                            val maxCount = weekdayCounts.maxOrNull() ?: 1

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                weekdayLabels.forEachIndexed { index, label ->
                                    val count = weekdayCounts[index]
                                    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            color = DarkTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(36.dp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(16.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DarkSurfaceVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(fraction = maxOf(0.05f, fraction))
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(themeColor)
                                            )
                                        }

                                        Text(
                                            text = count.toString(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(28.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// Custom line chart component
@Composable
fun ScoreTrendChart(
    points: List<Pair<String, Float>>,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 10.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        val width = size.width
        val height = size.height

        if (points.isEmpty()) return@Canvas

        val maxVal = 100f
        val pointsSize = points.size
        val xInterval = width / (pointsSize - 1)

        val drawPoints = points.mapIndexed { index, pair ->
            val x = index * xInterval
            val y = height - ((pair.second / maxVal) * height)
            Offset(x, y)
        }

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = (height / gridLines) * i
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw smooth path
        val path = Path()
        if (drawPoints.isNotEmpty()) {
            path.moveTo(drawPoints[0].x, drawPoints[0].y)
            for (i in 0 until drawPoints.size - 1) {
                val p1 = drawPoints[i]
                val p2 = drawPoints[i + 1]
                val controlX = (p1.x + p2.x) / 2
                path.cubicTo(
                    controlX, p1.y,
                    controlX, p2.y,
                    p2.x, p2.y
                )
            }
        }

        // Fill below the path
        val fillPath = Path()
        if (drawPoints.isNotEmpty()) {
            fillPath.addPath(path)
            fillPath.lineTo(drawPoints.last().x, height)
            fillPath.lineTo(drawPoints.first().x, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(themeColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )
        }

        // Draw line stroke
        drawPath(
            path = path,
            color = themeColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw circles at data points & labels
        drawPoints.forEachIndexed { index, offset ->
            drawCircle(
                color = themeColor,
                radius = 5.dp.toPx(),
                center = offset
            )
            drawCircle(
                color = Color.Black,
                radius = 2.5.dp.toPx(),
                center = offset
            )
        }
    }

    // Render X labels row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        points.forEachIndexed { index, pair ->
            val shouldShow = points.size <= 7 || index % 2 == 0 || index == points.size - 1
            Text(
                text = if (shouldShow) pair.first else "",
                color = DarkTextSecondary.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Custom bar chart component
@Composable
fun HistoryBarChart(
    points: List<Pair<String, Int>>,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 16.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        val width = size.width
        val height = size.height

        if (points.isEmpty()) return@Canvas

        val maxVal = maxOf(1, points.maxOf { it.second }).toFloat()
        val barsCount = points.size
        val barWidth = (width / barsCount) * 0.45f
        val xInterval = width / barsCount

        // Grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = (height / gridLines) * i
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        points.forEachIndexed { index, pair ->
            val barHeight = (pair.second / maxVal) * height
            val x = (index * xInterval) + (xInterval / 2) - (barWidth / 2)
            val y = height - barHeight

            // Draw shadow/track bar
            drawRoundRect(
                color = Color.White.copy(alpha = 0.03f),
                topLeft = Offset(x, 0f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(8.dp.toPx())
            )

            // Draw colored completion bar
            drawRoundRect(
                color = themeColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
        }
    }

    val colWidth = remember(points.size) { if (points.size > 7) 24.dp else 44.dp }
    val labelTextSize = remember(points.size) { if (points.size > 7) 8.sp else 9.sp }
    val valTextSize = remember(points.size) { if (points.size > 7) 9.sp else 11.sp }

    // Render labels row + numeric value above labels
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        points.forEachIndexed { index, pair ->
            val shouldShowLabel = points.size <= 7 || index % 2 == 0 || index == points.size - 1
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(colWidth)
            ) {
                Text(
                    text = if (shouldShowLabel) pair.second.toString() else "",
                    color = Color.White,
                    fontSize = valTextSize,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (shouldShowLabel) pair.first else "",
                    color = DarkTextSecondary.copy(alpha = 0.8f),
                    fontSize = labelTextSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class CalendarMonthData(
    val monthLabel: String,
    val days: List<CalendarDayData>
)

data class CalendarDayData(
    val dateString: String,
    val dayNumber: Int,
    val isValid: Boolean
)

data class InsightData(
    val title: String,
    val description: String,
    val recommendation: String,
    val trendType: String,
    val tintColor: Color,
    val currentCount: Int,
    val prevCount: Int
)

