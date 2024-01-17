package com.far.menugenerator.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Orientation
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentCompanyListBinding
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.adapters.CompanyAdapter
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.CompanyListViewModel
import javax.inject.Inject



class CompanyList : BaseActivity() {

    private lateinit var binding:FragmentCompanyListBinding
    private lateinit var _viewModel:CompanyListViewModel
    @Inject lateinit var companyListFactory:CompanyListViewModel.CompanyListViewModelFactory
    @Inject lateinit var navigation: ScreenNavigation
    @Inject lateinit var dialogManager: DialogManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        _viewModel = ViewModelProvider(this,companyListFactory)[CompanyListViewModel::class.java]
        binding = FragmentCompanyListBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initViews()
        initObservers()
    }


    override fun onResume() {
        super.onResume()
        getCompanies()
    }

    private fun initViews(){
        binding.btnNewCompany.setOnClickListener{
            navigation.companyActivity(null)
        }
        binding.btnRefresh.setOnClickListener{
            getCompanies()
        }
        val lm = LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        binding.rv.layoutManager = lm
    }
    private fun initObservers(){
        _viewModel.isLoading.observe(this){
            binding.pb.visibility = if(it) View.VISIBLE else View.GONE
        }
        _viewModel.isProcessing.observe(this){
            if(it) dialogManager.showLoadingDialog()
            else dialogManager.dismissLoadingDialog()
        }
        _viewModel.companies.observe(this){
            //TODO: si no hay companias, abrir navigation.companyActivity() automaticamente

            val adapters = CompanyAdapter(it){ comp->
                dialogManager.showOptionDialog(resources.getString(R.string.options), arrayOf(
                    resources.getString(R.string.show_menus),
                    resources.getString(R.string.edit),
                    resources.getString(R.string.delete))){option->

                    when(option){
                        resources.getString(R.string.show_menus)-> navigation.menuListFragment(company = comp)
                        resources.getString(R.string.edit)-> navigation.companyActivity(company = comp)
                        resources.getString(R.string.delete)-> _viewModel.deleteCompany(user = LoginActivity.account?.email!!, company = comp)
                    }
                }

            }
            binding.rv.adapter = adapters
        }
    }


    private fun getCompanies(){
        _viewModel.getCompanies(LoginActivity.account?.email!!)
    }
}