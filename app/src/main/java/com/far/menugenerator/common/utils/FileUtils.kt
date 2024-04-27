package com.far.menugenerator.common.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.math.min

object FileUtils {


    fun getRealFileName(contentResolver:ContentResolver, uri:Uri):String{
        var name:String="UNKNOWN"
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME))
        }
        cursor?.close()
        return name
    }
    fun getUriFileName(file: Uri):String{//Uris Locales, las remotas de firebase Storage no funciona
        val filePath = Path(file.path!!)
        return filePath.name
    }
    fun getUriFileName(path: String):String{
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

    //copia el contenido de un uri de tipo conten(content://) y lo copia en mi folder interno
    fun copyUriContentToFile(context: Context, file:Uri, outputFile: File){
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(file)
        copyStreamToFile(inputStream!!, outputFile)
        inputStream.close()
    }

    fun copyStreamToFile(inputStream: InputStream, outputFile: File) {
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
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