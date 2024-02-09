package com.far.menugenerator.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.storage.CompanyStorage
import com.far.menugenerator.model.storage.MenuStorage
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class CompanyListViewModel @Inject constructor (
    private val companyService: CompanyService,
    private val companyStorage:CompanyStorage,
    private val menuService: MenuService,
    private val menuStorage:MenuStorage
): ViewModel() {

    private val companies = MutableLiveData<List<CompanyFirebase?>>()
    private val deleteCompanyState = MutableLiveData<ProcessState?>()
    private val searchCompaniesState = MutableLiveData<ProcessState?>()

    fun getCompanies():LiveData<List<CompanyFirebase?>> = companies
    fun getSearchCompaniesState():LiveData<ProcessState?> = searchCompaniesState
    fun getDeleteCompanyState():LiveData<ProcessState?> = deleteCompanyState
    init {
        //companies.postValue(emptyList())
        //searchCompaniesState.postValue(null)
        //deleteCompanyState.postValue(null)
    }

    fun onResume(user:String){
        getCompanies(user=user)
    }

    fun getCompanies(user:String){
        searchCompaniesState.postValue(ProcessState(state =  State.LOADING))
        viewModelScope.launch{
            try{
                companies.postValue(companyService.getCompanies(user))
                searchCompaniesState.postValue(ProcessState(state =  State.SUCCESS))
            }catch (e:Exception){
                e.printStackTrace()
                searchCompaniesState.postValue(ProcessState(state =  State.ERROR,message= e.message))
            }

        }
    }

    fun deleteCompany(user:String, company:CompanyFirebase){
        deleteCompanyState.postValue(ProcessState(state =  State.LOADING))
        viewModelScope.launch {
            try {
                if(company.logoUrl != null)
                    deleteCompanyLogo(user=user,company=company)


                val menus = menuService.getMenus(user = user, companyId = company.companyId)
                //Delete all menus files files
                menus.forEach{
                    //Delete Files
                    removeAllMenuFiles(user = user, menuId = it?.menuId!!)
                    //Delete  Data
                    deleteMenuFromFirebaseDB(user = user, companyId = company.companyId,menu=it)
                }


                deleteCompanyFromFirebaseDB(user= user, company= company)
                deleteCompanyState.postValue(ProcessState(state =  State.SUCCESS))
            }catch (e:Exception){
                deleteCompanyState.postValue(ProcessState(state =  State.ERROR, message = e.message))
            }

            getCompanies(user = user)

        }
    }

    private suspend fun deleteCompanyLogo(user:String, company:CompanyFirebase){
        try{
            companyStorage.removeCompanyLogo(user = user, companyId = company.companyId, remoteFileName =company.logoFileName!!)
        }catch (e:StorageException){
            if(e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND){
                throw e
            }
        }
    }

    private suspend fun removeAllMenuFiles(user:String,menuId:String){
        try{
            menuStorage.removeAllMenuFiles(user = user, menuId = menuId)
        }catch (e:StorageException){
            if(e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND){
                throw e
            }
        }

    }

    private fun deleteMenuFromFirebaseDB(user:String,companyId:String,menu:MenuFirebase){
        try{
            menuService.deleteMenu(user = user, companyId = companyId,m=menu)
        }catch (e: FirebaseFirestoreException){
            if(e.code != FirebaseFirestoreException.Code.NOT_FOUND){
                throw e
            }
        }
    }

    private fun deleteCompanyFromFirebaseDB(user:String, company:CompanyFirebase){
        try{
            companyService.deleteCompany(user= user, company= company)
        }catch (e: FirebaseFirestoreException){
            if(e.code != FirebaseFirestoreException.Code.NOT_FOUND){
                throw e
            }
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