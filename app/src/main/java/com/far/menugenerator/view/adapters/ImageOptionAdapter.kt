package com.far.menugenerator.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.far.menugenerator.databinding.RowImageOptionBinding

class ImageOptionAdapter(private val options:List<ImageOption>, private val onclick:(ImageOption)->Unit): RecyclerView.Adapter<ImageOptionViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageOptionViewHolder {
        val binding = RowImageOptionBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ImageOptionViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return options.size
    }

    override fun onBindViewHolder(holder: ImageOptionViewHolder, position: Int) {
        holder.bind(options[position])
        holder.itemView.setOnClickListener {
            onclick(options[position])
        }
    }

}
class ImageOptionViewHolder(val binding: RowImageOptionBinding):RecyclerView.ViewHolder(binding.root){
    fun bind(obj:ImageOption){
        binding.imgSearch.setImageResource(obj.icon)
        binding.tvSearch.setText(obj.string)
    }
}
data class ImageOption(@DrawableRes val icon:Int, @StringRes val string:Int)