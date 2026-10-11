package com.ayla.pcos.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<UserPreferences> = dataStore.data.map { it.toUserPreferences() }

    suspend fun current(): UserPreferences = preferences.first()

    suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        dataStore.edit { mutable ->
            val updated = transform(mutable.toUserPreferences())
            mutable.write(updated)
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: defaults.onboardingDone,
            displayName = this[Keys.DISPLAY_NAME] ?: defaults.displayName,
            goals = this[Keys.GOALS]?.mapNotNull { runCatching { Goal.valueOf(it) }.getOrNull() }?.toSet() ?: defaults.goals,
            trackingSince = this[Keys.TRACKING_SINCE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            consentPersonalization = this[Keys.CONSENT_PERSONALIZATION] ?: defaults.consentPersonalization,
            consentAnalytics = this[Keys.CONSENT_ANALYTICS] ?: defaults.consentAnalytics,
            consentResearch = this[Keys.CONSENT_RESEARCH] ?: defaults.consentResearch,
            waterGoalCups = this[Keys.WATER_GOAL_CUPS] ?: defaults.waterGoalCups,
            cupMl = this[Keys.CUP_ML] ?: defaults.cupMl,
            waterRemindersEnabled = this[Keys.WATER_REMINDERS] ?: defaults.waterRemindersEnabled,
            waterIntervalMinutes = this[Keys.WATER_INTERVAL] ?: defaults.waterIntervalMinutes,
            activeHoursStart = this[Keys.ACTIVE_START].toTime(defaults.activeHoursStart),
            activeHoursEnd = this[Keys.ACTIVE_END].toTime(defaults.activeHoursEnd),
            quietHoursEnabled = this[Keys.QUIET_ENABLED] ?: defaults.quietHoursEnabled,
            quietHoursStart = this[Keys.QUIET_START].toTime(defaults.quietHoursStart),
            quietHoursEnd = this[Keys.QUIET_END].toTime(defaults.quietHoursEnd),
            hideNotificationContent = this[Keys.HIDE_NOTIFICATION_CONTENT] ?: defaults.hideNotificationContent,
            cycleNotificationsEnabled = this[Keys.CYCLE_NOTIFICATIONS] ?: defaults.cycleNotificationsEnabled,
            progressNotificationsEnabled = this[Keys.PROGRESS_NOTIFICATIONS] ?: defaults.progressNotificationsEnabled,
            eveningRoutineEnabled = this[Keys.EVENING_ROUTINE] ?: defaults.eveningRoutineEnabled,
            eveningRoutineTime = this[Keys.EVENING_ROUTINE_TIME].toTime(defaults.eveningRoutineTime),
            dailyCheckTime = this[Keys.DAILY_CHECK_TIME].toTime(defaults.dailyCheckTime),
            appLockEnabled = this[Keys.APP_LOCK] ?: defaults.appLockEnabled,
            blockScreenshots = this[Keys.BLOCK_SCREENSHOTS] ?: defaults.blockScreenshots,
            hidePrediction = this[Keys.HIDE_PREDICTION] ?: defaults.hidePrediction,
            showBodyMeasurements = this[Keys.SHOW_BODY] ?: defaults.showBodyMeasurements,
            lastLongCycleAlertKey = this[Keys.LAST_LONG_CYCLE_ALERT] ?: defaults.lastLongCycleAlertKey,
        )
    }

    private fun MutablePreferences.write(p: UserPreferences) {
        this[Keys.ONBOARDING_DONE] = p.onboardingDone
        this[Keys.DISPLAY_NAME] = p.displayName
        this[Keys.GOALS] = p.goals.map { it.name }.toSet()
        p.trackingSince?.let { this[Keys.TRACKING_SINCE] = it.toString() } ?: remove(Keys.TRACKING_SINCE)
        this[Keys.CONSENT_PERSONALIZATION] = p.consentPersonalization
        this[Keys.CONSENT_ANALYTICS] = p.consentAnalytics
        this[Keys.CONSENT_RESEARCH] = p.consentResearch
        this[Keys.WATER_GOAL_CUPS] = p.waterGoalCups
        this[Keys.CUP_ML] = p.cupMl
        this[Keys.WATER_REMINDERS] = p.waterRemindersEnabled
        this[Keys.WATER_INTERVAL] = p.waterIntervalMinutes
        this[Keys.ACTIVE_START] = p.activeHoursStart.toString()
        this[Keys.ACTIVE_END] = p.activeHoursEnd.toString()
        this[Keys.QUIET_ENABLED] = p.quietHoursEnabled
        this[Keys.QUIET_START] = p.quietHoursStart.toString()
        this[Keys.QUIET_END] = p.quietHoursEnd.toString()
        this[Keys.HIDE_NOTIFICATION_CONTENT] = p.hideNotificationContent
        this[Keys.CYCLE_NOTIFICATIONS] = p.cycleNotificationsEnabled
        this[Keys.PROGRESS_NOTIFICATIONS] = p.progressNotificationsEnabled
        this[Keys.EVENING_ROUTINE] = p.eveningRoutineEnabled
        this[Keys.EVENING_ROUTINE_TIME] = p.eveningRoutineTime.toString()
        this[Keys.DAILY_CHECK_TIME] = p.dailyCheckTime.toString()
        this[Keys.APP_LOCK] = p.appLockEnabled
        this[Keys.BLOCK_SCREENSHOTS] = p.blockScreenshots
        this[Keys.HIDE_PREDICTION] = p.hidePrediction
        this[Keys.SHOW_BODY] = p.showBodyMeasurements
        this[Keys.LAST_LONG_CYCLE_ALERT] = p.lastLongCycleAlertKey
    }

    private fun String?.toTime(default: LocalTime): LocalTime =
        this?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: default

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val GOALS = stringSetPreferencesKey("goals")
        val TRACKING_SINCE = stringPreferencesKey("tracking_since")
        val CONSENT_PERSONALIZATION = booleanPreferencesKey("consent_personalization")
        val CONSENT_ANALYTICS = booleanPreferencesKey("consent_analytics")
        val CONSENT_RESEARCH = booleanPreferencesKey("consent_research")
        val WATER_GOAL_CUPS = intPreferencesKey("water_goal_cups")
        val CUP_ML = intPreferencesKey("cup_ml")
        val WATER_REMINDERS = booleanPreferencesKey("water_reminders")
        val WATER_INTERVAL = intPreferencesKey("water_interval_minutes")
        val ACTIVE_START = stringPreferencesKey("active_hours_start")
        val ACTIVE_END = stringPreferencesKey("active_hours_end")
        val QUIET_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START = stringPreferencesKey("quiet_hours_start")
        val QUIET_END = stringPreferencesKey("quiet_hours_end")
        val HIDE_NOTIFICATION_CONTENT = booleanPreferencesKey("hide_notification_content")
        val CYCLE_NOTIFICATIONS = booleanPreferencesKey("cycle_notifications")
        val PROGRESS_NOTIFICATIONS = booleanPreferencesKey("progress_notifications")
        val EVENING_ROUTINE = booleanPreferencesKey("evening_routine_enabled")
        val EVENING_ROUTINE_TIME = stringPreferencesKey("evening_routine_time")
        val DAILY_CHECK_TIME = stringPreferencesKey("daily_check_time")
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
        val HIDE_PREDICTION = booleanPreferencesKey("hide_prediction")
        val SHOW_BODY = booleanPreferencesKey("show_body_measurements")
        val LAST_LONG_CYCLE_ALERT = stringPreferencesKey("last_long_cycle_alert")
    }
}
