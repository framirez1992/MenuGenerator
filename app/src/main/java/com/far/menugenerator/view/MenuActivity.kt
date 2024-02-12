package com.far.menugenerator.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.far.menugenerator.R
import com.far.menugenerator.databinding.ActivityMainBinding
import com.far.menugenerator.model.database.model.CompanyFirebase
import com.far.menugenerator.model.database.model.MenuFirebase
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.ScreenNavigation
import javax.inject.Inject

class MenuActivity : BaseActivity() {

    @Inject lateinit var navigation:ScreenNavigation

    private lateinit var _binding:ActivityMainBinding

    private var company: CompanyFirebase? = null
    private var menuFirebase:MenuFirebase?=null

    companion object {
        const val ARG_COMPANY = "company"
        const val ARG_MENU="menu"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)
        hideActionBar()

        company = intent.getSerializableExtra(ARG_COMPANY) as CompanyFirebase?
        menuFirebase = intent.getSerializableExtra(ARG_MENU) as MenuFirebase?

        navigation.createMenuFragment(company = company!!,menuFirebase = menuFirebase)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        navigation.currentFragmentOnActivityResult(requestCode, resultCode, data)
    }
}