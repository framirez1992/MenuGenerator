package com.far.menugenerator.view.adapters

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.databinding.RowBluetoothDeviceBinding

class BluetoothDevicesAdapter(
    private val devices:MutableList<BluetoothDevice>,
    private val onClick:(BluetoothDevice)->Unit): RecyclerView.Adapter<BluetoothDevicesAdapter.BTViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BTViewHolder {
        val binding: RowBluetoothDeviceBinding = RowBluetoothDeviceBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return BTViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(holder: BTViewHolder, position: Int) {
        holder.bind(devices[position])
        holder.binding.root.setOnClickListener{
            onClick(devices[position])
        }
    }
    /*
        private fun remove(id:Int){
            categories.removeAt(id)
            notifyDataSetChanged()
        }*/

    class BTViewHolder(val binding: RowBluetoothDeviceBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(device: BluetoothDevice){
            binding.name.text = device.name
            binding.address.text = device.address
        }
    }
}