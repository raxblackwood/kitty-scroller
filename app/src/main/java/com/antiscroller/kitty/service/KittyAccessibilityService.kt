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
     * Specifically checks if any comments container, bottom sheet, or engagement panel is active on screen.
     */
    private fun isCommentOpenOnScreen(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        try {
            // 1. Instagram comments container / composer / recycler / sheet
            val igCommentIds = arrayOf(
                "com.instagram.android:id/layout_comment_thread",
                "com.instagram.android:id/layout_comment_thread_edittext",
                "com.instagram.android:id/comment_composer",
                "com.instagram.android:id/comment_composer_view",
                "com.instagram.android:id/comments_recycler_view",
                "com.instagram.android:id/row_comment_container",
                "com.instagram.android:id/comment_feed",
                "com.instagram.android:id/comment_sheet",
                "com.instagram.android:id/bottom_sheet_container"
            )
            for (id in igCommentIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (nodes.isNotEmpty()) {
                    for (n in nodes) n.recycle()
                    return true
                }
            }

            // 2. YouTube comments engagement panel / container (EXCLUDING button comments_entry_point)
            val ytCommentIds = arrayOf(
                "com.google.android.youtube:id/engagement_panel",
                "com.google.android.youtube:id/engagement_panel_wrapper",
                "com.google.android.youtube:id/comment_section",
                "com.google.android.youtube:id/comment_composer",
                "com.google.android.youtube:id/comment_thread",
                "com.google.android.youtube:id/comments_sheet"
            )
            for (id in ytCommentIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (nodes.isNotEmpty()) {
                    for (n in nodes) n.recycle()
                    return true
                }
            }

            // 3. Fallback text search for open comment sheet headers / composers
            val commentTexts = rootNode.findAccessibilityNodeInfosByText("Add a comment")
                .plus(rootNode.findAccessibilityNodeInfosByText("Add a public comment"))
            if (commentTexts.isNotEmpty()) {
                for (n in commentTexts) n.recycle()
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking comments on screen: ${e.message}")
        }
        return false
    }

    /**
     * Fast check to see if the scrolled view belongs to a comment section or bottom sheet.
     */
    private fun isCommentScroller(sourceNode: AccessibilityNodeInfo?): Boolean {
        if (sourceNode == null) return false
        val sourceId = sourceNode.viewIdResourceName?.lowercase() ?: ""
        val sourceCls = sourceNode.className?.toString()?.lowercase() ?: ""
        val sourceDesc = sourceNode.contentDescription?.toString()?.lowercase() ?: ""

        if (sourceId.contains("comment") || sourceId.contains("engagement_panel") ||
            sourceId.contains("bottom_sheet") || sourceId.contains("reply") ||
            sourceCls.contains("comment") || sourceDesc.contains("comment")) {
            return true
        }

        var current: AccessibilityNodeInfo? = sourceNode.parent
        var depth = 0
        var isComment = false
        while (current != null && depth < 8) {
            val pId = current.viewIdResourceName?.lowercase() ?: ""
            val pCls = current.className?.toString()?.lowercase() ?: ""
            val pDesc = current.contentDescription?.toString()?.lowercase() ?: ""

            if (pId.contains("comment") || pId.contains("engagement_panel") ||
                pId.contains("bottom_sheet") || pId.contains("reply") ||
                pCls.contains("comment") || pDesc.contains("comment")) {
                isComment = true
                current.recycle()
                break
            }
            val next = current.parent
            current.recycle()
            current = next
            depth++
        }
        return isComment
    }

    /**
     * Strictly verifies whether the user is in Instagram Direct Messages (chat thread or inbox).
     */
    private fun isInstagramDirectMessages(rootNode: AccessibilityNodeInfo?, sourceNode: AccessibilityNodeInfo?): Boolean {
        val sourceId = sourceNode?.viewIdResourceName?.lowercase() ?: ""
        if (sourceId.contains("direct") || sourceId.contains("thread") ||
            sourceId.contains("message") || sourceId.contains("inbox") ||
            sourceId.contains("recipient")) {
            return true
        }

        if (rootNode == null) return false
        try {
            val dmIds = arrayOf(
                "com.instagram.android:id/row_thread_composer",
                "com.instagram.android:id/message_composer",
                "com.instagram.android:id/direct_inbox",
                "com.instagram.android:id/inbox_action_bar",
                "com.instagram.android:id/direct_text_message_text_view",
                "com.instagram.android:id/layout_thread_title",
                "com.instagram.android:id/thread_title",
                "com.instagram.android:id/message_list",
                "com.instagram.android:id/direct_unified_inbox",
                "com.instagram.android:id/direct_message_list",
                "com.instagram.android:id/row_inbox_container",
                "com.instagram.android:id/inbox_recycler_view",
                "com.instagram.android:id/direct_recycler_view",
                "com.instagram.android:id/direct_search"
            )
            for (id in dmIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (nodes.isNotEmpty()) {
                    for (n in nodes) n.recycle()
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Instagram DM: ${e.message}")
        }
        return false
    }

    /**
     * Helper to verify if an Instagram navigation tab is currently selected.
     */
    private fun isInstagramTabSelected(rootNode: AccessibilityNodeInfo?, tabId: String, tabText: String): Boolean {
        if (rootNode == null) return false
        try {
            val tabsById = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/$tabId")
            for (t in tabsById) {
                val sel = t.isSelected || t.parent?.isSelected == true
                t.recycle()
                if (sel) return true
            }

            val tabsByText = rootNode.findAccessibilityNodeInfosByText(tabText)
            for (t in tabsByText) {
                val isTabNode = t.className?.contains("Tab", ignoreCase = true) == true ||
                                t.viewIdResourceName?.contains("tab", ignoreCase = true) == true ||
                                t.parent?.viewIdResourceName?.contains("tab", ignoreCase = true) == true
                val sel = t.isSelected || t.parent?.isSelected == true
                t.recycle()
                if (isTabNode && sel) return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Instagram tab $tabId: ${e.message}")
        }
        return false
    }

    /**
     * Whitelist Target 1: Instagram Homepage Feed.
     * Rejects DMs, Comments, Explore, Profile, and Stories.
     */
    private fun isInstagramHomeFeed(sourceNode: AccessibilityNodeInfo?, rootNode: AccessibilityNodeInfo?): Boolean {
        if (isInstagramDirectMessages(rootNode, sourceNode)) return false
        if (isCommentScroller(sourceNode) || isCommentOpenOnScreen(rootNode)) return false

        val sourceId = sourceNode?.viewIdResourceName?.lowercase() ?: ""

        // Hard reject if scroller belongs to explore, profile, reels, direct, or stories
        if (sourceId.contains("clips_view_pager") ||
            sourceId.contains("explore_recycler_view") ||
            sourceId.contains("profile_recycler_view") ||
            sourceId.contains("direct") ||
            sourceId.contains("story_viewer") ||
            sourceId.contains("camera")) {
            return false
        }

        // When sourceNode is available (during scroll events):
        if (sourceNode != null) {
            if (sourceId.contains("feed_recycler_view") || sourceId.contains("sticky_header_list")) {
                return true
            }

            var current: AccessibilityNodeInfo? = sourceNode.parent
            var depth = 0
            while (current != null && depth < 8) {
                val pId = current.viewIdResourceName?.lowercase() ?: ""
                if (pId.contains("explore") || pId.contains("profile") || pId.contains("clips") || pId.contains("direct")) {
                    current.recycle()
                    return false
                }
                if (pId.contains("feed_recycler_view") || pId.contains("sticky_header_list")) {
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

        // When sourceNode is null (e.g. window state change check):
        if (rootNode != null) {
            if (isInstagramTabSelected(rootNode, "feed_tab", "Home")) {
                if (!isInstagramTabSelected(rootNode, "search_tab", "Search") &&
                    !isInstagramTabSelected(rootNode, "profile_tab", "Profile") &&
                    !isInstagramTabSelected(rootNode, "reels_tab", "Reels")) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Whitelist Target 2: Instagram Reels (swiping reels).
     * Rejects Comments, DMs, Feed, Explore, and Stories.
     */
    private fun isInstagramReels(sourceNode: AccessibilityNodeInfo?, rootNode: AccessibilityNodeInfo?): Boolean {
        if (isInstagramDirectMessages(rootNode, sourceNode)) return false
        if (isCommentScroller(sourceNode) || isCommentOpenOnScreen(rootNode)) return false

        val sourceId = sourceNode?.viewIdResourceName?.lowercase() ?: ""

        // Hard reject feed, explore, profile, and direct scrollers
        if (sourceId.contains("feed_recycler_view") ||
            sourceId.contains("explore_recycler_view") ||
            sourceId.contains("profile_recycler_view") ||
            sourceId.contains("direct")) {
            return false
        }

        // When sourceNode is available (during scroll events):
        if (sourceNode != null) {
            if (sourceId.contains("clips_view_pager") ||
                sourceId.contains("reel_viewer") ||
                sourceId.contains("vertical_view_pager") ||
                sourceId.contains("clips_media_container") ||
                sourceId.contains("reel_recycler")) {
                return true
            }

            var current: AccessibilityNodeInfo? = sourceNode.parent
            var depth = 0
            while (current != null && depth < 8) {
                val pId = current.viewIdResourceName?.lowercase() ?: ""
                if (pId.contains("feed_recycler_view") || pId.contains("explore_recycler_view") || pId.contains("profile_recycler_view")) {
                    current.recycle()
                    return false
                }
                if (pId.contains("clips_view_pager") ||
                    pId.contains("reel_viewer") ||
                    pId.contains("vertical_view_pager") ||
                    pId.contains("clips_media_container") ||
                    pId.contains("reel_recycler")) {
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

        // When sourceNode is null (e.g. window state change check):
        if (rootNode != null) {
            val reelsView = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_view_pager")
                .plus(rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/reel_viewer"))
            if (reelsView.isNotEmpty()) {
                for (n in reelsView) n.recycle()
                return true
            }
            if (isInstagramTabSelected(rootNode, "reels_tab", "Reels") ||
                isInstagramTabSelected(rootNode, "clips_tab", "Clips")) {
                return true
            }
        }

        return false
    }

    /**
     * Whitelist Target 3: YouTube Shorts (swiping shorts).
     * STRICTLY rejects YouTube Homepage (results_recycler_view), Search, Subscriptions, and Comments.
     */
    private fun isYouTubeShorts(sourceNode: AccessibilityNodeInfo?, rootNode: AccessibilityNodeInfo?): Boolean {
        if (isCommentScroller(sourceNode) || isCommentOpenOnScreen(rootNode)) return false

        val sourceId = sourceNode?.viewIdResourceName?.lowercase() ?: ""

        // HARD REJECT: Homepage feed, search results, filter bars, watch panel, and elements recycler
        if (sourceId.contains("results_recycler_view") ||
            sourceId.contains("feed_filter_bar") ||
            sourceId.contains("elements_recycler_view") ||
            sourceId.contains("pane_fragment") ||
            sourceId.contains("watch_panel") ||
            sourceId.contains("player_view") ||
            sourceId.contains("watch_scroll_view") ||
            sourceId.contains("channel_page")) {
            return false
        }

        // When sourceNode is available (during scroll events):
        if (sourceNode != null) {
            if (sourceId.contains("reel_recycler") ||
                sourceId.contains("reel_watch_fragment") ||
                sourceId.contains("shorts_container")) {
                return true
            }

            var current: AccessibilityNodeInfo? = sourceNode.parent
            var depth = 0
            while (current != null && depth < 8) {
                val pId = current.viewIdResourceName?.lowercase() ?: ""
                if (pId.contains("results_recycler_view") || pId.contains("pane_fragment") || pId.contains("watch_panel")) {
                    current.recycle()
                    return false
                }
                if (pId.contains("reel_recycler") ||
                    pId.contains("reel_watch_fragment") ||
                    pId.contains("shorts_container") ||
                    pId.contains("reel_player_overlay")) {
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

        // When sourceNode is null (e.g. window state change check):
        if (rootNode != null) {
            val shortsNodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_recycler")
                .plus(rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/reel_watch_fragment"))
            if (shortsNodes.isNotEmpty()) {
                for (n in shortsNodes) n.recycle()
                return true
            }

            try {
                val shortsTabs = rootNode.findAccessibilityNodeInfosByText("Shorts")
                for (t in shortsTabs) {
                    val isTab = t.className?.contains("Tab", ignoreCase = true) == true ||
                                t.viewIdResourceName?.contains("pivot", ignoreCase = true) == true ||
                                t.parent?.viewIdResourceName?.contains("pivot", ignoreCase = true) == true
                    val sel = t.isSelected || t.parent?.isSelected == true
                    t.recycle()
                    if (isTab && sel) return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Shorts tab: ${e.message}")
            }
        }

        return false
    }

    private fun handleWindowStateChanged(packageName: String, event: AccessibilityEvent) {
        if (packageName != currentForegroundPackage) {
            currentForegroundPackage = packageName
            lastInstagramIndex = -1
            lastYouTubeIndex = -1
        }

        // 1. Instagram: ONLY show overlay on Home feed or Reels (hide in DMs, Comments, Explore, Profile)
        if (packageName == ScrollDataManager.PKG_INSTAGRAM) {
            dataManager.checkAndPerformMidnightReset()

            val root = rootInActiveWindow
            val isTargetActive = (isInstagramHomeFeed(null, root) || isInstagramReels(null, root)) &&
                                 !isCommentOpenOnScreen(root) &&
                                 !isInstagramDirectMessages(root, null)
            root?.recycle()

            if (!isTargetActive) {
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

        // 2. YouTube: ONLY show overlay on Shorts (hide on Homepage, Subscriptions, Search, Comments)
        if (packageName == ScrollDataManager.PKG_YOUTUBE) {
            dataManager.checkAndPerformMidnightReset()

            val root = rootInActiveWindow
            val inShorts = isYouTubeShorts(null, root) && !isCommentOpenOnScreen(root)
            root?.recycle()

            if (!inShorts) {
                mainHandler.post {
                    overlayManager.hideOverlay()
                }
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
        val root = rootInActiveWindow

        // STRICT WHITELIST: ONLY 3 ALLOWED TARGETS (Instagram Home Feed, Instagram Reels, YouTube Shorts)
        val isAllowed = when (packageName) {
            ScrollDataManager.PKG_INSTAGRAM -> {
                isInstagramHomeFeed(source, root) || isInstagramReels(source, root)
            }
            ScrollDataManager.PKG_YOUTUBE -> {
                isYouTubeShorts(source, root)
            }
            else -> false
        }

        root?.recycle()

        if (!isAllowed) {
            Log.d(TAG, "Scroll ignored: Not on allowed target screen (pkg=$packageName)")
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

            val root = rootInActiveWindow
            val inShorts = isYouTubeShorts(source, root)
            root?.recycle()
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
