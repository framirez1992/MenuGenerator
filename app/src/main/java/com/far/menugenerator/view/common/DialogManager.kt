package com.far.menugenerator.view.common

import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import androidx.annotation.StringRes

import androidx.fragment.app.DialogFragment
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
    }

    fun showLoadingDialog(){
        if(currentLoadingDialog == null){
            currentLoadingDialog = DialogLoading.newInstance()
        }
        currentLoadingDialog!!.show(fragmentManager, "progress dialog")
    }
    fun dismissLoadingDialog(){
        if(currentLoadingDialog != null){
            currentLoadingDialog!!.dismiss()
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

    private fun showDialog(){
        fragmentManager.let {
            currentDialog.show(it, "DialogFragment")
        }
    }



}