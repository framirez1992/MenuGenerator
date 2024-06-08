package com.far.menugenerator.view

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.R
import com.far.menugenerator.common.helpers.ActivityHelper
import com.far.menugenerator.common.utils.PreferenceUtils
import com.far.menugenerator.common.utils.PrintUtils
import com.far.menugenerator.databinding.FragmentQRPreviewBinding
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.viewModel.QRPreviewViewModel
import com.far.menugenerator.viewModel.model.ProcessState
import com.far.menugenerator.viewModel.model.State
import com.google.android.material.snackbar.Snackbar
import java.io.File
import javax.inject.Inject


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


class QRPreview : BaseActivity() {
    // TODO: Rename and change types of parameters
    //private var menuRef: MenuReference? = null
    //private var companyId:String?=null

    private lateinit var binding:FragmentQRPreviewBinding
    private lateinit var viewModel: QRPreviewViewModel

    @Inject lateinit var factory:QRPreviewViewModel.QRPreviewViewModelFactory
    @Inject lateinit var dialogManager: DialogManager

    private var mainMenu:Menu? = null
    private var optionSelected:ImageOption? = null

    companion object {
        const val ARG_USER_ID="userId"
        const val ARG_COMPANY_ID = "companyId"
        const val ARG_MENU_ID = "menuId"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        viewModel = ViewModelProvider(this,factory)[QRPreviewViewModel::class.java]
        binding = FragmentQRPreviewBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val userId:String?
        val companyId:String?
        val menuId:String?
        if(savedInstanceState == null){
            userId = intent.extras?.getString(ARG_USER_ID)
            companyId = intent.extras?.getString(ARG_COMPANY_ID)
            menuId = intent.extras?.getString(ARG_MENU_ID)
        }else{
            userId = savedInstanceState.getString(ARG_USER_ID)
            companyId = savedInstanceState.getString(ARG_COMPANY_ID)
            menuId = savedInstanceState.getString(ARG_MENU_ID)
        }

        if(LoginActivity.userFirebase == null  || userId.isNullOrEmpty() || companyId.isNullOrEmpty() || menuId.isNullOrEmpty()){
            finish()
        }else {

            if(viewModel.userId.isNullOrEmpty() || viewModel.companyId.isNullOrEmpty() || viewModel.menuId.isNullOrEmpty()){
                viewModel.initialize(userId = userId,companyId = companyId, menuId = menuId)
            }

            initViews()
            initObservers()
            drawQRCode()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_USER_ID, viewModel.userId)
        outState.putString(ARG_COMPANY_ID,viewModel.companyId)
        outState.putString(ARG_MENU_ID, viewModel.menuId)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_qr_preview, menu)
        mainMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
         if(item.itemId == R.id.optionMenu)
            showOptions()
        else if(item.itemId == R.id.optionRefresh)
            drawQRCode()

        return true
    }
    private fun initViews() {
       binding.imgQR.setOnClickListener{
           showOptions()
       }
    }

    private fun initObservers(){
        viewModel.getState().observe(this){
            loadState(it)
        }
        viewModel.getQrBitmap().observe(this){
            binding.imgQR.setImageBitmap(it)
        }

    }

    private fun drawQRCode(){
        viewModel.drawMenu()
    }

    private fun showOptions(){
        val options = mutableListOf(
            //ImageOption(icon = R.drawable.baseline_remove_red_eye_24, R.string.preview),
            //ImageOption(icon = R.drawable.baseline_file_present_24,R.string.share_menu),
            ImageOption(icon = R.drawable.rounded_qr_code_2_24,R.string.share_qr_code)
        )
        if(PreferenceUtils.getAdminPreference(this,false)){
            options.add(ImageOption(icon = R.drawable.baseline_bluetooth_searching_24, R.string.scan))
            options.add(ImageOption(icon = R.drawable.round_print_24,R.string.print))
        }
        dialogManager.showImageBottomSheet(options){
            optionSelected = it
            when (it.string) {
                R.string.share_qr_code -> shareBitmap(binding.imgQR.drawable.toBitmap())
                R.string.scan -> startActivity(Intent(this, BluetoothConnect::class.java))
                R.string.print -> printQR()
            }

        }
    }

    @SuppressLint("MissingPermission")
    private fun printQR() {
        val mac = PreferenceUtils.getMacAddress(this, "")
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothDevice = bluetoothManager.adapter.bondedDevices.firstOrNull{ it.address ==  mac}
        if(bluetoothDevice != null){
            val p = PrintUtils(bluetoothDevice)
            p.printQRCode(
                data =  viewModel.getMenu().value?.shorUrl!!,
                onSuccess = {

                },
                onFail = {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                })
        }else{
            Toast.makeText(this, getString(R.string.not_found), Toast.LENGTH_LONG).show()
        }

    }


    private fun loadState(process: ProcessState){
        binding.pb.visibility = if(process.state == State.LOADING) View.VISIBLE else View.GONE
        binding.cvQR.visibility = if(process.state == State.SUCCESS) View.VISIBLE else View.GONE
        prepareMenuItems(process.state == State.SUCCESS)

        if(process.state == State.GENERAL_ERROR)
            Snackbar.make(binding.root,process.message?:getString(R.string.operation_failed_please_retry),Snackbar.LENGTH_LONG).show()
        else if(process.state == State.NETWORK_ERROR){
            dialogManager.showInternetErrorDialog()
        }


    }

    private fun shareBitmap(bitmap: Bitmap) {
        val file = File(applicationContext.filesDir, "qr_menu.png")
        file.outputStream().use { os ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
        }
        ActivityHelper.shareFile(this,file, viewModel.getMenu().value?.shorUrl?:"")

    }


    //override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    //    super.onActivityResult(requestCode, resultCode, data)
    //    if (requestCode == REQUEST_CODE_SHARE_IMAGE && resultCode == RESULT_OK) {
    //        // Clean up the temporary file
    //        file.delete()
    //    }
    //}

    private fun prepareMenuItems(dataLoaded: Boolean){
        mainMenu?.findItem(R.id.optionRefresh)?.isVisible = !dataLoaded
        mainMenu?.findItem(R.id.optionMenu)?.isVisible = dataLoaded
    }

}