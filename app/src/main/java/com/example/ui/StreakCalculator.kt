package com.example.ui

import com.example.data.DoLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakCalculator {
    fun calculateStreak(logs: List<DoLog>, doId: Int): Int {
        val habitLogs = logs.filter { it.doId == doId }
        val completedDates = habitLogs.filter { it.value > 0f }
            .map { it.date }
            .toSet()
        val skippedDates = habitLogs.filter { it.value == -1f }
            .map { it.date }
            .toSet()
        
        if (completedDates.isEmpty()) return 0

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        var streak = 0
        
        // Start from today and go backwards
        var dateStr = sdf.format(cal.time)
        
        // If today is neither completed nor skipped, check if logged yesterday to keep streak alive
        if (!completedDates.contains(dateStr) && !skippedDates.contains(dateStr)) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            dateStr = sdf.format(cal.time)
        }
        
        while (true) {
            if (completedDates.contains(dateStr)) {
                streak++
            } else if (skippedDates.contains(dateStr)) {
                // Skip days do not break the streak and do not count toward it.
                // Just keep traversing backward.
            } else {
                break
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
            dateStr = sdf.format(cal.time)
        }
        
        return streak
    }

    fun calculateLongestStreak(logs: List<DoLog>, doId: Int): Int {
        val habitLogs = logs.filter { it.doId == doId }
        val completedDates = habitLogs.filter { it.value > 0f }
            .map { it.date }
            .distinct()
            .sorted()
        
        if (completedDates.isEmpty()) return 0

        val skippedDates = habitLogs.filter { it.value == -1f }
            .map { it.date }
            .toSet()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var maxStreak = 0
        var currentStreak = 0
        var prevCal: Calendar? = null

        for (dateStr in completedDates) {
            val date = try { sdf.parse(dateStr) } catch (e: Exception) { null } ?: continue
            val cal = Calendar.getInstance()
            cal.time = date

            if (prevCal == null) {
                currentStreak = 1
            } else {
                var isStreakConnected = true
                val checkCal = Calendar.getInstance()
                checkCal.time = prevCal.time
                checkCal.add(Calendar.DAY_OF_YEAR, 1)
                
                var iterations = 0
                while (checkCal.before(cal) && iterations < 3650) {
                    iterations++
                    val checkStr = sdf.format(checkCal.time)
                    if (!skippedDates.contains(checkStr) && !completedDates.contains(checkStr)) {
                        isStreakConnected = false
                        break
                    }
                    checkCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                if (isStreakConnected) {
                    currentStreak++
                } else {
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                    currentStreak = 1
                }
            }
            prevCal = cal
        }

        return maxOf(maxStreak, currentStreak)
    }

    fun calculateHabitStrength(
        logs: List<DoLog>,
        doId: Int,
        isMeasurable: Boolean,
        targetValue: Float,
        upToDateStr: String
    ): Float {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = try { sdf.parse(upToDateStr) } catch (e: Exception) { Date() }
        
        val cal = Calendar.getInstance()
        cal.time = targetDate
        cal.add(Calendar.DAY_OF_YEAR, -45) // Go back 45 days to start simulation
        
        var score = 0f
        var consecutiveMisses = 0
        var consecutiveCompletes = 0
        
        val habitLogsMap = logs.filter { it.doId == doId }.associateBy { it.date }
        
        for (i in 0..45) {
            val currentDateStr = sdf.format(cal.time)
            val log = habitLogsMap[currentDateStr]
            
            if (log == null) {
                consecutiveCompletes = 0
                consecutiveMisses++
                val decay = if (consecutiveMisses > 1) 4f else 2f
                score = maxOf(0f, score - decay)
            } else if (log.value == -1f) {
                consecutiveMisses = 0
            } else if (log.value == 0f) {
                consecutiveCompletes = 0
                consecutiveMisses++
                val penalty = if (consecutiveMisses > 1) 12f else 6f
                score = maxOf(0f, score - penalty)
            } else {
                consecutiveMisses = 0
                consecutiveCompletes++
                
                val ratio = if (isMeasurable) {
                    if (targetValue > 0f) log.value / targetValue else 0f
                } else {
                    1f
                }
                val clampedRatio = minOf(1f, maxOf(0f, ratio))
                val baseReward = 8f * clampedRatio
                val streakBonus = minOf(4f, consecutiveCompletes * 0.4f)
                
                score = minOf(100f, score + baseReward + streakBonus)
            }
            
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return score
    }
}
