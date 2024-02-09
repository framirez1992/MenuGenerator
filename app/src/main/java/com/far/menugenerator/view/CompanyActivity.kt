package com.far.menugenerator.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageOptions
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentCompanyBinding
import com.far.menugenerator.model.Company
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.viewModel.CompanyViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 * Use the [CompanyActivity.newInstance] factory method to
 * create an instance of this fragment.
 */
//private const val REQUEST_CODE_PICK_IMAGE = 100
private const val REQUEST_CODE_CROP_IMAGE = 200
class CompanyActivity : BaseActivity() {
    // TODO: Rename and change types of parameters
    private var company: CompanyFirebase? = null
    private lateinit var _binding:FragmentCompanyBinding
    private lateinit var _viewModel:CompanyViewModel

    @Inject lateinit var viewModelFactory:CompanyViewModel.CompanyViewModelFactory
    @Inject lateinit var dialogManager: DialogManager


    companion object{
        const val ARG_COMPANY = "ARG_COMPANY"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        _viewModel = ViewModelProvider(this,viewModelFactory)[CompanyViewModel::class.java]
        _binding = FragmentCompanyBinding.inflate(layoutInflater)
        setContentView(_binding.root)


        company = intent.getSerializableExtra(ARG_COMPANY) as CompanyFirebase?
        _viewModel.prepareCompanyEdit(company)

        initViews()
        initObservers()

        hideActionBar()

    }



    private fun initViews(){

        _binding.btnNext.setOnClickListener{
            if(!validateMandatoryFields(_viewModel.getCurrentScreen().value!!))
                return@setOnClickListener

            _viewModel.nextScreen()
        }
        _binding.btnBack.setOnClickListener { _viewModel.previousScreen()}
        _binding.btnFinish.setOnClickListener {
           _viewModel.saveChanges(user= LoginActivity.account?.email!!,
               companyName = _binding.layoutCompanyName.etBusinessName.text.toString(),
               phone1 = "",
               phone2 = "",
               phone3 = "",
               address1 = _binding.layoutCompanyAddress.etAddress1.text.toString(),
               address2 = _binding.layoutCompanyAddress.etAddress2.text.toString(),
               address3 = _binding.layoutCompanyAddress.etAddress3.text.toString(),
               facebook = _binding.layoutCompanyContact.etFacebook.text.toString(),
               instagram = _binding.layoutCompanyContact.etInstagram.text.toString(),
               whatsapp = _binding.layoutCompanyContact.etWhatsapp.text.toString())
        }
        _binding.layoutCompanyLogo.imgLogo.setOnClickListener{
            val options = listOf(
                ImageOption(icon = R.drawable.baseline_image_search_24,R.string.search),
                ImageOption(icon = R.drawable.baseline_clear_24,R.string.clear),
                )
            dialogManager.showImageBottomSheet(options){
                if(it.string == R.string.search){
                    callCropImage()
                }else if(it.string == R.string.clear){
                    _viewModel.updateCurrentCompanyLogo(null)
                }

            }

        }


    }

    private fun initObservers(){
        _viewModel.getCurrentScreen().observe(this){
            navigate(it)
        }
        _viewModel.getState().observe(this){
            manageState(it)
        }
        _viewModel.company.observe(this){
            setCompanyData(it)
        }
        _viewModel.currentImage.observe(this){
            if(it != null){
                Glide.with(this)
                    .load(it)
                    .into(_binding.layoutCompanyLogo.imgLogo)
            }else{
                _binding.layoutCompanyLogo.imgLogo.setImageDrawable(null)
            }
        }

    }

    private fun callCropImage() {
        val intent = Intent(this,CropImageActivity::class.java)
        var options = CropImageOptions()
        options.activityBackgroundColor = getColor(R.color.grey_900)
        options.backgroundColor = getColor(R.color.grey_900)
        intent.putExtra(CropImage.CROP_IMAGE_EXTRA_OPTIONS,options)
        startActivityForResult(intent,REQUEST_CODE_CROP_IMAGE)
    }


