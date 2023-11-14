package com.far.menugenerator.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.far.menugenerator.model.LoggedInUser
import com.far.menugenerator.model.LoginState

class LoginViewModel:ViewModel() {

    private val _state = MutableLiveData<LoginState>()
    val state:LiveData<LoginState> =_state

    //var user = MutableLiveData<LoggedInUser?>()

    init {
        _state.value = LoginState(false,"")
        //user.value = null
    }




}