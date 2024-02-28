package com.far.menugenerator.viewModel

import android.net.Uri
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.R
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.model.Category
import com.far.menugenerator.model.CreateMenuState
import com.far.menugenerator.model.Item
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.ItemPreviewPosition
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.model.LogoShape
import com.far.menugenerator.model.MenuSettings
import com.far.menugenerator.model.MenuStyle
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.ItemFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
import com.far.menugenerator.model.database.room.services.MenuTempDS
import com.far.menugenerator.model.storage.MenuStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Provider


class CreateMenuViewModel @Inject constructor(
    private val menuStorage: MenuStorage,
    private val menuService:MenuService,
    private val menuTempDS:MenuTempDS
):ViewModel() {

    companion object{
        const val noCategoryId = "-1"
        const val noCategoryDescription="NO_CATEGORY"
    }
    private lateinit var menuId:String
    private val _state = MutableLiveData<CreateMenuState>()
    private val _stateProcessMenu = MutableLiveData<ProcessState>()
    private val _menuSettings= MutableLiveData<MenuSettings>()
    private val _categories = MutableLiveData<MutableList<Category>>()
    private val _items = MutableLiveData<MutableList<MenuItemsTemp>>()
    private val _itemsPreview = MutableLiveData<MutableList<ItemPreview>>()
    private val _editItem = MutableLiveData<MenuItemsTemp?>()

    private var _editMenu:MenuFirebase? = null

    val state:LiveData<CreateMenuState> get() = _state
    val stateProcessMenu:LiveData<ProcessState> get() = _stateProcessMenu
    val categories:LiveData<MutableList<Category>> get() = _categories
    val items:LiveData<MutableList<MenuItemsTemp>> get() = _items
    val itemsPreview:LiveData<MutableList<ItemPreview>> get() = _itemsPreview
    val editItem:LiveData<MenuItemsTemp?> get() = _editItem

    fun getMenuSettings():LiveData<MenuSettings> = _menuSettings


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

    fun prepareMenu(editMenu:MenuFirebase?){
        menuId = editMenu?.menuId?:UUID.randomUUID().toString()
        viewModelScope.launch(Dispatchers.IO) {
            initCategories(editMenu)
            initMenuData(editMenu)

            refreshCategories()
            refreshItems()
            refreshMenuPreview()
        }

    }


    private suspend fun initMenuData(menu:MenuFirebase?){
        if (_editMenu != null || menu == null) return

        //TODO: Buscar el menu en la db o en firebase y no pasar por parametro serializable
        _editMenu = menu
        _menuSettings.postValue(menu.menuSettings)

        val categories =  menu.items.filter { it.type == ItemStyle.MENU_CATEGORY_HEADER.name && it.id != noCategoryId }.map { Category(id = it.id, name = it.categoryName, position = it.position) }
        categories.forEach{
            saveCategory(menuId = menu.menuId,id = it.id, name = it.name, position = it.position)
        }

        val products = menu.items
            .filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name }
            .map {
                MenuItemsTemp(
                    id = it.id,
                    menuId = menuId,
                    type = it.type,
                    enabled = it.enabled,
                    name = it.name,
                    categoryId = it.categoryId,
                    categoryName = it.categoryName,
                    description = it.description?:"",
                    price = it.price?.toDouble()?:0.0,
                    localImageUri = null,
                    remoteImageUri = it.imageUrl,
                    position = it.position)
            }
        menuTempDS.addMenuItems(products)
    }


    fun addCategory(name:String){
        viewModelScope.launch {
            val foundCategory = menuTempDS.findMenuItemByName(type = ItemStyle.MENU_CATEGORY_HEADER.name, name = name)
            //val foundCategory = _categories.value?.find { it.name.lowercase().trim() == name.lowercase().trim() }
            if(foundCategory != null)
                return@launch

            //val categories = _categories.value!!
            //categories.add(
            //    Category(id = UUID.randomUUID().toString(),
            //        name = name,
            //        position = categories.size+1)//NUMERO ALTO PARA QUE SE ORDENE AL FINAL DE LAS CATEGORIAS
            //)

            val id =  UUID.randomUUID().toString()
            val category = MenuItemsTemp(
                id = id,
                menuId = menuId,
                type = ItemStyle.MENU_CATEGORY_HEADER.name,
                enabled = true,
                categoryId = id,
                categoryName = name,
                name = name,
                description = name,
                position = (categories.value?.size?:0) + 1
            )
            menuTempDS.addCategory(category)
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
                menuId = menuId,
                type = itemStyle.name,
                enabled = enabled,
                categoryId = category.id,
                categoryName = category.name,
                name=name,
                description=description,
                price = amount,
                localImageUri = currentItemImage.value?.toString(),
                remoteImageUri = null,
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

    fun editItem(itemPreview:ItemPreview?){
        viewModelScope.launch(Dispatchers.IO) {
            if(itemPreview != null) {
                val menuItem = menuTempDS.getMenuItemById(id = itemPreview!!.item.id)
                _editItem.postValue(menuItem)
            }else{
                _editItem.postValue(null)
            }
        }
    }
    fun saveEditItemChanges(enabled: Boolean,category:Category,name:String, description:String, price:Double){
        viewModelScope.launch(Dispatchers.IO) {
            val remoteImage = editItem.value?.remoteImageUri
            //Si la imagen actual es la misma que la remota, no se selecciono una imagen local (no se cambio la imagen)
            val currentImage = if(currentItemImage.value != null && currentItemImage.value.toString() != remoteImage) currentItemImage.value else null

            val itemStyle: ItemStyle = getItemStyle(
                hasImage = (currentImage ?: remoteImage) != null,
                hasDescription =  description.isNotBlank())

            val menuItem = _editItem.value?.copy(type = itemStyle.name,enabled = enabled, categoryId = category.id, categoryName = category.name, name = name, description = description, price = price, localImageUri = currentImage?.toString(), remoteImageUri =  remoteImage)
            menuTempDS.updateMenuItem(menuItem!!)
            _editItem.postValue(null)
            refreshItems()
            refreshMenuPreview()
        }

    }

    fun deleteItem(itemPreview:ItemPreview){
        viewModelScope.launch {
            menuTempDS.deleteMenuItemById(id = itemPreview.item.id)
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

    fun processMenu(user:String,companyId: String,fileName:String,itemPreviews:List<ItemPreview>,pdfPath:String){
        val menuSettings = getMenuSettings().value!!
        if(_editMenu == null)
            saveMenu(user= user, companyId= companyId, menuSettings = menuSettings,fileName= fileName, itemPreviews= itemPreviews, pdfPath =  pdfPath)
        else
            saveEditMenu(user = user, companyId = companyId, menuFirebase = _editMenu!!, menuSettings = menuSettings,fileName = fileName, pdfPath = pdfPath)
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
    private fun saveMenu(user:String,companyId: String,menuSettings: MenuSettings,fileName:String,itemPreviews:List<ItemPreview>,pdfPath:String){
        viewModelScope.launch {
            try {
                if(!NetworkUtils.isConnectedToInternet()){
                    throw TimeoutException()
                }

                val tempMenuId:String = UUID.randomUUID().toString()
                val menuStorageUrl = uploadMenuFile(user = user, menuId = tempMenuId, pdfPath = pdfPath).toString()
                val items = prepareItemsFirebase(user = user, menuId = tempMenuId, items = itemPreviews)
                val savedMenu = saveMenuFireBase(user =  user, companyId = companyId, menuId = tempMenuId,fileName = fileName, fileUrl =  menuStorageUrl, items =  items, menuSettings = menuSettings)
                _stateProcessMenu.postValue(ProcessState(State.SUCCESS,savedMenu.fileUrl))
            }catch (e:TimeoutException){
                _stateProcessMenu.postValue(ProcessState(State.NETWORK_ERROR))
            }
            catch (e:Exception){
                _stateProcessMenu.postValue(ProcessState(State.GENERAL_ERROR))
            }

        }
    }

    private fun saveEditMenu(user:String,companyId: String,menuFirebase: MenuFirebase,menuSettings: MenuSettings,fileName:String,pdfPath:String){
        viewModelScope.launch {
            try {
                if(!NetworkUtils.isConnectedToInternet()){
                    throw TimeoutException()
                }

                val menuStorageUrl = uploadMenuFile(user = user,menuId=menuFirebase.menuId, pdfPath = pdfPath).toString()
                deleteUnusedImagesFromFireStore(user = user,_itemsPreview.value!!)
                val items = prepareItemsFirebase(user = user, menuId=menuFirebase.menuId, items = _itemsPreview.value!!)

                val savedMenu = editMenuFireBase(user =  user, companyId = companyId, menuFirebase = menuFirebase,fileName = fileName, fileUrl =  menuStorageUrl, items =  items, menuSettings = menuSettings)
                _stateProcessMenu.postValue(ProcessState(State.SUCCESS,savedMenu.fileUrl))
            }catch (e:TimeoutException){
                _stateProcessMenu.postValue(ProcessState(State.NETWORK_ERROR))
            }catch (e:Exception){
                _stateProcessMenu.postValue(ProcessState(State.GENERAL_ERROR))
            }

        }
    }

    private suspend fun uploadMenuFile(user:String,menuId: String, pdfPath:String):Uri {
      return menuStorage.uploadFile(user,menuId,pdfPath)
    }
    private suspend fun prepareItemsFirebase(user:String,menuId: String, items:List<ItemPreview>):List<ItemFirebase>{

        //UPLOAD LOCAL IMAGES ONLY
        val firebaseItems = items.mapIndexed { i,preview->
            val item = preview.item
            val menuItem = menuTempDS.getMenuItemById(item.id)
            val image =  if(item.localImage !=null) "*" else item.remoteImage?.toString()
            ItemFirebase(id = item.id,type = preview.itemStyle.name, categoryId = item.categoryId,categoryName = menuItem.categoryName, name = item.name, description = item.description, price = item.amount.toString(),position = item.position, imageUrl = image)
        }

        firebaseItems.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.imageUrl.equals("*") }
            .forEach{
                val itemPreview = items.first{ i-> i.item.id == it.id}
                val url = menuStorage.uploadMenuItemsImages(user,menuId,itemPreview.item.localImage!!)
                it.imageUrl = url.toString()
            }
        return firebaseItems
    }

    private suspend fun deleteUnusedImagesFromFireStore(user:String,itemPreviews:List<ItemPreview>){
        if(_editMenu != null){
            //items de firebase con imagen
            val fireBaseItems = _editMenu!!.items.filter { it.imageUrl != null }

            //removed items (items que estaban en la DB de firebase y en despues de la edicion ya no)
            val deletedItems = fireBaseItems.filter {fbi->
               fbi.id !in itemPreviews.map { it.item.id }
            }
            //existing items with removed images (Items guardados en firebase que TENIAN IMAGEN y ahora NO TIENEN)
            val removedImages = fireBaseItems.filter { fbi->
                fbi.id in itemPreviews.filter { it.item.localImage == null && it.item.remoteImage == null }.map { it.item.id }
            }
            //images changed (items que tenian imagen remota y se la actualizaron)
            val changedImages = fireBaseItems.filter {fbi->
                fbi.id in itemPreviews.filter { it.item.localImage != null && it.item.remoteImage == null }.map { it.item.id }
            }

            val imagesToRemove = mutableListOf<String?>()
            imagesToRemove.addAll(deletedItems.map { it.imageUrl })
            imagesToRemove.addAll(removedImages.map { it.imageUrl })
            imagesToRemove.addAll(changedImages.map { it.imageUrl })
            //DELETE ALL UNUSED IMAGES
            imagesToRemove.forEach{
                    menuStorage.removeMenuItemsImages(user,_editMenu!!.menuId,Uri.parse(it))
                }
        }
    }
     private fun saveMenuFireBase(user:String,companyId:String,menuId:String, fileName:String, fileUrl:String, items:List<ItemFirebase>,menuSettings: MenuSettings):MenuFirebase{
         val menu = MenuFirebase(menuId = menuId,name=fileName,fileUrl= fileUrl, items = items, menuSettings = menuSettings)
         menuService.saveMenu(user, companyId = companyId,menu)
         return menu
    }

    private fun editMenuFireBase(user:String,companyId:String,menuFirebase: MenuFirebase, fileName:String, fileUrl:String, items:List<ItemFirebase>, menuSettings: MenuSettings):MenuFirebase{
        val menu = MenuFirebase(fireBaseRef = menuFirebase.fireBaseRef,menuId = menuFirebase.menuId,name=fileName,fileUrl= fileUrl, items = items, menuSettings = menuSettings)
        menuService.updateMenu(user, companyId = companyId,menu)
        return menu
    }




    private suspend fun initCategories(menuFirebase: MenuFirebase?){
        //reasignar valor inicial a la posicion de NO_CATEGORY
        val position = menuFirebase?.items?.firstOrNull{it.id == noCategoryId}?.position?:0
        if(_categories.value == null){
            menuTempDS.clearAll()
            saveCategory(menuId, noCategoryId, noCategoryDescription, position)
        }
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

    private suspend fun getItemPreviews():List<ItemPreview>{
        return menuTempDS.getMenuItemsByType(
            ItemStyle.MENU_CATEGORY_HEADER.name,
            ItemStyle.MENU_TITLE_PRICE.name,
            ItemStyle.MENU_TITLE_DESCRIPTION_PRICE.name,
            ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE.name).map {
            ItemPreview(
                item = Item(id = it.id,enabled = it.enabled, categoryId = it.categoryId, name = it.name, description = it.description, amount = it.price, position = it.position, localImage = if(!it.localImageUri.isNullOrBlank())Uri.parse(it.localImageUri) else null,remoteImage = if(!it.remoteImageUri.isNullOrBlank()) Uri.parse(it.remoteImageUri) else null),
                itemStyle = ItemStyle.valueOf(it.type)
            )
        }.sortedBy { it.item.position }
    }

    private fun orderCategoriesByName(categories:MutableList<Category>) {
        val noCategory = categories.first{it.id == noCategoryId}
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
        menuTempDS.addCategory(category)
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
        private val menuTempDS:Provider<MenuTempDS>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateMenuViewModel(menuStorage = menuStorageProvider.get(), menuService = menuService.get(), menuTempDS = menuTempDS.get()) as T
        }

    }

}