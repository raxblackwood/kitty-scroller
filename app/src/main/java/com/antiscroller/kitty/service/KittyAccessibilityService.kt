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
import com.antiscroller.kitty.ui.GuardianUnlockActivity

/**
 * Core Accessibility Service:
 * 1. Measures scroll distance in pixels: 1 Reel unit = 1 full phone screen height.
 * 2. Filters out tab switching (Home, Reels, Messages, Profile) and horizontal swipes.
 * 3. Ignores Direct Messages, Profile editing, and Stories.
 * 4. Protects against deletion using Parent / Accountability Partner Guardian PIN.
 */
class KittyAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "KittyAccessibility"

        const val PKG_SETTINGS = "com.android.settings"
        const val PKG_PACKAGE_INSTALLER = "com.google.android.packageinstaller"
        const val PKG_VENDING = "com.android.vending"
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
     * Determines whether the user is on Instagram DMs or Story camera (non-doomscroll).
     */
    private fun isInstagramDmOrCamera(className: String, sourceNode: AccessibilityNodeInfo?): Boolean {
        if (className.contains("Direct", ignoreCase = true) ||
            className.contains("Message", ignoreCase = true) ||
            className.contains("Inbox", ignoreCase = true) ||
            className.contains("StoryCamera", ignoreCase = true) ||
            className.contains("TransparentModalActivity", ignoreCase = true)) {
            return true
        }

        val viewId = sourceNode?.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("direct_") ||
            viewId.contains("thread_") ||
            viewId.contains("message_composer")) {
            return true
        }

        return false
    }

    /**
     * Fast, zero-IPC check to detect if the scrolled view belongs to comments.
     * Uses in-memory hierarchy traversal only (completes in < 0.05ms).
     */
    private fun isCommentSectionOrSheet(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false

        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""

        // Fast-path: Main feed and video scrollers are NEVER comments!
        if (sourceId.contains("reel_recycler") ||
            sourceId.contains("clips_view_pager") ||
            sourceId.contains("vertical_view_pager") ||
            sourceId.contains("feed_recycler") ||
            sourceId.contains("sticky_header_list")) {
            return false
        }

        val sourceCls = sourceNode.className?.toString()?.lowercase() ?: ""
        val sourceDesc = sourceNode.contentDescription?.toString()?.lowercase() ?: ""

        // Direct check on the scrolling container ID / class / desc
        if (isCommentOrSheetIdentifier(sourceId, sourceCls, sourceDesc)) {
            return true
        }

        // Fast in-memory hierarchy walk (up to 8 levels) looking for comment containers
        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        var isComment = false

        while (current != null && depth < 8) {
            val parentId = current.viewIdResourceName?.lowercase() ?: ""

            if (parentId.contains("reel_recycler") ||
                parentId.contains("clips_view_pager") ||
                parentId.contains("vertical_view_pager") ||
                parentId.contains("feed_recycler")) {
                current.recycle()
                return false
            }

            val parentCls = current.className?.toString()?.lowercase() ?: ""
            val parentDesc = current.contentDescription?.toString()?.lowercase() ?: ""

            if (isCommentOrSheetIdentifier(parentId, parentCls, parentDesc)) {
                isComment = true
                current.recycle()
                break
            }

            val nextParent = current.parent
            current.recycle()
            current = nextParent
            depth++
        }

        return isComment
    }

    private fun isCommentOrSheetIdentifier(id: String, cls: String, desc: String): Boolean {
        return id.contains("comment") ||
               id.contains("engagement_panel") ||
               id.contains("panel_content") ||
               id.contains("bottom_sheet") ||
               id.contains("touch_container") ||
               id.contains("design_bottom_sheet") ||
               id.contains("modal_container") ||
               id.contains("sheet_container") ||
               id.contains("reply") ||
               cls.contains("bottomsheet") ||
               cls.contains("comment") ||
               desc.contains("comment")
    }

    /**
     * Specifically verifies if a scroll in YouTube is occurring inside YouTube Shorts.
     * Filters out YouTube Home feed, search results, subscriptions, and comments.
     */
    private fun isYouTubeShortsScroller(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false

        // Comments or engagement panels are never Shorts video scrollers
        if (isCommentSectionOrSheet(sourceNode)) {
            return false
        }

        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""
        val sourceClass = sourceNode.className?.toString()?.lowercase() ?: ""

        // 1. If it's reel_recycler or contains shorts/reel, it's definitely Shorts!
        if (sourceId.contains("reel") || sourceId.contains("shorts") ||
            sourceClass.contains("reel") || sourceClass.contains("shorts")) {
            return true
        }

        // 2. Reject if the scroller itself is the home feed results list
        if (sourceId.contains("results_recycler_view") ||
            sourceId.contains("feed_filter_bar") ||
            sourceId.contains("pane_fragment") ||
            sourceId.contains("elements_recycler_view")) {
            return false
        }

        // 3. Walk up the parent hierarchy (up to 8 levels) looking for Shorts container
        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        var isShorts = false

        while (current != null && depth < 8) {
            val parentId = current.viewIdResourceName?.lowercase() ?: ""
            val parentClass = current.className?.toString()?.lowercase() ?: ""

            if (parentId.contains("reel") || parentId.contains("shorts") ||
                parentClass.contains("reel") || parentClass.contains("shorts")) {
                isShorts = true
                current.recycle()
                break
            }

            // Reject if it belongs to Home results
            if (parentId.contains("results_recycler_view") || parentId.contains("pane_fragment")) {
                current.recycle()
                return false
            }

            val nextParent = current.parent
            current.recycle()
            current = nextParent
            depth++
        }

        return isShorts
    }

    /**
     * Determines whether YouTube is currently in Shorts mode (vs Home feed or long videos).
     */
    private fun isYouTubeShortsActive(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        try {
            val reelRecyclers = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_recycler")
            if (reelRecyclers.isNotEmpty()) {
                for (node in reelRecyclers) node.recycle()
                return true
            }

            val shortsContainers = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_watch_fragment")
            if (shortsContainers.isNotEmpty()) {
                for (node in shortsContainers) node.recycle()
                return true
            }

            val shortsTabs = rootNode.findAccessibilityNodeInfosByText("Shorts")
            for (tab in shortsTabs) {
                val isSel = tab.isSelected || tab.parent?.isSelected == true
                tab.recycle()
                if (isSel) return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking YouTube Shorts state: ${e.message}")
        }
        return false
    }

    private fun handleWindowStateChanged(packageName: String, event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""

        if (packageName != currentForegroundPackage) {
            currentForegroundPackage = packageName
            lastInstagramIndex = -1
            lastYouTubeIndex = -1
        }

        // 1. Instagram: hide overlay in DMs/Camera, show overlay on Feed / Reels
        if (packageName == ScrollDataManager.PKG_INSTAGRAM) {
            dataManager.checkAndPerformMidnightReset()

            if (isInstagramDmOrCamera(className, event.source)) {
                mainHandler.post { overlayManager.hideOverlay() }
                return
            }

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

        // 2. YouTube: show overlay when inside Shorts or when miniplayer is active if limit reached
        if (packageName == ScrollDataManager.PKG_YOUTUBE) {
            dataManager.checkAndPerformMidnightReset()

            val root = rootInActiveWindow
            val inShorts = isYouTubeShortsActive(root)
            val miniplayerActive = isYouTubeMiniplayerActive(root)

            if (inShorts || (miniplayerActive && dataManager.isLimitReached())) {
                val screenHeight = resources.displayMetrics.heightPixels
                val dropPercentage = dataManager.getDropPercentage(screenHeight)
                if (dropPercentage <= 0.0001f) {
                    mainHandler.post { overlayManager.hideOverlay() }
                } else {
                    mainHandler.post {
                        overlayManager.showOrUpdateOverlay(dropPercentage)
                    }
                }
            } else {
                mainHandler.post {
                    overlayManager.hideOverlay()
                }
            }
            return
        }

        // 3. Settings / App Manager (Anti-Uninstall & Guardian Protection)
        if (isSettingsOrInstaller(packageName)) {
            checkAndApplyUninstallProtection(event)
            mainHandler.post {
                overlayManager.hideOverlay()
            }
            return
        }

        // 4. User switched to another app (WhatsApp, Launcher, Chrome, etc.)
        val activeRootPkg = rootInActiveWindow?.packageName?.toString()
        if (activeRootPkg != null && dataManager.isPackageMonitored(activeRootPkg)) {
            return
        }

        mainHandler.post {
            overlayManager.hideOverlay()
        }
    }

    private fun handleViewScrolled(packageName: String, event: AccessibilityEvent) {
        if (!dataManager.isPackageMonitored(packageName)) return

        val source = event.source

        // 1. Strict YouTube filter: MUST be YouTube Shorts! (Ignore Home feed, long videos, etc.)
        if (packageName == ScrollDataManager.PKG_YOUTUBE) {
            if (!isYouTubeShortsScroller(source)) {
                source?.recycle()
                return
            }
        }

        // 2. Filter out Comments completely on both Instagram and YouTube!
        if (isCommentSectionOrSheet(source)) {
            Log.d(TAG, "Scroll ignored: Inside comments section or sheet ($packageName)")
            source?.recycle()
            return
        }

        // 3. Instagram non-doomscroll check (DMs, story camera)
        val className = event.className?.toString() ?: ""
        if (packageName == ScrollDataManager.PKG_INSTAGRAM && isInstagramDmOrCamera(className, source)) {
            source?.recycle()
            return
        }

        source?.recycle()

        val deltaX = event.scrollDeltaX
        val deltaY = event.scrollDeltaY

        // Filter out horizontal swipes (e.g. story carousels, tab swiping)
        if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 20) {
            return
        }

        // CRITICAL: Scrolling UP (backwards/upward swipe to previous content) must NEVER pull the curtain down!
        // In Android, deltaY < 0 indicates scrolling up.
        if (deltaY < -10) {
            Log.d(TAG, "Scroll ignored: User scrolled UP (dy=$deltaY)")
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
                Log.d(TAG, "Scroll ignored: Page did not advance forward ($lastIndex -> $fromIndex, dy=$deltaY)")
                return
            }
        } else {
            // deltaY near zero and no fromIndex: do NOT blindly add pixels
            return
        }

        Log.d(TAG, "Forward scroll applied on $packageName: +$pixelsToAdd px (dy=$deltaY, dx=$deltaX)")
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
               pkg == PKG_VENDING
    }

    /**
     * Intercepts uninstallation or force-stop attempts in Settings.
     * If Guardian PIN is enabled, requires parent / friend to unlock with PIN.
     */
    private fun checkAndApplyUninstallProtection(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        try {
            val textNodes = rootNode.findAccessibilityNodeInfosByText("ClawBack")
                .plus(rootNode.findAccessibilityNodeInfosByText("AntiScroller"))
            val pkgNodes = rootNode.findAccessibilityNodeInfosByText(packageName)

            if (textNodes.isNotEmpty() || pkgNodes.isNotEmpty()) {
                val uninstallNodes = rootNode.findAccessibilityNodeInfosByText("Uninstall")
                    .plus(rootNode.findAccessibilityNodeInfosByText("Force stop"))
                    .plus(rootNode.findAccessibilityNodeInfosByText("Clear storage"))

                if (uninstallNodes.isNotEmpty()) {
                    // Check if Guardian Lock is active and not currently unlocked by parent
                    if (dataManager.isGuardianLockEnabled && !dataManager.isMaintenanceModeActive()) {
                        mainHandler.post {
                            Toast.makeText(
                                applicationContext,
                                "🛡️ Protected by Guardian PIN! Parent / friend must unlock to modify or delete.",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(applicationContext, GuardianUnlockActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(intent)
                        }
                        return
                    }

                    // Otherwise apply standard smart friction
                    if (dataManager.isSmartFrictionEnabled && !dataManager.isMaintenanceModeActive()) {
                        mainHandler.post {
                            Toast.makeText(
                                applicationContext,
                                "🐾 Smart Friction: ClawBack protects your focus! All your other apps remain fully accessible.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        if (dataManager.isLimitReached()) {
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }
                    }
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
