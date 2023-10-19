package com.far.menugenerator.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.R
import com.far.menugenerator.common.utils.NumberUtils
import com.far.menugenerator.databinding.FragmentCreateMenuBinding
import com.far.menugenerator.model.CreateMenuState
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.view.adapters.CategoriesAdapter
import com.far.menugenerator.view.adapters.MenuPreviewAdapter
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.viewModel.CreateMenuViewModel


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        _viewModel = ViewModelProvider(requireActivity())[CreateMenuViewModel::class.java]
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
       /* _binding.menuTypeScreen.imageTitleDescription.root.setOnClickListener{
            _viewModel.selectItemStyle(ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE)
        }
        _binding.menuTypeScreen.titleDescription.root.setOnClickListener{
            _viewModel.selectItemStyle(ItemStyle.MENU_TITLE_DESCRIPTION_PRICE)
        }
        _binding.menuTypeScreen.titlePrice.root.setOnClickListener{
            _viewModel.selectItemStyle(ItemStyle.MENU_TITLE_PRICE)
        }*/

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
        //_binding.addMenuItemScreen.etProductPrice.addTextChangedListener(/*resources.getString(R.string.currencyFormat)*/"#,##0.00"))

        _binding.categoriesScreen.rvCategories.layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,false)
        _binding.menuPreviewScreen.rvPreview.layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,false)
    }


    private fun initObservers(){
        _viewModel.state.observe(requireActivity()){
            navigate(it.currentScreen)
            //menuTypeSelected(it.selectedItemStyle)
            setCategoryAdapter(it.categories)
            fillSpinnerCategories(it.categories)
            fillPreviewAdapter(it)
        }
        _viewModel.currentItemImage.observe(requireActivity()){
            if(it != null){
                _binding.addMenuItemScreen.imgProduct.setImageURI(it)
            }else{
                _binding.addMenuItemScreen.imgProduct.setImageDrawable(null)
            }

        }
    }

    private fun fillSpinnerCategories(categories: MutableList<String>){
        var adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,categories)
        _binding.addMenuItemScreen.spnCategories.adapter = adapter
    }

    private fun navigate(currentView:Int){
        _binding.llBack.visibility = if(currentView == R.id.categoriesScreen) View.GONE else View.VISIBLE
        _binding.llNext.visibility = if(currentView == R.id.menuPreviewScreen) View.GONE else View.VISIBLE

        //_binding.menuTypeScreen.root.visibility= if(currentView == R.id.menuTypeScreen) View.VISIBLE else View.GONE
        _binding.categoriesScreen.root.visibility= if(currentView == R.id.categoriesScreen) View.VISIBLE else View.GONE
        _binding.addMenuItemScreen.root.visibility= if(currentView == R.id.addMenuItemScreen) View.VISIBLE else View.GONE
        _binding.menuPreviewScreen.root.visibility = if(currentView == R.id.menuPreviewScreen) View.VISIBLE else View.GONE
    }
/*
    private fun menuTypeSelected(itemStyle: ItemStyle) {
        val selectedTextColor:Int = resources.getColor(R.color.white)
        val selectedCardColor:Int = resources.getColor(R.color.purple_500)

        val unSelectedTextColor:Int = resources.getColor(R.color.black)
        val unSelectedCardColor:Int = resources.getColor(R.color.white)

        _binding.menuTypeScreen.imageTitleDescription.root.setCardBackgroundColor(if(itemStyle == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE) selectedCardColor else unSelectedCardColor)
        _binding.menuTypeScreen.imageTitleDescription.title.setTextColor(if(itemStyle == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE) selectedTextColor else unSelectedTextColor)
        _binding.menuTypeScreen.imageTitleDescription.body.setTextColor(if(itemStyle == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE) selectedTextColor else unSelectedTextColor)
        _binding.menuTypeScreen.imageTitleDescription.price.setTextColor(if(itemStyle == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE) selectedTextColor else unSelectedTextColor)

        _binding.menuTypeScreen.titleDescription.root.setCardBackgroundColor(if(itemStyle == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE) selectedCardColor else unSelectedCardColor)
        _binding.menuTypeScreen.titleDescription.title.setTextColor(if(itemStyle == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE) selectedTextColor else unSelectedTextColor)
        _binding.menuTypeScreen.titleDescription.body.setTextColor(if(itemStyle == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE) selectedTextColor else unSelectedTextColor)
        _binding.menuTypeScreen.titleDescription.price.setTextColor(if(itemStyle == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE) selectedTextColor else unSelectedTextColor)

        _binding.menuTypeScreen.titlePrice.root.setCardBackgroundColor(if(itemStyle == ItemStyle.MENU_TITLE_PRICE) selectedCardColor else unSelectedCardColor)
        _binding.menuTypeScreen.titlePrice.title.setTextColor(if(itemStyle == ItemStyle.MENU_TITLE_PRICE) selectedTextColor else unSelectedTextColor)
        _binding.menuTypeScreen.titlePrice.price.setTextColor(if(itemStyle == ItemStyle.MENU_TITLE_PRICE) selectedTextColor else unSelectedTextColor)


    }*/

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
        _binding.menuPreviewScreen.rvPreview.adapter = MenuPreviewAdapter(previewList)
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