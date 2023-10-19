package com.far.menugenerator.common.utils

import java.text.DecimalFormat

object StringUtils {

    fun doubleToMoneyString(amount:Double, currencyFormat:String):String{
        val decimalFormat = DecimalFormat(currencyFormat)
        return decimalFormat.format(amount)
    }


}