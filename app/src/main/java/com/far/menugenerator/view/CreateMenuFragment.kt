package com.far.menugenerator.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.far.menugenerator.R
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.common.utils.NumberUtils
import com.far.menugenerator.databinding.FragmentCreateMenuBinding
import com.far.menugenerator.databinding.ItemMenuFinalPreviewBinding
import com.far.menugenerator.model.Item
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.view.adapters.CategoriesAdapter
import com.far.menugenerator.view.adapters.MenuPreviewAdapter
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.CreateMenuViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * A simple [Fragment] subclass.
 * Use the [CreateMenuFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val ARG_COMPANY = "company"
private const val ARG_EDIT_MENU = "editMenu"
class CreateMenuFragment : BaseFragment() {
    // TODO: Rename and change types of parameters
    private lateinit var company: CompanyFirebase
    private var editMenu: MenuFirebase? = null

    private lateinit var _binding:FragmentCreateMenuBinding
    private lateinit var _viewModel:CreateMenuViewModel

    @Inject lateinit var screenNavigation: ScreenNavigation
    @Inject lateinit var dialogManager: DialogManager
    @Inject lateinit var createMenuViewModelFactory: CreateMenuViewModel.CreateMenuViewModelFactory


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            company = it.getSerializable(ARG_COMPANY) as CompanyFirebase
            editMenu = it.getSerializable(ARG_EDIT_MENU) as MenuFirebase?
        }
        presentationComponent.inject(this)
        _viewModel = ViewModelProvider(this,createMenuViewModelFactory)[CreateMenuViewModel::class.java]
        _viewModel.prepareMenuEdit(editMenu)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCreateMenuBinding.inflate(inflater,container,false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        initViews()
        initObservers()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            _viewModel.updateCurrentItemImage(imageUri)
        }
    }

    private fun initViews(){

        _binding.llNext.setOnClickListener { _viewModel.nextScreen() }
        _binding.llBack.setOnClickListener { _viewModel.previousScreen() }
        _binding.llGenerate.setOnClickListener {
            val fileName = "some name"//selectec by the user
            val pdfPath = "${FileUtils.getDownloadsPath()}/temp.pdf"
            var height = _binding.menuPreviewFinalScreen.llCompany.measuredHeight + calculateTotalItemHeight(_binding.menuPreviewFinalScreen.llItems) + _binding.menuPreviewFinalScreen.llFooter.measuredHeight
            lifecycleScope.launch(Dispatchers.Main) {
                //SHOW LOADING AND LOCK NAVIGATION
                Toast.makeText(baseActivity,"Generando...", Toast.LENGTH_SHORT).show()

                FileUtils.layoutToPdf(_binding.menuPreviewFinalScreen.root,pdfPath,height)

                val previewItems = (_binding.menuPreviewScreen.rvPreview.adapter as MenuPreviewAdapter).currentPreview
                _viewModel.processMenu(user= LoginActivity.account?.email!!, companyId = company.companyId, fileName =  fileName, itemPreviews =  previewItems, pdfPath =   pdfPath)
            }

        }


        _binding.categoriesScreen.btnAdd.setOnClickListener{
            val category = _binding.categoriesScreen.etCategory.text.toString()
            if(category == CreateMenuViewModel.NO_CATEGORY) return@setOnClickListener

            _viewModel.addCategory(category)
            _binding.categoriesScreen.etCategory.text?.clear()
        }
        _binding.categoriesScreen.btnEdit.setOnClickListener{
            _viewModel.saveEditCategory(_binding.categoriesScreen.etCategory.text.toString())
            _viewModel.setScreen(R.id.categoriesScreen)
            _binding.categoriesScreen.etCategory.text?.clear()
        }
        _binding.categoriesScreen.btnCancel.setOnClickListener{
            _binding.categoriesScreen.etCategory.text?.clear()
            _viewModel.setEditCategory(null)
            _viewModel.setScreen(R.id.categoriesScreen)
        }


        _binding.addMenuItemScreen.productData.btnAddProductImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).setType("image/*")
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }
        _binding.addMenuItemScreen.productData.btnClearProductImage.setOnClickListener {
            _viewModel.updateCurrentItemImage(null)
        }
        _binding.addMenuItemScreen.btnAdd.setOnClickListener {
            if(!validateProductData()) return@setOnClickListener
            _viewModel.addProduct(
                _binding.addMenuItemScreen.productData.spnCategories.selectedItem as String,
                _binding.addMenuItemScreen.productData.etProductName.text.toString(),
                _binding.addMenuItemScreen.productData.etProductDescription.text.toString(),
                _binding.addMenuItemScreen.productData.etProductPrice.text.toString().toDouble()
            )
            clearAddProductFields()
        }

        _binding.addMenuItemScreen.btnEdit.setOnClickListener{
            _viewModel.saveEditItemChanges(
                category= _binding.addMenuItemScreen.productData.spnCategories.selectedItem as String,
                name=_binding.addMenuItemScreen.productData.etProductName.text.toString(),
                description= _binding.addMenuItemScreen.productData.etProductDescription.text.toString(),
                price= _binding.addMenuItemScreen.productData.etProductPrice.text.toString().toDouble()
            )
            _viewModel.setScreen(R.id.menuPreviewScreen)
            clearAddProductFields()
        }
        _binding.addMenuItemScreen.btnCancel.setOnClickListener{
            clearAddProductFields()
            _viewModel.setScreen(R.id.menuPreviewScreen)
        }


        _binding.categoriesScreen.rvCategories.layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,false)
        _binding.menuPreviewScreen.rvPreview.layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,false)



    }


    private fun initObservers(){
        _viewModel.state.observe(viewLifecycleOwner){
            navigate(it.currentScreen)

        }
        _viewModel.editCategory.observe(viewLifecycleOwner){
            if(it != null){
                _binding.llNext.visibility = View.GONE
                _binding.llBack.visibility = View.GONE
                _binding.categoriesScreen.etCategory.setText(it)
                _binding.categoriesScreen.btnAdd.visibility = View.GONE
                _binding.categoriesScreen.btnEdit.visibility = View.VISIBLE
                _binding.categoriesScreen.btnCancel.visibility = View.VISIBLE
            }else{
                _binding.categoriesScreen.btnAdd.visibility = View.VISIBLE
                _binding.categoriesScreen.btnEdit.visibility = View.GONE
                _binding.categoriesScreen.btnCancel.visibility = View.GONE
            }
        }
        _viewModel.editItem.observe(viewLifecycleOwner){

            if(it != null){
                _binding.llNext.visibility = View.GONE
                _binding.llBack.visibility = View.GONE
            }

            _binding.addMenuItemScreen.btnAdd.visibility = if(it != null) View.GONE else View.VISIBLE
            _binding.addMenuItemScreen.btnEdit.visibility = if(it == null) View.GONE else View.VISIBLE
            _binding.addMenuItemScreen.btnCancel.visibility = if(it == null) View.GONE else View.VISIBLE
            _binding.addMenuItemScreen.title.text = resources.getString(if(it!=null) R.string.edit_product else R.string.products)

            if (it != null) {
                fillEditProductData(it)
            }
        }


        _viewModel.categories.observe(viewLifecycleOwner){
            setCategoryAdapter(it)
            fillSpinnerCategoriesAddProductScreen(it)
        }
        _viewModel.itemsPreview.observe(viewLifecycleOwner){
            fillPreviewAdapter(it)
        }
        _viewModel.currentItemImage.observe(viewLifecycleOwner){
            if(it != null){
                Glide.with(baseActivity)
                    .load(it)
                    .into(_binding.addMenuItemScreen.productData.imgProduct)
            }else{
                _binding.addMenuItemScreen.productData.imgProduct.setImageResource(R.drawable.search_image)
            }

        }
        _viewModel.savedMenuUrl.observe(requireActivity()){
            //FINISH PROCESS
            Toast.makeText(baseActivity,"Completado", Toast.LENGTH_SHORT).show()
            screenNavigation.qrImagePreview(it)
        }
    }

    private fun fillSpinnerCategoriesAddProductScreen(categories: MutableList<String>){
        var adapter = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,categories)
        _binding.addMenuItemScreen.productData.spnCategories.adapter = adapter
    }


    private fun navigate(currentView:Int){
        _binding.llBack.visibility = if(currentView == R.id.categoriesScreen) View.GONE else View.VISIBLE
        _binding.llNext.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.GONE else View.VISIBLE
        _binding.llGenerate.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.VISIBLE else View.GONE

        _binding.categoriesScreen.root.visibility= if(currentView == R.id.categoriesScreen) View.VISIBLE else View.GONE
        _binding.addMenuItemScreen.root.visibility= if(currentView == R.id.addMenuItemScreen) View.VISIBLE else View.GONE
        _binding.menuPreviewScreen.root.visibility = if(currentView == R.id.menuPreviewScreen) View.VISIBLE else View.GONE
        _binding.menuPreviewFinalScreen.root.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.VISIBLE else View.GONE

        if(currentView == R.id.menuPreviewFinalScreen){
            fillCompany()
            fillFinalPreview((_binding.menuPreviewScreen.rvPreview.adapter as MenuPreviewAdapter).currentPreview)
        }
    }


    private fun fillPreviewAdapter(items:MutableList<ItemPreview>){
        _binding.menuPreviewScreen.rvPreview.adapter = MenuPreviewAdapter(baseActivity,items){item->
            dialogManager.showOptionDialog(resources.getString(R.string.options),
                arrayOf(resources.getString(R.string.edit),getString(R.string.delete))){ option->
                if(option == resources.getString(R.string.edit)){
                   _viewModel.editItem(item)
                   _viewModel.setScreen(R.id.addMenuItemScreen)
                }else{
                    _viewModel.deleteItem(item)
                }
            }
        }
    }
    private fun setCategoryAdapter(categories:MutableList<String>){
        val categoriesFiltered = mutableListOf<String>()
        categoriesFiltered.addAll(categories.filter { it != CreateMenuViewModel.NO_CATEGORY })

        val adapter = CategoriesAdapter(categoriesFiltered){ category->
            dialogManager.showOptionDialog(resources.getString(R.string.options),
                arrayOf(resources.getString(R.string.edit),resources.getString(R.string.delete))){ option->
                if(option == resources.getString(R.string.delete)){
                    _viewModel.removeCategory(category)
                }else{
                    _viewModel.setEditCategory(category)
                }
            }
        }
        _binding.categoriesScreen.rvCategories.adapter = adapter
    }

    private fun validateProductData():Boolean{
        if(_binding.addMenuItemScreen.productData.etProductName.text.isNullOrBlank()){
            _binding.addMenuItemScreen.productData.etProductName.error = resources.getString(R.string.required)
            _binding.addMenuItemScreen.productData.etProductName.requestFocus()

            return false
        }
        if(_binding.addMenuItemScreen.productData.etProductPrice.text.isNullOrBlank()){
            _binding.addMenuItemScreen.productData.etProductPrice.error = resources.getString(R.string.required)
            _binding.addMenuItemScreen.productData.etProductPrice.requestFocus()
            return false
        }
        if(NumberUtils.stringIsNumber(_binding.addMenuItemScreen.productData.etProductPrice.text.toString())){
            _binding.addMenuItemScreen.productData.etProductPrice.error = resources.getString(R.string.invalid_value)
            _binding.addMenuItemScreen.productData.etProductPrice.requestFocus()
            return false
        }
        return true
    }

    private fun fillEditProductData(item:Item){
        _viewModel.updateCurrentItemImage(item.localImage?:item.remoteImage)

        _binding.addMenuItemScreen.productData.etProductName.setText(item.name)
        _binding.addMenuItemScreen.productData.etProductPrice.setText(item.amount.toString())
        _binding.addMenuItemScreen.productData.etProductDescription.setText(item.description)

       val position = _viewModel.categories.value?.lastIndexOf(item.category)
        _binding.addMenuItemScreen.productData.spnCategories.setSelection(position!!)
    }



    private fun clearAddProductFields(){
        _binding.addMenuItemScreen.productData.etProductName.text?.clear()
        _binding.addMenuItemScreen.productData.etProductPrice.text?.clear()
        _binding.addMenuItemScreen.productData.etProductDescription.text?.clear()
        clearAddProductImage()
        _binding.addMenuItemScreen.productData.etProductName.requestFocus()

    }


    private fun clearAddProductImage(){
        _viewModel.updateCurrentItemImage(null)
    }



    private fun calculateTotalItemHeight(viewGroup: ViewGroup): Int {
        var totalItemHeight = 0
        for (i in 0 until viewGroup.childCount) {
            val view = viewGroup.getChildAt(i)
            totalItemHeight += measureItemHeight(view)
        }
        return totalItemHeight
    }


    private fun measureItemHeight(view: View): Int {
        return view.measuredHeight
    }




    private fun fillFinalPreview(items: List<ItemPreview>){

        _binding.menuPreviewFinalScreen.llItems.removeAllViews()

        items.forEach { itemPreview ->
            val binding = ItemMenuFinalPreviewBinding.inflate(LayoutInflater.from(baseActivity),_binding.menuPreviewFinalScreen.llItems,false)

            binding.imageTitleDescription.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE) View.VISIBLE else View.GONE
            binding.titleDescription.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE) View.VISIBLE else View.GONE
            binding.titlePrice.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_TITLE_PRICE) View.VISIBLE else View.GONE
            binding.categoryTitle.root.visibility = if (itemPreview.itemStyle == ItemStyle.MENU_CATEGORY_HEADER) View.VISIBLE else View.GONE

            val item = itemPreview.item

            //Don`t show NO_CATEGORY ITEM
            if(itemPreview.itemStyle == ItemStyle.MENU_CATEGORY_HEADER &&  item.name == CreateMenuViewModel.NO_CATEGORY){
                binding.root.visibility = View.GONE
            }

            when (itemPreview.itemStyle) {
                ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE -> {
                    binding.imageTitleDescription.title.text = item.name
                    binding.imageTitleDescription.body.text = item.description
                    binding.imageTitleDescription.price.text = item.amount.toString()
                    Glide.with(baseActivity)
                        .load(item.localImage?:item.remoteImage)
                        .into(binding.imageTitleDescription.image)

                }
                ItemStyle.MENU_TITLE_DESCRIPTION_PRICE -> {
                    binding.titleDescription.title.text = item.name
                    binding.titleDescription.body.text = item.description
                    binding.titleDescription.price.text = item.amount.toString()
                }
                ItemStyle.MENU_TITLE_PRICE -> {
                    binding.titlePrice.title.text = item.name
                    binding.titlePrice.price.text = item.amount.toString()
                }
                else -> {
                    binding.categoryTitle.title.text = item.name
                }
            }




            // Get the layout parameters of the view.
            val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            if(itemPreview.itemStyle == ItemStyle.MENU_CATEGORY_HEADER)
                layoutParams.setMargins(0, 20, 0, 0)
            else
                layoutParams.setMargins(0, 0, 0, 0)

            binding.root.layoutParams = layoutParams
            _binding.menuPreviewFinalScreen.llItems.addView(binding.root)
        }

        _binding.menuPreviewFinalScreen.llItems.invalidate()
    }

    private fun fillCompany(){
        //val company = Company(companyId = "12345",businessName = "Cow Abunga Burgers", address1 = "4455 Landing Lange, APT 4", address2 = "Louisville, KY 40018-1234", address3 = "", phone1 = "", phone2 = "",phone3="", facebook = "https://cowabungaburgers.com", instagram = "@cowabungaburgers.com", whatsapp = "809-998-3580", localImage = null)
        _binding.menuPreviewFinalScreen.logo.setImageResource(R.drawable.foodtruck_logo)
        if(company.logoUrl == null){
            _binding.menuPreviewFinalScreen.logo.visibility = View.GONE
        }else{
            Glide.with(baseActivity)
                .load(company.logoUrl)
                .into(_binding.menuPreviewFinalScreen.logo)
        }

        if(company.businessName.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvCompanyName.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvCompanyName.text= company.businessName
            _binding.menuPreviewFinalScreen.tvCompanyName.visibility = View.VISIBLE
        }


        if(company.phone1.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvPhone1.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone1.text= company.phone1
            _binding.menuPreviewFinalScreen.tvPhone1.visibility = View.VISIBLE
        }

        if(company.phone2.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvPhone2.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone2.text= company.phone2
            _binding.menuPreviewFinalScreen.tvPhone2.visibility = View.VISIBLE
        }

        if(company.phone3.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvPhone3.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone3.text= company.phone3
            _binding.menuPreviewFinalScreen.tvPhone3.visibility = View.VISIBLE
        }


        if(company.address1.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvAddress1.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress1.text= company.address1
            _binding.menuPreviewFinalScreen.tvAddress1.visibility = View.VISIBLE
        }

        if(company.address2.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvAddress2.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress2.text= company.address2
            _binding.menuPreviewFinalScreen.tvAddress2.visibility = View.VISIBLE
        }

        if(company.address3.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvAddress3.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress3.text= company.address3
            _binding.menuPreviewFinalScreen.tvAddress3.visibility = View.VISIBLE
        }


        if(company.facebook.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.llFacebook.visibility = View.GONE

        }else{
            _binding.menuPreviewFinalScreen.llFacebook.visibility = View.VISIBLE
            _binding.menuPreviewFinalScreen.tvFacebook.text= company.facebook
        }

        if(company.instagram.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.llInstagram.visibility = View.GONE

        }else{
            _binding.menuPreviewFinalScreen.llInstagram.visibility = View.VISIBLE
            _binding.menuPreviewFinalScreen.tvInstagram.text= company.instagram
        }

        if(company.whatsapp.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.llWhatsapp.visibility = View.GONE

        }else{
            _binding.menuPreviewFinalScreen.llWhatsapp.visibility = View.VISIBLE
            _binding.menuPreviewFinalScreen.tvWhatsapp.text= company.whatsapp
        }
    }



    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 100
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CreateMenuFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(company: CompanyFirebase, menu:MenuFirebase?) =
            CreateMenuFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_COMPANY, company)
                    putSerializable(ARG_EDIT_MENU, menu)
                }
            }
    }
}