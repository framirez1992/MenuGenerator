package com.far.menugenerator.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.databinding.ItemMenuPreviewBinding
import com.far.menugenerator.model.ItemPreview
import com.far.menugenerator.model.ItemStyle
import java.util.*

class MenuPreviewAdapter(private val itemPreviewList:List<ItemPreview>): RecyclerView.Adapter<MenuPreviewAdapter.MenuPreviewViewHolder>() {

    private var preview:MutableList<ItemPreview> = mutableListOf()

    init {
       organizeArray()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuPreviewViewHolder {
        val binding = ItemMenuPreviewBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return MenuPreviewViewHolder(binding)
    }

    override fun getItemCount(): Int {
       return itemPreviewList.size
    }

    override fun onBindViewHolder(holder: MenuPreviewViewHolder, position: Int) {
        holder.bind(preview[position])
        holder.itemView.setOnClickListener{
            //moveUp(preview[position])
            moveDown(preview[position])
        }

    }

    private fun organizeArray(){
        preview.clear()
        //val orderComparator: Comparator<ItemPreview> = Comparator { s1, s2 -> s1.position.compareTo(s2.position) }
        val headers = itemPreviewList.filter { it.itemStyle == ItemStyle.MENU_CATEGORY_HEADER }.sortedBy { it.position }
        headers.forEach{ category->
            preview.add(category)
            var items = itemPreviewList.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.categoryName ==  category.name}.sortedBy { it.position }
            preview.addAll(items)
        }
    }


    private fun moveUp(currentItem: ItemPreview){
        when(currentItem.itemStyle){
            ItemStyle.MENU_CATEGORY_HEADER->{
                var categories = preview.filter { it.itemStyle == ItemStyle.MENU_CATEGORY_HEADER }
                if(categories.first() == currentItem) return

                var itemBefore = categories[categories.indexOf(currentItem) -1]
                Collections.swap(preview,preview.indexOf(itemBefore),preview.indexOf(currentItem))

                categories.forEach{ category->
                    var items = preview.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.categoryName == category.categoryName }
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
                var products = preview.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.categoryName == currentItem.categoryName }
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
                    var items = preview.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.categoryName == category.categoryName }
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
                var products = preview.filter { it.itemStyle != ItemStyle.MENU_CATEGORY_HEADER && it.categoryName == currentItem.categoryName }
                if(products.last() == currentItem) return

                var itemAfter = products[products.indexOf(currentItem) +1]
                Collections.swap(preview,preview.indexOf(itemAfter),preview.indexOf(currentItem))
            }
        }
        notifyDataSetChanged()
    }


    class MenuPreviewViewHolder(private val binding:ItemMenuPreviewBinding):RecyclerView.ViewHolder(binding.root){

        fun bind(itemPreview: ItemPreview){
            binding.imageTitleDescription.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE) View.VISIBLE else View.GONE
            binding.titleDescription.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE) View.VISIBLE else View.GONE
            binding.titlePrice.root.visibility = if(itemPreview.itemStyle == ItemStyle.MENU_TITLE_PRICE) View.VISIBLE else View.GONE
            binding.categoryTitle.root.visibility = if (itemPreview.itemStyle == ItemStyle.MENU_CATEGORY_HEADER) View.VISIBLE else View.GONE

            when (itemPreview.itemStyle) {
                ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE -> {
                    binding.imageTitleDescription.title.text = itemPreview.name
                    binding.imageTitleDescription.body.text = itemPreview.description
                    binding.imageTitleDescription.price.text = itemPreview.price
                    binding.imageTitleDescription.image.setImageURI(itemPreview.image)
                }
                ItemStyle.MENU_TITLE_DESCRIPTION_PRICE -> {
                    binding.titleDescription.title.text = itemPreview.name
                    binding.titleDescription.body.text = itemPreview.description
                    binding.titleDescription.price.text = itemPreview.price
                }
                ItemStyle.MENU_TITLE_PRICE -> {
                    binding.titlePrice.title.text = itemPreview.name
                    binding.titlePrice.price.text = itemPreview.price
                }
                else -> {
                    binding.categoryTitle.title.text = itemPreview.name
                }
            }
        }
    }
}