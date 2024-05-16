package com.far.menugenerator.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.viewModel.model.ProcessState
import com.far.menugenerator.viewModel.model.State
import com.far.menugenerator.model.firebase.firestore.UserService
import com.far.menugenerator.model.firebase.firestore.model.PLAN
import com.far.menugenerator.model.firebase.firestore.model.UserFirebase
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class LoginViewModel(
    val userService: UserService
):ViewModel() {

    private val _loadUserState = MutableLiveData<ProcessState>()
    val loadUserState:LiveData<ProcessState> =_loadUserState
    private lateinit var _userFirebase: UserFirebase

    fun getUser()= _userFirebase

    fun loadUser(firebaseUser: FirebaseUser){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loadUserState.postValue(ProcessState(State.LOADING))
                var user = userService.getUser(firebaseUser.uid)
                if(user == null) {
                    user = UserFirebase(
                        accountId = firebaseUser.uid,
                        email = firebaseUser.email,
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