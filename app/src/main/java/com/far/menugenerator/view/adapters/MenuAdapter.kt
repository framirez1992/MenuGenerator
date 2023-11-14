package com.far.menugenerator.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.databinding.RowMenuItemBinding
import com.far.menugenerator.model.database.model.MenuFirebase

class MenuAdapter(private val menus:List<MenuFirebase?>,val onclick:(MenuFirebase)->Unit):RecyclerView.Adapter<MenuAdapter.MenuAdapterVH>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuAdapterVH {
       val binding = RowMenuItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MenuAdapterVH(binding)
    }

    override fun getItemCount(): Int {
        return menus.size
    }

    override fun onBindViewHolder(holder: MenuAdapterVH, position: Int) {
        holder.bind(menus[position]!!)
        holder.itemView.setOnClickListener{
            onclick(menus[position]!!)
        }
    }

    class MenuAdapterVH(private val binding:RowMenuItemBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(menu:MenuFirebase){
            binding.tvMenuName.text = menu.name
        }
    }
}