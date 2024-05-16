package com.far.menugenerator.model.firebase.firestore.model

import java.util.Date

data class PurchaseFirebase(
    var status:String?=null,
    var message:String?=null,
    var orderId:String?=null,
    val purchaseToken:String?=null,
    var userId:String?=null,
    var companyId:String?=null,
    var menuId:String?=null,
    var createDate:Date? = null,
    var updateDate:Date? = null
    )

enum class PurchaseStatus{
    CONFIRMATION_PENDING, //Confirmacion del pago a google.
    CONFIRMATION_ERROR,//Error al confirmar
    CONFIRMED,//Pendingte de entregar menu al usuario
    PURCHASED,//Menu entregado
    PROCESSING, //Procesando menu

}