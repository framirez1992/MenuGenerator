package com.far.menugenerator.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.MainActivity
import com.far.menugenerator.model.LoggedInUser
import com.far.menugenerator.databinding.ActivityLoginBinding
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.LoginViewModel
import javax.inject.Inject

class LoginActivity : BaseActivity() {
    private lateinit var _binding:ActivityLoginBinding
    
    private lateinit var viewModel:LoginViewModel

    @Inject lateinit var screenNavigation:ScreenNavigation

    companion object{
        private var user: LoggedInUser?=null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        presentationComponent.inject(this)

        setContentView(_binding.root)
        initViews()
        initObservers()
    }
    
    private fun initViews(){
        _binding.btnLogin?.setOnClickListener {
            viewModel.login()
        }
    }
    private fun initObservers(){
        viewModel.state.observe(this){
            _binding.pbLoading?.visibility = if(it.loading) View.VISIBLE else View.GONE
            enableViews(!it.loading)

        }
        viewModel.user.observe(this){
            user = it
            if(user != null) screenNavigation.mainActivity()
        }
    }

    private fun enableViews(enable:Boolean){
        _binding.etUsername?.isEnabled = enable
        _binding.etPassword?.isEnabled = enable
        _binding.btnLogin?.isEnabled = enable

    }


}