package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.Do
import com.example.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReminderManager {
    private const val TAG = "ReminderManager"
    const val CHANNEL_ID = "do_reminders_channel"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Habit & Task Reminders"
            val descriptionText = "Notifications for your tasks, habits, and recurring doings"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleAlarmsForDo(context: Context, doItem: Do) {
        // Cancel any existing alarms for this do item
        cancelAlarmsForDoId(context, doItem.id)

        val reminderTimeStr = doItem.reminderTime ?: return
        val config = CustomReminderConfig.fromString(reminderTimeStr)
        if (config.times.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        config.times.forEachIndexed { index, timeStr ->
            val timeParts = timeStr.split(":")
            if (timeParts.size != 2) return@forEachIndexed
            val hour = timeParts[0].toIntOrNull() ?: return@forEachIndexed
            val minute = timeParts[1].toIntOrNull() ?: return@forEachIndexed

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the scheduled time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Check if specific days are enabled
            if (config.days.isNotEmpty()) {
                // Find next day of week that is enabled
                var matches = false
                for (i in 0..7) {
                    // Calendar days: Sunday=1, Monday=2, ..., Saturday=7
                    // Config days: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
                    val calDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val configDayOfWeek = when (calDayOfWeek) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        Calendar.SUNDAY -> 7
                        else -> 1
                    }
                    if (config.days.contains(configDayOfWeek) && cal.timeInMillis > System.currentTimeMillis()) {
                        matches = true
                        break
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (!matches) return@forEachIndexed
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("do_id", doItem.id)
                putExtra("do_name", doItem.name)
                putExtra("reminder_note", config.note)
                putExtra("reminder_sound", config.sound)
                putExtra("reminder_time_index", index)
            }

            val requestCode = doItem.id * 100 + index
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            cal.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled exact alarm for ${doItem.name} at ${cal.time} (ReqCode: $requestCode)")
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            cal.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Scheduled inexact alarm for ${doItem.name} at ${cal.time} (ReqCode: $requestCode)")
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for ${doItem.name} at ${cal.time} (ReqCode: $requestCode)")
                }
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "SecurityException: Scheduled inexact alarm for ${doItem.name} at ${cal.time}")
            }
        }
    }

    fun cancelAlarmsForDoId(context: Context, doId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        // Since we can have multiple alarm times per Do, we cancel indexes from 0 to 15
        for (index in 0..15) {
            val requestCode = doId * 100 + index
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled alarm for doId: $doId, index: $index (ReqCode: $requestCode)")
            }
        }
    }

    fun showNotification(context: Context, doId: Int, title: String, message: String, soundName: String) {
        createNotificationChannel(context)

        // Intent to launch MainActivity on click
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            doId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Select ringtone
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message.ifBlank { "Time for your scheduled activity!" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(doId, builder.build())
        Log.d(TAG, "Notification posted for doId: $doId")
    }
}
