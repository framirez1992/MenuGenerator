package com.far.menugenerator.model.api.model

import java.util.Date

data class TinyUrlRequest(val url:String?=null,
                          val domain:String?=null,
                          val alias:String?=null,
                          val tags:String?=null,
                          val expires_at:Date?=null,
                          val description:String?=null)