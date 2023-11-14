package com.far.menugenerator

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.databinding.ActivityMainBinding
import com.far.menugenerator.view.LoginActivity
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.MainActivityViewModel
import com.far.menugenerator.viewModel.MainActivityViewModel_Factory
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import javax.inject.Inject

class MainActivity : BaseActivity() {

    private lateinit var _binding:ActivityMainBinding
    private lateinit var viewModel:MainActivityViewModel
    @Inject lateinit var factory: MainActivityViewModel.MainActivityViewModelFactory

    @Inject lateinit var screenNavigation: ScreenNavigation
    @Inject lateinit var mGoogleSignInClient: GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this,factory)[MainActivityViewModel::class.java]

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        initObservers()

        if(savedInstanceState == null){
            //if is first time
            viewModel.searchCompanies(LoginActivity.account?.email!!)
        }
    }

    private fun initObservers(){
        viewModel.companies.observe(this){
            if(it.isEmpty()) {
                screenNavigation.companyFragment(null)
                //else if(it.size == 1)
                //screenNavigation.menuListFragment(it.first()!!)
            } else
            screenNavigation.companyListFragment()
        }

    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                finish()
            }
    }

}