package com.far.menugenerator.view


import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.R
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
        presentationComponent.inject(this)
        _viewModel = ViewModelProvider(this,companyListFactory)[CompanyListViewModel::class.java]
        binding = FragmentCompanyListBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initViews()
        initObservers()
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
        _viewModel.onResume(LoginActivity.account?.email!!)
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
            //TODO: si no hay companias, abrir navigation.companyActivity() automaticamente

            val adapters = CompanyAdapter(it){ comp->
                val options = listOf(
                    ImageOption(R.drawable.baseline_menu_book_24,R.string.show_menus),
                    ImageOption(R.drawable.round_edit_24,R.string.edit),
                    ImageOption(R.drawable.rounded_delete_24,R.string.delete)
                )
                dialogManager.showImageBottomSheet(options){option->

                    when(option.string){
                        R.string.show_menus -> navigation.menuListActivity(company = comp)
                        R.string.edit-> navigation.companyActivity(company = comp)
                        R.string.delete-> showDeleteCompanyConfirmationDialog(company = comp)
                    }
                }

            }
            binding.rv.adapter = adapters
        }
    }


    private fun getCompanies(){
        _viewModel.getCompanies(LoginActivity.account?.email!!)
    }
    private fun processCompanySearchState(processState: ProcessState?){
        if(processState == null)
            return

        binding.swipe.isRefreshing = processState.state == State.LOADING
        binding.rv.visibility = if(processState.state == State.LOADING) View.GONE else View.VISIBLE

        if(processState.state == State.SUCCESS && _viewModel.getCompanies().value!!.isEmpty()){

            dialogManager.showOptionDialog(
                title= R.string.add_company,
                message = R.string.do_you_want_to_add_a_new_company_now,
                positiveText = R.string.yes,
                negativeText = R.string.not_now){_,_->

                navigation.companyActivity(null)
            }
        }else if(processState.state == State.ERROR)
            Snackbar.make(binding.root,
                getString(R.string.operation_failed_please_retry),
                Snackbar.LENGTH_LONG).show()
    }

    private fun processDeleteCompanyState(processState: ProcessState?){
        if(processState == null)
            return
        if(processState.state == State.LOADING){
            dialogManager.showLoadingDialog()
        }else{
            dialogManager.dismissLoadingDialog()
        }
        if(processState.state == State.ERROR){
            Snackbar.make(binding.root,
                getString(R.string.operation_failed_please_retry),
                Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showDeleteCompanyConfirmationDialog(company:CompanyFirebase){

        dialogManager.showOptionDialog(
            title= R.string.delete_company,
            message = R.string.are_you_sure_you_want_to_delete_this_company,
            positiveText = R.string.delete,
            negativeText = R.string.cancel){_,_->

            _viewModel.deleteCompany(user = LoginActivity.account?.email!!, company = company)
        }
    }

    private fun signOut() {
        mGoogleSignInClient.signOut()
            .addOnCompleteListener(this) {
                finish()
            }
    }
}