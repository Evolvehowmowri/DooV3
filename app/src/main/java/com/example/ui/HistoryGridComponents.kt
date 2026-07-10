package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Do
import com.example.data.DoLog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryMatrixView(
    viewModel: DoViewModel,
    dos: List<Do>,
    logs: List<DoLog>,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onDoClick: (Do) -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("do_matrix_expanded", android.content.Context.MODE_PRIVATE) }
    var isExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("matrix_expanded", true)) }

    val persianCalendarEnabled by viewModel.persianCalendarEnabled.collectAsStateWithLifecycle()
    val usePersianScript by viewModel.usePersianScript.collectAsStateWithLifecycle()

    // Generate the last 7 days ending today
    val historyDays = remember(logs) {
        val list = mutableListOf<CalendarDayInfo>()
        val cal = Calendar.getInstance()
        // Go back 6 days to get a total of 7 columns (6 days ago -> today)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val numFormat = SimpleDateFormat("d", Locale.getDefault())
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        for (i in 0..6) {
            val dateStr = dateFormat.format(cal.time)
            list.add(
                CalendarDayInfo(
                    dateString = dateStr,
                    dayName = dayFormat.format(cal.time).uppercase(),
                    dayNumber = numFormat.format(cal.time),
                    isToday = dateStr == todayStr
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    var doToQuickLog by remember { mutableStateOf<Do?>(null) }
    var quickLogDate by remember { mutableStateOf("") }
    var quickLogInitialVal by remember { mutableStateOf(0f) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (showHeader) {
                // Header Toggle Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isExpanded = !isExpanded
                            sharedPrefs.edit().putBoolean("matrix_expanded", isExpanded).apply()
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "History Grid Icon",
                            tint = LavenderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Consistency Chain",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = !showHeader || isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = if (showHeader) 16.dp else 0.dp)) {
                    if (dos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create tasks/habits above to see the weekly matrix grid!",
                                color = DarkTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        // Table header row with dates
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Empty spacer on the left matching the Do details section
                            Spacer(modifier = Modifier.weight(1.3f))

                            Row(
                                modifier = Modifier.weight(2f),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                historyDays.forEach { day ->
                                    val displayDayName = remember(day.dateString, day.dayName, persianCalendarEnabled, usePersianScript) {
                                        if (persianCalendarEnabled && usePersianScript) {
                                            PersianCalendarHelper.getPersianShortDayName(day.dateString)
                                        } else {
                                            day.dayName.take(2).uppercase(Locale.getDefault())
                                        }
                                    }
                                    val displayDayNumber = remember(day.dateString, day.dayNumber, persianCalendarEnabled, usePersianScript) {
                                        if (persianCalendarEnabled) {
                                            val num = PersianCalendarHelper.getPersianDayNumber(day.dateString)
                                            if (usePersianScript) PersianCalendarHelper.toPersianDigits(num) else num
                                        } else {
                                            day.dayNumber
                                        }
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = displayDayName,
                                            color = if (day.isToday) LavenderAccent else DarkTextSecondary,
                                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 9.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = if (day.isToday) {
                                                Modifier
                                                    .size(16.dp)
                                                    .background(LavenderAccent.copy(alpha = 0.3f), CircleShape)
                                            } else {
                                                Modifier.size(16.dp)
                                            },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = displayDayNumber,
                                                color = if (day.isToday) LavenderAccent else Color.White,
                                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Divider
                        HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Rows grouped by category for each Do
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val groupedDos = remember(dos) {
                                dos.groupBy { it.category }
                            }
                            groupedDos.forEach { (category, categoryDos) ->
                                val catInfo = CategoryRegistry.getCategoryInfo(category)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = catInfo.icon,
                                        contentDescription = null,
                                        tint = catInfo.color,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = catInfo.name,
                                        color = catInfo.color,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                categoryDos.forEach { doItem ->
                                    val catInfo = CategoryRegistry.getCategoryInfo(doItem.category)

                                // Completion stats
                                val completionRate = remember(logs, doItem.id) {
                                    val doLogsMap = logs.filter { it.doId == doItem.id }.associateBy { it.date }
                                    var completedCount = 0
                                    historyDays.forEach { day ->
                                        val log = doLogsMap[day.dateString]
                                        if (log != null && log.value > 0f) {
                                            if (doItem.isMeasurable) {
                                                if (log.value >= doItem.targetValue) completedCount++
                                            } else {
                                                completedCount++
                                            }
                                        }
                                    }
                                    completedCount
                                }

                                val monthlyProgress = remember(logs, doItem.id) {
                                    val doLogsMap = logs.filter { it.doId == doItem.id }.associateBy { it.date }
                                    val cal = Calendar.getInstance()
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    var completed = 0
                                    val total = 30
                                    var totalDaysToConsider = 0
                                    for (i in 0 until total) {
                                        val dateStr = sdf.format(cal.time)
                                        val log = doLogsMap[dateStr]
                                        val isSkipped = log != null && log.value == -1f
                                        if (!isSkipped) {
                                            totalDaysToConsider++
                                            if (log != null && log.value > 0f) {
                                                if (doItem.isMeasurable) {
                                                    if (log.value >= doItem.targetValue) completed++
                                                } else {
                                                    completed++
                                                }
                                            }
                                        }
                                        cal.add(Calendar.DAY_OF_YEAR, -1)
                                    }
                                    if (totalDaysToConsider > 0) (completed.toFloat() / totalDaysToConsider) else 0f
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left section: Simplified Habit Info (Name and category icon only)
                                    Row(
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .clickable { onDoClick(doItem) },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val strokeWidth = 2.dp
                                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                                val strokeWidthPx = strokeWidth.toPx()
                                                val outerRadius = (size.minDimension - strokeWidthPx) / 2f
                                                
                                                // Muted background fill for the entire icon area
                                                drawCircle(
                                                    color = catInfo.color.copy(alpha = 0.12f),
                                                    radius = outerRadius
                                                )
                                                
                                                // Background ring track (semi-transparent white/gray)
                                                drawCircle(
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    radius = outerRadius,
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
                                                )
                                                
                                                // Active sweep representing the monthly completion rate
                                                drawArc(
                                                    color = catInfo.color,
                                                    startAngle = -90f,
                                                    sweepAngle = monthlyProgress * 360f,
                                                    useCenter = false,
                                                    topLeft = androidx.compose.ui.geometry.Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                                                    size = androidx.compose.ui.geometry.Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                        width = strokeWidthPx,
                                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                                    )
                                                )
                                            }
                                            
                                            Icon(
                                                imageVector = catInfo.icon,
                                                contentDescription = null,
                                                tint = catInfo.color,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = doItem.name,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Right section: 7 interactive grid boxes
                                    val logsMap = remember(logs) { logs.associateBy { "${it.doId}_${it.date}" } }
                                    val completedDatesForDo = remember(logs, doItem.id) {
                                        logs.filter { it.doId == doItem.id && it.value > 0f }
                                            .map { it.date }
                                            .sorted()
                                    }
                                    Row(
                                        modifier = Modifier.weight(2f),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        historyDays.forEach { day ->
                                            val log = logsMap["${doItem.id}_${day.dateString}"]
                                            val loggedValue = log?.value ?: 0f
                                            val isCompleted = if (doItem.isMeasurable) {
                                                loggedValue >= doItem.targetValue
                                            } else {
                                                loggedValue > 0f
                                            }
                                            val isSkipped = loggedValue == -1f
                                            
                                            // Calculate if it is mellow satisfied (within interval)
                                            val interval = doItem.reminderTime?.let { com.example.CustomReminderConfig.fromString(it).intervalDays } ?: 0
                                            val isMellowSatisfied = if (interval > 1 && !isCompleted && !isSkipped) {
                                                val lastCompletedDate = completedDatesForDo.lastOrNull { it <= day.dateString }
                                                if (lastCompletedDate != null) {
                                                    val daysDiff = getDaysBetween(lastCompletedDate, day.dateString)
                                                    daysDiff in 1 until interval
                                                } else {
                                                    false
                                                }
                                            } else {
                                                false
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 3.dp)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isCompleted) catInfo.color
                                                        else if (isMellowSatisfied) catInfo.color.copy(alpha = 0.35f)
                                                        else if (isSkipped) Color.White.copy(alpha = 0.08f)
                                                        else Color.White.copy(alpha = 0.03f)
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isCompleted) Color.Transparent
                                                                else if (isMellowSatisfied) catInfo.color.copy(alpha = 0.5f)
                                                                else if (isSkipped) Color.White.copy(alpha = 0.3f)
                                                                else GlassBorder,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .combinedClickable(
                                                        onClick = {
                                                            if (doItem.isMeasurable) {
                                                                doToQuickLog = doItem
                                                                quickLogDate = day.dateString
                                                                quickLogInitialVal = loggedValue
                                                            } else {
                                                                val nextValue = if (loggedValue > 0f) 0f else 1f
                                                                viewModel.logDoValue(doItem.id, day.dateString, nextValue)
                                                            }
                                                        },
                                                        onLongClick = {
                                                            val nextValue = if (loggedValue == -1f) 0f else -1f
                                                            viewModel.logDoValue(doItem.id, day.dateString, nextValue)
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSkipped) {
                                                    Text(
                                                        text = "-",
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                } else if (doItem.isMeasurable) {
                                                    val displayVal = remember(loggedValue, usePersianScript) {
                                                        if (loggedValue > 0f) {
                                                            val s = loggedValue.toInt().toString()
                                                            if (usePersianScript) PersianCalendarHelper.toPersianDigits(s) else s
                                                        } else {
                                                            ""
                                                        }
                                                    }
                                                    Text(
                                                        text = displayVal,
                                                        color = if (isCompleted) Color.Black else catInfo.color,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                } else {
                                                    if (isCompleted) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Logged Done",
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                    } else if (isMellowSatisfied) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(catInfo.color)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    // Quick Log Dialog for Measurable Dos
    doToQuickLog?.let { doItem ->
        MeasurableLogDialog(
            doItem = doItem,
            date = quickLogDate,
            initialValue = quickLogInitialVal,
            persianCalendarEnabled = persianCalendarEnabled,
            usePersianScript = usePersianScript,
            onDismiss = { doToQuickLog = null },
            onSave = { value ->
                viewModel.logDoValue(doItem.id, quickLogDate, value)
                doToQuickLog = null
            }
        )
    }
}

data class CalendarDayInfo(
    val dateString: String,
    val dayName: String,
    val dayNumber: String,
    val isToday: Boolean
)

fun getDaysBetween(date1: String, date2: String): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val d1 = sdf.parse(date1)!!
        val d2 = sdf.parse(date2)!!
        val diffMs = d2.time - d1.time
        (diffMs / (1000 * 60 * 60 * 24)).toInt()
    } catch (e: Exception) {
        0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurableLogDialog(
    doItem: Do,
    date: String,
    initialValue: Float,
    persianCalendarEnabled: Boolean = false,
    usePersianScript: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var textValue by remember { mutableStateOf(if (initialValue > 0f) initialValue.toInt().toString() else "") }
    val enteredValue = textValue.toFloatOrNull() ?: 0f
    val remaining = maxOf(0f, doItem.targetValue - enteredValue)

    val categoryColor = CategoryRegistry.getCategoryInfo(doItem.category).color

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Log Progress",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = doItem.name,
                            color = DarkTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Date Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date to Log", color = DarkTextSecondary, fontSize = 12.sp)
                    val displayDateText = remember(date, persianCalendarEnabled, usePersianScript) {
                        if (persianCalendarEnabled) {
                            PersianCalendarHelper.formatToPersianFull(date, usePersianScript)
                        } else {
                            val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
                            try {
                                val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
                                sdf.format(d)
                            } catch (e: Exception) {
                                date
                            }
                        }
                    }
                    Text(
                        text = displayDateText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Target status strip
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Goal:", color = DarkTextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${doItem.targetValue.toInt()} ${doItem.unit}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Progress Slider Bar
                    val progressFraction = if (doItem.targetValue > 0f) enteredValue / doItem.targetValue else 0f
                    LinearProgressIndicator(
                        progress = { minOf(1f, progressFraction) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = categoryColor,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )

                    // Target calculation status text
                    Text(
                        text = when {
                            enteredValue <= 0f -> "Not started. ${doItem.targetValue.toInt()} ${doItem.unit} remaining."
                            enteredValue < doItem.targetValue -> "${remaining.toInt()} ${doItem.unit} away from your target goal!"
                            enteredValue == doItem.targetValue -> "Perfect! Target goal achieved! 🎉"
                            else -> "Awesome! Target exceeded by ${(enteredValue - doItem.targetValue).toInt()} ${doItem.unit}! 🚀"
                        },
                        color = if (enteredValue >= doItem.targetValue) categoryColor else DarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }

                // Input Field and Presets
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            val current = textValue.toIntOrNull() ?: 0
                            if (current > 0) textValue = (current - 1).toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color.White)
                    }

                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = categoryColor,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        placeholder = { Text("0", color = DarkTextSecondary) },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val current = textValue.toIntOrNull() ?: 0
                            textValue = (current + 1).toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White)
                    }
                }

                // Presets row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { textValue = "0" },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Clear", color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { 
                            onSave(-1f)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1.1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Skip (-)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { textValue = (doItem.targetValue / 2).toInt().toString() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Half", color = Color.White, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { textValue = doItem.targetValue.toInt().toString() },
                        colors = ButtonDefaults.buttonColors(containerColor = categoryColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, categoryColor),
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Target", color = categoryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = DarkTextSecondary)
                    }

                    Button(
                        onClick = {
                            onSave(enteredValue)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = categoryColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Log", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(22.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val outerRadius = (size.minDimension - strokeWidth) / 2f
            
            // Background track (semi-transparent gray/white)
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = outerRadius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
            )
            
            // Active outer ring representing monthly score
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        
        // Inside: Category color mini circle indicator
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color.copy(alpha = 0.35f), CircleShape)
                .border(1.dp, color, CircleShape)
        )
    }
}

