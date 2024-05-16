package com.far.menugenerator.utils

import com.far.menugenerator.model.firebase.firestore.model.CompanyFirebase

object CompanyGenerator {
    fun getCompanies():List<CompanyFirebase?>{
        return listOf(
            CompanyFirebase(
                companyId = "1",
                businessName = "Company 1",
                phone1 = "1",
                phone2 = "1",
                phone3 = "1",
                address1 = "add1",
                address2 = "add2",
                address3 = "add3",
                facebook = "facebook",
                instagram = "instagram",
                whatsapp = "whatsapp",
                logoUrl = "http://sadasd",
                logoFileName = "name"
            ),
            CompanyFirebase(
                companyId = "2",
                businessName = "Company 2",
                phone1 = "1",
                phone2 = "1",
                phone3 = "1",
                address1 = "add1",
                address2 = "add2",
                address3 = "add3",
                facebook = "facebook",
                instagram = "instagram",
                whatsapp = "whatsapp",
                logoUrl = "http://sadasd",
                logoFileName = "name2"
            )
        )
    }
}