package com.far.menugenerator.model.database.model

import com.far.menugenerator.model.ItemStyle
import java.io.Serializable

data class MenuFirebase (var fireBaseRef:String?=null,val menuId:String="",val name:String="",val fileUrl:String="", val items:List<ItemFirebase> = emptyList()):Serializable
data class ItemFirebase(val id:String="",val type:String="",val categoryName:String="",val name:String="", val description:String?=null, val price:String?=null,var imageUrl:String?=null,val position:Int=0):Serializable