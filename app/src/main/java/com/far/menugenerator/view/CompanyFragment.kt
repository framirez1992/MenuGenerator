package com.far.menugenerator.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentCompanyBinding
import com.far.menugenerator.model.Company
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.viewModel.CompanyViewModel
import javax.inject.Inject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_COMPANY = "company"

/**
 * A simple [Fragment] subclass.
 * Use the [CompanyFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val REQUEST_CODE_PICK_IMAGE = 100
class CompanyFragment : BaseFragment() {
    // TODO: Rename and change types of parameters
    private var company: CompanyFirebase? = null
    private lateinit var _binding:FragmentCompanyBinding
    private lateinit var _viewModel:CompanyViewModel

    @Inject lateinit var viewModelFactory:CompanyViewModel.CompanyViewModelFactory
    @Inject lateinit var dialogManager: DialogManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            company = it.getSerializable(ARG_COMPANY) as CompanyFirebase?
        }
        presentationComponent.inject(this)
        _viewModel = ViewModelProvider(this,viewModelFactory)[CompanyViewModel::class.java]
        _viewModel.prepareCompanyEdit(company)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCompanyBinding.inflate(inflater,container,false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        _viewModel.state.observe(viewLifecycleOwner){
            navigate(it.currentScreen)
            if(it.isLoading)
                dialogManager.showLoadingDialog()
            else
                dialogManager.dismissLoadingDialog()
        }
        _viewModel.company.observe(viewLifecycleOwner){
            setCompanyData(it)
        }
        _viewModel.currentImage.observe(viewLifecycleOwner){
            if(it != null){
                Glide.with(baseActivity)
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


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CompanyFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(company: CompanyFirebase?) =
            CompanyFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_COMPANY, company)
                }
            }
    }
}