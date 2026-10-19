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
    private var isInNonDoomscrollMode: Boolean = false

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
     * Determines whether the user is on a non-doomscroll surface:
     * - Direct Messages / Chat / Inbox
     * - Tab bar switching
     * - Profile / Account / Settings
     * - Story creator / Camera
     * - Share trays, Likers list, Audio pages
     */
    private fun isNonDoomscrollSurface(className: String, sourceNode: AccessibilityNodeInfo?): Boolean {
        // 1. DMs, Chat, Inbox, Threads, Messages
        if (className.contains("Direct", ignoreCase = true) ||
            className.contains("Message", ignoreCase = true) ||
            className.contains("Thread", ignoreCase = true) ||
            className.contains("Inbox", ignoreCase = true) ||
            className.contains("Chat", ignoreCase = true) ||
            className.contains("TransparentModalActivity", ignoreCase = true)) {
            return true
        }

        // 2. Profile / Account / Story Camera / Settings / Share / Audio
        if (className.contains("Profile", ignoreCase = true) ||
            className.contains("EditProfile", ignoreCase = true) ||
            className.contains("Account", ignoreCase = true) ||
            className.contains("Setting", ignoreCase = true) ||
            className.contains("StoryCamera", ignoreCase = true) ||
            className.contains("Share", ignoreCase = true) ||
            className.contains("Audio", ignoreCase = true) ||
            className.contains("Music", ignoreCase = true) ||
            className.contains("Liker", ignoreCase = true)) {
            return true
        }

        // 3. View IDs of tab bars, navigation bars, DMs, profiles, share trays
        val viewId = sourceNode?.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("direct") ||
            viewId.contains("thread") ||
            viewId.contains("message") ||
            viewId.contains("inbox") ||
            viewId.contains("chat") ||
            viewId.contains("composer") ||
            viewId.contains("tab_bar") ||
            viewId.contains("bottom_nav") ||
            viewId.contains("navigation_bar") ||
            viewId.contains("profile") ||
            viewId.contains("action_bar") ||
            viewId.contains("share") ||
            viewId.contains("audio") ||
            viewId.contains("sound") ||
            viewId.contains("liker") ||
            viewId.contains("like_count")) {
            return true
        }

        return false
    }

    /**
     * Detects if the current scroll event is happening inside a comments section.
     * Applies to both Instagram and YouTube:
     * - Instagram comment threads, bottom sheets, composer trays
     * - YouTube Shorts comment engagement panels, reply threads
     */
    private fun isCommentSection(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false

        // 1. Direct check on the scrolling node
        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""
        val sourceClass = sourceNode.className?.toString()?.lowercase() ?: ""
        val sourceDesc = sourceNode.contentDescription?.toString()?.lowercase() ?: ""

        if (sourceId.contains("comment") ||
            sourceId.contains("engagement_panel") ||
            sourceId.contains("reply") ||
            sourceId.contains("replies") ||
            sourceClass.contains("comment") ||
            sourceDesc.contains("comment")) {
            return true
        }

        // 2. Check direct children (e.g. comment item views inside recycler)
        val checkCount = sourceNode.childCount.coerceAtMost(6)
        for (i in 0 until checkCount) {
            val child = sourceNode.getChild(i) ?: continue
            val childId = child.viewIdResourceName?.lowercase() ?: ""
            val childDesc = child.contentDescription?.toString()?.lowercase() ?: ""
            val childText = child.text?.toString()?.lowercase() ?: ""
            val isComment = childId.contains("comment") ||
                            childId.contains("reply") ||
                            childId.contains("engagement_panel") ||
                            childDesc.contains("comment") ||
                            childText.contains("add a comment")
            child.recycle()
            if (isComment) return true
        }

        // 3. Walk up the ancestor hierarchy (up to 12 levels)
        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        var isCommentAncestor = false

        while (current != null && depth < 12) {
            val parentId = current.viewIdResourceName?.lowercase() ?: ""
            val parentClass = current.className?.toString()?.lowercase() ?: ""
            val parentDesc = current.contentDescription?.toString()?.lowercase() ?: ""

            if (parentId.contains("comment") ||
                parentId.contains("engagement_panel") ||
                parentId.contains("reply") ||
                parentId.contains("replies") ||
                parentClass.contains("comment") ||
                parentDesc.contains("comment")) {
                isCommentAncestor = true
                current.recycle()
                break
            }

            // Check if ancestor contains a "Comments" title or comment composer
            try {
                val commentHeaders = current.findAccessibilityNodeInfosByText("Comments")
                if (commentHeaders.isNotEmpty()) {
                    for (h in commentHeaders) h.recycle()
                    isCommentAncestor = true
                    current.recycle()
                    break
                }
            } catch (_: Exception) {}

            val nextParent = current.parent
            current.recycle()
            current = nextParent
            depth++
        }

        return isCommentAncestor
    }

    /**
     * Specifically verifies if a scroll in YouTube is occurring inside YouTube Shorts.
     * Filters out YouTube Home feed, search results, subscriptions, and long-form video pages.
     */
    private fun isYouTubeShortsScroller(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false

        // 1. Explicit check on source node
        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""
        val sourceClass = sourceNode.className?.toString()?.lowercase() ?: ""

        // Immediate rejection of Home feed / search / watch containers
        if (sourceId.contains("results_recycler_view") ||
            sourceId.contains("feed_filter_bar") ||
            sourceId.contains("pane_fragment") ||
            sourceId.contains("watch_while") ||
            sourceId.contains("elements_recycler_view") ||
            sourceId.contains("related_chips")) {
            return false
        }

        if (sourceId.contains("reel") || sourceId.contains("shorts") ||
            sourceClass.contains("reel") || sourceClass.contains("shorts")) {
            return true
        }

        // 2. Walk up the parent hierarchy (up to 12 levels) looking for Shorts / Reel container
        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        var isShorts = false

        while (current != null && depth < 12) {
            val parentId = current.viewIdResourceName?.lowercase() ?: ""
            val parentClass = current.className?.toString()?.lowercase() ?: ""

            if (parentId.contains("reel") || parentId.contains("shorts") ||
                parentClass.contains("reel") || parentClass.contains("shorts")) {
                isShorts = true
                current.recycle()
                break
            }

            // Reject if we hit the Home feed coordinator
            if (parentId.contains("results_recycler_view") ||
                parentId.contains("pane_fragment") ||
                parentId.contains("watch_while")) {
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

        // Check if user is on DMs or Profile in Instagram
        if (packageName == ScrollDataManager.PKG_INSTAGRAM && isNonDoomscrollSurface(className, event.source)) {
            isInNonDoomscrollMode = true
            mainHandler.post {
                overlayManager.hideOverlay()
            }
            return
        } else {
            isInNonDoomscrollMode = false
        }

        // On YouTube: Only show the Kitty Curtain when inside YouTube Shorts!
        // If user is on Home feed or watching normal long videos, keep curtain hidden.
        if (packageName == ScrollDataManager.PKG_YOUTUBE) {
            val root = rootInActiveWindow
            val inShorts = isYouTubeShortsActive(root)
            if (!inShorts) {
                isInNonDoomscrollMode = true
                mainHandler.post {
                    overlayManager.hideOverlay()
                }
                return
            } else {
                isInNonDoomscrollMode = false
            }
        }

        // 1. Target doomscroll apps: Instagram or YouTube
        if (dataManager.isPackageMonitored(packageName)) {
            currentForegroundPackage = packageName
            dataManager.checkAndPerformMidnightReset()

            val screenHeight = resources.displayMetrics.heightPixels
            val dropPercentage = dataManager.getDropPercentage(screenHeight)

            mainHandler.post {
                overlayManager.showOrUpdateOverlay(dropPercentage)
            }
            return
        }

        // 2. Settings / App Manager (Anti-Uninstall & Guardian Protection)
        if (isSettingsOrInstaller(packageName)) {
            currentForegroundPackage = packageName
            checkAndApplyUninstallProtection(event)
            mainHandler.post {
                overlayManager.hideOverlay()
            }
            return
        }

        // 3. User switched to another app (WhatsApp, Notion, Chrome, etc.)
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

        // 3. Instagram non-doomscroll surfaces (DMs, profile, share tray, likes list)
        val className = event.className?.toString() ?: ""
        if (isInNonDoomscrollMode || isNonDoomscrollSurface(className, source)) {
            source?.recycle()
            return
        }

        source?.recycle()

        val deltaX = event.scrollDeltaX
        val deltaY = event.scrollDeltaY

        // STRICT VERTICAL FILTER:
        // If deltaY is 0, or if horizontal movement dominates (tab switch, story carousel), IGNORE!
        if (deltaY == 0 || Math.abs(deltaX) >= Math.abs(deltaY)) {
            return
        }

        // Filter out micro touch tremors (< 20px)
        val absDeltaY = Math.abs(deltaY).toFloat()
        if (absDeltaY < 20f) {
            return
        }

        val screenHeight = resources.displayMetrics.heightPixels
        val newDropPercentage = dataManager.addScrollPixels(absDeltaY, screenHeight)

        mainHandler.post {
            overlayManager.showOrUpdateOverlay(newDropPercentage)
        }
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
