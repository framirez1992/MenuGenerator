package com.far.menugenerator.view.common

import android.R.id.message
import android.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.far.menugenerator.view.adapters.ImageOption
import com.far.menugenerator.view.dialogs.DialogLoading
import com.far.menugenerator.view.dialogs.DialogProductEdit


class DialogManager(private val baseActivity: BaseActivity) {

    private val activity get() = baseActivity
    private val fragmentManager get() = activity.supportFragmentManager
    private lateinit var currentDialog:DialogFragment
    private var currentLoadingDialog:DialogLoading?=null
    fun showDialogEditProduct(dialogEditProductEditItem: DialogProductEdit.DialogProductEditItem){
        currentDialog = DialogProductEdit.newInstance(dialogEditProductEditItem)
        showDialog()
    }

    fun showOptionDialog(title:String,options:Array<String>,listener:(String)->Unit){
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setItems(options){ dialog, which ->
            listener(options[which])
            dialog.dismiss()
        }
        builder.show()
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

    private fun showDialog(){
        fragmentManager.let {
            currentDialog.show(it, "DialogFragment")
        }
    }


}