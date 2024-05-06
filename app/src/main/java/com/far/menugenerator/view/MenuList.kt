package com.far.menugenerator.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.R
import com.far.menugenerator.common.global.Constants
import com.far.menugenerator.common.helpers.ActivityHelper
import com.far.menugenerator.common.utils.PreferenceUtils
import com.far.menugenerator.databinding.DialogImageTitleDescriptionBinding
import com.far.menugenerator.databinding.FragmentMenuListBinding
import com.far.menugenerator.model.Enums
import com.far.menugenerator.model.State
import com.far.menugenerator.model.common.MenuReference
import com.far.menugenerator.model.database.room.services.MenuTempDS
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.adapters.MenuAdapter
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.MenuListViewModel
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
class MenuList : BaseActivity() {
    // TODO: Rename and change types of parameters

    private lateinit var binding:FragmentMenuListBinding
    private lateinit var viewModel:MenuListViewModel

    @Inject lateinit var factory: MenuListViewModel.MenuListViewModelFactory
    @Inject lateinit var screenNavigation: ScreenNavigation
    @Inject lateinit var dialogManager: DialogManager

    private var menu:Menu?=null

    companion object {
         const val ARG_COMPANY_ID = "COMPANY_ID"
         const val ARG_COMPANY_REF= "COMPANY_REF"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this,factory)[MenuListViewModel::class.java]
        binding = FragmentMenuListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val companyId:String?
        val companyRef:String?
        if(savedInstanceState == null){//se crea desde 0
            companyId = intent?.extras?.getString(ARG_COMPANY_ID)
            companyRef = intent?.extras?.getString(ARG_COMPANY_REF)
        }else{//vuelve despues de haber abandonado la pantalla (ya sea que la activifad siga viva o se haya guardado info con el metodo onSaveInstanceState)
            companyId = savedInstanceState.getString(ARG_COMPANY_ID)
            companyRef = savedInstanceState.getString(ARG_COMPANY_REF)
        }

        if(LoginActivity.userFirebase == null || companyId.isNullOrEmpty() || companyRef.isNullOrEmpty()){
            finish()
        }else{
            //Este ecenario se aplica si la actividad se borra por estar mucho tiempo en background.
            //el viewModel no sostiene la referencia y la info se borra. pero el onSaveInstanceState hace que los valores sobrevivan.
            //Probado usando el developer opcion (Eliminar actividades) que borra tda la info de la actividad inmediatamente el usuario las abandona.
            if(viewModel.companyId.isNullOrEmpty() || viewModel.companyRef.isNullOrEmpty()){
                viewModel.setInitialValues(companyId = companyId, companyReference = companyRef)
            }
            initViews()
            initObservers()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        this.menu = menu
        val showDemo = PreferenceUtils.getShowDemoPreference(context = this,true)
        this.menu?.findItem(R.id.showDemo)?.setVisible(!showDemo)
        this.menu?.findItem(R.id.hideDemo)?.setVisible(showDemo)
        return true
    }

