package com.antiscroller.kitty

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.antiscroller.kitty.admin.KittyAdminReceiver
import com.antiscroller.kitty.billing.BillingManager
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.databinding.ActivityMainBinding
import com.antiscroller.kitty.guardian.GuardianAlertManager
import com.antiscroller.kitty.overlay.KittyOverlayManager
import com.antiscroller.kitty.service.KittyAccessibilityService
import com.antiscroller.kitty.ui.PaywallBottomSheetDialog
import com.antiscroller.kitty.worker.DailyResetWorker
import com.antiscroller.kitty.worker.GuardianHeartbeatWorker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dataManager: ScrollDataManager
    private lateinit var overlayManager: KittyOverlayManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var billingManager: BillingManager

    private var previewedTheme: String = ScrollDataManager.THEME_GINGER
    private var isPreviewCurtainClosed: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private var activeSetupDialogUpdate: (() -> Unit)? = null
    private var initialSetupDialog: androidx.appcompat.app.AlertDialog? = null
    private var permissionSetupDialog: androidx.appcompat.app.AlertDialog? = null
    private var appOpsListener: AppOpsManager.OnOpChangedListener? = null
    private var activeBlurCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right,
                bottom = insets.bottom
            )
            windowInsets
        }

        dataManager = ScrollDataManager.getInstance(this)
        overlayManager = KittyOverlayManager.getInstance(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, KittyAdminReceiver::class.java)

        billingManager = BillingManager(this) { _, msg ->
            runOnUiThread {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                updateThemeChips()
                updateGuardianChildModeUI()
                updateEquipButtonState()
            }
        }

        DailyResetWorker.scheduleMidnightReset(this)

        try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val listener = AppOpsManager.OnOpChangedListener { _, pkg ->
                if (pkg == packageName) {
                    runOnUiThread {
                        checkPermissions()
                        activeSetupDialogUpdate?.invoke()
                    }
                }
            }
            appOpsListener = listener
            appOps?.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, packageName, listener)
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to register AppOps watcher", e)
        }

        setupUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
        checkPermissions()
        activeSetupDialogUpdate?.invoke()
        // Re-check after returning from Settings to overcome Binder IPC propagation delay
        handler.postDelayed({
            checkPermissions()
            activeSetupDialogUpdate?.invoke()
        }, 200)
        handler.postDelayed({
            checkPermissions()
            activeSetupDialogUpdate?.invoke()
        }, 500)
        handler.postDelayed({
            checkPermissions()
            activeSetupDialogUpdate?.invoke()
        }, 1000)
        updateThemeChips()
        updateGuardianChildModeUI()
        updateGuardianUI()
        updateDeviceOwnerUI()

        if (!dataManager.isOnboardingCompleted) {
            showOnboardingCarousel()
        } else if (!dataManager.isInitialSetupDone) {
            showInitialSetupDialog()
        }
    }

    override fun onDestroy() {
        appOpsListener?.let {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            try {
                appOps?.stopWatchingMode(it)
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to unregister AppOps watcher", e)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.root.setRenderEffect(null)
        }
        activeBlurCount = 0
        super.onDestroy()
        initialSetupDialog?.dismiss()
        initialSetupDialog = null
        permissionSetupDialog?.dismiss()
        permissionSetupDialog = null
        billingManager.endConnection()
    }

    private fun setupUI() {
        previewedTheme = dataManager.kittyTheme
        binding.previewCurtain.applyTheme(previewedTheme)
        updateThemeChips()
        updateEquipButtonState()

        // By default show the cat that comes down (curtain coming down) for all companion skins
        isPreviewCurtainClosed = false
        binding.previewCurtain.setDropPercentage(0.40f, animate = false)
        updatePreviewModeBadge()

        binding.etCustomLimit.setText(dataManager.maxDailyScrolls.toString())
        binding.tvCurrentLimitLabel.text = "Active Cap: ${dataManager.maxDailyScrolls} Reels / day"

        binding.switchInstagram.tag = "PROGRAMMATIC"
        binding.switchInstagram.isChecked = dataManager.isInstagramMonitored
        binding.switchInstagram.tag = null

        binding.switchYouTube.tag = "PROGRAMMATIC"
        binding.switchYouTube.isChecked = dataManager.isYouTubeMonitored
        binding.switchYouTube.tag = null

        updateGuardianChildModeUI()
        updateGuardianUI()
    }

    private fun setupListeners() {
        // Unlock Cosmetics Banner
        binding.btnUnlockCosmeticsBanner.setOnClickListener {
            showPaywallDialog(BillingManager.PRODUCT_COSMETIC_PACK)
        }

        // Kitty Breed Choosing (Live Preview)
        binding.chipGroupKittyTheme.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val chosenTheme = when (checkedIds.first()) {
                R.id.chipThemeTabby -> ScrollDataManager.THEME_TABBY
                R.id.chipThemeSaturn -> ScrollDataManager.THEME_SATURN
                R.id.chipThemeRolex -> ScrollDataManager.THEME_ROLEX
                R.id.chipThemeSalmon -> ScrollDataManager.THEME_SALMON
                R.id.chipThemeKatrina -> ScrollDataManager.THEME_KATRINA
                else -> ScrollDataManager.THEME_GINGER
            }
            if (chosenTheme != previewedTheme) {
                previewedTheme = chosenTheme
                binding.previewCurtain.applyTheme(previewedTheme)
                updateEquipButtonState()
                val targetDrop = if (isPreviewCurtainClosed) 1.0f else 0.40f
                binding.previewCurtain.setDropPercentage(targetDrop, animate = false)
            }
        }

        // Tap avatar preview to toggle between curtain coming down and full closed curtain
        val togglePreviewAction = View.OnClickListener {
            isPreviewCurtainClosed = !isPreviewCurtainClosed
            val targetDrop = if (isPreviewCurtainClosed) 1.0f else 0.40f
            binding.previewCurtain.setDropPercentage(targetDrop, animate = true)
            updatePreviewModeBadge()
        }
        binding.previewCurtain.setOnClickListener(togglePreviewAction)
        binding.tvPreviewModeBadge.setOnClickListener(togglePreviewAction)

        // Equip or Unlock Action Button
        binding.btnEquipOrUnlock.setOnClickListener {
            val isUnlocked = dataManager.isThemeUnlocked(previewedTheme)
            if (!isUnlocked) {
                showPaywallDialog(BillingManager.PRODUCT_COSMETIC_PACK)
            } else {
                if (dataManager.kittyTheme != previewedTheme) {
                    dataManager.kittyTheme = previewedTheme
                    val breedName = getBreedDisplayName(previewedTheme)
                    Toast.makeText(this, "$breedName equipped as active companion!", Toast.LENGTH_SHORT).show()
                    updateEquipButtonState()
                    updateThemeChips()
                }
            }
        }

        // Guardian Child Protection Pipeline Buttons
        binding.btnConfigGuardianEndpoint.setOnClickListener {
            showConfigureGuardianEndpointDialog()
        }

        binding.btnTestGuardianAlert.setOnClickListener {
            val endpoint = dataManager.guardianAlertEndpoint
            if (endpoint.isEmpty()) {
                Toast.makeText(this, "Please configure an alert endpoint first!", Toast.LENGTH_SHORT).show()
                showConfigureGuardianEndpointDialog()
                return@setOnClickListener
            }
            Toast.makeText(this, "Sending test ping to guardian endpoint...", Toast.LENGTH_SHORT).show()
            GuardianAlertManager.sendAlert(this, GuardianAlertManager.EVENT_SETUP_TEST) { _, msg ->
                Toast.makeText(this, "Test Ping: $msg", Toast.LENGTH_LONG).show()
            }
        }


        // Header status badge and three-bar menu
        binding.badgeHeaderStatus.setOnClickListener {
            showPermissionSetupDialog()
        }
        binding.btnMenu.setOnClickListener {
            showMainMenuSheet()
        }

        // Floating permission notification banner
        binding.cardPermissionFloater.setOnClickListener {
            showPermissionSetupDialog()
        }
        binding.btnEnablePermissionsFloater.setOnClickListener {
            showPermissionSetupDialog()
        }

        // Custom Limit Set Button
        binding.btnSetCustomLimit.setOnClickListener {
            val text = binding.etCustomLimit.text?.toString()?.trim() ?: ""
            val parsed = text.toIntOrNull()
            if (parsed == null) {
                Toast.makeText(this, "Please type a valid number of reels.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            attemptChangeDailyLimit(parsed)
        }

        // Target apps toggles with anti-impulse cooling-off protection
        binding.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchInstagram.tag == "PROGRAMMATIC") return@setOnCheckedChangeListener
            if (isChecked) {
                dataManager.isInstagramMonitored = true
                dataManager.pendingDisableInstagram = false
                Toast.makeText(this, "Instagram Reels protection active.", Toast.LENGTH_SHORT).show()
            } else {
                if (checkGuardianLocked()) {
                    binding.switchInstagram.tag = "PROGRAMMATIC"
                    binding.switchInstagram.isChecked = true
                    binding.switchInstagram.tag = null
                    return@setOnCheckedChangeListener
                }
                if (dataManager.isMaintenanceModeActive()) {
                    dataManager.isInstagramMonitored = false
                    dataManager.pendingDisableInstagram = false
                    Toast.makeText(this, "Instagram monitoring disabled by Guardian.", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }

                // Solo mode: Revert switch back to ON and offer cooling-off schedule
                binding.switchInstagram.tag = "PROGRAMMATIC"
                binding.switchInstagram.isChecked = true
                binding.switchInstagram.tag = null

                if (dataManager.isLimitReached()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("🔒 Today's Limit Reached")
                        .setMessage("You have completed your daily scroll budget today!\n\nTo break compulsive doomscrolling loops, you cannot turn off app protection to bypass the curtain. Protection stays active until tomorrow at midnight (00:00).")
                        .setPositiveButton("I'll Respect My Limit", null)
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("🛡️ Anti-Doomscroll Cooling-Off")
                        .setMessage("To stop impulsive binge scrolling, turning off protection for Instagram cannot be done instantly during an active day.\n\nWould you like to schedule turning off protection starting tomorrow at midnight (00:00)?")
                        .setPositiveButton("Schedule for Tomorrow") { _, _ ->
                            dataManager.pendingDisableInstagram = true
                            Toast.makeText(this, "Protection stays active today and will turn off tomorrow at midnight.", Toast.LENGTH_LONG).show()
                        }
                        .setNegativeButton("Keep Active Today", null)
                        .show()
                }
            }
        }

        binding.switchYouTube.setOnCheckedChangeListener { _, isChecked ->
            if (binding.switchYouTube.tag == "PROGRAMMATIC") return@setOnCheckedChangeListener
            if (isChecked) {
                dataManager.isYouTubeMonitored = true
                dataManager.pendingDisableYouTube = false
                Toast.makeText(this, "YouTube Shorts protection active.", Toast.LENGTH_SHORT).show()
            } else {
                if (checkGuardianLocked()) {
                    binding.switchYouTube.tag = "PROGRAMMATIC"
                    binding.switchYouTube.isChecked = true
                    binding.switchYouTube.tag = null
                    return@setOnCheckedChangeListener
                }
                if (dataManager.isMaintenanceModeActive()) {
                    dataManager.isYouTubeMonitored = false
                    dataManager.pendingDisableYouTube = false
                    Toast.makeText(this, "YouTube monitoring disabled by Guardian.", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }

                // Solo mode: Revert switch back to ON and offer cooling-off schedule
                binding.switchYouTube.tag = "PROGRAMMATIC"
                binding.switchYouTube.isChecked = true
                binding.switchYouTube.tag = null

                if (dataManager.isLimitReached()) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("🔒 Today's Limit Reached")
                        .setMessage("You have completed your daily scroll budget today!\n\nTo break compulsive doomscrolling loops, you cannot turn off app protection to bypass the curtain. Protection stays active until tomorrow at midnight (00:00).")
                        .setPositiveButton("I'll Respect My Limit", null)
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("🛡️ Anti-Doomscroll Cooling-Off")
                        .setMessage("To stop impulsive binge scrolling, turning off protection for YouTube Shorts cannot be done instantly during an active day.\n\nWould you like to schedule turning off protection starting tomorrow at midnight (00:00)?")
                        .setPositiveButton("Schedule for Tomorrow") { _, _ ->
                            dataManager.pendingDisableYouTube = true
                            Toast.makeText(this, "Protection stays active today and will turn off tomorrow at midnight.", Toast.LENGTH_LONG).show()
                        }
                        .setNegativeButton("Keep Active Today", null)
                        .show()
                }
            }
        }

        // Guardian PIN Controls
        binding.btnSetGuardianPin.setOnClickListener {
            showSetPinDialog()
        }

        binding.btnGuardianResetToday.setOnClickListener {
            dataManager.resetDailyCount()
            updateDashboard()
            Toast.makeText(this, "Today's scroll counter reset by Guardian.", Toast.LENGTH_SHORT).show()
        }

        binding.btnUnlockGuardian.setOnClickListener {
            if (dataManager.isMaintenanceModeActive()) {
                // Currently unlocked -> Lock now
                dataManager.lockMaintenanceNow()
                updateGuardianUI()
                Toast.makeText(this, "🔒 Settings locked now.", Toast.LENGTH_SHORT).show()
            } else {
                // Locked -> Prompt PIN to unlock
                showUnlockPinDialog()
            }
        }

        // Hardcore Device Owner Mode ADB command copy
        binding.btnDeviceOwnerAdb.setOnClickListener {
            val adbCmd = "adb shell dpm set-device-owner $packageName/com.antiscroller.kitty.admin.KittyAdminReceiver"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Clawse ADB Command", adbCmd)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "ADB command copied to clipboard!", Toast.LENGTH_SHORT).show()

            MaterialAlertDialogBuilder(this)
                .setTitle("Hardcore Un-uninstallable Mode")
                .setMessage(
                    "Command copied to clipboard!\n\n" +
                    "To activate OS-level uninstallation block via ADB:\n\n" +
                    "1. Connect phone to PC with USB Debugging enabled.\n" +
                    "2. Run in terminal:\n$adbCmd\n\n" +
                    "Note: Ensure no other secondary work profiles exist before running."
                )
                .setPositiveButton("Got It", null)
                .show()
        }
    }

    private fun updateDeviceOwnerUI() {
        val isDeviceOwner = try {
            devicePolicyManager.isDeviceOwnerApp(packageName)
        } catch (e: Exception) {
            false
        }

        if (isDeviceOwner) {
            try {
                devicePolicyManager.setUninstallBlocked(adminComponent, packageName, true)
            } catch (e: Exception) {
                // Ignore if not allowed
            }
            binding.tvDeviceOwnerStatus.text = "Status: 🛡️ Active (OS-Level Uninstall Blocked)"
            binding.tvDeviceOwnerStatus.setTextColor(getColor(R.color.accent_green))
            binding.btnDeviceOwnerAdb.text = "Hardcore Mode Active ✅"
            binding.btnDeviceOwnerAdb.isEnabled = false
            binding.btnDeviceOwnerAdb.alpha = 0.7f
        } else {
            binding.tvDeviceOwnerStatus.text = "Status: Standard Protection"
            binding.tvDeviceOwnerStatus.setTextColor(getColor(R.color.text_secondary))
            binding.btnDeviceOwnerAdb.text = "Copy ADB Setup Command"
            binding.btnDeviceOwnerAdb.isEnabled = true
            binding.btnDeviceOwnerAdb.alpha = 1.0f
        }
    }

    private fun checkGuardianLocked(): Boolean {
        if (dataManager.isGuardianLockEnabled && !dataManager.isMaintenanceModeActive()) {
            Toast.makeText(
                this,
                "🔒 Locked by Guardian PIN. Tap 'Unlock (5m)' in Guardian section to make changes.",
                Toast.LENGTH_LONG
            ).show()
            return true
        }
        return false
    }

    private fun getBreedDisplayName(theme: String): String {
        return when (theme) {
            ScrollDataManager.THEME_TABBY, ScrollDataManager.THEME_BLACK -> "Tabby 🐈‍⬛"
            ScrollDataManager.THEME_SATURN -> "Saturn 🐱"
            ScrollDataManager.THEME_ROLEX -> "Rolex 🕶️"
            ScrollDataManager.THEME_SALMON -> "Salmon ⚔️"
            ScrollDataManager.THEME_KATRINA -> "Katrina 🌸"
            else -> "Ginger 🍊"
        }
    }

    private fun updatePreviewModeBadge() {
        if (isPreviewCurtainClosed) {
            binding.tvPreviewModeBadge.text = "🌙 Closed (Tap to see hanging cat)"
        } else {
            binding.tvPreviewModeBadge.text = "🐾 Curtain Down (Tap to see closed)"
        }
    }

    private fun updateEquipButtonState() {
        val isEquipped = (previewedTheme == dataManager.kittyTheme)
        val isUnlocked = dataManager.isThemeUnlocked(previewedTheme)
        val breedName = getBreedDisplayName(previewedTheme)

        when {
            isEquipped -> {
                binding.btnEquipOrUnlock.text = "✓ Currently Equipped ($breedName)"
                binding.btnEquipOrUnlock.isEnabled = false
                binding.btnEquipOrUnlock.setBackgroundColor(getColor(R.color.surface_variant))
                binding.btnEquipOrUnlock.setTextColor(getColor(R.color.text_secondary))
            }
            isUnlocked -> {
                binding.btnEquipOrUnlock.text = "Equip $breedName"
                binding.btnEquipOrUnlock.isEnabled = true
                binding.btnEquipOrUnlock.setBackgroundColor(getColor(R.color.secondary))
                binding.btnEquipOrUnlock.setTextColor(getColor(R.color.on_secondary))
            }
            else -> {
                binding.btnEquipOrUnlock.text = "✨ Unlock $breedName to Use"
                binding.btnEquipOrUnlock.isEnabled = true
                binding.btnEquipOrUnlock.setBackgroundColor(getColor(R.color.primary))
                binding.btnEquipOrUnlock.setTextColor(getColor(R.color.on_primary))
            }
        }
    }

    private fun updateThemeChips() {
        val isUnlocked = dataManager.isCosmeticPackUnlocked
        binding.btnUnlockCosmeticsBanner.visibility = if (isUnlocked) View.GONE else View.VISIBLE

        binding.chipThemeGinger.text = if (isUnlocked) "🍊 Ginger" else "🍊 Ginger (Free)"
        binding.chipThemeTabby.text = if (isUnlocked) "🐈‍⬛ Tabby" else "🔒 Tabby"
        binding.chipThemeSaturn.text = if (isUnlocked) "🐱 Saturn" else "🔒 Saturn"
        binding.chipThemeRolex.text = if (isUnlocked) "🕶️ Rolex" else "🔒 Rolex"
        binding.chipThemeSalmon.text = if (isUnlocked) "⚔️ Salmon" else "🔒 Salmon"
        binding.chipThemeKatrina.text = if (isUnlocked) "🌸 Katrina" else "🔒 Katrina"

        val targetChipId = when (previewedTheme) {
            ScrollDataManager.THEME_TABBY, ScrollDataManager.THEME_BLACK -> R.id.chipThemeTabby
            ScrollDataManager.THEME_SATURN -> R.id.chipThemeSaturn
            ScrollDataManager.THEME_ROLEX -> R.id.chipThemeRolex
            ScrollDataManager.THEME_SALMON -> R.id.chipThemeSalmon
            ScrollDataManager.THEME_KATRINA -> R.id.chipThemeKatrina
            else -> R.id.chipThemeGinger
        }
        if (binding.chipGroupKittyTheme.checkedChipId != targetChipId) {
            binding.chipGroupKittyTheme.check(targetChipId)
        }

        updateEquipButtonState()
    }

    private fun updateGuardianChildModeUI() {
        val isSubscribed = dataManager.isParentalSubscribed
        binding.tvGuardianSubBadge.text = if (isSubscribed) "ACTIVE" else "PREMIUM"

        binding.switchChildMode.setOnCheckedChangeListener(null)
        binding.switchChildMode.isChecked = dataManager.isChildModeEnabled
        binding.switchChildMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !dataManager.isParentalSubscribed) {
                binding.switchChildMode.isChecked = false
                Toast.makeText(this, "Subscription required for Child Phone Guardian Alerts", Toast.LENGTH_LONG).show()
                showPaywallDialog(BillingManager.PRODUCT_PARENTAL_MONTHLY)
                return@setOnCheckedChangeListener
            }

            if (isChecked && dataManager.guardianAlertEndpoint.isEmpty()) {
                binding.switchChildMode.isChecked = false
                showConfigureGuardianEndpointDialog {
                    if (dataManager.guardianAlertEndpoint.isNotEmpty()) {
                        dataManager.isChildModeEnabled = true
                        binding.switchChildMode.isChecked = true
                        GuardianHeartbeatWorker.scheduleHeartbeat(this)
                        updateGuardianChildModeUI()
                    }
                }
                return@setOnCheckedChangeListener
            }

            dataManager.isChildModeEnabled = isChecked
            if (isChecked) {
                GuardianHeartbeatWorker.scheduleHeartbeat(this)
                Toast.makeText(this, "🛡️ Child Phone Protection active! Guardian will be alerted on uninstall attempts.", Toast.LENGTH_LONG).show()
            } else {
                GuardianHeartbeatWorker.cancelHeartbeat(this)
                Toast.makeText(this, "Child Phone Protection paused.", Toast.LENGTH_SHORT).show()
            }
            updateGuardianChildModeUI()
        }

        val ep = dataManager.guardianAlertEndpoint
        val device = dataManager.childDeviceName
        binding.tvGuardianEndpointSummary.text = if (ep.isEmpty()) {
            "Endpoint: Not configured (Tap Configure below)"
        } else {
            "Alert Target: $ep ($device)"
        }
    }

    private fun showConfigureGuardianEndpointDialog(onSaved: (() -> Unit)? = null) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
        }

        val tvEpLabel = TextView(this).apply {
            text = "Guardian Webhook, ntfy topic, or Email:"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 12f
        }
        val etEndpoint = EditText(this).apply {
            hint = "e.g. https://discord.com/api/webhooks/... or alerts_topic"
            setText(dataManager.guardianAlertEndpoint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }

        val tvDeviceLabel = TextView(this).apply {
            text = "Child Device Identifier:"
            setTextColor(getColor(R.color.text_secondary))
            textSize = 12f
            setPadding(0, 24, 0, 0)
        }
        val etDeviceName = EditText(this).apply {
            hint = "e.g. Timmy's Phone"
            setText(dataManager.childDeviceName)
            inputType = InputType.TYPE_CLASS_TEXT
        }

        layout.addView(tvEpLabel)
        layout.addView(etEndpoint)
        layout.addView(tvDeviceLabel)
        layout.addView(etDeviceName)

        MaterialAlertDialogBuilder(this)
            .setTitle("Guardian Alert Endpoint")
            .setMessage("Set where uninstallation and tamper alerts should be sent in real-time.\n\nTip: Enter any topic name (e.g. mytestalerts) to view live alerts on https://ntfy.sh/mytestalerts without signing up!")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val ep = etEndpoint.text.toString().trim()
                val dev = etDeviceName.text.toString().trim()
                dataManager.guardianAlertEndpoint = ep
                if (dev.isNotEmpty()) {
                    dataManager.childDeviceName = dev
                }
                updateGuardianChildModeUI()
                onSaved?.invoke()
                Toast.makeText(this, "Guardian alert configuration saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPaywallDialog(targetProduct: String? = null) {
        val dialog = PaywallBottomSheetDialog.newInstance(targetProduct)
        dialog.setBillingManager(billingManager)
        dialog.onPurchaseCompletedListener = {
            updateThemeChips()
            updateGuardianChildModeUI()
        }
        dialog.show(supportFragmentManager, PaywallBottomSheetDialog.TAG)
    }

    private fun updateGuardianUI() {
        val isUnlocked = dataManager.isMaintenanceModeActive()
        binding.llGuardianProtectedSettings.visibility = if (isUnlocked) View.VISIBLE else View.GONE

        if (!dataManager.isGuardianLockEnabled) {
            binding.tvGuardianStatus.text = "Status: Inactive (Tap to set Parent/Partner PIN)"
            binding.tvGuardianStatus.setTextColor(getColor(R.color.text_secondary))
            binding.btnSetGuardianPin.text = "Set PIN"
            binding.btnUnlockGuardian.visibility = View.GONE
            setSettingsLocked(false)
        } else {
            val isUnlocked = dataManager.isMaintenanceModeActive()
            if (isUnlocked) {
                val mins = (dataManager.getRemainingMaintenanceSeconds() / 60) + 1
                binding.tvGuardianStatus.text = "Status: 🔓 Unlocked by Parent/Partner (${mins}m remaining)"
                binding.tvGuardianStatus.setTextColor(getColor(R.color.accent_green))
                binding.btnSetGuardianPin.text = "Change PIN"
                binding.btnUnlockGuardian.visibility = View.VISIBLE
                binding.btnUnlockGuardian.text = "Lock Now"
                binding.btnUnlockGuardian.setBackgroundColor(getColor(R.color.accent_red))
                setSettingsLocked(false)
            } else {
                binding.tvGuardianStatus.text = "Status: 🔒 Locked by Guardian PIN"
                binding.tvGuardianStatus.setTextColor(getColor(R.color.primary))
                binding.btnSetGuardianPin.text = "Change PIN"
                binding.btnUnlockGuardian.visibility = View.VISIBLE
                binding.btnUnlockGuardian.text = "Unlock (5m)"
                binding.btnUnlockGuardian.setBackgroundColor(getColor(R.color.secondary))
                setSettingsLocked(true)
            }
        }
    }

    private fun setSettingsLocked(locked: Boolean) {
        val alpha = if (locked) 0.55f else 1.0f
        binding.tilCustomLimit.alpha = alpha
        binding.btnSetCustomLimit.alpha = alpha
        binding.btnSetCustomLimit.isEnabled = !locked
        binding.switchInstagram.alpha = alpha
        binding.switchYouTube.alpha = alpha
    }

    private fun showSetPinDialog() {
        if (dataManager.isGuardianLockEnabled) {
            showPromptDialog("Enter Current Guardian PIN", isPassword = true) { currentPin ->
                if (dataManager.verifyPin(currentPin)) {
                    showNewPinPromptDialog()
                } else {
                    Toast.makeText(this, "Incorrect current PIN!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            showNewPinPromptDialog()
        }
    }

    private fun showNewPinPromptDialog() {
        showPromptDialog("Create 4-Digit Guardian PIN", isPassword = true) { newPin ->
            if (newPin.length >= 4) {
                dataManager.setGuardianPin(newPin)
                dataManager.unlockMaintenance(5)
                updateGuardianUI()
                Toast.makeText(this, "🛡️ Guardian PIN set! Settings & uninstallation are now protected.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "PIN must be at least 4 digits!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUnlockPinDialog() {
        showPromptDialog("Enter Guardian Security PIN", isPassword = true) { inputPin ->
            if (dataManager.verifyPin(inputPin)) {
                dataManager.unlockMaintenance(5)
                updateGuardianUI()
                Toast.makeText(this, "🔓 Settings unlocked for 5 minutes.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPromptDialog(title: String, isPassword: Boolean, onConfirmed: (String) -> Unit) {
        val input = EditText(this).apply {
            inputType = if (isPassword) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
            maxLines = 1
            gravity = Gravity.CENTER
            textSize = 22f
            setPadding(40, 40, 40, 40)
            setTextColor(getColor(R.color.text_primary))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                onConfirmed(input.text.toString().trim())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateDashboard() {
        dataManager.checkAndPerformMidnightReset()
        val count = dataManager.dailyScrollCount
        val max = dataManager.maxDailyScrolls
        val screenHeight = resources.displayMetrics.heightPixels
        val pct = dataManager.getDropPercentage(screenHeight)

        binding.tvScrollCount.text = count.toString()
        binding.tvMaxScrolls.text = "/ $max reels"
        binding.tvDropPercentage.text = "${(pct * 100).toInt()}% covered"

        val pending = dataManager.pendingMaxDailyScrolls
        if (pending > 0) {
            binding.tvCurrentLimitLabel.text = "Active Cap: $max Reels / day (Tomorrow: $pending Reels)"
        } else {
            binding.tvCurrentLimitLabel.text = "Active Cap: $max Reels / day"
        }

        val currentInput = binding.etCustomLimit.text?.toString()?.trim()
        if (currentInput.isNullOrEmpty() || currentInput.toIntOrNull() != max) {
            binding.etCustomLimit.setText(max.toString())
        }

        binding.progressScroll.max = max
        binding.progressScroll.progress = count.coerceAtMost(max)
    }

    private fun attemptChangeDailyLimit(newLimit: Int) {
        if (checkGuardianLocked()) return

        val currentLimit = dataManager.maxDailyScrolls

        if (newLimit < 5) {
            Toast.makeText(this, "Minimum allowed limit is 5 reels.", Toast.LENGTH_SHORT).show()
            updateDashboard()
            return
        }

        if (newLimit == currentLimit) {
            Toast.makeText(this, "Daily limit is already $newLimit reels.", Toast.LENGTH_SHORT).show()
            return
        }

        // Tightening/reducing the limit is always allowed immediately
        if (newLimit < currentLimit) {
            dataManager.maxDailyScrolls = newLimit
            dataManager.pendingMaxDailyScrolls = 0
            updateDashboard()
            Toast.makeText(this, "Daily limit tightened to $newLimit reels! 👏", Toast.LENGTH_SHORT).show()
            return
        }

        // Guardian unlocked (maintenance mode): immediate increase
        if (dataManager.isMaintenanceModeActive()) {
            dataManager.maxDailyScrolls = newLimit
            dataManager.pendingMaxDailyScrolls = 0
            updateDashboard()
            Toast.makeText(this, "Daily limit updated to $newLimit reels by Guardian.", Toast.LENGTH_SHORT).show()
            return
        }

        // Solo Mode: Enforce 24-hour cooling-off (scheduled for tomorrow at midnight)
        val isReached = dataManager.isLimitReached()
        val title = if (isReached) "🔒 Today's Limit Reached" else "🛑 24-Hour Cooling-Off"
        val message = if (isReached) {
            "You have already reached your limit of $currentLimit reels today!\n\nTo break the compulsive doomscrolling habit, you cannot increase your cap today.\n\nWould you like to schedule this increase ($newLimit reels) to take effect tomorrow at midnight (00:00)?"
        } else {
            "To prevent impulsive binge scrolling, increasing your cap from $currentLimit to $newLimit reels takes effect tomorrow at midnight (00:00).\n\nTightening your limit is always applied immediately.\n\nSchedule this limit increase for tomorrow?"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Schedule for Tomorrow ($newLimit Reels)") { _, _ ->
                dataManager.pendingMaxDailyScrolls = newLimit
                updateDashboard()
                Toast.makeText(
                    this,
                    "Scheduled: Daily limit will increase to $newLimit reels tomorrow at midnight.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Keep Today's Cap ($currentLimit)", null)
            .show()
    }

    data class OnboardingSlide(
        val imageRes: Int,
        val title: String,
        val description: String
    )

    private var onboardingDialog: AlertDialog? = null

    private fun showOnboardingCarousel(canDismiss: Boolean = false) {
        if (onboardingDialog?.isShowing == true) return

        val slides = listOf(
            OnboardingSlide(
                imageRes = R.mipmap.ic_kitty_launcher,
                title = "Meet Clawse",
                description = "A mindful focus companion that quietly watches over your scrolling so you enjoy Reels and Shorts without losing hours."
            ),
            OnboardingSlide(
                imageRes = R.drawable.kitty_ginger_loaf,
                title = "The Kitty Curtain",
                description = "As you scroll Reels and Shorts, an animated kitten slowly lowers a curtain. When your daily budget is reached, doomscrolling is paused."
            ),
            OnboardingSlide(
                imageRes = R.drawable.kitty_salmon_loaf,
                title = "Security & Anti-Tamper",
                description = "Protect your limits with an optional PIN, 60-second cooldown friction, and device admin shields against uninstalls or bypasses."
            ),
            OnboardingSlide(
                imageRes = R.mipmap.ic_kitty_launcher,
                title = "100% Private & Local",
                description = "Clawse runs entirely on your device with zero ads, no accounts, and no data collection. Only scroll gestures on Reels and Shorts are counted."
            )
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_onboarding, null)
        val displayWidth = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val dialogWidth = (displayWidth * 0.92f).toInt().coerceAtMost((420 * density).toInt())
        dialogView.minimumWidth = dialogWidth

        val pager = dialogView.findViewById<ViewPager2>(R.id.pagerOnboarding)
        val btnSkip = dialogView.findViewById<MaterialButton>(R.id.btnOnboardingSkip)
        val btnAction = dialogView.findViewById<MaterialButton>(R.id.btnOnboardingAction)
        val layoutDots = dialogView.findViewById<LinearLayout>(R.id.layoutDots)
        layoutDots.removeAllViews()
        val dots = mutableListOf<View>()
        for (i in slides.indices) {
            val dot = View(this).apply {
                val w = if (i == 0) (24 * density).toInt() else (8 * density).toInt()
                val h = (8 * density).toInt()
                val lp = LinearLayout.LayoutParams(w, h).apply {
                    if (i < slides.size - 1) marginEnd = (6 * density).toInt()
                }
                layoutParams = lp
                setBackgroundResource(if (i == 0) R.drawable.bg_onboarding_dot_active else R.drawable.bg_onboarding_dot_inactive)
            }
            layoutDots.addView(dot)
            dots.add(dot)
        }

        fun updateDots(position: Int) {
            dots.forEachIndexed { index, dot ->
                val params = dot.layoutParams
                if (index == position) {
                    dot.setBackgroundResource(R.drawable.bg_onboarding_dot_active)
                    params.width = (24 * resources.displayMetrics.density).toInt()
                } else {
                    dot.setBackgroundResource(R.drawable.bg_onboarding_dot_inactive)
                    params.width = (8 * resources.displayMetrics.density).toInt()
                }
                dot.layoutParams = params
            }

            if (position == slides.size - 1) {
                btnAction.text = "Get Started"
                btnSkip.visibility = View.INVISIBLE
            } else {
                btnAction.text = "Continue"
                btnSkip.visibility = View.VISIBLE
            }
        }

        class OnboardingAdapter : RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder>() {
            inner class SlideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val ivImage: ImageView = itemView.findViewById(R.id.ivSlideImage)
                val tvTitle: TextView = itemView.findViewById(R.id.tvSlideTitle)
                val tvDesc: TextView = itemView.findViewById(R.id.tvSlideDesc)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
                val v = layoutInflater.inflate(R.layout.item_onboarding_slide, parent, false)
                v.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                return SlideViewHolder(v)
            }

            override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
                val slide = slides[position]
                holder.ivImage.setImageResource(slide.imageRes)
                holder.tvTitle.text = slide.title
                holder.tvDesc.text = slide.description
            }

            override fun getItemCount(): Int = slides.size
        }

        pager.adapter = OnboardingAdapter()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
            }
        })

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(canDismiss)
            .create()

        applyGlassBlur(dialog) {
            onboardingDialog = null
        }
        onboardingDialog = dialog

        dialog.setOnShowListener {
            dialog.window?.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT)
            pager.post {
                pager.requestLayout()
                (pager.getChildAt(0) as? RecyclerView)?.adapter?.notifyDataSetChanged()
            }
        }

        val completeOnboarding = {
            dataManager.isOnboardingCompleted = true
            dialog.dismiss()
            onboardingDialog = null
            if (!dataManager.isInitialSetupDone) {
                showInitialSetupDialog()
            }
        }

        btnSkip.setOnClickListener { completeOnboarding() }

        btnAction.setOnClickListener {
            if (pager.currentItem < slides.size - 1) {
                pager.currentItem = pager.currentItem + 1
            } else {
                completeOnboarding()
            }
        }

        dialog.show()
    }

    private fun showInitialSetupDialog() {
        if (initialSetupDialog?.isShowing == true) return
        if (dataManager.isInitialSetupDone) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_initial_setup, null)
        val displayWidth = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density
        val dialogWidth = (displayWidth * 0.92f).toInt().coerceAtMost((420 * density).toInt())
        dialogView.minimumWidth = dialogWidth

        val etLimit = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSetupLimit)
        val switchInsta = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSetupInstagram)
        val switchYt = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSetupYouTube)
        val btnLock = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLockInSetup)

        // Empty text so the translucent "Example: 25" placeholder shows cleanly
        etLimit.setText("")
        switchInsta.isChecked = dataManager.isInstagramMonitored
        switchYt.isChecked = dataManager.isYouTubeMonitored

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        applyGlassBlur(dialog) {
            initialSetupDialog = null
        }
        initialSetupDialog = dialog

        dialog.setOnShowListener {
            dialog.window?.setLayout(dialogWidth, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        btnLock.setOnClickListener {
            val inputStr = etLimit.text?.toString()?.trim() ?: ""
            val parsedLimit = inputStr.toIntOrNull() ?: 25
            val finalLimit = parsedLimit.coerceIn(5, 500)

            dataManager.maxDailyScrolls = finalLimit
            dataManager.isInstagramMonitored = switchInsta.isChecked
            dataManager.isYouTubeMonitored = switchYt.isChecked
            dataManager.isInitialSetupDone = true

            updateDashboard()
            checkPermissions()
            Toast.makeText(this, "Daily focus boundary locked in ($finalLimit reels)!", Toast.LENGTH_LONG).show()

            dialog.dismiss()
            initialSetupDialog = null

            // Prompt permissions step only after daily boundary is locked in
            checkAndPromptPermissions()
        }

        dialog.show()
    }

    private fun checkAndPromptPermissions() {
        val isOverlayGranted = isOverlayPermissionGranted()
        val isAccessibilityGranted = isAccessibilityServiceEnabled(KittyAccessibilityService::class.java)

        if (!isOverlayGranted || !isAccessibilityGranted) {
            showPermissionSetupDialog()
        }
    }

    private fun showPermissionSetupDialog() {
        if (permissionSetupDialog?.isShowing == true) return

        val dialogView = layoutInflater.inflate(R.layout.dialog_permission_setup, null)
        val btnClose = dialogView.findViewById<android.widget.ImageView>(R.id.btnClosePermDialog)
        val btnSetupOverlay = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSetupOverlay)
        val btnSetupAccessibility = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSetupAccessibility)
        val btnDone = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDonePermissions)
        val btnSkip = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSkipPermissions)

        fun updatePermButtons() {
            val isOver = isOverlayPermissionGranted()
            val isAcc = isAccessibilityServiceEnabled(KittyAccessibilityService::class.java)

            if (isOver) {
                btnSetupOverlay.text = "Granted ✓"
                btnSetupOverlay.backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent_green))
                btnSetupOverlay.isEnabled = false
            } else {
                btnSetupOverlay.text = "Enable"
                btnSetupOverlay.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                btnSetupOverlay.isEnabled = true
            }

            if (isAcc) {
                btnSetupAccessibility.text = "Granted ✓"
                btnSetupAccessibility.backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent_green))
                btnSetupAccessibility.isEnabled = false
            } else {
                btnSetupAccessibility.text = "Enable"
                btnSetupAccessibility.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
                btnSetupAccessibility.isEnabled = true
            }

            if (isOver && isAcc) {
                btnDone.text = "All Set! Enter Clawse 🐾"
                btnDone.backgroundTintList = ColorStateList.valueOf(getColor(R.color.accent_green))
            } else {
                btnDone.text = "Done / Go to Dashboard"
                btnDone.backgroundTintList = ColorStateList.valueOf(getColor(R.color.primary))
            }
        }

        updatePermButtons()

        btnSetupOverlay.setOnClickListener {
            requestOverlayPermission()
        }

        btnSetupAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Tap 'Clawse' under Downloaded / Installed apps and turn it ON",
                Toast.LENGTH_LONG
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        applyGlassBlur(dialog) {
            permissionSetupDialog = null
            activeSetupDialogUpdate = null
            checkPermissions()
        }
        permissionSetupDialog = dialog
        activeSetupDialogUpdate = { updatePermButtons() }

        val dismissAction = {
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dismissAction() }
        btnSkip.setOnClickListener { dismissAction() }
        btnDone.setOnClickListener { dismissAction() }

        dialog.show()
    }

    private fun showMainMenuSheet() {
        val sheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.sheet_main_menu, null)
        sheet.setContentView(sheetView)

        sheet.setOnShowListener {
            val bottomSheet = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        applyGlassBlur(sheet)

        val itemChangeLimit = sheetView.findViewById<View>(R.id.menuItemChangeLimit)
        val itemPermissions = sheetView.findViewById<View>(R.id.menuItemPermissions)
        val itemHowItWorks = sheetView.findViewById<View>(R.id.menuItemHowItWorks)

        itemChangeLimit.setOnClickListener {
            sheet.dismiss()
            showChangeLimitDialog()
        }

        itemPermissions.setOnClickListener {
            sheet.dismiss()
            showPermissionSetupDialog()
        }

        itemHowItWorks?.setOnClickListener {
            sheet.dismiss()
            showOnboardingCarousel(canDismiss = true)
        }

        sheet.show()
    }

    private fun showChangeLimitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_limit, null)
        val etLimit = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etChangeLimit)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveChangeLimit)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelChangeLimit)

        // Empty text so the translucent "Example: 25" placeholder shows cleanly
        etLimit.setText("")

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        applyGlassBlur(dialog)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val text = etLimit.text?.toString()?.trim() ?: ""
            val parsed = text.toIntOrNull()
            if (parsed == null || parsed < 5) {
                Toast.makeText(this, "Please enter at least 5 reels.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            attemptChangeDailyLimit(parsed)
        }

        dialog.show()
    }

    private fun applyGlassBlur(dialog: android.app.Dialog, onDismissExtra: (() -> Unit)? = null) {
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.40f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                attributes = attributes?.apply {
                    blurBehindRadius = 60
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activeBlurCount++
            // Real, hardware GPU Gaussian blur directly on the activity dashboard behind the modal
            val blur = RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.CLAMP)
            binding.root.setRenderEffect(blur)
        }

        dialog.setOnDismissListener {
            onDismissExtra?.invoke()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activeBlurCount = (activeBlurCount - 1).coerceAtLeast(0)
                if (activeBlurCount == 0) {
                    binding.root.setRenderEffect(null)
                }
            }
        }
    }

    private fun checkPermissions() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled(KittyAccessibilityService::class.java)
        val isOverlayEnabled = isOverlayPermissionGranted()

        // Header active status badge & floating permission notification
        if (isOverlayEnabled && isAccessibilityEnabled) {
            binding.badgeHeaderStatus.setBackgroundResource(R.drawable.bg_status_badge)
            binding.dotHeaderStatus.setBackgroundResource(R.drawable.dot_green)
            binding.tvHeaderStatus.text = "Active"
            binding.tvHeaderStatus.setTextColor(getColor(R.color.accent_green))
            binding.cardPermissionFloater.visibility = View.GONE
        } else {
            binding.badgeHeaderStatus.setBackgroundResource(R.drawable.bg_status_badge_warning)
            binding.dotHeaderStatus.setBackgroundResource(R.drawable.dot_orange)
            binding.tvHeaderStatus.text = "Setup"
            binding.tvHeaderStatus.setTextColor(getColor(R.color.kitty_orange))
            binding.cardPermissionFloater.visibility = View.VISIBLE
        }
    }

    private fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Please allow 'Display over other apps' in Settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isOverlayPermissionGranted(): Boolean {
        if (Settings.canDrawOverlays(this)) return true
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps?.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps?.checkOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    Process.myUid(),
                    packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun isAccessibilityServiceEnabled(serviceClass: Class<*>): Boolean {
        val expectedComponentName = ComponentName(this, serviceClass)
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        if (enabledServicesSetting.isNotEmpty()) {
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledComponent = ComponentName.unflattenFromString(componentNameString)
                if (enabledComponent != null &&
                    enabledComponent.packageName == expectedComponentName.packageName &&
                    enabledComponent.className == expectedComponentName.className
                ) {
                    return true
                }
            }
        }

        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.id.contains(expectedComponentName.packageName) &&
                service.id.contains(expectedComponentName.className)) {
                return true
            }
        }
        return false
    }
}
