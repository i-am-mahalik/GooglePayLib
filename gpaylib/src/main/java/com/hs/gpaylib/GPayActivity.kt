package com.hs.gpaylib

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.samples.wallet.PaymentsUtil
import com.google.android.gms.wallet.*
import org.json.JSONException
import org.json.JSONObject

class GPayActivity : AppCompatActivity() {

    /**
     * A client for interacting with the Google Pay API.
     *
     * @see [PaymentsClient](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient)
     */
    private val shippingCost = (90 * 1000000).toLong()
    private var googlePayButton: RelativeLayout? = null
    private var googlePayParam:GooglePayParam? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_gpay)
        googlePayParam = intent.extras?.getSerializable(BUNDLE_KEY) as GooglePayParam
        PaymentsUtil.requestPayment(googlePayParam,this@GPayActivity)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // value passed in AutoResolveHelper
            PaymentsUtil.LOAD_PAYMENT_DATA_REQUEST_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK ->
                        data?.let { intent ->
                            PaymentData.getFromIntent(intent)?.let(::handlePaymentSuccess)
                        }
                    Activity.RESULT_CANCELED -> {


                    }

                    AutoResolveHelper.RESULT_ERROR -> {
                        AutoResolveHelper.getStatusFromIntent(data)?.let {
                            handleError(it)

                        }
                    }
                }
                // Re-enables the Google Pay payment button.
                googlePayButton?.isClickable = true
            }
        }
        setResult(Activity.RESULT_OK)
        finish()
        Log.d("onActivityResult","onActivityResult"+"GPAyActivity")
    }


/**
 * PaymentData response object contains the payment information, as well as any additional
 * requested information, such as billing and shipping address.
 *
 * @param paymentData A response object returned by Google after a payer approves payment.
 * @see [Payment
 * Data](https://developers.google.com/pay/api/android/reference/object.PaymentData)
 */
private fun handlePaymentSuccess(paymentData: PaymentData) {
    val paymentInformation = paymentData.toJson() ?: return

    try {
        // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
        val paymentMethodData = JSONObject(paymentInformation).getJSONObject("paymentMethodData")

        // If the gateway is set to "example", no payment information is returned - instead, the
        // token will only consist of "examplePaymentMethodToken".
        if (paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("type") == "PAYMENT_GATEWAY" && paymentMethodData
                .getJSONObject("tokenizationData")
                .getString("token") == "examplePaymentMethodToken") {

            AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("Gateway name set to \"example\" - please modify " +
                        "Constants.java and replace it with your own gateway.")
                .setPositiveButton("OK", null)
                .create()
                .show()
        }

        val billingName = paymentMethodData.getJSONObject("info")
            .getJSONObject("billingAddress").getString("name")
        Log.d("BillingName", billingName)

        Toast.makeText(this, getString(com.hs.gpaylib.R.string.payments_show_name, billingName), Toast.LENGTH_LONG).show()

        // Logging token string.
        Log.d("GooglePaymentToken", paymentMethodData
            .getJSONObject("tokenizationData")
            .getString("token"))

    } catch (e: JSONException) {
        Log.e("handlePaymentSuccess", "Error: " + e.toString())
    }

}



    /**
     * At this stage, the user has already seen a popup informing them an error occurred. Normally,
     * only logging is required.
     *
     * @param statusCode will hold the value of any constant from CommonStatusCode or one of the
     * WalletConstants.ERROR_CODE_* constants.
     * @see [
     * Wallet Constants Library](https://developers.google.com/android/reference/com/google/android/gms/wallet/WalletConstants.constant-summary)
     */
    private fun handleError(status: Status) {
        Log.w("loadPaymentData failed", String.format("Error code: %d", status.statusCode))
        Log.w("loadPaymentData failed", String.format("Desc:", status.zzg()))

    }





companion object {
    const val BUNDLE_KEY :String = "gpayparam"
}

}
