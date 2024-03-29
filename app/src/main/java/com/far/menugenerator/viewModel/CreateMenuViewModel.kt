package com.far.menugenerator.viewModel

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.annotation.IdRes
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.R
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.common.utils.StringUtils
import com.far.menugenerator.model.Category
import com.far.menugenerator.model.CreateMenuState
import com.far.menugenerator.model.ItemPreviewPosition
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.model.LogoShape
import com.far.menugenerator.model.MenuSettings
import com.far.menugenerator.model.MenuStyle
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.common.MenuReference
import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.ItemFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.database.room.model.Menu
import com.far.menugenerator.model.database.room.model.MenuItems
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
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


class CreateMenuViewModel @Inject constructor(
    private val menuStorage: MenuStorage,
    private val menuService:MenuService,
    private val companyService:CompanyService,
    private val menuTempDS:MenuTempDS,
    private val menuDS:MenuDS
):ViewModel() {

    companion object{
        const val noCategoryId = "-1"
        const val noCategoryDescription="NO_CATEGORY"
    }

    private lateinit var _userId:String
    private var _companyReference:String?=null
    private var _menuReferenceId:String?=null
    private var _menuReferenceFirebaseRef:String?=null
    private var _isMenuOnline:Boolean?=null

    val companyReference get() = _companyReference
    val menuReferenceId get() = _menuReferenceId
    val menuReferenceFirebaseRef get() = _menuReferenceFirebaseRef
    val isMenuOnline get() = _isMenuOnline

    private var _menuId:String?=null
    val menuId get() = _menuId

    private var company:CompanyFirebase?=null

    private val _state = MutableLiveData<CreateMenuState>()
    private val _stateProcessMenu = MutableLiveData<ProcessState>()
    private val _menuSettings= MutableLiveData<MenuSettings>()
    private val _categories = MutableLiveData<MutableList<Category>>()//SOLO CATEGORIAS
    private val _items = MutableLiveData<MutableList<MenuItemsTemp>>()//SOLO PRODUCTOS
    private val _itemsPreview = MutableLiveData<MutableList<MenuItemsTemp>>()
    private val _editItem = MutableLiveData<MenuItemsTemp?>()

    private var _editMenu:MenuTemp? = null
    private var _editMenuItems:List<MenuItemsTemp>?=null
    private var _editItemsOriginalImages:List<Pair<String,String>> = listOf()

    val state:LiveData<CreateMenuState> get() = _state
    val stateProcessMenu:LiveData<ProcessState> get() = _stateProcessMenu
    val categories:LiveData<MutableList<Category>> get() = _categories
    val items:LiveData<MutableList<MenuItemsTemp>> get() = _items
    val itemsPreview:LiveData<MutableList<MenuItemsTemp>> get() = _itemsPreview
    val editItem:LiveData<MenuItemsTemp?> get() = _editItem

    fun getCompany() = company
    fun getMenuSettings():LiveData<MenuSettings> = _menuSettings


    fun initialize(userId:String,companyRef:String, menuReferenceId:String?, isOnlineMenu:Boolean?, menuReferenceFirebaseRef:String?){
        _userId = userId
        _companyReference = companyRef
        _menuReferenceId = menuReferenceId
        _isMenuOnline = isOnlineMenu
        _menuReferenceFirebaseRef = menuReferenceFirebaseRef
    }


    val currentItemImage = MutableLiveData<Uri?>()
        init {
            _menuSettings.value = MenuSettings(
                logoShape = LogoShape.NONE,
                showLogo = true,
                showBusinessName = true,
                showAddress1 = true,
                showAddress2 = true,
                showAddress3 = true,
                showPhone1 = true,
                showPhone2 = true,
                showPhone3 = true,
                showFacebook = true,
                showInstagram = true,
                showWhatsapp = true,
                menuStyle = MenuStyle.BASIC
            )
            _state.value = CreateMenuState(currentScreen = R.id.categoriesScreen)
            _itemsPreview.value = mutableListOf()
            _items.value = mutableListOf()
            _editItem.value = null

    }


    fun getCurrentMenuName():String?{
        return _editMenu?.name
    }



    fun prepareMenu(){
        _menuId = _menuReferenceId?:UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            if(company == null){
                company = companyService.getCompany(user = _userId, companyRef = _companyReference!!)
            }
            //if(_editMenu == null)//si no se esta editando actualmente ningun
            //    menuTempDS.clearAll()//LIMPIAR LAS TABLAS

            if(_editMenu == null && _menuReferenceId == null){//Nuevo menu desde 0
                saveCategory(_menuId!!, noCategoryId, noCategoryDescription, 0)
            }

            initMenuData()

            refreshCategories()
            refreshItems()
            refreshMenuPreview()
        }

    }


    private suspend fun initMenuData(){
        if (_editMenu != null || _menuReferenceId == null) return

        val menuTemp:MenuTemp
        val menuItemsTemp:List<MenuItemsTemp>
        if(_isMenuOnline!!){
            val firebaseMenu = menuService.getMenu(user = _userId, companyId = company!!.companyId, _menuReferenceFirebaseRef!!)
            menuTemp = MenuTemp(
                fireBaseRef = firebaseMenu!!.fireBaseRef,
                menuId = firebaseMenu.menuId,
                name = firebaseMenu.name,
                fileUrl = firebaseMenu.fileUrl,
                menuSettings = StringUtils.objectToJson(firebaseMenu.menuSettings))

            menuItemsTemp = firebaseMenu.items.map {
                MenuItemsTemp(
                    id = it.id,
                    menuId = _menuReferenceId!!,
                    type = it.type,
                    enabled = it.enabled,
                    categoryId = it.categoryId,
                    categoryName = it.categoryName,
                    name = it.name,
                    description = it.description?:"",
                    price = (it.price?:"0.0").toDouble(),
                    imageUri = it.imageUrl,
                    position = it.position)
            }
        }else{
            val menuLocal = menuDS.getMenuById(menuId = _menuReferenceId!!)
            val menuItems = menuDS.getMenuItemsByMenuId(menuId = _menuReferenceId!!)
            menuTemp = MenuTemp(
                fireBaseRef = null,
                menuId = menuLocal.menuId,
                name = menuLocal.name,
                fileUrl = menuLocal.fileUri!!,
                menuSettings = menuLocal.menuSettings
            )
            menuItemsTemp = menuItems.map {
                MenuItemsTemp(
                    id = it.id,
                    menuId = _menuId!!,
                    type = it.type,
                    enabled = it.enabled,
                    name = it.name,
                    categoryId = it.categoryId,
                    categoryName = it.categoryName,
                    description = it.description?:"",
                    price = it.price?.toDouble()?:0.0,
                    imageUri = it.imageUri,
                    position = it.position)
            }
        }

        _editMenu = menuTemp
        _editMenuItems = menuItemsTemp
        _editItemsOriginalImages = menuItemsTemp.filter { !it.imageUri.isNullOrBlank() }.map { it.id to it.imageUri!! }

        menuTempDS.addMenu(menuTemp)
        menuTempDS.addMenuItems(menuItemsTemp)
    }


    fun getCategoriesByName(name:String):List<Category>{
      return  categories.value?.filter{it.name.lowercase() == name.lowercase()}?: listOf()
    }
    fun addCategory(name:String){
        viewModelScope.launch {
            val foundCategory = menuTempDS.findMenuItemsByName(type = ItemStyle.MENU_CATEGORY_HEADER.name, name = name)
            if(foundCategory.isNotEmpty())
                return@launch


            val id =  UUID.randomUUID().toString()
            val category = MenuItemsTemp(
                id = id,
                menuId = _menuId!!,
                type = ItemStyle.MENU_CATEGORY_HEADER.name,
                enabled = true,
                categoryId = id,
                categoryName = name,
                name = name,
                description = name,
                position = (categories.value?.size?:0) + 1
            )
            menuTempDS.addMenuItem(category)
            refreshCategories()

        }

    }
    fun saveEditCategory(editCategory:Category){
        viewModelScope.launch(Dispatchers.IO) {
            val categoryItems = getMenuItems().filter { it.categoryId == editCategory.id }

            categoryItems.forEach {
                it.categoryId = editCategory.id
                it.categoryName = editCategory.name
            }
            menuTempDS.updateMenuItems(categoryItems)

            val category = menuTempDS.getMenuItemById(editCategory.id)
            category.name = editCategory.name
            menuTempDS.updateMenuItem(category)

            refreshCategories()
            refreshMenuPreview()

        }


    }

    /**
     * Siempre mover el NO_CATEGORY de primero
     */

    fun removeCategory(category: Category){
        viewModelScope.launch(Dispatchers.IO) {
            menuTempDS.deleteMenuItemById(category.id)
            val categoryItems = getMenuItems().filter { it.categoryId == category.id }
            categoryItems.forEach{
                it.categoryId = noCategoryId
                it.categoryName = noCategoryDescription
            }
            menuTempDS.updateMenuItems(categoryItems)

            refreshCategories()
            refreshMenuPreview()

        }

    }

    fun addProduct(enabled:Boolean,category: Category,name:String,description:String, amount:Double){

        viewModelScope.launch(Dispatchers.IO) {
            val itemStyle: ItemStyle = getItemStyle(currentItemImage.value != null,description.isNotBlank())

            val menuItem = MenuItemsTemp(
                id = UUID.randomUUID().toString(),
                menuId = _menuId!!,
                type = itemStyle.name,
                enabled = enabled,
                categoryId = category.id,
                categoryName = category.name,
                name=name,
                description=description,
                price = amount,
                imageUri = currentItemImage.value?.toString(),
                position = (_items.value?.size?:0)+1,
            )
            menuTempDS.addMenuItem(menuItem)
            refreshItems()
            refreshMenuPreview()
        }
    }

    fun updateCurrentItemImage(imageUri:Uri?){
        currentItemImage.postValue(imageUri)
    }

    fun editItem(menuItemsTemp: MenuItemsTemp?){
        viewModelScope.launch(Dispatchers.IO) {
            if(menuItemsTemp != null) {
                _editItem.postValue(menuItemsTemp)
            }else{
                _editItem.postValue(null)
            }
        }
    }
    fun saveEditItemChanges(enabled: Boolean,category:Category,name:String, description:String, price:Double){
        viewModelScope.launch(Dispatchers.IO) {
            val previousImage = editItem.value?.imageUri
            val image:String?
            if(currentItemImage.value == null){//NO IMAGE
                image = null
            }else if(currentItemImage.value.toString() == previousImage){//SAME IMAGE
                image = previousImage
            }else{
                image = currentItemImage.value.toString() //IMAGE CHANGE
            }

            val itemStyle: ItemStyle = getItemStyle(
                hasImage = image != null,
                hasDescription =  description.isNotBlank())

            val menuItem = _editItem.value?.copy(
                type = itemStyle.name,
                enabled = enabled,
                categoryId = category.id,
                categoryName = category.name,
                name = name,
                description = description,
                price = price,
                imageUri = image)

            menuTempDS.updateMenuItem(menuItem!!)
            _editItem.postValue(null)
            refreshItems()
            refreshMenuPreview()
        }

    }

    fun deleteItem(menuItemsTemp: MenuItemsTemp){
        viewModelScope.launch {
            menuTempDS.deleteMenuItemById(menuItemsTemp)
            _editItem.postValue(null)
            refreshItems()
            refreshMenuPreview()
        }

    }

    fun updateMenuSettings(ms: MenuSettings){
        _menuSettings.value = ms
    }

    private suspend fun refreshMenuPreview(){
        var previewList =  getItemPreviews().toMutableList()
        _itemsPreview.postValue(previewList)
    }

    fun nextScreen(){
        val nextScreen = when(_state.value?.currentScreen){
            R.id.categoriesScreen -> R.id.addMenuItemScreen
            R.id.addMenuItemScreen -> R.id.menuPreviewScreen
            else -> R.id.menuPreviewFinalScreen
        }
        _state.value = _state.value?.copy(currentScreen = nextScreen)
    }
    fun previousScreen(){
        val previousScreen = when(_state.value?.currentScreen){
            R.id.menuPreviewFinalScreen -> R.id.menuPreviewScreen
            R.id.menuPreviewScreen -> R.id.addMenuItemScreen
            else -> R.id.categoriesScreen
        }
        _state.value = _state.value?.copy(currentScreen = previousScreen)
    }

    fun setScreen(@IdRes screen:Int){
        _state.value = _state.value?.copy(currentScreen = screen)
    }


    fun generateMenu(context:Context,referenceName:String,view: View,fileHeight:Int, pdfFile:File,){
        val pdfPath = pdfFile.path
        viewModelScope.launch(Dispatchers.IO) {
            _stateProcessMenu.postValue(ProcessState(State.LOADING))

            try{
                val menuItems = menuTempDS.getMenuItemsByMenuId(menuId = _menuId!!)
                menuItems.filter { item-> item.imageUri != null }.forEach{ item->
                    //Validar si es una edicion de item y el item no sufrio modificaciones en su imagen
                    if(_editItemsOriginalImages.any{it.first == item.id && it.second == item.imageUri})
                        return@forEach

                    val imageUri = Uri.parse(item.imageUri)
                    val bitmap  = FileUtils.getBitmapFromUri(context=context.applicationContext, imageUri = imageUri)
                    val imageName = "${UUID.randomUUID()}.jpg"//usamos rowguid y no el nombre el file original para evitar problemas si 2 items tienen la misma imagen.
                    ///Tamposo usamos la extension con el uri.ToFile() porque el cropper me da una uri de Content y el metodo toFile() no es aplicable
                    val imageFile = File(context.applicationContext.filesDir, imageName)
                    FileUtils.resizeAndSaveBitmap(context.applicationContext,bitmap,512f,imageFile)
                    item.imageUri  = imageFile.toUri().toString()
                    menuTempDS.updateMenuItem(item)

                }

                FileUtils.layoutToPdf(layout =  view, pdfPath = pdfPath, height = fileHeight)

                if(_menuReferenceId != null && isMenuOnline!!){//(Solo se crean Locales, luego de comprados se mandan a firebase) Si es online Hacer por firebase
                    processMenu(
                        user = LoginActivity.userFirebase?.internalId!!,
                        companyId = company!!.companyId,
                        fileName = referenceName,
                        itemPreviews = mutableListOf(),
                        pdfPath = pdfPath
                    )

                }else {
                    processMenuLocal(
                        user = LoginActivity.userFirebase?.internalId!!,
                        companyId = company!!.companyId,
                        fileName = referenceName,
                        pdfFile = pdfFile.toUri(),
                        baseDirectory = context.applicationContext.filesDir
                    )
                }

                _stateProcessMenu.postValue(ProcessState(State.SUCCESS))

            }catch (e:TimeoutException){
                _stateProcessMenu.postValue(ProcessState(State.NETWORK_ERROR))
            }
            catch (e:Exception){
                _stateProcessMenu.postValue(ProcessState(State.GENERAL_ERROR))
            }
        }
    }


    private suspend fun processMenu(user:String,companyId: String,fileName:String,itemPreviews:List<MenuItemsTemp>,pdfPath:String){
        val menuSettings = getMenuSettings().value!!
        if(_editMenu == null)
            saveMenu(user= user, companyId= companyId, menuSettings = menuSettings,fileName= fileName, itemPreviews= itemPreviews, pdfPath =  pdfPath)
        else
            saveEditMenu(user = user, companyId = companyId, menuTemp = _editMenu!!, menuSettings = menuSettings,fileName = fileName, pdfPath = pdfPath)
    }

    private suspend fun processMenuLocal(user:String,companyId: String,fileName:String,pdfFile:Uri, baseDirectory: File){
        val menuSettings = getMenuSettings().value!!
        if(_editMenu == null)
            saveMenuLocal(user= user, companyId= companyId, menuSettings = menuSettings,fileName= fileName, pdfFile =  pdfFile, baseDirectory = baseDirectory)
        else
            saveEditMenuLocal(user = user, companyId = companyId, menuTemp = _editMenu!!, menuSettings = menuSettings,fileName = fileName, pdfFile = pdfFile, baseDirectory = baseDirectory)
    }


     fun updatePositions(itemPreviewPositions:List<ItemPreviewPosition>){
         viewModelScope.launch(Dispatchers.IO) {
             itemPreviewPositions.forEach{ itemP->
                 val menuItem = menuTempDS.getMenuItemById(id = itemP.id)
                 menuItem.position = itemP.position
                 menuTempDS.updateMenuItem(menuItem)
             }
             refreshItems()
         }
    }


    private suspend fun saveMenu(user:String,companyId: String,menuSettings: MenuSettings,fileName:String,itemPreviews:List<MenuItemsTemp>,pdfPath:String){
                if(!NetworkUtils.isConnectedToInternet()){
                    throw TimeoutException()
                }

                val tempMenuId:String = UUID.randomUUID().toString()
                val menuStorageUrl = uploadMenuFile(user = user, menuId = tempMenuId, pdfPath = pdfPath).toString()
                val items = prepareItemsFirebase(user = user, menuId = tempMenuId, items = itemPreviews)
                val savedMenu = saveMenuFireBase(user =  user, companyId = companyId, menuId = tempMenuId,fileName = fileName, fileUrl =  menuStorageUrl, items =  items, menuSettings = menuSettings)
    }

    private suspend fun saveEditMenu(user:String,companyId: String,menuTemp: MenuTemp,menuSettings: MenuSettings,fileName:String,pdfPath:String){
                if(!NetworkUtils.isConnectedToInternet()){
                    throw TimeoutException()
                }
                val menuItemsTemp = menuTempDS.getMenuItemsByMenuId(_menuId!!)

                val menuStorageUrl = uploadMenuFile(user = user,menuId=menuTemp.menuId, pdfPath = pdfPath).toString()
                deleteUnusedImages(userId = user, items = menuItemsTemp, online = true)
                val items = prepareItemsFirebase(user = user, menuId=menuTemp.menuId, items = menuItemsTemp)

                val savedMenu = editMenuFireBase(user =  user, companyId = companyId, menuTemp = menuTemp,fileName = fileName, fileUrl =  menuStorageUrl, items =  items, menuSettings = menuSettings)
    }

    private suspend fun saveMenuLocal(user:String,companyId: String,menuSettings: MenuSettings,fileName:String,pdfFile:Uri, baseDirectory: File){
                val tempMenuId:String = _menuId!!
                val menuItemsTemp = menuTempDS.getMenuItemsByMenuId(_menuId!!)

                //VERIFY DIRECTORY CREATION
                val menuDirectory = FileUtils.createDirectory(baseDirectory = baseDirectory, directoryName = _menuId!!)
                //MOVE PDF
                val menuFileUri = saveMenuFile(user = user, menuId = tempMenuId, pdfFile = pdfFile, menuDirectory = menuDirectory).toString()
                //MOVE IMAGES AND SET  URI to MenuItems
                val items = prepareItemsLocal(user = user, menuId = tempMenuId, items = menuItemsTemp, menuDirectory = menuDirectory)
                //SAVE IN DB
                val savedMenu = saveMenuDB(
                    user =  user,
                    companyId = companyId,
                    menuId = tempMenuId,
                    fileName = fileName,
                    fileUri =  menuFileUri,
                    items =  items,
                    menuSettings = menuSettings)

    }


    private suspend fun saveEditMenuLocal(user:String,companyId: String,menuTemp: MenuTemp,menuSettings: MenuSettings,fileName:String,pdfFile:Uri,baseDirectory:File){
        val tempMenuId:String = _menuId!!
        val menuItemsTemp = menuTempDS.getMenuItemsByMenuId(_menuId!!)

        //VERIFY DIRECTORY CREATION
        val menuDirectory = FileUtils.createDirectory(baseDirectory = baseDirectory, directoryName = _menuId!!)

        //MOVE PDF
        val menuFileUri = saveMenuFile(user = user, menuId = tempMenuId, pdfFile = pdfFile, menuDirectory = menuDirectory).toString()

        //REMOVE OLD IMAGES
        deleteUnusedImages(userId= user, items =  menuItemsTemp, online = false)

        val items = prepareItemsLocal(
            user = user,
            menuId=menuTemp.menuId,
            items = menuItemsTemp,
            menuDirectory = menuDirectory)
        val savedMenu = updateMenuDB(
            user =  user,
            companyId = companyId,
            menuId = tempMenuId,
            fileName = fileName,
            fileUri = menuFileUri,
            items = items,
            menuSettings = menuSettings)
    }


    private suspend fun uploadMenuFile(user:String,menuId:String, pdfPath:String):Uri {
      return menuStorage.uploadFile(user,menuId,pdfPath)
    }
    private fun saveMenuFile(user:String,menuId:String, pdfFile:Uri,menuDirectory: File):Uri? {
        return FileUtils.moveFile(fileUri = pdfFile, directory =  menuDirectory, fileName = "document.pdf")
    }
    private suspend fun prepareItemsFirebase(user:String,menuId: String, items:List<MenuItemsTemp>):List<ItemFirebase>{

        //UPLOAD LOCAL IMAGES ONLY
        val firebaseItems = items.mapIndexed { _,menuItem->
                val image =  if(menuItem.imageUri !=null
                    && (_editItemsOriginalImages.isEmpty() || _editItemsOriginalImages.any { it.first == menuItem.id && it.second != menuItem.imageUri }))
                    "*"
                else menuItem.imageUri

            ItemFirebase(
                id = menuItem.id,
                type = menuItem.type,
                categoryId = menuItem.categoryId,
                categoryName = menuItem.categoryName,
                name = menuItem.name,
                description = menuItem.description,
                price = menuItem.price.toString(),
                position = menuItem.position,
                imageUrl = image)
        }

        firebaseItems.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.imageUrl.equals("*") }
            .forEach{
                val menuItem = items.first{ i-> i.id == it.id}
                val url = menuStorage.uploadMenuItemsImages(user,menuId,Uri.parse(menuItem.imageUri))
                it.imageUrl = url.toString()
            }
        return firebaseItems
    }


    private fun prepareItemsLocal(user:String,menuId: String, items:List<MenuItemsTemp>,menuDirectory: File):List<MenuItems>{

        //UPLOAD LOCAL IMAGES ONLY
        val menuItems = items.mapIndexed { _,menuItem->
            val image =  if(menuItem.imageUri !=null
                && (_editItemsOriginalImages.isEmpty() || _editItemsOriginalImages.any { it.first == menuItem.id && it.second != menuItem.imageUri }))
                "*"
            else menuItem.imageUri

            MenuItems(
                id = menuItem.id,
                menuId = menuId,
                type = menuItem.type,
                enabled = menuItem.enabled,
                categoryId = menuItem.categoryId,
                categoryName = menuItem.categoryName,
                name = menuItem.name,
                description = menuItem.description,
                price = menuItem.price.toString(),
                imageUri = image,
                position = menuItem.position)
        }

        menuItems.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.imageUri.equals("*") }
            .forEach{
                val menuItemTemp = items.first{ i-> i.id == it.id}
                val uri = FileUtils.moveFile(
                    fileUri =  Uri.parse(menuItemTemp.imageUri),
                    directory = menuDirectory)
                it.imageUri = uri.toString()
            }
        return menuItems
    }

    private suspend fun deleteUnusedImages(userId:String,items:List<MenuItemsTemp>, online:Boolean){
        if(_editMenu != null){
            //removed items (items que estaban en la DB de firebase y en despues de la edicion ya no)
            val deletedItems = _editItemsOriginalImages.filter {oi->
                items.none { it.id == oi.first }
            }
            //existing items with removed images (Items guardados en firebase que TENIAN IMAGEN y ahora NO TIENEN)
            val removedImages = _editItemsOriginalImages.filter { oi->
                items.any { it.id == oi.first && it.imageUri == null  }
            }

            //images changed (items que tenian imagen remota y se la actualizaron)
            val changedImages = _editItemsOriginalImages.filter { oi->
                items.any { it.id == oi.first && it.imageUri != null && it.imageUri != oi.second  }
            }

            val imagesToRemove = mutableListOf<String?>()
            imagesToRemove.addAll(deletedItems.map { it.second })
            imagesToRemove.addAll(removedImages.map { it.second })
            imagesToRemove.addAll(changedImages.map { it.second })
            //DELETE ALL UNUSED IMAGES
            imagesToRemove.forEach{
                val uri = Uri.parse(it)
                if(online)
                    menuStorage.removeMenuItemsImages(userId,_editMenu!!.menuId,uri)
                else
                    FileUtils.deleteFile(uri)
                }
        }
    }

    private fun deleteUnusedImagesLocal(menuItemsTemp:List<MenuItemsTemp>){
        /*
        if(_editMenu != null){
            //items  con imagen
            val localItems = _editMenuItems!!.filter { it.remoteImageUri != null }

            //removed items (items que estaban en la DB y en despues de la edicion ya no)
            val deletedItems = localItems.filter {fbi->
                fbi.id !in menuItemsTemp.map { it.id }
            }
            //existing items with removed images (Items guardados que TENIAN IMAGEN y ahora NO TIENEN)
            val removedImages = localItems.filter { fbi->
                fbi.id in menuItemsTemp.filter { it.localImageUri == null && it.remoteImageUri == null }.map { it.id }
            }
            //images changed (items que tenian imagen y se la actualizaron)
            val changedImages = localItems.filter {fbi->
                fbi.id in menuItemsTemp.filter { it.localImageUri != null && it.remoteImageUri == null }.map { it.id }
            }

            val imagesToRemove = mutableListOf<String?>()
            imagesToRemove.addAll(deletedItems.map { it.remoteImageUri })
            imagesToRemove.addAll(removedImages.map { it.remoteImageUri })
            imagesToRemove.addAll(changedImages.map { it.remoteImageUri })
            //DELETE ALL UNUSED IMAGES
            imagesToRemove.forEach{
                FileUtils.deleteFile(Uri.parse(it))
            }
        }*/
    }


     private fun saveMenuFireBase(user:String,companyId:String,menuId:String, fileName:String, fileUrl:String, items:List<ItemFirebase>,menuSettings: MenuSettings):MenuFirebase{
         val menu = MenuFirebase(menuId = menuId,name=fileName,fileUrl= fileUrl, items = items, menuSettings = menuSettings)
         menuService.saveMenu(user, companyId = companyId,menu)
         return menu
    }

    private fun editMenuFireBase(user:String,companyId:String,menuTemp: MenuTemp, fileName:String, fileUrl:String, items:List<ItemFirebase>, menuSettings: MenuSettings):MenuFirebase{
        val menu = MenuFirebase(fireBaseRef = menuTemp.fireBaseRef,menuId = menuTemp.menuId,name=fileName,fileUrl= fileUrl, items = items, menuSettings = menuSettings)
        menuService.updateMenu(user, companyId = companyId,menu)
        return menu
    }


    private suspend fun saveMenuDB(user:String,companyId:String,menuId:String, fileName:String, fileUri:String, items:List<MenuItems>,menuSettings: MenuSettings):Menu{
        val menu = Menu(
            menuId = menuId,
            companyId = companyId,
            name=fileName,
            fileUri= fileUri,
            menuSettings = StringUtils.objectToJson(menuSettings))
        menuDS.addMenu(menu=menu)
        menuDS.addOrUpdateMenuItems(menuItems = items)
        return menu
    }

    private suspend fun updateMenuDB(user:String,companyId:String,menuId:String, fileName:String, fileUri:String, items:List<MenuItems>,menuSettings: MenuSettings):Menu{
        val menu = Menu(
            menuId = menuId,
            companyId = companyId,
            name=fileName,
            fileUri= fileUri,
            menuSettings = StringUtils.objectToJson(menuSettings))
        menuDS.updateMenu(menu=menu)
        menuDS.deleteMenuItemsByMenuId(menuId = menuId)
        menuDS.addOrUpdateMenuItems(menuItems = items)
        return menu
    }


    private suspend fun refreshCategories(){
        val categories = getCategories()
        orderCategoriesByName(categories)
        _categories.postValue(categories)
    }

    private suspend fun refreshItems(){
        val items = getMenuItems()
        _items.postValue(items)
    }


    private suspend fun getCategories():MutableList<Category>{
        return menuTempDS.getMenuItemsByType(ItemStyle.MENU_CATEGORY_HEADER.name).map { Category(id = it.id, name = it.name, position = it.position) }.toMutableList()
    }

    private suspend fun getMenuItems():MutableList<MenuItemsTemp>{
        return menuTempDS.getMenuItemsByType(
            ItemStyle.MENU_TITLE_PRICE.name,
            ItemStyle.MENU_TITLE_DESCRIPTION_PRICE.name,
            ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE.name).toMutableList()
    }

    private suspend fun getItemPreviews():List<MenuItemsTemp>{
        return menuTempDS.getMenuItemsByType(
            ItemStyle.MENU_CATEGORY_HEADER.name,
            ItemStyle.MENU_TITLE_PRICE.name,
            ItemStyle.MENU_TITLE_DESCRIPTION_PRICE.name,
            ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE.name)
            .sortedBy { it.position }
    }

    private fun orderCategoriesByName(categories:MutableList<Category>) {
        val noCategory = categories.firstOrNull{it.id == noCategoryId} ?: return
        categories.remove(noCategory)
        categories.sortBy { it.name }
        categories.add(0, noCategory)
    }

    private suspend fun saveCategory(menuId:String, id:String, name:String, position:Int) {
        val category = MenuItemsTemp(
            id = id,
            menuId = menuId,
            type = ItemStyle.MENU_CATEGORY_HEADER.name,
            enabled = true,
            categoryId = id,
            categoryName = name,
            name = name,
            description = name,
            position = position
        )
        menuTempDS.addMenuItem(category)
    }

    private fun getItemStyle(hasImage: Boolean, hasDescription: Boolean) =  if(hasImage){
            ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE
        }else if(hasDescription){
            ItemStyle.MENU_TITLE_DESCRIPTION_PRICE
        }else{
            ItemStyle.MENU_TITLE_PRICE
        }

    class CreateMenuViewModelFactory @Inject constructor (
        private val menuStorageProvider: Provider<MenuStorage>, //usamos provider para eviar bugs (ver video)
        private val menuService:Provider<MenuService>,
        private val companyService: Provider<CompanyService>,
        private val menuTempDS:Provider<MenuTempDS>,
        private val menuDS:Provider<MenuDS>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateMenuViewModel(
                menuStorage = menuStorageProvider.get(),
                menuService = menuService.get(), menuTempDS = menuTempDS.get(),
                companyService = companyService.get(),
                menuDS = menuDS.get()) as T
        }

    }

}