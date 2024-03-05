package com.far.menugenerator.model.common

import java.io.Serializable

data class MenuReference(
    val menuId:String,
    val firebaseRef:String?,
    val name:String,
    val online:Boolean):Serializable