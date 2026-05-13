package com.example.hibeauty

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hibeauty.databinding.ItemAdminProductBinding

class AdminProductAdapter : RecyclerView.Adapter<AdminProductAdapter.AdminProductViewHolder>() {

    private val products = mutableListOf<Product>()

    fun submitList(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminProductViewHolder {
        val binding = ItemAdminProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdminProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    class AdminProductViewHolder(
        private val binding: ItemAdminProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.adminProductName.text = product.name
            binding.adminProductCategory.text = product.category
            binding.adminProductStatus.text = if (product.isActive) "Activo" else "Pausado"
            binding.adminProductStock.text = product.presentations.entries.joinToString("\n") { entry ->
                "${entry.key}: ${entry.value.stock} uds - $${entry.value.price}"
            }.ifBlank {
                "Sin presentaciones"
            }

            Glide.with(binding.adminProductImage.context)
                .load(product.imageUrl)
                .centerCrop()
                .into(binding.adminProductImage)
        }
    }
}
