package com.far.menugenerator.model.database.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu")
data class Menu(
    @PrimaryKey(autoGenerate = false) val menuId:String,
    val menuType:String,
    val companyId:String,
    val name:String="",
    val fileUri:String?=null,
    val menuSettings: String? = null//json
)

@Entity(tableName = "menu_items")
data class MenuItems(
    @PrimaryKey(autoGenerate = false) val id:String,
    val menuId:String,
    val type:String="",
    val enabled:Boolean = true,
    val categoryId:String="",
    val categoryName:String="",
    val name:String="",
    val description:String?=null,
    val price:String?=null,
    var imageUri:String?=null,
    val position:Int=0
)