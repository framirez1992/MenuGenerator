package com.far.menugenerator.model.storage

import android.net.Uri
import com.far.menugenerator.common.utils.FileUtils
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

open class CompanyStorage(private val storage: FirebaseStorage) {
    suspend fun uploadCompanyLogo(user:String,companyId:String,file: Uri): UploadResult {
       val fileName = FileUtils.getUriFileName(file)
       val fileRef = storage.reference.child("$user/company/$companyId/${fileName}")
       fileRef.putFile(file).await()
       val fileUri = fileRef.downloadUrl.await()
       return  UploadResult(fileUri =  fileUri, name = fileName)
    }

    suspend fun removeCompanyLogo(user:String,companyId:String,remoteFileName:String){
        val fileRef = storage.reference.child("$user/company/$companyId/${remoteFileName}")
        fileRef.delete().await()
    }

}