package com.far.menugenerator.model.denpendencyInjection.application

import androidx.room.Room
import androidx.room.RoomDatabase
import com.far.menugenerator.model.database.room.RoomDB
import com.far.menugenerator.view.MyApplication
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides

@Module
class ApplicationModule(private val application:MyApplication) {

    @Provides
    @AppScope
    fun application() = application
    @Provides
    @AppScope
    fun firebaseStorage() = FirebaseStorage.getInstance()

    @Provides
    @AppScope
    fun firebaseFireStore() = Firebase.firestore

    @Provides
    @AppScope
    fun roomDatabase():RoomDB = Room.databaseBuilder(
        context = application,
        klass = RoomDB::class.java,
        name= "menu_generator.db")
        .build()

}