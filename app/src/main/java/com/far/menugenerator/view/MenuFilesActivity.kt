package com.far.menugenerator.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.far.menugenerator.R
import com.far.menugenerator.common.global.Constants
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.databinding.ActivityMenuFilesBinding
import com.far.menugenerator.databinding.DialogMenuNameBinding
import com.far.menugenerator.databinding.DialogSeachFilePermissionBinding
import com.far.menugenerator.viewModel.model.State
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.viewModel.MenuFilesViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class MenuFilesActivity : BaseActivity() {

    private val REQUEST_FILE_SELECTION = 100

    @Inject lateinit var dialogManager:DialogManager
    @Inject lateinit var factory:MenuFilesViewModel.MenuFilesViewModelFactory

    private lateinit var viewModel:MenuFilesViewModel
    private lateinit var binding:ActivityMenuFilesBinding
    private lateinit var requestPermissionLauncher:ActivityResultLauncher<String>


    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var adIsLoading: Boolean = false
    private var interstitialAd: InterstitialAd? = null
    private val TAG = "MenuFilesActivity"

    companion object {
        const val ARG_COMPANY_REF = "companyRef"
        const val ARG_MENU_TYPE = "menuType";
        const val ARG_MENU_ID="menuId"
        const val ARG_MENU_ONLINE="menuOnline"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this,factory)[MenuFilesViewModel::class.java]

        binding = ActivityMenuFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val menuType:String?
        val companyRef:String?
        val menuId:String?
        val isOnline:Boolean?
        val menuFirebaseRef:String?
        if(savedInstanceState == null){
            menuType = intent.getStringExtra(ARG_MENU_TYPE)
            companyRef = intent.getStringExtra(ARG_COMPANY_REF)
            menuId = intent.getStringExtra(ARG_MENU_ID)
            isOnline = intent.getBooleanExtra(ARG_MENU_ONLINE,false)
        }else{
            menuType = savedInstanceState.getString(ARG_MENU_TYPE)
            companyRef = savedInstanceState.getString(ARG_COMPANY_REF)
            menuId = savedInstanceState.getString(ARG_MENU_ID)
            isOnline = savedInstanceState.getBoolean(ARG_MENU_ONLINE,false)
        }

        if(LoginActivity.userFirebase == null || companyRef.isNullOrEmpty() || menuType.isNullOrEmpty()){
            finish()
        }else {
            if (viewModel.companyId.isNullOrEmpty() || viewModel.menuId.isNullOrEmpty()) {
                viewModel.initialize(
                    userId = LoginActivity.userFirebase?.accountId!!,
                    companyId = companyRef,
                    menuReferenceId = menuId,
                    menuType = menuType,
                    isOnlineMenu = isOnline
                )
            }
        } 
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher. You can use either a val, as shown in this snippet,
        // or a lateinit var in your onAttach() or onCreate() method.
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    readExternalStorage()
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    showInContextUI();
                }
            }

        initializeMobileAdsSdk()
        initViews()
        initObservers()

        viewModel.prepareMenu()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(MenuActivity.ARG_COMPANY_ID,viewModel.companyId)
        outState.putString(MenuActivity.ARG_MENU_ID,viewModel.menuReferenceId)
        outState.putBoolean(MenuActivity.ARG_MENU_ONLINE, viewModel.isMenuOnline?:false)
        outState.putString(MenuActivity.ARG_MENU_TYPE, viewModel.menuType?.name)
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        dialogManager.showExitConfirmDialog(){
            finish()
        }
    }

    private fun initViews(){
        binding.imgFile.setOnClickListener{
            requestPermission()
        }
        binding.btnFinish.setOnClickListener{
            showMenuNameDialog(viewModel.getCurrentMenuName())
        }
        prepareView(viewModel.selectedFile.value)
    }

    private fun initObservers(){
        viewModel.selectedFile.observe(this){
           prepareView(it)
        }
        viewModel.stateProcessMenu.observe(this){
            if (it.state == State.LOADING){
                dialogManager.showLoadingDialog()
            }else{
                //FINISH PROCESS
                //////////////////////////////////////////////////
                //clear all files and finish
                FileUtils.deleteAllFilesInFolder(this.applicationContext.filesDir)
                ///////////////////////////////////////////////////
                dialogManager.dismissLoadingDialog()
                if(it.state == State.SUCCESS){
                    this.finish()
                }else if(it.state == State.GENERAL_ERROR){
                    Snackbar.make(binding.root,getText(R.string.operation_failed_please_retry),
                        Snackbar.LENGTH_LONG).show()
                }else if(it.state == State.NETWORK_ERROR){
                    dialogManager.showInternetErrorDialog()
                }

            }

        }
    }

    private fun prepareView(uri: Uri?){
        if(uri == null){
            binding.btnFinish.isEnabled = false
            binding.imgFile.setImageResource(R.drawable.search)
            binding.tvFileName.text = ""
        }else{
            binding.btnFinish.isEnabled = true
            val name = FileUtils.getRealFileName(contentResolver,uri)
            binding.imgFile.setImageResource(if(name.contains(".pdf")) R.drawable.pdf else R.drawable.image)
            binding.tvFileName.text = name

            if(!binding.tvFileName.text.contains(".pdf"))
                Glide.with(this)
                    .load(uri)
                    .into(binding.imgInvisible)
        }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_FILE_SELECTION && resultCode == RESULT_OK && data != null) {
            val uri = data.data
            viewModel.setSelectedFile(uri!!)
            // Now you have the URI of the selected file
            // You can use this URI to read the file content, etc.


            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //    contentResolver.takePersistableUriPermission(uri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            //}
        }
    }
    private fun requestPermission(){
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                readExternalStorage()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                showInContextUI()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                askForPermission()
            }
        }
    }

    private fun askForPermission(){
        requestPermissionLauncher.launch(
            Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun showInContextUI() {
        val binding = DialogSeachFilePermissionBinding.inflate(layoutInflater)
        dialogManager.showTwoButtonsDialog(
            binding.root,
            R.string.allow,
            {
                askForPermission()
            },
            R.string.cancel)

    }


    private fun readExternalStorage(){
        val mimeTypes = arrayOf("application/pdf", "image/*")

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }

        val requestCode = REQUEST_FILE_SELECTION // Choose any unique request code
        startActivityForResult(intent, requestCode)
    }


    private fun showMenuNameDialog(menuName:String?) {
        LayoutInflater.from(this)
        val dialogBinding = DialogMenuNameBinding.inflate(LayoutInflater.from(this))
        val dialogBuilder = dialogManager.getMaterialDialogBuilder(dialogBinding.root)
        dialogBuilder.setPositiveButton(R.string.finish,null)

        if(menuName != null){
            dialogBinding.etMenuName.setText(menuName)
            dialogBinding.etMenuName.selectAll()
        }


        dialogBuilder.setNegativeButton(R.string.cancel){dialog,_ ->
            //dialog.dismiss() SE CIERRA SOLO
        }
        val d = dialogBuilder.create()

        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        d.show()
        val positive = d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
        //Poner los listener aqui para evitar que se cierre automaticamente
        positive.setOnClickListener{
            val menuName = dialogBinding.etMenuName.text.toString()
            if(menuNameTextValidation(menuName,dialogBinding.tilMenuName)){
                d.dismiss()
                showInterstitial {
                    generateMenu(menuName)
                }


            }
        }

        dialogBinding.etMenuName.requestFocus()

    }

    private fun menuNameTextValidation(menuName: String, textInputLayout: TextInputLayout):Boolean{
        if(menuName.isBlank()){
            textInputLayout.error = getString(R.string.invalid_value)
            return false
        }
        return true
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) { initializationStatus ->
            // Load an ad.
            loadAd()
        }
    }

    private fun loadAd() {
        // Request a new ad if one isn't already loaded.
        if (adIsLoading || interstitialAd != null) {
            return
        }
        adIsLoading = true
        val adRequest = AdManagerAdRequest.Builder().build()

        AdManagerInterstitialAd.load(
            this,
            Constants.INTERSTITIAL_AD_ID,
            adRequest,
            object : AdManagerInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    interstitialAd = null
                    adIsLoading = false
                }

                override fun onAdLoaded(ia: AdManagerInterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    interstitialAd = ia
                    adIsLoading = false
                }
            }
        )
    }


    private fun showInterstitial(onFinish:()->Unit) {
        // Show the ad if it's ready. Otherwise restart the game.
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        Log.d(TAG, "Ad was dismissed.")
                        onFinish()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        interstitialAd = null
                        Log.d(TAG, "Ad failed to show.")
                        onFinish()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                    }
                }

            interstitialAd?.show(this)
        } else {
            onFinish()
        }
    }


    private fun generateMenu(fileName:String){
        val file = File(this.applicationContext.filesDir, Constants.PDF_FILE_NAME)
        if(binding.tvFileName.text.contains(".pdf")){
            viewModel.generateMenu(
                context = this,
                referenceName = fileName,
                menuFile = file
            )
        }else{
            val height = binding.imgInvisible.measuredHeight
            viewModel.generateMenu(
                context = this,
                referenceName = fileName,
                view = binding.imgInvisible,
                fileHeight = height,
                menuFile = file)
        }



    }
}