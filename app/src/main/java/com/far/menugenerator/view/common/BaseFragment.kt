package com.far.menugenerator.view.common

import androidx.fragment.app.Fragment
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationComponent
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationModule

open class BaseFragment:Fragment() {

    val baseActivity get() = (activity as BaseActivity)
    val presentationComponent:PresentationComponent by lazy {
        baseActivity.activityComponent.newPresentationComponent(PresentationModule())
    }
}