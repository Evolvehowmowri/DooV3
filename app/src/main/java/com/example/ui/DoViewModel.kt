package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ReminderManager
import com.example.data.Category
import com.example.data.Do
import com.example.data.DoDatabase
import com.example.data.DoLog
import com.example.data.DoRepository
import com.example.data.Note
import com.example.data.DoNothingLog
import com.example.data.MeasurementTracker
import com.example.data.MeasurementLog
import com.example.data.AffirmationCategory
import com.example.data.AffirmationItem
import com.example.data.AffirmationLog
import com.example.data.NotificationActionItem
import com.example.data.MomentumMaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDay(
    val dateString: String,
    val dayName: String,
    val dayNumber: String
)

class DoViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("do_settings", android.content.Context.MODE_PRIVATE)
    private val repository: DoRepository

    init {
        // Load and apply the saved accent color preset immediately
        val savedPreset = prefs.getString("accent_color_preset", "Purple") ?: "Purple"
        com.example.ui.theme.applyAccentPreset(savedPreset)

        val database = DoDatabase.getDatabase(application)
        val dao = database.doDao()
        repository = DoRepository(dao)

        viewModelScope.launch(Dispatchers.IO) {
            if (repository.getCategoryCount() == 0) {
                repository.insertCategory(Category(name = "Health", colorHex = "#A8C7FA", iconName = "Favorite", orderIndex = 0))
                repository.insertCategory(Category(name = "Work", colorHex = "#D0BCFF", iconName = "Work", orderIndex = 1))
                repository.insertCategory(Category(name = "Personal", colorHex = "#FFFFB74D", iconName = "Home", orderIndex = 2))
                repository.insertCategory(Category(name = "Mind", colorHex = "#E040FB", iconName = "Psychology", orderIndex = 3))
                repository.insertCategory(Category(name = "Creative", colorHex = "#FFFF80AB", iconName = "Brush", orderIndex = 4))
                repository.insertCategory(Category(name = "General", colorHex = "#CFD8DC", iconName = "Star", orderIndex = 5))
            }

            if (dao.getDoCount() == 0) {
                val today = getTodayDateString()
                dao.insertDo(Do(name = "Drink Water", type = "RECURRING", isMeasurable = true, targetValue = 8f, unit = "glasses", priority = "1", category = "Health"))
                dao.insertDo(Do(name = "Morning Meditate", type = "RECURRING", isMeasurable = false, priority = "2", category = "Mind"))
                dao.insertDo(Do(name = "Read 10 Pages", type = "RECURRING", isMeasurable = true, targetValue = 10f, unit = "pages", priority = "3", category = "Creative"))
                dao.insertDo(Do(name = "Finish Project Setup", type = "TASK", isMeasurable = false, priority = "1", category = "Work", targetDate = today))
            }

            if (repository.getAffirmationCategoryCount() == 0) {
                val cat1Id = repository.insertAffirmationCategory(AffirmationCategory(name = "Gratitude & Peace", isDefault = true))
                repository.insertAffirmationItems(listOf(
                    AffirmationItem(categoryId = cat1Id, text = "I am grateful for this moment."),
                    AffirmationItem(categoryId = cat1Id, text = "My mind is calm, my heart is at peace."),
                    AffirmationItem(categoryId = cat1Id, text = "I choose joy and let go of worry."),
                    AffirmationItem(categoryId = cat1Id, text = "Every breath I take fills me with peace."),
                    AffirmationItem(categoryId = cat1Id, text = "I am surrounded by love and support.")
                ))

                val cat2Id = repository.insertAffirmationCategory(AffirmationCategory(name = "Self-Confidence", isDefault = true))
                repository.insertAffirmationItems(listOf(
                    AffirmationItem(categoryId = cat2Id, text = "I am capable, strong, and resilient."),
                    AffirmationItem(categoryId = cat2Id, text = "I believe in myself and my abilities."),
                    AffirmationItem(categoryId = cat2Id, text = "I am worthy of all the good things in my life."),
                    AffirmationItem(categoryId = cat2Id, text = "My voice matters, and I speak with confidence."),
                    AffirmationItem(categoryId = cat2Id, text = "I grow stronger and wiser every day.")
                ))

                val cat3Id = repository.insertAffirmationCategory(AffirmationCategory(name = "Abundance & Success", isDefault = true))
                repository.insertAffirmationItems(listOf(
                    AffirmationItem(categoryId = cat3Id, text = "I attract abundance and prosperity."),
                    AffirmationItem(categoryId = cat3Id, text = "Success flows easily and naturally to me."),
                    AffirmationItem(categoryId = cat3Id, text = "I am open to new opportunities and experiences."),
                    AffirmationItem(categoryId = cat3Id, text = "My work is meaningful and highly rewarding."),
                    AffirmationItem(categoryId = cat3Id, text = "I create my own wealth and destiny.")
                ))
            }

            if (repository.getMomentumMakersCount() == 0) {
                repository.insertMomentumMakers(listOf(
                    MomentumMaker(title = "Wipe down kitchen counters", category = "Clean", durationMinutes = 2),
                    MomentumMaker(title = "Take out the trash", category = "Clean", durationMinutes = 3),
                    MomentumMaker(title = "Empty or load the dishwasher", category = "Clean", durationMinutes = 5),
                    MomentumMaker(title = "Put away 5 items of clutter in your room", category = "Clean", durationMinutes = 2),
                    MomentumMaker(title = "Water the house plants", category = "Clean", durationMinutes = 3),
                    
                    MomentumMaker(title = "Do 3 slow, deep conscious breaths", category = "Mind", durationMinutes = 1),
                    MomentumMaker(title = "Write down 3 things you are grateful for right now", category = "Mind", durationMinutes = 2),
                    MomentumMaker(title = "Brain dump your current worries on paper", category = "Mind", durationMinutes = 5),
                    MomentumMaker(title = "Tidy up your computer or phone desktop", category = "Mind", durationMinutes = 5),
                    MomentumMaker(title = "Drink a full glass of cold water", category = "Mind", durationMinutes = 1),
                    
                    MomentumMaker(title = "Do 10 jumping jacks or bodyweight squats", category = "Body", durationMinutes = 2),
                    MomentumMaker(title = "Do a quick shoulder, neck and back stretch", category = "Body", durationMinutes = 2),
                    MomentumMaker(title = "Walk around the block or steps for fresh air", category = "Body", durationMinutes = 10),
                    MomentumMaker(title = "Hold a plank for 1 minute", category = "Body", durationMinutes = 1),
                    MomentumMaker(title = "Do a quick 5-minute full body stretching routine", category = "Body", durationMinutes = 5),
                    
                    MomentumMaker(title = "Reply to one outstanding email or text message", category = "Admin", durationMinutes = 3),
                    MomentumMaker(title = "Unsubscribe from one junk email newsletter list", category = "Admin", durationMinutes = 1),
                    MomentumMaker(title = "Sort through the mail stack on the counter", category = "Admin", durationMinutes = 5),
                    MomentumMaker(title = "Plan and write down your top 3 priorities for today", category = "Admin", durationMinutes = 5),
                    
                    MomentumMaker(title = "Send a quick appreciation text to a friend", category = "Social", durationMinutes = 2),
                    MomentumMaker(title = "Call a family member or loved one to say hi", category = "Social", durationMinutes = 10),
                    MomentumMaker(title = "Share a funny meme or a helpful link with a colleague", category = "Social", durationMinutes = 2)
                ))
            }
        }
    }

    val dos: StateFlow<List<Do>> = repository.allDos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<DoLog>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notes: StateFlow<List<Note>> = repository.allNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val doNothingLogs: StateFlow<List<DoNothingLog>> = repository.allDoNothingLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addDoNothingLog(targetMinutes: Int, actualSeconds: Int, categoryName: String, sentencesCount: Int, sentencesLogged: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDoNothingLog(
                DoNothingLog(
                    targetMinutes = targetMinutes,
                    actualSeconds = actualSeconds,
                    categoryName = categoryName,
                    sentencesCount = sentencesCount,
                    sentencesLogged = sentencesLogged
                )
            )
        }
    }

    fun clearDoNothingLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllDoNothingLogs()
        }
    }

    // Measurement Tracker state & actions
    val measurementTrackers: StateFlow<List<MeasurementTracker>> = repository.allMeasurementTrackers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getLogsForTracker(trackerId: Long): kotlinx.coroutines.flow.Flow<List<MeasurementLog>> {
        return repository.getAllLogsForTrackerFlow(trackerId)
    }

    fun addMeasurementTracker(
        name: String,
        unit: String,
        initialValue: Double,
        targetValue: Double,
        milestonesStr: String,
        trackingInterval: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMeasurementTracker(
                MeasurementTracker(
                    name = name,
                    unit = unit,
                    initialValue = initialValue,
                    targetValue = targetValue,
                    milestonesStr = milestonesStr,
                    trackingInterval = trackingInterval
                )
            )
        }
    }

    fun updateMeasurementTracker(tracker: MeasurementTracker) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMeasurementTracker(tracker)
        }
    }

    fun deleteMeasurementTracker(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMeasurementTracker(id)
        }
    }

    fun addMeasurementLog(trackerId: Long, value: Double, timestamp: Long, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMeasurementLog(
                MeasurementLog(
                    trackerId = trackerId,
                    value = value,
                    timestamp = timestamp,
                    notes = notes
                )
            )
        }
    }

    fun updateMeasurementLog(log: MeasurementLog) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMeasurementLog(log)
        }
    }

    fun deleteMeasurementLog(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMeasurementLog(id)
        }
    }

    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _doneDoBehavior = MutableStateFlow(prefs.getString("done_do_behavior", "DEFAULT") ?: "DEFAULT")
    val doneDoBehavior: StateFlow<String> = _doneDoBehavior.asStateFlow()

    fun setDoneDoBehavior(behavior: String) {
        _doneDoBehavior.value = behavior
        prefs.edit().putString("done_do_behavior", behavior).apply()
    }

    private val _persianCalendarEnabled = MutableStateFlow(prefs.getBoolean("persian_calendar_enabled", false))
    val persianCalendarEnabled: StateFlow<Boolean> = _persianCalendarEnabled.asStateFlow()

    fun setPersianCalendarEnabled(enabled: Boolean) {
        _persianCalendarEnabled.value = enabled
        prefs.edit().putBoolean("persian_calendar_enabled", enabled).apply()
    }

    private val _usePersianScript = MutableStateFlow(prefs.getBoolean("use_persian_script", false))
    val usePersianScript: StateFlow<Boolean> = _usePersianScript.asStateFlow()

    fun setUsePersianScript(enabled: Boolean) {
        _usePersianScript.value = enabled
        prefs.edit().putBoolean("use_persian_script", enabled).apply()
    }

    private val _persianTargetDateEnabled = MutableStateFlow(prefs.getBoolean("persian_target_date_enabled", false))
    val persianTargetDateEnabled: StateFlow<Boolean> = _persianTargetDateEnabled.asStateFlow()

    fun setPersianTargetDateEnabled(enabled: Boolean) {
        _persianTargetDateEnabled.value = enabled
        prefs.edit().putBoolean("persian_target_date_enabled", enabled).apply()
    }

    private val _accentColorPreset = MutableStateFlow(prefs.getString("accent_color_preset", "Purple") ?: "Purple")
    val accentColorPreset: StateFlow<String> = _accentColorPreset.asStateFlow()

    fun setAccentColorPreset(preset: String) {
        _accentColorPreset.value = preset
        prefs.edit().putString("accent_color_preset", preset).apply()
        com.example.ui.theme.applyAccentPreset(preset)
    }

    private val _selectedCategory = MutableStateFlow(prefs.getString("selected_category", "All") ?: "All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow(prefs.getString("type_filter", "ALL") ?: "ALL")
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow(prefs.getString("priority_filter", "ALL") ?: "ALL")
    val priorityFilter: StateFlow<String> = _priorityFilter.asStateFlow()

    private val _completionFilter = MutableStateFlow(prefs.getString("completion_filter", "ALL") ?: "ALL")
    val completionFilter: StateFlow<String> = _completionFilter.asStateFlow()

    private val _sortBy = MutableStateFlow(prefs.getString("sort_by", "PRIORITY") ?: "PRIORITY")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _overduePosition = MutableStateFlow(prefs.getString("overdue_position", "TOP") ?: "TOP") // "TOP", "BOTTOM", "STANDARD"
    val overduePosition: StateFlow<String> = _overduePosition.asStateFlow()

    // Sliding calendar of 15 days around today (7 days before, today, 7 days after)
    private val _calendarDays = MutableStateFlow(getCalendarDaysList())
    val calendarDays: StateFlow<List<CalendarDay>> = _calendarDays.asStateFlow()

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun refreshTodayAndCalendarDays() {
        _calendarDays.value = getCalendarDaysList()
        val today = getTodayDateString()
        if (_selectedDate.value != today) {
            _selectedDate.value = today
        }
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
        prefs.edit().putString("selected_category", category).apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(value: String) {
        _typeFilter.value = value
        prefs.edit().putString("type_filter", value).apply()
    }

    fun setPriorityFilter(value: String) {
        _priorityFilter.value = value
        prefs.edit().putString("priority_filter", value).apply()
    }

    fun setCompletionFilter(value: String) {
        _completionFilter.value = value
        prefs.edit().putString("completion_filter", value).apply()
    }

    fun setSortBy(value: String) {
        _sortBy.value = value
        prefs.edit().putString("sort_by", value).apply()
    }

    fun setOverduePosition(value: String) {
        _overduePosition.value = value
        prefs.edit().putString("overdue_position", value).apply()
    }

    private fun getCalendarDaysList(): List<CalendarDay> {
        val list = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val numFormat = SimpleDateFormat("d", Locale.getDefault())
        for (i in 0..14) {
            list.add(
                CalendarDay(
                    dateString = dateFormat.format(cal.time),
                    dayName = dayFormat.format(cal.time),
                    dayNumber = numFormat.format(cal.time)
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    // UI actions
    fun addDo(
        name: String,
        type: String, // "TASK" or "RECURRING"
        isMeasurable: Boolean,
        targetValue: Float,
        unit: String,
        priority: String, // "HIGH", "MEDIUM", "LOW"
        category: String,
        targetDate: String? = null,
        targetTime: String? = null,
        reminderTime: String? = null,
        purpose: String = "",
        benefit: String = "",
        cost: String = ""
    ) {
        viewModelScope.launch {
            val insertedId = repository.insertDo(
                Do(
                    name = name,
                    type = type,
                    isMeasurable = isMeasurable,
                    targetValue = targetValue,
                    unit = unit,
                    priority = priority,
                    category = category,
                    targetDate = targetDate,
                    targetTime = targetTime,
                    reminderTime = reminderTime,
                    purpose = purpose,
                    benefit = benefit,
                    cost = cost
                )
            )
            val newDo = Do(
                id = insertedId.toInt(),
                name = name,
                type = type,
                isMeasurable = isMeasurable,
                targetValue = targetValue,
                unit = unit,
                priority = priority,
                category = category,
                targetDate = targetDate,
                targetTime = targetTime,
                reminderTime = reminderTime,
                purpose = purpose,
                benefit = benefit,
                cost = cost
            )
            ReminderManager.scheduleAlarmsForDo(getApplication(), newDo)
        }
    }

    fun updateDo(doItem: Do) {
        viewModelScope.launch {
            repository.updateDo(doItem)
            ReminderManager.scheduleAlarmsForDo(getApplication(), doItem)
        }
    }

    fun archiveDo(id: Int) {
        viewModelScope.launch {
            ReminderManager.cancelAlarmsForDoId(getApplication(), id)
            repository.archiveDo(id)
        }
    }

    fun deleteDo(id: Int) {
        viewModelScope.launch {
            ReminderManager.cancelAlarmsForDoId(getApplication(), id)
            repository.deleteDo(id)
        }
    }

    fun logDoValue(doId: Int, date: String, value: Float) {
        viewModelScope.launch {
            repository.logDo(doId, date, value)
        }
    }

    // Category Operations
    fun addCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val count = repository.getCategoryCount()
            repository.insertCategory(Category(name = name, colorHex = colorHex, iconName = iconName, orderIndex = count))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    fun moveCategoryUp(index: Int, list: List<Category>) {
        if (index <= 0 || index >= list.size) return
        viewModelScope.launch {
            val current = list[index]
            val previous = list[index - 1]
            repository.updateCategory(current.copy(orderIndex = previous.orderIndex))
            repository.updateCategory(previous.copy(orderIndex = current.orderIndex))
        }
    }

    fun moveCategoryDown(index: Int, list: List<Category>) {
        if (index < 0 || index >= list.size - 1) return
        viewModelScope.launch {
            val current = list[index]
            val next = list[index + 1]
            repository.updateCategory(current.copy(orderIndex = next.orderIndex))
            repository.updateCategory(next.copy(orderIndex = current.orderIndex))
        }
    }

    // Note operations
    fun addNote(title: String, content: String, category: String = "General", subCategory: String = "", hashtags: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertNote(Note(title = title, content = content, category = category, subCategory = subCategory, hashtags = hashtags))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNote(note)
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(id)
        }
    }

    // Momentum Maker Operations
    val momentumMakers: StateFlow<List<MomentumMaker>> = repository.allMomentumMakers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addMomentumMaker(title: String, category: String, durationMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMomentumMaker(MomentumMaker(title = title, category = category, durationMinutes = durationMinutes))
        }
    }

    fun updateMomentumMaker(item: MomentumMaker) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMomentumMaker(item)
        }
    }

    fun deleteMomentumMaker(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMomentumMaker(id)
        }
    }

    fun resetMomentumMakers() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllMomentumMakers()
            repository.insertMomentumMakers(listOf(
                MomentumMaker(title = "Wipe down kitchen counters", category = "Clean", durationMinutes = 2),
                MomentumMaker(title = "Take out the trash", category = "Clean", durationMinutes = 3),
                MomentumMaker(title = "Empty or load the dishwasher", category = "Clean", durationMinutes = 5),
                MomentumMaker(title = "Put away 5 items of clutter in your room", category = "Clean", durationMinutes = 2),
                MomentumMaker(title = "Water the house plants", category = "Clean", durationMinutes = 3),
                MomentumMaker(title = "Do 3 slow, deep conscious breaths", category = "Mind", durationMinutes = 1),
                MomentumMaker(title = "Write down 3 things you are grateful for right now", category = "Mind", durationMinutes = 2),
                MomentumMaker(title = "Brain dump your current worries on paper", category = "Mind", durationMinutes = 5),
                MomentumMaker(title = "Tidy up your computer or phone desktop", category = "Mind", durationMinutes = 5),
                MomentumMaker(title = "Drink a full glass of cold water", category = "Mind", durationMinutes = 1),
                MomentumMaker(title = "Do 10 jumping jacks or bodyweight squats", category = "Body", durationMinutes = 2),
                MomentumMaker(title = "Do a quick shoulder, neck and back stretch", category = "Body", durationMinutes = 2),
                MomentumMaker(title = "Walk around the block or steps for fresh air", category = "Body", durationMinutes = 10),
                MomentumMaker(title = "Hold a plank for 1 minute", category = "Body", durationMinutes = 1),
                MomentumMaker(title = "Do a quick 5-minute full body stretching routine", category = "Body", durationMinutes = 5),
                MomentumMaker(title = "Reply to one outstanding email or text message", category = "Admin", durationMinutes = 3),
                MomentumMaker(title = "Unsubscribe from one junk email newsletter list", category = "Admin", durationMinutes = 1),
                MomentumMaker(title = "Sort through the mail stack on the counter", category = "Admin", durationMinutes = 5),
                MomentumMaker(title = "Plan and write down your top 3 priorities for today", category = "Admin", durationMinutes = 5),
                MomentumMaker(title = "Send a quick appreciation text to a friend", category = "Social", durationMinutes = 2),
                MomentumMaker(title = "Call a family member or loved one to say hi", category = "Social", durationMinutes = 10),
                MomentumMaker(title = "Share a funny meme or a helpful link with a colleague", category = "Social", durationMinutes = 2)
            ))
        }
    }

    // Affirmations flows
    val affirmationCategories: StateFlow<List<AffirmationCategory>> = repository.allAffirmationCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val affirmationLogs: StateFlow<List<AffirmationLog>> = repository.allAffirmationLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getAffirmationsForCategoryFlow(categoryId: Long): kotlinx.coroutines.flow.Flow<List<AffirmationItem>> {
        return repository.getAffirmationsForCategoryFlow(categoryId)
    }

    fun addAffirmationCategoryWithItems(name: String, batchText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val categoryId = repository.insertAffirmationCategory(AffirmationCategory(name = name))
            val items = batchText.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { AffirmationItem(categoryId = categoryId, text = it) }
            if (items.isNotEmpty()) {
                repository.insertAffirmationItems(items)
            }
        }
    }

    fun addAffirmationItemsToCategory(categoryId: Long, batchText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = batchText.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { AffirmationItem(categoryId = categoryId, text = it) }
            if (items.isNotEmpty()) {
                repository.insertAffirmationItems(items)
            }
        }
    }

    fun deleteAffirmationCategory(categoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAffirmationsForCategory(categoryId)
            repository.deleteAffirmationCategory(categoryId)
        }
    }

    fun deleteAffirmationItem(itemId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAffirmationItem(itemId)
        }
    }

    fun logAffirmationRepetition(categoryId: Long, categoryName: String, text: String, count: Int, durationSecs: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAffirmationLog(
                AffirmationLog(
                    categoryId = categoryId,
                    categoryName = categoryName,
                    affirmationText = text,
                    tapCount = count,
                    durationSeconds = durationSecs
                )
            )
        }
    }

    fun deleteAffirmationLog(logId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAffirmationLog(logId)
        }
    }

    fun clearAllAffirmationLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllAffirmationLogs()
        }
    }

    // --- SELF-CARE NOTIFICATION SYSTEM ENG ---

    val allMeasurementLogs: StateFlow<List<MeasurementLog>> = repository.allMeasurementLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _notificationStatesChanged = MutableStateFlow(0)

    fun snoozeNotification(id: String, durationHours: Int) {
        val until = System.currentTimeMillis() + durationHours * 3600 * 1000L
        prefs.edit().putLong("snoozed_until_$id", until).apply()
        _notificationStatesChanged.value += 1
    }

    fun ignoreNotification(id: String) {
        val today = getTodayDateString()
        prefs.edit().putString("ignored_date_$id", today).apply()
        _notificationStatesChanged.value += 1
    }

    fun resetNotificationStates() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("snoozed_until_") || it.startsWith("ignored_date_") }.forEach {
            editor.remove(it)
        }
        editor.apply()
        _notificationStatesChanged.value += 1
    }

    val pendingNotifications: StateFlow<List<NotificationActionItem>> = combine(
        measurementTrackers,
        allMeasurementLogs,
        affirmationLogs,
        doNothingLogs,
        _notificationStatesChanged
    ) { trackers, logs, affLogs, zenLogs, _ ->
        val list = mutableListOf<NotificationActionItem>()
        val now = System.currentTimeMillis()
        val todayStr = getTodayDateString()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        // 1. Measurement Trackers
        for (tracker in trackers) {
            val trackerId = tracker.id
            val notifId = "MEASUREMENT_$trackerId"

            val snoozedUntil = prefs.getLong("snoozed_until_$notifId", 0L)
            val ignoredDate = prefs.getString("ignored_date_$notifId", "")
            if (now < snoozedUntil || ignoredDate == todayStr) {
                continue
            }

            val trackerLogs = logs.filter { it.trackerId == trackerId }
            val latestLog = trackerLogs.maxByOrNull { it.timestamp }

            var isDue = false
            when (tracker.trackingInterval) {
                "Daily" -> {
                    isDue = latestLog == null || latestLog.timestamp < startOfToday
                }
                "Weekly" -> {
                    isDue = latestLog == null || latestLog.timestamp < (now - 7 * 24 * 3600 * 1000L)
                }
                "Bi-weekly" -> {
                    isDue = latestLog == null || latestLog.timestamp < (now - 14 * 24 * 3600 * 1000L)
                }
                "Monthly" -> {
                    isDue = latestLog == null || latestLog.timestamp < (now - 30 * 24 * 3600 * 1000L)
                }
                "Ad-hoc" -> {
                    isDue = latestLog == null
                }
            }

            if (isDue) {
                list.add(
                    NotificationActionItem(
                        id = notifId,
                        title = tracker.name,
                        description = "It's time to log a new reading for your ${tracker.name} (${tracker.unit}).",
                        type = "MEASUREMENT",
                        targetScreen = "MEASUREMENT_TRACKER",
                        trackerId = trackerId,
                        priority = 1
                    )
                )
            }
        }

        // 2. Affirmations Practice
        val affNotifId = "AFFIRMATION_PRACTICE"
        val affSnoozedUntil = prefs.getLong("snoozed_until_$affNotifId", 0L)
        val affIgnoredDate = prefs.getString("ignored_date_$affNotifId", "")
        if (now >= affSnoozedUntil && affIgnoredDate != todayStr) {
            val hasPracticeToday = affLogs.any { it.timestamp >= startOfToday }
            if (!hasPracticeToday) {
                list.add(
                    NotificationActionItem(
                        id = affNotifId,
                        title = "Affirmation Practice",
                        description = "Recite your positive affirmations today to cultivate a mindful and calm mindset.",
                        type = "AFFIRMATION",
                        targetScreen = "AFFIRMATION",
                        priority = 2
                    )
                )
            }
        }

        // 3. Do Nothing Zen
        val zenNotifId = "DO_NOTHING_ZEN"
        val zenSnoozedUntil = prefs.getLong("snoozed_until_$zenNotifId", 0L)
        val zenIgnoredDate = prefs.getString("ignored_date_$zenNotifId", "")
        if (now >= zenSnoozedUntil && zenIgnoredDate != todayStr) {
            val hasZenToday = zenLogs.any { it.timestamp >= startOfToday }
            if (!hasZenToday) {
                list.add(
                    NotificationActionItem(
                        id = zenNotifId,
                        title = "Zen Moment",
                        description = "Spend 2 minutes doing absolutely nothing to clear your mind and reduce stress.",
                        type = "DO_NOTHING",
                        targetScreen = "DO_NOTHING",
                        priority = 3
                    )
                )
            }
        }

        list.sortBy { it.priority }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

class DoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