    private fun navigate(currentScreen:Int){
        _binding.btnBack.visibility = if(currentScreen == R.id.layoutCompanyName) View.GONE else View.VISIBLE
        _binding.btnNext.visibility = if(currentScreen != R.id.layoutCompanyLogo) View.VISIBLE else View.GONE
        _binding.btnFinish.visibility = if(currentScreen == R.id.layoutCompanyLogo) View.VISIBLE else View.GONE
        _binding.layoutCompanyName.root.visibility = if(currentScreen == R.id.layoutCompanyName) View.VISIBLE else View.GONE
        _binding.layoutCompanyAddress.root.visibility = if(currentScreen == R.id.layoutCompanyAddress) View.VISIBLE else View.GONE
        _binding.layoutCompanyContact.root.visibility = if(currentScreen == R.id.layoutCompanyContact) View.VISIBLE else View.GONE
        _binding.layoutCompanyLogo.root.visibility = if(currentScreen == R.id.layoutCompanyLogo) View.VISIBLE else View.GONE


        var et:TextInputEditText?=null
        if(currentScreen == R.id.layoutCompanyName)
            et = _binding.layoutCompanyName.etBusinessName
        else if(currentScreen == R.id.layoutCompanyAddress)
            et=_binding.layoutCompanyAddress.etAddress1
        else if (currentScreen == R.id.layoutCompanyContact)
            et = _binding.layoutCompanyContact.etFacebook
        else if(currentScreen == R.id.layoutCompanyLogo) {
            hideKeyboard(_binding.layoutCompanyLogo.root.windowToken)
        }

        et?.requestFocus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        /*if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            //val imageUri: Uri? = data?.data
            //_viewModel.updateCurrentCompanyLogo(imageUri)

        }else */
        if(requestCode == REQUEST_CODE_CROP_IMAGE && resultCode == Activity.RESULT_OK){
            val imageUri: CropImage.ActivityResult? = data?.extras?.getParcelable(CropImage.CROP_IMAGE_EXTRA_RESULT)
            _viewModel.updateCurrentCompanyLogo(imageUri?.uriContent)
        }
    }

    override fun onPause() {
        super.onPause()
       setCurrentChanges()
    }

    private fun setCurrentChanges(){
        _viewModel.setCompanyData(
            companyName = _binding.layoutCompanyName.etBusinessName.text.toString(),
            phone1 = "",
            phone2 = "",
            phone3 = "",
            address1 = _binding.layoutCompanyAddress.etAddress1.text.toString(),
            address2 = _binding.layoutCompanyAddress.etAddress2.text.toString(),
            address3 = _binding.layoutCompanyAddress.etAddress3.text.toString(),
            facebook = _binding.layoutCompanyContact.etFacebook.text.toString(),
            instagram = _binding.layoutCompanyContact.etInstagram.text.toString(),
            whatsapp = _binding.layoutCompanyContact.etWhatsapp.text.toString()
        )
    }

    private fun setCompanyData(company:Company){
        _binding.layoutCompanyName.etBusinessName.setText(company.businessName)
        _binding.layoutCompanyAddress.etAddress1.setText(company.address1)
        _binding.layoutCompanyAddress.etAddress2.setText(company.address2)
        _binding.layoutCompanyAddress.etAddress3.setText(company.address3)
        _binding.layoutCompanyContact.etFacebook.setText(company.facebook)
        _binding.layoutCompanyContact.etInstagram.setText(company.instagram)
        _binding.layoutCompanyContact.etWhatsapp.setText(company.whatsapp)
    }

    private fun validateMandatoryFields(currentScreen: Int):Boolean{
        if(currentScreen == R.id.layoutCompanyName){
            if(_binding.layoutCompanyName.etBusinessName.text.isNullOrBlank()){
                _binding.layoutCompanyName.tilBusinessName.error = getString(R.string.mandatory)
                return false
            }else
                _binding.layoutCompanyName.tilBusinessName.error = null
        }
        return true
    }

    private fun manageState(processState: ProcessState){
        if(processState.state == State.LOADING)
            dialogManager.showLoadingDialog()
        else
            dialogManager.dismissLoadingDialog()

        if(processState.state == State.SUCCESS)
            finish()
        else if(processState.state == State.ERROR)
            Snackbar.make(_binding.root,R.string.operation_failed_please_retry,Snackbar.LENGTH_LONG).show()

    }



}