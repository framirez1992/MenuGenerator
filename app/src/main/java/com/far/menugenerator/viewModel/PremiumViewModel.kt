package com.far.menugenerator.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.far.menugenerator.common.global.Constants
import com.far.menugenerator.common.helpers.NetworkUtils
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.model.Enums
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.model.MenuSettings
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.PurchaseService
import com.far.menugenerator.model.database.model.ItemFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.database.model.PurchaseFirebase
import com.far.menugenerator.model.database.model.PurchaseStatus
import com.far.menugenerator.model.database.room.model.MenuItems
import com.far.menugenerator.model.database.room.services.MenuDS
import com.far.menugenerator.model.storage.MenuStorage
import com.google.common.collect.ImmutableList
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Provider

class PremiumViewModel(
    private val purchaseService:PurchaseService,
    private val menuService:MenuService,
    private val menuStorage:MenuStorage,
    private val menuDS: MenuDS
):ViewModel() {

    private val TAG = "PREMIUM_ACTIVITY"

    private var _userId: String?=null
    private var _companyId:String?=null
    private var _menuId:String?=null
    private var _menuType:Enums.MenuType?=null

    val userId get() = _userId
    val companyId get() = _companyId
    val menuId get() = _menuId
    val menuType get() = _menuType


    private var _productDetails:List<ProductDetails>?=null
    private var _googlePaidPurchases:MutableList<Purchase>?=null
    private var _googlePendingPurchases:List<Purchase>?=null
    private var _googleUnknownStatusPurchase:List<Purchase>?=null
    private var _confirmedPurchases:List<PurchaseFirebase?>? =null

    val productDetails get() = _productDetails?: listOf()
    val confirmedPurchases get() = _confirmedPurchases?: listOf()
    val googlePaidPurchases get() = _googlePaidPurchases?: listOf()
    val googlePendingPurchases get() = _googlePendingPurchases?: listOf()
    val googleUnknownStatusPurchase get() = _googleUnknownStatusPurchase?: listOf()


    val initGoogleBillingDataState = MutableLiveData<ProcessState>()
    val uploadMenuStatus = MutableLiveData<ProcessState>()
    val registerPaidPurchaseStatus = MutableLiveData<ProcessState>()
    val updateConfirmPendingPurchasesStatus = MutableLiveData<ProcessState>()
    val searchConfirmedPaymentsStatus = MutableLiveData<ProcessState>()

    fun initData(userId:String,companyId: String, menuId:String, menuType:String) {
        this._userId = userId
        this._companyId = companyId
        this._menuId = menuId
        this._menuType = Enums.MenuType.valueOf(menuType)
    }

    fun initGoogleBillingData(billingClient: BillingClient){
        if(_productDetails != null
            && _googlePaidPurchases != null
            && _googlePendingPurchases != null
            && _googleUnknownStatusPurchase!= null){
            Log.d(TAG, "initGoogleBillingData Already initialized. RETURN")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {

            Log.d(TAG, "initGoogleBillingData Start..")
            if(_productDetails == null){
                val productDetailResult = queryProductDetail(billingClient = billingClient)
                if(productDetailResult.billingResult.responseCode == BillingResponseCode.OK){
                    Log.d(TAG, "initAvailableProducts:OK. productCount:${productDetailResult.productDetailsList?.size?:0}")
                    _productDetails = productDetailResult.productDetailsList!!
                }else{
                    initGoogleBillingDataState.postValue(ProcessState(State.GENERAL_ERROR, code = productDetailResult.billingResult.responseCode))
                    return@launch
                }
            }

            val purchaseResult =  searchPurchases(billingClient = billingClient)
            if(purchaseResult.billingResult.responseCode == BillingResponseCode.OK){
                updatePurchaseData(purchases = purchaseResult.purchasesList)
                initGoogleBillingDataState.postValue(ProcessState(State.SUCCESS))
            }else{
                initGoogleBillingDataState.postValue(ProcessState(State.GENERAL_ERROR, code = purchaseResult.billingResult.responseCode))
            }
        }
    }

    fun updatePurchaseData(purchases:List<Purchase>){
        Log.d(TAG, "_googlePendingPurchases searchPurchases. BillingResponseCode:OK. GooglePurchasesTotal:${purchases.size}")
        Log.d(TAG, "GooglePurchases PENDING: ${purchases.filter { it.purchaseState == PurchaseState.PENDING }.size}")
        Log.d(TAG, "GooglePurchases PURCHASED: ${purchases.filter { it.purchaseState == PurchaseState.PURCHASED }.size}")
        Log.d(TAG, "GooglePurchases UNSPECIFIED_STATE: ${purchases.filter { it.purchaseState == PurchaseState.UNSPECIFIED_STATE }.size}")

        _googlePaidPurchases = purchases.filter { it.purchaseState == PurchaseState.PURCHASED }.toMutableList()
        _googlePendingPurchases = purchases.filter { it.purchaseState ==  PurchaseState.PENDING}
        _googleUnknownStatusPurchase = purchases.filter { it.purchaseState != PurchaseState.PURCHASED && it.purchaseState !=  PurchaseState.PENDING}
    }
    fun registerGooglePaidPurchases(){
        viewModelScope.launch(Dispatchers.IO) {
            registerPaidPurchaseStatus.postValue(ProcessState(state = State.LOADING))
            try{
                registerPaidPurchasesToFirebase()
                //Transacciones que el pago ya se aprobo
                registerPaidPurchaseStatus.postValue(ProcessState(state = State.SUCCESS))
            }catch (e:Exception){
                registerPaidPurchaseStatus.postValue(ProcessState(state = State.GENERAL_ERROR))
            }
        }

    }

    private suspend fun registerPaidPurchasesToFirebase() {
        googlePaidPurchases.forEach {
            var p = purchaseService.getPurchaseByToken(_userId!!,it.purchaseToken)
            if(p == null){
                p = PurchaseFirebase(
                    status = PurchaseStatus.CONFIRMATION_PENDING.name,
                    orderId = it.orderId,
                    purchaseToken = it.purchaseToken,
                    userId = _userId,
                    companyId = _companyId,
                    createDate = Calendar.getInstance().time
                )
                purchaseService.savePurchase(p)
                Log.d(TAG,"registerPaidPurchasesToFirebase. OrderId:${it.orderId}, Token:${it.purchaseToken}")
            }
            //Limpiar Lista
            _googlePaidPurchases?.clear()
            Log.d(TAG,"clear. _googlePaidPurchases after register in firebase")
        }
    }

    fun searchPurchaseToPay() {
        viewModelScope.launch(Dispatchers.IO) {
            searchConfirmedPaymentsStatus.postValue(ProcessState(State.LOADING))
            val purchases =  purchaseService.getPurchases(user = _userId!!)
            _confirmedPurchases = purchases.filter { it?.status == PurchaseStatus.CONFIRMED.name }
            Log.d(TAG, "searchPurchaseToPay. CONFIRMED_PURCHASES:${confirmedPurchases.size}")
            searchConfirmedPaymentsStatus.postValue(ProcessState(State.SUCCESS))
        }
    }
    private suspend fun queryProductDetail(billingClient: BillingClient): ProductDetailsResult {
        Log.d(TAG, "queryProductDetail")
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    ImmutableList.of(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(Constants.IN_APP_PRODUCT_MENU_ID
                                //"base_subscription"
                            )
                            .setProductType(BillingClient.ProductType.INAPP
                                //BillingClient.ProductType.SUBS
                            )
                            .build()))
                .build()

        return billingClient.queryProductDetails(queryProductDetailsParams)

    }


    private suspend fun searchPurchases(billingClient: BillingClient): PurchasesResult {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
        // uses queryPurchasesAsync Kotlin extension function
        return billingClient.queryPurchasesAsync(params.build())
    }

    fun uploadMenu(filesDir: File?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                uploadMenuStatus.postValue(ProcessState(state = State.LOADING))

                if(!NetworkUtils.isConnectedToInternet()){
                    throw TimeoutException()
                }

                //PURCHASE PROCESSING
                val purchase = confirmedPurchases.first()!!
                //////////////////////////////////////////////////////////////

                val localMenu = menuDS.getMenuById(menuId = _menuId!!)

                val localMenuItems = menuDS.getMenuItemsByMenuId(menuId = _menuId!!)
                val localMenuSettings = Gson().fromJson(localMenu.menuSettings!!,MenuSettings::class.java)

                //Upload PDF file
                val menuStorageUrl = uploadMenuFile(user = _userId!!, menuId = localMenu.menuId, pdfPath = Uri.parse(localMenu.fileUri).path!!).toString()
                //Upload Images
                val items = prepareItemsFirebase(user = _userId!!, menuId = localMenu.menuId, items = localMenuItems)
                //Save Menu in FireBase
                val savedMenu = saveMenuFireBase(user =  _userId!!, companyId = localMenu.companyId, menuId = localMenu.menuId, menuType = _menuType!!.name,fileName = localMenu.name, fileUrl =  menuStorageUrl, items =  items, menuSettings = localMenuSettings)

                //Update purchase status
                purchase.menuId = localMenu.menuId
                purchase.status = PurchaseStatus.PURCHASED.name
                purchaseService.updatePurchase(user = _userId!!, purchase = purchase)
                //Delete all local files
                val menuFolder = File(filesDir,_menuId!!)
                FileUtils.deleteAllFilesInFolder(menuFolder)
                //Delete local menu
                menuDS.deleteMenuItemsByMenuId(menuId = _menuId!!)
                menuDS.deleteMenu(localMenu)

                uploadMenuStatus.postValue(ProcessState(state = State.SUCCESS))
            }catch (e: TimeoutException){
                uploadMenuStatus.postValue(ProcessState(State.NETWORK_ERROR))
            }
            catch (e:Exception){
                uploadMenuStatus.postValue(ProcessState(State.GENERAL_ERROR))
            }

        }
    }




    private suspend fun prepareItemsFirebase(user:String,menuId: String, items:List<MenuItems>):List<ItemFirebase>{

        //UPLOAD LOCAL IMAGES ONLY
        val firebaseItems = items.mapIndexed { _,menuItem->
            ItemFirebase(
                id = menuItem.id,
                type = menuItem.type,
                categoryId = menuItem.categoryId,
                categoryName = menuItem.categoryName,
                name = menuItem.name,
                description = menuItem.description,
                price = menuItem.price.toString(),
                position = menuItem.position,
                imageUrl = menuItem.imageUri)
        }

        firebaseItems.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && !it.imageUrl.isNullOrEmpty() }
            .forEach{
                val menuItem = items.first{ i-> i.id == it.id}
                val url = menuStorage.uploadMenuItemsImages(user,menuId,Uri.parse(menuItem.imageUri))
                it.imageUrl = url.toString()
            }
        return firebaseItems
    }
    private suspend fun uploadMenuFile(user:String,menuId:String, pdfPath:String): Uri {
        return menuStorage.uploadFile(user,menuId,pdfPath)
    }
    private fun saveMenuFireBase(user:String,companyId:String,menuId:String,menuType: String, fileName:String, fileUrl:String, items:List<ItemFirebase>,menuSettings: MenuSettings): MenuFirebase {
        val menu = MenuFirebase(menuId = menuId, menuType = menuType,name=fileName,fileUrl= fileUrl, items = items, menuSettings = menuSettings)
        menuService.saveMenu(user, companyId = companyId,menu)
        return menu
    }

    fun updateConfirmPendingPurchases(billingClient: BillingClient) {
        viewModelScope.launch(Dispatchers.IO) {
            var errorCode:Int= BillingResponseCode.ERROR
            try {
                updateConfirmPendingPurchasesStatus.postValue(ProcessState(State.LOADING))
                val purchases = purchaseService.getPurchases(user = _userId!!)

                val confirmationPendingPurchases =  purchases.filter { it?.status == PurchaseStatus.CONFIRMATION_PENDING.name }
                Log.d(TAG, "updateConfirmPendingPurchases. firebase confirmationPendingPurchases:${confirmationPendingPurchases.size}")
                confirmationPendingPurchases.forEach {
                   val consumeResult =  confirmPurchase(billingClient=billingClient,it?.purchaseToken!!)
                    if(consumeResult.billingResult.responseCode == BillingResponseCode.OK){
                        it.status = PurchaseStatus.CONFIRMED.name
                        purchaseService.updatePurchase(user = _userId!!,purchase=it)
                        Log.d(TAG, "Purchase CONFIRMED. OrderId:${it.orderId}, Token:${it.purchaseToken}")
                    }else{
                        Log.d(TAG, "Purchase CONFIRMATION ERROR. Error:${getBillingResponseErrorMessage(consumeResult.billingResult.responseCode)}")
                        errorCode = consumeResult.billingResult.responseCode
                        throw Exception(getBillingResponseErrorMessage(consumeResult.billingResult.responseCode))
                    }
                }
                updateConfirmPendingPurchasesStatus.postValue(ProcessState(State.SUCCESS))
            }catch (e:Exception){
                updateConfirmPendingPurchasesStatus.postValue(ProcessState(State.GENERAL_ERROR, code = errorCode))
            }
        }

    }

    private suspend fun confirmPurchase(billingClient: BillingClient,purchaseToken:String):ConsumeResult{
        val consumeParams =
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
       return billingClient.consumePurchase(consumeParams)
    }


    private fun getBillingResponseErrorMessage(billingResponseCode:Int):String{
        when(billingResponseCode) {
            BillingResponseCode.USER_CANCELED->{
                return "USER_CANCELED"
            }
            BillingResponseCode.BILLING_UNAVAILABLE->{
                return "BILLING_UNAVAILABLE"
            }
            BillingResponseCode.DEVELOPER_ERROR->{
                return "DEVELOPER_ERROR"
            }
            BillingResponseCode.ERROR->{
                return "ERROR"
            }
            BillingResponseCode.FEATURE_NOT_SUPPORTED->{
                return "FEATURE_NOT_SUPPORTED"
            }
            BillingResponseCode.ITEM_ALREADY_OWNED->{
                return "ITEM_ALREADY_OWNED"
            }
            BillingResponseCode.ITEM_NOT_OWNED->{
                return "ITEM_NOT_OWNED"
            }
            BillingResponseCode.ITEM_UNAVAILABLE->{
                return "ITEM_UNAVAILABLE"
            }
            BillingResponseCode.NETWORK_ERROR->{
                return "NETWORK_ERROR"
            }
            BillingResponseCode.SERVICE_DISCONNECTED->{
                return "SERVICE_DISCONNECTED"
            }
            BillingResponseCode.SERVICE_UNAVAILABLE->{
                return "SERVICE_UNAVAILABLE"
            }
            else ->{
                return "UNKNOWN_ERROR"
            }
        }
    }


    class PremiumViewModelFactory @Inject constructor(
        private val purchaseService:Provider<PurchaseService>,
        private val menuService:Provider<MenuService>,
        private val menuStorage:Provider<MenuStorage>,
        private val menuDS: Provider<MenuDS>
    ):ViewModelProvider.Factory{

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PremiumViewModel(
                purchaseService = purchaseService.get(),
                menuService = menuService.get(),
                menuStorage=menuStorage.get(),
                menuDS = menuDS.get()) as T
        }
    }
}