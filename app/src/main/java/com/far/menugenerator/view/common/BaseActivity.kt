package com.far.menugenerator.view.common

import androidx.appcompat.app.AppCompatActivity
import com.far.menugenerator.model.denpendencyInjection.activity.ActivityComponent
import com.far.menugenerator.model.denpendencyInjection.activity.ActivityModule
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationComponent
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationModule
import com.far.menugenerator.view.MyApplication

open class BaseActivity :AppCompatActivity(){
    val applicationComponent get() = (application as MyApplication).appComponent

    val activityComponent:ActivityComponent by lazy {
        applicationComponent.newActivityComponent(ActivityModule(this))
    }
    protected val presentationComponent:PresentationComponent by lazy {
        activityComponent.newPresentationComponent(PresentationModule())
    }
}