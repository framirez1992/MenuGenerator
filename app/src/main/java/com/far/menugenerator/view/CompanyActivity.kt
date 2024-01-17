package com.far.menugenerator.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentCompanyBinding
import com.far.menugenerator.model.Company
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.viewModel.CompanyViewModel
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


/**
 * A simple [Fragment] subclass.
 * Use the [CompanyActivity.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val REQUEST_CODE_PICK_IMAGE = 100
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
    }



    private fun initViews(){

        _binding.btnNext.setOnClickListener{ _viewModel.nextScreen()}
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
        _binding.layoutCompanyLogo.btnAddLogoImage.setOnClickListener{
            val intent = Intent(Intent.ACTION_PICK).setType("image/*")
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }
        _binding.layoutCompanyLogo.btnClearLogoImage.setOnClickListener {
            _viewModel.updateCurrentCompanyLogo(null)
        }


    }

    private fun initObservers(){
        _viewModel.state.observe(this){
            navigate(it.currentScreen)
            if(it.isLoading)
                dialogManager.showLoadingDialog()
            else
                dialogManager.dismissLoadingDialog()
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
                _binding.layoutCompanyLogo.imgLogo.setImageResource(R.drawable.search_image)
            }
        }

    }


    private fun navigate(currentScreen:Int){
        _binding.btnBack.visibility = if(currentScreen == R.id.layoutCompanyName) View.GONE else View.VISIBLE
        _binding.btnNext.visibility = if(currentScreen != R.id.layoutCompanyLogo) View.VISIBLE else View.GONE
        _binding.btnFinish.visibility = if(currentScreen == R.id.layoutCompanyLogo) View.VISIBLE else View.GONE
        _binding.layoutCompanyName.root.visibility = if(currentScreen == R.id.layoutCompanyName) View.VISIBLE else View.GONE
        _binding.layoutCompanyAddress.root.visibility = if(currentScreen == R.id.layoutCompanyAddress) View.VISIBLE else View.GONE
        _binding.layoutCompanyContact.root.visibility = if(currentScreen == R.id.layoutCompanyContact) View.VISIBLE else View.GONE
        _binding.layoutCompanyLogo.root.visibility = if(currentScreen == R.id.layoutCompanyLogo) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            _viewModel.updateCurrentCompanyLogo(imageUri)
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
}