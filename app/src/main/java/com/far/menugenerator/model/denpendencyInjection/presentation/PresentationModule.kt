package com.far.menugenerator.model.denpendencyInjection.presentation

import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.MenuService
import com.far.menugenerator.model.database.room.RoomDB
import com.far.menugenerator.model.database.room.services.MenuTempDS
import com.far.menugenerator.model.storage.CompanyStorage
import com.far.menugenerator.model.storage.MenuStorage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides

@Module
class PresentationModule(){

    @PresentationScope
    @Provides
    fun storageMenu(firebaseStorage:FirebaseStorage):MenuStorage = MenuStorage(firebaseStorage)
    @PresentationScope
    @Provides
    fun menuServiceFirebase(firebase:FirebaseFirestore):MenuService = MenuService(firebase)

    @PresentationScope
    @Provides
    fun companyServiceFirebase(firebase: FirebaseFirestore):CompanyService = CompanyService(firebase)
    @PresentationScope
    @Provides
    fun storageCompany(firebaseStorage:FirebaseStorage):CompanyStorage = CompanyStorage(firebaseStorage)

    @PresentationScope
    @Provides
    fun menuTempDS(roomDB: RoomDB):MenuTempDS = MenuTempDS(roomDB)
}