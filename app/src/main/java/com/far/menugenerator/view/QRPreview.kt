package com.far.menugenerator.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import com.far.menugenerator.R
import com.far.menugenerator.databinding.FragmentQRPreviewBinding
import com.far.menugenerator.model.ProcessState
import com.far.menugenerator.model.State
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.common.BaseActivity
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.viewModel.QRPreviewViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File
import javax.inject.Inject


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


class QRPreview : BaseActivity() {
    // TODO: Rename and change types of parameters
    private var menuRef: String? = null
    private var companyId:String?=null
    private var filePath:File? =null

    private lateinit var binding:FragmentQRPreviewBinding
    private lateinit var _viewModel: QRPreviewViewModel

    @Inject lateinit var factory:QRPreviewViewModel.QRPreviewViewModelFactory
    @Inject lateinit var dialogManager: DialogManager

    private var mainMenu:Menu? = null
    private var optionSelected:ImageOption? = null

    companion object {
         const val ARG_MENU_REF = "menuRef"
         const val ARG_COMPANY_ID = "companyId"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentationComponent.inject(this)
        _viewModel = ViewModelProvider(this,factory)[QRPreviewViewModel::class.java]
        binding = FragmentQRPreviewBinding.inflate(layoutInflater)

        setContentView(binding.root)

        menuRef = intent.extras?.getString(ARG_MENU_REF)
        companyId = intent.extras?.getString(ARG_COMPANY_ID)

        if(menuRef.isNullOrEmpty() or companyId.isNullOrEmpty()){
            finish()
        }

        initViews()
        initObservers()

        searchMenu()
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
            searchMenu()

        return true
    }
    private fun initViews() {
       binding.imgQR.setOnClickListener{
           showOptions()
       }
    }

    private fun initObservers(){
        _viewModel.getState().observe(this){
            loadState(it)
        }
        _viewModel.getQrBitmap().observe(this){
            binding.imgQR.setImageBitmap(it)
        }
        _viewModel.getStateFileDownload().observe(this){
            processFileDownloadState(it)
        }

    }
    private fun searchMenu(){
        _viewModel.drawMenu(user = LoginActivity.account?.email!!, companyId = companyId!!, fireBaseRef = menuRef!!)
    }

    private fun showOptions(){
        val options = listOf(
            ImageOption(icon = R.drawable.baseline_remove_red_eye_24, R.string.preview),
            ImageOption(icon = R.drawable.baseline_file_present_24,R.string.share_menu),
            ImageOption(icon = R.drawable.rounded_qr_code_2_24,R.string.share_qr_code)
        )
        dialogManager.showImageBottomSheet(options){
            optionSelected = it
            when (it.string) {
                R.string.share_qr_code -> shareBitmap(binding.imgQR.drawable.toBitmap())
                R.string.share_menu ->  fileOperation()
                R.string.preview -> fileOperation()
            }

        }
    }

    private fun fileOperation(){
        if(filePath == null){
            filePath = File(applicationContext.filesDir, "menu.pdf") // adjust extension based on file type
            _viewModel.getFile(filePath!!)
        }else if(optionSelected?.string == R.string.share_menu){
            shareFile(filePath!!)
        }else if(optionSelected?.string == R.string.preview){
            openFile(filePath!!)
        }
    }

    private fun loadState(process: ProcessState){
        binding.pb.visibility = if(process.state == State.LOADING) View.VISIBLE else View.GONE
        binding.cvQR.visibility = if(process.state == State.SUCCESS) View.VISIBLE else View.GONE
        prepareMenuItems(process.state == State.SUCCESS)

        if(process.state == State.ERROR)
            Snackbar.make(binding.root,R.string.operation_failed_please_retry,Snackbar.LENGTH_LONG).show()


    }
    private fun processFileDownloadState(process:ProcessState){
        binding.pb.visibility = if(process.state == State.LOADING) View.VISIBLE else View.GONE
        if(process.state == State.ERROR){
            Snackbar.make(binding.root,R.string.operation_failed_please_retry,Snackbar.LENGTH_LONG).show()
            filePath = null
        }else if(process.state == State.SUCCESS){
            fileOperation()
        }

    }

    private fun shareBitmap(bitmap: Bitmap) {
        val file = File(applicationContext.filesDir, "qr_menu.png")
        file.outputStream().use { os ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
        }
        shareFile(file)

    }
    private fun shareFile(file:File){
        //crear xml/paths indicando las rutasde archivos a las que vamos a dar acceso
        //Definir provider en el manifest con nuestro authority (nombre.de.paquete.fileprovider)
        //especificar en el metadata del provider las rutas a las que vamos a dar acceso

        //com.far.menugenerator.fileprovider
        val authority = "$packageName.fileprovider"
        val uri = FileProvider.getUriForFile(applicationContext, authority, file)
        val mime = contentResolver.getType(uri)

        val intent = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            //.putExtra(Intent.EXTRA_TEXT, "Check out this cool image!")

        val chooser = Intent.createChooser(intent, /* title */ null)
        try {
            startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            // Define what your app should do if no activity can handle the intent.
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun openFile(file:File) {
        val authority = "$packageName.fileprovider"
        val uri = FileProvider.getUriForFile(applicationContext, authority, file)
        val mime = contentResolver.getType(uri)

// Prepare an implicit intent.
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)

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