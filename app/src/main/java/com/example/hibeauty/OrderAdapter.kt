package com.example.hibeauty

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.hibeauty.databinding.ItemOrderBinding

class OrderAdapter(
    private val showStatusAction: Boolean,
    private val onStatusAction: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val orders = mutableListOf<Order>()

    fun submitList(newOrders: List<Order>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding, showStatusAction, onStatusAction)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    class OrderViewHolder(
        private val binding: ItemOrderBinding,
        private val showStatusAction: Boolean,
        private val onStatusAction: (Order) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            binding.orderTitle.text = "Pedido #${order.id.takeLast(6).uppercase()}"
            binding.orderStatus.text = order.status.ifBlank { "Pendiente" }
            binding.orderItems.text = order.items.joinToString(separator = "\n") { item ->
                "${item.quantity} x ${item.name} (${item.presentation})"
            }.ifBlank {
                "Sin productos registrados"
            }
            binding.orderTotal.text = "Total: ${order.total.toCOP()}"
            binding.orderProgress.text = progressText(order.status)
            binding.orderTimeline.text = timelineText(order.status)

            if (order.riderName.isNotEmpty()) {
                binding.riderInfoContainer.isVisible = true
                binding.orderRiderText.text = "Repartidor: ${order.riderName} (${order.riderPhone})"
            } else {
                binding.riderInfoContainer.isVisible = false
            }

            val nextStatus = nextStatus(order.status)
            binding.btnNextOrderStatus.isVisible = showStatusAction && nextStatus != null
            binding.btnNextOrderStatus.text = nextStatus?.let { "Marcar como $it" } ?: ""
            binding.btnNextOrderStatus.setOnClickListener {
                onStatusAction(order)
            }
        }

        private fun nextStatus(status: String): String? {
            return when (status) {
                "Pendiente" -> "Preparando"
                "Preparando" -> "En camino"
                "En camino" -> "Entregado"
                else -> null
            }
        }

        private fun progressText(status: String): String {
            return when (status) {
                "Pendiente" -> "1 de 4 - Pedido recibido"
                "Preparando" -> "2 de 4 - Preparando tu compra"
                "En camino" -> "3 de 4 - Va en camino"
                "Entregado" -> "4 de 4 - Pedido entregado"
                "Cancelado" -> "Pedido cancelado"
                else -> "Estado pendiente de actualizar"
            }
        }

        private fun timelineText(status: String): String {
            return when (status) {
                "Pendiente" -> "● Pedido  ○ Preparando  ○ En camino  ○ Entregado"
                "Preparando" -> "● Pedido  ● Preparando  ○ En camino  ○ Entregado"
                "En camino" -> "● Pedido  ● Preparando  ● En camino  ○ Entregado"
                "Entregado" -> "● Pedido  ● Preparando  ● En camino  ● Entregado"
                else -> "● Pedido  ○ Preparando  ○ En camino  ○ Entregado"
            }
        }
    }
}
