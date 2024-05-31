package com.far.menugenerator.view

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.far.menugenerator.R
import com.far.menugenerator.databinding.ActivityBluetoothConnectBinding
import com.far.menugenerator.databinding.DialogSeachFilePermissionBinding
import com.far.menugenerator.view.adapters.BluetoothDevicesAdapter
import com.far.menugenerator.view.common.DialogManager
import com.far.menugenerator.view.common.MyBluetoothPrintersConnections
import javax.inject.Inject


private const val REQUEST_ENABLE_BT= 1
private const val REQUEST_PERMISSION_ENABLE_BT= 11
class BluetoothConnect : AppCompatActivity() {

    private lateinit var _binding:ActivityBluetoothConnectBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    @Inject lateinit var dialogManager:DialogManager

    private  lateinit var bluetoothAdapter: BluetoothAdapter
    private var devices = mutableListOf<BluetoothDevice>()

    private lateinit var mac:String

    //@SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityBluetoothConnectBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
        }

        _binding.rv.layoutManager = LinearLayoutManager(this, VERTICAL, false)
        _binding.rv.adapter = BluetoothDevicesAdapter(devices = devices){
            printdata(it)
            /*
            lifecycleScope.launch {
                try {
                    //myParingDevice.setPairingConfirmation(true);
                    //esto es para conexiones standar a bluetooh
                    val m: Method = it.javaClass.getMethod(
                        "createRfcommSocket",
                        Int::class.javaPrimitiveType
                    )
                    val socket = m.invoke(it, 1) as BluetoothSocket
                    socket.connect()
                    val ALIGN_CENTER = byteArrayOf(0x1b, 0x61, 0x01)
                    val ALIGN_LEFT = byteArrayOf(0x1b, 0x61, 0x00)
                    val ALIGN_RIGHT = byteArrayOf(0x1b, 0x61, 0x02)
                    val TEXT_SIZE_NORMAL = byteArrayOf(0x1b, 0x21, 0x00)
                    val TEXT_SIZE_LARGE = byteArrayOf(0x1b, 0x21, 0x30)
                    val INVERTED_COLOR_ON = byteArrayOf(0x1d, 0x42, 0x01)
                    val BEEPER = byteArrayOf(0x1b, 0x42, 0x05, 0x05)
                    val INIT = byteArrayOf(0x1b, 0x40)

                    //socket.outputStream.write(ALIGN_CENTER)
                    //socket.outputStream.write(TEXT_SIZE_LARGE)
                    //socket.outputStream.write("Configurado\n\n".toByteArray())



                }catch (e:Exception){
                    Log.d("aaa",e.message!!)
                }
            }*/


        }

        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher. You can use either a val, as shown in this snippet,
        // or a lateinit var in your onAttach() or onCreate() method.
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    //readExternalStorage()
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    //showInContextUI();
                }
            }

        _binding.btnScan.setOnClickListener{

            if (bluetoothAdapter?.isEnabled == false) {
                requestPermission(
                    permission = Manifest.permission.BLUETOOTH_CONNECT,
                    success =  {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                    },
                    showRationale = {
                        showInContextUI(Manifest.permission.BLUETOOTH_CONNECT)
                    })

            }else{
                fillBondedDevices()
                startScan()
            }
        }

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode  == REQUEST_ENABLE_BT  && resultCode == Activity.RESULT_OK){
            fillBondedDevices()
            startScan()
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action!!
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    devices.add(device)
                    refreshAdapter()
                }
            }
        }
    }

    private fun requestPermission(permission:String, success:()->Unit, showRationale:()->Unit){
        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                success()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, permission) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
               showRationale()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                askForPermission(permission)
            }
        }
    }


    private fun askForPermission(permission: String){
        requestPermissionLauncher.launch(permission)
    }

    private fun showInContextUI(permission: String) {
        val binding = DialogSeachFilePermissionBinding.inflate(layoutInflater)
        dialogManager.showTwoButtonsDialog(
            binding.root,
            R.string.allow,
            {
                askForPermission(permission)
            },
            R.string.cancel)

    }

    private fun fillBondedDevices(){
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        devices.addAll(pairedDevices?: listOf())
        refreshAdapter()
    }
    private fun refreshAdapter(){
        _binding.rv.adapter?.notifyDataSetChanged()
    }
    private fun startScan(){
        requestPermission(
            permission =  Manifest.permission.BLUETOOTH_SCAN,
            {

                //bluetoothAdapter.startDiscovery()
            },
            {
                showInContextUI(Manifest.permission.BLUETOOTH_SCAN)
            })

    }

    fun printdata(bluetoothDevice:BluetoothDevice) {
        try{
            val device =  BluetoothConnection(bluetoothDevice)
            device.connect()
            val printer = EscPosPrinter(device, 203, 48f, 32)
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

            printer.printFormattedText("""
                [C]<qrcode size='35'>https://tinyurl.com/4t7jm79w</qrcode>
                [L]
                [C]<font size='normal'><b>${"https://tinyurl.com/4t7jm79w".uppercase()}</b></font>
            """.trimIndent())
        }catch (e:Exception){
            Log.i("ERROR", e.message!!)
        }
    }



}