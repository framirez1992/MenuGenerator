package com.far.menugenerator.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImage
import com.far.menugenerator.R
import com.far.menugenerator.common.global.Constants
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.common.utils.NumberUtils
import com.far.menugenerator.common.utils.StringUtils
import com.far.menugenerator.databinding.ActivityMainBinding
import com.far.menugenerator.databinding.DialogCategoryBinding
import com.far.menugenerator.databinding.DialogImageTitleDescriptionBinding
import com.far.menugenerator.databinding.ItemMenuFinalPreviewBinding
import com.far.menugenerator.databinding.MenuNameDialogBinding
import com.far.menugenerator.databinding.MenuSettingsBinding
import com.far.menugenerator.model.Category
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.LogoShape
import com.far.menugenerator.model.MenuSettings
import com.far.menugenerator.model.MenuStyle
import com.far.menugenerator.viewModel.model.State
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
import com.far.menugenerator.view.adapters.CategoriesAdapter
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.adapters.MenuPreviewAdapter
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.CreateMenuViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


class MenuActivity : BaseActivity() {

    @Inject lateinit var navigation:ScreenNavigation

    private lateinit var _binding:ActivityMainBinding
    private lateinit var viewModel: CreateMenuViewModel

    @Inject lateinit var screenNavigation: ScreenNavigation
    @Inject lateinit var dialogManager: DialogManager
    @Inject lateinit var createMenuViewModelFactory: CreateMenuViewModel.CreateMenuViewModelFactory

    //private var menuReference:MenuReference?=null

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var adIsLoading: Boolean = false
    private var interstitialAd: InterstitialAd? = null
    private final val TAG = "MainActivity"

    companion object {
        const val ARG_COMPANY_ID = "companyId"
        const val ARG_MENU_TYPE = "menuType";
        const val ARG_MENU_ID="menuId"
        const val ARG_MENU_ONLINE="menuOnline"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this,createMenuViewModelFactory)[CreateMenuViewModel::class.java]

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        val menuType:String?
        val companyId:String?
        val menuId:String?
        val isOnline:Boolean?

        if(savedInstanceState == null){
            menuType = intent.getStringExtra(ARG_MENU_TYPE)
            companyId = intent.getStringExtra(ARG_COMPANY_ID)
            menuId = intent.getStringExtra(ARG_MENU_ID)
            isOnline = intent.getBooleanExtra(ARG_MENU_ONLINE,false)
        }else{
            menuType = savedInstanceState.getString(ARG_MENU_TYPE)
            companyId = savedInstanceState.getString(ARG_COMPANY_ID)
            menuId = savedInstanceState.getString(ARG_MENU_ID)
            isOnline = savedInstanceState.getBoolean(ARG_MENU_ONLINE,false)
        }
        
