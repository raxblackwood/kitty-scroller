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
     */
    private fun isNonDoomscrollSurface(className: String, sourceNode: AccessibilityNodeInfo?): Boolean {
        // 1. DMs, Chat, Inbox, Threads
        if (className.contains("Direct", ignoreCase = true) ||
            className.contains("Message", ignoreCase = true) ||
            className.contains("Thread", ignoreCase = true) ||
            className.contains("Inbox", ignoreCase = true) ||
            className.contains("Chat", ignoreCase = true) ||
            className.contains("TransparentModalActivity", ignoreCase = true)) {
            return true
        }

        // 2. Profile / Account / Story Camera / Settings
        if (className.contains("Profile", ignoreCase = true) ||
            className.contains("EditProfile", ignoreCase = true) ||
            className.contains("Account", ignoreCase = true) ||
            className.contains("Setting", ignoreCase = true) ||
            className.contains("StoryCamera", ignoreCase = true)) {
            return true
        }

        // 3. View IDs of tab bars, navigation bars, DMs, profiles
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
            viewId.contains("action_bar")) {
            return true
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

        // DMs, tab bar switches, and profile must NEVER count as doomscrolling
        val className = event.className?.toString() ?: ""
        if (isInNonDoomscrollMode || isNonDoomscrollSurface(className, event.source)) {
            return
        }

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
            val textNodes = rootNode.findAccessibilityNodeInfosByText("AntiScroller")
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
                                "🐾 Smart Friction: AntiScroller protects your focus! All your other apps remain fully accessible.",
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
