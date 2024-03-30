package com.far.menugenerator.view

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.far.menugenerator.R
import com.far.menugenerator.databinding.ActivityPremiumBinding
import com.far.menugenerator.databinding.DialogImageTitleDescriptionBinding
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.viewModel.PremiumViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class PremiumActivity : BaseActivity() {

    private val TAG = "PREMIUM_ACTIVITY"

    @Inject lateinit var dialogManager:DialogManager
    @Inject lateinit var viewModelFactory:PremiumViewModel.PremiumViewModelFactory
    private lateinit var binding:ActivityPremiumBinding
    private lateinit var viewModel:PremiumViewModel

    private var onPendingPurchaseStateReceived = false

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            Log.d(TAG,"PurchasesUpdatedListener. Purchases:${purchases?.size?:0}")
                if(billingResult.responseCode == BillingResponseCode.OK){
                    Log.d(TAG,"PurchasesUpdatedListener. BillingResponseCode:OK")
                    viewModel.updatePurchaseData(purchases = purchases!!)
                    viewModel.registerGooglePaidPurchases()
                }else{
                    processBillingResponseErrorAndClose(billingResult.responseCode)
                }


        }

    private val acknowledgePurchaseResponseListener: AcknowledgePurchaseResponseListener = AcknowledgePurchaseResponseListener{ billingResult->
        if(billingResult.responseCode == BillingResponseCode.OK){
            Log.d(TAG,"acknowledgePurchaseResponseListener OK")
            Toast.makeText(this,"Purchased", Toast.LENGTH_LONG).show()
        }else{
            Log.d(TAG,"acknowledgePurchaseResponseListener FAILED: ${billingResult.debugMessage}")
            Toast.makeText(this,billingResult.debugMessage, Toast.LENGTH_LONG).show()
        }
    }

    companion object{
        const val ARG_COMPANY_ID="COMPANY_ID"
        const val ARG_USER="USER_ID"
        const val ARG_MENU_ID="MENU_ID"
    }

    private lateinit var billingClient: BillingClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this, factory = viewModelFactory)[PremiumViewModel::class.java]

        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId:String?
        val companyId:String?
        val menuId:String?
        if(savedInstanceState == null){
            userId = intent.extras?.getString(ARG_USER)
            companyId = intent.extras?.getString(ARG_COMPANY_ID)
            menuId = intent.extras?.getString(ARG_MENU_ID)
        }else{
            userId = savedInstanceState.getString(ARG_USER)
            companyId = savedInstanceState.getString(ARG_COMPANY_ID)
            menuId = savedInstanceState.getString(ARG_MENU_ID)
        }

        if(LoginActivity.userFirebase == null || userId.isNullOrEmpty() || companyId.isNullOrEmpty() || menuId.isNullOrEmpty()){
            finish()
        }else{

            if(viewModel.userId.isNullOrEmpty() || viewModel.companyId.isNullOrEmpty() || viewModel.menuId.isNullOrEmpty()){
                viewModel.initData(userId = userId, companyId = companyId, menuId = menuId)
            }

            billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()

            initViews()
            initObservers()
            startGoogleBillingConnection()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_USER,viewModel.userId)
        outState.putString(ARG_COMPANY_ID,viewModel.companyId)
        outState.putString(ARG_MENU_ID,viewModel.menuId)
    }

    private fun initViews() {
        binding.btnBuy.setOnClickListener{
            load(isLoading = true)
            launchPurchaseFlow(viewModel.productDetails.first()) //Continuar con ciclo normal de compra
        }
        binding.btnCompletePurchase.setOnClickListener{
            viewModel.uploadMenu(applicationContext.filesDir)
        }
    }

    private fun initObservers() {
        viewModel.uploadMenuStatus.observe(this){
            processMenuUploadStatus(it)
        }
        //Solo se llama cuando el googleBillingClient carga tod lo necesario (productos, compras pendientes etc)
        viewModel.initGoogleBillingDataState.observe(this){
            if(it.state == State.SUCCESS){
                Log.d(TAG, "initGoogleBillingDataState SUCCESS")
                if(viewModel.googlePaidPurchases.isNotEmpty()){
                    Log.d(TAG, "initGoogleBillingDataState Google Paid Purchases Pending for Registration in firebase:${viewModel.googlePaidPurchases.size}")
                    //Si existen pagos PURCHASED de google pendientes de registrar en firebase y pendientes de confirmar con google debemos confirmarlos y registrarlos en firebase
                    viewModel.registerGooglePaidPurchases()
                }else if (viewModel.googlePendingPurchases.isNotEmpty()
                    || viewModel.googleUnknownStatusPurchase.isNotEmpty()) {
                    Log.d(TAG, "initGoogleBillingDataState SHOW PENDING PURCHASE DIALOG")
                    Log.d(TAG, "initGoogleBillingDataState Pending purchases:${viewModel.googlePendingPurchases.size}. Unknown Status Purchases:${viewModel.googleUnknownStatusPurchase.size}")
                    showPendingPurchaseDialog()
                }else {
                    viewModel.searchPurchaseToPay()
                }
            }else{
                processBillingResponseErrorAndClose(it.code)
            }

        }

        viewModel.registerPaidPurchaseStatus.observe(this){
                if(it.state == State.SUCCESS){
                    Log.d(TAG, "registerPaidPurchaseStatus SUCCESS.")
                    viewModel.updateConfirmPendingPurchases(billingClient = billingClient)
                }else if(it.state == State.GENERAL_ERROR){
                    Log.d(TAG, "registerPaidPurchaseStatus GENERAL_ERROR. Finish activity")
                    showToastAndFinish(getString(R.string.an_error_has_occurred_validating_the_purchase))
                }
        }

        viewModel.updateConfirmPendingPurchasesStatus.observe(this){
                if(it.state == State.SUCCESS){
                    Log.d(TAG,"updateConfirmPendingPurchasesStatus SUCCESS")
                    viewModel.searchPurchaseToPay()
                }else if(it.state == State.GENERAL_ERROR){
                    Log.d(TAG,"updateConfirmPendingPurchasesStatus GENERAL_ERROR")
                    processBillingResponseErrorAndClose(it.code)
                }
        }

        viewModel.searchConfirmedPaymentsStatus.observe(this){
                if(it.state == State.SUCCESS){
                    Log.d(TAG, "searchConfirmedPaymentsStatus SUCCESS")
                    if(viewModel.googleUnknownStatusPurchase.isNotEmpty() ||
                        viewModel.googlePendingPurchases.isNotEmpty() ||
                        viewModel.googlePaidPurchases.isNotEmpty()){
                        Log.d(TAG, "googleUnknownStatusPurchase:${viewModel.googleUnknownStatusPurchase.size}")
                        Log.d(TAG, "googlePendingPurchases:${viewModel.googlePendingPurchases.size}")
                        Log.d(TAG, "googlePaidPurchases:${viewModel.googlePaidPurchases.size}")
                        showPendingPurchaseDialog()
                    }else if(viewModel.confirmedPurchases.isNotEmpty()){//Pagos pendientes de etregar menu
                        load(false)
                        Log.d(TAG, "CONFIRMED purchases, PENDING FOR GENERATE MENU:${viewModel.confirmedPurchases.size}")
                        showCompletePaymentButtons()
                    }else{//No hay ningun tipo de pago pendiente
                        load(false)
                        Log.d(TAG, "NO PENDING purchases.")
                        showNewPaymentButtons()
                    }
                }else if(it.state == State.GENERAL_ERROR){
                    Log.d(TAG, "searchConfirmedPaymentsStatus GENERAL_ERROR")
                }
        }
    }

    private fun processMenuUploadStatus(it: ProcessState) {
            if(it.state == State.LOADING)
                load(isLoading = true)


        if(it.state == State.SUCCESS){
            finish()
        }else if(it.state == State.NETWORK_ERROR){
            load(false)
            dialogManager.showInternetErrorDialog()
        }else if(it.state == State.GENERAL_ERROR){
            load(false)
            Toast.makeText(this,getString(R.string.operation_failed_please_retry),Toast.LENGTH_LONG).show()
        }
    }




    private fun startGoogleBillingConnection(){
            Log.d(TAG, "startGoogleBillingConnection")
            load(true)
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {

                    when(billingResult.responseCode) {
                        BillingResponseCode.OK->{
                            lifecycleScope.launch(Dispatchers.IO) {
                                Log.d(TAG, "onBillingSetupFinished:OK")
                                viewModel.initGoogleBillingData(billingClient = billingClient)
                            }

                        }else->{
                            processBillingResponseErrorAndClose(billingResult.responseCode)
                        }
                    }
                }
                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                    Log.d(TAG, "onBillingServiceDisconnected")
                    startGoogleBillingConnection()
                }
            })

    }


    private fun load(isLoading: Boolean) {
        binding.pb.visibility = if(isLoading) View.VISIBLE else View.GONE
        binding.llButtons.visibility = if(isLoading) View.GONE else View.VISIBLE
    }
    private fun showNewPaymentButtons(){
        binding.btnBuy.visibility = View.VISIBLE
        binding.btnCompletePurchase.visibility =  View.GONE
    }
    private fun showCompletePaymentButtons(){
        binding.btnBuy.visibility =  View.GONE
        binding.btnCompletePurchase.visibility = View.VISIBLE
    }
    private fun showToastAndFinish(text:String){
        Toast.makeText(this,text,Toast.LENGTH_LONG).show()
        finish()
    }




    private fun launchPurchaseFlow(productDetails: ProductDetails){
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                .setProductDetails(productDetails)
                // For One-time product, "setOfferToken" method shouldn't be called.
                // For subscriptions, to get an offer token, call ProductDetails.subscriptionOfferDetails()
                // for a list of offers that are available to the user
                //.setOfferToken(productDetails.subscriptionOfferDetails!![0].offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

// Launch the billing flow
        val billingResult = billingClient.launchBillingFlow(this, billingFlowParams)
        if(billingResult.responseCode == BillingResponseCode.OK){
            //TODO not implemented
        }else{
           processBillingResponseErrorAndClose(billingResult.responseCode)
        }
    }





    private fun showPendingPurchaseDialog(){
        onPendingPurchaseStateReceived = true

        val dialogBinding = DialogImageTitleDescriptionBinding.inflate(layoutInflater)
        dialogBinding.img.setImageResource(R.drawable.pending_payment)
        dialogBinding.title.setText(R.string.pending_payment)
        dialogBinding.body.setText(R.string.the_payment_is_being_processed)
        dialogManager.showSingleButtonDialog(dialogBinding.root){
            finish()
        }
    }


    private fun processBillingResponseErrorAndClose(billingResultCode:Int){
        when(billingResultCode){
            BillingResponseCode.USER_CANCELED->{
                Log.d(TAG, "queryProductDetailsAsync:USER_CANCELED")
                finish()
            }
            BillingResponseCode.BILLING_UNAVAILABLE->{
                Log.d(TAG, "queryProductDetailsAsync:BILLING_UNAVAILABLE")
                finish()
            }
            BillingResponseCode.DEVELOPER_ERROR->{
                Log.d(TAG, "queryProductDetailsAsync:DEVELOPER_ERROR")
                showToastAndFinish(getString(R.string.operation_failed_please_retry))
            }
            BillingResponseCode.ERROR->{
                Log.d(TAG, "queryProductDetailsAsync:ERROR")
                showToastAndFinish(getString(R.string.operation_failed_please_retry))
            }
            BillingResponseCode.FEATURE_NOT_SUPPORTED->{
                Log.d(TAG, "queryProductDetailsAsync:FEATURE_NOT_SUPPORTED")
                showToastAndFinish(getString(R.string.feature_not_supported))
            }
            BillingResponseCode.ITEM_ALREADY_OWNED->{
                Log.d(TAG, "queryProductDetailsAsync:ITEM_ALREADY_OWNED")
                showToastAndFinish(getString(R.string.item_already_owned))
            }
            BillingResponseCode.ITEM_NOT_OWNED->{
                Log.d(TAG, "queryProductDetailsAsync:ITEM_NOT_OWNED")
                showToastAndFinish(getString(R.string.item_not_owned))
            }
            BillingResponseCode.ITEM_UNAVAILABLE->{
                Log.d(TAG, "queryProductDetailsAsync:ITEM_UNAVAILABLE")
                showToastAndFinish(getString(R.string.item_unavailable))
            }
            BillingResponseCode.NETWORK_ERROR->{
                Log.d(TAG, "queryProductDetailsAsync:NETWORK_ERROR")
                showToastAndFinish(getString(R.string.network_error))
            }
            BillingResponseCode.SERVICE_DISCONNECTED->{
                Log.d(TAG, "queryProductDetailsAsync:SERVICE_DISCONNECTED")
                showToastAndFinish(getString(R.string.service_disconnected))
            }
            BillingResponseCode.SERVICE_UNAVAILABLE->{
                Log.d(TAG, "queryProductDetailsAsync:SERVICE_UNAVAILABLE")
                showToastAndFinish(getString(R.string.service_unavailable))
            }

        }

    }

}