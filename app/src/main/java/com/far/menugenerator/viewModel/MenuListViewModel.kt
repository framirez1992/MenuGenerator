package com.far.menugenerator.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.storage.MenuStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class MenuListViewModel @Inject constructor(
    private val menuService:MenuService,
    private val menuStorage:MenuStorage
): ViewModel() {
    val menus = MutableLiveData<List<MenuFirebase?>>()
    val isLoading = MutableLiveData<Boolean>()

    init {
        menus.postValue(emptyList())
        isLoading.postValue(false)
    }

    fun getMenus(user:String, companyId:String){
        viewModelScope.launch {
            isLoading.postValue(true)
            val m = menuService.getMenus(user,companyId)
            menus.postValue(m)

            isLoading.postValue(false)
        }
    }

    fun deleteMenu(user:String,companyId:String,menuFirebase: MenuFirebase){
        viewModelScope.launch(Dispatchers.Default){
            try{
                menuStorage.removeAllMenuFiles(user = user, menuId = menuFirebase.menuId)
                menuService.deleteMenu(user = user,companyId = companyId,m=menuFirebase)
                getMenus(user=user,companyId=companyId)
            }catch (e:Exception){
                e.printStackTrace()
            }

        }


    }
   class MenuListViewModelFactory @Inject constructor(
       private val menuService: Provider<MenuService>,
       private val menuStorage: Provider<MenuStorage>
   ):ViewModelProvider.Factory{
       override fun <T : ViewModel> create(modelClass: Class<T>): T {
           return MenuListViewModel(menuService = menuService.get(), menuStorage = menuStorage.get()) as T
       }
   }

}