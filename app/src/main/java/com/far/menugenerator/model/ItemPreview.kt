package com.far.menugenerator.model

import android.net.Uri
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
import java.io.Serializable

 class ItemPreview(val menuItemsTemp: MutableList<MenuItemsTemp>, val scrollToItemId:String?=null)

enum class ItemStyle{
    MENU_CATEGORY_HEADER,
    MENU_IMAGE_TITLE_DESCRIPTION_PRICE,
    MENU_TITLE_DESCRIPTION_PRICE,
    MENU_TITLE_PRICE
}
