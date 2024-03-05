package com.far.menugenerator.model.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.far.menugenerator.model.database.room.model.Menu
import com.far.menugenerator.model.database.room.model.MenuItems

@Dao
interface MenuDao {
    @Insert
    suspend fun insert(entity: Menu)
    @Update
    suspend fun update(entity: Menu)
    @Delete
    suspend fun delete(entity: Menu)
    @Query("DELETE FROM menu")
    suspend fun deleteAll()

    @Query("SELECT * FROM menu")
    suspend fun getAll(): List<Menu>



    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(vararg items: MenuItems)
    @Update
    suspend fun update(vararg items:MenuItems)
    @Delete
    suspend fun delete(vararg items: MenuItems)
    @Query("DELETE FROM menu_items WHERE menuId=:menuId")
    suspend fun deleteMenuItemsByMenuId(menuId:String)
    @Query("SELECT * FROM menu_items WHERE menuId = :menuId")
    suspend fun getMenuItemsByMenuId(menuId:String):List<MenuItems>

}