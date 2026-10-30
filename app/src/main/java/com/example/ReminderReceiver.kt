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
        val alarmCycle = intent.getIntExtra("alarm_cycle", 1)
        val isSnoozeTrigger = intent.getBooleanExtra("is_snooze_trigger", false)

        Log.d("ReminderReceiver", "Alarm received for doId: $doId, name: $doName, cycle: $alarmCycle, isSnooze: $isSnoozeTrigger")

        if (doId != -1) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DoDatabase.getDatabase(context)
                    val doItem = db.doDao().getDoById(doId)
                    if (doItem != null && !doItem.isArchived && doItem.reminderTime != null) {
                        val config = CustomReminderConfig.fromString(doItem.reminderTime)
                        
                        if (config.enableRingingAlarm) {
                            CoroutineScope(Dispatchers.Main).launch {
                                AlarmSoundPlayer.startAlarm(context, doId, doName, config, alarmCycle)
                            }
                        } else {
                            ReminderManager.showNotification(
                                context = context,
                                doId = doId,
                                title = doName,
                                message = reminderNote,
                                soundName = reminderSound
                            )
                        }

                        // Reschedule next daily occurrence if NOT a snooze trigger
                        if (!isSnoozeTrigger) {
                            ReminderManager.scheduleAlarmsForDo(context, doItem)
                        }
                    } else {
                        // Fallback simple notification if doItem is deleted/archived
                        if (!isSnoozeTrigger) {
                            ReminderManager.showNotification(
                                context = context,
                                doId = doId,
                                title = doName,
                                message = reminderNote,
                                soundName = reminderSound
                            )
                        }
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
