package com.far.menugenerator.common.utils

import android.telephony.PhoneNumberUtils
import com.google.gson.Gson
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


    fun objectToJson(obj:Any):String{
        val gson = Gson()
        return gson.toJson(obj)
    }

    fun formatPhone(phoneNumber:String):String{
       return PhoneNumberUtils.formatNumber(phoneNumber,Locale.getDefault().country)
    }

}