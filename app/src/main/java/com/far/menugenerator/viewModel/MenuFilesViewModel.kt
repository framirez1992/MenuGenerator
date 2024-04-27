package com.far.menugenerator.viewModel

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.common.utils.StringUtils
import com.far.menugenerator.model.Enums
import com.far.menugenerator.model.MenuSettings
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.ItemFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.database.room.model.Menu
import com.far.menugenerator.model.database.room.model.MenuTemp
import com.far.menugenerator.model.database.room.services.MenuDS
import com.far.menugenerator.model.database.room.services.MenuTempDS
import com.far.menugenerator.model.storage.MenuStorage
import com.far.menugenerator.view.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Provider

class MenuFilesViewModel(
    private val menuService:MenuService,
    private val companyService:CompanyService,
    private val menuStorage:MenuStorage,
    private val menuDS:MenuDS,
    private val menuTempDS:MenuTempDS
):ViewModel() {

    private val _selectedFile = MutableLiveData<Uri>()
    val selectedFile:LiveData<Uri> = _selectedFile

    private val _stateProcessMenu = MutableLiveData<ProcessState>()
    val stateProcessMenu:LiveData<ProcessState> get() = _stateProcessMenu

    private lateinit var _userId:String
    private var _companyReference:String?=null
    private var _menuType: Enums.MenuType?=null
    private var _menuReferenceId:String?=null
    private var _menuReferenceFirebaseRef:String?=null
    private var _isMenuOnline:Boolean?=null

    private var company: CompanyFirebase?=null

    private var _editMenu:MenuTemp? = null

    val companyReference get() = _companyReference
    val menuReferenceId get() = _menuReferenceId
    val menuReferenceFirebaseRef get() = _menuReferenceFirebaseRef
    val isMenuOnline get() = _isMenuOnline
    val menuType get()= _menuType

    private var _menuId:String?=null
    val menuId get() = _menuId

    fun getCompany() = company


    fun initialize(userId:String,companyRef:String,menuType:String, menuReferenceId:String?, isOnlineMenu:Boolean?, menuReferenceFirebaseRef:String?){
        _userId = userId
        _companyReference = companyRef
        _menuType = Enums.MenuType.valueOf(menuType)
        _menuReferenceId = menuReferenceId
        _isMenuOnline = isOnlineMenu
        _menuReferenceFirebaseRef = menuReferenceFirebaseRef
    }

    fun prepareMenu(){
        _menuId = _menuReferenceId?:UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            if(company == null){
                company = companyService.getCompany(user = _userId, companyRef = _companyReference!!)
            }
            initMenuData()
        }

    }

    private suspend fun initMenuData(){
        if (_editMenu != null || _menuReferenceId == null) return

        val menuTemp: MenuTemp
        if(_isMenuOnline!!){
            val firebaseMenu = menuService.getMenu(user = _userId, companyId = company!!.companyId, _menuReferenceFirebaseRef!!)
            menuTemp = MenuTemp(
                fireBaseRef = firebaseMenu!!.fireBaseRef,
                menuId = firebaseMenu.menuId,
                menuType = firebaseMenu.menuType,
                name = firebaseMenu.name,
                fileUrl = firebaseMenu.fileUrl,
                menuSettings = StringUtils.objectToJson(firebaseMenu.menuSettings))
        }else{
            val menuLocal = menuDS.getMenuById(menuId = _menuReferenceId!!)
            menuTemp = MenuTemp(
                fireBaseRef = null,
                menuId = menuLocal.menuId,
                menuType = menuLocal.menuType,
                name = menuLocal.name,
                fileUrl = menuLocal.fileUri!!,
                menuSettings = menuLocal.menuSettings
            )
        }

        _editMenu = menuTemp
        menuTempDS.addMenu(menuTemp)
    }

    fun setSelectedFile(uri:Uri){
        _selectedFile.postValue(uri)
    }

    //PDF FILES
    fun generateMenu(context: Context, referenceName: String, menuFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _stateProcessMenu.postValue(ProcessState(State.LOADING))

            try{
                FileUtils.copyUriContentToFile(context=context, file = selectedFile.value!!,menuFile)

                generate(context = context, referenceName = referenceName, menuFile = menuFile.toUri())

                _stateProcessMenu.postValue(ProcessState(State.SUCCESS))

            }catch (e: TimeoutException){
                e.message
                _stateProcessMenu.postValue(ProcessState(State.NETWORK_ERROR))
            }
            catch (e:Exception){
                e.message
                _stateProcessMenu.postValue(ProcessState(State.GENERAL_ERROR))
            }
        }
    }

    //IMAGES in PDF
    fun generateMenu(context:Context, referenceName:String, view: View, fileHeight:Int, menuFile:File){

        viewModelScope.launch(Dispatchers.IO) {
            _stateProcessMenu.postValue(ProcessState(State.LOADING))

            try{
                FileUtils.layoutToPdf(layout =  view, pdfPath = menuFile.path, height = fileHeight)
                generate(context = context,referenceName=referenceName, menuFile = menuFile.toUri())
                _stateProcessMenu.postValue(ProcessState(State.SUCCESS))

            }catch (e:TimeoutException){
                _stateProcessMenu.postValue(ProcessState(State.NETWORK_ERROR))
            }
            catch (e:Exception){
                _stateProcessMenu.postValue(ProcessState(State.GENERAL_ERROR))
            }
        }
    }



    private suspend fun generate(context:Context,referenceName:String, menuFile:Uri,){
        if(_menuReferenceId != null && isMenuOnline!!){//(Solo se crean Locales, luego de comprados se mandan a firebase) Si es online Hacer por firebase
            processMenu(
                user = LoginActivity.userFirebase?.internalId!!,
                companyId = company!!.companyId,
                menuType = _menuType!!.name,
                fileName = referenceName,
                menuFile = menuFile
            )

        }else {
            processMenuLocal(
                user = LoginActivity.userFirebase?.internalId!!,
                companyId = company!!.companyId,
                menuType = _menuType!!.name,
                fileName = referenceName,
                menuSettings = MenuSettings(),
                pdfFile = menuFile,
                baseDirectory = context.applicationContext.filesDir
            )
        }
    }


    private suspend fun processMenu(user:String, companyId: String, menuType: String, fileName:String, menuFile:Uri){
        if(_editMenu == null)
            saveMenu(
                user = user, companyId = companyId, menuType = menuType, menuSettings = MenuSettings(),
                fileName = fileName, menuFile = menuFile)
        else
            saveEditMenu(
                user = user, companyId = companyId, menuTemp = _editMenu!!,
                fileName = fileName, menuFile = menuFile)
    }

    private suspend fun saveMenu(user:String,companyId: String,menuType: String,menuSettings: MenuSettings,fileName:String,menuFile:Uri){
        if(!NetworkUtils.isConnectedToInternet()){
            throw TimeoutException()
        }

        val tempMenuId:String = UUID.randomUUID().toString()
        val menuStorageUrl = uploadMenuFile(user = user, menuId = tempMenuId, menuFile = menuFile).toString()
        val savedMenu = saveMenuFireBase(user =  user, companyId = companyId, menuId = tempMenuId, menuType = menuType,fileName = fileName, fileUrl =  menuStorageUrl, items =  emptyList(), menuSettings = menuSettings)
    }

    private suspend fun saveEditMenu(user:String,companyId: String,menuTemp: MenuTemp,fileName:String,menuFile:Uri){
        if(!NetworkUtils.isConnectedToInternet()){
            throw TimeoutException()
        }
        val menuStorageUrl = uploadMenuFile(user = user,menuId=menuTemp.menuId, menuFile = menuFile).toString()
        val savedMenu = editMenuFireBase(user =  user, companyId = companyId, menuTemp = menuTemp,fileName = fileName, fileUrl =  menuStorageUrl, items =  emptyList(), menuSettings = MenuSettings())
    }

    private suspend fun uploadMenuFile(user:String, menuId:String, menuFile:Uri):Uri {
        return menuStorage.uploadFile(user,menuId,menuFile.path!!)
    }

    private fun saveMenuFireBase(user:String, companyId:String, menuId:String, menuType: String, fileName:String, fileUrl:String, items:List<ItemFirebase>, menuSettings: MenuSettings): MenuFirebase {
        val menu = MenuFirebase(menuId = menuId, menuType = menuType,name=fileName,fileUrl= fileUrl, items = items, menuSettings = menuSettings)
        menuService.saveMenu(user, companyId = companyId,menu)
        return menu
    }

    private fun editMenuFireBase(user:String, companyId:String, menuTemp: MenuTemp, fileName:String, fileUrl:String, items:List<ItemFirebase>, menuSettings: MenuSettings): MenuFirebase {
        val menu = MenuFirebase(fireBaseRef = menuTemp.fireBaseRef,menuId = menuTemp.menuId, menuType = menuTemp.menuType,name=fileName,fileUrl= fileUrl, items = items, menuSettings = menuSettings)
        menuService.updateMenu(user, companyId = companyId,menu)
        return menu
    }


    private suspend fun processMenuLocal(user:String,companyId: String,menuType: String,fileName:String,menuSettings: MenuSettings,pdfFile:Uri, baseDirectory: File){
        if(_editMenu == null)
            saveMenuLocal(user= user, companyId= companyId,menuType = menuType ,fileName= fileName,menuSettings=menuSettings, pdfFile =  pdfFile, baseDirectory = baseDirectory)
        else
            saveEditMenuLocal(user = user, companyId = companyId, menuTemp = _editMenu!!,fileName = fileName, pdfFile = pdfFile, baseDirectory = baseDirectory)
    }

    private suspend fun saveMenuLocal(user:String,companyId: String,menuType:String,fileName:String,menuSettings: MenuSettings,pdfFile:Uri, baseDirectory: File){
        val tempMenuId:String = _menuId!!

        //VERIFY DIRECTORY CREATION
        val menuDirectory = FileUtils.createDirectory(baseDirectory = baseDirectory, directoryName = _menuId!!)
        //MOVE PDF
        val menuFileUri = saveMenuFile(user = user, menuId = tempMenuId, menuFile = pdfFile, menuDirectory = menuDirectory).toString()
        //SAVE IN DB
        val savedMenu = saveMenuDB(
            user =  user,
            companyId = companyId,
            menuId = tempMenuId,
            menuType = menuType,
            fileName = fileName,
            menuSettings = menuSettings,
            fileUri =  menuFileUri)

    }
    private fun saveMenuFile(user:String, menuId:String, menuFile:Uri, menuDirectory: File):Uri? {
        val name = menuFile.toFile().name
        return FileUtils.moveFile(fileUri = menuFile, directory =  menuDirectory, fileName = name)
    }

    private suspend fun saveMenuDB(user:String, companyId:String, menuId:String, menuType:String, fileName:String,menuSettings: MenuSettings, fileUri:String): Menu {
        val menu = Menu(
            menuId = menuId,
            menuType = menuType,
            companyId = companyId,
            name=fileName,
            fileUri= fileUri,
            menuSettings = StringUtils.objectToJson(menuSettings))
        menuDS.addMenu(menu=menu)
        return menu
    }

    private suspend fun saveEditMenuLocal(user:String,companyId: String,menuTemp: MenuTemp,fileName:String,pdfFile:Uri,baseDirectory:File){
        val tempMenuId:String = _menuId!!
        //VERIFY DIRECTORY CREATION
        val menuDirectory = FileUtils.createDirectory(baseDirectory = baseDirectory, directoryName = _menuId!!)

        //MOVE PDF
        val menuFileUri = saveMenuFile(user = user, menuId = tempMenuId, menuFile = pdfFile, menuDirectory = menuDirectory).toString()

        val savedMenu = updateMenuDB(
            user =  user,
            companyId = companyId,
            menuId = tempMenuId,
            menuType = menuTemp.menuType,
            fileName = fileName,
            menuSettings = menuTemp.menuSettings,
            fileUri = menuFileUri)
    }
    private suspend fun updateMenuDB(user:String,companyId:String,menuId:String,menuType: String ,fileName:String,menuSettings: String?, fileUri:String):Menu{
        val menu = Menu(
            menuId = menuId,
            menuType = menuType,
            companyId = companyId,
            name=fileName,
            fileUri= fileUri,
            menuSettings = menuSettings)
        menuDS.updateMenu(menu=menu)
        return menu
    }

    fun getCurrentMenuName():String?{
        return _editMenu?.name
    }

    class MenuFilesViewModelFactory @Inject constructor(
        private val menuService: Provider<MenuService>,
        private val companyService:Provider<CompanyService>,
        private val menuStorage:Provider<MenuStorage>,
        private val menuDS:Provider<MenuDS>,
        private val menuTempDS:Provider<MenuTempDS>,

    ) :ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MenuFilesViewModel(
                menuService = menuService.get(),
                companyService = companyService.get(),
                menuStorage = menuStorage.get(),
                menuDS = menuDS.get(),
                menuTempDS = menuTempDS.get()) as T
        }
    }
}