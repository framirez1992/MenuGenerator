package com.far.menugenerator.viewModel

import android.accounts.NetworkErrorException
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.common.global.Constants
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.model.api.TinyUrlAPIInterface
import com.far.menugenerator.model.api.model.TinyUrlRequest
import com.far.menugenerator.viewModel.model.ProcessState
import com.far.menugenerator.viewModel.model.State
import com.far.menugenerator.model.firebase.firestore.MenuService
import com.far.menugenerator.model.firebase.firestore.model.MenuFirebase
import com.far.menugenerator.model.firebase.storage.MenuStorage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.await
import java.io.File
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Provider

class QRPreviewViewModel(
    private val menuService: MenuService,
    private val menuStorage: MenuStorage,
    private val tinyUrlService: TinyUrlAPIInterface
): ViewModel() {

    private val state = MutableLiveData<ProcessState>()
    private val menuFirebase = MutableLiveData<MenuFirebase?>()
    private val stateFileDownload = MutableLiveData<ProcessState>()
    private val qrBitmap = MutableLiveData<Bitmap>()

    private var _userId:String?=null
    private var _companyId:String?=null
    private var _menuId:String?=null

    val userId get() = _userId
    val companyId get() = _companyId
    val menuId get() = _menuId

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

    fun drawMenu(){
        state.value = ProcessState(State.LOADING)
        viewModelScope.launch {
            try{
                if(!NetworkUtils.isConnectedToInternet()){
                    throw NetworkErrorException()
                }

                val menu = menuService.getMenu(user = _userId!!, companyId = _companyId!!, menuId = _menuId!!)
                menuFirebase.postValue(menu)
                val url:String
                if(menu?.shorUrl != null){
                    url = menu.shorUrl!!
                }else{
                    url = shortUrl(menu = menu!!)
                    //actualiza el url localmente
                    menuFirebase.postValue(menuFirebase.value?.copy(shorUrl = url))
                }
                val bm = FileUtils.generateQRCode(url)
                qrBitmap.postValue(bm)
                state.postValue(ProcessState(State.SUCCESS))
            }catch (e:NetworkErrorException){
                state.postValue(ProcessState(State.NETWORK_ERROR))
            }catch (e:Exception){
                state.postValue(ProcessState(State.GENERAL_ERROR, message = e.message))
            }

        }
    }

   private suspend fun shortUrl(menu:MenuFirebase):String {
        val tinyUrlRequest = TinyUrlRequest(url = menu.fileUrl)
        val response = tinyUrlService.createPost(body = tinyUrlRequest, token = Constants.TYNY_URL_TOKEN).await()
        if (response.code == 0) {
            Log.i("TINY_URL", Gson().toJson(response))
            menu.shorUrl = response.data?.tiny_url
            menuService.updateMenu(user = userId!!,companyId = companyId!!, menu)
           return menu.shorUrl!!
        }else{
            throw Exception("tinyUrl error:${response.errors.joinToString(", ")}")
        }

    }

    fun initialize(userId:String, companyId: String, menuId:String) {
        _userId = userId
        _companyId = companyId
        _menuId = menuId
    }


    class QRPreviewViewModelFactory @Inject constructor(
        private val menuService: Provider<MenuService>,
        private val menuStorage: Provider<MenuStorage>,
        private val tinyUrlService: Provider<TinyUrlAPIInterface>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QRPreviewViewModel(menuService.get(),menuStorage.get(), tinyUrlService.get()) as T
        }
    }

}