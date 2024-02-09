package com.far.menugenerator.model.storage

import android.net.Uri
import com.far.menugenerator.common.utils.FileUtils
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

open class MenuStorage(private val storage:FirebaseStorage) {

    suspend fun uploadFile(user:String,menuId:String,pdfPath:String):Uri{

           val stream = withContext(Dispatchers.IO) {
               FileInputStream(File(pdfPath))
           }

            val  route = "$user/menus/$menuId/${FileUtils.getFileName(pdfPath)}"
            val fileRef = storage.reference.child(route)
            fileRef.putStream(stream).await()
            return fileRef.downloadUrl.await()
    }

    suspend fun uploadMenuItemsImages(user:String,menuId:String,file:Uri):Uri{
        val fileRef = storage.reference.child("$user/menus/$menuId/items/${FileUtils.getFileName(file)}")
        fileRef.putFile(file).await()
        return fileRef.downloadUrl.await()
    }

    suspend fun removeAllMenuFiles(user:String,menuId:String){
        val fileRef = storage.reference.child("$user/menus/$menuId/")
        //delete photos
        fileRef.child("items/").listAll().await()?.items?.forEach {
            it.delete()
        }
        //delete PDF
        fileRef.listAll().await()?.items?.forEach {
            it.delete()
        }

    }
    suspend fun removeMenuItemsImages(user:String,menuId:String,file:Uri){
        val fileRef = storage.reference.child("$user/menus/$menuId/items/${FileUtils.getFileName(file)}")
        fileRef.delete().await()
    }

     suspend fun downloadFileStorageReferenceFromUrl(url:String, destination:File){
         storage.getReferenceFromUrl(url).getFile(destination).await()
    }

}