package com.far.menugenerator.model.database.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.far.menugenerator.model.database.room.model.MenuTemp

@Dao
interface MenuTempDao {
    @Insert
    suspend fun insert(entity: MenuTemp)

    @Update
    suspend fun update(entity: MenuTemp)

    @Delete
    suspend fun delete(entity: MenuTemp)

    @Query("DELETE FROM menu_temp")
    suspend fun deleteAll()

    @Query("SELECT * FROM menu_temp")
    suspend fun getAll(): List<MenuTemp>



}