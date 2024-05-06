package com.far.menugenerator

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.databinding.ActivityMainBinding
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import com.far.menugenerator.viewModel.MainActivityViewModel
import javax.inject.Inject

class MainActivity : BaseActivity() {

    private lateinit var _binding:ActivityMainBinding
    private lateinit var viewModel:MainActivityViewModel

    @Inject lateinit var screenNavigation: ScreenNavigation


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
    }





}