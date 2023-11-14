package com.far.menugenerator.view.common

import android.content.Intent
import com.far.menugenerator.MainActivity
import com.far.menugenerator.R
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.view.CompanyFragment
import com.far.menugenerator.view.CompanyList
import com.far.menugenerator.view.CreateMenuFragment
import com.far.menugenerator.view.MainScreenFragment
import com.far.menugenerator.view.MenuListFragment
import com.far.menugenerator.view.QRPreviewFragment
import javax.inject.Inject

class ScreenNavigation (private val baseActivity: BaseActivity) {

    private val activity get() = baseActivity
    private val fragmentManager get() = activity.supportFragmentManager
    private lateinit var currentFragment:BaseFragment

    fun mainActivity(){
        activity.startActivity(Intent(activity,MainActivity::class.java))
    }
    fun mainScreenFragment(){
        currentFragment = MainScreenFragment.newInstance()
        setFragment()
    }
    fun companyFragment(company:CompanyFirebase?){
        currentFragment = CompanyFragment.newInstance(company)
        setFragment()
    }

    fun companyListFragment(){
        currentFragment = CompanyList.newInstance()
        setFragment()
    }

    fun menuListFragment(company:CompanyFirebase){
        currentFragment = MenuListFragment.newInstance(company)
        setFragment()
    }
    fun createMenuFragment(company: CompanyFirebase, menuFirebase: MenuFirebase?){
        currentFragment = CreateMenuFragment.newInstance(company,menuFirebase)
        setFragment()
    }
    fun qrImagePreview(data:String){
        currentFragment = QRPreviewFragment.newInstance(data)
        setFragment()
    }

    private fun setFragment(){
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame,currentFragment)
        fragmentTransaction.commit()
    }
}