package com.far.menugenerator.model.firebase.storage

import android.net.Uri
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.model.firebase.firestore.model.FirebaseFolders
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

open class CompanyStorage(private val storage: FirebaseStorage) {
    suspend fun uploadCompanyLogo(uid:String, companyId:String, file: Uri): UploadResult {
       val fileName = FileUtils.getUriFileName(file)
       val fileRef = getStorageReference(uid = uid, companyId = companyId, name = fileName)
       fileRef.putFile(file).await()
       val fileUri = fileRef.downloadUrl.await()
       return  UploadResult(fileUri =  fileUri, name = fileName)
    }

    suspend fun removeCompanyLogo(uid:String, companyId:String, remoteFileName:String){
        val fileRef = getStorageReference(uid = uid, companyId = companyId, name = remoteFileName)
        fileRef.delete().await()
    }


    private fun getStorageReference(uid:String, companyId:String, name:String):StorageReference{
        return storage.reference.child("${FirebaseFolders.USERS}/$uid/${FirebaseFolders.COMPANIES}/$companyId/${name}")
    }
}