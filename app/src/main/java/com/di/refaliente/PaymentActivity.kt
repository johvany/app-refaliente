package com.di.refaliente

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.di.refaliente.databinding.ActivityPaymentBinding
import com.di.refaliente.shared.*
import com.di.refaliente.view_adapters.CardMonthsAdapter
import com.di.refaliente.view_adapters.CardYearsAdapter
import mx.openpay.android.Openpay
import mx.openpay.android.OperationCallBack
import mx.openpay.android.OperationResult
import mx.openpay.android.exceptions.OpenpayServiceException
import mx.openpay.android.exceptions.ServiceUnavailableException
import mx.openpay.android.model.Card
import mx.openpay.android.model.Token
import org.json.JSONObject

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var loading: LoadingWindow
    private lateinit var customAlertDialog: CustomAlertDialog
    private var idPublication = 0
    private var idSelectedAddress = 0

    // ... Delete when finish testing ...
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initVars()
        loadCardExpMonths()
        loadCardExpYears()
        binding.performPayment.setOnClickListener { performProductPaymentSetp1() }

        // ... Delete when finish testing ...
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
            loading.show()

            try {
                val openpay = Openpay("mnlzf7phddp7tgia3ifp", "pk_985217fa745c4f5da1e88b90f69e9ddf", false)
                val deviceSessionId = openpay.deviceCollectorDefaultImpl.setup(this)

                val card = Card()
                card.holderName = binding.cardUserName.text.toString()
                card.cardNumber = binding.cardNumber.text.toString()
                card.expirationMonth = (binding.cardExpMonths.selectedItem as CardMonth).value
                card.expirationYear = (binding.cardExpYears.selectedItem as CardYear).value
                card.cvv2 = binding.cardCvv.text.toString()

                openpay.createToken(card, object: OperationCallBack<Token> {
                    override fun onError(error: OpenpayServiceException?) {
                        loading.hide()
                        Utilities.showRequestError(customAlertDialog, error?.description)
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
                startActivity(Intent(this, HomeMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
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

    // ... Get this data from backend ...
    private fun loadCardExpYears() {
        val cardYearsItems = ArrayList<CardYear>()
        cardYearsItems.add(CardYear("22"))
        cardYearsItems.add(CardYear("23"))
        cardYearsItems.add(CardYear("24"))
        cardYearsItems.add(CardYear("25"))
        cardYearsItems.add(CardYear("26"))
        binding.cardExpYears.adapter = CardYearsAdapter(cardYearsItems, layoutInflater)
    }
}