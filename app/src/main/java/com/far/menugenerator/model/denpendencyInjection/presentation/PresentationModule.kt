package com.far.menugenerator.model.denpendencyInjection.presentation

import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.storage.StorageMenu
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides

@Module
class PresentationModule(){

    @PresentationScope
    @Provides
    fun storageMenu(firebaseStorage:FirebaseStorage) = StorageMenu(firebaseStorage)
    @PresentationScope
    @Provides
    fun menuServiceFirebase(firebase:FirebaseFirestore) = MenuService(firebase)
}