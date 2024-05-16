package com.far.menugenerator.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.far.menugenerator.R
import com.far.menugenerator.databinding.RowCompanyBinding
import com.far.menugenerator.model.firebase.firestore.model.CompanyFirebase

class CompanyAdapter(private val companies:List<CompanyFirebase?>, val onclick: (CompanyFirebase)->Unit): RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val binding = RowCompanyBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return CompanyViewHolder(binding)
    }

    override fun getItemCount(): Int {
       return companies.size
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(companies[position]!!)
        holder.itemView.setOnClickListener{
            onclick(companies[position]!!)
        }
    }

    class CompanyViewHolder(private val binding:RowCompanyBinding):RecyclerView.ViewHolder(binding.root) {

        fun bind(company: CompanyFirebase){
            binding.tvCompanyName.text = company.businessName

            if(company.logoUrl != null)
            Glide.with(binding.root.context)
                .load(company.logoUrl)
                //.error(R.drawable.baseline_broken_image_24)
                .into(binding.imgLogo)
            else
                binding.imgLogo.setImageResource(R.drawable.baseline_image_24)
        }
    }
}