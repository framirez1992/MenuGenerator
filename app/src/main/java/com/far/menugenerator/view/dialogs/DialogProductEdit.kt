package com.far.menugenerator.view.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.far.menugenerator.R
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.view.CreateMenuFragment
import java.io.Serializable


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ITEM = "item"
private const val REQUEST_CODE_PICK_IMAGE = 110

/**
 * A simple [Fragment] subclass.
 * Use the [DialogProductEdit.newInstance] factory method to
 * create an instance of this fragment.
 */
class DialogProductEdit : DialogFragment() {
    // TODO: Rename and change types of parameters
    private  var dialogItem: DialogProductEditItem? = null

    //private lateinit var binding:EditMenuItemScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            dialogItem = it.getSerializable(ITEM) as DialogProductEditItem
        }
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //binding = EditMenuItemScreenBinding.inflate(inflater,container,false)
        return null///binding.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            //setImage(imageUri)
        }
    }
/*
    private fun setImage(uri: Uri?){
        if(uri != null){
            Glide.with(requireContext())
                .load(uri)
                .into(binding.editProduct.productData.imgProduct)
        }else{
            binding.editProduct.productData.imgProduct.setImageResource(R.drawable.search_image)
        }
    }

    private fun fillSpinnerCategories(categories: MutableList<String>){
        var adapter = ArrayAdapter<String>(requireContext(),android.R.layout.simple_list_item_1,categories)
        binding.editProduct.productData.spnCategories.adapter = adapter
    }*/
    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(dialogEditItem: DialogProductEditItem) =
            DialogProductEdit().apply {
                arguments = Bundle().apply {
                    putSerializable(ITEM, dialogEditItem)
                }
            }
    }

    data class DialogProductEditItem(val item: ItemPreview, val categories:MutableList<String>):Serializable
}