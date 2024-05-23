package com.far.menugenerator.common.helpers

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageOptions
import com.far.menugenerator.R
import com.far.menugenerator.view.common.BaseActivity
import com.google.firebase.ktx.BuildConfig
import java.io.File

object ActivityHelper {
    fun hideActionBar(activity:BaseActivity){
        activity.supportActionBar?.hide()
    }
    fun hideKeyboard(activity:BaseActivity, windowToken:IBinder){
        val imm = activity.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    fun callCropImage(baseActivity: BaseActivity) {
        //Implementar onActivityresult en la actividad desde dode se llame
        val intent = Intent(baseActivity, CropImageActivity::class.java)
        val options = CropImageOptions()
        options.activityBackgroundColor = baseActivity.getColor(R.color.grey_900)
        options.backgroundColor = baseActivity.getColor(R.color.grey_900)
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_OPTIONS,options)
        baseActivity.startActivityForResult(intent, BaseActivity.REQUEST_CODE_CROP_IMAGE)
    }

    fun shareFile(baseActivity: BaseActivity,file: File){
        //crear xml/paths indicando las rutasde archivos a las que vamos a dar acceso
        //Definir provider en el manifest con nuestro authority (nombre.de.paquete.fileprovider)
        //especificar en el metadata del provider las rutas a las que vamos a dar acceso

        //com.far.menugenerator.fileprovider
        val authority = "${baseActivity.applicationContext.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(baseActivity.applicationContext, authority, file)
        val mime = baseActivity.applicationContext.contentResolver.getType(uri)

        val intent = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
        //.putExtra(Intent.EXTRA_TEXT, "Check out this cool image!")

        val chooser = Intent.createChooser(intent, /* title */ null)
        try {
            baseActivity.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            // Define what your app should do if no activity can handle the intent.
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun viewFile(baseActivity: BaseActivity,file: File) {
        val authority = "${baseActivity.applicationContext.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(baseActivity.applicationContext, authority, file)
        val mime = baseActivity.applicationContext.contentResolver.getType(uri)

// Prepare an implicit intent.
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        baseActivity.startActivity(intent)

    }

    fun copyTextToClipboard(baseActivity: BaseActivity,label:String,text: String) {
        val clipboardManager = baseActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clipData)
    }
}