package com.far.menugenerator.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.far.menugenerator.R
import com.far.menugenerator.model.Company
import com.far.menugenerator.model.CompanyState
import com.far.menugenerator.model.database.CompanyService
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.storage.CompanyStorage
import com.far.menugenerator.model.storage.UploadResult
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

class CompanyViewModel @Inject constructor(
    private val companyService:CompanyService,
    private val companyStorage:CompanyStorage
): ViewModel() {

    private val _state = MutableLiveData<CompanyState>()
    val state:LiveData<CompanyState> = _state

    val currentImage = MutableLiveData<Uri?>()
    val company = MutableLiveData<Company>()
    private var editCompany:CompanyFirebase?=null


    val isLoading = MutableLiveData<Boolean>()

    init {
        _state.value = CompanyState(currentScreen= R.id.layoutCompanyName, isLoading = false)
        company.value = Company(companyId = UUID.randomUUID().toString())
        currentImage.value = null
    }

    fun prepareCompanyEdit(comp:CompanyFirebase?){
        if(editCompany != null || comp == null) return

        editCompany = comp
        val remoteImage = if(comp.logoUrl != null) Uri.parse(comp.logoUrl) else null
        company.value = Company(companyId = comp.companyId,
            businessName = comp.businessName, phone1 = comp.phone1, phone2 = comp.phone2, phone3 = comp.phone3,
            address1 = comp.address1, address2 = comp.address2, address3 = comp.address3,
            facebook = comp.facebook, instagram = comp.instagram, whatsapp = comp.whatsapp,
            remoteImage = remoteImage)

        currentImage.value = remoteImage
    }

    fun nextScreen(){
        val nextScreen = when (_state.value?.currentScreen){
            R.id.layoutCompanyName-> R.id.layoutCompanyAddress
            R.id.layoutCompanyAddress-> R.id.layoutCompanyContact
            else -> R.id.layoutCompanyLogo
        }
        _state.value = _state.value?.copy(currentScreen = nextScreen)

    }
    fun previousScreen(){
        val previousScreen = when (_state.value?.currentScreen){
            R.id.layoutCompanyLogo-> R.id.layoutCompanyContact
            R.id.layoutCompanyContact-> R.id.layoutCompanyAddress
            else -> R.id.layoutCompanyName
        }
        _state.value = _state.value?.copy(currentScreen = previousScreen)
    }


    fun updateCurrentCompanyLogo(imageUri:Uri?){
        currentImage.postValue(imageUri)
    }
    fun setCompanyData(companyName: String,
                       phone1: String,phone2: String,phone3: String,
                       address1: String,address2: String,address3: String,
                       facebook:String,instagram:String, whatsapp:String){
        val c = company.value?.copy(
            businessName = companyName,
            phone1=phone1,phone2=phone2,phone3=phone3,
            address1 = address1,address2=address2, address3=address3,
            facebook=facebook,instagram=instagram,whatsapp=whatsapp)

        company.postValue(c!!)
    }
    fun saveChanges(user:String,
                    companyName: String,
                    phone1: String,phone2: String,phone3: String,
                    address1: String,address2: String,address3: String,
                    facebook:String,instagram:String, whatsapp:String){
        val c = company.value!!.copy(
            businessName = companyName,
            phone1=phone1,phone2=phone2,phone3=phone3,
            address1 = address1,address2=address2, address3=address3,
            facebook=facebook,instagram=instagram,whatsapp=whatsapp)

        if(editCompany == null)
            saveCompany(user = user,company= c)
        else
            updateCompany(user = user,company= c)
    }
    private fun saveCompany(user:String,company:Company){

        viewModelScope.launch {
            _state.postValue(_state.value?.copy(isLoading = true))
            try {

                var uploadedFile:UploadResult? = null
                if(currentImage.value != null) {
                    uploadedFile = companyStorage.uploadCompanyLogo(
                        user = user,
                        companyId = company.companyId,
                        file = currentImage.value!!
                    )
                }

                val firebaseCompany = CompanyFirebase(companyId = company.companyId,businessName = company.businessName,
                    phone1 = company.phone1, phone2 = company.phone2, phone3 = company.phone3,
                    address1 = company.address1, address2 = company.address2, address3 = company.address3,
                    facebook = company.facebook, instagram = company.instagram, whatsapp = company.whatsapp,
                    logoUrl = uploadedFile?.fileUri?.toString(), logoFileName = uploadedFile?.name)
                companyService.saveCompany(user = user,firebaseCompany)
            }catch (e:Exception){
                e.printStackTrace()
            }
            _state.postValue(_state.value?.copy(isLoading = false))

        }

    }

    private fun updateCompany(user:String,company: Company){

        viewModelScope.launch {
            _state.postValue(_state.value?.copy(isLoading = true))
            try {
                deleteUnusedImagesFromFireStore(user=user, companyId = company.companyId)
                val uploadResult:UploadResult? = if(currentImage.value != null && editCompany?.logoUrl!=null &&  currentImage.value == Uri.parse(editCompany?.logoUrl )){//no se modifico la imagen que tenia (Dejar igual)
                    UploadResult(fileUri = Uri.parse(editCompany!!.logoUrl), name = editCompany?.logoFileName)
                }else if(currentImage.value != null && editCompany?.logoUrl==null){//No tenia imagen y se le esta agregando una ahora
                    companyStorage.uploadCompanyLogo(user = user, companyId = company.companyId, file = currentImage.value!!)
                }else if(currentImage.value != null && currentImage.value != Uri.parse(editCompany?.logoUrl )){//la imagen original cambio porque no es la misma que se cargo en currentImage.value
                    companyStorage.uploadCompanyLogo(user = user, companyId = company.companyId, file = currentImage.value!!)
                }else{//no imagen
                    null
                }

                val firebaseCompany = CompanyFirebase(companyId = company.companyId,businessName = company.businessName,
                    phone1 = company.phone1, phone2 = company.phone2, phone3 = company.phone3,
                    address1 = company.address1, address2 = company.address2, address3 = company.address3,
                    facebook = company.facebook, instagram = company.instagram, whatsapp = company.whatsapp,
                    logoUrl = uploadResult?.fileUri?.toString(), logoFileName = uploadResult?.name, fireBaseRef = editCompany!!.fireBaseRef)
                companyService.updateCompany(user = user, company =  firebaseCompany)
            }catch (e:Exception){
                e.printStackTrace()
            }

            _state.postValue(_state.value?.copy(isLoading = false))

        }

    }

    private suspend fun deleteUnusedImagesFromFireStore(user:String,companyId:String){
        if(editCompany?.logoUrl != null && (currentImage.value ==null || currentImage.value != Uri.parse(editCompany?.logoUrl))){
            companyStorage.removeCompanyLogo(user =  user,companyId= companyId, remoteFileName = editCompany?.logoFileName!!)
        }
    }


    class CompanyViewModelFactory @Inject constructor (
        private val companyService: Provider<CompanyService>,
        private val companyStorage: Provider<CompanyStorage>
    ):ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CompanyViewModel(companyService = companyService.get(),companyStorage=companyStorage.get()) as T
        }
    }

}