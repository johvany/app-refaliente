package com.di.refaliente

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.databinding.ActivityPaymentBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.CardMonthsAdapter
import com.di.refaliente.view_adapters.CardYearsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mx.openpay.android.Openpay
import mx.openpay.android.OperationCallBack
import mx.openpay.android.OperationResult
import mx.openpay.android.exceptions.OpenpayServiceException
import mx.openpay.android.exceptions.ServiceUnavailableException
import mx.openpay.android.model.Card
import mx.openpay.android.model.Token
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var loading: LoadingWindow
    private lateinit var customAlertDialog: CustomAlertDialog
    private var idPublication = 0
    private var idSelectedAddress = 0

    @SuppressLint("SetTextI18n") // ... Delete when finish testing ...
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initVars()
        loadCardExpMonths()
        getCardExpYears()
        binding.performPayment.setOnClickListener { performProductPaymentSetp1() }

        // TODO: Delete this when release a version of this app in production.
        binding.title.setOnClickListener {
            binding.cardUserName.setText("Juan Perez Ramirez")
            binding.cardNumber.setText("4111111111111111")
            binding.cardCvv.setText("110")
        }
    }

    private fun initVars() {
        customAlertDialog = CustomAlertDialog(this)
        idPublication = intent.extras!!.getInt("id_publication")
        idSelectedAddress = intent.extras!!.getInt("id_selected_address")

        // The views passed here will be disabled when the loading window is showing and will be
        // enabled after hide the loading window.
        loading = LoadingWindow(binding.loading, arrayOf(
            binding.cardUserName,
            binding.cardNumber,
            binding.cardExpMonths,
            binding.cardExpYears,
            binding.cardCvv,
            binding.performPayment
        ))

        loading.setMessage("Procesando pago...")
    }

    private fun performProductPaymentSetp1() {
        if (ConnectionHelper.getConnectionType(this) == ConnectionHelper.NONE) {
            Utilities.showUnconnectedMessage(customAlertDialog)
        } else {
            when {
                !requiredFieldsFilled() -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Campos requeridos")
                        .setMessage("Por favor llena todos los campos requeridos.")
                        .setCancelable(true)
                        .setPositiveButton("ACEPTAR", null)
                        .show()
                }
                !expirationMonthOk() -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Tarjeta expirada")
                        .setMessage("La fecha de expiración debe ser mayor o igual a la fecha actual. Por favor revisa el mes y año de expiración.")
                        .setCancelable(true)
                        .setPositiveButton("ACEPTAR", null)
                        .show()
                }
                else -> {
                    loading.show()

                    try {
                        val openpay = Openpay(
                            "mnlzf7phddp7tgia3ifp",
                            "pk_985217fa745c4f5da1e88b90f69e9ddf",
                            false
                        )
                        val deviceSessionId = openpay.deviceCollectorDefaultImpl.setup(this)

                        val card = Card()
                        card.holderName = binding.cardUserName.text.toString()
                        card.cardNumber = binding.cardNumber.text.toString()
                        card.expirationMonth =
                            (binding.cardExpMonths.selectedItem as CardMonth).value
                        card.expirationYear = (binding.cardExpYears.selectedItem as CardYear).value
                        card.cvv2 = binding.cardCvv.text.toString()

                        openpay.createToken(card, object : OperationCallBack<Token> {
                            override fun onError(error: OpenpayServiceException?) {
                                loading.hide()

                                error?.errorCode?.let { code ->
                                    val msg = when (code) {
                                        3001 -> "La tarjeta fue declinada por el banco (código $code)."
                                        3002 -> "La tarjeta ha expirado (código $code)."
                                        3003 -> "La tarjeta no tiene fondos suficientes (código $code)."
                                        3004 -> "La tarjeta ha sido identificada como una tarjeta robada (código $code)."
                                        3005 -> "La tarjeta ha sido rechazada por el sistema antifraude (código $code)."
                                        else -> "Código del incidente: $code (${error.description})"
                                    }

                                    Utilities.showRequestError(customAlertDialog, msg)
                                }
                            }

                            override fun onCommunicationError(error: ServiceUnavailableException?) {
                                loading.hide()
                                Utilities.showUnconnectedMessage(customAlertDialog)
                            }

                            override fun onSuccess(operationResult: OperationResult<Token>?) {
                                operationResult?.result?.id?.let { tokenId ->
                                    performProductPaymentSetp2(
                                        idPublication,
                                        idSelectedAddress,
                                        tokenId,
                                        deviceSessionId,
                                        true
                                    )
                                }
                            }
                        })
                    } catch (err: Exception) {
                        loading.hide()
                        Utilities.showUnknownError(customAlertDialog, null)
                    }
                }
            }
        }
    }

    private fun expirationMonthOk(): Boolean {
        return (binding.cardExpMonths.selectedItem as CardMonth).value.toInt() >= SimpleDateFormat("MM", Locale.ROOT).format(Date()).toInt()
    }

    private fun requiredFieldsFilled(): Boolean {
        var allok = true

        // Reset all errors of all required fields
        binding.cardUserNameContainer.error = null
        binding.cardUserNameContainer.isErrorEnabled = false
        binding.cardNumberContainer.error = null
        binding.cardNumberContainer.isErrorEnabled = false
        binding.cardCvvContainer.error = null
        binding.cardCvvContainer.isErrorEnabled = false

        if (binding.cardUserName.text.toString() == "") {
            allok = false
            binding.cardUserNameContainer.error = " "
            binding.cardUserNameContainer.isErrorEnabled = true
        }

        if (binding.cardNumber.text.toString() == "") {
            allok = false
            binding.cardNumberContainer.error = " "
            binding.cardNumberContainer.isErrorEnabled = true
        }

        if (binding.cardCvv.text.toString() == "") {
            allok = false
            binding.cardCvvContainer.error = " "
            binding.cardCvvContainer.isErrorEnabled = true
        }

        return allok
    }

    private fun performProductPaymentSetp2(
        idPublication: Int,
        idCustomerAddress: Int,
        tokenId: String,
        deviceSessionId: String,
        canRepeat: Boolean
    ) {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.POST,
            resources.getString(R.string.api_url) + "products/buy",
            JSONObject()
                .put("id_publication", idPublication)
                .put("id_customer_address", idCustomerAddress)
                .put("token_id", tokenId)
                .put("device_session_id", deviceSessionId),
            {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Compra realizada")
                    .setMessage(HtmlCompat.fromHtml(
                        "¡Enhorabuena tu compra se realizo correctamente! Puedes encontrarla en el menú <strong>Mis compras</strong>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ))
                    .setCancelable(false)
                    .setNegativeButton("SEGUIR BUSCANDO") { _, _ ->
                        startActivity(Intent(this, HomeMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    }
                    .setPositiveButton("VER MIS COMPRAS") { _, _ ->
                        startActivity(Intent(this, HomeMenuActivity::class.java)
                            .putExtra("should_load_purchases", true)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    }
                    .show()
            },
            { error ->
                SessionHelper.handleRequestError(error, this, customAlertDialog) {
                    if (canRepeat) {
                        performProductPaymentSetp2(idPublication, idCustomerAddress, tokenId, deviceSessionId, false)
                    } else {
                        Utilities.showRequestError(customAlertDialog, null)
                    }
                }
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", SessionHelper.user!!.token))
            }
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    private fun loadCardExpMonths() {
        val cardMonthsItems = ArrayList<CardMonth>()
        cardMonthsItems.add(CardMonth("01"))
        cardMonthsItems.add(CardMonth("02"))
        cardMonthsItems.add(CardMonth("03"))
        cardMonthsItems.add(CardMonth("04"))
        cardMonthsItems.add(CardMonth("05"))
        cardMonthsItems.add(CardMonth("06"))
        cardMonthsItems.add(CardMonth("07"))
        cardMonthsItems.add(CardMonth("08"))
        cardMonthsItems.add(CardMonth("09"))
        cardMonthsItems.add(CardMonth("10"))
        cardMonthsItems.add(CardMonth("11"))
        cardMonthsItems.add(CardMonth("12"))
        binding.cardExpMonths.adapter = CardMonthsAdapter(cardMonthsItems, layoutInflater)
    }

    private fun getCardExpYears() {
        Utilities.queue?.add(object: JsonObjectRequest(
            Method.GET,
            resources.getString(R.string.api_url) + "payments/card-years",
            null,
            { response ->
                loadCardExpYears(response.getJSONArray("card_years"))
            },
            { error ->
                try {
                    Utilities.showRequestError(customAlertDialog, error.networkResponse.data.decodeToString())
                } catch (err: Exception) {
                    Utilities.showRequestError(customAlertDialog, error.toString())
                }
            }
        ) {
            // Set request headers here if you need.
        }.apply {
            retryPolicy = DefaultRetryPolicy(
                ConstantValues.REQUEST_TIMEOUT,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
        })
    }

    private fun loadCardExpYears(data: JSONArray) {
        val limit = data.length()
        val cardYearsItems = ArrayList<CardYear>()
        for (i in 0 until limit) { cardYearsItems.add(CardYear(data.getString(i))) }
        binding.cardExpYears.adapter = CardYearsAdapter(cardYearsItems, layoutInflater)
    }
}