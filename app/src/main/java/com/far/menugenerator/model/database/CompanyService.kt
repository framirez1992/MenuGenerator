package com.far.menugenerator.model.database

import com.far.menugenerator.model.database.model.CompanyFirebase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
 class CompanyService(private val db: FirebaseFirestore) {
    fun saveCompany(user:String, company: CompanyFirebase){
        val c = companyToMap(company)
        db.collection(user).document("data").collection("company")
            .add(c)
    }

     fun updateCompany(user:String, company: CompanyFirebase){
        val c = companyToMap(company)
        db.collection(user).document("data").collection("company").document(company.fireBaseRef!!)
            .set(c)
    }
     suspend fun getCompanies(user:String):List<CompanyFirebase?>{
        val query = db.collection(user).document("data").collection("company").get().await()
        val companies =  query.documents.map { doc->
            doc.toObject(CompanyFirebase::class.java)?.apply {
            fireBaseRef = doc.reference.id
            }
        }
        return companies
    }

     fun deleteCompany(user:String, company: CompanyFirebase){
        db.collection(user).document("data").collection("company").document(company.fireBaseRef!!)
            .delete()
    }
    private fun companyToMap(company: CompanyFirebase) = hashMapOf(
            CompanyFirebase::companyId.name to company.companyId,
            CompanyFirebase::businessName.name to company.businessName,
            CompanyFirebase::phone1.name to company.phone1,
            CompanyFirebase::phone2.name to company.phone2,
            CompanyFirebase::phone3.name to company.phone3,
            CompanyFirebase::address1.name to company.address1,
            CompanyFirebase::address2.name to company.address2,
            CompanyFirebase::address3.name to company.address3,
            CompanyFirebase::facebook.name to company.facebook,
            CompanyFirebase::instagram.name to company.instagram,
            CompanyFirebase::whatsapp.name to company.whatsapp,
            CompanyFirebase::logoUrl.name to company.logoUrl,
            CompanyFirebase::logoFileName.name to company.logoFileName
        )


}