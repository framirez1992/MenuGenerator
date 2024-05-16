package com.far.menugenerator.model.firebase.firestore

import com.far.menugenerator.model.firebase.firestore.model.FirebaseCollections
import com.far.menugenerator.model.firebase.firestore.model.UserFirebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

open class UserService(private val db:FirebaseFirestore) {

    suspend fun registerUser(user: UserFirebase){
        val u = hashMapOf(
            UserFirebase::accountId.name to user.accountId,
            UserFirebase::email.name to user.email,
            UserFirebase::plan.name to user.plan,
            UserFirebase::enabled.name to user.enabled,

        )
        db.collection(FirebaseCollections.USERS).document(user.accountId!!).set(u).await()
    }
    suspend fun getUser(uid:String): UserFirebase?{
        val userFirebase = db.collection(FirebaseCollections.USERS).document(uid).get().await()
        return userFirebase?.toObject(UserFirebase::class.java)
    }
}