package com.antiscroller.kitty.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.antiscroller.kitty.data.ScrollDataManager

/**
 * Manages Google Play In-App Billing for ClawBack:
 * 1. Cosmetic Pack ($2.50 One-Time Lifetime): Unlocks Tabby, Saturn, Rolex, Salmon, and Katrina breeds.
 * 2. Guardian Parental Control ($0.50/month Subscription): Real-time tamper alerts & heartbeat.
 * Supports UPI (Google Pay, PhonePe, Paytm) and Cards out-of-the-box via Google Play Checkout.
 */
class BillingManager(
    private val context: Context,
    private val onPurchaseResultListener: ((Boolean, String) -> Unit)? = null
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        const val PRODUCT_COSMETIC_PACK = "clawback_cosmetic_pack"
        const val PRODUCT_PARENTAL_MONTHLY = "clawback_parental_monthly"
    }

    private val dataManager: ScrollDataManager = ScrollDataManager.getInstance(context)

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    private var cosmeticProductDetails: ProductDetails? = null
    private var parentalProductDetails: ProductDetails? = null

    private var isConnected = false

    init {
        startConnection()
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    Log.i(TAG, "Google Play Billing client connected successfully.")
                    queryProducts()
                    queryActivePurchases()
                    onConnected?.invoke()
                } else {
                    Log.w(TAG, "Billing setup failed with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                Log.w(TAG, "Billing service disconnected. Will reconnect on next user action.")
            }
        })
    }

    fun queryProducts() {
        if (!billingClient.isReady) return

        // Query In-App One-Time (Cosmetics)
        val inAppProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_COSMETIC_PACK)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppProductList)
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (details in productDetailsResult.productDetailsList) {
                    if (details.productId == PRODUCT_COSMETIC_PACK) {
                        cosmeticProductDetails = details
                        Log.i(TAG, "Loaded cosmetic details: ${details.name} - ${getCosmeticFormattedPrice()}")
                    }
                }
            }
        }

        // Query Subscription (Parental Monthly)
        val subsProductList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PARENTAL_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subsProductList)
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { billingResult, productDetailsResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (details in productDetailsResult.productDetailsList) {
                    if (details.productId == PRODUCT_PARENTAL_MONTHLY) {
                        parentalProductDetails = details
                        Log.i(TAG, "Loaded parental sub details: ${details.name} - ${getParentalFormattedPrice()}")
                    }
                }
            }
        }
    }

    fun getCosmeticFormattedPrice(): String {
        val offerDetails = cosmeticProductDetails?.oneTimePurchaseOfferDetails
        return offerDetails?.formattedPrice ?: "$2.50"
    }

    fun getParentalFormattedPrice(): String {
        val subPricing = parentalProductDetails?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()
        return subPricing?.formattedPrice?.let { "$it / mo" } ?: "$0.50 / month"
    }

    fun launchCosmeticPurchase(activity: Activity): Boolean {
        val details = cosmeticProductDetails
        if (details == null) {
            if (!billingClient.isReady) {
                startConnection { launchCosmeticPurchase(activity) }
            } else {
                queryProducts()
            }
            onPurchaseResultListener?.invoke(false, "Play Store product details still loading. Please try again.")
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
        return responseCode == BillingClient.BillingResponseCode.OK
    }

    fun launchParentalSubscriptionPurchase(activity: Activity): Boolean {
        val details = parentalProductDetails
        val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (details == null || offerToken == null) {
            if (!billingClient.isReady) {
                startConnection { launchParentalSubscriptionPurchase(activity) }
            } else {
                queryProducts()
            }
            onPurchaseResultListener?.invoke(false, "Play Store subscription details still loading. Please try again.")
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, flowParams).responseCode
        return responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled the purchase.")
                onPurchaseResultListener?.invoke(false, "Purchase cancelled.")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                onPurchaseResultListener?.invoke(false, "Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return
        }

        var unlockedCosmetic = false
        var subscribedParental = false

        if (purchase.products.contains(PRODUCT_COSMETIC_PACK)) {
            dataManager.isCosmeticPackUnlocked = true
            unlockedCosmetic = true
            Log.i(TAG, "Cosmetic Pack unlocked successfully!")
        }

        if (purchase.products.contains(PRODUCT_PARENTAL_MONTHLY)) {
            dataManager.isParentalSubscribed = true
            subscribedParental = true
            Log.i(TAG, "Parental Control subscription activated!")
        }

        if (!purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Purchase acknowledged successfully.")
                } else {
                    Log.w(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                }
            }
        }

        val msg = when {
            unlockedCosmetic && subscribedParental -> "All Kitties & Parental Control unlocked!"
            unlockedCosmetic -> "Tabby, Saturn, Rolex, Salmon, and Katrina breeds unlocked!"
            subscribedParental -> "Guardian Parental Alerts activated!"
            else -> "Purchase successful!"
        }
        onPurchaseResultListener?.invoke(true, msg)
    }

    fun restorePurchases(onComplete: (Boolean, String) -> Unit) {
        if (!billingClient.isReady) {
            startConnection { restorePurchases(onComplete) }
            return
        }

        var restoredCount = 0

        // Query In-App
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
            if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in inAppPurchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (purchase.products.contains(PRODUCT_COSMETIC_PACK)) {
                            dataManager.isCosmeticPackUnlocked = true
                            restoredCount++
                        }
                        if (!purchase.isAcknowledged) {
                            acknowledgeAsync(purchase)
                        }
                    }
                }
            }

            // Query Subscriptions
            val subsParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
                if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (purchase in subsPurchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            if (purchase.products.contains(PRODUCT_PARENTAL_MONTHLY)) {
                                dataManager.isParentalSubscribed = true
                                restoredCount++
                            }
                            if (!purchase.isAcknowledged) {
                                acknowledgeAsync(purchase)
                            }
                        }
                    }
                }

                if (restoredCount > 0) {
                    onComplete(true, "Successfully restored $restoredCount purchases!")
                } else {
                    onComplete(false, "No active purchases found for this Google account.")
                }
            }
        }
    }

    fun queryActivePurchases() {
        if (!billingClient.isReady) return

        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(inAppParams) { res, purchases ->
            if (res.responseCode == BillingClient.BillingResponseCode.OK) {
                for (p in purchases) {
                    if (p.purchaseState == Purchase.PurchaseState.PURCHASED && p.products.contains(PRODUCT_COSMETIC_PACK)) {
                        dataManager.isCosmeticPackUnlocked = true
                    }
                }
            }
        }

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { res, purchases ->
            if (res.responseCode == BillingClient.BillingResponseCode.OK) {
                for (p in purchases) {
                    if (p.purchaseState == Purchase.PurchaseState.PURCHASED && p.products.contains(PRODUCT_PARENTAL_MONTHLY)) {
                        dataManager.isParentalSubscribed = true
                    }
                }
            }
        }
    }

    private fun acknowledgeAsync(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
