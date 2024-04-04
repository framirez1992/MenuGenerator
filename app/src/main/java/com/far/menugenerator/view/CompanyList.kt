package com.far.menugenerator.view


import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.R
import com.far.menugenerator.common.utils.PreferenceUtils
import com.far.menugenerator.databinding.DialogImageTitleDescriptionBinding
import com.far.menugenerator.databinding.FragmentCompanyListBinding
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.adapters.CompanyAdapter
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.CompanyListViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject


class CompanyList : BaseActivity() {

    private lateinit var binding:FragmentCompanyListBinding
    private lateinit var _viewModel:CompanyListViewModel
    @Inject lateinit var companyListFactory:CompanyListViewModel.CompanyListViewModelFactory
    @Inject lateinit var navigation: ScreenNavigation
    @Inject lateinit var dialogManager: DialogManager
    @Inject lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(LoginActivity.userFirebase == null){
            finish()
        }else{
            presentationComponent.inject(this)
            _viewModel = ViewModelProvider(this,companyListFactory)[CompanyListViewModel::class.java]
            binding = FragmentCompanyListBinding.inflate(layoutInflater)


            setContentView(binding.root)

            initViews()
            initObservers()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.company_list_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.optionRefresh -> getCompanies()
            R.id.optionNewMenu -> navigation.companyActivity(null)
            R.id.optionLogout -> signOut()
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        _viewModel.onResume(LoginActivity.userFirebase?.internalId!!)
    }
    override fun onBackPressed() {
        //super.onBackPressed()
    }

    private fun initViews(){

        binding.swipe.setOnRefreshListener {
            getCompanies()
        }
        val lm = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        binding.rv.layoutManager = lm
    }
    private fun initObservers(){
        _viewModel.getSearchCompaniesState().observe(this){
            processCompanySearchState(it)
        }
        _viewModel.getDeleteCompanyState().observe(this){
            processDeleteCompanyState(it)
        }
        _viewModel.getCompanies().observe(this){

            val adapters = CompanyAdapter(it){ comp->
                val options = listOf(
                    ImageOption(R.drawable.baseline_menu_book_24,R.string.show_menus),
                    ImageOption(R.drawable.round_edit_24,R.string.edit),
                    ImageOption(R.drawable.rounded_delete_24,R.string.delete)
                )
                dialogManager.showImageBottomSheet(options){option->

                    when(option.string){
                        R.string.show_menus -> navigation.menuListActivity(companyId = comp.companyId, companyRef = comp.fireBaseRef!!)
                        R.string.edit-> navigation.companyActivity(company = comp)
                        R.string.delete-> showDeleteCompanyConfirmationDialog(company = comp)
                    }
                }

            }
            binding.rv.adapter = adapters
        }
    }


    private fun getCompanies(){
        _viewModel.getCompanies(LoginActivity.userFirebase?.internalId!!)
    }
    private fun processCompanySearchState(processState: ProcessState?){
        if(processState == null)
            return

        binding.swipe.isRefreshing = processState.state == State.LOADING
        binding.rv.visibility = if(processState.state == State.LOADING) View.GONE else View.VISIBLE

        if(processState.state == State.SUCCESS
            && _viewModel.getCompanies().value!!.isEmpty()
            && PreferenceUtils.getShowNoCompanyAlert(context = this,true)){

            PreferenceUtils.setShowNoCompanyAlert(context = this,false)
            showFirstCompanyDialog()
        }else if(processState.state == State.GENERAL_ERROR)
            Snackbar.make(binding.root,
                getString(R.string.operation_failed_please_retry),
                Snackbar.LENGTH_LONG).show()
    }

    private fun showFirstCompanyDialog(){
        val dialogBinding = DialogImageTitleDescriptionBinding.inflate(layoutInflater)
        dialogBinding.title.setText(R.string.add_company)
        dialogBinding.body.setText(R.string.do_you_want_to_add_a_new_company_now)
        dialogBinding.img.setImageResource(R.drawable.ask)
        dialogManager.showTwoButtonsDialog(dialogBinding.root,
            button1Label = R.string.yes,
            onButton1Click = {
                navigation.companyActivity(null)
            },
            button2Label = R.string.not_now,
            onButton2Click = {})
    }

    private fun processDeleteCompanyState(processState: ProcessState?){
        if(processState == null)
            return
        if(processState.state == State.LOADING){
            dialogManager.showLoadingDialog()
        }else{
            dialogManager.dismissLoadingDialog()
        }
        if(processState.state == State.GENERAL_ERROR){
            Snackbar.make(binding.root,
                getString(R.string.operation_failed_please_retry),
                Snackbar.LENGTH_LONG).show()
        }else if(processState.state == State.NETWORK_ERROR){
            dialogManager.showInternetErrorDialog()
        }
    }

    private fun showDeleteCompanyConfirmationDialog(company:CompanyFirebase){

        val dialogBinding = DialogImageTitleDescriptionBinding.inflate(layoutInflater)
        dialogBinding.title.setText(R.string.delete_company)
        dialogBinding.body.setText(R.string.are_you_sure_you_want_to_delete_this_company)
        dialogBinding.img.setImageResource(R.drawable.delete)
        dialogManager.showTwoButtonsDialog( view = dialogBinding.root,
            button1Label = R.string.delete,
            onButton1Click = {
                _viewModel.deleteCompany(user = LoginActivity.userFirebase?.internalId!!, company = company)
            },
            button2Label = R.string.cancel,
            onButton2Click = {
            })
    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                finish()
            }
    }
}