package com.far.menugenerator.model.database

import com.far.menugenerator.model.database.model.MenuFirebase
import com.google.firebase.firestore.FirebaseFirestore

class MenuService(private val db:FirebaseFirestore) {

     fun saveMenu(user:String,menuId:String, m:MenuFirebase){

        val menu = hashMapOf(
            MenuFirebase::name.name to m.name,
            MenuFirebase::fileUrl.name to m.fileUrl,
            MenuFirebase::items.name to m.items
        )
        db.collection(user).document("menu").collection(menuId)
            .add(menu)
    }
}