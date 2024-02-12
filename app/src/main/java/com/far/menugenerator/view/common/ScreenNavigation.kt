package com.far.menugenerator.view.common

import android.content.Intent
import com.far.menugenerator.MainActivity
import com.far.menugenerator.R
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.view.CompanyActivity
import com.far.menugenerator.view.CompanyList
import com.far.menugenerator.view.CreateMenuFragment
import com.far.menugenerator.view.MenuActivity
import com.far.menugenerator.view.MenuList
import com.far.menugenerator.view.QRPreview

class ScreenNavigation (private val baseActivity: BaseActivity) {

    private val activity get() = baseActivity
    private val fragmentManager get() = activity.supportFragmentManager
    private lateinit var currentFragment:BaseFragment

    fun companyActivity(company:CompanyFirebase?){
        val i= Intent(activity,CompanyActivity::class.java).apply {
            putExtra(CompanyActivity.ARG_COMPANY,company)
        }
        activity.startActivity(i)
    }
    fun companyListActivity(){
        activity.startActivity(Intent(activity,CompanyList::class.java))
    }
    fun menuListActivity(company:CompanyFirebase){
        val i = Intent(activity,MenuList::class.java).apply {
            putExtra(MenuList.ARG_COMPANY,company)
        }
        activity.startActivity(i)
    }

    fun menuActivity(company: CompanyFirebase, menuFirebase: MenuFirebase?){
        val i = Intent(activity,MenuActivity::class.java).apply {
            putExtra(MenuActivity.ARG_COMPANY,company)
            putExtra(MenuActivity.ARG_MENU,menuFirebase)
        }
        activity.startActivity(i)
    }

    fun createMenuFragment(company: CompanyFirebase, menuFirebase: MenuFirebase?){
        currentFragment = CreateMenuFragment.newInstance(company,menuFirebase)
        setFragment()
    }
    fun qrImagePreview(companyId:String,firebaseRef:String){
        val i = Intent(activity, QRPreview::class.java).apply {
            putExtra(QRPreview.ARG_MENU_REF,firebaseRef)
            putExtra(QRPreview.ARG_COMPANY_ID,companyId)
        }
        activity.startActivity(i)
    }

    fun currentFragmentOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        currentFragment?.onActivityResult(requestCode, resultCode, data)
    }
    private fun setFragment(){
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame,currentFragment)
        fragmentTransaction.commit()
    }
}