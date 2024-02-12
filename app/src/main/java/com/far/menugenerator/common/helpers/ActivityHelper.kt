package com.far.menugenerator.common.helpers

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageOptions
import com.far.menugenerator.R
import com.far.menugenerator.view.common.BaseActivity

object ActivityHelper {
    fun hideActionBar(activity:BaseActivity){
        activity.supportActionBar?.hide()
    }
    fun hideKeyboard(activity:BaseActivity, windowToken:IBinder){
        val imm = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    fun callCropImage(baseActivity: BaseActivity) {
        //Implementar onActivityresult en la actividad desde dode se llame
        val intent = Intent(baseActivity, CropImageActivity::class.java)
        val options = CropImageOptions()
        options.activityBackgroundColor = baseActivity.getColor(R.color.grey_900)
        options.backgroundColor = baseActivity.getColor(R.color.grey_900)
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_OPTIONS,options)
        baseActivity.startActivityForResult(intent, BaseActivity.REQUEST_CODE_CROP_IMAGE)
    }
}