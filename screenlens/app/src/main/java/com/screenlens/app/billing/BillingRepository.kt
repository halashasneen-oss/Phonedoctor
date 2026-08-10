package com.screenlens.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The Play Console product ID ScreenLens Premium would be configured under, if billing is set up. */
const val PREMIUM_PRODUCT_ID = "screenlens_premium_lifetime"

sealed class PremiumState {
    data object Connecting : PremiumState()
    /** Play Billing connected fine, but [PREMIUM_PRODUCT_ID] doesn't exist in Play Console yet. */
    data object NotConfigured : PremiumState()
    data class Available(val productDetails: ProductDetails) : PremiumState()
    data object Purchased : PremiumState()
    data class Unavailable(val reason: String) : PremiumState()
}

/**
 * Real Google Play Billing Library integration. There is no fake purchase flow: if
 * [PREMIUM_PRODUCT_ID] has not actually been created in Play Console for this app,
 * [PremiumState.NotConfigured] is reported honestly instead of simulating success.
 */
class BillingRepository(context: Context) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow<PremiumState>(PremiumState.Connecting)
    val state: StateFlow<PremiumState> = _state.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() {
        if (billingClient.isReady) return
        _state.value = PremiumState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    restorePurchases()
                } else {
                    _state.value = PremiumState.Unavailable(
                        result.debugMessage.ifBlank { "Billing is unavailable on this device." }
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.value = PremiumState.Unavailable("Billing connection was lost.")
            }
        })
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        scope.launch {
            val result = billingClient.queryProductDetails(params)
            val details = result.productDetailsList.orEmpty()
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK && details.isNotEmpty()) {
                _state.value = PremiumState.Available(details.first())
            } else {
                // Connected to Play Billing successfully, but the product ID above has
                // not been created in Play Console — this is the real, honest state.
                _state.value = PremiumState.NotConfigured
            }
        }
    }

    private fun restorePurchases() {
        val params = com.android.billingclient.api.QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { _, purchases ->
            if (purchases.any { it.products.contains(PREMIUM_PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
                _state.value = PremiumState.Purchased
            }
        }
    }

    /** [productDetails] is a one-time (INAPP) product, which needs no offer token. */
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.contains(PREMIUM_PRODUCT_ID)
                ) {
                    _state.value = PremiumState.Purchased
                }
            }
        }
    }
}
