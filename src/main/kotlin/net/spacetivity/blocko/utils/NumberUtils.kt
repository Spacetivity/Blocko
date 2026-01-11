package net.spacetivity.blocko.utils

import java.text.NumberFormat
import java.util.*

object NumberUtils {

    fun format(int: Int): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.of("de", "DE"))
        val formattedNumber = numberFormat.format(int)
        return formattedNumber
    }

}