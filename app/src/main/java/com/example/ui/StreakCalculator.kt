package com.example.ui

import com.example.data.DoLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StreakCalculator {
    fun calculateStreak(logs: List<DoLog>, doId: Int): Int {
        val completedDates = logs.filter { it.doId == doId && it.value > 0f }
            .map { it.date }
            .toSet()
        val skippedDates = logs.filter { it.doId == doId && it.value == -1f }
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
        val completedDates = logs.filter { it.doId == doId && it.value > 0f }
            .map { it.date }
            .distinct()
            .sorted()
        
        if (completedDates.isEmpty()) return 0

        val skippedDates = logs.filter { it.doId == doId && it.value == -1f }
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
}
