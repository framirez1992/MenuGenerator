package com.far.menugenerator.model

import android.net.Uri
import java.util.UUID

data class CreateMenuState(val currentScreen:Int)

data class Item(val id:String,var category:String, val name:String, val description:String,val amount:Double,
                val localImage:Uri?=null, val remoteImage:Uri?=null)
//tempImage la que se edita, localImage la que se llena si se esta editando un item que ya estaba en un menu existente, remoteImage la imagen de firebae storage

