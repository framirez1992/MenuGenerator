package com.far.menugenerator.view.common

import android.content.Intent
import com.far.menugenerator.MainActivity
import com.far.menugenerator.R
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.view.CompanyActivity
import com.far.menugenerator.view.CompanyList
import com.far.menugenerator.view.CreateMenuFragment
import com.far.menugenerator.view.MainScreenFragment
import com.far.menugenerator.view.MenuListFragment
import com.far.menugenerator.view.QRPreviewFragment

class ScreenNavigation (private val baseActivity: BaseActivity) {

    private val activity get() = baseActivity
    private val fragmentManager get() = activity.supportFragmentManager
    private lateinit var currentFragment:BaseFragment

    fun mainActivity(){
        activity.startActivity(Intent(activity,MainActivity::class.java))
    }
    fun companyActivity(company:CompanyFirebase?){
        val i= Intent(activity,CompanyActivity::class.java)
        i.putExtra(CompanyActivity.ARG_COMPANY,company)
        activity.startActivity(i)
    }
    fun companyListActivity(){
        activity.startActivity(Intent(activity,CompanyList::class.java))
    }

    fun mainScreenFragment(){
        currentFragment = MainScreenFragment.newInstance()
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