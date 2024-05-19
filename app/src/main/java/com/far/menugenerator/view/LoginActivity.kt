package com.far.menugenerator.view

import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.far.menugenerator.viewModel.model.ProcessState
import com.far.menugenerator.viewModel.model.State
import com.far.menugenerator.model.firebase.firestore.model.UserFirebase
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


const val TAG="LoginActivity"
class LoginActivity : BaseActivity() {
    private lateinit var _binding:ActivityLoginBinding
    private lateinit var viewModel:LoginViewModel

    @Inject lateinit var factory:LoginViewModel.LoginViewModelFactory
    @Inject lateinit var screenNavigation:ScreenNavigation
    @Inject lateinit var credentialManager: CredentialManager
    private lateinit var auth: FirebaseAuth

    companion object{
        var userFirebase: UserFirebase? =null
        private const val RC_SIGN_IN = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        _binding = ActivityLoginBinding.inflate(layoutInflater)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this,factory)[LoginViewModel::class.java]

        setContentView(_binding.root)
        initViews()
        initObservers()
    }

    private fun updateUI(firebaseUser: FirebaseUser){
        viewModel.loadUser(firebaseUser)
    }
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

    private fun loadUserState(processState: ProcessState){
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

    private suspend fun handleSignIn(result: GetCredentialResponse) {
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


                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()

                        if(authResult.user != null){ updateUI(authResult.user!!) }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                        val message = "${getString(R.string.operation_failed_please_retry)}. ${e.message.toString()}"
                        Snackbar.make(_binding.root,
                            message,
                            Snackbar.LENGTH_LONG).show()
                    }catch (e:FirebaseAuthException){
                        Log.e(TAG, "Received FirebaseAuthException", e)
                        val message = "${e.message.toString()}"
                        Snackbar.make(_binding.root,
                            message,
                            Snackbar.LENGTH_LONG).show()
                        isLoading(false)
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                    val message = "${getString(R.string.operation_failed_please_retry)}. ${"Unexpected type of credential"}"
                    Snackbar.make(_binding.root,
                        message,
                        Snackbar.LENGTH_LONG).show()
                    isLoading(false)
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
                val message = "${getString(R.string.operation_failed_please_retry)}. ${"Unexpected type of credential"}"
                Snackbar.make(_binding.root,
                    message,
                    Snackbar.LENGTH_LONG).show()
                isLoading(false)
            }
        }
    }




}