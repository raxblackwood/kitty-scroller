package com.antiscroller.kitty

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
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

        when (dataManager.maxDailyScrolls) {
            50 -> binding.chip50.isChecked = true
            100 -> binding.chip100.isChecked = true
            200 -> binding.chip200.isChecked = true
            500 -> binding.chip500.isChecked = true
            else -> binding.chip100.isChecked = true
        }

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

        // Budget Limit chips
        binding.chipGroupLimits.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkGuardianLocked()) {
                updateDashboard()
                return@setOnCheckedStateChangeListener
            }
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val newLimit = when (checkedIds.first()) {
                R.id.chip50 -> 50
                R.id.chip100 -> 100
                R.id.chip200 -> 200
                R.id.chip500 -> 500
                else -> 100
            }
            dataManager.maxDailyScrolls = newLimit
            updateDashboard()
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
        binding.chipGroupLimits.alpha = alpha
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
        binding.tvMaxScrolls.text = "/ $max scrolls"
        binding.tvDropPercentage.text = "${(pct * 100).toInt()}% covered"

        binding.progressScroll.max = max
        binding.progressScroll.progress = count.coerceAtMost(max)
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
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (service in enabledServices) {
            if (service.id.contains(expectedComponentName.packageName) &&
                service.id.contains(expectedComponentName.className)) {
                return true
            }
        }
        return false
    }
}
