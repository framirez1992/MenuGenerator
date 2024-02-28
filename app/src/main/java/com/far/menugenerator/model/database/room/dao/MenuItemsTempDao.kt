package com.far.menugenerator.model.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.far.menugenerator.model.database.room.model.MenuItemsTemp

@Dao
interface MenuItemsTempDao {

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

    @Query("DELETE FROM menu_items_temp")
    suspend fun deleteAll()

    @Query("SELECT * FROM menu_items_temp where id = :id")
    suspend fun getMenuItem(id:String):MenuItemsTemp
    @Query("SELECT * FROM menu_items_temp")
    suspend fun getAllMenuItems(): List<MenuItemsTemp>

    @Query("SELECT * FROM menu_items_temp where type in (:type)")
    suspend fun getMenuItemsByType(vararg type:String): List<MenuItemsTemp>
}