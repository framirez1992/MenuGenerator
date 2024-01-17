package com.far.menugenerator.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.view.View
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.github.g0dkar.qrcode.QRCode
import io.github.g0dkar.qrcode.render.Colors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import kotlin.io.path.Path
import kotlin.io.path.name

object FileUtils {


    fun getFileName(file: Uri):String{//Uris Locales, las remotas de firebase Storage no funciona
        val filePath = Path(file.path!!)
        return filePath.name
    }
    fun getFileName(path: String):String{
        val filePath = Path(path)
        return filePath.name
    }
    fun getDownloadsPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
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



}