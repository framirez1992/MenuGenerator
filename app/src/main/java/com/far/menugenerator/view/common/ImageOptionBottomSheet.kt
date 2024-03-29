package com.far.menugenerator.view.common

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentImageOptionBottomSheetBinding
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.adapters.ImageOptionAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment



/**
 * A simple [Fragment] subclass.
 * Use the [ImageOptionBottomSheet.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImageOptionBottomSheet(val options:List<ImageOption>,val onclick:(ImageOption)->Unit) : BottomSheetDialogFragment() {


    private lateinit var binding:FragmentImageOptionBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentImageOptionBottomSheetBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rv.layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
        binding.rv.adapter = ImageOptionAdapter(options=options){
            onclick(it)
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // used to show the bottom sheet dialog
        dialog?.setOnShowListener { it ->
            val d = it as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    companion object{
        val TAG:String = "ImageOptionBottomSheet"
    }

}