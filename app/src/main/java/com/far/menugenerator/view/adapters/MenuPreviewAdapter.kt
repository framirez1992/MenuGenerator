package com.far.menugenerator.view.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.far.menugenerator.R
import com.far.menugenerator.common.utils.StringUtils
import com.far.menugenerator.databinding.ItemMenuPreviewBinding
import com.far.menugenerator.model.ItemPreviewPosition
import com.far.menugenerator.model.ItemStyle
import com.far.menugenerator.model.database.room.model.MenuItemsTemp
import com.far.menugenerator.view.common.BaseActivity
import java.util.*


class MenuPreviewAdapter(private val activity:BaseActivity,
                         private val itemPreviewList:List<MenuItemsTemp>,
                         private val onPositionChanged:(List<ItemPreviewPosition>)->Unit,
                         private val onclick:(MenuItemsTemp)->Unit): RecyclerView.Adapter<MenuPreviewAdapter.MenuPreviewViewHolder>() {

    private var preview:MutableList<MenuItemsTemp> = mutableListOf()
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
        holder.binding.btnUp.setOnClickListener{
            moveUp(preview[position])
        }
        holder.binding.btnDown.setOnClickListener{
            moveDown(preview[position])
        }
        holder.itemView.setOnClickListener {
            if(preview[position].type == ItemStyle.MENU_CATEGORY_HEADER.name) return@setOnClickListener
            onclick(preview[position])
        }


    }

    private fun organizeArray(){
        preview.clear()
        val headers = itemPreviewList.filter { it.type == ItemStyle.MENU_CATEGORY_HEADER.name }.sortedBy { it.position }
        headers.forEach{ category->
            var items = itemPreviewList.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.categoryId ==  category.id}.sortedBy { it.position }
            if(items.isNotEmpty()){//show only relevant data
                preview.add(category)
                preview.addAll(items)
            }

        }
    }


    private fun moveUp(currentItem: MenuItemsTemp){
        when(currentItem.type){
            ItemStyle.MENU_CATEGORY_HEADER.name->{
                var categories = preview.filter { it.type == ItemStyle.MENU_CATEGORY_HEADER.name }
                if(categories.first() == currentItem) return

                var itemBefore = categories[categories.indexOf(currentItem) -1]
                Collections.swap(preview,preview.indexOf(itemBefore),preview.indexOf(currentItem))



                categories.forEach{ category->
                    var items = preview.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.categoryId == category.id }
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
                var products = preview.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.categoryId == currentItem.categoryId }
                if(products.first() == currentItem) return

                var itemBefore = products[products.indexOf(currentItem) -1]
                Collections.swap(preview,preview.indexOf(itemBefore),preview.indexOf(currentItem))
            }

        }
        notifyDataSetChanged()
        updatePositions()
    }


    private fun moveDown(currentItem: MenuItemsTemp){
        when(currentItem.type){
            ItemStyle.MENU_CATEGORY_HEADER.name->{
                var categories = preview.filter { it.type == ItemStyle.MENU_CATEGORY_HEADER.name }
                if(categories.last() == currentItem) return

                var itemAfter = categories[categories.indexOf(currentItem) +1]
                Collections.swap(preview,preview.indexOf(itemAfter),preview.indexOf(currentItem))

                categories.forEach{ category->
                    var items = preview.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.categoryId == category.id }
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
                var products = preview.filter { it.type != ItemStyle.MENU_CATEGORY_HEADER.name && it.categoryId == currentItem.categoryId }
                if(products.last() == currentItem) return

                var itemAfter = products[products.indexOf(currentItem) +1]
                Collections.swap(preview,preview.indexOf(itemAfter),preview.indexOf(currentItem))
            }
        }
        notifyDataSetChanged()
        updatePositions()
    }


    private fun updatePositions(){
        //Update PreviewPositions value
        preview.forEach {
            it.position = preview.indexOf(it)
        }
        //Actualiza la propiedad Position de las categorias y productos a como se ordeno en el preview
        onPositionChanged(preview.map { ItemPreviewPosition(id = it.id, position = it.position) })
    }

    class MenuPreviewViewHolder(val binding:ItemMenuPreviewBinding):RecyclerView.ViewHolder(binding.root){

        fun bind(activity: BaseActivity,itemPreview: MenuItemsTemp){
            binding.imageTitleDescription.root.visibility = if(itemPreview.type == ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE.name) View.VISIBLE else View.GONE
            binding.titleDescription.root.visibility = if(itemPreview.type == ItemStyle.MENU_TITLE_DESCRIPTION_PRICE.name) View.VISIBLE else View.GONE
            binding.titlePrice.root.visibility = if(itemPreview.type == ItemStyle.MENU_TITLE_PRICE.name) View.VISIBLE else View.GONE
            binding.categoryTitle.root.visibility = if (itemPreview.type == ItemStyle.MENU_CATEGORY_HEADER.name) View.VISIBLE else View.GONE
            binding.imgVisible.visibility = if(itemPreview.type == ItemStyle.MENU_CATEGORY_HEADER.name) View.GONE else View.VISIBLE

            binding.imgVisible.setImageResource(if(itemPreview.enabled) R.drawable.baseline_remove_red_eye_24 else R.drawable.baseline_visibility_off_24)
            val price = StringUtils.doubleToMoneyString(
                amount = itemPreview.price,
                country = "US",
                language = "en"
            )
            when (itemPreview.type) {
                ItemStyle.MENU_IMAGE_TITLE_DESCRIPTION_PRICE.name -> {
                    binding.imageTitleDescription.title.text = itemPreview.name
                    binding.imageTitleDescription.body.text = itemPreview.description
                    binding.imageTitleDescription.price.text = price
                    Glide.with(activity)
                        .load(itemPreview.imageUri)
                        .error(R.drawable.loading)
                        .encodeQuality(80)
                        .into(binding.imageTitleDescription.image)

                }
                ItemStyle.MENU_TITLE_DESCRIPTION_PRICE.name -> {
                    binding.titleDescription.title.text = itemPreview.name
                    binding.titleDescription.body.text = itemPreview.description
                    binding.titleDescription.price.text = price
                }
                ItemStyle.MENU_TITLE_PRICE.name -> {
                    binding.titlePrice.title.text = itemPreview.name
                    binding.titlePrice.price.text = price
                }
                else -> {
                    binding.categoryTitle.title.text = itemPreview.name
                }
            }




            // Get the layout parameters of the view.
            val layoutParams = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            if(itemPreview.type == ItemStyle.MENU_CATEGORY_HEADER.name)
                layoutParams.setMargins(0, 20, 0, 0)
            else
                layoutParams.setMargins(0, 0, 0, 0)

            binding.root.layoutParams = layoutParams
        }


    }
}