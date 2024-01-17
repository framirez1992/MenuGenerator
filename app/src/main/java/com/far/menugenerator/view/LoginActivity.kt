package com.far.menugenerator.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.MainActivity
import com.far.menugenerator.databinding.ActivityLoginBinding
import com.far.menugenerator.model.LoggedInUser
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import javax.inject.Inject


class LoginActivity : BaseActivity() {
    private lateinit var _binding:ActivityLoginBinding
    private lateinit var viewModel:LoginViewModel

    @Inject lateinit var screenNavigation:ScreenNavigation
    @Inject lateinit var mGoogleSignInClient:GoogleSignInClient


    companion object{
        var account:GoogleSignInAccount?=null
        private const val RC_SIGN_IN = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        presentationComponent.inject(this)

        setContentView(_binding.root)
        initViews()
        initObservers()

        /*
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);*/
    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        updateUI(account)
    }

    private fun updateUI(account:GoogleSignInAccount?){
        if(account != null){
            LoginActivity.account = account
            getAccountInfo()
            screenNavigation.companyListActivity()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            val TAG = "LoginActivity"
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode())
            updateUI(null)
        }
    }
    private fun initViews(){
        _binding.signInButton?.setSize(SignInButton.SIZE_STANDARD)
        _binding.signInButton?.setOnClickListener {
           signIn()
        }
    }
    private fun initObservers(){
        viewModel.state.observe(this){
            _binding.pbLoading?.visibility = if(it.loading) View.VISIBLE else View.GONE
            enableViews(!it.loading)

        }
    }

    private fun enableViews(enable:Boolean){
        _binding.etUsername?.isEnabled = enable
        _binding.etPassword?.isEnabled = enable
        _binding.btnLogin?.isEnabled = enable

    }


    private  fun signIn(){
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    private fun getAccountInfo(){
        if (LoginActivity.account != null) {
            val acct = LoginActivity.account
            val personName = acct!!.displayName
            val personGivenName = acct.givenName
            val personFamilyName = acct.familyName
            val personEmail= acct.email
            val personId = acct.id
            val personPhoto = acct.photoUrl
        }
    }


}