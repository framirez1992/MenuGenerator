package com.far.menugenerator.model

import android.net.Uri
import java.io.Serializable

data class ItemPreview (val item:Item,val itemStyle:ItemStyle):Serializable


enum class ItemStyle{
    MENU_CATEGORY_HEADER,
    MENU_IMAGE_TITLE_DESCRIPTION_PRICE,
    MENU_TITLE_DESCRIPTION_PRICE,
    MENU_TITLE_PRICE
}
