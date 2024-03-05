package com.far.menugenerator.model.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
import com.far.menugenerator.model.database.room.model.MenuTemp

@Dao
interface MenuTempDao {

    @Insert
    suspend fun insert(menu:MenuTemp)
    @Insert
    suspend fun insert(entity: MenuItemsTemp)
    @Insert
    suspend fun insert(vararg entity: MenuItemsTemp)
    @Update
    suspend fun update(entity: MenuItemsTemp)
    @Update
    suspend fun update(vararg entity: MenuItemsTemp)

    @Delete
    suspend fun delete(entity: MenuItemsTemp)
    @Query("DELETE FROM menu_items_temp WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM menu_temp")
    suspend fun deleteAllMenu()
    @Query("DELETE FROM menu_items_temp")
    suspend fun deleteAllMenuItems()

    @Query("SELECT * FROM menu_items_temp where id = :id")
    suspend fun getMenuItem(id:String):MenuItemsTemp
    @Query("SELECT * FROM menu_items_temp")
    suspend fun getAllMenuItems(): List<MenuItemsTemp>

    @Query("SELECT * FROM menu_items_temp where type in (:type)")
    suspend fun getMenuItemsByType(vararg type:String): List<MenuItemsTemp>
}