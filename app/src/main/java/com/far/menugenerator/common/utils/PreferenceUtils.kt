package com.far.menugenerator.common.utils

import android.content.Context


const val LOCAL_PREFERENCE="localPreference"
const val SHOW_DEMO_PREF="showDemo"
const val SHOW_NO_COMPANY_ALERT="noCompanyAlert"
object PreferenceUtils {
    fun setShowDemoPreference(context:Context, value:Boolean){
        savePreference(context = context, key = SHOW_DEMO_PREF ,value)
    }
    fun getShowDemoPreference(context: Context, defaultValue:Boolean):Boolean{
        val sharedPref = getSharedPreference(context)
        return sharedPref.getBoolean(SHOW_DEMO_PREF,defaultValue)
    }
    fun setShowNoCompanyAlert(context: Context, value:Boolean){
        savePreference(context = context, key = SHOW_NO_COMPANY_ALERT ,value)
    }
    fun getShowNoCompanyAlert(context: Context, defaultValue:Boolean):Boolean{
        val sharedPref = getSharedPreference(context)
        return sharedPref.getBoolean(SHOW_NO_COMPANY_ALERT,defaultValue)
    }

    private fun savePreference(context:Context, key:String,value:Boolean){
        val shared = getEditor(context = context)
        shared.putBoolean(key,value)
        shared.apply()
    }

    private fun getSharedPreference(context: Context) = context.getSharedPreferences(LOCAL_PREFERENCE, Context.MODE_PRIVATE)
    private fun getEditor(context: Context) = getSharedPreference(context=context).edit()
}