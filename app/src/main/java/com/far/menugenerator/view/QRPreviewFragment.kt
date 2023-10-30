package com.far.menugenerator.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.R
import com.far.menugenerator.common.utils.FileUtils
import com.far.menugenerator.databinding.FragmentQRPreviewBinding
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.viewModel.QRPreviewViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "url"

/**
 * A simple [Fragment] subclass.
 * Use the [QRPreviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class QRPreviewFragment : BaseFragment() {
    // TODO: Rename and change types of parameters
    private var data: String? = null

    private lateinit var binding:FragmentQRPreviewBinding
    private lateinit var _viewModel: QRPreviewViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            data = it.getString(ARG_PARAM1)
        }
        _viewModel = ViewModelProvider(this)[QRPreviewViewModel::class.java]
        if(savedInstanceState == null)
            _viewModel.processUrl(data!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

            initObservers()
    }

    private fun initObservers(){
        _viewModel.qrBitmap.observe(requireActivity()){
            binding.imgQR.setImageBitmap(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentQRPreviewBinding.inflate(inflater,container,false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment QRPreviewFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(data: String) =
            QRPreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, data)
                }
            }
    }
}