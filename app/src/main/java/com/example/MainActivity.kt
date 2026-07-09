package com.example

import android.app.Application
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Do
import com.example.data.DoLog
import com.example.data.Category
import com.example.data.Note
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class ActiveTab {
    Agenda, Notes, Tools, More
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup notification channels and request permission
        ReminderManager.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

data class CustomReminderConfig(
    val times: List<String> = emptyList(), // e.g. ["08:30", "20:00"]
    val days: List<Int> = emptyList(), // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun. Empty means Daily.
    val sound: String = "Default Accent",
    val snooze: String = "No Snooze",
    val note: String = "",
    val intervalDays: Int = 0, // e.g. every 15 days
    val enableRandomWhispers: Boolean = false
) {
    fun toSerializedString(): String {
        val timesStr = times.joinToString(",")
        val daysStr = days.joinToString(",")
        val escapedNote = note.replace("|", "\\|").replace("=", "\\=")
        return "CONFIG_V1|times:$timesStr|days:$daysStr|sound:$sound|snooze:$snooze|note:$escapedNote|intervalDays:$intervalDays|enableRandomWhispers:$enableRandomWhispers"
    }

    companion object {
        fun fromString(str: String?): CustomReminderConfig {
            if (str == null || str.isBlank()) return CustomReminderConfig()
            if (!str.startsWith("CONFIG_V1|")) {
                // Legacy support for "08:30" format
                return if (str.contains(":")) {
                    CustomReminderConfig(times = listOf(str.trim()))
                } else {
                    CustomReminderConfig()
                }
            }
            try {
                val parts = str.substring("CONFIG_V1|".length).split("|")
                var times = emptyList<String>()
                var days = emptyList<Int>()
                var sound = "Default Accent"
                var snooze = "No Snooze"
                var note = ""
                var intervalDays = 0
                var enableRandomWhispers = false

                for (part in parts) {
                    val colonIdx = part.indexOf(":")
                    if (colonIdx != -1) {
                        val key = part.substring(0, colonIdx)
                        val value = part.substring(colonIdx + 1)
                        when (key) {
                            "times" -> {
                                if (value.isNotBlank()) {
                                    times = value.split(",")
                                }
                            }
                            "days" -> {
                                if (value.isNotBlank()) {
                                    days = value.split(",").mapNotNull { 
                                        val day = it.toIntOrNull()
                                        if (day != null && day in 1..7) day else null
                                    }
                                }
                            }
                            "sound" -> sound = value
                            "snooze" -> snooze = value
                            "note" -> note = value.replace("\\|", "|").replace("\\=", "=")
                            "intervalDays" -> intervalDays = value.toIntOrNull() ?: 0
                            "enableRandomWhispers" -> enableRandomWhispers = value.toBoolean()
                        }
                    }
                }
                return CustomReminderConfig(times, days, sound, snooze, note, intervalDays, enableRandomWhispers)
            } catch (e: Exception) {
                return CustomReminderConfig()
            }
        }
    }

    fun getDisplayString(): String {
        if (times.isEmpty() && intervalDays <= 0) return ""
        val timesFormatted = times.joinToString(", ") { formatTime(it) }
        val daysFormatted = if (intervalDays > 1) {
            "Every $intervalDays days"
        } else {
            when {
                days.isEmpty() || days.size == 7 -> "Daily"
                days.sorted() == listOf(1, 2, 3, 4, 5) -> "Weekdays"
                days.sorted() == listOf(6, 7) -> "Weekends"
                else -> {
                    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.sorted().joinToString(", ") { dayNum ->
                        if (dayNum in 1..7) dayNames[dayNum - 1] else "?"
                    }
                }
            }
        }
        val noteSuffix = if (note.isNotBlank()) " • \"$note\"" else ""
        val soundSuffix = if (sound != "Default Accent") " 🔔 $sound" else ""
        val whisperSuffix = if (enableRandomWhispers) " • 🎙️ Whispers Enabled" else ""
        return "$timesFormatted ($daysFormatted)$soundSuffix$noteSuffix$whisperSuffix"
    }

    private fun formatTime(hhmm: String): String {
        val parts = hhmm.split(":")
        if (parts.size != 2) return hhmm
        val h = parts[0].toIntOrNull() ?: return hhmm
        val m = parts[1].toIntOrNull() ?: return hhmm
        val ampm = if (h >= 12) "PM" else "AM"
        val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, ampm)
    }
}

@Composable
fun GlassyBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LavenderAccent.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.15f),
                    radius = size.width * 0.7f
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.8f, size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LightBlueAccent.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.75f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width * 0.15f, size.height * 0.75f)
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: DoViewModel = viewModel(factory = DoViewModelFactory(application))

    val dos by viewModel.dos.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()
    val persianCalendarEnabled by viewModel.persianCalendarEnabled.collectAsStateWithLifecycle()
    val usePersianScript by viewModel.usePersianScript.collectAsStateWithLifecycle()
    val calendarDays by viewModel.calendarDays.collectAsStateWithLifecycle()
    val overduePosition by viewModel.overduePosition.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTodayAndCalendarDays()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(categoriesList) {
        CategoryRegistry.dynamicCategoriesList = categoriesList
    }

    var activeTab by remember { mutableStateOf(ActiveTab.Agenda) }
    var showAddDialog by remember { mutableStateOf(false) }
    var doToEdit by remember { mutableStateOf<Do?>(null) }
    var selectedDoForStats by remember { mutableStateOf<Do?>(null) }

    GlassyBackground {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = DarkBg.copy(alpha = 0.85f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    NavigationBarItem(
                        selected = activeTab == ActiveTab.Agenda,
                        onClick = { activeTab = ActiveTab.Agenda },
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Do") },
                        label = { Text("Do") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleContainer,
                            selectedTextColor = LavenderAccent,
                            indicatorColor = LavenderAccent,
                            unselectedIconColor = DarkTextSecondary,
                            unselectedTextColor = DarkTextSecondary
                        ),
                        modifier = Modifier.testTag("tab_agenda")
                    )
                    NavigationBarItem(
                        selected = activeTab == ActiveTab.Notes,
                        onClick = { activeTab = ActiveTab.Notes },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = "Notes") },
                        label = { Text("Notes") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleContainer,
                            selectedTextColor = LavenderAccent,
                            indicatorColor = LavenderAccent,
                            unselectedIconColor = DarkTextSecondary,
                            unselectedTextColor = DarkTextSecondary
                        ),
                        modifier = Modifier.testTag("tab_notes")
                    )
                    NavigationBarItem(
                        selected = activeTab == ActiveTab.Tools,
                        onClick = { activeTab = ActiveTab.Tools },
                        icon = {
                            val pendingNotifications by viewModel.pendingNotifications.collectAsStateWithLifecycle()
                            val notifCount = pendingNotifications.size
                            if (notifCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color.Red,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = notifCount.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.testTag("tools_notif_count_badge")
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.VolunteerActivism, contentDescription = "Tools")
                                }
                            } else {
                                Icon(Icons.Default.VolunteerActivism, contentDescription = "Tools")
                            }
                        },
                        label = { Text("Tools") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleContainer,
                            selectedTextColor = LavenderAccent,
                            indicatorColor = LavenderAccent,
                            unselectedIconColor = DarkTextSecondary,
                            unselectedTextColor = DarkTextSecondary
                        ),
                        modifier = Modifier.testTag("tab_tools")
                    )
                    NavigationBarItem(
                        selected = activeTab == ActiveTab.More,
                        onClick = { activeTab = ActiveTab.More },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                        label = { Text("More") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepPurpleContainer,
                            selectedTextColor = LavenderAccent,
                            indicatorColor = LavenderAccent,
                            unselectedIconColor = DarkTextSecondary,
                            unselectedTextColor = DarkTextSecondary
                        ),
                        modifier = Modifier.testTag("tab_more")
                    )
                }
            },
            floatingActionButton = {
                if (activeTab == ActiveTab.Agenda) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = LavenderAccent,
                        contentColor = DeepPurpleContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .testTag("add_do_fab")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Do", modifier = Modifier.size(28.dp))
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    ActiveTab.Agenda -> AgendaScreen(
                        viewModel = viewModel,
                        dos = dos,
                        logs = logs,
                        selectedDate = selectedDate,
                        onDetailClick = { doItem ->
                            selectedDoForStats = doItem
                        },
                        onEditDo = { doItem ->
                            doToEdit = doItem
                            showAddDialog = true
                        }
                    )
                    ActiveTab.Notes -> NotesScreen(
                        viewModel = viewModel
                    )
                    ActiveTab.Tools -> ToolsScreen(
                        viewModel = viewModel
                    )
                    ActiveTab.More -> MoreScreen(
                        viewModel = viewModel,
                        dos = dos,
                        logs = logs
                    )
                }
            }
        }
    }

    if (selectedDoForStats != null) {
        com.example.ui.HabitDetailScreen(
            doItem = selectedDoForStats!!,
            logs = logs,
            onDismiss = { selectedDoForStats = null },
            onEdit = {
                doToEdit = selectedDoForStats
                showAddDialog = true
                selectedDoForStats = null
            },
            onDelete = {
                viewModel.deleteDo(selectedDoForStats!!.id)
                selectedDoForStats = null
            },
            viewModel = viewModel
        )
    }

    if (showAddDialog) {
        AddEditDoDialog(
            viewModel = viewModel,
            doToEdit = doToEdit,
            defaultDateString = selectedDate,
            onDismiss = {
                showAddDialog = false
                doToEdit = null
            },
            onSave = { name, type, isMeasurable, targetValue, unit, priority, category, targetDate, targetTime, reminder, purpose, benefit, cost ->
                if (doToEdit != null) {
                    viewModel.updateDo(
                        doToEdit!!.copy(
                            name = name,
                            type = type,
                            isMeasurable = isMeasurable,
                            targetValue = targetValue,
                            unit = unit,
                            priority = priority,
                            category = category,
                            targetDate = targetDate,
                            targetTime = targetTime,
                            reminderTime = reminder,
                            purpose = purpose,
                            benefit = benefit,
                            cost = cost
                        )
                    )
                } else {
                    viewModel.addDo(
                        name = name,
                        type = type,
                        isMeasurable = isMeasurable,
                        targetValue = targetValue,
                        unit = unit,
                        priority = priority,
                        category = category,
                        targetDate = targetDate,
                        targetTime = targetTime,
                        reminderTime = reminder,
                        purpose = purpose,
                        benefit = benefit,
                        cost = cost
                    )
                }
                showAddDialog = false
                doToEdit = null
            }
        )
    }
}

// ---------------- AGENDA & WORKSPACE SCREEN ----------------

enum class ViewScope {
    DAILY, ALL_TIME
}

