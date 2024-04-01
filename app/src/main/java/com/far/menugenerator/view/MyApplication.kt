package com.far.menugenerator.view

import android.app.Application
import com.far.menugenerator.R
import com.far.menugenerator.model.denpendencyInjection.application.AppComponent
import com.far.menugenerator.model.denpendencyInjection.application.ApplicationModule
import com.far.menugenerator.model.denpendencyInjection.application.DaggerAppComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MyApplication: Application() {

    lateinit var appComponent:AppComponent
    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent
            .builder().applicationModule(ApplicationModule(this))
            .build()
    }
}