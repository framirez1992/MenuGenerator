package com.far.menugenerator.utils

import com.far.menugenerator.model.database.model.MenuFirebase

object MenuGenerator {
    fun generateMenus()= listOf<MenuFirebase?>(
        MenuFirebase(fireBaseRef = "ref1", menuId = "menu1", name = "menu1", fileUrl = "file1Url", items = emptyList()),
        MenuFirebase(fireBaseRef = "ref2", menuId = "menu2", name = "menu2", fileUrl = "file2Url", items = emptyList()),
        MenuFirebase(fireBaseRef = "ref3", menuId = "menu3", name = "menu3", fileUrl = "file3Url", items = emptyList())
    )
}