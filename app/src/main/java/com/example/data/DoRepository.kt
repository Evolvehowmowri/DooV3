package com.example.data

import kotlinx.coroutines.flow.Flow

class DoRepository(private val doDao: DoDao) {
    val allDos: Flow<List<Do>> = doDao.getAllDosFlow()
    val allLogs: Flow<List<DoLog>> = doDao.getAllLogsFlow()
    val allCategories: Flow<List<Category>> = doDao.getAllCategoriesFlow()

    suspend fun insertDo(doItem: Do): Long = doDao.insertDo(doItem)
    suspend fun updateDo(doItem: Do) = doDao.updateDo(doItem)
    suspend fun archiveDo(id: Int) = doDao.archiveDo(id)
    suspend fun deleteDo(id: Int) = doDao.deleteDo(id)

    suspend fun getCategoryCount(): Int = doDao.getCategoryCount()
    suspend fun insertCategory(category: Category) = doDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = doDao.updateCategory(category)
    suspend fun deleteCategory(id: Int) = doDao.deleteCategory(id)

    fun getLogsByDate(date: String): Flow<List<DoLog>> = doDao.getLogsByDateFlow(date)
    fun getLogsForDo(doId: Int): Flow<List<DoLog>> = doDao.getLogsForDoFlow(doId)

    suspend fun logDo(doId: Int, date: String, value: Float) {
        if (value == 0f) {
            doDao.deleteLog(doId, date)
        } else {
            doDao.insertOrUpdateLog(DoLog(doId = doId, date = date, value = value))
        }
    }

    // Note operations
    val allNotes: Flow<List<Note>> = doDao.getAllNotesFlow()
    suspend fun insertNote(note: Note): Long = doDao.insertNote(note)
    suspend fun updateNote(note: Note) = doDao.updateNote(note)
    suspend fun deleteNote(id: Int) = doDao.deleteNote(id)

    // DoNothing operations
    val allDoNothingLogs: Flow<List<DoNothingLog>> = doDao.getAllDoNothingLogsFlow()
    suspend fun insertDoNothingLog(log: DoNothingLog): Long = doDao.insertDoNothingLog(log)
    suspend fun clearAllDoNothingLogs() = doDao.clearAllDoNothingLogs()

    // Measurement Tracker Operations
    val allMeasurementTrackers: Flow<List<MeasurementTracker>> = doDao.getAllMeasurementTrackersFlow()
    val allMeasurementLogs: Flow<List<MeasurementLog>> = doDao.getAllMeasurementLogsFlow()
    
    suspend fun getMeasurementTrackerById(id: Long): MeasurementTracker? = doDao.getMeasurementTrackerById(id)
    suspend fun insertMeasurementTracker(tracker: MeasurementTracker): Long = doDao.insertMeasurementTracker(tracker)
    suspend fun updateMeasurementTracker(tracker: MeasurementTracker) = doDao.updateMeasurementTracker(tracker)
    suspend fun deleteMeasurementTracker(id: Long) = doDao.deleteMeasurementTracker(id)

    fun getAllLogsForTrackerFlow(trackerId: Long): Flow<List<MeasurementLog>> = doDao.getAllLogsForTrackerFlow(trackerId)
    suspend fun insertMeasurementLog(log: MeasurementLog): Long = doDao.insertMeasurementLog(log)
    suspend fun updateMeasurementLog(log: MeasurementLog) = doDao.updateMeasurementLog(log)
    suspend fun deleteMeasurementLog(id: Long) = doDao.deleteMeasurementLog(id)

    // Affirmation operations
    val allAffirmationCategories: Flow<List<AffirmationCategory>> = doDao.getAllAffirmationCategoriesFlow()
    val allAffirmationLogs: Flow<List<AffirmationLog>> = doDao.getAllAffirmationLogsFlow()

    suspend fun getAffirmationCategoryCount(): Int = doDao.getAffirmationCategoryCount()

    suspend fun insertAffirmationCategory(category: AffirmationCategory): Long = doDao.insertAffirmationCategory(category)
    suspend fun deleteAffirmationCategory(id: Long) = doDao.deleteAffirmationCategory(id)

    fun getAffirmationsForCategoryFlow(categoryId: Long): Flow<List<AffirmationItem>> = doDao.getAffirmationsForCategoryFlow(categoryId)
    suspend fun getAffirmationsForCategory(categoryId: Long): List<AffirmationItem> = doDao.getAffirmationsForCategory(categoryId)

    suspend fun insertAffirmationItem(item: AffirmationItem): Long = doDao.insertAffirmationItem(item)
    suspend fun insertAffirmationItems(items: List<AffirmationItem>) = doDao.insertAffirmationItems(items)
    suspend fun deleteAffirmationItem(id: Long) = doDao.deleteAffirmationItem(id)
    suspend fun deleteAffirmationsForCategory(categoryId: Long) = doDao.deleteAffirmationsForCategory(categoryId)

    suspend fun insertAffirmationLog(log: AffirmationLog): Long = doDao.insertAffirmationLog(log)
    suspend fun deleteAffirmationLog(id: Long) = doDao.deleteAffirmationLog(id)
    suspend fun clearAllAffirmationLogs() = doDao.clearAllAffirmationLogs()

    // Momentum Maker Operations
    val allMomentumMakers: Flow<List<MomentumMaker>> = doDao.getAllMomentumMakersFlow()
    suspend fun getMomentumMakersCount(): Int = doDao.getMomentumMakersCount()
    suspend fun insertMomentumMaker(item: MomentumMaker): Long = doDao.insertMomentumMaker(item)
    suspend fun insertMomentumMakers(items: List<MomentumMaker>) = doDao.insertMomentumMakers(items)
    suspend fun updateMomentumMaker(item: MomentumMaker) = doDao.updateMomentumMaker(item)
    suspend fun deleteMomentumMaker(id: Long) = doDao.deleteMomentumMaker(id)
    suspend fun clearAllMomentumMakers() = doDao.clearAllMomentumMakers()
}
