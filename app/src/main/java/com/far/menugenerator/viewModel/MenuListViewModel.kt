package com.far.menugenerator.viewModel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.common.MenuReference
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.database.room.services.MenuDS
import com.far.menugenerator.model.storage.MenuStorage
import com.far.menugenerator.view.common.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Provider

class MenuListViewModel @Inject constructor(
    private val menuService:MenuService,
    private val menuStorage:MenuStorage,
    private val menuDS: MenuDS
): ViewModel() {

    private var fileUri: Uri? =null

    val menus = MutableLiveData<List<MenuReference?>>()
    private val searchMenuProcess = MutableLiveData<ProcessState>()
    private val deleteMenuProcess = MutableLiveData<ProcessState>()
    val previewFileState = MutableLiveData<ProcessState>()
    val shareFileState = MutableLiveData<ProcessState>()

    fun getFileUri() = fileUri!!


    fun getSearchMenuProcess():LiveData<ProcessState> = searchMenuProcess
    fun getDeleteMenuProcess():LiveData<ProcessState> = deleteMenuProcess

    init {
        menus.postValue(emptyList())
    }

    fun getMenus(user:String, companyId:String){
        viewModelScope.launch {
            searchMenuProcess.postValue(ProcessState(State.LOADING))
            val menuList = mutableListOf<MenuReference?>()
            val onlineMenus = menuService.getMenus(user,companyId).map {  MenuReference(menuId = it!!.menuId, firebaseRef = it.fireBaseRef!!, name = it.name, fileUri = it.fileUrl, online = true)}
            val localMenus = menuDS.getMenusByCompanyId(companyId).map { MenuReference(menuId = it.menuId, firebaseRef = null, name = it.name,it.fileUri!!, online = false) }

            menuList.addAll(onlineMenus)
            menuList.addAll(localMenus)
            menus.postValue(menuList)

            searchMenuProcess.postValue(ProcessState(State.SUCCESS))
        }
    }

    fun deleteMenu(user:String,companyId:String,menuReference: MenuReference){
        if(menuReference.online)
            deleteMenuFirebase(user = user,companyId=companyId, menuReference = menuReference)
        else
            deleteMenuLocal(user = user,companyId=companyId, menuReference = menuReference)
    }

    private fun deleteMenuFirebase(user:String,companyId:String,menuReference: MenuReference){
       deleteMenuProcess.postValue(ProcessState(State.LOADING))
       viewModelScope.launch(Dispatchers.Default){
           try{
               if(!NetworkUtils.isConnectedToInternet()){
                   throw TimeoutException()
               }

               val menuFirebase = menuService.getMenu(user = user, companyId = companyId, firebaseRef = menuReference.firebaseRef!!)

               menuStorage.removeAllMenuFiles(user = user, menuId = menuReference.menuId)
               menuService.deleteMenu(user = user,companyId = companyId,m=menuFirebase!!)
               deleteMenuProcess.postValue(ProcessState(State.SUCCESS))
           }catch (e:TimeoutException){
               deleteMenuProcess.postValue(ProcessState(State.NETWORK_ERROR))
           }
           catch (e:Exception){
               deleteMenuProcess.postValue(ProcessState(State.GENERAL_ERROR))
           }

       }
    }


    private fun deleteMenuLocal(user:String,companyId:String,menuReference: MenuReference){
        deleteMenuProcess.postValue(ProcessState(State.LOADING))
        viewModelScope.launch(Dispatchers.Default){
            try{
                val menu = menuDS.getMenuById(menuId = menuReference.menuId)
                val menuItems = menuDS.getMenuItemsByMenuId(menuId = menuReference.menuId)

                //Eliminar archivo
                if(menu.fileUri != null)
                    FileUtils.deleteFile(Uri.parse(menu.fileUri))

                //Eliminar imagenes
                menuItems.filter { !it.imageUri.isNullOrBlank() }.forEach{
                    FileUtils.deleteFile(Uri.parse(it.imageUri))
                }
                menuDS.deleteMenuItems(menuItems = menuItems)
                menuDS.deleteMenu(menu = menu)

                deleteMenuProcess.postValue(ProcessState(State.SUCCESS))
            }
            catch (e:Exception){
                deleteMenuProcess.postValue(ProcessState(State.GENERAL_ERROR))
            }

        }


    }

    fun searchPreviewUri(user:String,
                         companyId: String,
                         downloadDirectory:File,
                         menuReference: MenuReference,
                         ){
        viewModelScope.launch(Dispatchers.IO) {
            previewFileState.postValue(ProcessState(State.LOADING))
            try{
                fileUri = getFilePath(
                    user =  user,
                    companyId =  companyId,
                    downloadDirectory =  downloadDirectory,
                    menuReference =  menuReference)

                previewFileState.postValue(ProcessState(State.SUCCESS))
            }catch (e:TimeoutException){
                previewFileState.postValue(ProcessState(State.NETWORK_ERROR))
            }catch (e:Exception){
                previewFileState.postValue(ProcessState(State.GENERAL_ERROR))
            }

        }

    }

    fun searchShareUri(user:String,
                         companyId: String,
                         downloadDirectory:File,
                         menuReference: MenuReference,
    ){
        viewModelScope.launch(Dispatchers.IO) {

            previewFileState.postValue(ProcessState(State.LOADING))

            try{
                fileUri = getFilePath(
                    user =  user,
                    companyId =  companyId,
                    downloadDirectory =  downloadDirectory,
                    menuReference =  menuReference)

                shareFileState.postValue(ProcessState(State.SUCCESS))
            }catch (e:TimeoutException){
                shareFileState.postValue(ProcessState(State.NETWORK_ERROR))
            }catch (e:Exception){
                shareFileState.postValue(ProcessState(State.GENERAL_ERROR))
            }

        }

    }

    private suspend fun getFilePath(user:String,
                                     companyId: String,
                                     downloadDirectory:File,
                                     menuReference: MenuReference):Uri{
        val fileUri:String
        if(menuReference.online){
            if(!NetworkUtils.isConnectedToInternet()){
                throw TimeoutException()
            }
            val menu = menuService.getMenu(user = user, companyId = companyId, firebaseRef = menuReference.firebaseRef!!)
            fileUri = menu!!.fileUrl
            return downloadFile(downloadDirectory = downloadDirectory, fileUrl = fileUri)
        }else{
            val menu = menuDS.getMenuById(menuId = menuReference.menuId)
            return Uri.parse(menu.fileUri!!)
        }
    }

    private suspend fun downloadFile(downloadDirectory:File, fileUrl:String):Uri{
        val destination = File(downloadDirectory, "doc.pdf")
        menuStorage.downloadFileStorageReferenceFromUrl(url = fileUrl, destination = destination)
        return destination.toUri()
    }

   class MenuListViewModelFactory @Inject constructor(
       private val menuService: Provider<MenuService>,
       private val menuStorage: Provider<MenuStorage>,
       private val menuDS: Provider<MenuDS>
   ):ViewModelProvider.Factory{
       override fun <T : ViewModel> create(modelClass: Class<T>): T {
           return MenuListViewModel(
               menuService = menuService.get(),
               menuStorage = menuStorage.get(),
               menuDS = menuDS.get()) as T
       }
   }

}