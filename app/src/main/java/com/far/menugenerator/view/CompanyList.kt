package com.far.menugenerator.view


import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentCompanyListBinding
import com.far.menugenerator.model.LoadingState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.adapters.CompanyAdapter
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
        if(item.itemId == R.id.optionNewMenu)
            navigation.companyActivity(null)
        else if(item.itemId == R.id.optionLogout)
            signOut()

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
                dialogManager.showOptionDialog(resources.getString(R.string.options), arrayOf(
                    resources.getString(R.string.show_menus),
                    resources.getString(R.string.edit),
                    resources.getString(R.string.delete))){option->

                    when(option){
                        resources.getString(R.string.show_menus)-> navigation.menuListFragment(company = comp)
                        resources.getString(R.string.edit)-> navigation.companyActivity(company = comp)
                        resources.getString(R.string.delete)-> showDeleteCompanyConfirmationDialog(company = comp)
                    }
                }

            }
            binding.rv.adapter = adapters
        }
    }


    private fun getCompanies(){
        _viewModel.getCompanies(LoginActivity.account?.email!!)
    }
    private fun processCompanySearchState(loadingState: LoadingState?){
        if(loadingState == null)
            return

        binding.swipe.isRefreshing = loadingState.state == State.LOADING

        if(loadingState.state == State.SUCCESS && _viewModel.getCompanies().value!!.isEmpty()){
            dialogManager.showOptionDialog(title = getString(R.string.do_you_want_to_add_a_new_company_now),
                options =  arrayOf(getString(R.string.yes), getString(R.string.not_now))){
                if(it == getString(R.string.yes))
                    navigation.companyActivity(null)
            }
        }else if(loadingState.state == State.ERROR)
            Snackbar.make(binding.root,
                getString(R.string.operation_failed_please_retry),
                Snackbar.LENGTH_LONG).show()
    }

    private fun processDeleteCompanyState(loadingState: LoadingState?){
        if(loadingState == null)
            return
        if(loadingState.state == State.LOADING){
            dialogManager.showLoadingDialog()
        }else{
            dialogManager.dismissLoadingDialog()
        }
        if(loadingState.state == State.ERROR){
            Snackbar.make(binding.root,
                getString(R.string.operation_failed_please_retry),
                Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showDeleteCompanyConfirmationDialog(company:CompanyFirebase){
        dialogManager.showOptionDialog(getString(R.string.are_you_sure_you_want_to_delete_this_company),
            arrayOf(getString(R.string.delete),getString(R.string.cancel))){
            if(it == getString(R.string.delete))
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