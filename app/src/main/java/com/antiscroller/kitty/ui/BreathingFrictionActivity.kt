package com.antiscroller.kitty.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.antiscroller.kitty.R
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.databinding.ActivityBreathingFrictionBinding

/**
 * 60-Second Cooling-Off Period (Cognitive Friction Engine).
 * When a user without a Guardian PIN attempts to modify Clawse settings, force stop,
 * or clear data during an impulsive dopamine craving, this activity provides a calm
 * 60-second breathing pause to let the impulse subside.
 *
 * Strict Requirement: If the user leaves, minimizes, or closes the popup before the
 * full 60 seconds elapsed and being confirmed, the timer resets completely back to 60s.
 */
class BreathingFrictionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBreathingFrictionBinding
    private lateinit var dataManager: ScrollDataManager
    private var countdownTimer: CountDownTimer? = null
    private var isProceedUnlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreathingFrictionBinding.inflate(layoutInflater)
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

        setupListeners()
        startCoolingOffTimer()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                returnToHome()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // If re-entering the screen without having completed the proceed flow,
        // unconditionally restart the 60-second timer from the beginning.
        if (!isProceedUnlocked && countdownTimer == null) {
            startCoolingOffTimer()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!isProceedUnlocked) {
            startCoolingOffTimer()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // User explicitly left the activity (Home gesture, app switch, etc.)
        // Cancel timer and finish so it cannot tick down in the background.
        if (!isProceedUnlocked) {
            countdownTimer?.cancel()
            countdownTimer = null
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop timer immediately whenever activity is not the active foreground window
        if (!isProceedUnlocked) {
            countdownTimer?.cancel()
            countdownTimer = null
        }
    }

    override fun onStop() {
        super.onStop()
        // If the activity is stopped or screen turned off without unlocking, finish it.
        // Re-opening will always create a fresh instance with a full 60s timer.
        if (!isProceedUnlocked) {
            countdownTimer?.cancel()
            countdownTimer = null
            finish()
        }
    }

    private fun setupListeners() {
        // Tapping outside the card dismisses the popup and returns to Home (resetting timer)
        binding.rootScrollView.setOnClickListener {
            returnToHome()
        }
        binding.containerLayout.setOnClickListener {
            returnToHome()
        }
        binding.cardFriction.setOnClickListener {
            // Consume tap inside card so it does not dismiss
        }

        binding.btnKeepFocus.setOnClickListener {
            returnToHome()
        }

        binding.btnProceedDeactivate.setOnClickListener {
            // Only clickable after 60s countdown completes
            isProceedUnlocked = true
            dataManager.unlockMaintenance(minutes = 5)
            Toast.makeText(this, "Settings unlocked for 5 minutes.", Toast.LENGTH_SHORT).show()
            if (!dataManager.isInitialSetupDone) {
                val setupIntent = Intent(this, com.antiscroller.kitty.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(setupIntent)
            }
            finish()
        }
    }

    private fun startCoolingOffTimer() {
        val totalMillis = 60_000L
        val intervalMillis = 1_000L

        countdownTimer?.cancel()
        countdownTimer = null

        // Unconditionally reset UI to full 60s state
        binding.tvCountdownSeconds.text = "60"
        binding.progressCountdown.progress = 60
        binding.btnProceedDeactivate.isEnabled = false
        binding.btnProceedDeactivate.alpha = 0.5f
        binding.btnProceedDeactivate.text = "Wait 60s to Modify Settings"
        binding.btnProceedDeactivate.setTextColor(getColor(R.color.text_secondary))
        binding.btnProceedDeactivate.setBackgroundColor(getColor(R.color.surface_variant))

        countdownTimer = object : CountDownTimer(totalMillis, intervalMillis) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000L).toInt()
                binding.tvCountdownSeconds.text = secondsRemaining.toString()
                binding.progressCountdown.progress = secondsRemaining
                binding.btnProceedDeactivate.text = "Wait ${secondsRemaining}s to Modify Settings"
            }

            override fun onFinish() {
                binding.tvCountdownSeconds.text = "0"
                binding.progressCountdown.progress = 0
                binding.btnProceedDeactivate.isEnabled = true
                binding.btnProceedDeactivate.alpha = 1.0f
                binding.btnProceedDeactivate.text = "Proceed to Settings (5m Access)"
                binding.btnProceedDeactivate.setTextColor(getColor(R.color.on_secondary))
                binding.btnProceedDeactivate.setBackgroundColor(getColor(R.color.secondary))
            }
        }.start()
    }

    private fun returnToHome() {
        isProceedUnlocked = false
        countdownTimer?.cancel()
        countdownTimer = null
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent bypassing the friction screen via Back button to reach settings
        returnToHome()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        countdownTimer = null
    }
}
