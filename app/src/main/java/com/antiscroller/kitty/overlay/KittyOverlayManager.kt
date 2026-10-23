package com.antiscroller.kitty.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.ui.KittyCurtainView

/**
 * Manages the floating WindowManager overlay that shows the kitty sliding down.
 * Ensures the overlay stays persistently on screen in Instagram / YouTube Shorts,
 * with touch passthrough until 100% doomscroll lock is reached.
 */
class KittyOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "KittyOverlayManager"

        @Volatile
        private var instance: KittyOverlayManager? = null

        fun getInstance(context: Context): KittyOverlayManager {
            return instance ?: synchronized(this) {
                instance ?: KittyOverlayManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var kittyCurtainView: KittyCurtainView? = null
    private var isOverlayAttached = false
    private var currentDropPercentage: Float = 0f
    private var lastAppliedTheme: String = ""

    fun canDrawOverlay(): Boolean {
        if (Settings.canDrawOverlays(context)) return true
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps?.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps?.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Shows or updates the Kitty Curtain overlay with the user's selected kitty breed.
     */
    @Synchronized
    fun showOrUpdateOverlay(dropPercentage: Float) {
        if (!canDrawOverlay()) {
            Log.w(TAG, "Cannot draw overlay: permission not granted.")
            return
        }

        val theme = ScrollDataManager.getInstance(context).kittyTheme
        currentDropPercentage = dropPercentage.coerceIn(0f, 1f)

        mainHandler.post {
            if (!isOverlayAttached || overlayView == null) {
                attachOverlay()
            }
            if (theme != lastAppliedTheme) {
                lastAppliedTheme = theme
                kittyCurtainView?.applyTheme(theme)
            }
            kittyCurtainView?.setDropPercentage(currentDropPercentage, animate = true)
            updateTouchFlags(isFullScreenCovered = currentDropPercentage >= 0.98f)
        }
    }

    /**
     * Hides and detaches the overlay when leaving target apps.
     */
    @Synchronized
    fun hideOverlay() {
        mainHandler.post {
            if (isOverlayAttached && overlayView != null) {
                try {
                    windowManager.removeView(overlayView)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing overlay view: ${e.message}")
                }
                isOverlayAttached = false
                overlayView = null
                kittyCurtainView = null
                lastAppliedTheme = ""
                lastFullScreenCovered = null
            }
        }
    }

    private fun attachOverlay() {
        try {
            val curtain = KittyCurtainView(context)
            val theme = ScrollDataManager.getInstance(context).kittyTheme
            curtain.applyTheme(theme)

            kittyCurtainView = curtain
            overlayView = curtain

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                // Touch passthrough so user can still interact until kitty covers the screen
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            windowManager.addView(curtain, layoutParams)
            isOverlayAttached = true
            Log.i(TAG, "Kitty Curtain overlay attached successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay: ${e.message}")
        }
    }

    private var lastFullScreenCovered: Boolean? = null

    private fun updateTouchFlags(isFullScreenCovered: Boolean) {
        if (lastFullScreenCovered == isFullScreenCovered) return
        lastFullScreenCovered = isFullScreenCovered

        val view = overlayView ?: return
        try {
            val params = view.layoutParams as? WindowManager.LayoutParams ?: return
            if (isFullScreenCovered) {
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
            } else {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            }
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay flags: ${e.message}")
        }
    }

    fun isAttached(): Boolean = isOverlayAttached
}
