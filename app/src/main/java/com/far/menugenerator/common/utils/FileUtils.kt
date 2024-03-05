package com.far.menugenerator.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.math.min

object FileUtils {


    fun getFileName(file: Uri):String{//Uris Locales, las remotas de firebase Storage no funciona
        val filePath = Path(file.path!!)
        return filePath.name
    }
    fun getFileName(path: String):String{
        val filePath = Path(path)
        return filePath.name
    }

   suspend fun layoutToPdf(layout: View, pdfPath: String, height:Int) = withContext(Dispatchers.IO){
            // declaring width and height
            // for our PDF file.
            var pageHeight = height
            var pageWidth = layout.measuredWidth

            // Create a new PDF document.
            val pdfDocument = PdfDocument()

            val pdfInfo = PdfDocument.PageInfo.Builder(pageWidth,pageHeight,1).create()
            // Create a new PDF page.
            val pdfPage = pdfDocument.startPage(pdfInfo)

            // Draw the layout onto the PDF page.
            val canvas = pdfPage.canvas
            layout.draw(canvas)

            // Finish the PDF page.
            pdfDocument.finishPage(pdfPage)

            // Save the PDF document.
            pdfDocument.writeTo(FileOutputStream(pdfPath))

            // Close the PDF document.
            pdfDocument.close()
        }

    /*
    suspend fun generateQRCodeFromString(string: String, qrFilePath:String) {
        withContext(Dispatchers.IO){
            val background = Colors.css("#8b949e")
            val foreground = Colors.css("#0d1117")

            val fileOut = FileOutputStream(qrFilePath)

            QRCode(qrFilePath).render(
                brightColor = background, // Background
                darkColor = foreground,   // Foreground (aka the "black squares")
                marginColor = background  // Margin (ignored since margin = 0)
            ).writeImage(fileOut)
        }

    }*/
    suspend fun generateQRCode(data:String) =withContext(Dispatchers.IO){
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            return@withContext bitmap
        }


    fun getBitmapFromUri(context: Context, imageUri:Uri):Bitmap{
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(imageUri)
        return BitmapFactory.decodeStream(inputStream)
    }

    fun resizeAndSaveBitmap(context: Context, bitmap: Bitmap, targetSize: Float, targetFile: File) {
        // Calculate scaling factor if upscaling is unavoidable
        val originalWidth = bitmap.width.toFloat()
        val originalHeight = bitmap.height.toFloat()
        val requiredAspectRatio =  (originalWidth / originalHeight)

        val maxArea = min(targetSize, (originalWidth * originalHeight))
        val targetWidth = min((maxArea / requiredAspectRatio), maxArea)
        val targetHeight = (targetWidth / requiredAspectRatio)


        val w = targetWidth.toInt()
        val h = targetHeight.toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap,w, h, true)

        // Compress the resized bitmap, but avoid excessive quality loss
        val outputStream = context.openFileOutput(targetFile.name, Context.MODE_PRIVATE)

        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        outputStream.write(byteArrayOutputStream.toByteArray())

        outputStream.close()
        scaledBitmap.recycle()
    }

    fun deleteAllFilesInFolder(directory:File){
        if(directory.isDirectory)
            directory.listFiles()?.forEach {
                it.delete()
            }
    }

    fun moveFile(fileUri: Uri, directory: File, fileName:String?=null): Uri? {
        val file = fileUri.toFile()
        val newFile = File(directory, fileName?:file.name) // Create a new file object in the destination directory

        if (file.exists()) {
            if (file.renameTo(newFile)) {
                return newFile.toUri()
            } else {
                throw Exception("Error moving file!")
            }
        } else {
            throw Exception("File ${file.name} not found!")
        }
    }

    fun createDirectory(baseDirectory:File,directoryName: String):File {

        val directory = File(baseDirectory, directoryName)
        if(!directory.exists()){
            if(!directory.mkdirs()) throw Exception("Directory $directory could not be created")
        }
        return directory
    }

    fun deleteFile(fileUri: Uri) {
        val file = fileUri.toFile()
        if (file.exists()) {
            file.delete()
        }
    }


}