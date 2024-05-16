package com.far.menugenerator.model.firebase.firestore.model

data class UserFirebase(val accountId:String?=null,
                        val email:String?=null,
                        val plan:String= PLAN.FREE.name,
                        val enabled:Boolean = false)

enum class PLAN{
    FREE,
    PREMIUM,
    PAYASYOUGO
}