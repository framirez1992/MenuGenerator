package com.far.menugenerator.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.R
import com.far.menugenerator.databinding.ActivityLoginBinding
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.model.UserFirebase
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject


class LoginActivity : BaseActivity() {
    private lateinit var _binding:ActivityLoginBinding
    private lateinit var viewModel:LoginViewModel

    @Inject lateinit var factory:LoginViewModel.LoginViewModelFactory
    @Inject lateinit var screenNavigation:ScreenNavigation
    @Inject lateinit var mGoogleSignInClient:GoogleSignInClient

    companion object{
        var userFirebase:UserFirebase? =null
        private const val RC_SIGN_IN = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityLoginBinding.inflate(layoutInflater)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this,factory)[LoginViewModel::class.java]

        setContentView(_binding.root)
        initViews()
        initObservers()
    }

    override fun onStart() {
        super.onStart()
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        if(account != null) {
            viewModel.loadUser(account)
        }else{
            isLoading(false)
        }
    }

    private fun updateUI(account:GoogleSignInAccount?){
        if(account != null){
            viewModel.loadUser(account)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            val TAG = "LoginActivity"
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode())
            updateUI(null)
            Snackbar.make(_binding.root,
                e.message.toString(),
                Snackbar.LENGTH_LONG).show()
        }
    }
    private fun initViews(){
        _binding.signInButton.setSize(SignInButton.SIZE_STANDARD)
        _binding.signInButton.setOnClickListener {
           signIn()
        }
    }
    private fun initObservers(){
        viewModel.loadUserState.observe(this){
            loadUserState(it)
        }
    }

    private fun loadUserState(processState:ProcessState){
        if(processState.state == State.LOADING){
            isLoading(true)
        }else if(processState.state == State.GENERAL_ERROR){
            isLoading(false)
            Snackbar.make(_binding.root,getString(R.string.operation_failed_please_retry),Snackbar.LENGTH_LONG).show()
        }else{//success
            userFirebase = viewModel.getUser()
            screenNavigation.companyListActivity()
        }
    }

    private fun isLoading(loading: Boolean){
        _binding.pb.visibility = if(loading) View.VISIBLE else View.GONE
        _binding.signInButton.visibility = if(loading) View.GONE else View.VISIBLE
    }


    private  fun signIn(){
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun getAccountInfo(){
        //if (LoginActivity.userFirebase != null) {
        //    val acct = LoginActivity.userFirebase
        //    val personName = acct!!.displayName
        //    val personGivenName = acct.givenName
        //    val personFamilyName = acct.familyName
        //    val personEmail= acct.email
        //    val personId = acct.id
        //    val personPhoto = acct.photoUrl
        //}
    }


}