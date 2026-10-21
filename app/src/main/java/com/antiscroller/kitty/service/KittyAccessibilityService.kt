package com.antiscroller.kitty.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.overlay.KittyOverlayManager
import com.antiscroller.kitty.ui.BreathingFrictionActivity
import com.antiscroller.kitty.ui.GuardianUnlockActivity

/**
 * Core Accessibility Service:
 * 1. Measures scroll distance in pixels: 1 Reel unit = 1 full phone screen height.
 * 2. Filters out tab switching (Home, Reels, Messages, Profile) and horizontal swipes.
 * 3. Ignores Direct Messages, Profile editing, and Stories.
 * 4. Multi-layer anti-tamper self-defense (Guardian PIN & 60s Cooling-Off Breathing Room).
 */
class KittyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KittyAccessibility"

        const val PKG_SETTINGS = "com.android.settings"
        const val PKG_PACKAGE_INSTALLER_1 = "com.google.android.packageinstaller"
        const val PKG_PACKAGE_INSTALLER_2 = "com.android.packageinstaller"
        const val PKG_VENDING = "com.android.vending"

        // Multi-language tampering and uninstallation keywords
        private val TAMPER_KEYWORDS = listOf(
            // English
            "uninstall", "force stop", "clear storage", "clear data", "deactivate",
            "remove admin", "turn off admin", "use clawback", "clawback shortcut", "stop clawback",
            // Spanish
            "desinstalar", "forzar detención", "borrar almacenamiento", "borrar datos", "desactivar",
            // French
            "désinstaller", "forcer l'arrêt", "effacer le stockage", "effacer les données", "désactiver",
            // German
            "deinstallieren", "stoppen erzwingen", "beenden erzwingen", "speicherinhalt löschen", "deaktivieren",
            // Hindi
            "अनइंस्टॉल", "फ़ोर्स स्टॉप", "डेटा साफ़ करें"
        )
    }

    private lateinit var dataManager: ScrollDataManager
    private lateinit var overlayManager: KittyOverlayManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentForegroundPackage: String = ""
    private var lastInstagramIndex: Int = -1
    private var lastYouTubeIndex: Int = -1

    private var pendingDropPercentage: Float? = null
    private val overlayUpdateRunnable = Runnable {
        val pct = pendingDropPercentage ?: return@Runnable
        pendingDropPercentage = null
        overlayManager.showOrUpdateOverlay(pct)
    }

    private fun scheduleOverlayUpdate(dropPercentage: Float) {
        pendingDropPercentage = dropPercentage
        mainHandler.removeCallbacks(overlayUpdateRunnable)
        mainHandler.post(overlayUpdateRunnable)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        dataManager = ScrollDataManager.getInstance(this)
        overlayManager = KittyOverlayManager.getInstance(this)
        Log.i(TAG, "Kitty Accessibility Service connected and active.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventPkg = event.packageName?.toString() ?: return

        // Ignore our own overlay window, keyboards, and system UI bars
        if (isIgnoredPackage(eventPkg)) {
            return
        }

        // Intercept tampering in Settings, Package Installer, or Google Play Store
        if (isSettingsOrInstaller(eventPkg)) {
            checkAndApplyUninstallProtection(eventPkg)
            mainHandler.post { overlayManager.hideOverlay() }
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(eventPkg, event)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleViewScrolled(eventPkg, event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(eventPkg, event)
            }
        }
    }

    private fun isIgnoredPackage(pkg: String): Boolean {
        val myPkg = applicationContext.packageName
        return pkg == myPkg ||
               pkg == "com.android.systemui" ||
               pkg == "android" ||
               pkg.contains("inputmethod") ||
               pkg.contains("keyboard") ||
               pkg.contains("keyguard")
    }

    /**
     * Fast, lightweight check to see if the scrolled view belongs to a comment section or bottom sheet.
     */
    private fun isCommentScroll(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false
        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""
        val sourceCls = sourceNode.className?.toString()?.lowercase() ?: ""
        val sourceDesc = sourceNode.contentDescription?.toString()?.lowercase() ?: ""

        if (sourceId.contains("comment") || sourceId.contains("engagement_panel") ||
            sourceId.contains("reply") || sourceCls.contains("comment") || sourceDesc.contains("comment")) {
            return true
        }

        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        while (current != null && depth < 5) {
            val pId = current.viewIdResourceName?.lowercase() ?: ""
            val pDesc = current.contentDescription?.toString()?.lowercase() ?: ""
            if (pId.contains("comment") || pId.contains("engagement_panel") ||
                pId.contains("reply") || pDesc.contains("comment")) {
                current.recycle()
                return true
            }
            val next = current.parent
            current.recycle()
            current = next
            depth++
        }
        return false
    }

    /**
     * Fast check to see if the scrolled view belongs to Instagram Direct Messages (inbox or chat thread).
     */
    private fun isInstagramDmScroll(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false
        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""
        if (sourceId.contains("direct") || sourceId.contains("thread") ||
            sourceId.contains("message") || sourceId.contains("inbox") ||
            sourceId.contains("recipient")) {
            return true
        }

        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        while (current != null && depth < 5) {
            val pId = current.viewIdResourceName?.lowercase() ?: ""
            if (pId.contains("direct") || pId.contains("thread") ||
                pId.contains("message") || pId.contains("inbox") ||
                pId.contains("recipient")) {
                current.recycle()
                return true
            }
            val next = current.parent
            current.recycle()
            current = next
            depth++
        }
        return false
    }

    /**
     * Fast check to see if the scrolled view belongs to Instagram Explore (Search grid) or Profile grid.
     */
    private fun isInstagramExploreOrProfileScroll(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false
        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""
        if (sourceId.contains("explore_recycler") || sourceId.contains("profile_recycler")) {
            return true
        }

        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        while (current != null && depth < 5) {
            val pId = current.viewIdResourceName?.lowercase() ?: ""
            if (pId.contains("explore_recycler") || pId.contains("profile_recycler")) {
                current.recycle()
                return true
            }
            val next = current.parent
            current.recycle()
            current = next
            depth++
        }
        return false
    }

    /**
     * Fast check to see if the scrolled view belongs to YouTube Shorts.
     * Rejects YouTube Homepage feed, Search, regular Video Watch Player, and Comments.
     */
    private fun isYouTubeShortsScroll(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false
        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""

        // Hard reject homepage feed, search results, watch panel, and comments
        if (sourceId.contains("comment") || sourceId.contains("engagement_panel") ||
            sourceId.contains("results_recycler_view") || sourceId.contains("feed_filter_bar") ||
            sourceId.contains("elements_recycler_view") || sourceId.contains("pane_fragment") ||
            sourceId.contains("watch_panel") || sourceId.contains("player_view") ||
            sourceId.contains("watch_scroll_view") || sourceId.contains("search")) {
            return false
        }

        if (sourceId.contains("reel_recycler") || sourceId.contains("reel_watch_fragment") ||
            sourceId.contains("shorts_container")) {
            return true
        }

        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        while (current != null && depth < 6) {
            val pId = current.viewIdResourceName?.lowercase() ?: ""
            if (pId.contains("comment") || pId.contains("engagement_panel") ||
                pId.contains("results_recycler_view") || pId.contains("pane_fragment") ||
                pId.contains("watch_panel") || pId.contains("player_view")) {
                current.recycle()
                return false
            }
            if (pId.contains("reel_recycler") || pId.contains("reel_watch_fragment") ||
                pId.contains("shorts_container") || pId.contains("reel_player_overlay")) {
                current.recycle()
                return true
            }
            val next = current.parent
            current.recycle()
            current = next
            depth++
        }
        return false
    }

    private fun handleWindowStateChanged(packageName: String, event: AccessibilityEvent) {
        if (packageName != currentForegroundPackage) {
            currentForegroundPackage = packageName
            lastInstagramIndex = -1
            lastYouTubeIndex = -1
        }

        // Monitored packages: Instagram and YouTube
        // The curtain card remains visible across ALL tabs and comment sheets as long as dropPercentage > 0!
        if (packageName == ScrollDataManager.PKG_INSTAGRAM || packageName == ScrollDataManager.PKG_YOUTUBE) {
            dataManager.checkAndPerformMidnightReset()

            val screenHeight = resources.displayMetrics.heightPixels
            val dropPercentage = dataManager.getDropPercentage(screenHeight)
            if (dropPercentage <= 0.0001f) {
                mainHandler.post { overlayManager.hideOverlay() }
            } else {
                mainHandler.post {
                    overlayManager.showOrUpdateOverlay(dropPercentage)
                }
            }
            return
        }

        // Check for Settings uninstall/force-stop protection
        if (isSettingsOrInstaller(packageName)) {
            checkAndApplyUninstallProtection(packageName)
            mainHandler.post {
                overlayManager.hideOverlay()
            }
            return
        }

        // Outside monitored apps: hide overlay
        mainHandler.post {
            overlayManager.hideOverlay()
        }
    }

    private fun handleViewScrolled(packageName: String, event: AccessibilityEvent) {
        if (!dataManager.isPackageMonitored(packageName)) return

        val source = event.source

        // STRICT TARGET FILTERING:
        // Only 3 places pull the curtain down:
        // 1. Instagram Home Feed (downward scroll)
        // 2. Instagram Reels (swiping reels)
        // 3. YouTube Shorts (swiping shorts)
        // Everything else (comments, DMs, explore, profile, YouTube homepage) is ignored!
        val isAllowed = when (packageName) {
            ScrollDataManager.PKG_INSTAGRAM -> {
                if (isCommentScroll(source) ||
                    isInstagramDmScroll(source) ||
                    isInstagramExploreOrProfileScroll(source)) {
                    false
                } else {
                    true
                }
            }
            ScrollDataManager.PKG_YOUTUBE -> {
                isYouTubeShortsScroll(source)
            }
            else -> false
        }

        source?.recycle()

        if (!isAllowed) {
            Log.i(TAG, "Scroll ignored: Not on allowed target screen (pkg=$packageName)")
            return
        }

        val deltaX = event.scrollDeltaX
        val deltaY = event.scrollDeltaY

        // Filter out horizontal swipes (e.g. story carousels, tab swiping)
        if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 20) {
            return
        }

        // CRITICAL: Scrolling UP (backwards/upward swipe to previous content) must NEVER pull the curtain down!
        // In Android, deltaY < 0 indicates scrolling up.
        if (deltaY < -10) {
            Log.i(TAG, "Scroll ignored: User scrolled UP (dy=$deltaY)")
            val fromIdx = event.fromIndex
            if (fromIdx >= 0) {
                if (packageName == ScrollDataManager.PKG_INSTAGRAM) lastInstagramIndex = fromIdx
                if (packageName == ScrollDataManager.PKG_YOUTUBE) lastYouTubeIndex = fromIdx
            }
            return
        }

        val screenHeight = resources.displayMetrics.heightPixels
        val fromIndex = event.fromIndex

        val pixelsToAdd: Float
        if (deltaY > 15) {
            // Definite downward/forward scroll
            pixelsToAdd = deltaY.toFloat()
            if (fromIndex >= 0) {
                if (packageName == ScrollDataManager.PKG_INSTAGRAM) lastInstagramIndex = fromIndex
                if (packageName == ScrollDataManager.PKG_YOUTUBE) lastYouTubeIndex = fromIndex
            }
        } else if (fromIndex >= 0) {
            // deltaY is near zero (e.g. ViewPager page snap). Check if page index actually advanced forward!
            val lastIndex = if (packageName == ScrollDataManager.PKG_INSTAGRAM) lastInstagramIndex else lastYouTubeIndex
            if (lastIndex < 0) {
                // First event seen in session: seed initial index without adding pixels
                if (packageName == ScrollDataManager.PKG_INSTAGRAM) lastInstagramIndex = fromIndex
                if (packageName == ScrollDataManager.PKG_YOUTUBE) lastYouTubeIndex = fromIndex
                return
            }
            if (fromIndex > lastIndex) {
                // Page advanced forward to the next reel/short!
                val diff = (fromIndex - lastIndex).coerceAtMost(3)
                if (packageName == ScrollDataManager.PKG_INSTAGRAM) lastInstagramIndex = fromIndex
                if (packageName == ScrollDataManager.PKG_YOUTUBE) lastYouTubeIndex = fromIndex
                pixelsToAdd = screenHeight * diff * 0.85f
                Log.i(TAG, "Reel page advanced: $lastIndex -> $fromIndex (+$pixelsToAdd px)")
            } else {
                // fromIndex <= lastIndex: page did not advance, or user swiped up to a previous reel
                if (packageName == ScrollDataManager.PKG_INSTAGRAM) lastInstagramIndex = fromIndex
                if (packageName == ScrollDataManager.PKG_YOUTUBE) lastYouTubeIndex = fromIndex
                Log.i(TAG, "Scroll ignored: Page did not advance forward ($lastIndex -> $fromIndex, dy=$deltaY)")
                return
            }
        } else {
            // deltaY near zero and no fromIndex: do NOT blindly add pixels
            return
        }

        Log.i(TAG, "Forward scroll applied on $packageName: +$pixelsToAdd px (dy=$deltaY, dx=$deltaX)")
        val newDropPercentage = dataManager.addScrollPixels(pixelsToAdd, screenHeight)
        scheduleOverlayUpdate(newDropPercentage)
    }

    private fun handleViewClicked(packageName: String, event: AccessibilityEvent) {
        if (!dataManager.isPackageMonitored(packageName)) return
        if (packageName != ScrollDataManager.PKG_YOUTUBE) return

        val source = event.source ?: return
        try {
            val viewId = source.viewIdResourceName?.lowercase() ?: ""
            val contentDesc = event.contentDescription?.toString()?.lowercase() ?: ""
            val text = source.text?.toString()?.lowercase() ?: ""

            // Detect if clicked view is a "Next" / "Skip" button
            val isNextButton = viewId.contains("next") ||
                               contentDesc.contains("next") ||
                               text.contains("next") ||
                               viewId.contains("skip")

            if (!isNextButton) return

            val inShorts = isYouTubeShortsScroll(source)
            if (!inShorts) return

            // Verify if it is in the YouTube Shorts miniplayer or player container
            if (isMiniplayerOrShortsNext(source)) {
                Log.i(TAG, "YouTube Shorts / miniplayer Next button clicked -> +1 Reel")
                val screenHeight = resources.displayMetrics.heightPixels
                val newDropPercentage = dataManager.addSingleScrollReel(screenHeight)

                mainHandler.post {
                    overlayManager.showOrUpdateOverlay(newDropPercentage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling view click: ${e.message}")
        } finally {
            source.recycle()
        }
    }

    private fun isMiniplayerOrShortsNext(sourceNode: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = sourceNode
        var depth = 0
        var isMatch = false

        while (current != null && depth < 10) {
            val id = current.viewIdResourceName?.lowercase() ?: ""
            val cls = current.className?.toString()?.lowercase() ?: ""

            if (id.contains("miniplayer") ||
                id.contains("mini_player") ||
                id.contains("modern_miniplayer") ||
                id.contains("reel") ||
                id.contains("shorts") ||
                cls.contains("miniplayer") ||
                cls.contains("reel")) {
                isMatch = true
                if (current != sourceNode) current.recycle()
                break
            }

            val parent = current.parent
            if (current != sourceNode) current.recycle()
            current = parent
            depth++
        }

        if (isMatch) return true

        val root = rootInActiveWindow ?: return false
        try {
            val miniplayerNodes = root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/miniplayer_container")
                .plus(root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/modern_miniplayer"))
                .plus(root.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/mini_player"))
            if (miniplayerNodes.isNotEmpty()) {
                for (n in miniplayerNodes) n.recycle()
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    private fun isYouTubeMiniplayerActive(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        try {
            val miniplayers = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/miniplayer_container")
                .plus(rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/modern_miniplayer"))
                .plus(rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/mini_player"))
            if (miniplayers.isNotEmpty()) {
                for (n in miniplayers) n.recycle()
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    private fun isSettingsOrInstaller(pkg: String): Boolean {
        return pkg.startsWith("com.android.settings") ||
               pkg.startsWith("com.google.android.packageinstaller") ||
               pkg.startsWith("com.android.packageinstaller") ||
               pkg == PKG_VENDING
    }

    private var lastProtectionTriggerTs = 0L

    /**
     * Intercepts uninstallation, force-stop, data clearing, device admin deactivation,
     * and accessibility disabling across Android Settings, Package Installer, and Google Play Store.
     * 
     * Defense Rules:
     * 1. If maintenance mode is active (granted via PIN or 60s breathing timer), user has 5m unhindered access.
     * 2. If Guardian Lock is active, requires parent / accountability partner PIN via GuardianUnlockActivity.
     * 3. If Guardian Lock is not active, enforces a 60-second cooling-off period via BreathingFrictionActivity.
     */
    private fun checkAndApplyUninstallProtection(eventPkg: String) {
        if (dataManager.isMaintenanceModeActive()) return

        val now = System.currentTimeMillis()
        if (now - lastProtectionTriggerTs < 2000L) return

        val rootNode = rootInActiveWindow ?: return
        try {
            val myPkg = applicationContext.packageName

            // 1. Check if the screen references ClawBack
            val appNodes = rootNode.findAccessibilityNodeInfosByText("ClawBack")
                .plus(rootNode.findAccessibilityNodeInfosByText("AntiScroller"))
                .plus(rootNode.findAccessibilityNodeInfosByText(myPkg))

            val hasAppText = appNodes.isNotEmpty()
            for (node in appNodes) node.recycle()

            // 2. Check if a tampering/uninstall/deactivation keyword exists on screen
            var hasTamperAction = false
            for (keyword in TAMPER_KEYWORDS) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
                if (nodes.isNotEmpty()) {
                    hasTamperAction = true
                    for (node in nodes) node.recycle()
                    break
                }
            }

            // 3. For package installer: also check package name
            val isPackageInstaller = eventPkg.startsWith("com.google.android.packageinstaller") ||
                                    eventPkg.startsWith("com.android.packageinstaller")

            val isTamperingTarget = (hasAppText && hasTamperAction) ||
                                    (isPackageInstaller && hasAppText)

            if (!isTamperingTarget) return

            lastProtectionTriggerTs = now

            mainHandler.post {
                if (dataManager.isGuardianLockEnabled) {
                    Toast.makeText(
                        applicationContext,
                        "🛡️ Protected by Guardian PIN! Parent / partner must enter PIN to modify or delete.",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(applicationContext, GuardianUnlockActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        applicationContext,
                        "🌿 60-Second Cooling-Off Period: Pause and take a breath.",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(applicationContext, BreathingFrictionActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating uninstall protection: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Kitty Accessibility Service interrupted.")
        overlayManager.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hideOverlay()
    }
}
