package com.far.menugenerator.model.denpendencyInjection.presentation

import com.far.menugenerator.MainActivity
import com.far.menugenerator.view.CompanyActivity
import com.far.menugenerator.view.CompanyList
import com.far.menugenerator.view.CreateMenuFragment
import com.far.menugenerator.view.LoginActivity
import com.far.menugenerator.view.MenuListFragment
import dagger.Subcomponent

@PresentationScope
@Subcomponent(modules = [PresentationModule::class])
interface PresentationComponent {


    fun inject(loginActivity:LoginActivity)
    fun inject(mainActivity: MainActivity)
    fun inject(company:CompanyActivity)
    fun inject(companyList:CompanyList)
    fun inject(menuList:MenuListFragment)
    fun inject(createMenu:CreateMenuFragment)
}