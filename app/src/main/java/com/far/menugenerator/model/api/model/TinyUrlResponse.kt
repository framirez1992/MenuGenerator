package com.far.menugenerator.model.api.model

import java.util.Date

data class TinyUrlResponse (val data:TinyUrlData?=null, val code:Int=0,val errors:List<String> = emptyList())

data class TinyUrlData(
    val domain:String?=null,
    val alias:String?=null,
    val deleted:Boolean=false,
    val archived:Boolean=false,
    val created_at:Date?=null,
    val expires_at:Date?=null,
    val tiny_url:String?=null,
    val url:String?=null)