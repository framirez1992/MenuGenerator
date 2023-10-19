package com.far.menugenerator.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.far.menugenerator.R
import com.far.menugenerator.model.CreateMenuState
import com.far.menugenerator.model.Item

class CreateMenuViewModel:ViewModel() {

    private val _state = MutableLiveData<CreateMenuState>()
    val state:LiveData<CreateMenuState> = _state
    val currentItemImage = MutableLiveData<Uri?>()


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
            else -> R.id.menuPreviewScreen
        }
        _state.value = _state.value?.copy(currentScreen = nextScreen)
    }
    fun previousScreen(){
        val previousScreen = when(_state.value?.currentScreen){
            R.id.menuPreviewScreen -> R.id.addMenuItemScreen
            else -> R.id.categoriesScreen
        }
        _state.value = _state.value?.copy(currentScreen = previousScreen)
    }

}