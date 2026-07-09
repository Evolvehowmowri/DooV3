package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.DoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted, rescheduling all reminders")
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DoDatabase.getDatabase(context)
                    // Get all active (non-archived) items
                    val allDos = db.doDao().getAllDosFlow().first()
                    for (doItem in allDos) {
                        if (doItem.reminderTime != null) {
                            ReminderManager.scheduleAlarmsForDo(context, doItem)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
