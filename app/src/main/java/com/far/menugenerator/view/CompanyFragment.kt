package com.far.menugenerator.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentCompanyBinding
import com.far.menugenerator.view.common.BaseFragment
import com.far.menugenerator.viewModel.CompanyViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CompanyFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CompanyFragment : BaseFragment() {
    // TODO: Rename and change types of parameters
    //private var param1: String? = null
    //private var param2: String? = null
    private lateinit var _binding:FragmentCompanyBinding
    private lateinit var _viewModel:CompanyViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //param1 = it.getString(ARG_PARAM1)
            //param2 = it.getString(ARG_PARAM2)
        }
        _viewModel = ViewModelProvider(this)[CompanyViewModel::class.java]
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
        _binding.btnFinish.setOnClickListener {  }
    }

    private fun initObservers(){
        _viewModel.state.observe(viewLifecycleOwner){
            navigate(it.currentScreen)
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
        fun newInstance(/*param1: String, param2: String*/) =
            CompanyFragment().apply {
                arguments = Bundle().apply {
                    //putString(ARG_PARAM1, param1)
                    //putString(ARG_PARAM2, param2)
                }
            }
    }
}