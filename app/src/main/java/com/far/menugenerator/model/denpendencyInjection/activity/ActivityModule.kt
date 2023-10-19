package com.far.menugenerator.model.denpendencyInjection.activity

import com.far.menugenerator.view.common.BaseActivity
import dagger.Module
import dagger.Provides


@Module
class ActivityModule(private val activity: BaseActivity) {

    @Provides
    fun activity() = activity

}