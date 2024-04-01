package com.far.menugenerator.model.database.model

import com.far.menugenerator.model.MenuSettings
import java.io.Serializable

data class MenuFirebase (var fireBaseRef:String?=null,val menuId:String="",val name:String="",val fileUrl:String="",var shorUrl:String?=null, val items:List<ItemFirebase> = emptyList(), val menuSettings: MenuSettings=MenuSettings()):Serializable
data class ItemFirebase(val id:String="",
                        val type:String="",
                        val enabled:Boolean = true,
                        val categoryId:String="",
                        val categoryName:String="",
                        val name:String="",
                        val description:String?=null,
                        val price:String?=null,
                        var imageUrl:String?=null,
                        val position:Int=0):Serializable

