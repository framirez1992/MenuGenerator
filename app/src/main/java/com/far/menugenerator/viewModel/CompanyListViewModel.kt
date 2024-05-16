package com.far.menugenerator.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.viewModel.model.ProcessState
import com.far.menugenerator.viewModel.model.State
import com.far.menugenerator.model.firebase.firestore.CompanyService
import com.far.menugenerator.model.firebase.firestore.MenuService
import com.far.menugenerator.model.firebase.firestore.model.CompanyFirebase
import com.far.menugenerator.model.firebase.firestore.model.MenuFirebase
import com.far.menugenerator.model.firebase.storage.CompanyStorage
import com.far.menugenerator.model.firebase.storage.MenuStorage
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.launch
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Provider

class CompanyListViewModel @Inject constructor (
    private val companyService: CompanyService,
    private val companyStorage: CompanyStorage,
    private val menuService: MenuService,
    private val menuStorage: MenuStorage
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

    fun onResume(uid:String){
        getCompanies(uid=uid)
    }

    fun getCompanies(uid:String){
        searchCompaniesState.postValue(ProcessState(state =  State.LOADING))
        viewModelScope.launch{
            try{
                companies.postValue(companyService.getCompanies(uid))
                searchCompaniesState.postValue(ProcessState(state =  State.SUCCESS))
            }catch (e:Exception){
                e.printStackTrace()
                searchCompaniesState.postValue(ProcessState(state =  State.GENERAL_ERROR,message= e.message))
            }

        }
    }

    fun deleteCompany(uid:String, company: CompanyFirebase){
        deleteCompanyState.postValue(ProcessState(state =  State.LOADING))
        viewModelScope.launch {
            try {

                if(!NetworkUtils.isConnectedToInternet()){
                    throw TimeoutException()
                }

                if(company.logoUrl != null)
                    deleteCompanyLogo(uid=uid,company=company)


                val menus = menuService.getMenus(user = uid, companyId = company.companyId)
                //Delete all menus files files
                menus.forEach{
                    //Delete Files
                    removeAllMenuFiles(uid = uid, companyId = company.companyId, menuId = it?.menuId!!)
                    //Delete  Data
                    deleteMenuFromFirebaseDB(user = uid, companyId = company.companyId,menu=it)
                }


                deleteCompanyFromFirebaseDB(user= uid, company= company)
                deleteCompanyState.postValue(ProcessState(state =  State.SUCCESS))
            }catch (e:TimeoutException){
                deleteCompanyState.postValue(ProcessState(state =  State.NETWORK_ERROR, message = e.message))
            }
            catch (e:Exception){
                deleteCompanyState.postValue(ProcessState(state =  State.GENERAL_ERROR, message = e.message))
            }

            getCompanies(uid = uid)

        }
    }

    private suspend fun deleteCompanyLogo(uid:String, company: CompanyFirebase){
        try{
            companyStorage.removeCompanyLogo(uid = uid, companyId = company.companyId, remoteFileName =company.logoFileName!!)
        }catch (e:StorageException){
            if(e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND){
                throw e
            }
        }
    }

    private suspend fun removeAllMenuFiles(uid:String,companyId: String, menuId:String){
        try{
            menuStorage.removeAllMenuFiles(uid = uid, companyId=companyId, menuId = menuId)
        }catch (e:StorageException){
            if(e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND){
                throw e
            }
        }

    }

    private fun deleteMenuFromFirebaseDB(user:String,companyId:String,menu: MenuFirebase){
        try{
            menuService.deleteMenu(user = user, companyId = companyId,m=menu)
        }catch (e: FirebaseFirestoreException){
            if(e.code != FirebaseFirestoreException.Code.NOT_FOUND){
                throw e
            }
        }
    }

    private fun deleteCompanyFromFirebaseDB(user:String, company: CompanyFirebase){
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