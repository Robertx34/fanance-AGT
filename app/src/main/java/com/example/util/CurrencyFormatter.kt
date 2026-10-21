package com.example.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)

    fun format(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun parseToDouble(value: String): Double {
        val cleanString = value.replace("[^0-9]".toRegex(), "")
        val parsed = cleanString.toDoubleOrNull() ?: 0.0
        return parsed / 100.0
    }
}
