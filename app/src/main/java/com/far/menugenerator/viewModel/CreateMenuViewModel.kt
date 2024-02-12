package com.far.menugenerator.viewModel

import android.net.Uri
import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.R
import com.far.menugenerator.model.Category
import com.far.menugenerator.model.CreateMenuState
import com.far.menugenerator.model.Item
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.ItemFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.storage.MenuStorage
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider


class CreateMenuViewModel @Inject constructor(
    private val menuStorage: MenuStorage,
    private val menuService:MenuService
):ViewModel() {

    companion object{
        val noCategory = Category(id = "-1", name = "NO_CATEGORY", position = 0)
    }
    private val _state = MutableLiveData<CreateMenuState>()
    private val _categories = MutableLiveData<MutableList<Category>>()
    private val _items = MutableLiveData<MutableList<Item>>()
    private val _itemsPreview = MutableLiveData<MutableList<ItemPreview>>()
    private val _editItem = MutableLiveData<Item?>()

    private var _editMenu:MenuFirebase? = null
    val state:LiveData<CreateMenuState> get() = _state
    val categories:LiveData<MutableList<Category>> get() = _categories
    val items:LiveData<MutableList<Item>> get() = _items
    val itemsPreview:LiveData<MutableList<ItemPreview>> get() = _itemsPreview
    val editItem:LiveData<Item?> get() = _editItem



    val currentItemImage = MutableLiveData<Uri?>()
    val savedMenuUrl = MutableLiveData<String>()


    init {

        currentItemImage.value = null
        _editItem.value = null
        _itemsPreview.value = mutableListOf()

        _state.value = CreateMenuState(currentScreen = R.id.categoriesScreen)
        _categories.value = mutableListOf(noCategory)
        _items.value = mutableListOf()
    }

    fun prepareMenuEdit(editMenu:MenuFirebase?){
        if (_editMenu != null || editMenu == null) return

        _editMenu = editMenu
        val categories =  editMenu.items.filter { it.type == ItemStyle.MENU_CATEGORY_HEADER.name && it.categoryName == it.name && it.name != noCategory.name }.sortedBy { it.position }.map { Category(id = UUID.randomUUID().toString(), name = it.categoryName, position = 0) }
        val c = _categories.value
        c?.addAll(categories)
        _categories.postValue(c!!)
        val products = editMenu.items.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name }.sortedBy { it.position }.map { Item(id = it.id, category = Category(id = UUID.randomUUID().toString(), name = it.categoryName, position = 0), name = it.name, description = it.description?:"", amount = it.price?.toDouble()?:0.0, remoteImage = if(it.imageUrl!= null) Uri.parse(it.imageUrl) else null) }
        _items.value?.addAll(products)
        refreshMenuPreview()

    }

    fun addCategory(name:String){
       val foundCategory = _categories.value?.find { it.name.lowercase().trim() == name.lowercase().trim() }
        if(foundCategory != null)
            return
        val categories = _categories.value
        categories?.add(Category(id = UUID.randomUUID().toString(), name = name, position = categories.size +1))
        _categories.postValue(categories!!)
    }
    fun saveEditCategory(editCategory:Category){
        val categoryItems = _items.value?.filter { it.category.id == editCategory.id }
        categoryItems?.forEach {
            it.category = editCategory
        }

        val categories = _categories.value!!
        val oldCategory = _categories.value!!.find { it.id == editCategory.id }
        val index = categories.indexOf(oldCategory)
        categories?.remove(oldCategory)
        categories?.add(index,editCategory)

        _categories.postValue(categories)
        refreshMenuPreview()

    }

    fun removeCategory(category: Category){
        val categories = _categories.value
        categories?.remove(category)

        val categoryItems = _items.value?.filter { it.category == category }
        categoryItems?.forEach{
            it.category = noCategory
        }
        _categories.postValue(categories!!)
        refreshMenuPreview()
    }

    fun addProduct(category: Category,name:String,description:String, amount:Double){
        val products = _items.value
        products?.add(Item(id = UUID.randomUUID().toString(),category=category,name=name,description=description,amount=amount, localImage = currentItemImage.value))
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
        val currentImage = currentItemImage.value
        val remoteImage = editItem.value?.remoteImage
        val item = _editItem.value?.copy(category = category, name = name, description = description, amount = price, localImage = currentItemImage.value, remoteImage = if(remoteImage == currentImage) remoteImage else null)

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
    private fun refreshMenuPreview(){
        var previewList = mutableListOf<ItemPreview>()
        val categories = _categories.value
        val items = _items.value
        previewList.addAll(categories!!.map { ItemPreview(item=Item(id = it.id, category = it, name = it.name, description = it.name, amount = 0.0, localImage = null, remoteImage = null),itemStyle = ItemStyle.MENU_CATEGORY_HEADER, position = categories.indexOf(it)) })

        previewList.addAll(items!!.map {
            var itemStyle: ItemStyle = if((it.localImage ?: it.remoteImage) != null){
                ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE
            }else if(!it.description.isNullOrBlank()){
                ItemStyle.MENU_TITLE_DESCRIPTION_PRICE
            }else{
                ItemStyle.MENU_TITLE_PRICE
            }

            ItemPreview(item = it,itemStyle = itemStyle, position = items.indexOf(it))
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
        if(_editMenu == null)
            saveMenu(user= user, companyId= companyId, fileName= fileName, itemPreviews= itemPreviews, pdfPath =  pdfPath)
        else
            saveEditMenu(user = user, companyId = companyId, menuFirebase = _editMenu!!,fileName = fileName, pdfPath = pdfPath)
    }
    private fun saveMenu(user:String,companyId: String,fileName:String,itemPreviews:List<ItemPreview>,pdfPath:String){
        viewModelScope.launch {
            try {
                val tempMenuId:String = UUID.randomUUID().toString()
                val menuStorageUrl = uploadMenuFile(user = user, menuId = tempMenuId, pdfPath = pdfPath).toString()
                val items = prepareItemsFirebase(user = user, menuId = tempMenuId, items = itemPreviews)
                val savedMenu = saveMenuFireBase(user =  user, companyId = companyId, menuId = tempMenuId,fileName = fileName, fileUrl =  menuStorageUrl, items =  items)
                savedMenuUrl.postValue(savedMenu.fileUrl)
            }catch (e:Exception){
                e.printStackTrace()
            }

        }
    }

    private fun saveEditMenu(user:String,companyId: String,menuFirebase: MenuFirebase,fileName:String,pdfPath:String){
        viewModelScope.launch {
            try {
                //TODO: REPLACE OLD PDF FILE WITH NEW ONE
                val menuStorageUrl = uploadMenuFile(user = user,menuId=menuFirebase.menuId, pdfPath = pdfPath).toString()

                //TODO: ITEMS MODIFIED WITH LOCAL IMAGES: Replace old ones with new ones
                deleteUnusedImagesFromFireStore(user = user,_itemsPreview.value!!)
                val items = prepareItemsFirebase(user = user, menuId=menuFirebase.menuId, items = _itemsPreview.value!!)

                val savedMenu = editMenuFireBase(user =  user, companyId = companyId, menuFirebase = menuFirebase,fileName = fileName, fileUrl =  menuStorageUrl, items =  items)
                savedMenuUrl.postValue(savedMenu.fileUrl)
            }catch (e:Exception){
                e.printStackTrace()
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
            ItemFirebase(id = item.id,type = preview.itemStyle.name,categoryName = item.category.name, name = item.name, description = item.description, price = item.amount.toString(),position = i, imageUrl = image)
        }

        firebaseItems.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.imageUrl.equals("*") }
            .forEach{
                val url = menuStorage.uploadMenuItemsImages(user,menuId,items[it.position].item.localImage!!)
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
     private fun saveMenuFireBase(user:String,companyId:String,menuId:String, fileName:String, fileUrl:String, items:List<ItemFirebase>):MenuFirebase{
         val menu = MenuFirebase(menuId = menuId,name=fileName,fileUrl= fileUrl, items = items)
         menuService.saveMenu(user, companyId = companyId,menu)
         return menu
    }

    private fun editMenuFireBase(user:String,companyId:String,menuFirebase: MenuFirebase, fileName:String, fileUrl:String, items:List<ItemFirebase>):MenuFirebase{
        val menu = MenuFirebase(fireBaseRef = menuFirebase.fireBaseRef,menuId = menuFirebase.menuId,name=fileName,fileUrl= fileUrl, items = items)
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