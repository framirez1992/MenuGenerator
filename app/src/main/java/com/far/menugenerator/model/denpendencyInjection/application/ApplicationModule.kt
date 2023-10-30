package com.far.menugenerator.model.denpendencyInjection.application

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

}