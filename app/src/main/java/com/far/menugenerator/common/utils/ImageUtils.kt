package com.far.menugenerator.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri


object ImageUtils {

    fun getBitmapFromUri(context: Context, uri: Uri?, width:Float, height:Float): Bitmap? {
        // Get the BitmapFactory.Options object.
        val options = BitmapFactory.Options()

        // Set the inJustDecodeBounds field to true.
        options.inJustDecodeBounds = true

        // Decode the image from the Uri.
        BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri!!), null, options)

        // Calculate the scale factor for the image.
        val scaleFactor = Math.min(
            options.outWidth.toFloat() / width,
            options.outHeight.toFloat() / height
        )

        // Set the inSampleSize field to the scale factor.
        options.inJustDecodeBounds = false
        options.inSampleSize = scaleFactor.toInt()

        // Decode the image from the Uri again.
        return BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(uri),
            null,
            options
        )
    }

    fun getThumbnailFromImage(image: Bitmap, desiredWidth: Int, desiredHeight: Int): Bitmap {
        val scaleFactor = Math.min(desiredWidth / image.width, desiredHeight / image.height)
        val matrix = Matrix()
        matrix.postScale(scaleFactor.toFloat(), scaleFactor.toFloat())
        val thumbnail = Bitmap.createBitmap(desiredWidth, desiredHeight, Bitmap.Config.ARGB_8888) // Create a new bitmap to hold the thumbnail
        val canvas = Canvas(thumbnail)
        canvas.drawBitmap(image, matrix, null) // Draw the original image onto the new bitmap, scaled to the desired width and height
        return thumbnail
    }


}