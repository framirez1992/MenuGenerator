package com.far.menugenerator.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.databinding.RowItemCategoryBinding

class CategoriesAdapter(private val categories:MutableList<String>): RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding:RowItemCategoryBinding = RowItemCategoryBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int {
       return categories.size
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
        holder.binding.btnDelete.setOnClickListener{
            remove(position)
        }
    }

    private fun remove(position:Int){
        categories.removeAt(position)
        notifyDataSetChanged()
    }

    class CategoryViewHolder(val binding:RowItemCategoryBinding):RecyclerView.ViewHolder(binding.root){

        fun bind(category: String){
            binding.categoryName.text = category
        }
    }
}