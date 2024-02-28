package com.far.menugenerator.model

import android.net.Uri
import androidx.annotation.StyleRes
import java.io.Serializable

data class CreateMenuState(val currentScreen:Int)

data class Item(val id:String,var enabled:Boolean,var categoryId:String, val name:String, val description:String,val amount:Double,var position:Int,
                var localImage:Uri?=null, val remoteImage:Uri?=null)
//tempImage la que se edita, localImage la que se llena si se esta editando un item que ya estaba en un menu existente, remoteImage la imagen de firebae storage

data class Category(val id:String, var name:String,var position:Int){
    override fun toString(): String {
        return name
    }
}

data class ItemPreviewPosition(val id:String, val position: Int)

data class MenuSettings(
    val logoShape:LogoShape = LogoShape.NONE,
    val showLogo:Boolean = false,
    val showBusinessName:Boolean = false,
    val showAddress1:Boolean = false,
    val showAddress2:Boolean = false,
    val showAddress3:Boolean = false,
    val showPhone1:Boolean = false,
    val showPhone2:Boolean = false,
    val showPhone3:Boolean = false,
    val showFacebook:Boolean = false,
    val showInstagram:Boolean = false,
    val showWhatsapp:Boolean = false,
    val menuStyle:MenuStyle = MenuStyle.BASIC
    ) : Serializable

enum class MenuStyle{
    BASIC,
    CATALOG
}

enum class LogoShape{
    NONE,
    CIRCULAR,
    ROUNDED_CORNERS
}
