package com.far.menugenerator.model.storage

import android.net.Uri
import com.far.menugenerator.common.utils.FileUtils
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class CompanyStorage(private val storage: FirebaseStorage) {
    suspend fun uploadCompanyLogo(user:String,companyId:String,file: Uri): Uri {
        val fileRef = storage.reference.child("$user/company/$companyId/${FileUtils.getFileName(file)}")
        fileRef.putFile(file).await()
        return fileRef.downloadUrl.await()
    }

    suspend fun removeCompanyLogo(user:String,companyId:String,file:Uri){
        val fileRef = storage.reference.child("$user/company/$companyId/${FileUtils.getFileName(file)}")
        fileRef.delete().await()
    }
}