        if(LoginActivity.userFirebase == null || companyId.isNullOrEmpty() || menuType.isNullOrEmpty()){
            finish()
        }else {
            if(viewModel.companyId.isNullOrEmpty() || viewModel.menuId.isNullOrEmpty()){
                viewModel.initialize(
                    context = this,
                    userId = LoginActivity.userFirebase?.accountId!!,
                    companyId = companyId,
                    menuReferenceId = menuId,
                    menuType = menuType,
                    isOnlineMenu = isOnline)
            }
            
            hideActionBar()
            initializeMobileAdsSdk()

            initViews()
            initObservers()
            viewModel.prepareMenu()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_CODE_CROP_IMAGE && resultCode == Activity.RESULT_OK){
            val imageUri: CropImage.ActivityResult? = data?.extras?.getParcelable(CropImage.CROP_IMAGE_EXTRA_RESULT)
            viewModel.updateCurrentItemImage(imageUri?.uriContent)
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_COMPANY_ID,viewModel.companyId)
        outState.putString(ARG_MENU_ID,viewModel.menuReferenceId)
        outState.putBoolean(ARG_MENU_ONLINE, viewModel.isMenuOnline?:false)
        outState.putString(ARG_MENU_TYPE, viewModel.menuType?.name)
    }

    override fun onBackPressed() {
        //super.onBackPressed()
        dialogManager.showExitConfirmDialog(){
            finish()
        }
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

                override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    this@MenuActivity.interstitialAd = interstitialAd
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
            //loadAd()
            onFinish()

        }
    }


    private fun generateValidations(): Boolean {
        val enabledItems = viewModel.items.value?.filter { it.enabled }
        if(enabledItems == null ||  enabledItems.isEmpty()){
            val binding = DialogImageTitleDescriptionBinding.inflate(layoutInflater)
            binding.img.setImageResource(R.drawable.warning)
            binding.title.setText(R.string.warning)
            binding.body.setText(R.string.add_products_to_continue)
            dialogManager.showSingleButtonDialog(binding.root)
            return false
        }
        return true
    }

    private fun initViews(){

        _binding.btnNext.setOnClickListener { viewModel.nextScreen() }
        _binding.btnBack.setOnClickListener { viewModel.previousScreen() }
        _binding.btnOptions.setOnClickListener {
            val options = listOf(
                ImageOption(R.drawable.rounded_key_visualizer_24, R.string.menu_settings),
                ImageOption(R.drawable.baseline_picture_as_pdf_24,R.string.generate)
            )
            dialogManager.showImageBottomSheet(options){
                if(it.string == R.string.generate){
                    if(!generateValidations()){
                        return@showImageBottomSheet
                    }

                    showMenuNameDialog(viewModel.getCurrentMenuName())
                }else if(it.string == R.string.menu_settings){
                    showMenuSettingsDialog()
                }
            }
        }

        _binding.addMenuItemScreen.productData.imgProduct.setOnClickListener{
            showImageOptions()
        }

        _binding.categoriesScreen.btnAdd.setOnClickListener{
            showCategoryDialog(null)
        }

        _binding.addMenuItemScreen.productData.btnAdd.setOnClickListener {
            if(!validateProductData()) return@setOnClickListener
            viewModel.addProduct(
                _binding.addMenuItemScreen.productData.enabled.isChecked,
                _binding.addMenuItemScreen.productData.spnCategories.selectedItem as Category,
                _binding.addMenuItemScreen.productData.etProductName.text.toString(),
                _binding.addMenuItemScreen.productData.etProductDescription.text.toString(),
                _binding.addMenuItemScreen.productData.etProductPrice.text.toString().toDouble()
            )
            clearAddProductFields()

            Toast.makeText(this, getString(R.string.product_added),Toast.LENGTH_SHORT).show()
        }

        _binding.addMenuItemScreen.productData.btnEdit.setOnClickListener{
            viewModel.saveEditItemChanges(
                enabled = _binding.addMenuItemScreen.productData.enabled.isChecked,
                category= _binding.addMenuItemScreen.productData.spnCategories.selectedItem as Category,
                name=_binding.addMenuItemScreen.productData.etProductName.text.toString(),
                description= _binding.addMenuItemScreen.productData.etProductDescription.text.toString(),
                price= _binding.addMenuItemScreen.productData.etProductPrice.text.toString().toDouble()
            )
            viewModel.setScreen(R.id.menuPreviewScreen)
            clearAddProductFields()
            Toast.makeText(this, getString(R.string.product_edited),Toast.LENGTH_SHORT).show()
        }
        _binding.addMenuItemScreen.productData.btnCancel.setOnClickListener{
            viewModel.editItem(null)
            clearAddProductFields()
            viewModel.setScreen(R.id.menuPreviewScreen)

            Toast.makeText(this, getString(R.string.cancelled),Toast.LENGTH_SHORT).show()
        }
        _binding.addMenuItemScreen.productData.enabled.setOnCheckedChangeListener { buttonView, isChecked ->
            _binding.addMenuItemScreen.productData.imgVisible.setImageResource(
                if (isChecked) R.drawable.baseline_remove_red_eye_24 else R.drawable.baseline_visibility_off_24
            )
        }


        _binding.categoriesScreen.rvCategories.layoutManager = LinearLayoutManager(this,
            RecyclerView.VERTICAL,false)
        _binding.menuPreviewScreen.rvPreview.layoutManager = LinearLayoutManager(this,
            RecyclerView.VERTICAL,false)



    }

    private fun initObservers(){
        viewModel.state.observe(this){
            navigate(it.currentScreen)

        }
        viewModel.editItem.observe(this){

            if(it != null){
                _binding.btnNext.visibility = View.GONE
                _binding.btnBack.visibility = View.GONE
            }

            _binding.addMenuItemScreen.productData.btnAdd.visibility = if(it != null) View.GONE else View.VISIBLE
            _binding.addMenuItemScreen.productData.btnEdit.visibility = if(it == null) View.GONE else View.VISIBLE
            _binding.addMenuItemScreen.productData.btnCancel.visibility = if(it == null) View.GONE else View.VISIBLE
            _binding.addMenuItemScreen.title.text = resources.getString(if(it!=null) R.string.edit_product else R.string.products)

            if (it != null) {
                fillEditProductData(it)
            }
        }

        viewModel.getMenuSettings().observe(this){
            if(viewModel.state.value?.currentScreen == R.id.menuPreviewFinalScreen){
                fillCompany(it)
                //VOLVER A ACTIVAR CUANDO SE EFECTUEN CAMBIOS SOBRE LOS ITEMS (V2)
               // val preview = (_binding.menuPreviewScreen.rvPreview.adapter as MenuPreviewAdapter).currentPreview
               // viewModel.loadFinalPreview(
               //     parent = _binding.menuPreviewFinalScreen.llItems,
               //     orderedPreview = preview)
                //fillFinalPreview(it, preview)
            }
        }


        viewModel.categories.observe(this){
            setCategoryAdapter(it)
            fillSpinnerCategoriesAddProductScreen(it)
        }
        viewModel.itemsPreview.observe(this){
            fillPreviewAdapter(it)
        }
        viewModel.currentItemImage.observe(this){
            if(it != null){
                Glide.with(this)
                    .load(it)
                    //.error(R.drawable.baseline_broken_image_24)
                    .into(_binding.addMenuItemScreen.productData.imgProduct)
            }else{
                _binding.addMenuItemScreen.productData.imgProduct.setImageDrawable(null)
            }

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
                    Snackbar.make(_binding.root,getText(R.string.operation_failed_please_retry),
                        Snackbar.LENGTH_LONG).show()
                }else if(it.state == State.NETWORK_ERROR){
                    dialogManager.showInternetErrorDialog()
                }

            }

        }

        viewModel.stateLoadingFinalPreviewItems.observe(this){
            if(it.state == State.LOADING){
                _binding.menuPreviewFinalScreen.llItems.visibility = View.INVISIBLE
                dialogManager.showLoadingDialog()
            }else{
                fillFinalPreviewItems(viewModel.orderedFinalPreviewItems)
                _binding.menuPreviewFinalScreen.llItems.visibility = View.VISIBLE
                dialogManager.dismissLoadingDialog()
            }
        }
    }



    private fun showImageOptions() {
        val options = listOf(
            ImageOption(R.drawable.baseline_image_search_24,R.string.search),
            ImageOption(R.drawable.baseline_clear_24,R.string.clear)
        )
        dialogManager.showImageBottomSheet(options){
            if(it.string == R.string.search){
                this.callCropImage()
            }else if(it.string == R.string.clear){
                viewModel.updateCurrentItemImage(null)
            }
        }
    }

    private fun showCategoryDialog(category: Category?) {
        val dialogBinding = DialogCategoryBinding.inflate(layoutInflater)
        val dialogBuilder = dialogManager.getMaterialDialogBuilder(dialogBinding.root)
        dialogBuilder.setPositiveButton(
            if(category == null) R.string.add else R.string.edit,
            null)

        dialogBuilder.setNegativeButton(R.string.cancel){dialog,_ ->
            //dialog.dismiss() SE CIERRA SOLO
        }
        val d = dialogBuilder.create()

        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        d.show()
        d.setCancelable(false)
        val positive = d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
        //Poner los listener aqui para evitar que se cierre automaticamente
        if(category == null){

            positive.setOnClickListener{
                val c = dialogBinding.etCategoryName.text.toString()
                if(categoryTextValidation(name = c, categoryId = null, textInputLayout =  dialogBinding.tilCategoryName)){
                    viewModel.addCategory(c)
                    d.dismiss()
                }
            }
        }else{

            positive.setOnClickListener{
                val c = dialogBinding.etCategoryName.text.toString()
                if(categoryTextValidation(name = c, categoryId = category.id, textInputLayout =  dialogBinding.tilCategoryName)){
                    category.name = c
                    viewModel.saveEditCategory(category)
                    d.dismiss()
                }
            }


            dialogBinding.etCategoryName.setText(category.name)
            dialogBinding.etCategoryName.selectAll()
        }

        dialogBinding.etCategoryName.requestFocus()

    }

    private fun showMenuNameDialog(menuName:String?) {
        LayoutInflater.from(this)
        val dialogBinding = MenuNameDialogBinding.inflate(LayoutInflater.from(this))
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


    private fun generateMenu(fileName:String){
        val file = File(this.applicationContext.filesDir, Constants.PDF_FILE_NAME)
        val height = _binding.menuPreviewFinalScreen.llCompany.measuredHeight + calculateTotalItemHeight(_binding.menuPreviewFinalScreen.llItems) + _binding.menuPreviewFinalScreen.llFooter.measuredHeight
        viewModel.generateMenu(
            context = this,
            referenceName = fileName,
            view = _binding.menuPreviewFinalScreen.root,
            fileHeight = height,
            pdfFile = file
        )
    }



    private data class Shape(val shape: LogoShape, val name:String){
        override fun toString(): String {
            return name
        }
    }
    private fun showMenuSettingsDialog() {
        val dialogBinding = MenuSettingsBinding.inflate(layoutInflater)
        val dialogBuilder = dialogManager.getMaterialDialogBuilder(dialogBinding.root)

        dialogBuilder.setPositiveButton(getString(R.string.apply)){dialog,_->
            val menuSettings = MenuSettings(
                logoShape = (dialogBinding.spnShape.selectedItem as Shape).shape,
                showLogo = dialogBinding.cbLogo.isChecked,
                showBusinessName = dialogBinding.cbBusinessName.isChecked,
                showAddress1 = dialogBinding.cbAddress1.isChecked,
                showAddress2 = dialogBinding.cbAddress2.isChecked,
                showAddress3 = dialogBinding.cbAddress3.isChecked,
                showPhone1 = dialogBinding.cbPhone1.isChecked,
                showPhone2 = dialogBinding.cbPhone2.isChecked,
                showPhone3 = dialogBinding.cbPhone3.isChecked,
                showFacebook = dialogBinding.cbFacebook.isChecked,
                showInstagram = dialogBinding.cbInstagram.isChecked,
                showWhatsapp =  dialogBinding.cbWhatsapp.isChecked,
                menuStyle = if(dialogBinding.spnStyle.selectedItem as String == getString(R.string.basic)) MenuStyle.BASIC else MenuStyle.CATALOG
            )
            viewModel.updateMenuSettings(menuSettings)
        }
        dialogBuilder.setNegativeButton(R.string.cancel){dialog,_ ->
            //dialog.dismiss() SE CIERRA SOLO
        }
        val d = dialogBuilder.create()

        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        d.show()

        val menuSettings = viewModel.getMenuSettings().value!!
        val menuStyles = listOf(
            getString(R.string.basic),
            getString(R.string.catalog)
        )
        val logoStyles = listOf(
            Shape(LogoShape.NONE,getString(R.string.none)),
            Shape(LogoShape.CIRCULAR,getString(R.string.rounded)),
            Shape(LogoShape.ROUNDED_CORNERS, getString(R.string.rounded_square))
        )


        dialogBinding.apply {
            spnShape.adapter = ArrayAdapter(this@MenuActivity,android.R.layout.simple_list_item_1,logoStyles)
            cbLogo.isChecked = menuSettings.showLogo
            cbBusinessName.isChecked = menuSettings.showBusinessName
            cbAddress1.isChecked = menuSettings.showAddress1
            cbAddress2.isChecked = menuSettings.showAddress2
            cbAddress3.isChecked = menuSettings.showAddress3
            cbPhone1.isChecked = menuSettings.showPhone1
            cbPhone2.isChecked = menuSettings.showPhone2
            cbPhone3.isChecked = menuSettings.showPhone3
            cbFacebook.isChecked = menuSettings.showFacebook
            cbInstagram.isChecked = menuSettings.showInstagram
            cbWhatsapp.isChecked = menuSettings.showWhatsapp
            spnStyle.adapter = ArrayAdapter<String>(this@MenuActivity,android.R.layout.simple_list_item_1,menuStyles)
            spnStyle.post {
                spnStyle.setSelection(if(menuSettings.menuStyle == MenuStyle.BASIC)0 else 1)
                spnShape.setSelection(logoStyles.indexOf(logoStyles.first{it.shape == menuSettings.logoShape}))
            }

        }



    }







    private fun categoryTextValidation(name: String,categoryId:String? = null,textInputLayout: TextInputLayout):Boolean{
        if(name.isBlank()){
            textInputLayout.error = getString(R.string.invalid_value)
            return false
        }
        if(categoryId == null &&  viewModel.getCategoriesByName(name).isNotEmpty()){//NUEVOS
            textInputLayout.error = getString(R.string.name_in_use)
            return false
        }
        if(categoryId != null &&  viewModel.getCategoriesByName(name).filter { it.id != categoryId }.isNotEmpty()){
            textInputLayout.error = getString(R.string.name_in_use)
            return false
        }
        return true
    }
    private fun menuNameTextValidation(menuName: String, textInputLayout: TextInputLayout):Boolean{
        if(menuName.isBlank()){
            textInputLayout.error = getString(R.string.invalid_value)
            return false
        }
        return true
    }



    private fun fillSpinnerCategoriesAddProductScreen(categories: MutableList<Category>){
        var adapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,categories)
        _binding.addMenuItemScreen.productData.spnCategories.adapter = adapter
    }


    private fun navigate(currentView:Int){
        _binding.btnBack.visibility = if(currentView == R.id.categoriesScreen) View.GONE else View.VISIBLE
        _binding.btnNext.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.GONE else View.VISIBLE
        _binding.btnOptions.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.VISIBLE else View.GONE

        _binding.categoriesScreen.root.visibility= if(currentView == R.id.categoriesScreen) View.VISIBLE else View.GONE
        _binding.addMenuItemScreen.root.visibility= if(currentView == R.id.addMenuItemScreen) View.VISIBLE else View.GONE
        _binding.menuPreviewScreen.root.visibility = if(currentView == R.id.menuPreviewScreen) View.VISIBLE else View.GONE
        _binding.menuPreviewFinalScreen.root.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.VISIBLE else View.GONE

        if(currentView == R.id.menuPreviewFinalScreen){
            val menuSettings = viewModel.getMenuSettings().value!!
            fillCompany(menuSettings)
            val preview = (_binding.menuPreviewScreen.rvPreview.adapter as MenuPreviewAdapter).currentPreview
            viewModel.loadFinalPreview(
                parent = _binding.menuPreviewFinalScreen.llItems,
                orderedPreview = preview)
            //fillFinalPreview(menuSettings,(_binding.menuPreviewScreen.rvPreview.adapter as MenuPreviewAdapter).currentPreview)
        }
        if(currentView == R.id.menuPreviewScreen && _binding.menuPreviewScreen.root.windowToken!=null){
            this.hideKeyboard(_binding.menuPreviewScreen.root.windowToken)
        }

    }


    private fun fillPreviewAdapter(itemPreview: ItemPreview){
        _binding.menuPreviewScreen.rvPreview.adapter = MenuPreviewAdapter(
            this,
            itemPreview.menuItemsTemp,
            onPositionChanged = {
                viewModel.updatePositions(it)
            }){item->
            val options = listOf(
                ImageOption(R.drawable.round_edit_24,R.string.edit),
                ImageOption(R.drawable.rounded_delete_24,R.string.delete),
            )
            dialogManager.showImageBottomSheet(options = options){ option->
                if(option.string == R.string.edit){
                    viewModel.editItem(item)
                    viewModel.setScreen(R.id.addMenuItemScreen)
                }else if(option.string == R.string.delete){
                    viewModel.deleteItem(item)
                }
            }

        }

        val position =(_binding.menuPreviewScreen.rvPreview.adapter as MenuPreviewAdapter).getItemPosition(itemId = itemPreview.scrollToItemId)
        _binding.menuPreviewScreen.rvPreview.scrollToPosition(position)
    }


    private fun setCategoryAdapter(categories:MutableList<Category>){
        val categoriesFiltered = mutableListOf<Category>()
        categoriesFiltered.addAll(categories.filter { it.id != CreateMenuViewModel.noCategoryId })

        val adapter = CategoriesAdapter(categoriesFiltered){ category->

            val options = listOf(
                ImageOption(R.drawable.round_edit_24,R.string.edit),
                ImageOption(R.drawable.rounded_delete_24,R.string.delete),
            )
            dialogManager.showImageBottomSheet(options = options){ option->
                if(option.string == R.string.edit){
                    showCategoryDialog(category)
                }else if(option.string == R.string.delete){
                    viewModel.removeCategory(category)
                }
            }
        }
        _binding.categoriesScreen.rvCategories.adapter = adapter
    }

    private fun validateProductData():Boolean{
        if(_binding.addMenuItemScreen.productData.etProductName.text.isNullOrBlank()){
            _binding.addMenuItemScreen.productData.etProductName.error = resources.getString(R.string.required)
            _binding.addMenuItemScreen.productData.etProductName.requestFocus()

            return false
        }
        if(_binding.addMenuItemScreen.productData.etProductPrice.text.isNullOrBlank()){
            _binding.addMenuItemScreen.productData.etProductPrice.error = resources.getString(R.string.required)
            _binding.addMenuItemScreen.productData.etProductPrice.requestFocus()
            return false
        }
        if(NumberUtils.stringIsNumber(_binding.addMenuItemScreen.productData.etProductPrice.text.toString())){
            _binding.addMenuItemScreen.productData.etProductPrice.error = resources.getString(R.string.invalid_value)
            _binding.addMenuItemScreen.productData.etProductPrice.requestFocus()
            return false
        }
        return true
    }

    private fun fillEditProductData(menuItem: MenuItemsTemp){
        val imageUri = menuItem.imageUri
        viewModel.updateCurrentItemImage(if(imageUri != null) Uri.parse(imageUri) else null)

        _binding.addMenuItemScreen.productData.enabled.isChecked = menuItem.enabled
        _binding.addMenuItemScreen.productData.etProductName.setText(menuItem.name)
        _binding.addMenuItemScreen.productData.etProductPrice.setText(menuItem.price.toString())
        _binding.addMenuItemScreen.productData.etProductDescription.setText(menuItem.description)

        val category = viewModel.categories.value?.first { it.id ==  menuItem.categoryId}
        val position = viewModel.categories.value?.lastIndexOf(category)
        _binding.addMenuItemScreen.productData.spnCategories.setSelection(position!!)
    }



    private fun clearAddProductFields(){
        _binding.addMenuItemScreen.productData.enabled.isChecked = true
        _binding.addMenuItemScreen.productData.etProductName.text?.clear()
        _binding.addMenuItemScreen.productData.etProductPrice.text?.clear()
        _binding.addMenuItemScreen.productData.etProductDescription.text?.clear()
        clearAddProductImage()
        _binding.addMenuItemScreen.productData.etProductName.requestFocus()

    }


    private fun clearAddProductImage(){
        viewModel.updateCurrentItemImage(null)
    }



    private fun calculateTotalItemHeight(viewGroup: ViewGroup): Int {
        var totalItemHeight = 0
        for (i in 0 until viewGroup.childCount) {
            val view = viewGroup.getChildAt(i)
            totalItemHeight += measureItemHeight(view)
        }
        return totalItemHeight
    }


    private fun measureItemHeight(view: View): Int {
        return view.measuredHeight
    }





