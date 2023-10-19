package com.far.menugenerator.view.common

import android.content.Intent
import com.far.menugenerator.MainActivity
import com.far.menugenerator.R
import com.far.menugenerator.view.CompanyFragment
import com.far.menugenerator.view.CreateMenuFragment
import com.far.menugenerator.view.MainScreenFragment
import javax.inject.Inject

class ScreenNavigation @Inject constructor(private val activity: BaseActivity) {


    fun mainActivity(){
        activity.startActivity(Intent(activity,MainActivity::class.java))
    }
    fun mainScreenFragment(){
        setFragment(MainScreenFragment.newInstance())
    }
    fun companyFragment(){
        setFragment(CompanyFragment.newInstance())
    }
    fun createMenuFragment(){
        setFragment(CreateMenuFragment.newInstance())
    }

    private fun setFragment(baseFragment: BaseFragment){
        val fragmentManager = activity.supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.frame, baseFragment)
        fragmentTransaction.commit()
    }
}