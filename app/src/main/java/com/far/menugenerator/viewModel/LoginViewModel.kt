package com.far.menugenerator.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.UserService
import com.far.menugenerator.model.database.model.PLAN
import com.far.menugenerator.model.database.model.UserFirebase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

class LoginViewModel(
    val userService: UserService
):ViewModel() {

    private val _loadUserState = MutableLiveData<ProcessState>()
    val loadUserState:LiveData<ProcessState> =_loadUserState
    private lateinit var _userFirebase:UserFirebase

    fun getUser()= _userFirebase

    fun loadUser(account: GoogleSignInAccount){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loadUserState.postValue(ProcessState(State.LOADING))
                var user = userService.getUser(account.email!!)
                if(user == null) {
                    user = UserFirebase(
                        internalId = UUID.randomUUID().toString(),
                        accountId = account.id,
                        email = account.email!!,
                        plan = PLAN.FREE.name,
                        enabled = true
                    )
                    userService.registerUser(user = user)
                    _userFirebase = user
                }else{
                   _userFirebase = user
                }
                _loadUserState.postValue(ProcessState(State.SUCCESS))

            }catch (e:Exception){
                Log.i("LOGIN", e.message.toString())
                _loadUserState.postValue(ProcessState(State.GENERAL_ERROR))
            }
        }
    }



    class LoginViewModelFactory @Inject constructor(
        private val userService: Provider<UserService>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(userService = userService.get()) as T
        }
    }


}