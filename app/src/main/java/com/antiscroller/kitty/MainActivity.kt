package com.antiscroller.kitty

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dataManager: ScrollDataManager
    private lateinit var overlayManager: KittyOverlayManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var billingManager: BillingManager

    private var previewedTheme: String = ScrollDataManager.THEME_GINGER

    private val handler = Handler(Looper.getMainLooper())
    private var activeSetupDialogUpdate: (() -> Unit)? = null

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

        setupUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
        checkPermissions()
        activeSetupDialogUpdate?.invoke()
        updateThemeChips()
        updateGuardianChildModeUI()
        updateGuardianUI()
        updateDeviceOwnerUI()

        if (!dataManager.isInitialSetupDone) {
            showInitialSetupDialog()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }

    private fun setupUI() {
        previewedTheme = dataManager.kittyTheme
        binding.previewCurtain.applyTheme(previewedTheme)
        updateThemeChips()
        updateEquipButtonState()

        val screenHeight = resources.displayMetrics.heightPixels
        val dropPct = dataManager.getDropPercentage(screenHeight)
        binding.previewCurtain.setDropPercentage(if (dropPct > 0) dropPct else 0.35f, animate = false)

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
                else -> ScrollDataManager.THEME_GINGER
            }
            if (chosenTheme != previewedTheme) {
                previewedTheme = chosenTheme
                binding.previewCurtain.applyTheme(previewedTheme)
                updateEquipButtonState()
            }
        }

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

        // Live preview slider
        binding.sliderPreview.addOnChangeListener { _, value, _ ->
            val fraction = value / 100f
            binding.previewCurtain.setDropPercentage(fraction, animate = true)
            binding.tvSliderLabel.text = "Testing Kitty Drop: ${(fraction * 100).toInt()}%"
        }

        // Test floating overlay
        binding.btnTestOverlay.setOnClickListener {
            if (!overlayManager.canDrawOverlay()) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            val testPercentage = binding.sliderPreview.value / 100f
            overlayManager.showOrUpdateOverlay(testPercentage)
            Toast.makeText(this, "Displaying Kitty Curtain for 5 seconds...", Toast.LENGTH_SHORT).show()

            handler.postDelayed({
                overlayManager.hideOverlay()
            }, 5000)
        }

        // Banner Setup buttons
        binding.btnBannerOverlay.setOnClickListener {
            requestOverlayPermission()
        }
        binding.btnBannerAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Tap 'Clawse' under Downloaded / Installed apps and turn it ON",
                Toast.LENGTH_LONG
            ).show()
        }

        // Accessibility Permission button
        binding.btnPermAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Find 'Clawse' under Downloaded Apps and turn it ON",
                Toast.LENGTH_LONG
            ).show()
        }

        // Overlay Permission button
        binding.btnPermOverlay.setOnClickListener {
            requestOverlayPermission()
        }

        // Device Admin button
        binding.btnPermAdmin.setOnClickListener {
            if (checkGuardianLocked()) return@setOnClickListener

            if (devicePolicyManager.isAdminActive(adminComponent)) {
                Toast.makeText(this, "Protection is already active!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.device_admin_description)
                    )
                }
                startActivity(intent)
            }
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
            else -> "Ginger 🍊"
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

        val targetChipId = when (previewedTheme) {
            ScrollDataManager.THEME_TABBY, ScrollDataManager.THEME_BLACK -> R.id.chipThemeTabby
            ScrollDataManager.THEME_SATURN -> R.id.chipThemeSaturn
            ScrollDataManager.THEME_ROLEX -> R.id.chipThemeRolex
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
            .setMessage("Set where uninstallation and tamper alerts should be sent in real-time.")
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

    private fun showInitialSetupDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_initial_setup, null)
        val chipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupSetupLimit)
        val etLimit = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etSetupLimit)
        val switchInsta = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSetupInstagram)
        val switchYt = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchSetupYouTube)
        val btnSetupOverlay = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSetupOverlay)
        val btnSetupAccessibility = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSetupAccessibility)
        val btnLock = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLockInSetup)

        // Pre-fill with current settings
        etLimit.setText(dataManager.maxDailyScrolls.toString())
        switchInsta.isChecked = dataManager.isInstagramMonitored
        switchYt.isChecked = dataManager.isYouTubeMonitored

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chipLimit15 -> etLimit.setText("15")
                R.id.chipLimit25 -> etLimit.setText("25")
                R.id.chipLimit50 -> etLimit.setText("50")
            }
        }

        fun updateDialogPermButtons() {
            val isOver = Settings.canDrawOverlays(this)
            val isAcc = isAccessibilityServiceEnabled(KittyAccessibilityService::class.java)

            if (isOver) {
                btnSetupOverlay.text = "Granted ✓"
                btnSetupOverlay.setBackgroundColor(getColor(R.color.accent_green))
            } else {
                btnSetupOverlay.text = "Enable"
                btnSetupOverlay.setBackgroundColor(getColor(R.color.primary))
            }

            if (isAcc) {
                btnSetupAccessibility.text = "Granted ✓"
                btnSetupAccessibility.setBackgroundColor(getColor(R.color.accent_green))
            } else {
                btnSetupAccessibility.text = "Enable"
                btnSetupAccessibility.setBackgroundColor(getColor(R.color.primary))
            }

            if (isOver && isAcc) {
                btnLock.text = "Lock In & Start Clawse 🐾"
            } else {
                btnLock.text = "Lock In Daily Boundary 🔒"
            }
        }

        updateDialogPermButtons()

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
            .setCancelable(false)
            .create()

        activeSetupDialogUpdate = { updateDialogPermButtons() }
        dialog.setOnDismissListener { activeSetupDialogUpdate = null }

        btnLock.setOnClickListener {
            val inputStr = etLimit.text?.toString()?.trim() ?: "25"
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
        }

        dialog.show()
    }

    private fun checkPermissions() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled(KittyAccessibilityService::class.java)
        if (isAccessibilityEnabled) {
            binding.btnPermAccessibility.text = "Granted ✓"
            binding.btnPermAccessibility.setBackgroundColor(getColor(R.color.accent_green))
            binding.btnBannerAccessibility.text = "Accessibility ✓"
            binding.btnBannerAccessibility.setBackgroundColor(getColor(R.color.accent_green))
        } else {
            binding.btnPermAccessibility.text = "Enable"
            binding.btnPermAccessibility.setBackgroundColor(getColor(R.color.primary))
            binding.btnBannerAccessibility.text = "2. Accessibility"
            binding.btnBannerAccessibility.setBackgroundColor(getColor(R.color.primary))
        }

        val isOverlayEnabled = Settings.canDrawOverlays(this)
        if (isOverlayEnabled) {
            binding.btnPermOverlay.text = "Granted ✓"
            binding.btnPermOverlay.setBackgroundColor(getColor(R.color.accent_green))
            binding.btnBannerOverlay.text = "Overlay ✓"
            binding.btnBannerOverlay.setBackgroundColor(getColor(R.color.accent_green))
        } else {
            binding.btnPermOverlay.text = "Enable"
            binding.btnPermOverlay.setBackgroundColor(getColor(R.color.primary))
            binding.btnBannerOverlay.text = "1. Overlay"
            binding.btnBannerOverlay.setBackgroundColor(getColor(R.color.primary))
        }

        val isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
        if (isAdminActive) {
            binding.btnPermAdmin.text = "Active ✓"
            binding.btnPermAdmin.setBackgroundColor(getColor(R.color.accent_green))
        } else {
            binding.btnPermAdmin.text = "Activate"
            binding.btnPermAdmin.setBackgroundColor(getColor(R.color.secondary))
        }

        // Dynamic Setup & Guarding Banner
        if (isOverlayEnabled && isAccessibilityEnabled) {
            binding.tvSetupBannerIcon.text = "🟢"
            binding.tvSetupBannerTitle.text = "Clawse is Active & Guarding"
            binding.tvSetupBannerDesc.text = "Kitty Curtain is ready and will lower as you scroll in Instagram and YouTube Shorts."
            binding.cardSetupBanner.strokeColor = getColor(R.color.accent_green)
            binding.layoutSetupBannerActions.visibility = View.GONE
        } else {
            binding.tvSetupBannerIcon.text = "⚠️"
            binding.tvSetupBannerTitle.text = "Setup Required to Activate"
            val missing = when {
                !isOverlayEnabled && !isAccessibilityEnabled -> "Display Over Other Apps & Accessibility"
                !isOverlayEnabled -> "Display Over Other Apps (Overlay)"
                else -> "Accessibility Service (Scroll Tracker)"
            }
            binding.tvSetupBannerDesc.text = "Enable $missing so the Kitty Curtain can appear over reels."
            binding.cardSetupBanner.strokeColor = getColor(R.color.kitty_orange)
            binding.layoutSetupBannerActions.visibility = View.VISIBLE
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
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
