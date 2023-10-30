package com.far.menugenerator.model.denpendencyInjection.presentation

import com.far.menugenerator.MainActivity
import com.far.menugenerator.view.CreateMenuFragment
import com.far.menugenerator.view.LoginActivity
import com.far.menugenerator.view.common.ScreenNavigation
import dagger.Subcomponent

@PresentationScope
@Subcomponent(modules = [PresentationModule::class])
interface PresentationComponent {


    fun inject(loginActivity:LoginActivity)
    fun inject(mainActivity: MainActivity)

    fun inject(createMenu:CreateMenuFragment)
}