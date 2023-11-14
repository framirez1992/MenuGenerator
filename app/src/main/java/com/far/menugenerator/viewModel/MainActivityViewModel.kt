package com.far.menugenerator.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.model.CompanyFirebase
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class MainActivityViewModel @Inject constructor(
    private val companyService: CompanyService
):ViewModel() {

    val companies = MutableLiveData<List<CompanyFirebase?>>()
    val isLoading = MutableLiveData<Boolean>()

    fun searchCompanies(user:String){
        viewModelScope.launch {
            isLoading.postValue(true)
            val c = companyService.getCompanies(user)
            companies.postValue(c)
            isLoading.postValue(false)
        }

    }

    class MainActivityViewModelFactory @Inject constructor(
        private val companyService: Provider<CompanyService>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(companyService = companyService.get()) as T
        }
    }

}