package com.far.menugenerator.model.database.model

data class UserFirebase(var fireBaseRef:String?=null,
                        val internalId:String?=null,
                        val accountId:String?=null,
                        val email:String?=null,
                        val plan:String=PLAN.FREE.name,
                        val enabled:Boolean = false)

enum class PLAN{
    FREE,
    PREMIUM,
    PAYASYOUGO
}