package com.far.menugenerator.model.denpendencyInjection.activity

import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationComponent
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationModule
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [ActivityModule::class])
interface  ActivityComponent{

    fun newPresentationComponent(presentationModule: PresentationModule):PresentationComponent
}