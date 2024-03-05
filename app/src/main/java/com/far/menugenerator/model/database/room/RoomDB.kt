package com.far.menugenerator.model.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.far.menugenerator.model.database.room.dao.MenuTempDao
import com.far.menugenerator.model.database.room.dao.MenuDao
import com.far.menugenerator.model.database.room.model.Menu
import com.far.menugenerator.model.database.room.model.MenuItems
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
import com.far.menugenerator.model.database.room.model.MenuTemp


@Database(entities = [Menu::class, MenuItems::class,MenuTemp::class,MenuItemsTemp::class], version = 1, exportSchema = true)
abstract class RoomDB: RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun menuTempDao(): MenuTempDao
}