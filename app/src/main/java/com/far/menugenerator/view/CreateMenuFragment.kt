package com.far.menugenerator.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
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
import com.far.menugenerator.model.Company
import com.far.menugenerator.model.CreateMenuState
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.model.storage.StorageMenu
import com.far.menugenerator.view.adapters.CategoriesAdapter
import com.far.menugenerator.view.adapters.MenuPreviewAdapter
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.CreateMenuViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.name


/**
 * A simple [Fragment] subclass.
 * Use the [CreateMenuFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateMenuFragment : BaseFragment() {
    // TODO: Rename and change types of parameters
    //private var param1: String? = null
    //private var param2: String? = null

    private lateinit var _binding:FragmentCreateMenuBinding
    private lateinit var _viewModel:CreateMenuViewModel

    @Inject lateinit var screenNavigation: ScreenNavigation
    @Inject lateinit var createMenuViewModelFactory: CreateMenuViewModel.CreateMenuViewModelFactory


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        presentationComponent.inject(this)
        _viewModel = ViewModelProvider(this,createMenuViewModelFactory)[CreateMenuViewModel::class.java]
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
        _binding.llGenerate.setOnClickListener{
            val fileName = "some name"//selectec by the user
            val pdfRoute = "${FileUtils.getDownloadsPath()}/temp.pdf"
            var height = _binding.menuPreviewFinalScreen.llCompany.measuredHeight + calculateTotalItemHeight(_binding.menuPreviewFinalScreen.llItems) + _binding.menuPreviewFinalScreen.llFooter.measuredHeight
            lifecycleScope.launch(Dispatchers.Main) {
                //SHOW LOADING AND LOCK NAVIGATION
                Toast.makeText(baseActivity,"Generando...", Toast.LENGTH_SHORT).show()

                FileUtils.layoutToPdf(_binding.menuPreviewFinalScreen.root,pdfRoute,height)

                val previewItems = (_binding.menuPreviewScreen.rvPreview.adapter as MenuPreviewAdapter).currentPreview
                _viewModel.saveMenu(LoginActivity.account?.email!!,fileName,previewItems,pdfRoute)
            }

        }


        _binding.categoriesScreen.btnAdd.setOnClickListener{
            val category = _binding.categoriesScreen.etCategory.text.toString()
            _viewModel.addCategory(category)
            _binding.categoriesScreen.etCategory.text?.clear()
        }
        _binding.addMenuItemScreen.btnAddProductImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).setType("image/*")
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }
        _binding.addMenuItemScreen.btnClearProductImage.setOnClickListener {
            _viewModel.updateCurrentItemImage(null)
        }
        _binding.addMenuItemScreen.btnAdd.setOnClickListener {
            if(!validateProductData()) return@setOnClickListener
            _viewModel.addProduct(
                _binding.addMenuItemScreen.spnCategories.selectedItem as String,
                _binding.addMenuItemScreen.etProductName.text.toString(),
                _binding.addMenuItemScreen.etProductDescription.text.toString(),
                _binding.addMenuItemScreen.etProductPrice.text.toString().toDouble()
            )
            clearProductFields()
        }
        _binding.categoriesScreen.rvCategories.layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,false)
        _binding.menuPreviewScreen.rvPreview.layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,false)



    }


    private fun initObservers(){
        _viewModel.state.observe(viewLifecycleOwner){
            navigate(it.currentScreen)
            setCategoryAdapter(it.categories)
            fillSpinnerCategories(it.categories)
            fillPreviewAdapter(it)

            if(it.currentScreen == R.id.menuPreviewFinalScreen){
                fillCompany()
                fillFinalPreview((_binding.menuPreviewScreen.rvPreview.adapter as MenuPreviewAdapter).currentPreview)
            }

        }
        _viewModel.currentItemImage.observe(viewLifecycleOwner){
            if(it != null){
                Glide.with(baseActivity)
                    .load(it)
                    .into(_binding.addMenuItemScreen.imgProduct)
            }else{
                _binding.addMenuItemScreen.imgProduct.setImageResource(R.drawable.baseline_photo_library_24)
            }

        }
        _viewModel.savedMenuUrl.observe(requireActivity()){
            //FINISH PROCESS
            Toast.makeText(baseActivity,"Completado", Toast.LENGTH_SHORT).show()
            screenNavigation.qrImagePreview(it)
        }
    }

    private fun fillSpinnerCategories(categories: MutableList<String>){
        var adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,categories)
        _binding.addMenuItemScreen.spnCategories.adapter = adapter
    }

    private fun navigate(currentView:Int){
        _binding.llBack.visibility = if(currentView == R.id.categoriesScreen) View.GONE else View.VISIBLE
        _binding.llNext.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.GONE else View.VISIBLE
        _binding.llGenerate.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.VISIBLE else View.GONE

        _binding.categoriesScreen.root.visibility= if(currentView == R.id.categoriesScreen) View.VISIBLE else View.GONE
        _binding.addMenuItemScreen.root.visibility= if(currentView == R.id.addMenuItemScreen) View.VISIBLE else View.GONE
        _binding.menuPreviewScreen.root.visibility = if(currentView == R.id.menuPreviewScreen) View.VISIBLE else View.GONE
        _binding.menuPreviewFinalScreen.root.visibility = if(currentView == R.id.menuPreviewFinalScreen) View.VISIBLE else View.GONE
    }


    private fun fillPreviewAdapter(state:CreateMenuState){
        var previewList = mutableListOf<ItemPreview>()
        previewList.addAll(state.categories.map { ItemPreview(itemStyle = ItemStyle.MENU_CATEGORY_HEADER, position = state.categories.indexOf(it), categoryName =  it, name =  it, price =  null, description =  null, image =  null) })

        previewList.addAll(state.items.map {
            var itemStyle: ItemStyle = if(it.image != null){
                ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE
            }else if(!it.description.isNullOrBlank()){
                ItemStyle.MENU_TITLE_DESCRIPTION_PRICE
            }else{
                ItemStyle.MENU_TITLE_PRICE
            }

            ItemPreview(itemStyle = itemStyle, position = state.items.indexOf(it), categoryName =  it.category, name =  it.name, price =  it.amount.toString(), description =  it.description, image =  it.image)
        })
        _binding.menuPreviewScreen.rvPreview.adapter = MenuPreviewAdapter(baseActivity,previewList)
    }
    private fun setCategoryAdapter(categories:MutableList<String>){
        val adapter = CategoriesAdapter(categories)
        _binding.categoriesScreen.rvCategories.adapter = adapter
    }

    private fun validateProductData():Boolean{
        if(_binding.addMenuItemScreen.etProductName.text.isNullOrBlank()){
            _binding.addMenuItemScreen.etProductName.error = resources.getString(R.string.required)
            _binding.addMenuItemScreen.etProductName.requestFocus()

            return false
        }
        if(_binding.addMenuItemScreen.etProductPrice.text.isNullOrBlank()){
            _binding.addMenuItemScreen.etProductPrice.error = resources.getString(R.string.required)
            _binding.addMenuItemScreen.etProductPrice.requestFocus()
            return false
        }
        if(NumberUtils.stringIsNumber(_binding.addMenuItemScreen.etProductPrice.text.toString())){
            _binding.addMenuItemScreen.etProductPrice.error = resources.getString(R.string.invalid_value)
            _binding.addMenuItemScreen.etProductPrice.requestFocus()
            return false
        }
        return true
    }

    private fun clearProductFields(){
        _binding.addMenuItemScreen.etProductName.text?.clear()
        _binding.addMenuItemScreen.etProductPrice.text?.clear()
        _binding.addMenuItemScreen.etProductDescription.text?.clear()
        clearImage()
        _binding.addMenuItemScreen.etProductName.requestFocus()

    }
    private fun clearImage(){
        _viewModel.updateCurrentItemImage(null)
    }


    fun calculateTotalItemHeight(recyclerView: RecyclerView): Int {
        var totalItemHeight = 0
        for (i in 0 until recyclerView.adapter!!.itemCount) {
            val view = recyclerView.adapter!!
                .onCreateViewHolder(recyclerView, i).itemView
            totalItemHeight += measureItemHeight(view)
        }
        return totalItemHeight
    }

    fun calculateTotalItemHeight(viewGroup: ViewGroup): Int {
        var totalItemHeight = 0
        for (i in 0 until viewGroup.childCount) {
            val view = viewGroup.getChildAt(i)
            totalItemHeight += measureItemHeight(view)
        }
        return totalItemHeight
    }


    fun measureItemHeight(view: View): Int {
        //val widthSpec = View.MeasureSpec.makeMeasureSpec(view.measuredWidth, View.MeasureSpec.AT_MOST)
        //val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        //view.measure(widthSpec, heightSpec)
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

            when (itemPreview.itemStyle) {
                ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE -> {
                    binding.imageTitleDescription.title.text = itemPreview.name
                    binding.imageTitleDescription.body.text = itemPreview.description
                    binding.imageTitleDescription.price.text = itemPreview.price
                    Glide.with(baseActivity)
                        .load(itemPreview.image)
                        .into(binding.imageTitleDescription.image)

                }
                ItemStyle.MENU_TITLE_DESCRIPTION_PRICE -> {
                    binding.titleDescription.title.text = itemPreview.name
                    binding.titleDescription.body.text = itemPreview.description
                    binding.titleDescription.price.text = itemPreview.price
                }
                ItemStyle.MENU_TITLE_PRICE -> {
                    binding.titlePrice.title.text = itemPreview.name
                    binding.titlePrice.price.text = itemPreview.price
                }
                else -> {
                    binding.categoryTitle.title.text = itemPreview.name
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
        val comp = Company(businessName = "Cow Abunga Burgers", address1 = "4455 Landing Lange, APT 4", address2 = "Louisville, KY 40018-1234", address3 = "", phone1 = "", phone2 = "",phone3="", facebook = "https://cowabungaburgers.com", instagram = "@cowabungaburgers.com", whatsapp = "809-998-3580", logo = null)
        _binding.menuPreviewFinalScreen.logo.setImageResource(R.drawable.foodtruck_logo)
        if(comp.businessName.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvCompanyName.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvCompanyName.text= comp.businessName
            _binding.menuPreviewFinalScreen.tvCompanyName.visibility = View.VISIBLE
        }


        if(comp.phone1.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvPhone1.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone1.text= comp.phone1
            _binding.menuPreviewFinalScreen.tvPhone1.visibility = View.VISIBLE
        }

        if(comp.phone2.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvPhone2.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone2.text= comp.phone2
            _binding.menuPreviewFinalScreen.tvPhone2.visibility = View.VISIBLE
        }

        if(comp.phone3.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvPhone3.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvPhone3.text= comp.phone3
            _binding.menuPreviewFinalScreen.tvPhone3.visibility = View.VISIBLE
        }


        if(comp.address1.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvAddress1.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress1.text= comp.address1
            _binding.menuPreviewFinalScreen.tvAddress1.visibility = View.VISIBLE
        }

        if(comp.address2.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvAddress2.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress2.text= comp.address2
            _binding.menuPreviewFinalScreen.tvAddress2.visibility = View.VISIBLE
        }

        if(comp.address3.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.tvAddress3.visibility = View.GONE
        }else{
            _binding.menuPreviewFinalScreen.tvAddress3.text= comp.address3
            _binding.menuPreviewFinalScreen.tvAddress3.visibility = View.VISIBLE
        }


        if(comp.facebook.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.llFacebook.visibility = View.GONE

        }else{
            _binding.menuPreviewFinalScreen.llFacebook.visibility = View.VISIBLE
            _binding.menuPreviewFinalScreen.tvFacebook.text= comp.facebook
        }

        if(comp.instagram.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.llInstagram.visibility = View.GONE

        }else{
            _binding.menuPreviewFinalScreen.llInstagram.visibility = View.VISIBLE
            _binding.menuPreviewFinalScreen.tvInstagram.text= comp.instagram
        }

        if(comp.whatsapp.isNullOrBlank()){
            _binding.menuPreviewFinalScreen.llWhatsapp.visibility = View.GONE

        }else{
            _binding.menuPreviewFinalScreen.llWhatsapp.visibility = View.VISIBLE
            _binding.menuPreviewFinalScreen.tvWhatsapp.text= comp.whatsapp
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
        fun newInstance(/*param1: String, param2: String*/) =
            CreateMenuFragment().apply {
                arguments = Bundle().apply {
                    //putString(ARG_PARAM1, param1)
                    //putString(ARG_PARAM2, param2)
                }
            }
    }
}