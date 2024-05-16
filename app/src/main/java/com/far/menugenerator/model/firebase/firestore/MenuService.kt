package com.far.menugenerator.model.firebase.firestore

import com.far.menugenerator.model.firebase.firestore.model.FirebaseCollections
import com.far.menugenerator.model.firebase.firestore.model.MenuFirebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

open class MenuService(private val db:FirebaseFirestore) {

     fun saveMenu(user:String,companyId:String, m: MenuFirebase){
        //db.collection(user).document(companyId).collection("menu").document(m.menuId)
         getMenuReference(uid = user, companyId = companyId, menuId = m.menuId)
            .set(map(menu = m))
    }

    fun updateMenu(user:String,companyId:String, m: MenuFirebase){
        getMenuReference(uid = user, companyId = companyId, menuId = m.menuId)
            .set(map(menu = m))
    }

    fun deleteMenu(user:String,companyId:String, m: MenuFirebase){
        getMenuReference(uid = user, companyId = companyId, menuId = m.menuId)
            .delete()
    }

    suspend fun getMenus(user:String, companyId:String):List<MenuFirebase?>{
        //val query = db.collection(user).document(companyId).collection("menu").get().await()
        val query = db.collection(FirebaseCollections.USERS).document(user)
            .collection(FirebaseCollections.COMPANIES).document(companyId)
            .collection(FirebaseCollections.MENUS).get().await()
        val menus =  query.documents.map {doc->
            doc.toObject(MenuFirebase::class.java)
        }
        return menus
    }

    suspend fun getMenu(user:String, companyId:String, menuId:String): MenuFirebase?{
        return getMenuReference(uid = user, companyId=companyId, menuId = menuId)
            .get()
            .await()
            .toObject(MenuFirebase::class.java)
    }

    private fun map(menu: MenuFirebase):HashMap<String, Any?>{
        return hashMapOf(
            MenuFirebase::menuId.name to menu.menuId,
            MenuFirebase::menuType.name to menu.menuType,
            MenuFirebase::name.name to menu.name,
            MenuFirebase::fileUrl.name to menu.fileUrl,
            MenuFirebase::items.name to menu.items,
            MenuFirebase::menuSettings.name to menu.menuSettings,
            MenuFirebase::shorUrl.name to menu.shorUrl

        )
    }

    private fun getMenuReference(uid:String, companyId:String, menuId:String):DocumentReference{
        return db.collection(FirebaseCollections.USERS).document(uid)
            .collection(FirebaseCollections.COMPANIES).document(companyId)
            .collection(FirebaseCollections.MENUS).document(menuId)
    }
}