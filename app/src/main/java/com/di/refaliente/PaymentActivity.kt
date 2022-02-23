package com.di.refaliente

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.di.refaliente.databinding.ActivityPaymentBinding
import com.di.refaliente.shared.CardMonth
import com.di.refaliente.shared.CardYear
import com.di.refaliente.shared.LoadingWindow
import com.di.refaliente.view_adapters.CardMonthsAdapter
import com.di.refaliente.view_adapters.CardYearsAdapter

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var loading: LoadingWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initVars()
        loadCardExpMonths()
        loadCardExpYears()
        binding.performPayment.setOnClickListener { performProductPayment() }
    }

    private fun initVars() {
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

    private fun performProductPayment() {
        loading.show()

        Handler(Looper.getMainLooper()).postDelayed({
            loading.hide()
        }, 5000)

        /* try {
            val openpay = Openpay("ajklhsdjkajskd", "alskdjlkasjdkl", false)
            val deviceId = openpay.deviceCollectorDefaultImpl.setup(this)
            Toast.makeText(this, "device_session_id: $deviceId", Toast.LENGTH_LONG).show()
        } catch (err: Exception) {
            Toast.makeText(this, "ERROR\n\n: $err", Toast.LENGTH_LONG).show()
        } */
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