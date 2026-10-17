package com.antiscroller.kitty.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.databinding.ActivityGuardianUnlockBinding

/**
 * Activity for Parent / Accountability Partner PIN verification.
 * Blocks unauthorized access to settings, force stop, or uninstallation.
 */
class GuardianUnlockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuardianUnlockBinding
    private lateinit var dataManager: ScrollDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianUnlockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataManager = ScrollDataManager.getInstance(this)

        binding.btnUnlock.setOnClickListener {
            val inputPin = binding.etPin.text?.toString()?.trim() ?: ""
            if (dataManager.verifyPin(inputPin)) {
                // Grant 5 minutes of maintenance mode
                dataManager.unlockMaintenance(5)
                Toast.makeText(
                    this,
                    "🔓 Unlocked for maintenance (5 minutes). Settings & modifications enabled.",
                    Toast.LENGTH_LONG
                ).show()
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                binding.tvPinError.visibility = View.VISIBLE
                binding.etPin.text?.clear()
            }
        }

        binding.btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}
