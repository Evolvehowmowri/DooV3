package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Do::class, DoLog::class, Category::class, Note::class, DoNothingLog::class, MeasurementTracker::class, MeasurementLog::class, AffirmationCategory::class, AffirmationItem::class, AffirmationLog::class, MomentumMaker::class], version = 11, exportSchema = false)
abstract class DoDatabase : RoomDatabase() {
    abstract fun doDao(): DoDao

    companion object {
        @Volatile
        private var INSTANCE: DoDatabase? = null

        fun getDatabase(context: Context): DoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoDatabase::class.java,
                    "do_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
