package com.mynix.planlekcji.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mynix.planlekcji.data.local.AppDatabase
import com.mynix.planlekcji.data.local.SettingsManager
import com.mynix.planlekcji.notifications.LessonAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val db = AppDatabase.getDatabase(application)
    private val lessonDao = db.lessonDao()

    init {
        // Automatycznie odświeżaj alarmy przy każdej zmianie danych lub ustawień
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.flow.combine(
                lessonDao.getAllLessons(),
                settingsManager.notifyEnabled,
                settingsManager.notifyMinutes
            ) { _, _, _ -> }.collect {
                rescheduleAll()
            }
        }
    }

    val notifyEnabled = settingsManager.notifyEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notifyMinutes = settingsManager.notifyMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val showLockScreenNotif = settingsManager.showLockScreenNotif
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastViewMode = settingsManager.lastViewMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotifyEnabled(enabled)
            if (enabled) rescheduleAll() else cancelAll()
        }
    }

    fun setMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsManager.setNotifyMinutes(minutes)
            // Od razu przelicz alarmy po zmianie czasu
            rescheduleAll()
        }
    }

    fun setShowLockScreenNotif(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setShowLockScreenNotif(enabled)
        }
    }

    fun setLastViewMode(mode: Int) {
        viewModelScope.launch {
            settingsManager.setLastViewMode(mode)
        }
    }

    fun rescheduleAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = settingsManager.notifyEnabled.first()
            val minutes = settingsManager.notifyMinutes.first()
            val allLessons = lessonDao.getAllLessons().first()

            if (!enabled || allLessons.isEmpty()) {
                LessonAlarmScheduler.cancelAll(getApplication(), allLessons)
                return@launch
            }
            LessonAlarmScheduler.scheduleAll(getApplication(), allLessons, minutes)
        }
    }

    fun cancelAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val allLessons = lessonDao.getAllLessons().first()
            LessonAlarmScheduler.cancelAll(getApplication(), allLessons)
        }
    }
}