    override fun onResume() {
        super.onResume()
        searchMenus()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_COMPANY_ID,viewModel.companyId)
        outState.putString(ARG_COMPANY_REF,viewModel.companyRef)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.add, R.id.addAction ->{
                dialogManager.showMenuTypeDialog{menuType->
                    if(menuType == Enums.MenuType.DATA_MENU){
                        viewModel.clearMenuTempData()
                        screenNavigation.menuActivity(
                            companyReference = viewModel.companyRef!!,
                            menuId = null,
                            menuType = Enums.MenuType.DATA_MENU.name,
                            isOnline = null,
                            menuRef = null)
                    }else{
                        screenNavigation.menuFileActivity(
                            companyReference = viewModel.companyRef!!,
                            menuId = null,
                            menuType = Enums.MenuType.FILE_MENU.name,
                            isOnline = null,
                            menuRef = null
                        )
                    }
                }

            }
            R.id.optionRefresh -> searchMenus()
            R.id.showDemo -> {
                PreferenceUtils.setShowDemoPreference(context = this,true)
                menu?.findItem(R.id.showDemo)?.setVisible(false)
                menu?.findItem(R.id.hideDemo)?.setVisible(true)
                searchMenus()
            }
            R.id.hideDemo->{
                PreferenceUtils.setShowDemoPreference(context = this,false)
                menu?.findItem(R.id.showDemo)?.setVisible(true)
                menu?.findItem(R.id.hideDemo)?.setVisible(false)
                searchMenus()
            }
        }
        return true
    }

    private fun initViews(){
        val manager = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        binding.rv.layoutManager = manager
        binding.swipe.setOnRefreshListener {
            searchMenus()
        }
        showMenuList()
    }

    private fun showMenuList(){
        binding.upgrade.root.visibility = View.GONE
        binding.swipe.visibility = View.VISIBLE
    }

    private fun initObservers(){
        viewModel.menus.observe(this){
            val adapter = MenuAdapter(it){ menu->

                val options = mutableListOf<ImageOption>()

                if(!menu.online && !menu.isDemo)
                    options.add(ImageOption(icon = R.drawable.rounded_upgrade_24,R.string.upgrade))

                options.add(ImageOption(R.drawable.baseline_remove_red_eye_24,R.string.preview))
                options.add(ImageOption(icon = R.drawable.baseline_file_present_24,R.string.share_menu))

                if(menu.online) {
                    options.add(ImageOption(icon = R.drawable.round_link_24, R.string.copy_link))
                    options.add(ImageOption(icon = R.drawable.rounded_qr_code_2_24, R.string.qr_code))
                }

                if(!menu.isDemo){
                    options.add(ImageOption(R.drawable.round_edit_24,R.string.edit))
                    options.add(ImageOption(R.drawable.rounded_delete_24,R.string.delete))
                }

                dialogManager.showImageBottomSheet(options){option->
                    val userId = if(menu.isDemo) Constants.USERID_DEMO else LoginActivity.userFirebase?.internalId!!
                    val companyId = if(menu.isDemo) Constants.COMPANYID_DEMO else viewModel.companyId!!
                    when(option.string){
                        R.string.upgrade->{
                            screenNavigation.premiumActivity(
                                userId = userId,
                                companyId = companyId,
                                menuId = menu.menuId,
                                menuType = menu.menuType
                                )}
                        R.string.preview -> {
                            viewModel.searchPreviewUri(
                            user = userId,
                            companyId = companyId,
                            downloadDirectory = applicationContext.filesDir,
                            menuReference = menu)}
                        R.string.share_menu -> {
                            viewModel.searchShareUri(
                                user = userId,
                                companyId = companyId,
                                downloadDirectory = applicationContext.filesDir,
                                menuReference = menu)
                        }
                        R.string.copy_link->{
                            //TODO: Acortar URL
                            viewModel.shortenUrl(
                                url = menu.fileUri.split("&token")[0],
                                token = Constants.TYNY_URL_TOKEN,
                                userId = userId,
                                companyId = companyId,
                                firebaseRef = menu.firebaseRef!!)
                        }
                        R.string.qr_code->screenNavigation.qrImagePreview(userId = userId,companyId = companyId, menuFirebaseRef = menu.firebaseRef!!)
                        R.string.edit-> {
                            if(menu.menuType == Enums.MenuType.DATA_MENU.name){
                                viewModel.clearMenuTempData()
                                screenNavigation.menuActivity(
                                    companyReference = viewModel.companyRef!!,
                                    menuId = menu.menuId,
                                    menuType = Enums.MenuType.DATA_MENU.name,
                                    isOnline = menu.online,
                                    menuRef = menu.firebaseRef
                                )
                            }else{
                                screenNavigation.menuFileActivity(
                                    companyReference = viewModel.companyRef!!,
                                    menuId = menu.menuId,
                                    menuType = Enums.MenuType.DATA_MENU.name,
                                    isOnline = menu.online,
                                    menuRef = menu.firebaseRef
                                )
                            }

                        }
                        R.string.delete-> showDeleteMenuConfirmationDialog(menuReference = menu)
                    }
                }
            }
            binding.rv.adapter = adapter
        }

        viewModel.getSearchMenuProcess().observe(this){
            binding.swipe.isRefreshing  = it.state == State.LOADING
            binding.rv.visibility = if(it.state == State.LOADING) View.GONE else View.VISIBLE
        }
        viewModel.getDeleteMenuProcess().observe(this){
            if(it.state  == State.LOADING){
                dialogManager.showLoadingDialog()
            }else{
                dialogManager.dismissLoadingDialog()
            }

            if(it.state == State.SUCCESS){
                searchMenus()
            }else if(it.state == State.GENERAL_ERROR){
                Snackbar.make(binding.root,getString(R.string.operation_failed_please_retry),Snackbar.LENGTH_LONG).show()
                searchMenus()
            }else if(it.state == State.NETWORK_ERROR){
                dialogManager.showInternetErrorDialog()
            }
        }

        viewModel.shareFileState.observe(this){
            if(it.state == State.LOADING)
                dialogManager.showLoadingDialog()
            else
                dialogManager.dismissLoadingDialog()

            if(it.state == State.SUCCESS)
            ActivityHelper.shareFile(this,viewModel.getFileUri().toFile())
            else if(it.state == State.GENERAL_ERROR)
                Snackbar.make(binding.root,getString(R.string.operation_failed_please_retry),Snackbar.LENGTH_LONG).show()
            else if(it.state == State.NETWORK_ERROR)
                dialogManager.showInternetErrorDialog()
        }

        viewModel.previewFileState.observe(this){
            if(it.state == State.LOADING)
                dialogManager.showLoadingDialog()
            else
                dialogManager.dismissLoadingDialog()

            if(it.state == State.SUCCESS)
                ActivityHelper.viewFile(this,viewModel.getFileUri().toFile())
            else if(it.state == State.GENERAL_ERROR)
                Snackbar.make(binding.root,getString(R.string.operation_failed_please_retry),Snackbar.LENGTH_LONG).show()
            else if(it.state == State.NETWORK_ERROR)
                dialogManager.showInternetErrorDialog()
        }

        viewModel.shortUrlState.observe(this){
            if(it.state == State.LOADING)
                dialogManager.showLoadingDialog()
            else
                dialogManager.dismissLoadingDialog()

            if(it.state == State.SUCCESS) {
                ActivityHelper.copyTextToClipboard(this, label = "URL", it.message!!)
                Toast.makeText(this,getString(R.string.text_copied_to_clipboard),Toast.LENGTH_SHORT).show()
            }else if(it.state == State.GENERAL_ERROR)
                Snackbar.make(binding.root,getString(R.string.operation_failed_please_retry),Snackbar.LENGTH_LONG).show()
            else if(it.state == State.NETWORK_ERROR)
                dialogManager.showInternetErrorDialog()
        }
    }

    private fun searchMenus(){
        viewModel.getMenus(
            user= LoginActivity.userFirebase?.internalId!!,
            showDemo = PreferenceUtils.getShowDemoPreference(context = this,true),
            demoId = getString(R.string.menu_demo_id))
    }

    private fun showDeleteMenuConfirmationDialog(menuReference: MenuReference){

        val dialogBinding = DialogImageTitleDescriptionBinding.inflate(layoutInflater)
        dialogBinding.title.setText(R.string.delete_menu)
        dialogBinding.body.setText(R.string.are_you_sure_you_want_to_delete_this_menu)
        dialogBinding.img.setImageResource(R.drawable.delete)
        dialogManager.showTwoButtonsDialog( view = dialogBinding.root,
            button1Label = R.string.delete,
            onButton1Click = {
                viewModel.deleteMenu(LoginActivity.userFirebase?.internalId!!, menuReference = menuReference)
            },
            button2Label = R.string.cancel,
            onButton2Click = {
            })
    }

}