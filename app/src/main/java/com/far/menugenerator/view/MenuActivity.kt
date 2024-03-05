package com.far.menugenerator.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.far.menugenerator.R
import com.far.menugenerator.databinding.ActivityMainBinding
import com.far.menugenerator.model.common.MenuReference
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


class MenuActivity : BaseActivity() {

    @Inject lateinit var navigation:ScreenNavigation

    private lateinit var _binding:ActivityMainBinding
    private var menuReference:MenuReference?=null

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var adIsLoading: Boolean = false
    private var interstitialAd: InterstitialAd? = null
    private final val TAG = "MainActivity"

    companion object {
        const val ARG_COMPANY = "company"
        const val ARG_MENU="menu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        hideActionBar()
        val companyRef = intent.getStringExtra(ARG_COMPANY)
        menuReference = intent.getSerializableExtra(ARG_MENU) as MenuReference?

        if(companyRef == null) finish()
        navigation.createMenuFragment(companyReference = companyRef!!, menuReference = menuReference)

        initializeMobileAdsSdk()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        navigation.currentFragmentOnActivityResult(requestCode, resultCode, data)
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) { initializationStatus ->
            // Load an ad.
            loadAd()
        }
    }

    private fun loadAd() {
        // Request a new ad if one isn't already loaded.
        if (adIsLoading || interstitialAd != null) {
            return
        }
        adIsLoading = true
        val adRequest = AdManagerAdRequest.Builder().build()

        AdManagerInterstitialAd.load(
            this,
            getString(R.string.interstitial_add_id_test),
            adRequest,
            object : AdManagerInterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    interstitialAd = null
                    adIsLoading = false
                }

                override fun onAdLoaded(interstitialAd: AdManagerInterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    this@MenuActivity.interstitialAd = interstitialAd
                    adIsLoading = false
                }
            }
        )
    }


    fun showInterstitial(onFinish:()->Unit) {
        // Show the ad if it's ready. Otherwise restart the game.
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        Log.d(TAG, "Ad was dismissed.")
                        onFinish()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        interstitialAd = null
                        Log.d(TAG, "Ad failed to show.")
                        onFinish()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                    }
                }

            interstitialAd?.show(this)
        } else {
            //loadAd()
            onFinish()

        }
    }
}