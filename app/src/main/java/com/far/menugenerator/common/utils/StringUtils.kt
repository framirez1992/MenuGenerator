package com.far.menugenerator.common.utils

import android.telephony.PhoneNumberUtils
import android.util.Patterns
import com.google.gson.Gson
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.regex.Pattern


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
        try {
            return PhoneNumberUtils.formatNumber(phoneNumber,Locale.getDefault().country)
        }catch (e:Exception){
            return phoneNumber
        }

    }

    fun isValidEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isValidPassword(password: String): Boolean {
        val passwordPattern = "^\\S{6,}$" // Ensures no whitespace and length of 6
        val matcher = Pattern.compile(passwordPattern).matcher(password)
        return matcher.matches()
    }

}