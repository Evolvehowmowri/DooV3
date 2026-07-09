package com.example

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.DoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val doId = intent.getIntExtra("do_id", -1)
        val doName = intent.getStringExtra("do_name") ?: "Reminder"
        val reminderNote = intent.getStringExtra("reminder_note") ?: ""
        val reminderSound = intent.getStringExtra("reminder_sound") ?: "Default Accent"

        Log.d("ReminderReceiver", "Alarm received for doId: $doId, name: $doName")

        if (doId != -1) {
            // Post the notification
            ReminderManager.showNotification(
                context = context,
                doId = doId,
                title = doName,
                message = reminderNote,
                soundName = reminderSound
            )

            // Reschedule the next alarm in the background
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DoDatabase.getDatabase(context)
                    val doItem = db.doDao().getDoById(doId)
                    if (doItem != null && !doItem.isArchived && doItem.reminderTime != null) {
                        ReminderManager.scheduleAlarmsForDo(context, doItem)
                    }
                } catch (e: Exception) {
                    Log.e("ReminderReceiver", "Error rescheduling alarm", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
