package com.far.menugenerator.model.database

import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.PurchaseFirebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class PurchaseService(private val db:FirebaseFirestore) {

    fun savePurchase(purchase: PurchaseFirebase){
        val o = purchaseToMap(purchase)
        db.collection(purchase.userId!!).document("data").collection("purchases")
            .add(o)
    }

    fun updatePurchase(user:String, purchase: PurchaseFirebase){
        purchase.updateDate = Calendar.getInstance().time
        val c = purchaseToMap(purchase)
        db.collection(user).document("data").collection("purchases").document(purchase.fireBaseRef!!)
            .set(c)
    }
    suspend fun getPurchases(user:String):List<PurchaseFirebase?>{
        val query = db.collection(user).document("data").collection("purchases").get().await()
        val purchases =  query.documents.map { doc->
            doc.toObject(PurchaseFirebase::class.java)?.apply {
                fireBaseRef = doc.reference.id
            }
        }
        return purchases
    }
    suspend fun getPurchase(user:String,purchaseRef:String): PurchaseFirebase?{
        val doc = db.collection(user).document("data").collection("purchases").document(purchaseRef).get().await()
        val purchase = doc.toObject(PurchaseFirebase::class.java)?.apply {
            fireBaseRef = doc.reference.id
        }
        return purchase
    }
    suspend fun getPurchaseByToken(user:String,token:String): PurchaseFirebase?{
        val query = db.collection(user).document("data").collection("purchases").whereEqualTo(PurchaseFirebase::purchaseToken.name,token).get().await()
        val purchaseRef = query.firstOrNull()

        val purchase = purchaseRef?.toObject(PurchaseFirebase::class.java)?.apply {
            fireBaseRef = purchaseRef.reference.id
        }
        return purchase
    }

    suspend fun getPurchaseByOrderID(user:String,orderId:String): PurchaseFirebase?{
        val query = db.collection(user).document("data").collection("purchases").whereEqualTo(PurchaseFirebase::orderId.name,orderId).get().await()
        val purchaseRef = query.firstOrNull()

        val purchase = purchaseRef?.toObject(PurchaseFirebase::class.java)?.apply {
            fireBaseRef = purchaseRef.reference.id
        }
        return purchase
    }

    fun deletePurchase(user:String, purchase: PurchaseFirebase){
        db.collection(user).document("data").collection("purchases").document(purchase.fireBaseRef!!)
            .delete()
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
}