package com.far.menugenerator.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.storage.MenuStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Provider

class MenuListViewModel @Inject constructor(
    private val menuService:MenuService,
    private val menuStorage:MenuStorage
): ViewModel() {
    val menus = MutableLiveData<List<MenuFirebase?>>()
    private val searchMenuProcess = MutableLiveData<ProcessState>()
    private val deleteMenuProcess = MutableLiveData<ProcessState>()

    fun getSearchMenuProcess():LiveData<ProcessState> = searchMenuProcess
    fun getDeleteMenuProcess():LiveData<ProcessState> = deleteMenuProcess

    init {
        menus.postValue(emptyList())
    }

    fun getMenus(user:String, companyId:String){
        viewModelScope.launch {
            searchMenuProcess.postValue(ProcessState(State.LOADING))
            val m = menuService.getMenus(user,companyId)
            menus.postValue(m)

            searchMenuProcess.postValue(ProcessState(State.SUCCESS))
        }
    }

    fun deleteMenu(user:String,companyId:String,menuFirebase: MenuFirebase){
        deleteMenuProcess.postValue(ProcessState(State.LOADING))
        viewModelScope.launch(Dispatchers.Default){
            try{
                if(!NetworkUtils.isConnectedToInternet()){
                    throw TimeoutException()
                }
                menuStorage.removeAllMenuFiles(user = user, menuId = menuFirebase.menuId)
                menuService.deleteMenu(user = user,companyId = companyId,m=menuFirebase)
                deleteMenuProcess.postValue(ProcessState(State.SUCCESS))
            }catch (e:TimeoutException){
                deleteMenuProcess.postValue(ProcessState(State.NETWORK_ERROR))
            }
            catch (e:Exception){
                deleteMenuProcess.postValue(ProcessState(State.GENERAL_ERROR))
            }

        }


    }
   class MenuListViewModelFactory @Inject constructor(
       private val menuService: Provider<MenuService>,
       private val menuStorage: Provider<MenuStorage>
   ):ViewModelProvider.Factory{
       override fun <T : ViewModel> create(modelClass: Class<T>): T {
           return MenuListViewModel(
               menuService = menuService.get(),
               menuStorage = menuStorage.get()) as T
       }
   }

}