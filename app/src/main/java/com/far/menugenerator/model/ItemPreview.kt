package com.far.menugenerator.model

import android.net.Uri

data class ItemPreview (val itemStyle:ItemStyle, val position:Int,val categoryName:String, val name:String,val price:String?,val description:String?, val image:Uri?)


enum class ItemStyle{
    MENU_CATEGORY_HEADER,
    MENU_IMAGE_TITLE_DESCRIPTION_PRICE,
    MENU_TITLE_DESCRIPTION_PRICE,
    MENU_TITLE_PRICE
}
