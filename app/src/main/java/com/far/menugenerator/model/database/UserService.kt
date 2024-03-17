package com.far.menugenerator.model.database

import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.model.database.model.UserFirebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

open class UserService(private val db:FirebaseFirestore) {

    suspend fun registerUser(user: UserFirebase){
        val u = hashMapOf(
            UserFirebase::accountId.name to user.accountId,
            UserFirebase::email.name to user.email,
            UserFirebase::plan.name to user.plan
        )
        val ref = db.collection("users").add(u).await()
        user.fireBaseRef = ref.id
    }
    suspend fun getUser(userEmail:String):UserFirebase?{
        val menu = db.collection("users").whereEqualTo(UserFirebase::email.name,userEmail)
        val userFirebase = menu.get().await().firstOrNull()
        return userFirebase?.toObject(UserFirebase::class.java)?.apply { fireBaseRef = userFirebase.id}
    }
}