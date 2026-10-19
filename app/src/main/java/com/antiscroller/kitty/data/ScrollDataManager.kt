package com.antiscroller.kitty.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages daily scroll distance, limits, target apps, kitty themes,
 * automatic midnight resets, and Parent / Accountability Partner Security PIN control.
 */
class ScrollDataManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "anti_scroller_prefs"
        private const val KEY_SCROLL_COUNT = "daily_scroll_count"
        private const val KEY_ACCUMULATED_PX = "accumulated_scroll_px"
        private const val KEY_MAX_SCROLLS = "max_daily_scrolls"
        private const val KEY_LAST_DATE = "last_active_date"
        private const val KEY_SMART_FRICTION = "smart_friction_enabled"
        private const val KEY_TARGET_INSTA = "target_instagram"
        private const val KEY_TARGET_YOUTUBE = "target_youtube"
        private const val KEY_KITTY_THEME = "kitty_theme"
        private const val KEY_GUARDIAN_PIN = "guardian_pin"
        private const val KEY_MAINTENANCE_EXPIRES = "maintenance_expires_ts"

        const val THEME_ORANGE = "orange"
        const val THEME_BLACK = "black"

        const val DEFAULT_MAX_SCROLLS = 50
        const val PKG_INSTAGRAM = "com.instagram.android"
        const val PKG_YOUTUBE = "com.google.android.youtube"

        @Volatile
        private var instance: ScrollDataManager? = null

        fun getInstance(context: Context): ScrollDataManager {
            return instance ?: synchronized(this) {
                instance ?: ScrollDataManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        checkAndPerformMidnightReset()
    }

    private fun getTodayDateString(): String = dateFormat.format(Date())

    /**
     * Checks if a new day has arrived. If so, automatically resets daily scroll count to 0.
     */
    fun checkAndPerformMidnightReset(): Boolean {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        if (today != lastDate) {
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_SCROLL_COUNT, 0)
                .putFloat(KEY_ACCUMULATED_PX, 0f)
                .apply()
            return true
        }
        return false
    }

    var dailyScrollCount: Int
        get() {
            checkAndPerformMidnightReset()
            return prefs.getInt(KEY_SCROLL_COUNT, 0)
        }
        set(value) {
            prefs.edit().putInt(KEY_SCROLL_COUNT, value).apply()
        }

    var accumulatedPx: Float
        get() {
            checkAndPerformMidnightReset()
            return prefs.getFloat(KEY_ACCUMULATED_PX, 0f)
        }
        set(value) {
            prefs.edit().putFloat(KEY_ACCUMULATED_PX, value).apply()
        }

    var maxDailyScrolls: Int
        get() = prefs.getInt(KEY_MAX_SCROLLS, DEFAULT_MAX_SCROLLS)
        set(value) {
            prefs.edit().putInt(KEY_MAX_SCROLLS, value.coerceAtLeast(10)).apply()
        }

    var isSmartFrictionEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_FRICTION, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SMART_FRICTION, value).apply()
        }

    var isInstagramMonitored: Boolean
        get() = prefs.getBoolean(KEY_TARGET_INSTA, true)
        set(value) {
            prefs.edit().putBoolean(KEY_TARGET_INSTA, value).apply()
        }

    var isYouTubeMonitored: Boolean
        get() = prefs.getBoolean(KEY_TARGET_YOUTUBE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_TARGET_YOUTUBE, value).apply()
        }

    var kittyTheme: String
        get() = prefs.getString(KEY_KITTY_THEME, THEME_ORANGE) ?: THEME_ORANGE
        set(value) {
            prefs.edit().putString(KEY_KITTY_THEME, value).apply()
        }

    // ==========================================
    // PARENT / ACCOUNTABILITY PARTNER PIN GUARD
    // ==========================================

    val isGuardianLockEnabled: Boolean
        get() = getGuardianPin().isNotEmpty()

    fun getGuardianPin(): String {
        return prefs.getString(KEY_GUARDIAN_PIN, "") ?: ""
    }

    fun setGuardianPin(pin: String) {
        prefs.edit().putString(KEY_GUARDIAN_PIN, pin.trim()).apply()
    }

    fun removeGuardianPin(pin: String): Boolean {
        if (verifyPin(pin)) {
            prefs.edit()
                .remove(KEY_GUARDIAN_PIN)
                .remove(KEY_MAINTENANCE_EXPIRES)
                .apply()
            return true
        }
        return false
    }

    fun verifyPin(inputPin: String): Boolean {
        val stored = getGuardianPin()
        if (stored.isEmpty()) return true
        return stored == inputPin.trim()
    }

    /**
     * When unlocked with PIN, grants maintenance mode for 5 minutes.
     */
    fun unlockMaintenance(minutes: Int = 5) {
        val expiresAt = System.currentTimeMillis() + (minutes * 60 * 1000L)
        prefs.edit().putLong(KEY_MAINTENANCE_EXPIRES, expiresAt).apply()
    }

    fun lockMaintenanceNow() {
        prefs.edit().remove(KEY_MAINTENANCE_EXPIRES).apply()
    }

    fun isMaintenanceModeActive(): Boolean {
        if (!isGuardianLockEnabled) return true
        val expiresAt = prefs.getLong(KEY_MAINTENANCE_EXPIRES, 0L)
        return System.currentTimeMillis() < expiresAt
    }

    fun getRemainingMaintenanceSeconds(): Long {
        val expiresAt = prefs.getLong(KEY_MAINTENANCE_EXPIRES, 0L)
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000L
        return remaining.coerceAtLeast(0L)
    }

    // ==========================================
    // SCROLL DISTANCE ACCUMULATOR
    // ==========================================

    /**
     * Adds vertical scroll distance in pixels.
     * Every screenHeight pixels scrolled accumulates to 1 full Reel unit.
     */
    @Synchronized
    fun addScrollPixels(pixels: Float, screenHeightPx: Int): Float {
        checkAndPerformMidnightReset()
        if (screenHeightPx <= 0) return getDropPercentage()

        var currentAcc = accumulatedPx + pixels
        var reelsToAdd = 0

        while (currentAcc >= screenHeightPx) {
            currentAcc -= screenHeightPx
            reelsToAdd++
        }

        prefs.edit()
            .putFloat(KEY_ACCUMULATED_PX, currentAcc)
            .putInt(KEY_SCROLL_COUNT, dailyScrollCount + reelsToAdd)
            .apply()

        return getDropPercentage(screenHeightPx)
    }

    /**
     * Adds 1 full reel unit directly (e.g. when tapping "Next" on YouTube Shorts miniplayer).
     */
    @Synchronized
    fun addSingleScrollReel(screenHeightPx: Int = 0): Float {
        checkAndPerformMidnightReset()
        prefs.edit()
            .putInt(KEY_SCROLL_COUNT, dailyScrollCount + 1)
            .apply()
        return getDropPercentage(screenHeightPx)
    }

    fun getDropPercentage(screenHeightPx: Int = 0): Float {
        val count = dailyScrollCount
        val max = maxDailyScrolls
        if (max <= 0) return 0f

        val fractionOfCurrentReel = if (screenHeightPx > 0) {
            accumulatedPx / screenHeightPx.toFloat()
        } else 0f

        val totalEffectiveReels = count.toFloat() + fractionOfCurrentReel
        return (totalEffectiveReels / max.toFloat()).coerceIn(0f, 1f)
    }

    fun isLimitReached(): Boolean {
        return dailyScrollCount >= maxDailyScrolls
    }

    fun resetDailyCount() {
        prefs.edit()
            .putInt(KEY_SCROLL_COUNT, 0)
            .putFloat(KEY_ACCUMULATED_PX, 0f)
            .apply()
    }

    fun isPackageMonitored(packageName: String): Boolean {
        return (isInstagramMonitored && packageName == PKG_INSTAGRAM) ||
               (isYouTubeMonitored && packageName == PKG_YOUTUBE)
    }
}
