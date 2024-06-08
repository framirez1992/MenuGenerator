package com.far.menugenerator.common.utils

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection

class PrintUtils(private val bluetoothDevice: BluetoothDevice) {
    fun printConnected(onSuccess:()->Unit, onFail:(Exception)->Unit){
        try{
            val device =  BluetoothConnection(bluetoothDevice)
            device.connect()
            val printer = EscPosPrinter(device, 203, 48f, 32)

            printer.printFormattedText("""
                [C]<font size='normal'>connected</font>
                [L]
                [L]
            """.trimIndent())
            onSuccess()

        }catch (e:Exception){
            Log.i("ERROR", e.message!!)
            onFail(e)
        }
    }

    fun printQRCode(data:String, onSuccess:()->Unit, onFail:(Exception)->Unit){
        try{
            val device =  BluetoothConnection(bluetoothDevice)
            device.connect()
            val printer = EscPosPrinter(device, 203, 48f, 32)
            printer.printFormattedText("""
                [L]
                [C]<qrcode size='35'>${data}</qrcode>
                [L]
                [C]<font size='normal'><b>${data.uppercase()}</b></font>
                [C]<font size='normal'><b>--------------------------------</b></font>
            """.trimIndent())
            onSuccess()
        }catch (e:Exception){
            Log.i("ERROR", e.message!!)
            onFail(e)
        }
    }

    /*
            printer
                .printFormattedText(
                    """
        [C]<img>${
                        PrinterTextParserImg.bitmapToHexadecimalString(
                            printer,
                            this.applicationContext.resources.getDrawableForDensity(
                                R.drawable.delete,
                                DisplayMetrics.DENSITY_MEDIUM
                            )
                        )
                    }</img>
        [L]
        [C]<u><font size='big'>ORDER N°045</font></u>
        [L]
        [C]================================
        [L]
        [L]<b>BEAUTIFUL SHIRT</b>[R]9.99e
        [L]  + Size : S
        [L]
        [L]<b>AWESOME HAT</b>[R]24.99e
        [L]  + Size : 57/58
        [L]
        [C]--------------------------------
        [R]TOTAL PRICE :[R]34.98e
        [R]TAX :[R]4.23e
        [L]
        [C]================================
        [L]
        [L]<font size='tall'>Customer :</font>
        [L]Raymond DUPONT
        [L]5 rue des girafes
        [L]31547 PERPETES
        [L]Tel : +33801201456
        [L]
        [C]<barcode type='ean13' height='10'>831254784551</barcode>
        [C]<qrcode size='20'>https://dantsu.com/</qrcode>
        """.trimIndent()
                )*/
}