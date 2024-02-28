package com.far.menugenerator.model.database.room.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.far.menugenerator.model.MenuSettings
import com.far.menugenerator.model.database.model.ItemFirebase

@Entity(tableName = "menu_temp")
data class MenuTemp(
    @PrimaryKey(autoGenerate = false) val menuId:String,
    val name:String="",
    val fileUri:String?=null,
    val menuSettings: String? = null//json
)
@Entity(tableName = "menu_items_temp")
data class MenuItemsTemp(
    @PrimaryKey(autoGenerate = false) val id:String,
    val menuId:String="",
    val type:String="",
    val enabled:Boolean = true,
    var categoryId:String="",
    var categoryName:String="",
    var name:String="",
    val description:String="",
    val price:Double=0.0,
    var localImageUri:String?=null,
    var remoteImageUri:String?=null,
    var position:Int=0
)
