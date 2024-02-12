package com.far.menugenerator.model

import android.net.Uri

data class CreateMenuState(val currentScreen:Int)

data class Item(val id:String,var category:Category, val name:String, val description:String,val amount:Double,
                val localImage:Uri?=null, val remoteImage:Uri?=null)
//tempImage la que se edita, localImage la que se llena si se esta editando un item que ya estaba en un menu existente, remoteImage la imagen de firebae storage

data class Category(val id:String, var name:String,var position:Int){
    override fun toString(): String {
        return name
    }
}
