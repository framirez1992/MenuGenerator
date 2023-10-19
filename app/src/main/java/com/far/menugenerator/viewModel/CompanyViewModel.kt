package com.far.menugenerator.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.far.menugenerator.R
import com.far.menugenerator.model.Company
import com.far.menugenerator.model.CompanyState

class CompanyViewModel: ViewModel() {

    private val _state = MutableLiveData<CompanyState>()
    val state:LiveData<CompanyState> = _state
    init {
        _state.value = CompanyState(currentScreen= R.id.layoutCompanyName, company= Company())
    }

    fun nextScreen(){
        val nextScreen = when (_state.value?.currentScreen){
            R.id.layoutCompanyName-> R.id.layoutCompanyAddress
            R.id.layoutCompanyAddress-> R.id.layoutCompanyContact
            else -> R.id.layoutCompanyLogo
        }
        _state.value = _state.value?.copy(currentScreen = nextScreen)

    }
    fun previousScreen(){
        val previousScreen = when (_state.value?.currentScreen){
            R.id.layoutCompanyLogo-> R.id.layoutCompanyContact
            R.id.layoutCompanyContact-> R.id.layoutCompanyAddress
            else -> R.id.layoutCompanyName
        }
        _state.value = _state.value?.copy(currentScreen = previousScreen)
    }


}