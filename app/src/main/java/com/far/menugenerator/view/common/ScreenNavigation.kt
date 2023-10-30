package com.far.menugenerator.view.common

import android.content.Intent
import com.far.menugenerator.MainActivity
import com.far.menugenerator.R
import com.far.menugenerator.view.CompanyFragment
import com.far.menugenerator.view.CreateMenuFragment
import com.far.menugenerator.view.MainScreenFragment
import com.far.menugenerator.view.QRPreviewFragment
import javax.inject.Inject

class ScreenNavigation (val baseActivity: BaseActivity) {

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
    fun companyFragment(){
        currentFragment = CompanyFragment.newInstance()
        setFragment()
    }
    fun createMenuFragment(){
        currentFragment = CreateMenuFragment.newInstance()
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