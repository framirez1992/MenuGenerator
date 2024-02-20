package com.far.menugenerator.common.utils

import android.content.Context
import android.util.TypedValue

object PixelUtils {
    fun dpToPx(dp: Float, context: Context): Float {
        val metrics = context.resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics)
    }
    fun pxToDp(px: Float, context: Context): Float {
        val metrics = context.resources.displayMetrics
        val density = metrics.density // Get the screen density
        return px / density // Approximate dp based on density
    }
}