package com.far.menugenerator.model

import android.net.Uri

data class CreateMenuState(val currentScreen:Int,
                           val categories:MutableList<String> = mutableListOf(),
                           val items:MutableList<Item> = mutableListOf())

data class Item(val category:String, val name:String, val description:String,val amount:Double, val image:Uri?=null)

