package com.mynix.planlekcji.notifications

import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import com.mynix.planlekcji.data.local.AppDatabase
import com.mynix.planlekcji.data.local.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            val settingsManager = SettingsManager(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val enabled = settingsManager.notifyEnabled.first()
                if (enabled) {
                    val lessons = db.lessonDao().getAllLessons().first()
                    val minutes = settingsManager.notifyMinutes.first()
                    LessonAlarmScheduler.scheduleAll(context, lessons, minutes)
                }
            }
        }
    }
}
