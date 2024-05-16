package com.far.menugenerator.utils

import com.far.menugenerator.model.firebase.firestore.model.MenuFirebase

object MenuGenerator {
    fun generateMenus()= listOf<MenuFirebase?>(
        MenuFirebase(menuId = "menu1", name = "menu1", fileUrl = "file1Url", items = emptyList()),
        MenuFirebase(menuId = "menu2", name = "menu2", fileUrl = "file2Url", items = emptyList()),
        MenuFirebase(menuId = "menu3", name = "menu3", fileUrl = "file3Url", items = emptyList())
    )
}