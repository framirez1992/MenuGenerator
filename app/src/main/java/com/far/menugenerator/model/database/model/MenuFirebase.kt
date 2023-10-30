package com.far.menugenerator.model.database.model

import com.far.menugenerator.model.ItemStyle

data class MenuFirebase (val name:String,val fileUrl:String, val items:List<ItemFirebase>)
data class ItemFirebase(val type:String,val categoryName:String,val name:String, val description:String?, val price:String?,var imageUrl:String?,val position:Int)