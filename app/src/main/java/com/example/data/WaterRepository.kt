package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WaterRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "water_reminder_prefs"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_REMINDER_INTERVAL_MINS = "reminder_interval_mins"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        private const val PREFIX_GLASSES_DAY = "glasses_"
        
        const val DEFAULT_GOAL = 8
        const val DEFAULT_INTERVAL_MINS = 60
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    fun getDailyGoal(): Int {
        return prefs.getInt(KEY_DAILY_GOAL, DEFAULT_GOAL)
    }

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL, goal).apply()
    }

    fun getTodayGlasses(): Int {
        val todayKey = PREFIX_GLASSES_DAY + getTodayDateString()
        return prefs.getInt(todayKey, 0)
    }

    fun addTodayGlass(): Int {
        val todayKey = PREFIX_GLASSES_DAY + getTodayDateString()
        val current = getTodayGlasses()
        val newVal = current + 1
        prefs.edit().putInt(todayKey, newVal).apply()
        return newVal
    }

    fun removeTodayGlass(): Int {
        val todayKey = PREFIX_GLASSES_DAY + getTodayDateString()
        val current = getTodayGlasses()
        val newVal = if (current > 0) current - 1 else 0
        prefs.edit().putInt(todayKey, newVal).apply()
        return newVal
    }

    fun getReminderIntervalMins(): Int {
        return prefs.getInt(KEY_REMINDER_INTERVAL_MINS, DEFAULT_INTERVAL_MINS)
    }

    fun setReminderIntervalMins(mins: Int) {
        prefs.edit().putInt(KEY_REMINDER_INTERVAL_MINS, mins).apply()
    }

    fun getRemindersEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
    }

    /**
     * Returns list of pair of (Day Name, Glasses Consumed) for the last 7 days.
     * E.g. ("Mon", 5), ("Tue", 8)... including today as the last element.
     */
    fun getLast7DaysHistory(): List<HistoryEntry> {
        val historyList = ArrayList<HistoryEntry>()
        val calendar = Calendar.getInstance()
        
        // Move calendar 6 days back to start there
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        
        val displayDayFormat = SimpleDateFormat("EEE", Locale.getDefault()) // "Mon", "Tue" etc.
        val dateValueFormat = SimpleDateFormat("MMM d", Locale.getDefault()) // "Jun 5"

        for (i in 0..6) {
            val dateStr = dateFormat.format(calendar.time)
            val key = PREFIX_GLASSES_DAY + dateStr
            val glasses = prefs.getInt(key, 0)
            val dayLabel = displayDayFormat.format(calendar.time)
            val fullDateLabel = dateValueFormat.format(calendar.time)
            
            historyList.add(HistoryEntry(dayLabel, fullDateLabel, glasses))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return historyList
    }
}

data class HistoryEntry(
    val dayOfWeek: String, // "Mon"
    val dateLabel: String, // "Jun 5"
    val glasses: Int
)
