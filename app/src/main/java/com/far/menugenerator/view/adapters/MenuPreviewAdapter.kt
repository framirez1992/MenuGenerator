package com.far.menugenerator.view.adapters


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.far.menugenerator.R
import com.far.menugenerator.common.utils.NumberUtils
import com.far.menugenerator.databinding.ItemMenuPreviewBinding
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.view.common.BaseActivity
import java.util.*


class MenuPreviewAdapter(private val activity:BaseActivity,private val itemPreviewList:List<ItemPreview>, private val onclick:(ItemPreview)->Unit): RecyclerView.Adapter<MenuPreviewAdapter.MenuPreviewViewHolder>() {

    private var preview:MutableList<ItemPreview> = mutableListOf()
    val currentPreview get() = preview
    init {
       organizeArray()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuPreviewViewHolder {
        val binding = ItemMenuPreviewBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MenuPreviewViewHolder(binding)
    }

    override fun getItemCount(): Int {
       return preview.size
    }

    override fun onBindViewHolder(holder: MenuPreviewViewHolder, position: Int) {
        holder.bind(activity,preview[position])
        holder.binding.options.btnUp.setOnClickListener{
            moveUp(preview[position])
        }
        holder.binding.options.btnDown.setOnClickListener{
            moveDown(preview[position])
        }
        holder.itemView.setOnClickListener {
            if(preview[position].itemStyle == ItemStyle.MENU_CATEGORY_HEADER) return@setOnClickListener
            onclick(preview[position])
        }

    }

    private fun organizeArray(){
        preview.clear()
        val headers = itemPreviewList.filter { it.itemStyle == ItemStyle.MENU_CATEGORY_HEADER }.sortedBy { it.position }
        headers.forEach{ categoryPreview->
            val category = categoryPreview.item
            var items = itemPreviewList.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.item.category ==  category.name}.sortedBy { it.position }
            if(items.isNotEmpty()){//show only relevant data
                preview.add(categoryPreview)
                preview.addAll(items)
            }

        }
    }


    private fun moveUp(currentItem: ItemPreview){
        when(currentItem.itemStyle){
            ItemStyle.MENU_CATEGORY_HEADER->{
                var categories = preview.filter { it.itemStyle == ItemStyle.MENU_CATEGORY_HEADER }
                if(categories.first() == currentItem) return

                var itemBefore = categories[categories.indexOf(currentItem) -1]
                Collections.swap(preview,preview.indexOf(itemBefore),preview.indexOf(currentItem))

                categories.forEach{ categoryPreview->
                    val category = categoryPreview.item
                    var items = preview.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.item.category == category.name }
                    items.reversed().forEach{
                        if(preview.last() == categoryPreview){
                            preview.remove(it)
                            preview.add(it) // at the end
                        }else{
                            preview.remove(it)
                            preview.add(preview.indexOf(categoryPreview)+1,it)// after current category
                        }

                    }
                }


            }
            else -> {
                var products = preview.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.item.category == currentItem.item.category }
                if(products.first() == currentItem) return

                var itemBefore = products[products.indexOf(currentItem) -1]
                Collections.swap(preview,preview.indexOf(itemBefore),preview.indexOf(currentItem))
            }
        }
        notifyDataSetChanged()
    }


    private fun moveDown(currentItem: ItemPreview){
        when(currentItem.itemStyle){
            ItemStyle.MENU_CATEGORY_HEADER->{
                var categories = preview.filter { it.itemStyle == ItemStyle.MENU_CATEGORY_HEADER }
                if(categories.last() == currentItem) return

                var itemAfter = categories[categories.indexOf(currentItem) +1]
                Collections.swap(preview,preview.indexOf(itemAfter),preview.indexOf(currentItem))

                categories.forEach{ category->
                    var items = preview.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.item.category == category.item.name }
                    items.reversed().forEach{
                        if(preview.last() == category){
                            preview.remove(it)
                            preview.add(it) // at the end
                        }else{
                            preview.remove(it)
                            preview.add(preview.indexOf(category)+1,it)// after current category
                        }

                    }
                }


            }
            else -> {
                var products = preview.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.item.category == currentItem.item.category }
                if(products.last() == currentItem) return

                var itemAfter = products[products.indexOf(currentItem) +1]
                Collections.swap(preview,preview.indexOf(itemAfter),preview.indexOf(currentItem))
            }
        }
        notifyDataSetChanged()
    }


    class MenuPreviewViewHolder(val binding:ItemMenuPreviewBinding):RecyclerView.ViewHolder(binding.root){

        fun bind(activity: BaseActivity,itemPreview: ItemPreview){
            binding.imageTitleDescription.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE) View.VISIBLE else View.GONE
            binding.titleDescription.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE) View.VISIBLE else View.GONE
            binding.titlePrice.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_TITLE_PRICE) View.VISIBLE else View.GONE
            binding.categoryTitle.root.visibility = if (itemPreview.itemStyle == ItemStyle.MENU_CATEGORY_HEADER) View.VISIBLE else View.GONE

            val item = itemPreview.item
            when (itemPreview.itemStyle) {
                ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE -> {
                    binding.imageTitleDescription.title.text = item.name
                    binding.imageTitleDescription.body.text = item.description
                    binding.imageTitleDescription.price.text = item.amount.toString()
                    Glide.with(activity)
                        .load(item.localImage?:item.remoteImage)
                        .into(binding.imageTitleDescription.image)

                }
                ItemStyle.MENU_TITLE_DESCRIPTION_PRICE -> {
                    binding.titleDescription.title.text = item.name
                    binding.titleDescription.body.text = item.description
                    binding.titleDescription.price.text = item.amount.toString()
                }
                ItemStyle.MENU_TITLE_PRICE -> {
                    binding.titlePrice.title.text = item.name
                    binding.titlePrice.price.text = item.amount.toString()
                }
                else -> {
                    binding.categoryTitle.title.text = item.name
                }
            }




            // Get the layout parameters of the view.
            val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            if(itemPreview.itemStyle == ItemStyle.MENU_CATEGORY_HEADER)
                layoutParams.setMargins(0, 20, 0, 0)
            else
                layoutParams.setMargins(0, 0, 0, 0)

            binding.root.layoutParams = layoutParams
        }


    }
}