package com.far.menugenerator.model.denpendencyInjection.application

import com.far.menugenerator.model.denpendencyInjection.activity.ActivityComponent
import com.far.menugenerator.model.denpendencyInjection.activity.ActivityModule
import dagger.Component

@Component(modules = [ApplicationModule::class])
@AppScope
interface AppComponent {

    fun newActivityComponent(activityModule:ActivityModule):ActivityComponent
}