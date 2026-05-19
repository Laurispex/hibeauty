package com.example.hibeauty

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hibeauty.databinding.ItemAdminProductBinding
import com.google.firebase.firestore.FirebaseFirestore

class AdminProductAdapter :
    RecyclerView.Adapter<AdminProductAdapter.AdminProductViewHolder>() {

    private val products =
        mutableListOf<Product>()

    private val db =
        FirebaseFirestore.getInstance()

    fun submitList(
        newProducts: List<Product>
    ) {

        products.clear()

        products.addAll(newProducts)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AdminProductViewHolder {

        val binding =
            ItemAdminProductBinding.inflate(

                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return AdminProductViewHolder(
            binding
        )
    }

    override fun onBindViewHolder(
        holder: AdminProductViewHolder,
        position: Int
    ) {

        holder.bind(
            products[position]
        )
    }

    override fun getItemCount(): Int =
        products.size

    inner class AdminProductViewHolder(
        private val binding: ItemAdminProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {

            binding.adminProductName.text =
                product.name

            binding.adminProductCategory.text =
                product.category

            binding.adminProductStatus.text =
                if (product.isActive)
                    "Activo"
                else
                    "Pausado"

            binding.adminProductStock.text =
                product.presentations.entries.joinToString(
                    "\n"
                ) { entry ->

                    "${entry.key}: ${entry.value.stock} uds - ${entry.value.price.toCOP()}"
                }.ifBlank {

                    "Sin presentaciones"
                }

            Glide.with(
                binding.adminProductImage.context
            )
                .load(product.imageUrl)
                .centerCrop()
                .into(binding.adminProductImage)

            // DELETE BUTTON

            binding.btnDeleteProduct.setOnClickListener {

                AlertDialog.Builder(
                    binding.root.context
                )
                    .setTitle(
                        "Eliminar producto"
                    )

                    .setMessage(
                        "¿Deseas eliminar ${product.name}?"
                    )

                    .setPositiveButton(
                        "Eliminar"
                    ) { _, _ ->

                        deleteProduct(product)
                    }

                    .setNegativeButton(
                        "Cancelar",
                        null
                    )

                    .show()
            }

            // EDIT BUTTON

            binding.btnEditProduct.setOnClickListener {

                Toast.makeText(
                    binding.root.context,
                    "Próximamente podrás editar este producto ✨",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        private fun deleteProduct(
            product: Product
        ) {

            db.collection("products")
                .document(product.id)
                .delete()

                .addOnSuccessListener {

                    Toast.makeText(
                        binding.root.context,
                        "Producto eliminado",
                        Toast.LENGTH_LONG
                    ).show()
                }

                .addOnFailureListener {

                    Toast.makeText(
                        binding.root.context,
                        "No se pudo eliminar",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}