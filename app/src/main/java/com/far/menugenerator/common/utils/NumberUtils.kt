package com.far.menugenerator.common.utils

object NumberUtils {
    fun stringIsNumber(value:String):Boolean{
        return value.toDoubleOrNull() == null
    }
}