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
     * Accurately detects if the user is scrolling inside a comments list.
     * Strictly verifies the scrolling container itself, not individual comment buttons on video posts.
     */
    private fun isCommentSection(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false

        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""

        // Never mistake the main video scrollers for comments!
        if (sourceId.contains("reel_recycler") ||
            sourceId.contains("clips_view_pager") ||
            sourceId.contains("vertical_view_pager") ||
            sourceId.contains("feed_recycler")) {
            return false
        }

        // Direct check on the scrolling container ID:
        if (sourceId.contains("comment_thread") ||
            sourceId.contains("comments_recycler") ||
            sourceId.contains("layout_comment") ||
            sourceId.contains("engagement_panel_id_comment") ||
            sourceId.contains("comment_rows")) {
            return true
        }

        // Check if the scroller's immediate parent (up to 4 levels) is explicitly a comment sheet container
        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        var isComment = false

        while (current != null && depth < 4) {
            val parentId = current.viewIdResourceName?.lowercase() ?: ""
            if (parentId.contains("reel_recycler") || parentId.contains("clips_view_pager")) {
                current.recycle()
                return false
            }
            if (parentId.contains("comment_thread") ||
                parentId.contains("comments_recycler") ||
                parentId.contains("engagement_panel_id_comment") ||
                parentId.contains("layout_comment_thread")) {
                isComment = true
                current.recycle()
                break
            }
            val parent = current.parent
            current.recycle()
            current = parent
            depth++
        }

        return isComment
    }

    /**
     * Specifically verifies if a scroll in YouTube is occurring inside YouTube Shorts.
     * Filters out YouTube Home feed, search results, subscriptions, and long-form video pages.
     */
    private fun isYouTubeShortsScroller(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false

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

        if (isShorts) return true

        // 4. Fallback check: is Shorts active in the root window?
        return isYouTubeShortsActive(rootInActiveWindow)
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

        // 1. Instagram: hide overlay in DMs/Camera, show overlay on Feed / Reels
        if (packageName == ScrollDataManager.PKG_INSTAGRAM) {
            currentForegroundPackage = packageName
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
            currentForegroundPackage = packageName
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
            currentForegroundPackage = packageName
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

        currentForegroundPackage = packageName
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
        if (isCommentSection(source)) {
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

        val screenHeight = resources.displayMetrics.heightPixels
        val absDeltaY = Math.abs(deltaY).toFloat()

        // If deltaY is non-zero, use it. If ViewPager fired scroll without reporting deltaY, record fractional reel swipe
        val pixelsToAdd = if (absDeltaY >= 20f) {
            absDeltaY
        } else {
            screenHeight * 0.35f
        }

        Log.d(TAG, "Scroll detected on $packageName: +$pixelsToAdd px (dy=$deltaY, dx=$deltaX)")
        val newDropPercentage = dataManager.addScrollPixels(pixelsToAdd, screenHeight)

        mainHandler.post {
            overlayManager.showOrUpdateOverlay(newDropPercentage)
        }
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
