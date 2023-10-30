package com.far.menugenerator.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.R
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.model.CreateMenuState
import com.far.menugenerator.model.Item
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.ItemFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.storage.StorageMenu
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

class CreateMenuViewModel @Inject constructor(
    private val storageMenu: StorageMenu,
    private val menuService:MenuService
):ViewModel() {

    private val _state = MutableLiveData<CreateMenuState>()
    val state:LiveData<CreateMenuState> = _state
    val currentItemImage = MutableLiveData<Uri?>()
    val savedMenuUrl = MutableLiveData<String>()

    private val menuId:String = UUID.randomUUID().toString()


    init {
        currentItemImage.value = null
        _state.value = CreateMenuState(currentScreen = R.id.categoriesScreen)
    }

    /*
    fun selectItemStyle(itemStyle: ItemStyle){
        _state.value = _state.value?.copy(selectedItemStyle = itemStyle)
    }*/
    fun addCategory(category:String){
       val foundCategory = _state.value?.categories?.find { it.lowercase().trim() == category.lowercase().trim() }
        if(!foundCategory.isNullOrEmpty())
            return

        val categories = _state.value?.categories
        categories?.add(category)
        _state.value = _state.value?.copy(categories = categories!!)
    }

    fun addProduct(category: String,name:String,description:String, amount:Double){
        val items = _state.value?.items
        items?.add(Item(category=category,name=name,description=description,amount=amount, image = currentItemImage.value))
        _state.value = _state.value?.copy(items = items!!)
    }

    fun updateCurrentItemImage(imageUri:Uri?){
        currentItemImage.value = imageUri
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

    fun saveMenu(user:String,fileName:String,itemPreviews:List<ItemPreview>,pdfPath:String){
        viewModelScope.launch {
            try {
                val menuStorageUrl = uploadMenuFile(user = user, pdfPath = pdfPath).toString()
                val items = prepareItemsFirebase(user = user, items = itemPreviews)
                val savedMenu = saveMenuFireBase(user,fileName, menuStorageUrl.toString(),items)
                savedMenuUrl.postValue(savedMenu.fileUrl)
            }catch (e:Exception){
                e.printStackTrace()
            }


        }
    }

    private suspend fun uploadMenuFile(user:String, pdfPath:String):Uri {
      return storageMenu.uploadFile(user,menuId,pdfPath)
    }
    private suspend fun prepareItemsFirebase(user:String, items:List<ItemPreview>):List<ItemFirebase>{
        val firebaseItems = items.mapIndexed { i,item->
            ItemFirebase(type = item.itemStyle.name,categoryName = item.categoryName, name = item.name, description = item.description, price = item.price, imageUrl = if(item.image!=null) "*" else null, position = i)
        }
        firebaseItems.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.imageUrl.equals("*") }
            .forEach{
                val url = storageMenu.uploadMenuItemsImages(user,menuId,items[it.position].image!!)
                it.imageUrl = url.toString()
            }
        return firebaseItems
    }
     private fun saveMenuFireBase(user:String, fileName:String, fileUrl:String, items:List<ItemFirebase>):MenuFirebase{
         val menu = MenuFirebase(name=fileName,fileUrl= fileUrl, items = items)
         menuService.saveMenu(user,menuId,menu)
         return menu
    }

    class CreateMenuViewModelFactory @Inject constructor (
        private val storageMenuProvider: Provider<StorageMenu>, //usamos provider para eviar bugs (ver video)
        private val menuService:Provider<MenuService>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateMenuViewModel(storageMenu = storageMenuProvider.get(), menuService = menuService.get()) as T
        }

    }

}