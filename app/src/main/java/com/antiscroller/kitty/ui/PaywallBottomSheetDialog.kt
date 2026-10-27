package com.antiscroller.kitty.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.antiscroller.kitty.billing.BillingManager
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.databinding.DialogPaywallBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Material 3 Bottom Sheet Dialog presenting:
 * 1. Cosmetic Pack ($2.50 One-Time Lifetime): Unlocks Tabby, Saturn, Rolex, Salmon, and Katrina.
 * 2. Guardian Parental Control ($0.50/mo Subscription): Real-time uninstall alerts.
 */
class PaywallBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: DialogPaywallBinding? = null
    private val binding get() = _binding!!

    private var billingManager: BillingManager? = null
    private lateinit var dataManager: ScrollDataManager

    var onPurchaseCompletedListener: (() -> Unit)? = null

    companion object {
        const val TAG = "PaywallBottomSheet"
        private const val ARG_TARGET_PRODUCT = "target_product"

        fun newInstance(targetProduct: String? = null): PaywallBottomSheetDialog {
            val dialog = PaywallBottomSheetDialog()
            val args = Bundle()
            if (targetProduct != null) {
                args.putString(ARG_TARGET_PRODUCT, targetProduct)
            }
            dialog.arguments = args
            return dialog
        }
    }

    fun setBillingManager(manager: BillingManager) {
        this.billingManager = manager
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPaywallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        dataManager = ScrollDataManager.getInstance(context)

        updateButtonStates()

        binding.btnBuyCosmetics.setOnClickListener {
            val mgr = billingManager
            if (mgr == null) {
                Toast.makeText(context, "Billing service unavailable. Please check Play Store.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mgr.launchCosmeticPurchase(requireActivity())
        }

        binding.btnBuyParental.setOnClickListener {
            val mgr = billingManager
            if (mgr == null) {
                Toast.makeText(context, "Billing service unavailable. Please check Play Store.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mgr.launchParentalSubscriptionPurchase(requireActivity())
        }

        binding.btnRestorePurchases.setOnClickListener {
            val mgr = billingManager
            if (mgr == null) {
                Toast.makeText(context, "Billing service unavailable.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.btnRestorePurchases.isEnabled = false
            binding.btnRestorePurchases.text = "Checking Google Play..."
            mgr.restorePurchases { success, msg ->
                activity?.runOnUiThread {
                    binding.btnRestorePurchases.isEnabled = true
                    binding.btnRestorePurchases.text = "🔄 Restore Previous Purchases"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (success) {
                        updateButtonStates()
                        onPurchaseCompletedListener?.invoke()
                    }
                }
            }
        }
    }

    fun updateButtonStates() {
        if (_binding == null) return

        if (dataManager.isCosmeticPackUnlocked) {
            binding.btnBuyCosmetics.text = "✓ UNLOCKED & ACTIVE"
            binding.btnBuyCosmetics.isEnabled = false
            binding.btnBuyCosmetics.alpha = 0.7f
            binding.tvCosmeticsPriceBadge.text = "PURCHASED"
        } else {
            val price = billingManager?.getCosmeticFormattedPrice() ?: "$2.50"
            binding.btnBuyCosmetics.text = "Unlock All Breeds • $price"
            binding.btnBuyCosmetics.isEnabled = true
            binding.btnBuyCosmetics.alpha = 1.0f
        }

        if (dataManager.isParentalSubscribed) {
            binding.btnBuyParental.text = "✓ SUBSCRIBED & ACTIVE"
            binding.btnBuyParental.isEnabled = false
            binding.btnBuyParental.alpha = 0.7f
            binding.tvParentalPriceBadge.text = "ACTIVE"
        } else {
            val price = billingManager?.getParentalFormattedPrice() ?: "$0.50 / month"
            binding.btnBuyParental.text = "Subscribe Guardian Alert • $price"
            binding.btnBuyParental.isEnabled = true
            binding.btnBuyParental.alpha = 1.0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
