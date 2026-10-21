package com.antiscroller.kitty.ui

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.antiscroller.kitty.R
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.databinding.ActivityBreathingFrictionBinding

/**
 * 60-Second Cooling-Off Period (Cognitive Friction Engine).
 * When a user without a Guardian PIN attempts to deactivate Device Admin or uninstall
 * ClawBack during a doomscroll craving, this activity provides a calm 60-second breathing
 * pause to let the dopamine impulse subside.
 */
class BreathingFrictionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBreathingFrictionBinding
    private lateinit var dataManager: ScrollDataManager
    private var countdownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBreathingFrictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = ScrollDataManager.getInstance(this)

        setupListeners()
        startCoolingOffTimer()
    }

    private fun setupListeners() {
        binding.btnKeepFocus.setOnClickListener {
            returnToHome()
        }

        binding.btnProceedDeactivate.setOnClickListener {
            // Only clickable after 60s countdown completes
            dataManager.unlockMaintenance(minutes = 5)
            Toast.makeText(this, "Settings unlocked for 5 minutes.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startCoolingOffTimer() {
        val totalMillis = 60_000L
        val intervalMillis = 1_000L

        countdownTimer?.cancel()
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
    }
}
