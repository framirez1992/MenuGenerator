package com.far.menugenerator.model.denpendencyInjection.presentation

import com.far.menugenerator.model.firebase.firestore.CompanyService
import com.far.menugenerator.model.firebase.firestore.MenuService
import com.far.menugenerator.model.firebase.firestore.PurchaseService
import com.far.menugenerator.model.firebase.firestore.UserService
import com.far.menugenerator.model.database.room.RoomDB
import com.far.menugenerator.model.database.room.services.MenuDS
import com.far.menugenerator.model.database.room.services.MenuTempDS
import com.far.menugenerator.model.firebase.storage.CompanyStorage
import com.far.menugenerator.model.firebase.storage.MenuStorage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides

@Module
class PresentationModule(){

    @PresentationScope
    @Provides
    fun storageMenu(firebaseStorage:FirebaseStorage): MenuStorage = MenuStorage(firebaseStorage)
    @PresentationScope
    @Provides
    fun menuServiceFirebase(firebase:FirebaseFirestore): MenuService = MenuService(firebase)

    @PresentationScope
    @Provides
    fun companyServiceFirebase(firebase: FirebaseFirestore): CompanyService = CompanyService(firebase)
    @PresentationScope
    @Provides
    fun storageCompany(firebaseStorage:FirebaseStorage): CompanyStorage = CompanyStorage(firebaseStorage)

    @PresentationScope
    @Provides
    fun userServiceFirebase(firebase:FirebaseFirestore): UserService = UserService(firebase)
    @PresentationScope
    @Provides
    fun orderServiceFirebase(firebase:FirebaseFirestore): PurchaseService = PurchaseService(firebase)

    @PresentationScope
    @Provides
    fun menuTempDS(room: RoomDB):MenuTempDS = MenuTempDS(room)
    @PresentationScope
    @Provides
    fun menuDS(room:RoomDB):MenuDS = MenuDS(room)
}