package com.far.menugenerator.model.database

import com.far.menugenerator.model.database.model.MenuFirebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

open class MenuService(private val db:FirebaseFirestore) {

     fun saveMenu(user:String,companyId:String, m:MenuFirebase){

        val menu = hashMapOf(
            MenuFirebase::menuId.name to m.menuId,
            MenuFirebase::name.name to m.name,
            MenuFirebase::fileUrl.name to m.fileUrl,
            MenuFirebase::items.name to m.items,
            MenuFirebase::menuSettings.name to m.menuSettings,
            MenuFirebase::shorUrl.name to m.shorUrl

        )
        db.collection(user).document(companyId).collection("menu")
            .add(menu)
    }

    fun updateMenu(user:String,companyId:String, m:MenuFirebase){

        val menu = hashMapOf(
            MenuFirebase::menuId.name to m.menuId,
            MenuFirebase::name.name to m.name,
            MenuFirebase::fileUrl.name to m.fileUrl,
            MenuFirebase::items.name to m.items,
            MenuFirebase::menuSettings.name to m.menuSettings,
            MenuFirebase::shorUrl.name to m.shorUrl
        )
        db.collection(user).document(companyId).collection("menu").document(m.fireBaseRef!!)
            .set(menu)
    }

    fun deleteMenu(user:String,companyId:String, m:MenuFirebase){
        db.collection(user).document(companyId).collection("menu").document(m.fireBaseRef!!)
            .delete()
    }

    suspend fun getMenus(user:String, companyId:String):List<MenuFirebase?>{
        val query = db.collection(user).document(companyId).collection("menu").get().await()
        val menus =  query.documents.map {doc->
            doc.toObject(MenuFirebase::class.java)?.apply {
                fireBaseRef = doc.reference.id
            }
        }
        return menus
    }

    suspend fun getMenu(user:String, companyId:String, firebaseRef:String):MenuFirebase?{
        val menu = db.collection(user).document(companyId).collection("menu").document(firebaseRef).get().await()
        val menuFireBase = menu.toObject(MenuFirebase::class.java)?.apply {
            fireBaseRef = menu.reference.id
        }
        return menuFireBase
    }
}