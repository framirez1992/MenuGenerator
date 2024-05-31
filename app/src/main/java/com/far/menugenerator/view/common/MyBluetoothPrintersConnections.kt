package com.far.menugenerator.view.common

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnections
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosConnectionException

class MyBluetoothPrintersConnections : BluetoothConnections() {
    /**
     * Get a list of bluetooth printers.
     *
     * @return an array of EscPosPrinterCommands
     */
    @SuppressLint("MissingPermission")
    override fun getList(): Array<BluetoothConnection?>? {
        val bluetoothDevicesList = super.getList() ?: return null
        var i = 0
        val printersTmp = arrayOfNulls<BluetoothConnection>(bluetoothDevicesList.size)
        for (bluetoothConnection in bluetoothDevicesList) {
            val device = bluetoothConnection.device
            val majDeviceCl = device.bluetoothClass.majorDeviceClass
            val deviceCl = device.bluetoothClass.deviceClass
            if (majDeviceCl == BluetoothClass.Device.Major.IMAGING && (deviceCl == 1664 || deviceCl == BluetoothClass.Device.Major.IMAGING) || device.name == "InnerPrinter") {
                printersTmp[i++] = BluetoothConnection(device)
            }
        }
        val bluetoothPrinters = arrayOfNulls<BluetoothConnection>(i)
        System.arraycopy(printersTmp, 0, bluetoothPrinters, 0, i)
        return bluetoothPrinters
    }

    fun getDevice(mac:String):BluetoothConnection?{
        val bluetoothDevicesList = super.getList() ?: return null
        for (bluetoothConnection in bluetoothDevicesList) {
            if(bluetoothConnection.device.address == mac){
                return BluetoothConnection(bluetoothConnection.device)
            }
        }
        return null
    }

    companion object {
        /**
         * Easy way to get the first bluetooth printer paired / connected.
         *
         * @return a EscPosPrinterCommands instance
         */
        fun selectFirstPaired(): BluetoothConnection? {
            val printers =  MyBluetoothPrintersConnections()
            val bluetoothPrinters = printers.list
            if (bluetoothPrinters != null && bluetoothPrinters.size > 0) {
                for (printer in bluetoothPrinters) {
                    try {
                        return printer?.connect()
                    } catch (e: EscPosConnectionException) {
                        e.printStackTrace()
                    }
                }
            }
            return null
        }
    }
}