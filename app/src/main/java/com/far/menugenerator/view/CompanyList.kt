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
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.CompanyListViewModel
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//private const val ARG_PARAM1 = "param1"
//private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CompanyList.newInstance] factory method to
 * create an instance of this fragment.
 */
class CompanyList : BaseFragment() {
    // TODO: Rename and change types of parameters
    //private var param1: String? = null
    //private var param2: String? = null

    private lateinit var binding:FragmentCompanyListBinding
    private lateinit var _viewModel:CompanyListViewModel
    @Inject lateinit var companyListFactory:CompanyListViewModel.CompanyListViewModelFactory
    @Inject lateinit var navigation: ScreenNavigation
    @Inject lateinit var dialogManager: DialogManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //param1 = it.getString(ARG_PARAM1)
            //param2 = it.getString(ARG_PARAM2)
        }
        presentationComponent.inject(this)
        _viewModel = ViewModelProvider(this,companyListFactory)[CompanyListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCompanyListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initObservers()

        getCompanies()
    }


    private fun initViews(){
        binding.btnNewCompany.setOnClickListener{
            navigation.companyFragment(null)
        }
        binding.btnRefresh.setOnClickListener{
            getCompanies()
        }
        val lm = LinearLayoutManager(requireContext(),RecyclerView.VERTICAL,false)
        binding.rv.layoutManager = lm
    }
    private fun initObservers(){
        _viewModel.isLoading.observe(viewLifecycleOwner){
            binding.pb.visibility = if(it) View.VISIBLE else View.GONE
        }
        _viewModel.isProcessing.observe(viewLifecycleOwner){
            if(it) dialogManager.showLoadingDialog()
            else dialogManager.dismissLoadingDialog()
        }
        _viewModel.companies.observe(viewLifecycleOwner){
            val adapters = CompanyAdapter(it){ comp->
                dialogManager.showOptionDialog(resources.getString(R.string.options), arrayOf(
                    resources.getString(R.string.show_menus),
                    resources.getString(R.string.edit),
                    resources.getString(R.string.delete))){option->

                    when(option){
                        resources.getString(R.string.show_menus)-> navigation.menuListFragment(company = comp)
                        resources.getString(R.string.edit)-> navigation.companyFragment(comp)
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
    companion object {
        @JvmStatic
        fun newInstance() =
            CompanyList().apply {
                arguments = Bundle().apply {
                    //putString(ARG_PARAM1, param1)
                    //putString(ARG_PARAM2, param2)
                }
            }
    }
}