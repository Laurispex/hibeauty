package com.example.hibeauty

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hibeauty.databinding.ItemCartProductBinding

class CartAdapter(
    private val onRemoveClick: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val items = mutableListOf<CartItem>()

    fun submitList(newItems: List<CartItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CartViewHolder(
        private val binding: ItemCartProductBinding,
        private val onRemoveClick: (CartItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.cartProductName.text = item.name
            binding.cartProductPresentation.text = "Presentación: ${item.presentation}"
            binding.cartProductQuantity.text = "Cantidad: ${item.quantity}"
            binding.cartProductPrice.text = (item.price * item.quantity).toCOP()

            Glide.with(binding.cartProductImage.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(binding.cartProductImage)

            binding.btnRemoveCartItem.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }
}
