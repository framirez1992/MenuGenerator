package com.far.menugenerator.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentMenuListBinding
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.adapters.MenuAdapter
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.MenuListViewModel
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_COMPANY = "company"

/**
 * A simple [Fragment] subclass.
 * Use the [MenuListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MenuListFragment : BaseFragment() {
    // TODO: Rename and change types of parameters
    private var company: CompanyFirebase? = null

    private lateinit var binding:FragmentMenuListBinding
    private lateinit var viewModel:MenuListViewModel

    @Inject lateinit var factory: MenuListViewModel.MenuListViewModelFactory
    @Inject lateinit var screenNavigation: ScreenNavigation
    @Inject lateinit var dialogManager: DialogManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            company = it.getSerializable(ARG_COMPANY) as CompanyFirebase
        }
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this,factory)[MenuListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuListBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()

        searchMenus()
    }

        private fun initViews(){
        val manager = LinearLayoutManager(requireContext(),RecyclerView.VERTICAL,false)
        binding.rv.layoutManager = manager
        binding.btnRefresh.setOnClickListener{
            searchMenus()
        }
        binding.btnNewMenu.setOnClickListener{
            screenNavigation.createMenuFragment(company!!,null)
        }
    }

    private fun initObservers(){
        viewModel.menus.observe(viewLifecycleOwner){
            //if(it.isEmpty()){
            //    screenNavigation.createMenuFragment(company!!)
            //    return@observe
            //}
            val adapter = MenuAdapter(it){ menu->
                dialogManager.showOptionDialog(resources.getString(R.string.options),
                    arrayOf(resources.getString(R.string.preview),resources.getString(R.string.edit),resources.getString(R.string.delete))
                ){ option->
                    when(option){
                        resources.getString(R.string.preview)-> screenNavigation.qrImagePreview(menu.fileUrl)
                        resources.getString(R.string.edit) -> screenNavigation.createMenuFragment(company = company!!,menu)
                        resources.getString(R.string.delete) -> viewModel.deleteMenu(LoginActivity.account?.email!!, companyId = company?.companyId!!, menuFirebase = menu)
                    }
                }

            }
            binding.rv.adapter = adapter
        }

        viewModel.isLoading.observe(viewLifecycleOwner){
            binding.pb.visibility = if(it) View.VISIBLE else View.GONE
        }
    }

    private fun searchMenus(){
        viewModel.getMenus(user= LoginActivity.account?.email!!, companyId = company?.companyId!!)
    }
    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(company: CompanyFirebase) =
            MenuListFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_COMPANY, company)
                }
            }
    }
}