package com.far.menugenerator.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.net.toFile
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.far.menugenerator.R
import com.far.menugenerator.common.helpers.ActivityHelper
import com.far.menugenerator.databinding.DialogImageTitleDescriptionBinding
import com.far.menugenerator.databinding.DialogSellProductsBinding
import com.far.menugenerator.databinding.FragmentMenuListBinding
import com.far.menugenerator.model.State
import com.far.menugenerator.model.common.MenuReference
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.adapters.MenuAdapter
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.MenuListViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 * Use the [MenuList.newInstance] factory method to
 * create an instance of this fragment.
 */
class MenuList : BaseActivity() {
    // TODO: Rename and change types of parameters
    private var company: CompanyFirebase? = null

    private lateinit var binding:FragmentMenuListBinding
    private lateinit var viewModel:MenuListViewModel

    @Inject lateinit var factory: MenuListViewModel.MenuListViewModelFactory
    @Inject lateinit var screenNavigation: ScreenNavigation
    @Inject lateinit var dialogManager: DialogManager

    companion object {
         const val ARG_COMPANY = "company"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)

        binding = FragmentMenuListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        company = intent?.extras?.getSerializable(ARG_COMPANY) as CompanyFirebase
        viewModel = ViewModelProvider(this,factory)[MenuListViewModel::class.java]

        initViews()
        initObservers()


    }

    override fun onResume() {
        super.onResume()
        searchMenus()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.add->screenNavigation.menuActivity(companyReference = company!!.fireBaseRef!!,menuFirebase = null)
            R.id.optionRefresh -> searchMenus()
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

    private fun showUpgrade(){

    }

    private fun showMenuList(){
        binding.upgrade.root.visibility = View.GONE
        binding.swipe.visibility = View.VISIBLE
    }

    private fun initObservers(){
        viewModel.menus.observe(this){
            val adapter = MenuAdapter(it){ menu->

                val options = mutableListOf<ImageOption>()

                if(!menu.online)
                    options.add(ImageOption(icon = R.drawable.rounded_upgrade_24,R.string.upgrade))

                options.add(ImageOption(R.drawable.baseline_remove_red_eye_24,R.string.preview))
                options.add(ImageOption(icon = R.drawable.baseline_file_present_24,R.string.share_menu))
                if(menu.online) {
                    //options.add(ImageOption(icon = R.drawable.round_link_24, R.string.copy_link))
                    options.add(ImageOption(icon = R.drawable.rounded_qr_code_2_24, R.string.qr_code))
                }

                options.add(ImageOption(R.drawable.round_edit_24,R.string.edit))
                options.add(ImageOption(R.drawable.rounded_delete_24,R.string.delete))

                dialogManager.showImageBottomSheet(options){option->
                    when(option.string){
                        R.string.upgrade->{
                            screenNavigation.premiumActivity(
                                userId = LoginActivity.userFirebase?.internalId!!,
                                companyId = company?.companyId!!,
                                menuId = menu.menuId
                                )}
                        R.string.preview -> {
                            viewModel.searchPreviewUri(
                            user = LoginActivity.userFirebase?.internalId!!,
                            companyId = company?.companyId!!,
                            downloadDirectory = applicationContext.filesDir,
                            menuReference = menu)}
                        R.string.share_menu -> {
                            viewModel.searchShareUri(
                                user = LoginActivity.userFirebase?.internalId!!,
                                companyId = company?.companyId!!,
                                downloadDirectory = applicationContext.filesDir,
                                menuReference = menu)
                        }
                        R.string.copy_link->{
                            //TODO: Acortar URL
                            //ActivityHelper.copyTextToClipboard(this,"url",menu.fileUri)
                        }
                        R.string.qr_code->screenNavigation.qrImagePreview(companyId = company?.companyId!!, menuReference = menu)
                        R.string.edit-> screenNavigation.menuActivity(companyReference = company?.fireBaseRef!!,menu)
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
    }

    private fun searchMenus(){
        viewModel.getMenus(user= LoginActivity.userFirebase?.internalId!!, companyId = company?.companyId!!)
    }

    private fun showDeleteMenuConfirmationDialog(menuReference: MenuReference){

        val dialogBinding = DialogImageTitleDescriptionBinding.inflate(layoutInflater)
        dialogBinding.title.setText(R.string.delete_menu)
        dialogBinding.body.setText(R.string.are_you_sure_you_want_to_delete_this_menu)
        dialogBinding.img.setImageResource(R.drawable.delete)
        dialogManager.showTwoButtonsDialog( view = dialogBinding.root,
            button1Label = R.string.delete,
            onButton1Click = {
                viewModel.deleteMenu(LoginActivity.userFirebase?.internalId!!, companyId = company?.companyId!!, menuReference = menuReference)
            },
            button2Label = R.string.cancel,
            onButton2Click = {
            })
    }

}