/*
    private fun fillFinalPreview(menuSettings: MenuSettings, items: List<MenuItemsTemp>){
       lifecycleScope.launch(Dispatchers.Main) {
           //draw.postValue(true)
           _binding.menuPreviewFinalScreen.llItems.removeAllViews()

            items.filter{it.enabled}.forEach{ menuItem ->

                if(menuItem.type == ItemStyle.MENU_CATEGORY_HEADER.name //Si es una categoria y esta categoria no tiene items visibles(NO MOSTRAR)
                    && items.none { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.categoryId == menuItem.id
                            && it.enabled }) return@forEach


                val binding = ItemMenuFinalPreviewBinding.inflate(LayoutInflater.from(this@MenuActivity),_binding.menuPreviewFinalScreen.llItems,false)

                binding.imageTitleDescription.root.visibility = if(menuSettings.menuStyle == MenuStyle.BASIC && menuItem.type == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE.name) View.VISIBLE else View.GONE
                binding.imageTitleDescriptionCatalog.root.visibility = if(menuSettings.menuStyle == MenuStyle.CATALOG && menuItem.type == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE.name) View.VISIBLE else View.GONE
                binding.titleDescription.root.visibility = if(menuItem.type == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE.name) View.VISIBLE else View.GONE
                binding.titlePrice.root.visibility = if(menuItem.type == ItemStyle.MENU_TITLE_PRICE.name) View.VISIBLE else View.GONE
                binding.categoryTitle.root.visibility = if (menuItem.type == ItemStyle.MENU_CATEGORY_HEADER.name) View.VISIBLE else View.GONE


                //Don`t show NO_CATEGORY ITEM
                if(menuItem.type == ItemStyle.MENU_CATEGORY_HEADER.name &&  menuItem.id == CreateMenuViewModel.noCategoryId){
                    binding.root.visibility = View.GONE
                }

                when (menuItem.type) {
                    ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE.name -> {

                        if(menuSettings.menuStyle == MenuStyle.BASIC) {
                            binding.imageTitleDescription.title.text = menuItem.name
                            binding.imageTitleDescription.body.text = menuItem.description
                            binding.imageTitleDescription.price.text = StringUtils.doubleToMoneyString(
                                amount = menuItem.price,
                                country = "US",
                                language = "en"
                            )
                            Glide.with(this@MenuActivity)
                                .load(menuItem.imageUri)
                                //.error(R.drawable.baseline_broken_image_24)
                                .into(binding.imageTitleDescription.image)
                        }else if(menuSettings.menuStyle == MenuStyle.CATALOG){
                            binding.imageTitleDescriptionCatalog.title.text = menuItem.name
                            binding.imageTitleDescriptionCatalog.body.text = menuItem.description
                            binding.imageTitleDescriptionCatalog.price.text = StringUtils.doubleToMoneyString(amount = menuItem.price, country = "US", language = "en")
                            Glide.with(this@MenuActivity)
                                .load(menuItem.imageUri)
                                //.error(R.drawable.baseline_broken_image_24)
                                .into(binding.imageTitleDescriptionCatalog.image)
                        }

                    }
                    ItemStyle.MENU_TITLE_DESCRIPTION_PRICE.name -> {
                        binding.titleDescription.title.text = menuItem.name
                        binding.titleDescription.body.text = menuItem.description
                        binding.titleDescription.price.text = StringUtils.doubleToMoneyString(amount = menuItem.price, country = "US", language = "en")
                    }
                    ItemStyle.MENU_TITLE_PRICE.name -> {
                        binding.titlePrice.title.text = menuItem.name
                        binding.titlePrice.price.text = StringUtils.doubleToMoneyString(amount = menuItem.price, country = "US", language = "en")
                    }
                    else -> {
                        binding.categoryTitle.title.text = menuItem.name
                    }
                }




                // Get the layout parameters of the view.
                val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
                if(menuItem.type == ItemStyle.MENU_CATEGORY_HEADER.name)
                    layoutParams.setMargins(0, 20, 0, 0)
                else
                    layoutParams.setMargins(0, 0, 0, 0)

                binding.root.layoutParams = layoutParams

                _binding.menuPreviewFinalScreen.llItems.addView(binding.root)
                // y no tiene items visibles IGNORAR
            }

            _binding.menuPreviewFinalScreen.llItems.invalidate()
           //draw.postValue(false)

        }

    }*/

    private fun fillFinalPreviewItems(orderedFinalPreviewItems: MutableList<Pair<String?,ItemMenuFinalPreviewBinding>>) {
        _binding.menuPreviewFinalScreen.llItems.removeAllViews()
        orderedFinalPreviewItems.forEach {
            if(!it.first.isNullOrEmpty()){
                Glide.with(this)
                    .load(it.first)
                    //.error(R.drawable.baseline_broken_image_24)
                    .into(it.second.imageTitleDescription.image)
            }
            _binding.menuPreviewFinalScreen.llItems.addView(it.second.root)
        }
        _binding.menuPreviewFinalScreen.llItems.invalidate()
    }

    private fun fillCompany(menuSettings: MenuSettings){
        val company = viewModel.getCompany()!!

        if(company.logoUrl.isNullOrEmpty() || !menuSettings.showLogo){
            _binding.menuPreviewFinalScreen.logo.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.logo.visibility = View.VISIBLE
            Glide.with(this)
                .load(company.logoUrl)
                //.error(R.drawable.baseline_broken_image_24)
                .into(_binding.menuPreviewFinalScreen.logo)

            val logoSize = resources.getDimension(R.dimen.img_logo_size)
            var shape: ShapeAppearanceModel = when(menuSettings.logoShape){
                LogoShape.CIRCULAR ->{
                    _binding.menuPreviewFinalScreen.logo.shapeAppearanceModel.toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED,  (logoSize * 0.50f)) // Set corner radius
                        .build()
                }
                LogoShape.ROUNDED_CORNERS->{
                    _binding.menuPreviewFinalScreen.logo.shapeAppearanceModel.toBuilder()
                        .setAllCorners(CornerFamily.ROUNDED, (logoSize * 0.1).toFloat())
                        .build()
                }else->{
                    ShapeAppearanceModel()
                }

            }
            _binding.menuPreviewFinalScreen.logo.shapeAppearanceModel = shape
        }

        if(company.businessName.isNullOrBlank() || !menuSettings.showBusinessName){
            _binding.menuPreviewFinalScreen.tvCompanyName.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvCompanyName.text= company.businessName
            _binding.menuPreviewFinalScreen.tvCompanyName.visibility = View.VISIBLE
        }


        if(company.phone1.isNullOrBlank() || !menuSettings.showPhone1){
            _binding.menuPreviewFinalScreen.tvPhone1.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone1.text= StringUtils.formatPhone(company.phone1)
            _binding.menuPreviewFinalScreen.tvPhone1.visibility = View.VISIBLE
        }


        if(company.phone2.isNullOrBlank() || !menuSettings.showPhone2){
            _binding.menuPreviewFinalScreen.tvPhone2.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone2.text= StringUtils.formatPhone(company.phone2)
            _binding.menuPreviewFinalScreen.tvPhone2.visibility = View.VISIBLE
        }

        if(company.phone3.isNullOrBlank() || !menuSettings.showPhone3){
            _binding.menuPreviewFinalScreen.tvPhone3.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone3.text= StringUtils.formatPhone(company.phone3)
            _binding.menuPreviewFinalScreen.tvPhone3.visibility = View.VISIBLE
        }


        if(company.address1.isNullOrBlank() || !menuSettings.showAddress1){
            _binding.menuPreviewFinalScreen.tvAddress1.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress1.text= company.address1
            _binding.menuPreviewFinalScreen.tvAddress1.visibility = View.VISIBLE
        }

        if(company.address2.isNullOrBlank() || !menuSettings.showAddress2){
            _binding.menuPreviewFinalScreen.tvAddress2.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress2.text= company.address2
            _binding.menuPreviewFinalScreen.tvAddress2.visibility = View.VISIBLE
        }

        if(company.address3.isNullOrBlank() || !menuSettings.showAddress3){
            _binding.menuPreviewFinalScreen.tvAddress3.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress3.text= company.address3
            _binding.menuPreviewFinalScreen.tvAddress3.visibility = View.VISIBLE
        }


        if(company.facebook.isNullOrBlank() || !menuSettings.showFacebook){
            _binding.menuPreviewFinalScreen.llFacebook.visibility = View.GONE

        }else{
            _binding.menuPreviewFinalScreen.llFacebook.visibility = View.VISIBLE
            _binding.menuPreviewFinalScreen.tvFacebook.text= company.facebook
        }

        if(company.instagram.isNullOrBlank() || !menuSettings.showInstagram){
            _binding.menuPreviewFinalScreen.llInstagram.visibility = View.GONE

        }else{
            _binding.menuPreviewFinalScreen.llInstagram.visibility = View.VISIBLE
            _binding.menuPreviewFinalScreen.tvInstagram.text= company.instagram
        }

        if(company.whatsapp.isNullOrBlank() || !menuSettings.showWhatsapp){
            _binding.menuPreviewFinalScreen.llWhatsapp.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.llWhatsapp.visibility = View.VISIBLE
            val ws = StringUtils.formatPhone(company.whatsapp)
            _binding.menuPreviewFinalScreen.tvWhatsapp.text= ws
        }
    }

    
}