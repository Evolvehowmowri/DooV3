package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DoDao {
    @Query("SELECT COUNT(*) FROM dos")
    suspend fun getDoCount(): Int

    @Query("SELECT * FROM dos WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllDosFlow(): Flow<List<Do>>

    @Query("SELECT * FROM dos WHERE id = :id")
    suspend fun getDoById(id: Int): Do?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDo(doItem: Do): Long

    @Update
    suspend fun updateDo(doItem: Do)

    @Query("UPDATE dos SET isArchived = 1 WHERE id = :id")
    suspend fun archiveDo(id: Int)

    @Query("DELETE FROM dos WHERE id = :id")
    suspend fun deleteDo(id: Int)

    // Log Queries
    @Query("SELECT * FROM do_logs")
    fun getAllLogsFlow(): Flow<List<DoLog>>

    @Query("SELECT * FROM do_logs WHERE date = :date")
    fun getLogsByDateFlow(date: String): Flow<List<DoLog>>

    @Query("SELECT * FROM do_logs WHERE doId = :doId")
    fun getLogsForDoFlow(doId: Int): Flow<List<DoLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: DoLog)

    @Query("DELETE FROM do_logs WHERE doId = :doId AND date = :date")
    suspend fun deleteLog(doId: Int, date: String)

    // Category Queries
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Int)

    // Note Queries
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotesFlow(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Int)

    // DoNothing Log Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoNothingLog(log: DoNothingLog): Long

    @Query("SELECT * FROM do_nothing_logs ORDER BY timestamp DESC")
    fun getAllDoNothingLogsFlow(): Flow<List<DoNothingLog>>

    @Query("DELETE FROM do_nothing_logs")
    suspend fun clearAllDoNothingLogs()

    // Measurement Tracker Queries
    @Query("SELECT * FROM measurement_trackers ORDER BY name ASC")
    fun getAllMeasurementTrackersFlow(): Flow<List<MeasurementTracker>>

    @Query("SELECT * FROM measurement_trackers WHERE id = :id")
    suspend fun getMeasurementTrackerById(id: Long): MeasurementTracker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurementTracker(tracker: MeasurementTracker): Long

    @Update
    suspend fun updateMeasurementTracker(tracker: MeasurementTracker)

    @Query("DELETE FROM measurement_trackers WHERE id = :id")
    suspend fun deleteMeasurementTracker(id: Long)

    // Measurement Log Queries
    @Query("SELECT * FROM measurement_logs ORDER BY timestamp DESC")
    fun getAllMeasurementLogsFlow(): Flow<List<MeasurementLog>>

    @Query("SELECT * FROM measurement_logs WHERE trackerId = :trackerId ORDER BY timestamp ASC")
    fun getAllLogsForTrackerFlow(trackerId: Long): Flow<List<MeasurementLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurementLog(log: MeasurementLog): Long

    @Update
    suspend fun updateMeasurementLog(log: MeasurementLog)

    @Query("DELETE FROM measurement_logs WHERE id = :id")
    suspend fun deleteMeasurementLog(id: Long)

    // Affirmation Queries
    @Query("SELECT COUNT(*) FROM affirmation_categories")
    suspend fun getAffirmationCategoryCount(): Int

    @Query("SELECT * FROM affirmation_categories ORDER BY isDefault DESC, name ASC")
    fun getAllAffirmationCategoriesFlow(): Flow<List<AffirmationCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAffirmationCategory(category: AffirmationCategory): Long

    @Query("DELETE FROM affirmation_categories WHERE id = :id")
    suspend fun deleteAffirmationCategory(id: Long)

    @Query("SELECT * FROM affirmation_items WHERE categoryId = :categoryId ORDER BY id ASC")
    fun getAffirmationsForCategoryFlow(categoryId: Long): Flow<List<AffirmationItem>>

    @Query("SELECT * FROM affirmation_items WHERE categoryId = :categoryId ORDER BY id ASC")
    suspend fun getAffirmationsForCategory(categoryId: Long): List<AffirmationItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAffirmationItem(item: AffirmationItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAffirmationItems(items: List<AffirmationItem>)

    @Query("DELETE FROM affirmation_items WHERE id = :id")
    suspend fun deleteAffirmationItem(id: Long)

    @Query("DELETE FROM affirmation_items WHERE categoryId = :categoryId")
    suspend fun deleteAffirmationsForCategory(categoryId: Long)

    @Query("SELECT * FROM affirmation_logs ORDER BY timestamp DESC")
    fun getAllAffirmationLogsFlow(): Flow<List<AffirmationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAffirmationLog(log: AffirmationLog): Long

    @Query("DELETE FROM affirmation_logs WHERE id = :id")
    suspend fun deleteAffirmationLog(id: Long)

    @Query("DELETE FROM affirmation_logs")
    suspend fun clearAllAffirmationLogs()

    // Momentum Maker Queries
    @Query("SELECT * FROM momentum_makers ORDER BY createdAt DESC")
    fun getAllMomentumMakersFlow(): Flow<List<MomentumMaker>>

    @Query("SELECT COUNT(*) FROM momentum_makers")
    suspend fun getMomentumMakersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMomentumMaker(item: MomentumMaker): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMomentumMakers(items: List<MomentumMaker>)

    @Update
    suspend fun updateMomentumMaker(item: MomentumMaker)

    @Query("DELETE FROM momentum_makers WHERE id = :id")
    suspend fun deleteMomentumMaker(id: Long)

    @Query("DELETE FROM momentum_makers")
    suspend fun clearAllMomentumMakers()
}
