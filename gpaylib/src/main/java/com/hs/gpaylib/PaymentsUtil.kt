/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.samples.wallet

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import com.hs.gpaylib.GPayListener
import com.hs.gpaylib.GooglePayParam
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Contains helper static methods for dealing with the Payments API.
 *
 * Many of the parameters used in the code are optional and are set here merely to call out their
 * existence. Please consult the documentation to learn more and feel free to remove ones not
 * relevant to your implementation.
 */
object PaymentsUtil {
    val MICROS = BigDecimal(1000000.0)
    private lateinit var paymentsClient: PaymentsClient
    private lateinit var gPayListener: GPayListener
    public val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
    /**
     * Create a Google Pay API base request object with properties used in all requests.
     *
     * @return Google Pay API base request object.
     * @throws JSONException
     */
    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    fun initPaymentProcess(context: Activity,gPayListener: GPayListener){
        paymentsClient = createPaymentsClient(context)
        this.gPayListener = gPayListener
    }



    /**
     * Determine the viewer's ability to pay with a payment method supported by your app and display a
     * Google Pay payment button.
     *
     * @see [](https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient.html.isReadyToPay
    ) */
     fun possiblyShowGooglePayButton() {

        val isReadyToPayJson = isReadyToPayRequest() ?: return
        val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString()) ?: return

