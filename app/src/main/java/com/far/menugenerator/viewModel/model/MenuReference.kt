package com.far.menugenerator.viewModel.model

import java.io.Serializable

data class MenuReference(
    val menuId:String,
    val menuType:String,
    val name:String,
    val fileUri:String,
    val online:Boolean,
    val isDemo:Boolean):Serializable