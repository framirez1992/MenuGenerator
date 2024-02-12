package com.far.menugenerator.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.storage.MenuStorage
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

class QRPreviewViewModel(
    private val menuService:MenuService,
    private val menuStorage:MenuStorage): ViewModel() {

    private val state = MutableLiveData<ProcessState>()
    private val menuFirebase = MutableLiveData<MenuFirebase?>()
    private val stateFileDownload = MutableLiveData<ProcessState>()
    private val qrBitmap = MutableLiveData<Bitmap>()

    fun getState():LiveData<ProcessState> = state
    fun getQrBitmap():LiveData<Bitmap> = qrBitmap
    fun getStateFileDownload():LiveData<ProcessState> = stateFileDownload


    fun getFile(destination:File){
        stateFileDownload.value = ProcessState(State.LOADING)
        viewModelScope.launch {
            try{
                menuStorage.downloadFileStorageReferenceFromUrl(url = menuFirebase.value?.fileUrl!!, destination = destination)
                stateFileDownload.value = ProcessState(State.SUCCESS)
            }catch (e:Exception){
                stateFileDownload.value = ProcessState(State.ERROR)
            }
        }

    }

    fun drawMenu(user:String, companyId:String, fireBaseRef:String){
        state.value = ProcessState(State.LOADING)

        viewModelScope.launch {
            try{
                val menu = menuService.getMenu(user = user, companyId = companyId, firebaseRef = fireBaseRef)
                menuFirebase.postValue(menu)
                val bm = FileUtils.generateQRCode(menu?.fileUrl!!)
                qrBitmap.postValue(bm)
                state.postValue(ProcessState(State.SUCCESS))
            }catch (e:Exception){
                state.postValue(ProcessState(State.ERROR))
            }

        }
    }

    class QRPreviewViewModelFactory @Inject constructor(
        private val menuService: Provider<MenuService>,
        private val menuStorage: Provider<MenuStorage>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QRPreviewViewModel(menuService.get(),menuStorage.get()) as T
        }
    }

}