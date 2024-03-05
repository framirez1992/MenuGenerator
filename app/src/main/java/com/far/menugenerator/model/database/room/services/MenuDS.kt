package com.far.menugenerator.model.database.room.services

import com.far.menugenerator.model.database.room.RoomDB
import com.far.menugenerator.model.database.room.model.Menu
import com.far.menugenerator.model.database.room.model.MenuItems

open class MenuDS(private val db: RoomDB) {

    suspend fun addMenu(menu:Menu){
        db.menuDao().insert(entity = menu)
    }
    suspend fun updateMenu(menu:Menu){
        db.menuDao().update(entity = menu)
    }
    suspend fun deleteMenu(menu:Menu){
        db.menuDao().delete(entity = menu)
    }
    suspend fun addOrUpdateMenuItems(menuItems:List<MenuItems>){
        db.menuDao().insertOrReplace(items = menuItems.toTypedArray())
    }
    suspend fun updateMenuItems(menuItems:List<MenuItems>){
        db.menuDao().update(items = menuItems.toTypedArray())
    }

    suspend fun deleteMenuItems(menuItems:List<MenuItems>){
        db.menuDao().delete(items =  menuItems.toTypedArray())
    }
    suspend fun deleteMenuItemsByMenuId(menuId:String){
        db.menuDao().deleteMenuItemsByMenuId(menuId =  menuId)
    }

    suspend fun getMenuById(menuId:String):Menu{
        return db.menuDao().getAll().first{it.menuId == menuId}
    }
    suspend fun getMenusByCompanyId(companyId:String):List<Menu>{
       return db.menuDao().getAll().filter { it.companyId == companyId }
    }

    suspend fun getMenuItemsByMenuId(menuId:String):List<MenuItems>{
        return db.menuDao().getMenuItemsByMenuId(menuId = menuId)
    }
}