package com.far.menugenerator.common.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale




object StringUtils {

    fun doubleToMoneyString(amount:Double, currencyFormat:String):String{
        val decimalFormat = DecimalFormat(currencyFormat)
        return decimalFormat.format(amount)
    }

    fun doubleToMoneyString(amount:Double,country:String,language:String):String
    =  NumberFormat.getCurrencyInstance(Locale(language, country)).format(amount)



}