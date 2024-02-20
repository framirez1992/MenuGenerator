package com.far.menugenerator.viewModel

import android.net.Uri
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.R
import com.far.menugenerator.common.utils.FileUtils
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
import com.far.menugenerator.model.storage.MenuStorage
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.coroutineContext


class CreateMenuViewModel @Inject constructor(
    private val menuStorage: MenuStorage,
    private val menuService:MenuService
):ViewModel() {

    companion object{
        val noCategory = Category(id = "-1", name = "NO_CATEGORY", position = 0)
    }
    private val _state = MutableLiveData<CreateMenuState>()
    private val _stateProcessMenu = MutableLiveData<ProcessState>()
    private val _menuSettings= MutableLiveData<MenuSettings>()
    private val _categories = MutableLiveData<MutableList<Category>>()
    private val _items = MutableLiveData<MutableList<Item>>()
    private val _itemsPreview = MutableLiveData<MutableList<ItemPreview>>()
    private val _editItem = MutableLiveData<Item?>()

    private var _editMenu:MenuFirebase? = null

    val state:LiveData<CreateMenuState> get() = _state
    val stateProcessMenu:LiveData<ProcessState> get() = _stateProcessMenu
    val categories:LiveData<MutableList<Category>> get() = _categories
    val items:LiveData<MutableList<Item>> get() = _items
    val itemsPreview:LiveData<MutableList<ItemPreview>> get() = _itemsPreview
    val editItem:LiveData<Item?> get() = _editItem

    fun getMenuSettings():LiveData<MenuSettings> = _menuSettings


    val currentItemImage = MutableLiveData<Uri?>()

    init {
        currentItemImage.value = null
        _editItem.value = null
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
        _itemsPreview.value = mutableListOf()

        _state.value = CreateMenuState(currentScreen = R.id.categoriesScreen)
        _categories.value = mutableListOf(noCategory)
        _items.value = mutableListOf()
    }

    fun getCurrentMenuName():String?{
        return _editMenu?.name
    }
    fun prepareMenuEdit(editMenu:MenuFirebase?){
        if (_editMenu != null || editMenu == null) return

        _editMenu = editMenu
        val categories =  editMenu.items.filter { it.type == ItemStyle.MENU_CATEGORY_HEADER.name && it.categoryName == it.name && it.name != noCategory.name }.sortedBy { it.categoryName }.map { Category(id = it.id, name = it.categoryName, position = it.position) }
        val currentCategories = _categories.value
        currentCategories?.addAll(categories)
        _categories.postValue(currentCategories!!)
        _menuSettings.postValue(editMenu.menuSettings)

        val products = editMenu.items
            .filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name }
            .sortedBy { it.position }
            .map {
                Item(
                    id = it.id,
                    name = it.name,
                    description = it.description?:"",
                    amount = it.price?.toDouble()?:0.0,
                    remoteImage = if(it.imageUrl!= null) Uri.parse(it.imageUrl) else null,
                    position = it.position,
                    category = Category(
                        id = it.categoryId,
                        name = it.categoryName,
                        position = currentCategories.first { c -> c.id == it.categoryId }.position))
            }
        _items.value?.addAll(products)
        refreshMenuPreview()

    }

    fun addCategory(name:String){
       val foundCategory = _categories.value?.find { it.name.lowercase().trim() == name.lowercase().trim() }
        if(foundCategory != null)
            return
        val categories = _categories.value!!

        categories.add(
            Category(id = UUID.randomUUID().toString(),
                name = name,
                position = categories.size+1)//NUMERO ALTO PARA QUE SE ORDENE AL FINAL DE LAS CATEGORIAS
        )
        orderCategoriesByName(categories)
        _categories.postValue(categories)
    }
    fun saveEditCategory(editCategory:Category){
        val categoryItems = _items.value?.filter { it.category.id == editCategory.id }
        categoryItems?.forEach {
            it.category = editCategory
        }

        val categories = _categories.value!!
        categories.first { it.id == editCategory.id }.name = editCategory.name
        orderCategoriesByName(categories)
        _categories.postValue(categories)
        refreshMenuPreview()

    }

    /**
     * Siempre mover el NO_CATEGORY de primero
     */
    private fun orderCategoriesByName(categories:MutableList<Category>) {
        categories.removeAt(categories.indexOf(noCategory))
        categories.sortBy { it.name }
        categories.add(0, noCategory)
    }

    fun removeCategory(category: Category){
        val categories = _categories.value
        categories?.remove(category)

        _items.value?.filter { it.category == category }?.forEach{
            it.category = noCategory
        }
        _categories.postValue(categories!!)
        refreshMenuPreview()
    }

    fun addProduct(category: Category,name:String,description:String, amount:Double){
        val products = _items.value
        products?.add(
            Item(id = UUID.randomUUID().toString(),
                category=category,
                name=name,
                description=description,
                amount=amount,
                position = (products.size)+1,
                localImage = currentItemImage.value))
        _items.postValue(products!!)
        refreshMenuPreview()
    }

    fun updateCurrentItemImage(imageUri:Uri?){
        currentItemImage.value = imageUri
    }

    fun editItem(itemPreview:ItemPreview){
        _editItem.postValue(itemPreview.item)
    }
    fun saveEditItemChanges(category:Category,name:String, description:String, price:Double){
        val remoteImage = editItem.value?.remoteImage
        //Si la imagen actual es la misma que la remota, no se selecciono una imagen local (no se cambio la imagen)
        val currentImage = if(currentItemImage.value != remoteImage) currentItemImage.value else null

        val item = _editItem.value?.copy(category = category, name = name, description = description, amount = price, localImage = currentImage, remoteImage =  remoteImage)

        val oldItem = items.value?.find { it.id == item?.id }
        _items.value?.remove(oldItem)
        _items.value?.add(item!!)
        _editItem.value = null
        refreshMenuPreview()
    }

    fun deleteItem(itemPreview:ItemPreview){
        _items.value?.remove(itemPreview.item)
        refreshMenuPreview()
    }

    fun updateMenuSettings(ms: MenuSettings){
        _menuSettings.value = ms
    }
    private fun refreshMenuPreview(){
        var previewList = mutableListOf<ItemPreview>()
        val categories = _categories.value
        previewList.addAll(
            categories!!
                .map {
                    ItemPreview(
                        item=Item(id = it.id, category = it, name = it.name, description = it.name, amount = 0.0, position = it.position, localImage = null, remoteImage = null),itemStyle = ItemStyle.MENU_CATEGORY_HEADER) })
        val items = _items.value
        previewList.addAll(items!!.map {
            var itemStyle: ItemStyle = if((it.localImage ?: it.remoteImage) != null){
                ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE
            }else if(!it.description.isNullOrBlank()){
                ItemStyle.MENU_TITLE_DESCRIPTION_PRICE
            }else{
                ItemStyle.MENU_TITLE_PRICE
            }

            ItemPreview(item = it,itemStyle = itemStyle)
        })
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
        itemPreviewPositions.forEach{ itemP->
            categories.value?.firstOrNull{it.id == itemP.id}?.position = itemP.position
            items.value?.firstOrNull{it.id == itemP.id}?.position = itemP.position
        }

    }
    private fun saveMenu(user:String,companyId: String,menuSettings: MenuSettings,fileName:String,itemPreviews:List<ItemPreview>,pdfPath:String){
        viewModelScope.launch {
            try {
                val tempMenuId:String = UUID.randomUUID().toString()
                val menuStorageUrl = uploadMenuFile(user = user, menuId = tempMenuId, pdfPath = pdfPath).toString()
                val items = prepareItemsFirebase(user = user, menuId = tempMenuId, items = itemPreviews)
                val savedMenu = saveMenuFireBase(user =  user, companyId = companyId, menuId = tempMenuId,fileName = fileName, fileUrl =  menuStorageUrl, items =  items, menuSettings = menuSettings)
                _stateProcessMenu.postValue(ProcessState(State.SUCCESS,savedMenu.fileUrl))
            }catch (e:Exception){
                e.printStackTrace()
                _stateProcessMenu.postValue(ProcessState(State.ERROR))
            }

        }
    }

    private fun saveEditMenu(user:String,companyId: String,menuFirebase: MenuFirebase,menuSettings: MenuSettings,fileName:String,pdfPath:String){
        viewModelScope.launch {
            try {
                //TODO: REPLACE OLD PDF FILE WITH NEW ONE
                val menuStorageUrl = uploadMenuFile(user = user,menuId=menuFirebase.menuId, pdfPath = pdfPath).toString()

                //TODO: ITEMS MODIFIED WITH LOCAL IMAGES: Replace old ones with new ones
                deleteUnusedImagesFromFireStore(user = user,_itemsPreview.value!!)
                val items = prepareItemsFirebase(user = user, menuId=menuFirebase.menuId, items = _itemsPreview.value!!)

                val savedMenu = editMenuFireBase(user =  user, companyId = companyId, menuFirebase = menuFirebase,fileName = fileName, fileUrl =  menuStorageUrl, items =  items, menuSettings = menuSettings)
                _stateProcessMenu.postValue(ProcessState(State.SUCCESS,savedMenu.fileUrl))
            }catch (e:Exception){
                e.printStackTrace()
                _stateProcessMenu.postValue(ProcessState(State.ERROR))
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
            val image =  if(item.localImage !=null) "*" else item.remoteImage?.toString()
            ItemFirebase(id = item.id,type = preview.itemStyle.name, categoryId = item.category.id,categoryName = item.category.name, name = item.name, description = item.description, price = item.amount.toString(),position = item.position, imageUrl = image)
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



    class CreateMenuViewModelFactory @Inject constructor (
        private val menuStorageProvider: Provider<MenuStorage>, //usamos provider para eviar bugs (ver video)
        private val menuService:Provider<MenuService>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateMenuViewModel(menuStorage = menuStorageProvider.get(), menuService = menuService.get()) as T
        }

    }

}