        // The call to isReadyToPay is asynchronous and returns a Task. We need to provide an
        // OnCompleteListener to be triggered when the result of the call is known.
        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                completedTask.getResult(ApiException::class.java)?.let(::setGooglePayAvailable)
            } catch (exception: ApiException) {
                // Process error
                exception.printStackTrace()
                Log.w("isReadyToPay failed", exception)
            }
        }
    }

    /**
     * An object describing accepted forms of payment by your app, used to determine a viewer's
     * readiness to pay.
     *
     * @return API version and payment methods supported by the app.
     * @see [IsReadyToPayRequest](https://developers.google.com/pay/api/android/reference/object.IsReadyToPayRequest)
     */
    fun isReadyToPayRequest(): JSONObject? {
        return try {
            val isReadyToPayRequest = JSONObject(baseRequest.toString())
            isReadyToPayRequest.put(
                "allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))

            isReadyToPayRequest

        } catch (e: JSONException) {
            null
        }
    }

    private fun setGooglePayAvailable(available: Boolean) {
        gPayListener.isAppElgibleForPayment(available)
    }





     fun requestPayment(googlePayParam: GooglePayParam?,context: Activity) {

        // Disables the button to prevent multiple clicks.

        // The price provided to the API should include taxes and shipping.
        // This price is not displayed to the user.
        val price = /*(selectedGarment.getDouble("price")).roundToLong()*/ "0.01"
        //val price = (garmentPriceMicros + shippingCost).microsToString()

        val paymentDataRequestJson = getPaymentDataRequest(googlePayParam)
        if (paymentDataRequestJson == null) {
            Log.e("RequestPayment", "Can't fetch payment data request")
            return
        }
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

        // Since loadPaymentData may show the UI asking the user to select a payment method, we use
        // AutoResolveHelper to wait for the user interacting with it. Once completed,
        // onActivityResult will be called with the result.
        if (request != null) {
            AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(request), context, LOAD_PAYMENT_DATA_REQUEST_CODE)
        }
    }

    /**
     * Gateway Integration: Identify your gateway and your app's gateway merchant identifier.
     *
     *
     * The Google Pay API response will return an encrypted payment method capable of being charged
     * by a supported gateway after payer authorization.
     *
     *
     * TODO: Check with your gateway on the parameters to pass and modify them in Constants.java.
     *
     * @return Payment data tokenization for the CARD payment method.
     * @throws JSONException
     * @see [PaymentMethodTokenizationSpecification](https://developers.google.com/pay/api/android/reference/object.PaymentMethodTokenizationSpecification)
     */
    private fun gatewayTokenizationSpecification(): JSONObject {
        if (Constants.PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS.isEmpty()) {
            throw RuntimeException(
                    "Please edit the Constants.java file to add gateway name and other " +
                    "parameters your processor requires")
        }

        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject(Constants.PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS))
        }
    }

    /**
     * `DIRECT` Integration: Decrypt a response directly on your servers. This configuration has
     * additional data security requirements from Google and additional PCI DSS compliance complexity.
     *
     *
     * Please refer to the documentation for more information about `DIRECT` integration. The
     * type of integration you use depends on your payment processor.
     *
     * @return Payment data tokenization for the CARD payment method.
     * @throws JSONException
     * @see [PaymentMethodTokenizationSpecification](https://developers.google.com/pay/api/android/reference/object.PaymentMethodTokenizationSpecification)
     */
    private fun directTokenizationSpecification(): JSONObject {
        if (Constants.DIRECT_TOKENIZATION_PUBLIC_KEY == "REPLACE_ME" ||
                (Constants.DIRECT_TOKENIZATION_PARAMETERS.isEmpty() ||
                 Constants.DIRECT_TOKENIZATION_PUBLIC_KEY.isEmpty())) {

            throw RuntimeException(
                    "Please edit the Constants.java file to add protocol version & public key.")
        }

        return JSONObject().apply {
            put("type", "DIRECT")
            put("parameters", JSONObject(Constants.DIRECT_TOKENIZATION_PARAMETERS))
        }
    }

    /**
     * Card networks supported by your app and your gateway.
     *
     *
     * TODO: Confirm card networks supported by your app and gateway & update in Constants.java.
     *
     * @return Allowed card networks
     * @see [CardParameters](https://developers.google.com/pay/api/android/reference/object.CardParameters)
     */
    private val allowedCardNetworks = JSONArray(Constants.SUPPORTED_NETWORKS)

    /**
     * Card authentication methods supported by your app and your gateway.
     *
     *
     * TODO: Confirm your processor supports Android device tokens on your supported card networks
     * and make updates in Constants.java.
     *
     * @return Allowed card authentication methods.
     * @see [CardParameters](https://developers.google.com/pay/api/android/reference/object.CardParameters)
     */
    private val allowedCardAuthMethods = JSONArray(Constants.SUPPORTED_METHODS)

    /**
     * Describe your app's support for the CARD payment method.
     *
     *
     * The provided properties are applicable to both an IsReadyToPayRequest and a
     * PaymentDataRequest.
     *
     * @return A CARD PaymentMethod object describing accepted cards.
     * @throws JSONException
     * @see [PaymentMethod](https://developers.google.com/pay/api/android/reference/object.PaymentMethod)
     */
    // Optionally, you can add billing address/phone number associated with a CARD payment method.
    private fun baseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {

            val parameters = JSONObject().apply {
                put("allowedAuthMethods", allowedCardAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
                put("billingAddressRequired", true)
                put("billingAddressParameters", JSONObject().apply {
                    put("format", "FULL")
                })
            }

            put("type", "CARD")
            put("parameters", parameters)
        }
    }

    /**
     * Describe the expected returned payment data for the CARD payment method
     *
     * @return A CARD PaymentMethod describing accepted cards and optional fields.
     * @throws JSONException
     * @see [PaymentMethod](https://developers.google.com/pay/api/android/reference/object.PaymentMethod)
     */
    private fun cardPaymentMethod(): JSONObject {
        val cardPaymentMethod = baseCardPaymentMethod()
        cardPaymentMethod.put("tokenizationSpecification", gatewayTokenizationSpecification())

        return cardPaymentMethod
    }


    /**
     * Information about the merchant requesting payment information
     *
     * @return Information about the merchant.
     * @throws JSONException
     * @see [MerchantInfo](https://developers.google.com/pay/api/android/reference/object.MerchantInfo)
     */
  /*  private val merchantInfo: JSONObject
        @Throws(JSONException::class)
        get() = JSONObject().put("merchantName", "Example Merchant")*/

    private val merchantInfo = JSONObject().apply {
        put("merchantName", "Example Merchant")
        put("merchantId", "01234567890123456789")
    }

    /**
     * Creates an instance of [PaymentsClient] for use in an [Activity] using the
     * environment and theme set in [Constants].
     *
     * @param activity is the caller's activity.
     */
    fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(Constants.PAYMENTS_ENVIRONMENT)
                .build()

        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    /**
     * Provide Google Pay API with a payment amount, currency, and amount status.
     *
     * @return information about the requested payment.
     * @throws JSONException
     * @see [TransactionInfo](https://developers.google.com/pay/api/android/reference/object.TransactionInfo)
     */
    @Throws(JSONException::class)
    private fun getTransactionInfo(googlePayParam: GooglePayParam?): JSONObject {
        return JSONObject().apply {
            put("totalPrice", googlePayParam?.price)
            put("totalPriceStatus", "FINAL")
            put("countryCode", googlePayParam?.countryCode)
            put("currencyCode", googlePayParam?.currency)
        }
    }

    /**
     * An object describing information requested in a Google Pay payment sheet
     *
     * @return Payment data expected by your app.
     * @see [PaymentDataRequest](https://developers.google.com/pay/api/android/reference/object.PaymentDataRequest)
     */
    fun getPaymentDataRequest(googlePayParam: GooglePayParam?): JSONObject? {
        try {
            return JSONObject(baseRequest.toString()).apply {
                put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod()))
                put("transactionInfo", getTransactionInfo(googlePayParam))
                put("merchantInfo", merchantInfo)

                // An optional shipping address requirement is a top-level property of the
                // PaymentDataRequest JSON object.
                val shippingAddressParameters = JSONObject().apply {
                    put("phoneNumberRequired", false)
                    put("allowedCountryCodes", JSONArray(Constants.SHIPPING_SUPPORTED_COUNTRIES))
                }
               /* put("shippingAddressRequired", false)
                put("shippingAddressParameters", shippingAddressParameters)*/
            }
        } catch (e: JSONException) {
            return null
        }

    }

    fun setListener(gPayListener: GPayListener) {
        this.gPayListener = gPayListener
    }








}

/**
 * Converts micros to a string format accepted by [PaymentsUtil.getPaymentDataRequest].
 *
 * @param micros value of the price.
 */
fun Long.microsToString() = BigDecimal(this)
        .divide(PaymentsUtil.MICROS)
        .setScale(2, RoundingMode.HALF_EVEN)
        .toString()
