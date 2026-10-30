package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

object AlarmSoundPlayer {
    private const val TAG = "AlarmSoundPlayer"
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // Static app context cache
    private var appContext: Context? = null

    // Active alarm state
    var activeDoId: Int? = null
    var activeDoName: String = ""
    var activeCycle: Int = 1
    var maxCycles: Int = 3
    var ringDurationMs: Long = 60000L
    var autoSnoozeMs: Long = 300000L
    var isRinging: Boolean = false
    var activeNote: String = ""

    // Callback to update UI in MainActivity
    var onStateChanged: (() -> Unit)? = null

    private val autoSnoozeRunnable = Runnable {
        Log.d(TAG, "Auto-snooze timer triggered after ring duration")
        autoSnooze()
    }

    fun startAlarm(context: Context, doId: Int, doName: String, config: CustomReminderConfig, cycle: Int) {
        val applicationCtx = context.applicationContext
        appContext = applicationCtx
        
        stopSoundOnly()
        
        activeDoId = doId
        activeDoName = doName
        activeCycle = cycle
        maxCycles = config.maxAlarmCycles
        ringDurationMs = config.ringDurationMinutes * 60 * 1000L
        autoSnoozeMs = config.autoSnoozeMinutes * 60 * 1000L
        activeNote = config.note
        isRinging = true
        
        Log.d(TAG, "Starting alarm for doId: $doId, name: $doName, cycle: $cycle/$maxCycles")

        // Play alarm sound
        try {
            var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            
            ringtone = RingtoneManager.getRingtone(applicationCtx, alarmUri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ringtone", e)
        }

        // Post alarm active notification
        showAlarmNotification(applicationCtx, doId, doName, "⏰ Ringing! (Cycle $cycle/$maxCycles) Tap to open & control.", config.note)

        // Schedule auto snooze after ring duration
        handler.removeCallbacks(autoSnoozeRunnable)
        handler.postDelayed(autoSnoozeRunnable, ringDurationMs)

        onStateChanged?.invoke()
    }

    fun stopAlarmAndDismiss(context: Context) {
        val applicationCtx = context.applicationContext
        Log.d(TAG, "Dismissing alarm for doId: $activeDoId")
        stopSoundOnly()
        
        activeDoId?.let { id ->
            cancelPendingAutoSnooze(applicationCtx, id)
            // Cancel notification
            val notificationManager = applicationCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(id)
        }
        
        isRinging = false
        activeDoId = null
        onStateChanged?.invoke()
    }

    fun stopAlarmAndSnooze(context: Context) {
        val applicationCtx = context.applicationContext
        Log.d(TAG, "Manual snooze requested for doId: $activeDoId")
        stopSoundOnly()
        
        val id = activeDoId
        val name = activeDoName
        val note = activeNote
        val nextCycle = activeCycle + 1
        
        if (id != null) {
            scheduleSnoozeAlarm(applicationCtx, id, name, note, nextCycle)
            
            // Show snooze notification
            showAlarmNotification(applicationCtx, id, name, "💤 Snoozed. Will ring again in ${autoSnoozeMs / 60000} min (Cycle $nextCycle/$maxCycles)", note)
        }
        
        isRinging = false
        activeDoId = null
        onStateChanged?.invoke()
    }

    private fun autoSnooze() {
        val context = appContext ?: return
        val id = activeDoId ?: return
        val name = activeDoName
        val note = activeNote
        val nextCycle = activeCycle + 1
        
        Log.d(TAG, "Auto-snoozing alarm for doId: $id. Next cycle: $nextCycle/$maxCycles")
        stopSoundOnly()
        
        if (activeCycle < maxCycles) {
            scheduleSnoozeAlarm(context, id, name, note, nextCycle)
            showAlarmNotification(context, id, name, "💤 Auto-Snoozed. Will ring again in ${autoSnoozeMs / 60000} min (Cycle $nextCycle/$maxCycles)", note)
        } else {
            // Reached max cycles
            showAlarmNotification(context, id, name, "⏰ Alarm finished after $maxCycles cycles.", note)
        }
        
        isRinging = false
        activeDoId = null
        onStateChanged?.invoke()
    }

    private fun stopSoundOnly() {
        handler.removeCallbacks(autoSnoozeRunnable)
        try {
            ringtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop ringtone", e)
        }
        ringtone = null
    }

    private fun scheduleSnoozeAlarm(context: Context, doId: Int, doName: String, note: String, cycle: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("do_id", doId)
            putExtra("do_name", doName)
            putExtra("reminder_note", note)
            putExtra("alarm_cycle", cycle)
            putExtra("is_snooze_trigger", true)
        }
        
        val requestCode = doId * 100 + 99 // Special request code index for snooze trigger
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = System.currentTimeMillis() + autoSnoozeMs
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled snooze alarm for doId: $doId at ${java.util.Date(triggerTime)} (Cycle: $cycle)")
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelPendingAutoSnooze(context: Context, doId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = doId * 100 + 99
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled pending snooze alarm for doId: $doId")
        }
    }

    private fun showAlarmNotification(context: Context, doId: Int, title: String, statusText: String, note: String) {
        // Intent to launch MainActivity on click
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_alarm_dialog", true)
            putExtra("alarm_do_id", doId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            doId + 50000, // Unique request code
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(statusText)
            .setSubText(note.take(40))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(isRinging) // persistent when ringing
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(doId, builder.build())
    }
}
