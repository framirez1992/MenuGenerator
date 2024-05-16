package com.far.menugenerator.model.firebase.firestore

import com.far.menugenerator.model.firebase.firestore.model.FirebaseCollections
import com.far.menugenerator.model.firebase.firestore.model.CompanyFirebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
 class CompanyService(private val db: FirebaseFirestore) {
    fun saveCompany(user:String, company: CompanyFirebase){
        //db.collection(user).document("data").collection("company").document(company.companyId)
        //    .set(c)
        getCompanyReference(uid = user, companyId = company.companyId)
            .set(companyToMap(company))
    }

     fun updateCompany(user:String, company: CompanyFirebase){
        //db.collection(user).document("data").collection("company").document(company.companyId)
        //    .set(c)
         getCompanyReference(uid = user, companyId = company.companyId)
             .set(companyToMap(company))
    }
     suspend fun getCompanies(user:String):List<CompanyFirebase?>{
        //val query = db.collection(user).document("data").collection("company").get().await()
         val query = db.collection(FirebaseCollections.USERS).document(user).collection(
             FirebaseCollections.COMPANIES).get().await()
        val companies =  query.documents.map { doc->
            doc.toObject(CompanyFirebase::class.java)
        }
        return companies
    }
     suspend fun getCompany(user:String, companyId:String): CompanyFirebase?{
         //val doc = db.collection(user).document("data").collection("company").document(companyId).get().await()
         val doc = getCompanyReference(uid = user, companyId = companyId).get().await()
         return doc.toObject(CompanyFirebase::class.java)
     }

     fun deleteCompany(user:String, company: CompanyFirebase){
        //db.collection(user).document("data").collection("company").document(company.companyId)
        //    .delete()
         getCompanyReference(uid = user, companyId = company.companyId)
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


     private fun getCompanyReference(uid:String, companyId:String):DocumentReference{
         return db.collection(FirebaseCollections.USERS).document(uid)
             .collection(FirebaseCollections.COMPANIES).document(companyId)
     }

}