package com.far.menugenerator.model.storage

import android.net.Uri
import android.os.Environment
import androidx.core.net.toFile
import com.far.menugenerator.common.utils.FileUtils
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

class StorageMenu(private val storage:FirebaseStorage) {

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

}