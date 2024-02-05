package com.far.menugenerator.model

import android.net.Uri


data class Company (
                    val companyId:String,
                    val businessName:String="",
                    val phone1:String="", val phone2:String="", val phone3:String="",
                    val address1:String="", val address2:String="", val address3:String="",
                    val facebook:String="", val instagram:String="", val whatsapp:String="",
                    val remoteImage:Uri?=null)