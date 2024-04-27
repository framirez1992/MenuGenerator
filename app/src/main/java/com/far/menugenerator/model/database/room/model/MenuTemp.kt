package com.far.menugenerator.model.database.room.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.far.menugenerator.model.MenuSettings
import com.far.menugenerator.model.database.model.ItemFirebase

@Entity(tableName = "menu_temp")
data class MenuTemp(
    @PrimaryKey(autoGenerate = false) val menuId:String="",
    val menuType:String,
    var fireBaseRef:String?=null,
    val name:String="",
    val fileUrl:String="",
    val menuSettings: String?
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
    var imageUri:String?=null,
    var position:Int=0
)