enum class GroupingStrategy {
    NONE, TYPE, CATEGORY, PRIORITY, COMPLETION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: DoViewModel,
    dos: List<Do>,
    logs: List<DoLog>,
    selectedDate: String,
    onDetailClick: (Do) -> Unit,
    onEditDo: (Do) -> Unit
) {
    val doneDoBehavior by viewModel.doneDoBehavior.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val persianCalendarEnabled by viewModel.persianCalendarEnabled.collectAsStateWithLifecycle()
    val usePersianScript by viewModel.usePersianScript.collectAsStateWithLifecycle()
    val calendarDays by viewModel.calendarDays.collectAsStateWithLifecycle()
    val overduePosition by viewModel.overduePosition.collectAsStateWithLifecycle()

    val displayDateText = remember(selectedDate, persianCalendarEnabled, usePersianScript) {
        if (persianCalendarEnabled) {
            PersianCalendarHelper.formatToPersianFull(selectedDate, usePersianScript)
        } else {
            val displayDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            val dateParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            try {
                val date = dateParser.parse(selectedDate)
                displayDateFormat.format(date ?: java.util.Date())
            } catch (e: Exception) {
                selectedDate
            }
        }
    }

    // Grouping, Sorting, and Collapsible states
    var groupingStrategy by rememberSaveable { mutableStateOf(GroupingStrategy.NONE) }
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val priorityFilter by viewModel.priorityFilter.collectAsStateWithLifecycle()
    val completionFilter by viewModel.completionFilter.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("agenda_expanded_states", android.content.Context.MODE_PRIVATE) }
    var isControlsExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("controls_expanded", false)) }
    var isCCExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("cc_expanded", true)) }
    var isTasksExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("tasks_expanded", true)) }
    var isHabitsAndTasksExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("habits_and_tasks_expanded", true)) }
    var isAllTimeLibrarySelected by rememberSaveable { mutableStateOf(false) }

    // Today's Agenda Items
    val todayDos = remember(dos, selectedDate, logs) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        dos.filter { doItem ->
            if (doItem.type == "RECURRING") {
                true
            } else {
                val isToday = doItem.targetDate == selectedDate
                val isOverdue = if (selectedDate == todayStr) {
                    doItem.type == "TASK" &&
                    !doItem.targetDate.isNullOrBlank() &&
                    doItem.targetDate < todayStr &&
                    run {
                        val isCompleted = logs.any { log ->
                            log.doId == doItem.id &&
                            if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value >= 1f
                        }
                        !isCompleted
                    }
                } else false

                isToday || isOverdue
            }
        }
    }

    // Filter by search, category, type, priority, and completion
    val filteredTodayDos = remember(todayDos, selectedCategory, searchQuery, typeFilter, priorityFilter, completionFilter, logs, selectedDate, doneDoBehavior) {
        todayDos.filter { doItem ->
            // Search
            val matchSearch = searchQuery.isEmpty() || doItem.name.contains(searchQuery, ignoreCase = true)

            // Category
            val matchCategory = selectedCategory == "All" || doItem.category.split(",").map { it.trim().lowercase() }.contains(selectedCategory.lowercase())

            // Type
            val matchType = when (typeFilter) {
                "ALL" -> true
                "RE" -> doItem.type == "RECURRING"
                "TASKS" -> doItem.type == "TASK"
                "DUE" -> {
                    doItem.type == "TASK" && doItem.targetDate == selectedDate
                }
                "OVERDUE" -> {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
                    doItem.type == "TASK" && !doItem.targetDate.isNullOrBlank() && doItem.targetDate < todayStr && run {
                        val isCompleted = logs.any { l ->
                            l.doId == doItem.id &&
                            if (doItem.isMeasurable) l.value >= doItem.targetValue else l.value >= 1f
                        }
                        !isCompleted
                    }
                }
                else -> true
            }

            // Priority
            val matchPriority = when (priorityFilter) {
                "ALL" -> true
                "1" -> doItem.priority == "1" || doItem.priority == "HIGH"
                "2" -> doItem.priority == "2" || doItem.priority == "MEDIUM"
                "3" -> doItem.priority == "3" || doItem.priority == "LOW"
                "4" -> doItem.priority == "4"
                else -> true
            }

            // Completion
            val log = logs.find { it.doId == doItem.id && it.date == selectedDate }
            val currentValue = log?.value ?: 0f
            val isCompleted = if (doItem.isMeasurable) currentValue >= doItem.targetValue else currentValue >= 1f
            val matchCompletion = when (completionFilter) {
                "ALL" -> true
                "PENDING" -> !isCompleted
                "DONE" -> isCompleted
                else -> true
            }

            // Done Do behavior
            val matchDoneBehavior = if (doneDoBehavior == "HIDE") {
                !isCompleted
            } else {
                true
            }

            matchSearch && matchCategory && matchType && matchPriority && matchCompletion && matchDoneBehavior
        }
    }

    // Sort items
    val sortedTodayDos = remember(filteredTodayDos, sortBy, logs, selectedDate, doneDoBehavior, overduePosition) {
        filteredTodayDos.sortedWith { v1, v2 ->
            // Overdue Placement
            if (overduePosition == "TOP" || overduePosition == "BOTTOM") {
                val isOverdue1 = v1.type == "TASK" && !v1.targetDate.isNullOrBlank() && v1.targetDate < selectedDate && run {
                    val log1 = logs.find { it.doId == v1.id && it.date == selectedDate }
                    val isComp1 = if (v1.isMeasurable) (log1?.value ?: 0f) >= v1.targetValue else (log1?.value ?: 0f) >= 1f
                    !isComp1
                }
                val isOverdue2 = v2.type == "TASK" && !v2.targetDate.isNullOrBlank() && v2.targetDate < selectedDate && run {
                    val log2 = logs.find { it.doId == v2.id && it.date == selectedDate }
                    val isComp2 = if (v2.isMeasurable) (log2?.value ?: 0f) >= v2.targetValue else (log2?.value ?: 0f) >= 1f
                    !isComp2
                }

                if (isOverdue1 != isOverdue2) {
                    return@sortedWith if (overduePosition == "TOP") {
                        if (isOverdue1) -1 else 1
                    } else {
                        if (isOverdue1) 1 else -1
                    }
                }
            }

            if (doneDoBehavior == "STRIKETHROUGH_BOTTOM") {
                val log1 = logs.find { it.doId == v1.id && it.date == selectedDate }
                val isComp1 = if (v1.isMeasurable) (log1?.value ?: 0f) >= v1.targetValue else (log1?.value ?: 0f) >= 1f
                val log2 = logs.find { it.doId == v2.id && it.date == selectedDate }
                val isComp2 = if (v2.isMeasurable) (log2?.value ?: 0f) >= v2.targetValue else (log2?.value ?: 0f) >= 1f
                
                if (isComp1 != isComp2) {
                    return@sortedWith if (isComp1) 1 else -1
                }
            }

            when (sortBy) {
                "PRIORITY" -> {
                    val p1 = when (v1.priority) { "1", "HIGH" -> 0; "2", "MEDIUM" -> 1; "3", "LOW" -> 2; "4" -> 3; else -> 4 }
                    val p2 = when (v2.priority) { "1", "HIGH" -> 0; "2", "MEDIUM" -> 1; "3", "LOW" -> 2; "4" -> 3; else -> 4 }
                    val cmp = p1.compareTo(p2)
                    if (cmp != 0) cmp else v1.name.compareTo(v2.name, ignoreCase = true)
                }
                "TIME" -> {
                    val d1 = v1.targetDate.takeIf { !it.isNullOrBlank() } ?: "9999-12-31"
                    val d2 = v2.targetDate.takeIf { !it.isNullOrBlank() } ?: "9999-12-31"
                    val cmpDate = d1.compareTo(d2)
                    if (cmpDate != 0) {
                        cmpDate
                    } else {
                        val config1 = CustomReminderConfig.fromString(v1.reminderTime)
                        val time1 = v1.targetTime.takeIf { !it.isNullOrBlank() } ?: config1.times.minOrNull() ?: "23:59"
                        val config2 = CustomReminderConfig.fromString(v2.reminderTime)
                        val time2 = v2.targetTime.takeIf { !it.isNullOrBlank() } ?: config2.times.minOrNull() ?: "23:59"
                        val cmpTime = time1.compareTo(time2)
                        if (cmpTime != 0) cmpTime else v1.name.compareTo(v2.name, ignoreCase = true)
                    }
                }
                "NAME" -> {
                    v1.name.compareTo(v2.name, ignoreCase = true)
                }
                "STATUS" -> {
                    val log1 = logs.find { it.doId == v1.id && it.date == selectedDate }
                    val isComp1 = if (v1.isMeasurable) (log1?.value ?: 0f) >= v1.targetValue else (log1?.value ?: 0f) >= 1f
                    val log2 = logs.find { it.doId == v2.id && it.date == selectedDate }
                    val isComp2 = if (v2.isMeasurable) (log2?.value ?: 0f) >= v2.targetValue else (log2?.value ?: 0f) >= 1f
                    
                    val cmp = isComp1.compareTo(isComp2)
                    if (cmp != 0) cmp else v1.name.compareTo(v2.name, ignoreCase = true)
                }
                "CREATION" -> {
                    v2.createdAt.compareTo(v1.createdAt)
                }
                else -> v1.createdAt.compareTo(v2.createdAt)
            }
        }
    }

    // Filter, Sort and Group for Library Catalog
    val filteredLibraryDos = remember(dos, selectedCategory, searchQuery, typeFilter, priorityFilter) {
        dos.filter { doItem ->
            val matchSearch = searchQuery.isEmpty() || doItem.name.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == "All" || doItem.category.split(",").map { it.trim().lowercase() }.contains(selectedCategory.lowercase())
            val matchType = when (typeFilter) {
                "ALL" -> true
                "RE" -> doItem.type == "RECURRING"
                "TASKS" -> doItem.type == "TASK"
                else -> true
            }
            val matchPriority = when (priorityFilter) {
                "ALL" -> true
                "1" -> doItem.priority == "1" || doItem.priority == "HIGH"
                "2" -> doItem.priority == "2" || doItem.priority == "MEDIUM"
                "3" -> doItem.priority == "3" || doItem.priority == "LOW"
                "4" -> doItem.priority == "4"
                else -> true
            }
            matchSearch && matchCategory && matchType && matchPriority
        }
    }

    val sortedLibraryDos = remember(filteredLibraryDos, sortBy) {
        filteredLibraryDos.sortedWith { v1, v2 ->
            when (sortBy) {
                "PRIORITY" -> {
                    val p1 = when (v1.priority) { "1", "HIGH" -> 0; "2", "MEDIUM" -> 1; "3", "LOW" -> 2; "4" -> 3; else -> 4 }
                    val p2 = when (v2.priority) { "1", "HIGH" -> 0; "2", "MEDIUM" -> 1; "3", "LOW" -> 2; "4" -> 3; else -> 4 }
                    val cmp = p1.compareTo(p2)
                    if (cmp != 0) cmp else v1.name.compareTo(v2.name, ignoreCase = true)
                }
                "NAME" -> {
                    v1.name.compareTo(v2.name, ignoreCase = true)
                }
                "CREATION" -> {
                    v2.createdAt.compareTo(v1.createdAt)
                }
                else -> v1.createdAt.compareTo(v2.createdAt)
            }
        }
    }

    val activeDos = remember(isAllTimeLibrarySelected, sortedTodayDos, sortedLibraryDos) {
        if (isAllTimeLibrarySelected) sortedLibraryDos else sortedTodayDos
    }

    // Determine overdue tasks (Incomplete, targetDate in past relative to real today)
    val overdueTasks = remember(dos, logs) {
        val realToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        dos.filter { doItem ->
            doItem.type == "TASK" &&
            !doItem.targetDate.isNullOrBlank() &&
            doItem.targetDate < realToday &&
            run {
                val isCompleted = logs.any { log ->
                    log.doId == doItem.id &&
                    if (doItem.isMeasurable) log.value >= doItem.targetValue else log.value >= 1f
                }
                !isCompleted
            }
        }
    }

    // Group items dynamically for the unified display
    val groupedSections = remember(activeDos, groupingStrategy, logs, selectedDate, overdueTasks, isAllTimeLibrarySelected) {
        val baseSections = when (groupingStrategy) {
            GroupingStrategy.NONE -> {
                listOf("" to activeDos)
            }
            GroupingStrategy.TYPE -> {
                val res = activeDos.filter { it.type == "RECURRING" }
                val tasks = activeDos.filter { it.type == "TASK" }
                val list = mutableListOf<Pair<String, List<Do>>>()
                if (res.isNotEmpty()) list.add("Recurring REs (Re)" to res)
                if (tasks.isNotEmpty()) list.add("One-Time Tasks" to tasks)
                list
            }
            GroupingStrategy.CATEGORY -> {
                val categoriesMap = mutableMapOf<String, MutableList<Do>>()
                for (item in activeDos) {
                    val cats = item.category.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val finalCats = if (cats.isEmpty()) listOf("Uncategorized") else cats
                    for (cat in finalCats) {
                        categoriesMap.getOrPut(cat) { mutableListOf() }.add(item)
                    }
                }
                categoriesMap.toList().sortedBy { it.first }
            }
            GroupingStrategy.PRIORITY -> {
                val priorityMap = activeDos.groupBy {
                    when (it.priority) {
                        "1", "HIGH" -> "Priority 1 (High)"
                        "2", "MEDIUM" -> "Priority 2 (Medium)"
                        "3", "LOW" -> "Priority 3 (Low)"
                        "4" -> "Priority 4"
                        else -> "Priority ${it.priority}"
                    }
                }
                priorityMap.toList().sortedBy { it.first }
            }
            GroupingStrategy.COMPLETION -> {
                val pending = mutableListOf<Do>()
                val completed = mutableListOf<Do>()
                for (item in activeDos) {
                    val log = logs.find { it.doId == item.id && it.date == selectedDate }
                    val currentValue = log?.value ?: 0f
                    val isComp = if (item.isMeasurable) currentValue >= item.targetValue else currentValue >= 1f
                    if (isComp) completed.add(item) else pending.add(item)
                }
                val list = mutableListOf<Pair<String, List<Do>>>()
                if (pending.isNotEmpty()) list.add("Pending" to pending)
                if (completed.isNotEmpty()) list.add("Done" to completed)
                list
            }
        }

        if (!isAllTimeLibrarySelected && overdueTasks.isNotEmpty()) {
            listOf("⚠️ Overdue Tasks (Incomplete)" to overdueTasks) + baseSections
        } else {
            baseSections
        }
    }



    Column(modifier = Modifier.fillMaxSize()) {
        // Pinned Calendar at the top of the screen (does not scroll away, which is beautiful and premium!)
        HorizontalCalendar(
            calendarDays = calendarDays,
            selectedDate = selectedDate,
            onDateSelected = { viewModel.setSelectedDate(it) },
            persianCalendarEnabled = persianCalendarEnabled,
            usePersianScript = usePersianScript
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dropdown 1: Advanced Filters, Search & Grouping Controls
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                isControlsExpanded = !isControlsExpanded
                                sharedPrefs.edit().putBoolean("controls_expanded", isControlsExpanded).apply()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LavenderAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Filters & Search",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isControlsExpanded) "Tap to hide search and filters" else "Tap to search & customize agenda view",
                                    color = DarkTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isControlsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isControlsExpanded) "Collapse" else "Expand",
                            tint = DarkTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (isControlsExpanded) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {



                    // A. Search Bar
                    Text(
                        text = "Search Query",
                        color = DarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by name...", color = DarkTextSecondary, fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = LavenderAccent,
                            unfocusedBorderColor = GlassBorder
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = DarkTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // B. Grouping Strategy
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Group List By",
                            color = DarkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                GroupingStrategy.NONE to "None",
                                GroupingStrategy.TYPE to "Type",
                                GroupingStrategy.CATEGORY to "Category",
                                GroupingStrategy.PRIORITY to "Priority",
                                GroupingStrategy.COMPLETION to "Completion"
                            ).forEach { (strat, label) ->
                                val isSelected = groupingStrategy == strat
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) LavenderAccent else DarkSurfaceVariant)
                                        .border(1.dp, if (isSelected) Color.Transparent else GlassBorder, RoundedCornerShape(6.dp))
                                        .clickable { groupingStrategy = strat }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // C. Horizontal Category Quick Filter Row
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Quick Category Filter",
                            color = DarkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val cats = listOf("All") + CategoryRegistry.dynamicCategoriesList.map { it.name }
                            items(cats) { cat ->
                                val isSelected = selectedCategory == cat
                                val categoryColor = if (cat == "All") LavenderAccent else CategoryRegistry.getCategoryInfo(cat).color
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) categoryColor else DarkSurfaceVariant)
                                        .border(1.dp, if (isSelected) Color.Transparent else GlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setSelectedCategory(cat) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // D. Filter by Type & Status
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Filter by Type / Status",
                            color = DarkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            listOf(
                                "ALL" to "All",
                                "RE" to "REs Only",
                                "TASKS" to "Tasks Only",
                                "DUE" to "Due Today",
                                "OVERDUE" to "Overdue"
                            ).forEach { (key, label) ->
                                val isSelected = typeFilter == key
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) LavenderAccent else DarkSurfaceVariant)
                                        .border(1.dp, if (isSelected) Color.Transparent else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setTypeFilter(key) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // E. Priority and Status Filter Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Priority Selector
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Priority Filter", color = DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("ALL" to "All", "1" to "1", "2" to "2", "3" to "3", "4" to "4").forEach { (key, label) ->
                                    val isSelected = priorityFilter == key
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) LavenderAccent else DarkSurfaceVariant)
                                            .border(1.dp, if (isSelected) Color.Transparent else GlassBorder, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.setPriorityFilter(key) }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(text = label, color = if (isSelected) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Completion Status Selector
                        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Completion Status", color = DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("ALL" to "All", "PENDING" to "Pending", "DONE" to "Done").forEach { (key, label) ->
                                    val isSelected = completionFilter == key
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) LavenderAccent else DarkSurfaceVariant)
                                            .border(1.dp, if (isSelected) Color.Transparent else GlassBorder, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.setCompletionFilter(key) }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Text(text = label, color = if (isSelected) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // F. Sorting Preference
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Sorting Preference",
                            color = DarkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "PRIORITY" to "Priority",
                                "TIME" to "Time",
                                "NAME" to "Name",
                                "STATUS" to "Status",
                                "CREATION" to "Created"
                            ).forEach { (key, label) ->
                                val isSelected = sortBy == key
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) LavenderAccent else DarkSurfaceVariant)
                                        .border(1.dp, if (isSelected) Color.Transparent else GlassBorder, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setSortBy(key) }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Consistency Chain Section Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            isCCExpanded = !isCCExpanded
                            sharedPrefs.edit().putBoolean("cc_expanded", isCCExpanded).apply()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LavenderAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "Consistency Chain",
                                tint = LavenderAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Consistency Chain",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isCCExpanded) "Tap to hide daily consistency grid" else "Tap to show habit heat matrix",
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isCCExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isCCExpanded) "Collapse" else "Expand",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (isCCExpanded) {
            item {
                HistoryMatrixView(
                    viewModel = viewModel,
                    dos = filteredLibraryDos.filter { it.type == "RECURRING" },
                    logs = logs,
                    showHeader = false,
                    onDoClick = { onDetailClick(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Habits & Tasks Section Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            isHabitsAndTasksExpanded = !isHabitsAndTasksExpanded
                            sharedPrefs.edit().putBoolean("habits_and_tasks_expanded", isHabitsAndTasksExpanded).apply()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(LavenderAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Doings",
                                tint = LavenderAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Doings",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isHabitsAndTasksExpanded) "Tap to hide habits and tasks list" else "Tap to view habits and tasks",
                                color = DarkTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isHabitsAndTasksExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isHabitsAndTasksExpanded) "Collapse" else "Expand",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (isHabitsAndTasksExpanded) {
            // Render view toggles inside the dropdown container
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left Button: Date Based
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isAllTimeLibrarySelected) LavenderAccent else DarkSurface)
                            .border(1.dp, if (!isAllTimeLibrarySelected) Color.Transparent else GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { isAllTimeLibrarySelected = false }
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date Based",
                                tint = if (!isAllTimeLibrarySelected) Color.Black else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Date Based",
                                color = if (!isAllTimeLibrarySelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Right Button: All-Time Library
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAllTimeLibrarySelected) LavenderAccent else DarkSurface)
                            .border(1.dp, if (isAllTimeLibrarySelected) Color.Transparent else GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { isAllTimeLibrarySelected = true }
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "All-Time Library",
                                tint = if (isAllTimeLibrarySelected) Color.Black else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "All-Time Library",
                                color = if (isAllTimeLibrarySelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Render list items or empty state
            if (activeDos.isEmpty() && (!isAllTimeLibrarySelected || filteredLibraryDos.isEmpty())) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No doings found.",
                            color = DarkTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                groupedSections.forEach { (sectionTitle, sectionItems) ->
                    if (sectionTitle.isNotEmpty()) {
                        item {
                            Text(
                                text = sectionTitle,
                                color = if (sectionTitle.startsWith("⚠️")) Color(0xFFFF5252) else LavenderAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                    }
                    items(sectionItems, key = { doItem -> "${sectionTitle}_${doItem.id}" }) { doItem ->
                        val log = logs.find { it.doId == doItem.id && it.date == selectedDate }
                        val currentValueOnSelectedDate = log?.value ?: 0f
                        val isCompletedOnSelectedDate = if (doItem.isMeasurable) {
                            currentValueOnSelectedDate >= doItem.targetValue
                        } else {
                            currentValueOnSelectedDate >= 1f
                        }

                        val completedLogAnyDay = if (isAllTimeLibrarySelected) {
                            logs.filter { it.doId == doItem.id }.find { l ->
                                if (doItem.isMeasurable) l.value >= doItem.targetValue else l.value >= 1f
                            }
                        } else {
                            null
                        }

                        val isCompleted = if (isAllTimeLibrarySelected && completedLogAnyDay != null) {
                            true
                        } else {
                            isCompletedOnSelectedDate
                        }

                        val currentValue = if (isAllTimeLibrarySelected && completedLogAnyDay != null) {
                            completedLogAnyDay.value
                        } else {
                            currentValueOnSelectedDate
                        }

                        val completedDate = if (isAllTimeLibrarySelected && completedLogAnyDay != null) {
                            completedLogAnyDay.date
                        } else {
                            ""
                        }

                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            AgendaDoItem(
                                doItem = doItem,
                                currentValue = currentValue,
                                isCompleted = isCompleted,
                                onLogToggle = {
                                    val nextValue = if (isCompletedOnSelectedDate) 0f else 1f
                                    viewModel.logDoValue(doItem.id, selectedDate, nextValue)
                                },
                                onIncrement = {
                                    val nextValue = (currentValueOnSelectedDate + 1f).coerceAtMost(doItem.targetValue * 10f)
                                    viewModel.logDoValue(doItem.id, selectedDate, nextValue)
                                },
                                onDecrement = {
                                    val nextValue = (currentValueOnSelectedDate - 1f).coerceAtLeast(0f)
                                    viewModel.logDoValue(doItem.id, selectedDate, nextValue)
                                },
                                onLogSkip = {
                                    val nextValue = if (currentValueOnSelectedDate == -1f) 0f else -1f
                                    viewModel.logDoValue(doItem.id, selectedDate, nextValue)
                                },
                                doneDoBehavior = doneDoBehavior,
                                onDetailClick = { onDetailClick(doItem) },
                                selectedDate = selectedDate,
                                completedDate = completedDate,
                                persianCalendarEnabled = persianCalendarEnabled,
                                usePersianScript = usePersianScript
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
}

@Composable
fun HorizontalCalendar(
    calendarDays: List<CalendarDay>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    persianCalendarEnabled: Boolean = false,
    usePersianScript: Boolean = false
) {
    val listState = rememberLazyListState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val todayDateString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()) }
    
    // 54.dp is card width. Spacing is 8.dp.
    val centerOffsetPx = with(density) {
        ((configuration.screenWidthDp - 54) / 2).dp.roundToPx()
    }

    LaunchedEffect(selectedDate, calendarDays) {
        val index = calendarDays.indexOfFirst { it.dateString == selectedDate }
        if (index >= 0) {
            listState.animateScrollToItem(index, -centerOffsetPx)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(width = (0.5).dp, color = GlassBorder, shape = RoundedCornerShape(0.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(calendarDays) { day ->
            val isSelected = day.dateString == selectedDate
            val isToday = day.dateString == todayDateString
            val borderModifier = if (isSelected) {
                Modifier.border(1.5.dp, LavenderAccent, RoundedCornerShape(12.dp))
            } else if (isToday) {
                Modifier.border(1.dp, LavenderAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            } else {
                Modifier.border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        DeepPurpleContainer.copy(alpha = 0.4f)
                    } else if (isToday) {
                        DeepPurpleContainer.copy(alpha = 0.15f)
                    } else {
                        DarkSurface
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = borderModifier
                    .width(54.dp)
                    .height(68.dp)
                    .clickable { onDateSelected(day.dateString) }
                    .testTag("calendar_day_${day.dateString}")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val displayDayName = remember(day.dateString, day.dayName, persianCalendarEnabled, usePersianScript) {
                        if (persianCalendarEnabled && usePersianScript) {
                            PersianCalendarHelper.getPersianShortDayName(day.dateString)
                        } else {
                            day.dayName.uppercase(Locale.getDefault())
                        }
                    }
                    Text(
                        text = displayDayName,
                        color = if (isSelected) LavenderAccent else if (isToday) LavenderAccent.copy(alpha = 0.8f) else DarkTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val displayDayNumber = remember(day.dateString, day.dayNumber, persianCalendarEnabled, usePersianScript) {
                        if (persianCalendarEnabled) {
                            val num = PersianCalendarHelper.getPersianDayNumber(day.dateString)
                            if (usePersianScript) PersianCalendarHelper.toPersianDigits(num) else num
                        } else {
                            day.dayNumber
                        }
                    }
                    Text(
                        text = displayDayNumber,
                        color = if (isSelected) Color.White else if (isToday) LavenderAccent else DarkTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isToday) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(LavenderAccent, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgendaDoItem(
    doItem: Do,
    currentValue: Float,
    isCompleted: Boolean,
    onLogToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onLogSkip: () -> Unit,
    doneDoBehavior: String = "DEFAULT",
    onDetailClick: () -> Unit = {},
    selectedDate: String = "",
    completedDate: String = "",
    persianCalendarEnabled: Boolean = false,
    usePersianScript: Boolean = false
) {
    val isSkipped = currentValue == -1f
    val categoryInfo = CategoryRegistry.getCategoryInfo(doItem.category)
    val priorityColor = when (doItem.priority) {
        "1", "HIGH" -> Color(0xFFFF5252)
        "2", "MEDIUM" -> Color(0xFFFFAB40)
        "3", "LOW" -> Color(0xFF448AFF)
        "4" -> Color(0xFFAB47BC)
        else -> DarkTextSecondary
    }

    val isReCompleted = isCompleted && !doItem.isMeasurable
    val cardBg = if (isSkipped) {
        Color(0xFF232425).copy(alpha = 0.5f)
    } else if (isReCompleted) {
        Color(0xFF333537).copy(alpha = 0.6f)
    } else {
        DarkSurface
    }
    val cardBorderColor = if (isSkipped) {
        Color.White.copy(alpha = 0.15f)
    } else if (isCompleted) {
        LavenderAccent.copy(alpha = 0.3f)
    } else {
        GlassBorder
    }

    val dueBadgeInfo = remember(doItem.targetDate, isCompleted, completedDate, persianCalendarEnabled, usePersianScript) {
        if (doItem.type == "TASK" && !doItem.targetDate.isNullOrBlank() && !isCompleted) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val target = sdf.parse(doItem.targetDate)
                val realTodayStr = sdf.format(java.util.Date())
                val current = sdf.parse(realTodayStr)
                if (target != null && current != null) {
                    val cal1 = java.util.Calendar.getInstance().apply { time = target }
                    val cal2 = java.util.Calendar.getInstance().apply { time = current }
                    cal1.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal1.set(java.util.Calendar.MINUTE, 0)
                    cal1.set(java.util.Calendar.SECOND, 0)
                    cal1.set(java.util.Calendar.MILLISECOND, 0)
                    cal2.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal2.set(java.util.Calendar.MINUTE, 0)
                    cal2.set(java.util.Calendar.SECOND, 0)
                    cal2.set(java.util.Calendar.MILLISECOND, 0)
                    val diffMs = cal1.timeInMillis - cal2.timeInMillis
                    val diffDays = Math.round(diffMs.toDouble() / (1000 * 60 * 60 * 24)).toInt()
                    when {
                        diffDays < 0 -> {
                            val absDays = -diffDays
                            "⚠️ Overdue by $absDays day${if (absDays > 1) "s" else ""}" to Color(0xFFFF5252)
                        }
                        diffDays == 0 -> {
                            "⏰ Due Today" to Color(0xFFFFAB40)
                        }
                        diffDays == 1 -> {
                            "📅 Due Tomorrow" to Color(0xFF448AFF)
                        }
                        else -> {
                            "📅 Due in $diffDays days" to Color(0xFFAB47BC)
                        }
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else if (doItem.type == "TASK" && doItem.targetDate.isNullOrBlank() && !isCompleted) {
            "☁ Someday" to Color(0xFFB0BEC5)
        } else if (doItem.type == "TASK" && isCompleted) {
            val dateStr = (if (!completedDate.isNullOrBlank()) completedDate else doItem.targetDate) ?: ""
            val formattedDate = if (persianCalendarEnabled) {
                val slashDate = PersianCalendarHelper.formatToPersianSlash(dateStr)
                if (usePersianScript) PersianCalendarHelper.toPersianDigits(slashDate) else slashDate
            } else {
                dateStr
            }
            "✓ Done on $formattedDate" to Color(0xFF4CAF50)
        } else {
            null
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(24.dp)
            )
            .combinedClickable(
                onClick = { onDetailClick() },
                onLongClick = { onLogSkip() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category accent circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = if (isSkipped) 0.05f else 0.15f))
                    .border(1.dp, categoryInfo.color.copy(alpha = if (isSkipped) 0.1f else 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryInfo.icon,
                    contentDescription = doItem.category,
                    tint = if (isSkipped) categoryInfo.color.copy(alpha = 0.5f) else categoryInfo.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = doItem.name,
                        color = if (isSkipped) {
                            DarkTextSecondary.copy(alpha = 0.5f)
                        } else if (isCompleted) {
                            DarkTextSecondary
                        } else {
                            Color.White
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isSkipped) {
                            TextDecoration.None
                        } else if (isCompleted && (!doItem.isMeasurable || doneDoBehavior == "STRIKETHROUGH_BOTTOM")) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Priority Mini-Dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSkipped) priorityColor.copy(alpha = 0.4f) else priorityColor)
                    )
                    if (isSkipped) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SKIPPED",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (dueBadgeInfo != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .background(dueBadgeInfo.second.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(0.5.dp, dueBadgeInfo.second.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dueBadgeInfo.first,
                            color = dueBadgeInfo.second,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Type Tag
                    Text(
                        text = if (doItem.type == "RECURRING") "Re" else "Task",
                        color = if (isSkipped) {
                            (if (doItem.type == "RECURRING") LightBlueAccent else BrightAmber).copy(alpha = 0.5f)
                        } else {
                            if (doItem.type == "RECURRING") LightBlueAccent else BrightAmber
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        color = DarkTextSecondary.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                    // Multiple Category Badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        doItem.category.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { catName ->
                            val catInfo = CategoryRegistry.getCategoryInfo(catName)
                            Box(
                                modifier = Modifier
                                    .background(catInfo.color.copy(alpha = if (isSkipped) 0.05f else 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, catInfo.color.copy(alpha = if (isSkipped) 0.1f else 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = catName,
                                    color = if (isSkipped) catInfo.color.copy(alpha = 0.5f) else catInfo.color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = "•",
                        color = DarkTextSecondary.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                    if (doItem.priority == "1" || doItem.priority == "HIGH") {
                        Text(
                            text = "1",
                            color = if (isSkipped) HighPriorityText.copy(alpha = 0.5f) else HighPriorityText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(if (isSkipped) HighPriorityRed.copy(alpha = 0.4f) else HighPriorityRed, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    } else {
                        val displayPriority = when (doItem.priority) {
                            "2", "MEDIUM" -> "2"
                            "3", "LOW" -> "3"
                            "4" -> "4"
                            else -> doItem.priority
                        }
                        Text(
                            text = displayPriority,
                            color = if (isSkipped) priorityColor.copy(alpha = 0.5f) else priorityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val reminderConfig = remember(doItem.reminderTime) {
                        CustomReminderConfig.fromString(doItem.reminderTime)
                    }
                    if (reminderConfig.times.isNotEmpty()) {
                        Text(
                            text = "•",
                            color = DarkTextSecondary.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminder",
                            tint = (if (isSkipped) LavenderAccent.copy(alpha = 0.4f) else LavenderAccent.copy(alpha = 0.8f)),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = reminderConfig.getDisplayString(),
                            color = if (isSkipped) DarkTextSecondary.copy(alpha = 0.5f) else DarkTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (doItem.isMeasurable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isSkipped) {
                        Text(
                            text = "Day Skipped",
                            fontSize = 12.sp,
                            color = DarkTextSecondary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    } else {
                        val progressFraction = (currentValue / doItem.targetValue).coerceIn(0f, 1f)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = categoryInfo.color,
                                trackColor = DarkSurfaceVariant
                            )
                            Text(
                                text = "${currentValue.toInt()}/${doItem.targetValue.toInt()} ${doItem.unit}",
                                fontSize = 12.sp,
                                color = if (isCompleted) LavenderAccent else DarkTextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action section (Checkbox, Increment, or Management controls)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Progress Logging controls
                if (isSkipped) {
                    IconButton(
                        onClick = onLogSkip,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "-",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (doItem.isMeasurable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier
                                .size(30.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrement",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier
                                .size(30.dp)
                                .background(categoryInfo.color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increment",
                                tint = categoryInfo.color,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onLogToggle,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("do_checkbox_${doItem.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isCompleted) Color(0xFFB6EEA9) else DarkTextSecondary, CircleShape)
                                .background(if (isCompleted) Color(0xFFB6EEA9) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = Color(0xFF0F370E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- DO LIST SCREEN ----------------
@Composable
fun DoScreen(
    viewModel: DoViewModel,
    dos: List<Do>,
    logs: List<DoLog>,
    selectedCategory: String,
    searchQuery: String,
    onEditDo: (Do) -> Unit,
    onDetailClick: (Do) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("do_expanded_states", android.content.Context.MODE_PRIVATE) }
    var isResExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("res_expanded", true)) }
    var isTasksExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("tasks_expanded", true)) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search dos...", color = DarkTextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = DarkTextSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = DarkTextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = LavenderAccent,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .testTag("search_field"),
            singleLine = true
        )

        // Categories Horizontal Filters
        val filterCategories = listOf("All") + CategoryRegistry.categories.map { it.name }
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterCategories) { category ->
                val isSelected = category == selectedCategory
                val info = if (category != "All") CategoryRegistry.getCategoryInfo(category) else null
                
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedCategory(category) },
                    label = { Text(category) },
                    leadingIcon = {
                        info?.let {
                            Icon(
                                imageVector = it.icon,
                                contentDescription = category,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = info?.color ?: LavenderAccent,
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black,
                        containerColor = DarkSurface,
                        labelColor = DarkTextSecondary,
                        iconColor = DarkTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = GlassBorder,
                        selectedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.testTag("filter_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter the dos list
        val filteredDos = remember(dos, selectedCategory, searchQuery) {
            dos.filter { doItem ->
                val matchCategory = selectedCategory == "All" || doItem.category.split(",").map { it.trim().lowercase() }.contains(selectedCategory.lowercase())
                val matchSearch = searchQuery.isEmpty() || doItem.name.contains(searchQuery, ignoreCase = true)
                matchCategory && matchSearch
            }
        }

        if (filteredDos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Empty list",
                        tint = DarkTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = "No dos found",
                        color = DarkTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Create a new RE or task to track it offline.",
                        color = DarkTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Group by Type (Re vs Tasks)
            val recurringREs = filteredDos.filter { it.type == "RECURRING" }
            val taskDos = filteredDos.filter { it.type == "TASK" }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    com.example.ui.HistoryMatrixView(
                        viewModel = viewModel,
                        dos = filteredDos,
                        logs = logs,
                        modifier = Modifier.padding(bottom = 8.dp),
                        onDoClick = onDetailClick
                    )
                }

                if (recurringREs.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isResExpanded = !isResExpanded
                                    sharedPrefs.edit().putBoolean("res_expanded", isResExpanded).apply()
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recurring REs (Re)",
                                color = LightBlueAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = if (isResExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isResExpanded) "Collapse" else "Expand",
                                tint = LightBlueAccent
                            )
                        }
                    }
                    if (isResExpanded) {
                        items(recurringREs, key = { "re_${it.id}" }) { doItem ->
                            DoDetailCard(
                                doItem = doItem,
                                logs = logs,
                                onEdit = { onEditDo(doItem) },
                                onDelete = { viewModel.deleteDo(doItem.id) },
                                onDetailClick = { onDetailClick(doItem) }
                            )
                        }
                    }
                }

                if (taskDos.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isTasksExpanded = !isTasksExpanded
                                    sharedPrefs.edit().putBoolean("tasks_expanded", isTasksExpanded).apply()
                                }
                                .padding(top = 12.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "One-Time Tasks",
                                color = BrightAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Icon(
                                imageVector = if (isTasksExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isTasksExpanded) "Collapse" else "Expand",
                                tint = BrightAmber
                            )
                        }
                    }
                    if (isTasksExpanded) {
                        items(taskDos, key = { "task_${it.id}" }) { doItem ->
                            val taskLog = logs.find { it.doId == doItem.id }
                            val isTaskDone = taskLog != null && taskLog.value > 0f
                            
                            DoDetailCard(
                                doItem = doItem,
                                logs = logs,
                                isTaskDone = isTaskDone,
                                onEdit = { onEditDo(doItem) },
                                onDelete = { viewModel.deleteDo(doItem.id) },
                                onDetailClick = { onDetailClick(doItem) }
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun DoDetailCard(
    doItem: Do,
    logs: List<DoLog>,
    isTaskDone: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDetailClick: () -> Unit
) {
    val categoryInfo = CategoryRegistry.getCategoryInfo(doItem.category)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
            .clickable { onDetailClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(categoryInfo.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryInfo.icon,
                        contentDescription = null,
                        tint = categoryInfo.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = doItem.name,
                        color = if (isTaskDone) DarkTextSecondary else Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isTaskDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        doItem.category.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { catName ->
                            val catInfo = CategoryRegistry.getCategoryInfo(catName)
                            Box(
                                modifier = Modifier
                                    .background(catInfo.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(0.5.dp, catInfo.color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = catName,
                                    color = catInfo.color,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Edit/Delete actions
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DarkTextSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252).copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------------- MORE & SETTINGS SCREEN ----------------
@Composable
fun MoreScreen(
    viewModel: DoViewModel,
    dos: List<Do>,
    logs: List<DoLog>
) {
    var activeSubTab by remember { mutableStateOf("ANALYTICS") } // "ANALYTICS" or "SETTINGS"
    val doneDoBehavior by viewModel.doneDoBehavior.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Sub-tabs Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("ANALYTICS" to "Analytics", "SETTINGS" to "Settings").forEach { (tabKey, label) ->
                val isSelected = activeSubTab == tabKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) LavenderAccent else Color.Transparent)
                        .clickable { activeSubTab = tabKey }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.Black else DarkTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (activeSubTab == "ANALYTICS") {
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsScreen(dos = dos, logs = logs)
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                SettingsContent(
                    viewModel = viewModel,
                    doneDoBehavior = doneDoBehavior,
                    onBehaviorSelected = { viewModel.setDoneDoBehavior(it) },
                    dos = dos,
                    logs = logs
                )
            }
        }
    }
}

// Helper functions for CSV and JSON export
fun escapeJson(str: String?): String {
    if (str == null) return ""
    return str.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}

fun exportToJson(dos: List<Do>, logs: List<DoLog>): String {
    val sb = java.lang.StringBuilder()
    sb.append("{\n")
    sb.append("  \"exportedAt\": \"").append(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())).append("\",\n")
    
    sb.append("  \"dos\": [\n")
    dos.forEachIndexed { index, doItem ->
        sb.append("    {\n")
        sb.append("      \"id\": ").append(doItem.id).append(",\n")
        sb.append("      \"name\": \"").append(escapeJson(doItem.name)).append("\",\n")
        sb.append("      \"type\": \"").append(doItem.type).append("\",\n")
        sb.append("      \"isMeasurable\": ").append(doItem.isMeasurable).append(",\n")
        sb.append("      \"targetValue\": ").append(doItem.targetValue).append(",\n")
        sb.append("      \"unit\": \"").append(escapeJson(doItem.unit)).append("\",\n")
        sb.append("      \"priority\": \"").append(doItem.priority).append("\",\n")
        sb.append("      \"category\": \"").append(escapeJson(doItem.category)).append("\",\n")
        sb.append("      \"targetDate\": ").append(if (doItem.targetDate != null) "\"${doItem.targetDate}\"" else "null").append(",\n")
        sb.append("      \"targetTime\": ").append(if (doItem.targetTime != null) "\"${doItem.targetTime}\"" else "null").append(",\n")
        sb.append("      \"reminderTime\": ").append(if (doItem.reminderTime != null) "\"${escapeJson(doItem.reminderTime)}\"" else "null").append(",\n")
        sb.append("      \"purpose\": \"").append(escapeJson(doItem.purpose)).append("\",\n")
        sb.append("      \"benefit\": \"").append(escapeJson(doItem.benefit)).append("\",\n")
        sb.append("      \"cost\": \"").append(escapeJson(doItem.cost)).append("\"\n")
        sb.append("    }").append(if (index < dos.size - 1) ",\n" else "\n")
    }
    sb.append("  ],\n")
    
    sb.append("  \"logs\": [\n")
    logs.forEachIndexed { index, log ->
        sb.append("    {\n")
        sb.append("      \"doId\": ").append(log.doId).append(",\n")
        sb.append("      \"date\": \"").append(log.date).append("\",\n")
        sb.append("      \"value\": ").append(log.value).append(",\n")
        sb.append("      \"loggedAt\": ").append(log.loggedAt).append("\n")
        sb.append("    }").append(if (index < logs.size - 1) ",\n" else "\n")
    }
    sb.append("  ]\n")
    sb.append("}")
    return sb.toString()
}

fun escapeCsv(str: String?): String {
    if (str == null) return ""
    var escaped = str.replace("\"", "\"\"")
    if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
        escaped = "\"$escaped\""
    }
    return escaped
}

fun exportToCsv(dos: List<Do>, logs: List<DoLog>): String {
    val sb = java.lang.StringBuilder()
    sb.append("LogDate,DoName,Category,Type,Priority,LoggedValue,TargetValue,Unit,Purpose,Benefit,Cost,LoggedAtTimestamp\n")
    val doMap = dos.associateBy { it.id }
    logs.sortedByDescending { it.date }.forEach { log ->
        val doItem = doMap[log.doId]
        val doName = doItem?.name ?: "Unknown"
        val category = doItem?.category ?: "Unknown"
        val type = doItem?.type ?: ""
        val priority = doItem?.priority ?: ""
        val targetVal = doItem?.targetValue ?: 1.0f
        val unit = doItem?.unit ?: ""
        val purpose = doItem?.purpose ?: ""
        val benefit = doItem?.benefit ?: ""
        val cost = doItem?.cost ?: ""
        
        sb.append(escapeCsv(log.date)).append(",")
          .append(escapeCsv(doName)).append(",")
          .append(escapeCsv(category)).append(",")
          .append(escapeCsv(type)).append(",")
          .append(escapeCsv(priority)).append(",")
          .append(log.value).append(",")
          .append(targetVal).append(",")
          .append(escapeCsv(unit)).append(",")
          .append(escapeCsv(purpose)).append(",")
          .append(escapeCsv(benefit)).append(",")
          .append(escapeCsv(cost)).append(",")
          .append(log.loggedAt).append("\n")
    }
    return sb.toString()
}

@Composable
fun SettingsContent(
    viewModel: DoViewModel,
    doneDoBehavior: String,
    onBehaviorSelected: (String) -> Unit,
    dos: List<Do>,
    logs: List<DoLog>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("settings_expanded_states", android.content.Context.MODE_PRIVATE) }
    var isBehaviorExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("behavior_expanded", true)) }
    var isExportExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("export_expanded", true)) }
    var isCategoriesExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("categories_expanded", false)) }
    
    val isPersianEnabled by viewModel.persianCalendarEnabled.collectAsStateWithLifecycle()
    val usePersianScript by viewModel.usePersianScript.collectAsStateWithLifecycle()
    val persianTargetDateEnabled by viewModel.persianTargetDateEnabled.collectAsStateWithLifecycle()
    val accentColorPreset by viewModel.accentColorPreset.collectAsStateWithLifecycle()

    var isPersianExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("persian_expanded", false)) }
    var isAccentExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("accent_expanded", false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "App Settings",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Accent Color Selection Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            modifier = Modifier
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isAccentExpanded = !isAccentExpanded
                            sharedPrefs.edit().putBoolean("accent_expanded", isAccentExpanded).apply()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LavenderAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Accent Color",
                            tint = LavenderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Accent Color",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose the main color theme for the whole app",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isAccentExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Section",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isAccentExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        Text(
                            text = "Select a color scheme to immediately apply it across all screens, buttons, indicators, and details:",
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            com.example.ui.theme.AccentPresets.forEach { preset ->
                                val isSelected = accentColorPreset.equals(preset.name, ignoreCase = true)
                                val presetColor = Color(android.graphics.Color.parseColor(preset.accentHex))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) DeepPurpleContainer.copy(alpha = 0.3f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) LavenderAccent.copy(alpha = 0.6f) else GlassBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.setAccentColorPreset(preset.name) }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(presetColor)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.name,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, if (isSelected) LavenderAccent else DarkTextSecondary, CircleShape)
                                            .background(if (isSelected) LavenderAccent else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
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

        // Persian Calendar Jalali Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            modifier = Modifier
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isPersianExpanded = !isPersianExpanded
                            sharedPrefs.edit().putBoolean("persian_expanded", isPersianExpanded).apply()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LavenderAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Persian Calendar",
                            tint = LavenderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Persian Calendar (Jalali)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enable Shamsi/Jalali dates and customize format",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isPersianExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Section",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isPersianExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        // Toggle 1: Enable
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Shamsi/Jalali Calendar",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enable Shamsi/Jalali dates across the entire app",
                                    color = DarkTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isPersianEnabled,
                                onCheckedChange = { viewModel.setPersianCalendarEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LavenderAccent,
                                    checkedTrackColor = DeepPurpleContainer
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        // Toggle 2: Target Date Format (only active/meaningful if enabled)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isPersianEnabled) 1f else 0.5f)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use Persian Target Date Format",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Show target date inputs in Shamsi e.g. 1405-04-17",
                                    color = DarkTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = persianTargetDateEnabled,
                                enabled = isPersianEnabled,
                                onCheckedChange = { viewModel.setPersianTargetDateEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LavenderAccent,
                                    checkedTrackColor = DeepPurpleContainer
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        // Toggle 3: Script (only active / meaningful if enabled, but we show it clearly)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isPersianEnabled) 1f else 0.5f)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Use Persian Script & Digits",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Display in Persian script e.g. ۱۶ تیر instead of 16 Tir",
                                    color = DarkTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = usePersianScript,
                                enabled = isPersianEnabled,
                                onCheckedChange = { viewModel.setUsePersianScript(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LavenderAccent,
                                    checkedTrackColor = DeepPurpleContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            modifier = Modifier
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isBehaviorExpanded = !isBehaviorExpanded
                            sharedPrefs.edit().putBoolean("behavior_expanded", isBehaviorExpanded).apply()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LavenderAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Done Do Behavior",
                            tint = LavenderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "When a Do gets Done",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose what happens on the Agenda screen",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isBehaviorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Section",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isBehaviorExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        val options = listOf(
                            Triple(
                                "DEFAULT",
                                "Default Position",
                                "Keep item in its current position"
                            ),
                            Triple(
                                "STRIKETHROUGH_BOTTOM",
                                "Strikethrough & Bottom",
                                "Put a line on it and move it to the bottom"
                            ),
                            Triple(
                                "HIDE",
                                "Disappear Completely",
                                "Hide Done items from Agenda list"
                            )
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            options.forEach { (key, title, desc) ->
                                val isSelected = doneDoBehavior == key
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) DeepPurpleContainer.copy(alpha = 0.3f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) LavenderAccent.copy(alpha = 0.6f) else GlassBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onBehaviorSelected(key) }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, if (isSelected) LavenderAccent else DarkTextSecondary, CircleShape)
                                            .background(if (isSelected) LavenderAccent else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = desc,
                                            color = DarkTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overdue Items Placement Card
        val overduePosition by viewModel.overduePosition.collectAsStateWithLifecycle()
        var isOverdueExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("overdue_expanded", true)) }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            modifier = Modifier
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isOverdueExpanded = !isOverdueExpanded
                            sharedPrefs.edit().putBoolean("overdue_expanded", isOverdueExpanded).apply()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LavenderAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Overdue Items Position",
                            tint = LavenderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overdue Items Placement",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose where to show overdue items in today's agenda",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isOverdueExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Section",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isOverdueExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        val overdueOptions = listOf(
                            Triple(
                                "TOP",
                                "On Top of the List",
                                "Show overdue tasks on top of today's list"
                            ),
                            Triple(
                                "BOTTOM",
                                "On Bottom of the List",
                                "Show overdue tasks below all today's items"
                            ),
                            Triple(
                                "STANDARD",
                                "Interleaved (Standard Sorting)",
                                "Keep overdue tasks in normal sorted position"
                            )
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            overdueOptions.forEach { (key, title, desc) ->
                                val isSelected = overduePosition == key
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) DeepPurpleContainer.copy(alpha = 0.3f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) LavenderAccent.copy(alpha = 0.6f) else GlassBorder,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.setOverduePosition(key) }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, if (isSelected) LavenderAccent else DarkTextSecondary, CircleShape)
                                            .background(if (isSelected) LavenderAccent else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = desc,
                                            color = DarkTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Management card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            modifier = Modifier
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isCategoriesExpanded = !isCategoriesExpanded
                            sharedPrefs.edit().putBoolean("categories_expanded", isCategoriesExpanded).apply()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LavenderAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = LavenderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Category Management",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Add, remove, sort and resort categories",
                            color = DarkTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = if (isCategoriesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Section",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isCategoriesExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val categoriesList by viewModel.categories.collectAsStateWithLifecycle()

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        // 1. Categories List with Reordering & Deleting
                        Text(
                            text = "Current Categories",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoriesList.forEachIndexed { index, cat ->
                                val catInfo = cat.toCategoryInfo()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceVariant)
                                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Color bubble
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(catInfo.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catInfo.icon,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        Text(
                                            text = cat.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }

                                    // Sorting & Deleting controls
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Move Up button
                                        IconButton(
                                            onClick = { viewModel.moveCategoryUp(index, categoriesList) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Move Up",
                                                tint = if (index > 0) Color.White else DarkTextSecondary.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Move Down button
                                        IconButton(
                                            onClick = { viewModel.moveCategoryDown(index, categoriesList) },
                                            enabled = index < categoriesList.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Move Down",
                                                tint = if (index < categoriesList.size - 1) Color.White else DarkTextSecondary.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Delete button
                                        IconButton(
                                            onClick = { viewModel.deleteCategory(cat.id) },
                                            enabled = categoriesList.size > 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = if (categoriesList.size > 1) HighPriorityRed else DarkTextSecondary.copy(alpha = 0.3f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        // 2. Add New Category section
                        Text(
                            text = "Create Custom Category",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        var newCatName by remember { mutableStateOf("") }
                        
                        // Row of standard vibrant colors + Custom option
                        val presetColors = listOf(
                            "#A8C7FA" to "Blue",
                            "#D0BCFF" to "Lavender",
                            "#FFFFB74D" to "Orange",
                            "#E040FB" to "Neon Purple",
                            "#FFFF80AB" to "Pink",
                            "#CFD8DC" to "Slate",
                            "#81C784" to "Green",
                            "#E57373" to "Red",
                            "#4DD0E1" to "Cyan",
                            "#DCE775" to "Lime"
                        )
                        var selectedColorHex by remember { mutableStateOf(presetColors[0].first) }

                        // Row of standard beautiful icons
                        val presetIcons = listOf(
                            "Favorite" to Icons.Default.Favorite,
                            "Work" to Icons.Default.Work,
                            "Home" to Icons.Default.Home,
                            "Psychology" to Icons.Default.Psychology,
                            "Brush" to Icons.Default.Brush,
                            "School" to Icons.Default.School,
                            "ShoppingCart" to Icons.Default.ShoppingCart,
                            "MusicNote" to Icons.Default.MusicNote,
                            "Flight" to Icons.Default.Flight,
                            "Pets" to Icons.Default.Pets,
                            "Restaurant" to Icons.Default.Restaurant,
                            "DirectionsRun" to Icons.AutoMirrored.Filled.DirectionsRun,
                            "Spa" to Icons.Default.Spa,
                            "FitnessCenter" to Icons.Default.FitnessCenter,
                            "AttachMoney" to Icons.Default.AttachMoney,
                            "Code" to Icons.Default.Code,
                            "People" to Icons.Default.People,
                            "LocalCafe" to Icons.Default.LocalCafe,
                            "Book" to Icons.Default.Book,
                            "WaterDrop" to Icons.Default.WaterDrop,
                            "Lightbulb" to Icons.Default.Lightbulb,
                            "Event" to Icons.Default.Event,
                            "CheckCircle" to Icons.Default.CheckCircle,
                            "Build" to Icons.Default.Build,
                            "PhoneAndroid" to Icons.Default.PhoneAndroid,
                            "BarChart" to Icons.Default.BarChart
                        )
                        var selectedIconName by remember { mutableStateOf(presetIcons[0].first) }

                        OutlinedTextField(
                            value = newCatName,
                            onValueChange = { newCatName = it },
                            label = { Text("Category Name", color = DarkTextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = LavenderAccent,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Color Selector Title
                        Text(
                            text = "Select Color Accent",
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetColors) { (hex, name) ->
                                val color = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex }
                                )
                            }
                        }

                        // Icon Selector Title
                        Text(
                            text = "Select Icon",
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetIcons) { (iconName, iconVector) ->
                                val isSelected = selectedIconName == iconName
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) LavenderAccent else DarkSurfaceVariant)
                                        .border(1.dp, if (isSelected) Color.White else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { selectedIconName = iconName }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = iconName,
                                        tint = if (isSelected) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (newCatName.isNotBlank()) {
                                    viewModel.addCategory(
                                        name = newCatName.trim(),
                                        colorHex = selectedColorHex,
                                        iconName = selectedIconName
                                    )
                                    newCatName = ""
                                }
                            },
                            enabled = newCatName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LavenderAccent,
                                contentColor = Color.Black,
                                disabledContainerColor = DarkSurfaceVariant,
                                disabledContentColor = DarkTextSecondary.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Category", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Export card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isExportExpanded = !isExportExpanded
                            sharedPrefs.edit().putBoolean("export_expanded", isExportExpanded).apply()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LightBlueAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Data",
                            tint = LightBlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Data Portability & Export",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export your tracks, intention alignment, and streaks",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isExportExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Section",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isExportExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        Text(
                            text = "Backup your daily efforts, custom intentions, habits, target dates, and historical log completions. Choose a format below to share or save your data for deep personal analysis.",
                            color = DarkTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val csvData = exportToCsv(dos, logs)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Do Tracker CSV Data Export")
                                        putExtra(Intent.EXTRA_TEXT, csvData)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export CSV via"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LightBlueAccent, contentColor = Color.Black),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    val jsonData = exportToJson(dos, logs)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Do Tracker JSON Backup Export")
                                        putExtra(Intent.EXTRA_TEXT, jsonData)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export JSON via"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent, contentColor = Color.Black),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Export Mind Dumps & Notes Card
        val notesList by viewModel.notes.collectAsStateWithLifecycle()
        var isNotesExportExpanded by remember { mutableStateOf(sharedPrefs.getBoolean("notes_export_expanded", false)) }
        var mainNotesToDeleteAfterExport by remember { mutableStateOf<List<Note>>(emptyList()) }
        var showMainDeleteConfirmDialog by remember { mutableStateOf(false) }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = GlassBorder, shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isNotesExportExpanded = !isNotesExportExpanded
                            sharedPrefs.edit().putBoolean("notes_export_expanded", isNotesExportExpanded).apply()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LavenderAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Export Notes",
                            tint = LavenderAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Mind Dumps & Notes",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export your written thoughts in .md or .txt formats",
                            color = DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isNotesExportExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Section",
                        tint = DarkTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isNotesExportExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(GlassBorder)
                        )

                        if (notesList.isEmpty()) {
                            Text(
                                text = "You haven't dumped any thoughts yet! Go to the Notes tab to capture your thoughts.",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Text(
                                text = "Choose a note to export, or combine all notes into one export:",
                                color = DarkTextSecondary,
                                fontSize = 12.sp
                            )

                            // Export ALL notes combined
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "All Captured Notes",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Combines all ${notesList.size} dumps",
                                        color = DarkTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            val combinedTxt = notesList.joinToString("\n\n===\n\n") { note ->
                                                formatNoteContentForSharing(note, "txt", context)
                                            }
                                            NoteExporter.shareNoteAsFile(context, "All_Captured_Notes", combinedTxt, "txt")
                                            mainNotesToDeleteAfterExport = notesList
                                            showMainDeleteConfirmDialog = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = LightBlueAccent)
                                    ) {
                                        Text(".TXT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            val combinedMd = notesList.joinToString("\n\n---\n\n") { note ->
                                                formatNoteContentForSharing(note, "md", context)
                                            }
                                            NoteExporter.shareNoteAsFile(context, "All_Captured_Notes", combinedMd, "md")
                                            mainNotesToDeleteAfterExport = notesList
                                            showMainDeleteConfirmDialog = true
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = LavenderAccent)
                                    ) {
                                        Text(".MD", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // List individual notes
                            notesList.forEach { note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = note.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val txt = formatNoteContentForSharing(note, "txt", context)
                                                NoteExporter.shareNoteAsFile(context, note.title, txt, "txt")
                                                mainNotesToDeleteAfterExport = listOf(note)
                                                showMainDeleteConfirmDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Description, contentDescription = "TXT", tint = LightBlueAccent, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                val md = formatNoteContentForSharing(note, "md", context)
                                                NoteExporter.shareNoteAsFile(context, note.title, md, "md")
                                                mainNotesToDeleteAfterExport = listOf(note)
                                                showMainDeleteConfirmDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.MenuBook, contentDescription = "MD", tint = LavenderAccent, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Main Export Deletion Confirmation Dialog
        if (showMainDeleteConfirmDialog && mainNotesToDeleteAfterExport.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = {
                    showMainDeleteConfirmDialog = false
                    mainNotesToDeleteAfterExport = emptyList()
                },
                containerColor = Color(0xFF1E1F22),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
                title = {
                    Text(
                        text = "Delete Exported Notes?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Do you want to delete the exported notes really? Only the successfully exported note(s) will be deleted. This action cannot be undone.",
                        color = DarkTextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            mainNotesToDeleteAfterExport.forEach { note ->
                                viewModel.deleteNote(note.id)
                            }
                            showMainDeleteConfirmDialog = false
                            mainNotesToDeleteAfterExport = emptyList()
                        }
                    ) {
                        Text("Yes, Delete Them", color = HighPriorityRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showMainDeleteConfirmDialog = false
                            mainNotesToDeleteAfterExport = emptyList()
                        }
                    ) {
                        Text("No, Keep Them", color = Color.White)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ---------------- ANALYTICS SCREEN ----------------
@Composable
fun AnalyticsScreen(
    dos: List<Do>,
    logs: List<DoLog>
) {
    if (dos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No analytics data available yet. Start tracking to see metrics!",
                color = DarkTextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val totalDos = dos.size
        val totalLogs = logs.size
        val completedToday = remember(dos, logs) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            dos.count { doItem ->
                val log = logs.find { it.doId == doItem.id && it.date == todayStr }
                if (doItem.isMeasurable) {
                    (log?.value ?: 0f) >= doItem.targetValue
                } else {
                    (log?.value ?: 0f) >= 1.0f
                }
            }
        }

        val longestStreakOverall = remember(dos, logs) {
            dos.filter { it.type == "RECURRING" }
                .maxOfOrNull { StreakCalculator.calculateLongestStreak(logs, it.id) } ?: 0
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Dashboard Metrics",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Overview Grid Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            title = "Done Today",
                            value = "$completedToday / $totalDos",
                            subtitle = "Daily Actions Done",
                            color = LavenderAccent,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total Logs",
                            value = "$totalLogs",
                            subtitle = "Tracked Repetitions",
                            color = LightBlueAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(
                            title = "Longest Streak",
                            value = "$longestStreakOverall days",
                            subtitle = "Best Active RE",
                            color = BrightAmber,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total Dos",
                            value = "$totalDos",
                            subtitle = "Active Tasks & Re",
                            color = Color(0xFFFF4081),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Custom category breakdown chart (Pie/Donut on Canvas)
            item {
                CategoryDonutChartCard(dos = dos, logs = logs)
            }

            // Custom bar chart showing weekly trend
            item {
                WeeklyTrendBarChartCard(logs = logs)
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = modifier.border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DarkTextSecondary.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

@Composable
fun CategoryDonutChartCard(dos: List<Do>, logs: List<DoLog>) {
    val categoryCounts = remember(dos, logs) {
        val counts = mutableMapOf<String, Int>()
        logs.forEach { log ->
            val doItem = dos.find { it.id == log.doId }
            if (doItem != null) {
                counts[doItem.category] = (counts[doItem.category] ?: 0) + 1
            }
        }
        counts
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Log Distribution by Category",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (categoryCounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No logs recorded yet.", color = DarkTextSecondary, fontSize = 12.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Canvas
                    val totalLogs = categoryCounts.values.sum()
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            categoryCounts.forEach { (cat, count) ->
                                val sweepAngle = (count.toFloat() / totalLogs) * 360f
                                val color = CategoryRegistry.getCategoryInfo(cat).color
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 16.dp.toPx())
                                )
                                startAngle += sweepAngle
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalLogs",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Logs",
                                color = DarkTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // Legend Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryCounts.forEach { (category, count) ->
                            val info = CategoryRegistry.getCategoryInfo(category)
                            val percentage = ((count.toFloat() / totalLogs) * 100).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(info.color)
                                )
                                Text(
                                    text = "$category ($percentage%)",
                                    color = DarkTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
fun WeeklyTrendBarChartCard(logs: List<DoLog>) {
    val weeklyData = remember(logs) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val dataList = mutableListOf<Pair<String, Int>>() // Day Label to Count
        
        // Fetch last 7 days starting from 6 days ago up to today
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        
        for (i in 0..6) {
            val dateStr = sdf.format(cal.time)
            val dayLabel = dayLabelFormat.format(cal.time)
            val count = logs.count { it.date == dateStr && it.value > 0f }
            dataList.add(Pair(dayLabel, count))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        dataList
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Consistency Chain (CC)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            val maxCount = weeklyData.maxOfOrNull { it.second } ?: 0
            val chartHeight = 120.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEach { (day, count) ->
                    val barHeightFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (count > 0) {
                            Text(
                                text = "$count",
                                color = LavenderAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        // Custom Bar brush
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.85f * barHeightFraction + 0.05f) // minimum thin line
                                .width(18.dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(LavenderAccent, LavenderAccent.copy(alpha = 0.3f))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = day,
                            color = DarkTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ---------------- ADD / EDIT DIALOG ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDoDialog(
    viewModel: DoViewModel,
    doToEdit: Do?,
    defaultDateString: String = "",
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: String,
        isMeasurable: Boolean,
        targetValue: Float,
        unit: String,
        priority: String,
        category: String,
        targetDate: String?,
        targetTime: String?,
        reminderTime: String?,
        purpose: String,
        benefit: String,
        cost: String
    ) -> Unit
) {
    val isPersianEnabled by viewModel.persianCalendarEnabled.collectAsStateWithLifecycle()
    val persianTargetDateEnabled by viewModel.persianTargetDateEnabled.collectAsStateWithLifecycle()
    val isPersianTargetDateOn = isPersianEnabled && persianTargetDateEnabled

    var name by remember { mutableStateOf(doToEdit?.name ?: "") }
    var type by remember { mutableStateOf(doToEdit?.type ?: "TASK") }
    var isMeasurable by remember { mutableStateOf(doToEdit?.isMeasurable ?: false) }
    var targetValue by remember { mutableStateOf(doToEdit?.targetValue?.toString() ?: "1") }
    var unit by remember { mutableStateOf(doToEdit?.unit ?: "") }
    var priority by remember { mutableStateOf(doToEdit?.priority ?: "MEDIUM") }
    var selectedCategories by remember {
        mutableStateOf(
            doToEdit?.category?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: listOf("General")
        )
    }
    var purpose by remember {
        mutableStateOf(
            if (doToEdit != null) {
                val parts = listOfNotNull(
                    doToEdit.purpose.takeIf { it.isNotBlank() },
                    doToEdit.benefit.takeIf { it.isNotBlank() }?.let { "Benefit: $it" },
                    doToEdit.cost.takeIf { it.isNotBlank() }?.let { "Obstacle: $it" }
                ).filter { it.isNotEmpty() }
                if (parts.isNotEmpty()) parts.joinToString("\n") else ""
            } else ""
        )
    }
    var benefit by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    
    // Default task target date to selectedDate if creating new, or use existing
    val todaySdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = todaySdf.format(Date())
    val defaultDate = if (!defaultDateString.isNullOrBlank()) defaultDateString else todayStr
    val rawInitialDate = doToEdit?.targetDate ?: defaultDate
    val initDate = if (isPersianTargetDateOn) {
        com.example.ui.PersianCalendarHelper.formatGregorianToJalaliHyphen(rawInitialDate)
    } else {
        rawInitialDate
    }
    var targetDate by remember(initDate) { mutableStateOf(initDate) }
    var targetTime by remember { mutableStateOf(doToEdit?.targetTime ?: "") }

    // Rich Reminder Customization States
    val initialConfig = remember(doToEdit) {
        CustomReminderConfig.fromString(doToEdit?.reminderTime)
    }
    var hasReminder by remember { mutableStateOf(doToEdit?.reminderTime != null && initialConfig.times.isNotEmpty()) }
    var reminderTimes by remember { mutableStateOf(initialConfig.times) }
    var selectedDays by remember { mutableStateOf(initialConfig.days) }
    var selectedSound by remember { mutableStateOf(initialConfig.sound) }
    var selectedSnooze by remember { mutableStateOf(initialConfig.snooze) }
    var reminderNote by remember { mutableStateOf(initialConfig.note) }
    var intervalDaysText by remember { mutableStateOf(if (initialConfig.intervalDays > 0) initialConfig.intervalDays.toString() else "") }
    var enableRandomWhispers by remember { mutableStateOf(initialConfig.enableRandomWhispers) }

    var pickerHour by remember { mutableStateOf(8) }
    var pickerMinute by remember { mutableStateOf(0) }
    var pickerIsPm by remember { mutableStateOf(false) }

    var showError by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("add_edit_do_expanded_states", android.content.Context.MODE_PRIVATE) }
    var showPhilosophicalSection by remember { mutableStateOf(sharedPrefs.getBoolean("philosophical_expanded", purpose.isNotBlank() || benefit.isNotBlank() || cost.isNotBlank())) }
    var showRemindersSection by remember { mutableStateOf(sharedPrefs.getBoolean("reminders_expanded", hasReminder || intervalDaysText.isNotBlank())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (doToEdit != null) "Edit Do" else "Create New Do",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        containerColor = DarkBg,
        modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; showError = false },
                        label = { Text("What action/RE? (Do)") },
                        isError = showError && name.isBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LavenderAccent,
                            focusedLabelColor = LavenderAccent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedBorderColor = GlassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_name_input"),
                        singleLine = true
                    )
                }

                // Type Selection: Recurring vs Task (Custom Segmented Control)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Frequency Type", color = DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val frequencyOptions = listOf(
                                "RECURRING" to "Re (Recurring)",
                                "TASK" to "Task (One-time)"
                            )
                            frequencyOptions.forEach { (optionKey, optionLabel) ->
                                val isSelected = type == optionKey
                                val activeBg = if (optionKey == "RECURRING") LightBlueAccent else BrightAmber
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) activeBg else Color.Transparent)
                                        .clickable { type = optionKey }
                                        .padding(vertical = 10.dp)
                                        .testTag(if (optionKey == "RECURRING") "type_recurring_button" else "type_task_button")
                                ) {
                                    Text(
                                        text = optionLabel,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Schedule Date & Scheduled Time stacked (full-width for generous spacing and readability)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = targetDate,
                            onValueChange = { targetDate = it },
                            label = { Text(if (type == "TASK") "Target Date" else "Scheduled Date") },
                            placeholder = { Text("yyyy-MM-dd") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LavenderAccent,
                                focusedLabelColor = LavenderAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Quick Date Chips (Equal weights, no vertical compression!)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val tomorrowCal = Calendar.getInstance()
                            val tGregorian = todaySdf.format(tomorrowCal.time)
                            tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
                            val tmGregorian = todaySdf.format(tomorrowCal.time)

                            val tStr = if (isPersianTargetDateOn) {
                                com.example.ui.PersianCalendarHelper.formatGregorianToJalaliHyphen(tGregorian)
                            } else {
                                tGregorian
                            }
                            val tmStr = if (isPersianTargetDateOn) {
                                com.example.ui.PersianCalendarHelper.formatGregorianToJalaliHyphen(tmGregorian)
                            } else {
                                tmGregorian
                            }
                            
                            // Today chip
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (targetDate == tStr) LavenderAccent.copy(alpha = 0.2f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (targetDate == tStr) LavenderAccent else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { targetDate = tStr }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Today", color = if (targetDate == tStr) LavenderAccent else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            // Tomorrow chip
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .background(if (targetDate == tmStr) LavenderAccent.copy(alpha = 0.2f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (targetDate == tmStr) LavenderAccent else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { targetDate = tmStr }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tomorrow", color = if (targetDate == tmStr) LavenderAccent else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Someday chip
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .background(if (targetDate.isBlank()) LavenderAccent.copy(alpha = 0.2f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (targetDate.isBlank()) LavenderAccent else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { 
                                        targetDate = ""
                                        targetTime = ""
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Someday", color = if (targetDate.isBlank()) LavenderAccent else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = targetTime,
                            onValueChange = { targetTime = it },
                            label = { Text("Scheduled Time") },
                            placeholder = { Text("HH:mm") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LavenderAccent,
                                focusedLabelColor = LavenderAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val h24 = when {
                                            pickerIsPm && pickerHour < 12 -> pickerHour + 12
                                            !pickerIsPm && pickerHour == 12 -> 0
                                            pickerIsPm && pickerHour == 12 -> 12
                                            else -> pickerHour
                                        }
                                        targetTime = String.format(Locale.getDefault(), "%02d:%02d", h24, pickerMinute)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Use Clock Time",
                                        tint = LavenderAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Quick Time Chips (Equal weights, completely legible!)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val times = listOf("08:00" to "Morning", "12:00" to "Noon", "18:00" to "Evening")
                            times.forEach { (tVal, tLabel) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (targetTime == tVal) LavenderAccent.copy(alpha = 0.2f) else DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (targetTime == tVal) LavenderAccent else GlassBorder, RoundedCornerShape(8.dp))
                                        .clickable { targetTime = tVal }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = tLabel, color = if (targetTime == tVal) LavenderAccent else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            // Clear time
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { targetTime = "" }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Clear", color = DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Measurement Toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Measurable Action", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Log quantities (e.g. 8 glasses water)", color = DarkTextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isMeasurable,
                            onCheckedChange = { isMeasurable = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = LavenderAccent,
                                checkedTrackColor = DeepPurpleContainer
                            )
                        )
                    }
                }

                if (isMeasurable) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = targetValue,
                                onValueChange = { targetValue = it },
                                label = { Text("Target Goal") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LavenderAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = GlassBorder
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Unit (e.g., cups)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LavenderAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = GlassBorder
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                // Category Selection
                item {
                    Text("Category (Select one or more)", color = DarkTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        items(CategoryRegistry.categories) { cat ->
                            val isSelected = selectedCategories.contains(cat.name)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) cat.color else DarkSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable {
                                        val current = selectedCategories.toMutableList()
                                        if (current.contains(cat.name)) {
                                            if (current.size > 1) {
                                                current.remove(cat.name)
                                            }
                                        } else {
                                            current.add(cat.name)
                                        }
                                        selectedCategories = current
                                    }
                                    .testTag("category_select_${cat.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.Black else cat.color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = cat.name,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Priority Selector (Custom Segmented Round-Cornered Layout)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Priority Level", color = DarkTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val priorities = listOf("1" to "High", "2" to "Medium", "3" to "Low", "4" to "None")
                            priorities.forEach { (pr, prLabel) ->
                                val isSelected = priority == pr || (pr == "1" && priority == "HIGH") || (pr == "2" && priority == "MEDIUM") || (pr == "3" && priority == "LOW")
                                val prColor = when (pr) {
                                    "1" -> Color(0xFFFF5252)
                                    "2" -> Color(0xFFFFAB40)
                                    "3" -> Color(0xFF448AFF)
                                    "4" -> Color(0xFFAB47BC)
                                    else -> DarkTextSecondary
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) prColor.copy(alpha = 0.25f) else DarkSurfaceVariant)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) prColor else GlassBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { priority = pr }
                                        .padding(vertical = 10.dp)
                                        .testTag("priority_button_$pr")
                                ) {
                                    Text(
                                        text = prLabel,
                                        color = if (isSelected) prColor else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Collapsible: Details (Optional)
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { 
                                showPhilosophicalSection = !showPhilosophicalSection
                                sharedPrefs.edit().putBoolean("philosophical_expanded", showPhilosophicalSection).apply()
                            },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Details",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Why, benefit, cost, or personal motivation (Optional)",
                                        color = DarkTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showPhilosophicalSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showPhilosophicalSection) "Collapse" else "Expand",
                                tint = LavenderAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (showPhilosophicalSection) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(16.dp))
                                .border(1.dp, GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            OutlinedTextField(
                                value = purpose,
                                onValueChange = { purpose = it },
                                label = { Text("Details") },
                                placeholder = { 
                                    Text(
                                        "e.g.\n" +
                                        "- Why? To stay hydrated & maintain clear focus\n" +
                                        "- Benefit? Fewer headaches, healthy glowing skin\n" +
                                        "- Cost? Need to carry water container everywhere"
                                    ) 
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedLabelColor = LavenderAccent,
                                    unfocusedLabelColor = DarkTextSecondary,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8
                            )
                        }
                    }
                }

                // Collapsible: Advanced Reminders & Snooze
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { 
                                showRemindersSection = !showRemindersSection
                                sharedPrefs.edit().putBoolean("reminders_expanded", showRemindersSection).apply()
                            },
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Advanced Reminders, Snooze & Sounds",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Set auditory cues & snooze strategies (Optional)",
                                        color = DarkTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showRemindersSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (showRemindersSection) "Collapse" else "Expand",
                                tint = LavenderAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (showRemindersSection) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Reminders",
                                    tint = LavenderAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Enable Audio Reminders",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = hasReminder,
                                onCheckedChange = { hasReminder = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LavenderAccent,
                                    checkedTrackColor = DeepPurpleContainer
                                )
                            )
                        }
                    }
                }

                if (showRemindersSection && hasReminder) {
                    // Added Reminder Times Badge Row
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Reminder Times",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (reminderTimes.isEmpty()) {
                                Text(
                                    text = "No times added yet. Pick a time below and click 'Add'.",
                                    color = DarkTextSecondary.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(reminderTimes) { hhmm ->
                                        // Format for beautiful display, e.g. "08:30 AM"
                                        val formatted = remember(hhmm) {
                                            val parts = hhmm.split(":")
                                            if (parts.size == 2) {
                                                val h = parts[0].toIntOrNull() ?: 8
                                                val m = parts[1].toIntOrNull() ?: 0
                                                val ampm = if (h >= 12) "PM" else "AM"
                                                val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
                                                String.format(Locale.getDefault(), "%d:%02d %s", h12, m, ampm)
                                            } else hhmm
                                        }
                                        Card(
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = DeepPurpleContainer.copy(alpha = 0.6f)),
                                            modifier = Modifier
                                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                                                .clickable {
                                                    reminderTimes = reminderTimes.filter { it != hhmm }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = formatted,
                                                    color = LavenderAccent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Time",
                                                    tint = LavenderAccent,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Frosted Time Picker UI
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Create Reminder Time",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Hour Selector Column
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = { pickerHour = if (pickerHour == 12) 1 else pickerHour + 1 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up Hour", tint = LavenderAccent)
                                        }
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d", pickerHour),
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { pickerHour = if (pickerHour == 1) 12 else pickerHour - 1 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down Hour", tint = LavenderAccent)
                                        }
                                    }

                                    Text(
                                        text = ":",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    // Minute Selector Column
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconButton(
                                            onClick = { pickerMinute = (pickerMinute + 5) % 60 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up Minute", tint = LavenderAccent)
                                        }
                                        Text(
                                            text = String.format(Locale.getDefault(), "%02d", pickerMinute),
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = { pickerMinute = if (pickerMinute < 5) 55 else pickerMinute - 5 },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down Minute", tint = LavenderAccent)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // AM/PM Selector Segmented Tab
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkBg)
                                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = "AM",
                                            color = if (!pickerIsPm) Color.Black else Color.White,
                                            modifier = Modifier
                                                .clickable { pickerIsPm = false }
                                                .background(if (!pickerIsPm) LavenderAccent else Color.Transparent)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "PM",
                                            color = if (pickerIsPm) Color.Black else Color.White,
                                            modifier = Modifier
                                                .clickable { pickerIsPm = true }
                                                .background(if (pickerIsPm) LavenderAccent else Color.Transparent)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Add Time Button
                                    IconButton(
                                        onClick = {
                                            val h24 = when {
                                                pickerIsPm && pickerHour < 12 -> pickerHour + 12
                                                !pickerIsPm && pickerHour == 12 -> 0
                                                pickerIsPm && pickerHour == 12 -> 12
                                                else -> pickerHour
                                            }
                                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", h24, pickerMinute)
                                            if (!reminderTimes.contains(timeStr)) {
                                                reminderTimes = reminderTimes + timeStr
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(LavenderAccent)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Time", tint = DeepPurpleContainer)
                                    }
                                }
                            }
                        }
                    }

                    // Custom Day Selector
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Reminder Days",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                                for (i in 1..7) {
                                    val isSelected = selectedDays.contains(i)
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) LavenderAccent else DarkSurfaceVariant)
                                            .border(1.dp, if (isSelected) Color.Transparent else GlassBorder, CircleShape)
                                            .clickable {
                                                selectedDays = if (isSelected) {
                                                    selectedDays.filter { it != i }
                                                } else {
                                                    selectedDays + i
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = dayLabels[i - 1],
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = { selectedDays = emptyList() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = LavenderAccent),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Everyday", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { selectedDays = listOf(1, 2, 3, 4, 5) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = LavenderAccent),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Weekdays", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(
                                    onClick = { selectedDays = listOf(6, 7) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = LavenderAccent),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Weekends", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Sound Vibe Selector
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Reminder Sound Theme",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val soundVibes = listOf("Default Accent", "Zen Gong", "Energetic Beats", "Nature Stream", "Silent Haptic")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                items(soundVibes) { vibe ->
                                    val isSelected = selectedSound == vibe
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) LavenderAccent else DarkSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { selectedSound = vibe }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.Black else LavenderAccent,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = vibe,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Snooze Preference
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Snooze Strategy",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val snoozeOptions = listOf("No Snooze", "Persistent", "Relentless", "Gentle")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                snoozeOptions.forEach { opt ->
                                    val isSelected = selectedSnooze == opt
                                    Button(
                                        onClick = { selectedSnooze = opt },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) LavenderAccent else DarkSurfaceVariant,
                                            contentColor = if (isSelected) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Text(opt, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Interval Days input (e.g. Every 15 days)
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Interval Schedule (e.g. Every X days)",
                                color = DarkTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = intervalDaysText,
                                onValueChange = { intervalDaysText = it.filter { char -> char.isDigit() } },
                                label = { Text("Every X Days (0 or empty = daily)") },
                                placeholder = { Text("e.g. 15") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LavenderAccent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = GlassBorder
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Enable Random Whispers Switch
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable Random Whispers", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Generate periodic audio cues of your whisper note", color = DarkTextSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = enableRandomWhispers,
                                onCheckedChange = { enableRandomWhispers = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LavenderAccent,
                                    checkedTrackColor = DeepPurpleContainer
                                )
                            )
                        }
                    }

                    // Customized Whisper Message / Note
                    item {
                        OutlinedTextField(
                            value = reminderNote,
                            onValueChange = { reminderNote = it },
                            label = { Text("Custom Whisper Message") },
                            placeholder = { Text("e.g. Time to take a breath!") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LavenderAccent,
                                focusedLabelColor = LavenderAccent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                unfocusedBorderColor = GlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        val parsedVal = targetValue.toFloatOrNull() ?: 1f
                        val rawDate = targetDate.ifBlank { null }
                        val dateToSave = if (isPersianTargetDateOn && !rawDate.isNullOrBlank()) {
                            com.example.ui.PersianCalendarHelper.formatJalaliToGregorianHyphen(rawDate)
                        } else {
                            rawDate
                        }
                        onSave(
                            name,
                            type,
                            isMeasurable,
                            parsedVal,
                            unit,
                            priority,
                            selectedCategories.joinToString(","),
                            dateToSave,
                            targetTime.ifBlank { null },
                            if ((hasReminder && reminderTimes.isNotEmpty()) || (intervalDaysText.isNotBlank() && (intervalDaysText.toIntOrNull() ?: 0) > 0)) {
                                CustomReminderConfig(
                                    times = reminderTimes,
                                    days = selectedDays,
                                    sound = selectedSound,
                                    snooze = selectedSnooze,
                                    note = reminderNote,
                                    intervalDays = intervalDaysText.toIntOrNull() ?: 0,
                                    enableRandomWhispers = enableRandomWhispers
                                ).toSerializedString()
                            } else null,
                            purpose,
                            benefit,
                            cost
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LavenderAccent, contentColor = DeepPurpleContainer),
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DarkTextSecondary)
            }
        }
    )
}
