package com.antiscroller.kitty.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
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

    @Volatile
    private var inMemoryScrollCount: Int = -1
    @Volatile
    private var inMemoryAccumulatedPx: Float = -1f
    @Volatile
    private var inMemoryMaxScrolls: Int = -1

    private val saveHandler = Handler(Looper.getMainLooper())
    private val saveRunnable = Runnable {
        prefs.edit()
            .putFloat(KEY_ACCUMULATED_PX, inMemoryAccumulatedPx)
            .putInt(KEY_SCROLL_COUNT, inMemoryScrollCount)
            .apply()
    }

    private fun scheduleDebouncedSave() {
        saveHandler.removeCallbacks(saveRunnable)
        saveHandler.postDelayed(saveRunnable, 500)
    }

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
        private const val KEY_INITIAL_SETUP_DONE = "initial_setup_done"

        // Anti-Impulse Cooling-Off Keys
        private const val KEY_PENDING_MAX_SCROLLS = "pending_max_scrolls"
        private const val KEY_PENDING_DISABLE_INSTA = "pending_disable_insta"
        private const val KEY_PENDING_DISABLE_YOUTUBE = "pending_disable_youtube"

        // Play Store In-App Purchases & Parental Subscriptions
        private const val KEY_COSMETIC_PACK_UNLOCKED = "cosmetic_pack_unlocked"
        private const val KEY_PARENTAL_SUBSCRIBED = "parental_control_subscribed"
        private const val KEY_CHILD_MODE_ENABLED = "child_mode_enabled"
        private const val KEY_GUARDIAN_ENDPOINT = "guardian_alert_endpoint"
        private const val KEY_CHILD_DEVICE_NAME = "child_device_name"
        private const val KEY_LAST_HEARTBEAT_TS = "last_heartbeat_timestamp"

        const val THEME_GINGER = "ginger"
        const val THEME_TABBY = "tabby"
        const val THEME_SATURN = "saturn"
        const val THEME_ROLEX = "rolex"
        const val THEME_SALMON = "salmon"
        const val THEME_KATRINA = "katrina"

        // Backward compatibility mappings
        const val THEME_ORANGE = "orange"
        const val THEME_BLACK = "black"

        const val DEFAULT_MAX_SCROLLS = 25
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
        inMemoryScrollCount = prefs.getInt(KEY_SCROLL_COUNT, 0)
        inMemoryAccumulatedPx = prefs.getFloat(KEY_ACCUMULATED_PX, 0f)
        inMemoryMaxScrolls = prefs.getInt(KEY_MAX_SCROLLS, DEFAULT_MAX_SCROLLS)
    }

    private fun getTodayDateString(): String = dateFormat.format(Date())

    /**
     * Checks if a new day has arrived. If so, automatically resets daily scroll count to 0
     * and applies any scheduled anti-impulse cooling-off adjustments.
     */
    fun checkAndPerformMidnightReset(): Boolean {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        if (today != lastDate) {
            inMemoryScrollCount = 0
            inMemoryAccumulatedPx = 0f
            val editor = prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_SCROLL_COUNT, 0)
                .putFloat(KEY_ACCUMULATED_PX, 0f)

            // Apply any pending limit increase scheduled during cooling-off
            val pendingCap = prefs.getInt(KEY_PENDING_MAX_SCROLLS, 0)
            if (pendingCap > 0) {
                inMemoryMaxScrolls = pendingCap
                editor.putInt(KEY_MAX_SCROLLS, pendingCap)
                editor.remove(KEY_PENDING_MAX_SCROLLS)
            }

            // Apply any pending app monitoring unhooks scheduled during cooling-off
            if (prefs.getBoolean(KEY_PENDING_DISABLE_INSTA, false)) {
                editor.putBoolean(KEY_TARGET_INSTA, false)
                editor.remove(KEY_PENDING_DISABLE_INSTA)
            }
            if (prefs.getBoolean(KEY_PENDING_DISABLE_YOUTUBE, false)) {
                editor.putBoolean(KEY_TARGET_YOUTUBE, false)
                editor.remove(KEY_PENDING_DISABLE_YOUTUBE)
            }

            editor.apply()
            return true
        }
        return false
    }

    var dailyScrollCount: Int
        get() {
            checkAndPerformMidnightReset()
            if (inMemoryScrollCount < 0) {
                inMemoryScrollCount = prefs.getInt(KEY_SCROLL_COUNT, 0)
            }
            return inMemoryScrollCount
        }
        set(value) {
            inMemoryScrollCount = value
            prefs.edit().putInt(KEY_SCROLL_COUNT, value).apply()
        }

    var accumulatedPx: Float
        get() {
            checkAndPerformMidnightReset()
            if (inMemoryAccumulatedPx < 0f) {
                inMemoryAccumulatedPx = prefs.getFloat(KEY_ACCUMULATED_PX, 0f)
            }
            return inMemoryAccumulatedPx
        }
        set(value) {
            inMemoryAccumulatedPx = value
            prefs.edit().putFloat(KEY_ACCUMULATED_PX, value).apply()
        }

    var maxDailyScrolls: Int
        get() {
            if (inMemoryMaxScrolls <= 0) {
                inMemoryMaxScrolls = prefs.getInt(KEY_MAX_SCROLLS, DEFAULT_MAX_SCROLLS)
            }
            return inMemoryMaxScrolls
        }
        set(value) {
            val clamped = value.coerceAtLeast(10)
            inMemoryMaxScrolls = clamped
            prefs.edit().putInt(KEY_MAX_SCROLLS, clamped).apply()
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

    var pendingMaxDailyScrolls: Int
        get() = prefs.getInt(KEY_PENDING_MAX_SCROLLS, 0)
        set(value) {
            if (value <= 0) {
                prefs.edit().remove(KEY_PENDING_MAX_SCROLLS).apply()
            } else {
                prefs.edit().putInt(KEY_PENDING_MAX_SCROLLS, value).apply()
            }
        }

    var pendingDisableInstagram: Boolean
        get() = prefs.getBoolean(KEY_PENDING_DISABLE_INSTA, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PENDING_DISABLE_INSTA, value).apply()
        }

    var pendingDisableYouTube: Boolean
        get() = prefs.getBoolean(KEY_PENDING_DISABLE_YOUTUBE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PENDING_DISABLE_YOUTUBE, value).apply()
        }

    var kittyTheme: String
        get() {
            val saved = prefs.getString(KEY_KITTY_THEME, THEME_GINGER) ?: THEME_GINGER
            return when (saved.lowercase()) {
                THEME_BLACK, THEME_TABBY -> THEME_TABBY
                THEME_SATURN -> THEME_SATURN
                THEME_ROLEX -> THEME_ROLEX
                THEME_SALMON -> THEME_SALMON
                THEME_KATRINA -> THEME_KATRINA
                else -> THEME_GINGER
            }
        }
        set(value) {
            prefs.edit().putString(KEY_KITTY_THEME, value).apply()
        }

    var isInitialSetupDone: Boolean
        get() = prefs.getBoolean(KEY_INITIAL_SETUP_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_INITIAL_SETUP_DONE, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    // ==========================================
    // COSMETIC PACK & PARENTAL CONTROL BILLING
    // ==========================================

    var isCosmeticPackUnlocked: Boolean
        get() = prefs.getBoolean(KEY_COSMETIC_PACK_UNLOCKED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_COSMETIC_PACK_UNLOCKED, value).apply()
        }

    fun isThemeUnlocked(theme: String): Boolean {
        return when (theme.lowercase()) {
            THEME_GINGER, THEME_ORANGE -> true
            else -> isCosmeticPackUnlocked
        }
    }

    var isParentalSubscribed: Boolean
        get() = prefs.getBoolean(KEY_PARENTAL_SUBSCRIBED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PARENTAL_SUBSCRIBED, value).apply()
        }

    var isChildModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHILD_MODE_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_CHILD_MODE_ENABLED, value).apply()
        }

    var guardianAlertEndpoint: String
        get() = prefs.getString(KEY_GUARDIAN_ENDPOINT, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_GUARDIAN_ENDPOINT, value.trim()).apply()
        }

    var childDeviceName: String
        get() = prefs.getString(KEY_CHILD_DEVICE_NAME, android.os.Build.MODEL) ?: android.os.Build.MODEL
        set(value) {
            prefs.edit().putString(KEY_CHILD_DEVICE_NAME, value.trim()).apply()
        }

    var lastHeartbeatTimestamp: Long
        get() = prefs.getLong(KEY_LAST_HEARTBEAT_TS, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_HEARTBEAT_TS, value).apply()
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

        var currentAcc = inMemoryAccumulatedPx + pixels
        var reelsToAdd = 0

        while (currentAcc >= screenHeightPx) {
            currentAcc -= screenHeightPx
            reelsToAdd++
        }

        inMemoryAccumulatedPx = currentAcc
        inMemoryScrollCount += reelsToAdd

        if (reelsToAdd > 0) {
            saveHandler.removeCallbacks(saveRunnable)
            prefs.edit()
                .putFloat(KEY_ACCUMULATED_PX, currentAcc)
                .putInt(KEY_SCROLL_COUNT, inMemoryScrollCount)
                .apply()
        } else {
            scheduleDebouncedSave()
        }

        return getDropPercentage(screenHeightPx)
    }

    /**
     * Adds 1 full reel unit directly (e.g. when tapping "Next" on YouTube Shorts miniplayer).
     */
    @Synchronized
    fun addSingleScrollReel(screenHeightPx: Int = 0): Float {
        checkAndPerformMidnightReset()
        inMemoryScrollCount += 1
        saveHandler.removeCallbacks(saveRunnable)
        prefs.edit()
            .putInt(KEY_SCROLL_COUNT, inMemoryScrollCount)
            .apply()
        return getDropPercentage(screenHeightPx)
    }

    fun getDropPercentage(screenHeightPx: Int = 0): Float {
        val count = inMemoryScrollCount.coerceAtLeast(0)
        val max = inMemoryMaxScrolls.coerceAtLeast(1)

        val fractionOfCurrentReel = if (screenHeightPx > 0 && inMemoryAccumulatedPx > 0f) {
            inMemoryAccumulatedPx / screenHeightPx.toFloat()
        } else 0f

        val totalEffectiveReels = count.toFloat() + fractionOfCurrentReel
        return (totalEffectiveReels / max.toFloat()).coerceIn(0f, 1f)
    }

    fun isLimitReached(): Boolean {
        return dailyScrollCount >= maxDailyScrolls
    }

    fun resetDailyCount() {
        inMemoryScrollCount = 0
        inMemoryAccumulatedPx = 0f
        saveHandler.removeCallbacks(saveRunnable)
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
