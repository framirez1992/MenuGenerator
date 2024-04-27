package com.far.menugenerator.model.denpendencyInjection.activity

import androidx.credentials.CredentialManager
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import dagger.Module
import dagger.Provides


@Module
class ActivityModule(private val activity: BaseActivity) {

    @Provides
    fun activity() = activity
    @ActivityScope
    @Provides
    fun googleCredentialManager(activity: BaseActivity):CredentialManager =  CredentialManager.create(activity)

    @ActivityScope
    @Provides
    fun screenNavigation(activity: BaseActivity) = ScreenNavigation(activity)
    @ActivityScope
    @Provides
    fun dialogManager(activity: BaseActivity) = DialogManager(activity)

}