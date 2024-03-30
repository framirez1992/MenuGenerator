package com.far.menugenerator.view.common

import android.content.Intent
import com.far.menugenerator.R
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.CompanyActivity
import com.far.menugenerator.view.CompanyList
import com.far.menugenerator.view.MenuActivity
import com.far.menugenerator.view.MenuList
import com.far.menugenerator.view.PremiumActivity
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
    fun menuListActivity(companyId:String, companyRef:String){
        val i = Intent(activity,MenuList::class.java).apply {
            putExtra(MenuList.ARG_COMPANY_ID,companyId)
            putExtra(MenuList.ARG_COMPANY_REF,companyRef)
        }
        activity.startActivity(i)
    }

    fun menuActivity(companyReference: String,menuId: String?,isOnline:Boolean?,menuRef:String?){
        val i = Intent(activity,MenuActivity::class.java).apply {
            putExtra(MenuActivity.ARG_COMPANY_REF,companyReference)
            putExtra(MenuActivity.ARG_MENU_ID,menuId)
            putExtra(MenuActivity.ARG_MENU_ONLINE,isOnline)
            putExtra(MenuActivity.ARG_MENU_FIREBASE_REF,menuRef)
        }
        activity.startActivity(i)
    }

    fun qrImagePreview(userId: String,companyId:String,menuFirebaseRef: String){
        val i = Intent(activity, QRPreview::class.java).apply {
            putExtra(QRPreview.ARG_USER_ID,userId)
            putExtra(QRPreview.ARG_COMPANY_ID,companyId)
            putExtra(QRPreview.ARG_MENU_FIREBASE_REF,menuFirebaseRef)
        }
        activity.startActivity(i)
    }

    fun premiumActivity(userId:String, companyId:String, menuId:String){
        val i = Intent(activity, PremiumActivity::class.java).apply {
            putExtra(PremiumActivity.ARG_USER,userId)
            putExtra(PremiumActivity.ARG_COMPANY_ID,companyId)
            putExtra(PremiumActivity.ARG_MENU_ID,menuId)
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