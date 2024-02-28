package com.far.menugenerator.model.database.room.services

import com.far.menugenerator.model.database.room.RoomDB
import com.far.menugenerator.model.database.room.model.MenuItemsTemp


open class MenuTempDS(private val db: RoomDB) {
    suspend fun addCategory(menuItemTemp:MenuItemsTemp){
        db.menuItemTempDao().insert(menuItemTemp)
    }
    suspend fun findMenuItemByName(type:String, name:String):MenuItemsTemp?{
       return db.menuItemTempDao().getMenuItemsByType(type).firstOrNull { it.name.lowercase() == name.lowercase() }
    }

    suspend fun addMenuItems(menuItem:List<MenuItemsTemp>){
        db.menuItemTempDao().insert(entity = menuItem.toTypedArray())
    }
    suspend fun addMenuItem(menuItem:MenuItemsTemp){
        db.menuItemTempDao().insert(entity = menuItem)
    }

    suspend fun updateMenuItem(menuItem:MenuItemsTemp){
        db.menuItemTempDao().update(entity = menuItem)
    }
    suspend fun updateMenuItems(menuItems:List<MenuItemsTemp>){
        db.menuItemTempDao().update(entity = menuItems.toTypedArray())
    }
    suspend fun getMenuItemsByType(vararg type:String):List<MenuItemsTemp>{
        return db.menuItemTempDao().getMenuItemsByType(type = type)
    }
    suspend fun getMenuItemById(id:String):MenuItemsTemp{
        return db.menuItemTempDao().getMenuItem(id)
    }

    suspend fun deleteMenuItemById(id:String){
        db.menuItemTempDao().delete(id)
    }

    suspend fun clearAll() {
        db.menuItemTempDao().deleteAll()
        db.menuTempDao().deleteAll()
    }
}