package com.far.menugenerator.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.common.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QRPreviewViewModel: ViewModel() {
    val qrBitmap = MutableLiveData<Bitmap>()

    fun processUrl(url:String){
        viewModelScope.launch {
            try {
                val bm = FileUtils.generateQRCode(url)
                qrBitmap.postValue(bm)
            }catch (e:Exception){
                e.printStackTrace()
            }

        }
    }

}