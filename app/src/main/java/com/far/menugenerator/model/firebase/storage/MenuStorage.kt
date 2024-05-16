package com.far.menugenerator.model.firebase.storage

import android.net.Uri
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.model.firebase.firestore.model.FirebaseFolders
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

open class MenuStorage(private val storage:FirebaseStorage) {

    suspend fun uploadFile(uid:String,companyId:String,menuId:String,pdfPath:String):Uri{

           val stream = withContext(Dispatchers.IO) {
               FileInputStream(File(pdfPath))
           }

            val  route = "${FirebaseFolders.USERS}/${uid}/${FirebaseFolders.COMPANIES}/$companyId/${FirebaseFolders.MENUS}/$menuId/${FileUtils.getUriFileName(pdfPath)}"
            val fileRef = storage.reference.child(route)
            fileRef.putStream(stream).await()
            return fileRef.downloadUrl.await()
    }

    suspend fun uploadMenuItemsImages(uid:String,companyId: String,menuId:String,file:Uri):Uri{
        val fileRef = storage.reference.child("${FirebaseFolders.USERS}/${uid}/${FirebaseFolders.COMPANIES}/$companyId/${FirebaseFolders.MENUS}/$menuId/${FirebaseFolders.ITEMS}/${FileUtils.getUriFileName(file)}")//storage.reference.child("$user/menus/$menuId/items/${FileUtils.getUriFileName(file)}")
        fileRef.putFile(file).await()
        return fileRef.downloadUrl.await()
    }

    suspend fun removeAllMenuFiles(uid:String,companyId:String,menuId:String){
        val fileRef = storage.reference.child("${FirebaseFolders.USERS}/${uid}/${FirebaseFolders.COMPANIES}/$companyId/${FirebaseFolders.MENUS}/$menuId/")
        //delete photos
        fileRef.child("${FirebaseFolders.ITEMS}/").listAll().await()?.items?.forEach {
            it.delete()
        }
        //delete PDF
        fileRef.listAll().await()?.items?.forEach {
            it.delete()
        }

    }
    suspend fun removeMenuItemsImage(uid:String,companyId: String, menuId:String, file:Uri){
        val fileRef = storage.reference.child("${FirebaseFolders.USERS}/$uid/${FirebaseFolders.COMPANIES}/${companyId}/${FirebaseFolders.MENUS}/$menuId/${FirebaseFolders.ITEMS}/${FileUtils.getUriFileName(file)}")
        fileRef.delete().await()
    }

     suspend fun downloadFileStorageReferenceFromUrl(url:String, destination:File){
         storage.getReferenceFromUrl(url).getFile(destination).await()
    }

}