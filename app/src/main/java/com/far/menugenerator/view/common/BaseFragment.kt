package com.far.menugenerator.view.common

import androidx.fragment.app.Fragment
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationComponent
import com.far.menugenerator.model.denpendencyInjection.presentation.PresentationModule

open class BaseFragment:Fragment() {

    val presentationComponent:PresentationComponent by lazy {
        (activity as BaseActivity).activityComponent.newPresentationComponent(PresentationModule())
    }
}