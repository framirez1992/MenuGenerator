package com.far.menugenerator

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.databinding.ActivityMainBinding
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.MainActivityViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import javax.inject.Inject

class MainActivity : BaseActivity() {

    private lateinit var _binding:ActivityMainBinding
    private  lateinit var viewModel:MainActivityViewModel

    @Inject lateinit var screenNavigation: ScreenNavigation
    @Inject lateinit var mGoogleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        presentationComponent.inject(this)


        if(savedInstanceState == null){
            //screenNavigation.companyFragment()
            screenNavigation.createMenuFragment()
        }
    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                finish()
            }
    }

}