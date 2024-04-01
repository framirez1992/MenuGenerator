package com.far.menugenerator.view.common

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import androidx.annotation.StringRes

import androidx.fragment.app.DialogFragment
import com.far.menugenerator.R
import com.far.menugenerator.databinding.DialogImageTitleDescriptionBinding
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.dialogs.DialogLoading
import com.far.menugenerator.view.dialogs.DialogProductEdit
import com.google.android.material.dialog.MaterialAlertDialogBuilder



class DialogManager(private val baseActivity: BaseActivity) {

    private val activity get() = baseActivity
    private val fragmentManager get() = activity.supportFragmentManager
    private lateinit var currentDialog:DialogFragment
    private var currentLoadingDialog:DialogLoading?=null
    fun showDialogEditProduct(dialogEditProductEditItem: DialogProductEdit.DialogProductEditItem){
        currentDialog = DialogProductEdit.newInstance(dialogEditProductEditItem)
        showDialog()
    }

    /*
    fun showOptionDialog(@StringRes title:Int,
                         @StringRes message:Int,
                         @StringRes positiveText:Int,
                         @StringRes negativeText:Int,
                         positiveListener:DialogInterface.OnClickListener){

        val dialog = MaterialAlertDialogBuilder(activity).apply {
            setTitle(title)
            setMessage(message)
            setPositiveButton(positiveText,positiveListener)
            setNegativeButton(negativeText){_,_->

            }
        }
        dialog.show()
    }*/

    fun showLoadingDialog(){
        if(currentLoadingDialog == null){
            currentLoadingDialog = DialogLoading.newInstance()
        }
        currentLoadingDialog!!.show(fragmentManager, "progress dialog")
    }
    fun dismissLoadingDialog(){
        if(currentLoadingDialog != null){
            currentLoadingDialog!!.dismissAllowingStateLoss()
            currentLoadingDialog = null
        }
    }

    fun showImageBottomSheet(options:List<ImageOption>,onclick:(ImageOption)->Unit){
        val modal = ImageOptionBottomSheet(options,onclick)
        fragmentManager.let { modal.show(it, ImageOptionBottomSheet.TAG) }
    }

    fun getMaterialDialogBuilder(view: View):MaterialAlertDialogBuilder{
        val dialog = MaterialAlertDialogBuilder(activity).apply {
            setView(view)
        }
        return dialog
    }

    fun showInternetErrorDialog(){
        val binding = DialogImageTitleDescriptionBinding.inflate(baseActivity.layoutInflater)
        binding.img.setImageResource(R.drawable.no_internet)
        binding.title.setText(R.string.network_error)
        binding.body.setText(R.string.check_your_network)
        showSingleButtonDialog(binding.root)
    }

    fun showSingleButtonDialog(view:View,onButtonClick:()->Unit = {}){
        val dialogBuilder = getMaterialDialogBuilder(view)
        dialogBuilder.setCancelable(false)
        dialogBuilder.setPositiveButton(R.string.close){ dialog, _->
            onButtonClick()
            dialog.dismiss()
        }
        dialogBuilder.show()
    }

    fun showTwoButtonsDialog(view:View,
                             @StringRes button1Label:Int,
                             onButton1Click:()->Unit = {},
                             @StringRes button2Label:Int,
                             onButton2Click:()->Unit = {}){
        val dialogBuilder = getMaterialDialogBuilder(view)
        dialogBuilder.setCancelable(false)
        dialogBuilder.setPositiveButton(button1Label){ dialog, _->
            onButton1Click()
            dialog.dismiss()
        }
        dialogBuilder.setNegativeButton(button2Label){ dialog, _->
            onButton2Click()
            dialog.dismiss()
        }
        dialogBuilder.show()
    }

    private fun showDialog(){
        fragmentManager.let {
            currentDialog.show(it, "DialogFragment")
        }
    }



}