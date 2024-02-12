package com.far.menugenerator.view.common

import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageOptions
import com.far.menugenerator.R
import com.far.menugenerator.common.helpers.ActivityHelper
import com.far.menugenerator.model.denpendencyInjection.activity.ActivityComponent
import com.far.menugenerator.model.denpendencyInjection.activity.ActivityModule
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationComponent
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationModule
import com.far.menugenerator.view.MyApplication


open class BaseActivity :AppCompatActivity(){
    companion object{
        const val REQUEST_CODE_CROP_IMAGE = 200
    }

    val applicationComponent get() = (application as MyApplication).appComponent

    val activityComponent:ActivityComponent by lazy {
        applicationComponent.newActivityComponent(ActivityModule(this))
    }
    protected val presentationComponent:PresentationComponent by lazy {
        activityComponent.newPresentationComponent(PresentationModule())
    }

    fun hideKeyboard(windowToken:IBinder){
        ActivityHelper.hideKeyboard(this,windowToken)
    }
    fun hideActionBar(){
        ActivityHelper.hideActionBar(this)
    }

    fun callCropImage() {
        ActivityHelper.callCropImage(this)
    }

}