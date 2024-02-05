package com.far.menugenerator.common.helpers

import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.far.menugenerator.view.common.BaseActivity

object ActivityHelper {
    fun hideActionBar(activity:BaseActivity){
        activity.supportActionBar?.hide()
    }
    fun hideKeyboard(activity:BaseActivity, windowToken:IBinder){
        val imm = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}