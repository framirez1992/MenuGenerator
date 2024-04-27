package com.far.menugenerator.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.far.menugenerator.R
import com.far.menugenerator.common.global.Constants
import com.far.menugenerator.databinding.ActivityLoginBinding
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.model.UserFirebase
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import javax.inject.Inject


const val TAG="LoginActivity"
class LoginActivity : BaseActivity() {
    private lateinit var _binding:ActivityLoginBinding
    private lateinit var viewModel:LoginViewModel

    @Inject lateinit var factory:LoginViewModel.LoginViewModelFactory
    @Inject lateinit var screenNavigation:ScreenNavigation
    @Inject lateinit var credentialManager: CredentialManager
    //@Inject lateinit var mGoogleSignInClient:GoogleSignInClient

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

    private fun updateUI(googleIdTokenCredential: GoogleIdTokenCredential){
        viewModel.loadUser(googleIdTokenCredential)
    }
    /*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }*/

    /*
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
            val message = "${getString(R.string.operation_failed_please_retry)}. ${e.message.toString()}"
            Snackbar.make(_binding.root,
                message,
                Snackbar.LENGTH_LONG).show()
        }
    }*/
    private fun initViews(){
        _binding.btnLogin.setOnClickListener(){
            login()
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
            Snackbar.make(_binding.root,getString(R.string.operation_failed_please_retry),Snackbar.LENGTH_LONG).show()
            isLoading(false)
        }else{//success
            userFirebase = viewModel.getUser()
            screenNavigation.companyListActivity()
            isLoading(false)
        }
    }

    private fun isLoading(loading: Boolean){
        _binding.pb.visibility = if(loading) View.VISIBLE else View.GONE
        //_binding.signInButton.visibility = if(loading) View.GONE else View.VISIBLE
        _binding.btnLogin.visibility = if(loading) View.GONE else View.VISIBLE
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

    private fun login(){
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(Constants.ANDROID_OAUTH_CLIENT_ID)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            isLoading(true)
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity,
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                //handleFailure(e)
                isLoading(false)
                Snackbar.make(_binding.root,e.message?:"GetCredentialException",Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.

        when (val credential = result.credential) {
            /*
            is PublicKeyCredential -> {
                // Share responseJson such as a GetCredentialResponse on your server to
                // validate and authenticate
                val responseJson = credential.authenticationResponseJson
                Toast.makeText(this@LoginActivity,responseJson,Toast.LENGTH_LONG).show()
            }

            is PasswordCredential -> {
                // Send ID and password to your server to validate and authenticate.
                val username = credential.id
                val password = credential.password
                Toast.makeText(this@LoginActivity,username,Toast.LENGTH_LONG).show()
            }
*/
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        updateUI(googleIdTokenCredential)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                        val message = "${getString(R.string.operation_failed_please_retry)}. ${e.message.toString()}"
                        Snackbar.make(_binding.root,
                            message,
                            Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                    val message = "${getString(R.string.operation_failed_please_retry)}. ${"Unexpected type of credential"}"
                    Snackbar.make(_binding.root,
                        message,
                        Snackbar.LENGTH_LONG).show()
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
                val message = "${getString(R.string.operation_failed_please_retry)}. ${"Unexpected type of credential"}"
                Snackbar.make(_binding.root,
                    message,
                    Snackbar.LENGTH_LONG).show()
            }
        }

        isLoading(false)
    }




}