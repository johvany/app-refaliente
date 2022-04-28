package com.di.refaliente.shared

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

@Suppress("MemberVisibilityCanBePrivate", "unused")
class NumberFormatHelper {
    private val numberFormat = NumberFormat.getInstance(Locale.US)
    private val decimalFormat2Decimals = DecimalFormat("#,###,###,##0.00")

    fun doubleToStr(decimals: Int, number: Double): String {
        return String.format(Locale.US, "%.${decimals}f", number)
    }

    fun strToDouble(number: String): Double {
        var n = 0.0
        try { numberFormat.parse(number)?.let { n = it.toDouble() } } catch (error: Exception) { /* ... */ }
        return n
    }

    fun format2Decimals(number: String): String {
        return decimalFormat2Decimals.format(strToDouble(number))
    }
}