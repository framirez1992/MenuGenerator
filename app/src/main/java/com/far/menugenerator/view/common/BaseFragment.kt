package com.far.menugenerator.view.common

import android.content.Intent
import androidx.fragment.app.Fragment
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageOptions
import com.far.menugenerator.R
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationComponent
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationModule

open class BaseFragment:Fragment() {

    val baseActivity get() = (activity as BaseActivity)
    val presentationComponent:PresentationComponent by lazy {
        baseActivity.activityComponent.newPresentationComponent(PresentationModule())
    }

    fun callCropImage() {
        //Implementar onActivityresult en la actividad desde dode se llame
        val intent = Intent(requireContext(), CropImageActivity::class.java)
        val options = CropImageOptions()
        //options.activityBackgroundColor = getColor(R.color.grey_900)
        //options.backgroundColor = getColor(R.color.grey_900)
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_OPTIONS,options)
        startActivityForResult(intent, BaseActivity.REQUEST_CODE_CROP_IMAGE)
    }
}