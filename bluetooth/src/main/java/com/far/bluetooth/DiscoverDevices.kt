package com.far.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.far.bluetooth.databinding.ActivityDiscoverDevicesBinding
import java.util.UUID

const val REQUEST_CODE_BLUETOOTH_SCAN = 1
const val REQUEST_ENABLE_BT = 2
class DiscoverDevices : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDiscoverDevicesBinding

    private lateinit var bluetoothAdapter:BluetoothAdapter

    private lateinit var adapter:ArrayAdapter<String>
    private var list = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDiscoverDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_content_discover_devices)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        adapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,list)

        //init()
        startScan()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                list.add(device?.address?:"unknown")
                // Add the device to a list or display its name and address
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }



    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_discover_devices)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK){
            startScan()
        }
    }

    private fun init(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_CODE_BLUETOOTH_SCAN)
        }
    }

    private fun startScan(){
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
            // Proceed with Bluetooth scanning
            startDiscovery()
        } else {
            // Handle the case where Bluetooth is not available or enabled
            // Prompt the user to enable Bluetooth if needed
            enableBluetooth()
        }
    }

    private fun startDiscovery(){
        val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, discoverDevicesIntent)
        bluetoothAdapter.startDiscovery()
    }

    private fun enableBluetooth(){
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun connect(deviceAddress:String){
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        val socket = device.createRfcommSocketToServiceRecord(UUID.randomUUID())

        socket.connect()
    }
}