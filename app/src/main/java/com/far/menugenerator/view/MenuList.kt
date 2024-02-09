package com.far.menugenerator.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentMenuListBinding
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.adapters.MenuAdapter
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.MenuListViewModel
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

        searchMenus()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.add->screenNavigation.createMenuFragment(company = company!!,menuFirebase = null)
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
    }

    private fun initObservers(){
        viewModel.menus.observe(this){
            val adapter = MenuAdapter(it){ menu->
                dialogManager.showOptionDialog(resources.getString(R.string.options),
                    arrayOf(resources.getString(R.string.preview),resources.getString(R.string.edit),resources.getString(R.string.delete))
                ){ option->
                    when(option){
                        resources.getString(R.string.preview)-> screenNavigation.qrImagePreview(companyId = company?.companyId!!, firebaseRef = menu.fireBaseRef!!)
                        resources.getString(R.string.edit) -> screenNavigation.createMenuFragment(company = company!!,menu)
                        resources.getString(R.string.delete) -> viewModel.deleteMenu(LoginActivity.account?.email!!, companyId = company?.companyId!!, menuFirebase = menu)
                    }
                }

            }
            binding.rv.adapter = adapter
        }

        viewModel.isLoading.observe(this){
            binding.swipe.isRefreshing  = it
            binding.rv.visibility = if(it) View.GONE else View.VISIBLE
        }
    }

    private fun searchMenus(){
        viewModel.getMenus(user= LoginActivity.account?.email!!, companyId = company?.companyId!!)
    }

}