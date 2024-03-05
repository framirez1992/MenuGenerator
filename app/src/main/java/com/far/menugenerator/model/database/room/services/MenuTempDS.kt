package com.far.menugenerator.model.database.room.services

import com.far.menugenerator.model.database.room.RoomDB
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
import com.far.menugenerator.model.database.room.model.MenuTemp


open class MenuTempDS(private val db: RoomDB) {
    suspend fun findMenuItemsByName(type:String, name:String):List<MenuItemsTemp>{
       return db.menuTempDao().getMenuItemsByType(type).filter { it.name.lowercase() == name.lowercase() }
    }

    suspend fun addMenu(menu:MenuTemp){
        db.menuTempDao().insert(menu = menu)
    }

    suspend fun addMenuItems(menuItem:List<MenuItemsTemp>){
        db.menuTempDao().insert(entity = menuItem.toTypedArray())
    }
    suspend fun addMenuItem(menuItem:MenuItemsTemp){
        db.menuTempDao().insert(entity = menuItem)
    }

    suspend fun updateMenuItem(menuItem:MenuItemsTemp){
        db.menuTempDao().update(entity = menuItem)
    }
    suspend fun updateMenuItems(menuItems:List<MenuItemsTemp>){
        db.menuTempDao().update(entity = menuItems.toTypedArray())
    }
    suspend fun getMenuItemsByType(vararg type:String):List<MenuItemsTemp>{
        return db.menuTempDao().getMenuItemsByType(type = type)
    }
    suspend fun getMenuItemById(id:String):MenuItemsTemp{
        return db.menuTempDao().getMenuItem(id)
    }
    suspend fun getMenuItemsByMenuId(menuId:String):List<MenuItemsTemp>{
        return db.menuTempDao().getAllMenuItems().filter { it.menuId ==  menuId}
    }

    suspend fun deleteMenuItemById(menuItem:MenuItemsTemp){
        db.menuTempDao().delete(menuItem)
    }
    suspend fun deleteMenuItemById(id:String){
        db.menuTempDao().delete(id)
    }

    suspend fun clearAll() {
        db.menuTempDao().deleteAllMenu()
        db.menuTempDao().deleteAllMenuItems()
    }
}