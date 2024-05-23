package com.far.menugenerator.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.core.widget.addTextChangedListener
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.far.menugenerator.R
import com.far.menugenerator.common.global.Constants
import com.far.menugenerator.common.helpers.ActivityHelper
import com.far.menugenerator.common.utils.StringUtils
import com.far.menugenerator.databinding.ActivityLoginBinding
import com.far.menugenerator.databinding.DialogImageTitleDescriptionBinding
import com.far.menugenerator.databinding.DialogPasswordResetBinding
import com.far.menugenerator.viewModel.model.ProcessState
import com.far.menugenerator.viewModel.model.State
import com.far.menugenerator.model.firebase.firestore.model.UserFirebase
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
    @Inject lateinit var dialogManager: DialogManager
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
        setScreenVisible(showMain = true)

        _binding.loginScreen.imgLogo.setOnLongClickListener {
            Toast.makeText(baseContext,Constants.getAppVersion(applicationContext),Toast.LENGTH_LONG).show()
            return@setOnLongClickListener true
        }

        _binding.loginScreen.btnLogin.setOnClickListener{
            _binding.loginScreen.tilEmail.error =""
            _binding.loginScreen.tilPassword.error=""

            val email = _binding.loginScreen.etEmail.text.toString()
            val password = _binding.loginScreen.etPassword.text.toString()
            if(!emailValidation(email,_binding.loginScreen.tilEmail,_binding.loginScreen.etEmail)){
                return@setOnClickListener
            }
            if(!passwordValidation(password,_binding.loginScreen.tilPassword, _binding.loginScreen.etPassword)){
                return@setOnClickListener
            }
            login(email, password)
            hideKeyboard(_binding.container.windowToken)

            //login()
            //signInWithGoogle()
        }

        _binding.loginScreen.tvForgotPassword.setOnClickListener{
            showPasswordResetDialog()
        }

        _binding.loginScreen.llSignup.setOnClickListener{
            setScreenVisible(showMain = false)
        }

        _binding.signUpScreen.btnSignUp.setOnClickListener{
            val email = _binding.signUpScreen.etEmail.text.toString()
            val password = _binding.signUpScreen.etPassword.text.toString()
            val passwordConfirm = _binding.signUpScreen.etPasswordConfirm.text.toString()

            if(!emailValidation(email,_binding.signUpScreen.tilEmail,_binding.loginScreen.etEmail)){
                return@setOnClickListener
            }
            if(!passwordValidation(password,_binding.signUpScreen.tilPassword, _binding.loginScreen.etPassword)){
                return@setOnClickListener
            }

            if(passwordConfirm != password){
                _binding.signUpScreen.tilPasswordConfirm.error = getString(R.string.passwords_don_t_match)
                _binding.signUpScreen.etPasswordConfirm.requestFocus()
                return@setOnClickListener
            }


            registerUser(email,password)
        }
        _binding.signUpScreen.llSignIn.setOnClickListener{
            setScreenVisible(showMain = true)
        }

        _binding.loginScreen.etPassword.addTextChangedListener {
            _binding.loginScreen.tilPassword.error=""
        }
        _binding.loginScreen.etEmail.addTextChangedListener {
            _binding.loginScreen.tilEmail.error =""
        }

        _binding.signUpScreen.etPassword.addTextChangedListener {
            _binding.signUpScreen.tilPassword.error=""
        }
        _binding.signUpScreen.etPasswordConfirm.addTextChangedListener {
            _binding.signUpScreen.tilPasswordConfirm.error=""
        }
        _binding.signUpScreen.etEmail.addTextChangedListener {
            _binding.signUpScreen.tilEmail.error =""
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
        _binding.loginScreen.pb.visibility = if(loading) View.VISIBLE else View.GONE
        _binding.loginScreen.btnLogin.visibility = if(loading) View.INVISIBLE else View.VISIBLE
        _binding.loginScreen.btnLogin.isEnabled = !loading
    }
    private fun isLoadingSignUp(loading: Boolean){
        _binding.signUpScreen.pb.visibility = if(loading) View.VISIBLE else View.GONE
        _binding.signUpScreen.btnSignUp.visibility = if(loading) View.INVISIBLE else View.VISIBLE
        _binding.signUpScreen.btnSignUp.isEnabled = !loading
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

    private fun registerUser(email:String, password:String) {
        isLoadingSignUp(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    //val user = auth.currentUser
                    isLoadingSignUp(false)
                    showDialogSuccessfulRegistration()
                } else {
                    // Registration failed
                    val exception = task.exception
                    Snackbar.make(_binding.root,exception?.message?:getString(R.string.operation_failed_please_retry), Snackbar.LENGTH_SHORT).show()
                    isLoadingSignUp(false)
                }
            }
    }

    private fun login(email: String, password: String) {
        isLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful
                    // Navigate to main screen or handle success as needed
                    val user = task.result.user
                    updateUI(user!!)
                } else {
                    // Login failed
                    val exception = task.exception
                    // Handle login failure (e.g., display error message)
                    Snackbar.make(_binding.root,exception?.message?:getString(R.string.operation_failed_please_retry), Snackbar.LENGTH_SHORT).show()
                    isLoading(false)
                }
            }
    }

    private fun restorePassword(email:String){
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Password reset email sent successfully
                    // Inform user to check their email for instructions
                    showDialogEmailPasswordSend()
                } else {
                    // Email sending failed
                    val exception = task.exception
                    // Handle email sending failure (e.g., display error message to user)
                    Snackbar.make(_binding.root,exception?.message?:getString(R.string.operation_failed_please_retry), Snackbar.LENGTH_SHORT).show()
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



    private fun signInWithGoogle() {
        isLoading(true)
        val signInIntent = GoogleSignIn.getClient(this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(Constants.ANDROID_OAUTH_CLIENT_ID) // Replace with your web client ID
            .requestEmail()
            .build())
            .signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN) // Replace RC_SIGN_IN with a unique request code
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }else{
            isLoading(false)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account = task.result
            // Signed in successfully, proceed with user information
            val firebaseAuth = FirebaseAuth.getInstance()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { authResult ->
                    if (authResult.isSuccessful) {
                        val user = authResult.result?.user
                        updateUI(user!!)
                    } else {
                        // Handle sign in failure
                        Log.e(TAG, "firebaseAuth.signInWithCredential failed")
                        val message = authResult.exception?.message?:getString(R.string.operation_failed_please_retry)
                        Snackbar.make(_binding.root,
                            message,
                            Snackbar.LENGTH_LONG).show()
                        isLoading(false)
                    }
                }
        } else {
            // Handle sign in failure (e.g., display error message)
            Log.e(TAG, "Task<GoogleSignInAccount> failure ")
            val message = task.exception?.message?:getString(R.string.operation_failed_please_retry)
            Snackbar.make(_binding.root,
                message,
                Snackbar.LENGTH_LONG).show()
            isLoading(false)
        }
    }

    private fun showPasswordResetDialog(){
        val dialogBinding = DialogPasswordResetBinding.inflate(layoutInflater)
        val dialogBuilder = dialogManager.getMaterialDialogBuilder(dialogBinding.root)
        dialogBinding.etEmail.addTextChangedListener {
            dialogBinding.tilEmail.error =""
        }
        dialogBuilder.setNegativeButton(R.string.reset,null)

        dialogBuilder.setPositiveButton(R.string.cancel){_,_ ->
            //dialog.dismiss() SE CIERRA SOLO
        }
        val d = dialogBuilder.create()

        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        d.show()
        val btnReset = d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
        //Poner los listener aqui para evitar que se cierre automaticamente
        btnReset.setOnClickListener{
            val email = dialogBinding.etEmail.text.toString()
            if(StringUtils.isValidEmail(email)){
                d.dismiss()
                restorePassword(email=email)
            }else{
                dialogBinding.tilEmail.error = getString(R.string.invalid_email)
            }
        }

        dialogBinding.etEmail.requestFocus()

    }

    private fun showDialogEmailPasswordSend(){
        val dialogBinding = DialogImageTitleDescriptionBinding.inflate(layoutInflater)
        dialogBinding.img.setImageResource(R.drawable.email_password)
        dialogBinding.title.setText(R.string.check_your_inbox)
        dialogBinding.body.setText(getString(R.string.we_have_sent_you_an_email_with_further_instructions))
        dialogManager.showSingleButtonDialog(dialogBinding.root)
    }


    private fun setScreenVisible(showMain: Boolean){
        _binding.loginScreen.root.visibility = if(showMain) View.VISIBLE else View.GONE
        _binding.signUpScreen.root.visibility = if(!showMain) View.VISIBLE else View.GONE

        if(!showMain)
            _binding.signUpScreen.etEmail.requestFocus()


    }


    private fun emailValidation(email: String, til:TextInputLayout,et:TextInputEditText ):Boolean{
        if(!StringUtils.isValidEmail(email=email)){
            til.error = getString(R.string.invalid_email)
            et.requestFocus()
            return false
        }
        return true
    }

    private fun passwordValidation(password: String, til:TextInputLayout,et:EditText ):Boolean{
        if(!StringUtils.isValidPassword(password=password)){
            til.error = getString(R.string.must_be_at_least_characters_long,"6")
            et.requestFocus()
            return false
        }
        return true
    }

    private fun showDialogSuccessfulRegistration(){
        val dialogBinding = DialogImageTitleDescriptionBinding.inflate(layoutInflater)
        dialogBinding.img.setImageResource(R.drawable.fireworks)
        dialogBinding.title.text = getString(R.string.welcome_aboard)
        dialogBinding.body.text = getString(R.string.your_account_has_been_created)
        dialogManager.showSingleButtonDialog(dialogBinding.root){
            _binding.signUpScreen.etEmail.text?.clear()
            _binding.signUpScreen.etPassword.text?.clear()
            _binding.signUpScreen.etPasswordConfirm.text?.clear()
            setScreenVisible(showMain = true)
            _binding.loginScreen.etEmail.requestFocus()
        }
    }

}

