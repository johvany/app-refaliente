package com.di.refaliente.shared

import java.text.NumberFormat
import java.util.*

class NumberFormatHelper {
    private val numberFormat = NumberFormat.getInstance(Locale.US)

    fun doubleToStr(decimals: Int, number: Double): String {
        return String.format(Locale.US, "%.${decimals}f", number)
    }

    fun strToDouble(number: String): Double {
        var n = 0.0

        try {
            numberFormat.parse(number)?.let { n = it.toDouble() }
        } catch (error: Exception) {
            // ...
        }

        return n
    }
}