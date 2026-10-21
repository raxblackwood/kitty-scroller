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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.antiscroller.kitty.admin.KittyAdminReceiver
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.databinding.ActivityMainBinding
import com.antiscroller.kitty.overlay.KittyOverlayManager
import com.antiscroller.kitty.service.KittyAccessibilityService
import com.antiscroller.kitty.worker.DailyResetWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dataManager: ScrollDataManager
    private lateinit var overlayManager: KittyOverlayManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = ScrollDataManager.getInstance(this)
        overlayManager = KittyOverlayManager.getInstance(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, KittyAdminReceiver::class.java)

        DailyResetWorker.scheduleMidnightReset(this)

        setupUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
        checkPermissions()
        updateGuardianUI()
        updateDeviceOwnerUI()
    }

    private fun setupUI() {
        val currentTheme = dataManager.kittyTheme
        binding.previewCurtain.applyTheme(currentTheme)
        if (currentTheme == ScrollDataManager.THEME_BLACK) {
            binding.chipThemeBlack.isChecked = true
        } else {
            binding.chipThemeOrange.isChecked = true
        }

        val screenHeight = resources.displayMetrics.heightPixels
        val dropPct = dataManager.getDropPercentage(screenHeight)
        binding.previewCurtain.setDropPercentage(if (dropPct > 0) dropPct else 0.35f, animate = false)

        binding.chip50.isChecked = (dataManager.maxDailyScrolls == 50)
        binding.etCustomLimit.setText(dataManager.maxDailyScrolls.toString())
        binding.tvCurrentLimitLabel.text = "Active Cap: ${dataManager.maxDailyScrolls} Reels / day"

        binding.switchInstagram.isChecked = dataManager.isInstagramMonitored
        binding.switchYouTube.isChecked = dataManager.isYouTubeMonitored

        updateGuardianUI()
    }

    private fun setupListeners() {
        // Kitty Breed Selection
        binding.chipGroupKittyTheme.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedTheme = if (checkedIds.first() == R.id.chipThemeBlack) {
                ScrollDataManager.THEME_BLACK
            } else {
                ScrollDataManager.THEME_ORANGE
            }
            dataManager.kittyTheme = selectedTheme
            binding.previewCurtain.applyTheme(selectedTheme)
            val breedName = if (selectedTheme == ScrollDataManager.THEME_BLACK) "Midnight Black Cat 🐈‍⬛" else "Ginger Tabby 🍊"
            Toast.makeText(this, "$breedName selected!", Toast.LENGTH_SHORT).show()
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

        // Accessibility Permission button
        binding.btnPermAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Find 'ClawBack' under Downloaded Apps and turn it ON",
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

        // Recommended 50 chip
        binding.chip50.setOnClickListener {
            attemptChangeDailyLimit(50)
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

        // Target apps toggles
        binding.switchInstagram.setOnCheckedChangeListener { _, isChecked ->
            if (checkGuardianLocked()) {
                binding.switchInstagram.isChecked = dataManager.isInstagramMonitored
                return@setOnCheckedChangeListener
            }
            dataManager.isInstagramMonitored = isChecked
        }
        binding.switchYouTube.setOnCheckedChangeListener { _, isChecked ->
            if (checkGuardianLocked()) {
                binding.switchYouTube.isChecked = dataManager.isYouTubeMonitored
                return@setOnCheckedChangeListener
            }
            dataManager.isYouTubeMonitored = isChecked
        }

        // Reset today's scrolls
        binding.btnResetToday.setOnClickListener {
            if (checkGuardianLocked()) return@setOnClickListener

            dataManager.resetDailyCount()
            updateDashboard()
            Toast.makeText(this, "Today's scroll meter reset to 0!", Toast.LENGTH_SHORT).show()
        }

        // Guardian PIN Controls
        binding.btnSetGuardianPin.setOnClickListener {
            showSetPinDialog()
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
            val adbCmd = "adb shell dpm set-device-owner com.antiscroller.kitty/.admin.KittyAdminReceiver"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ClawBack ADB Command", adbCmd)
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

    private fun updateGuardianUI() {
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
        binding.chip50.alpha = alpha
        binding.tilCustomLimit.alpha = alpha
        binding.btnSetCustomLimit.alpha = alpha
        binding.btnSetCustomLimit.isEnabled = !locked
        binding.switchInstagram.alpha = alpha
        binding.switchYouTube.alpha = alpha
        binding.btnResetToday.alpha = alpha
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
        binding.tvCurrentLimitLabel.text = "Active Cap: $max Reels / day"

        binding.chip50.isChecked = (max == 50)
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
            updateDashboard()
            Toast.makeText(this, "Daily limit tightened to $newLimit reels! 👏", Toast.LENGTH_SHORT).show()
            return
        }

        // User is attempting to INCREASE the limit!
        // 1. If limit is ALREADY REACHED for today -> STRICT LOCKOUT!
        if (dataManager.isLimitReached()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("🔒 Today's Limit Reached")
                .setMessage("You have already reached your limit of $currentLimit reels today!\n\nTo break the compulsive doomscrolling habit, you cannot increase your cap today. Your limit will reset fresh tomorrow at midnight (00:00).\n\n(If a parent or partner set a Guardian PIN, they can unlock it in the Guardian section.)")
                .setPositiveButton("I'll Respect My Limit", null)
                .show()
            updateDashboard()
            return
        }

        // 2. If limit is NOT yet reached, but user is increasing it during the day:
        // Enforce 30-Second Mindful Reflection Challenge
        showMindfulIncreaseDialog(newLimit)
    }

    private fun showMindfulIncreaseDialog(newLimit: Int) {
        val currentLimit = dataManager.maxDailyScrolls

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("🛑 Mindful Pause")
            .setMessage("You are increasing your daily limit from $currentLimit to $newLimit reels.\n\nTake a breath: Are you increasing this to give in to doomscrolling?\n\nTo prevent impulsive decisions, please wait 30 seconds before confirming.")
            .setPositiveButton("Confirm (30s)", null)
            .setNegativeButton("Cancel (Keep $currentLimit)", null)
            .setCancelable(false)
            .create()

        dialog.show()

        val confirmBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
        confirmBtn.isEnabled = false

        val timer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000).toInt() + 1
                confirmBtn.text = "Confirm (${sec}s)"
            }

            override fun onFinish() {
                confirmBtn.text = "Yes, Increase to $newLimit"
                confirmBtn.isEnabled = true
            }
        }.start()

        confirmBtn.setOnClickListener {
            timer.cancel()
            dataManager.maxDailyScrolls = newLimit
            updateDashboard()
            Toast.makeText(this@MainActivity, "Daily limit updated to $newLimit reels.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            timer.cancel()
            updateDashboard()
        }
    }

    private fun checkPermissions() {
        val isAccessibilityEnabled = isAccessibilityServiceEnabled(KittyAccessibilityService::class.java)
        if (isAccessibilityEnabled) {
            binding.btnPermAccessibility.text = "Granted ✓"
            binding.btnPermAccessibility.setBackgroundColor(getColor(R.color.accent_green))
        } else {
            binding.btnPermAccessibility.text = "Enable"
            binding.btnPermAccessibility.setBackgroundColor(getColor(R.color.primary))
        }

        val isOverlayEnabled = Settings.canDrawOverlays(this)
        if (isOverlayEnabled) {
            binding.btnPermOverlay.text = "Granted ✓"
            binding.btnPermOverlay.setBackgroundColor(getColor(R.color.accent_green))
        } else {
            binding.btnPermOverlay.text = "Enable"
            binding.btnPermOverlay.setBackgroundColor(getColor(R.color.primary))
        }

        val isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
        if (isAdminActive) {
            binding.btnPermAdmin.text = "Active ✓"
            binding.btnPermAdmin.setBackgroundColor(getColor(R.color.accent_green))
        } else {
            binding.btnPermAdmin.text = "Activate"
            binding.btnPermAdmin.setBackgroundColor(getColor(R.color.secondary))
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
