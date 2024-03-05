package com.far.menugenerator.view.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.R
import com.far.menugenerator.databinding.RowMenuItemBinding
import com.far.menugenerator.model.common.MenuReference
import com.far.menugenerator.model.database.model.MenuFirebase

class MenuAdapter(private val menus:List<MenuReference?>,val onclick:(MenuReference)->Unit):RecyclerView.Adapter<MenuAdapter.MenuAdapterVH>() {


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
        fun bind(menu:MenuReference){
            binding.tvMenuName.text = menu.name
            binding.imgStatus.setImageResource(if(menu.online) R.drawable.outline_cloud_done_24 else R.drawable.baseline_cloud_off_24)
            val color = binding.root.context.resources.getColor(if(menu.online) R.color.green_900 else R.color.red_A700)
            binding.imgStatus.imageTintList = ColorStateList.valueOf(color)
        }
    }
}