package com.far.menugenerator.model.denpendencyInjection.activity

import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Module
import dagger.Provides


@Module
class ActivityModule(private val activity: BaseActivity) {

    @Provides
    fun activity() = activity
    @ActivityScope
    @Provides
    fun googleSignInClient(activity:BaseActivity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        // Build a GoogleSignInClient with the options specified by gso.
        return GoogleSignIn.getClient(activity, gso);
    }
    @ActivityScope
    @Provides
    fun screenNavigation(activity: BaseActivity) = ScreenNavigation(activity)

}