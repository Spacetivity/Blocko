package net.spacetivity.blocko.utils

import java.text.NumberFormat
import java.util.*

object NumberUtils {

    fun format(int: Int): String {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale("de", "DE")) // Specify the desired locale
        val formattedNumber: String = numberFormat.format(int)
        return formattedNumber
    }

}