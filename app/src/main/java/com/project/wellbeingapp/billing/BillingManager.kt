package com.project.wellbeingapp.billing

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Verbindungs-/Kaufzustand für die Paywall-UI. */
enum class BillingStatus { CONNECTING, READY, ERROR }

/**
 * Kapselt die Google-Play-Billing-Anbindung für die Premium-Freischaltung
 * (einmaliger In-App-Kauf, kein Abo). Die Berechtigung ist bewusst NICHT lokal
 * persistiert — Quelle der Wahrheit ist [BillingClient.queryPurchasesAsync],
 * sodass der Kauf geräteübergreifend gilt und nicht durch App-Daten-Löschen
 * umgangen werden kann.
 *
 * App-weites Singleton (siehe AppContainer). [start] beim App-Start aufrufen,
 * [refresh] bei jedem ON_RESUME (fängt Käufe ab, die außerhalb der App passierten).
 */
class BillingManager(context: Context) : PurchasesUpdatedListener {

    companion object {
        /**
         * MUSS exakt mit der Produkt-ID des in der Play Console angelegten
         * (einmaligen) In-App-Produkts übereinstimmen.
         */
        const val PREMIUM_PRODUCT_ID = "premium_unlock"
    }

    private val appContext = context.applicationContext

    private val _isPremium = MutableStateFlow(false)
    /** true, sobald der Premium-Kauf vorliegt (bestätigt oder noch zu bestätigen). */
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _price = MutableStateFlow<String?>(null)
    /** Lokalisierter Preis-String (z. B. „2,99 €") fürs Anzeigen; null bis geladen. */
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _status = MutableStateFlow(BillingStatus.CONNECTING)
    val status: StateFlow<BillingStatus> = _status.asStateFlow()

    /** Zwischengespeicherte Produktdetails — nötig, um den Kauf-Flow zu starten. */
    private var productDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    /** Verbindung aufbauen und Produkt + bestehende Käufe abfragen. Idempotent. */
    fun start() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryPurchases()
            return
        }
        _status.value = BillingStatus.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryPurchases()
                } else {
                    _status.value = BillingStatus.ERROR
                }
            }

            override fun onBillingServiceDisconnected() {
                _status.value = BillingStatus.ERROR
            }
        })
    }

    /** Aktuellen Kaufstatus neu abfragen (oder Verbindung erneut aufbauen). */
    fun refresh() {
        if (billingClient.isReady) queryPurchases() else start()
    }

    private fun queryProductDetails() {
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
        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = queryResult.productDetailsList.firstOrNull()
                productDetails = details
                _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
                _status.value = BillingStatus.READY
            } else {
                _status.value = BillingStatus.ERROR
            }
        }
    }

    /** Startet den Google-Play-Kauf-Dialog. Benötigt die aktuelle Activity. */
    fun launchPurchase(activity: Activity) {
        val details = productDetails ?: run {
            // Produkt noch nicht geladen → erneut versuchen, Nutzer kann gleich nochmal tippen.
            queryProductDetails()
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            // Einmalprodukt: KEIN setOfferToken (nur bei Abos nötig).
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
        // USER_CANCELED / Fehler: nichts tun — die Paywall bleibt einfach stehen.
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val owned = purchases.any {
            it.products.contains(PREMIUM_PRODUCT_ID) &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (owned) _isPremium.value = true

        // Käufe bestätigen, sonst storniert Google sie nach 3 Tagen automatisch.
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { acknowledge(it) }
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { /* Ergebnis hier unkritisch */ }
    }
}
