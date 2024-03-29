package com.far.menugenerator.viewModel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.storage.MenuStorage
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Provider

class QRPreviewViewModel(
    private val menuService:MenuService,
    private val menuStorage:MenuStorage): ViewModel() {

    private val state = MutableLiveData<ProcessState>()
    private val menuFirebase = MutableLiveData<MenuFirebase?>()
    private val stateFileDownload = MutableLiveData<ProcessState>()
    private val qrBitmap = MutableLiveData<Bitmap>()

    //private lateinit var menuRef:MenuReference
    private var _companyId:String?=null
    private var _menuFireBaseRef:String?=null

    val companyId get() = _companyId
    val menuFirebaseRef get() = _menuFireBaseRef

    fun getState():LiveData<ProcessState> = state
    fun getQrBitmap():LiveData<Bitmap> = qrBitmap
    fun getStateFileDownload():LiveData<ProcessState> = stateFileDownload
    fun getMenu():LiveData<MenuFirebase?> = menuFirebase


    fun getFile(destination:File){
        stateFileDownload.value = ProcessState(State.LOADING)
        viewModelScope.launch {
            try{
                if(!NetworkUtils.isConnectedToInternet()){
                    throw TimeoutException()
                }
                menuStorage.downloadFileStorageReferenceFromUrl(url = menuFirebase.value?.fileUrl!!, destination = destination)
                stateFileDownload.value = ProcessState(State.SUCCESS)
            }catch (e:TimeoutException){
                stateFileDownload.value = ProcessState(State.NETWORK_ERROR)
            }
            catch (e:Exception){
                stateFileDownload.value = ProcessState(State.GENERAL_ERROR)
            }
        }

    }

    fun drawMenu(user:String){
        state.value = ProcessState(State.LOADING)

        viewModelScope.launch {
            try{
                val menu = menuService.getMenu(user = user, companyId = _companyId!!, firebaseRef = _menuFireBaseRef!!)
                menuFirebase.postValue(menu)
                val url = menu?.fileUrl!!.split("&token")[0]//SIN TOKEN
                val bm = FileUtils.generateQRCode(url)
                qrBitmap.postValue(bm)
                state.postValue(ProcessState(State.SUCCESS))
            }catch (e:Exception){
                state.postValue(ProcessState(State.GENERAL_ERROR))
            }

        }
    }

    fun initialize(companyId: String, menuFireBaseRef:String) {
        _companyId = companyId
        _menuFireBaseRef = menuFireBaseRef
    }

    /*
     fun shortenUrl() {
        viewModelScope.launch(Dispatchers.IO) {

            val url = "https://tinyurl.com/api-create.php?url=${menuFirebase.value?.fileUrl}"
            try {
                val response = withTimeout(5_000) { URL(url).readText() }
            } catch (e: Exception) {
                Log.e("TinyURL", "Error shortening URL: $e")
            }
        }
    }
    */


    class QRPreviewViewModelFactory @Inject constructor(
        private val menuService: Provider<MenuService>,
        private val menuStorage: Provider<MenuStorage>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QRPreviewViewModel(menuService.get(),menuStorage.get()) as T
        }
    }

}