package com.far.menugenerator.common.global

import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.ktx.BuildConfig

object Constants {
    const val USERID_DEMO="ftUIocmQdGMl1rZMEylQk0QrzBa2"
    const val COMPANYID_DEMO="873e1237-4e21-48a5-93be-7db45d6e7306"

    const val PDF_FILE_NAME="document.pdf"
    //OJO USAR SIEMPRE LA WEB (FUNCIONA PARA DEBUG Y PARA RELEASE)
    //release_android
    //const val ANDROID_OAUTH_CLIENT_ID="1025547524096-8iikvuajim3ep8qo2m59ll1o368emnq8.apps.googleusercontent.com"
    //debug_android
    //const val ANDROID_OAUTH_CLIENT_ID="1025547524096-0c3en766snnolcmpsij9kkma192ho0i7.apps.googleusercontent.com"
    //web_client
    const val ANDROID_OAUTH_CLIENT_ID="1025547524096-1d3jb0ekfqkqgbvu9fnrsqk1n0ksn0pn.apps.googleusercontent.com"

    const val IN_APP_PRODUCT_MENU_ID="single_virtual_menu"
    const val TYNY_URL_ENDPOINT = "https://api.tinyurl.com"
    const val TYNY_URL_TOKEN="np97L2ZzMLkPPhJhbtWFkjFLHpZQcCdygPGKksE5hb7hJqVtsAl7nCalTt25"

    //const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"//TEST
    const val INTERSTITIAL_AD_ID = "ca-app-pub-8775587472639334/9465721003"//PROD


    fun getAppVersion(context:Context):String{
        val packageName = context.packageName
        val pm = context.packageManager
        var versionName = "?"

        try {
            val packageInfo = pm.getPackageInfo(packageName, 0)
            versionName = packageInfo.versionName
            return versionName
        } catch (e: PackageManager.NameNotFoundException) {
            //Log.e("Version Error", "Package name not found", e)
        }
        return versionName;
    }


}