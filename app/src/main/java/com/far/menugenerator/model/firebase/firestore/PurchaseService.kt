package com.far.menugenerator.model.firebase.firestore

import com.far.menugenerator.model.firebase.firestore.model.FirebaseCollections
import com.far.menugenerator.model.firebase.firestore.model.PurchaseFirebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class PurchaseService(private val db:FirebaseFirestore) {

    fun savePurchase(purchase: PurchaseFirebase){
        getPurchaseReference(uid = purchase.userId!!, orderId = purchase.orderId!!)
            .set(purchaseToMap(purchase))
    }

    fun updatePurchase(user:String, purchase: PurchaseFirebase){
        purchase.updateDate = Calendar.getInstance().time
        getPurchaseReference(uid = user, orderId = purchase.orderId!!)
            .set(purchaseToMap(purchase))
    }
    suspend fun getPurchases(user:String):List<PurchaseFirebase?>{
        val query = db.collection(FirebaseCollections.USERS).document(user)
            .collection(FirebaseCollections.PURCHASES).get().await()

        val purchases =  query.documents.map { doc->
            doc.toObject(PurchaseFirebase::class.java)
        }
        return purchases
    }
    suspend fun getPurchaseByToken(user:String,token:String): PurchaseFirebase?{
        val query = db.collection(FirebaseCollections.USERS).document(user)
            .collection(FirebaseCollections.PURCHASES)
            .whereEqualTo(PurchaseFirebase::purchaseToken.name,token).get().await()
        val purchaseRef = query.firstOrNull()

        return purchaseRef?.toObject(PurchaseFirebase::class.java)
    }


    private fun purchaseToMap(order: PurchaseFirebase) = hashMapOf(
        PurchaseFirebase::status.name to order.status,
        PurchaseFirebase::orderId.name to order.orderId,
        PurchaseFirebase::purchaseToken.name to order.purchaseToken,
        PurchaseFirebase::userId.name to order.userId,
        PurchaseFirebase::companyId.name to order.companyId,
        PurchaseFirebase::menuId.name to order.menuId,
        PurchaseFirebase::createDate.name to order.createDate,
        PurchaseFirebase::updateDate.name to order.updateDate,
    )

    private fun getPurchaseReference(uid:String, orderId:String):DocumentReference{
       return db.collection(FirebaseCollections.USERS).document(uid)
           .collection(FirebaseCollections.PURCHASES).document(orderId)
    }
}