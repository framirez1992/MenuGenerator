package com.far.menugenerator.model.denpendencyInjection.application

import com.far.menugenerator.view.MyApplication
import dagger.Module
import dagger.Provides

@Module
class ApplicationModule(private val application:MyApplication) {

    @Provides
    @AppScope
    fun application() = application

}