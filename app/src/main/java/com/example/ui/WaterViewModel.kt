package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryEntry
import com.example.data.WaterRepository
import com.example.worker.WaterReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WaterRepository(application.applicationContext)

    // State holders
    private val _dailyGoal = MutableStateFlow(8)
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    private val _todayGlasses = MutableStateFlow(0)
    val todayGlasses: StateFlow<Int> = _todayGlasses.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _reminderInterval = MutableStateFlow(60)
    val reminderInterval: StateFlow<Int> = _reminderInterval.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads/reloads states from SharedPreferences. Handles automatic date rollover check.
     */
    fun loadData() {
        val goal = repository.getDailyGoal()
        val today = repository.getTodayGlasses()
        val interval = repository.getReminderIntervalMins()
        val enabled = repository.getRemindersEnabled()
        val hist = repository.getLast7DaysHistory()

        _dailyGoal.value = goal
        _todayGlasses.value = today
        _reminderInterval.value = interval
        _remindersEnabled.value = enabled
        _history.value = hist
    }

    fun addGlass() {
        viewModelScope.launch {
            val newAmount = repository.addTodayGlass()
            _todayGlasses.value = newAmount
            // Refresh history since today is included
            _history.value = repository.getLast7DaysHistory()
        }
    }

    fun removeGlass() {
        viewModelScope.launch {
            val newAmount = repository.removeTodayGlass()
            _todayGlasses.value = newAmount
            // Refresh history since today is included
            _history.value = repository.getLast7DaysHistory()
        }
    }

    fun updateDailyGoal(newGoal: Int) {
        viewModelScope.launch {
            repository.setDailyGoal(newGoal)
            _dailyGoal.value = newGoal
            _history.value = repository.getLast7DaysHistory()
        }
    }

    fun updateReminderInterval(minutes: Int) {
        viewModelScope.launch {
            repository.setReminderIntervalMins(minutes)
            _reminderInterval.value = minutes
            
            // Re-schedule reminder work with new interval if reminders are enabled
            if (_remindersEnabled.value) {
                WaterReminderWorker.scheduleReminder(getApplication(), minutes)
            }
        }
    }

    fun toggleReminders(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRemindersEnabled(enabled)
            _remindersEnabled.value = enabled
            
            if (enabled) {
                WaterReminderWorker.scheduleReminder(getApplication(), _reminderInterval.value)
            } else {
                WaterReminderWorker.cancelReminder(getApplication())
            }
        }
    }

    /**
     * Initial startup flow logic to register default reminder if app launches for first time.
     */
    fun setupOnStartup() {
        if (_remindersEnabled.value) {
            WaterReminderWorker.scheduleReminder(getApplication(), _reminderInterval.value)
        }
    }
}
