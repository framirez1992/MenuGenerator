package com.far.menugenerator.model.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.far.menugenerator.model.database.room.dao.MenuItemsTempDao
import com.far.menugenerator.model.database.room.dao.MenuTempDao
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
import com.far.menugenerator.model.database.room.model.MenuTemp


@Database(entities = [MenuTemp::class, MenuItemsTemp::class], version = 1, exportSchema = true)
abstract class RoomDB: RoomDatabase() {
    abstract fun menuTempDao(): MenuTempDao
    abstract fun menuItemTempDao(): MenuItemsTempDao
}