package com.far.menugenerator.viewModel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.storage.CompanyStorage
import com.far.menugenerator.model.storage.MenuStorage
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class CompanyListViewModel @Inject constructor (
    private val companyService:CompanyService,
    private val companyStorage:CompanyStorage,
    private val menuService: MenuService,
    private val menuStorage:MenuStorage
): ViewModel() {

    val companies = MutableLiveData<List<CompanyFirebase?>>()
    val isLoading = MutableLiveData<Boolean>()
    val isProcessing = MutableLiveData<Boolean>()

    init {
        companies.postValue(emptyList())
        isLoading.postValue(false)
        isProcessing.postValue(false)
    }
    fun getCompanies(user:String){
        isLoading.postValue(true)
        viewModelScope.launch {
            try{
                companies.postValue(companyService.getCompanies(user))
            }catch (e:Exception){
                e.printStackTrace()
            }
            isLoading.postValue(false)
        }
    }

    fun deleteCompany(user:String, company:CompanyFirebase){
        isProcessing.postValue(true)
        viewModelScope.launch {
            try {
                if(company.logoUrl != null)
                    companyStorage.removeCompanyLogo(user = user, companyId = company.companyId, remoteFileName =company.logoFileName!!)

                val menus = menuService.getMenus(user = user, companyId = company.companyId)

                //Delete all menus files files
                menus?.forEach{
                    //Delete Files
                    menuStorage.removeAllMenuFiles(user = user, menuId = it?.menuId!!)
                    //Delete  Data
                    menuService.deleteMenu(user = user, companyId = company.companyId,it)
                }

                companyService.deleteCompany(user= user, company= company)
            }catch (e:Exception){

            }
            isProcessing.postValue(false)
            getCompanies(user = user)

        }
    }

   class CompanyListViewModelFactory @Inject constructor(
       private val companyService: Provider<CompanyService>,
       private val companyStorage:Provider<CompanyStorage>,
       private val menuService:Provider<MenuService>,
       private val menuStorage:Provider<MenuStorage>
   ):ViewModelProvider.Factory{
       override fun <T : ViewModel> create(modelClass: Class<T>): T {
           return CompanyListViewModel(companyService = companyService.get(),
               companyStorage=companyStorage.get(),
               menuService = menuService.get(),
               menuStorage=menuStorage.get()) as T
       }
   }
}