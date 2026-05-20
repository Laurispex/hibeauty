package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hibeauty.databinding.ItemRoutineStepBinding

class RoutineAdapter(

    private val onStepClicked:
        (RoutineStep) -> Unit

) : RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder>() {

    private val steps =
        mutableListOf<RoutineStep>()

    fun submitList(
        newSteps: List<RoutineStep>
    ) {

        steps.clear()

        steps.addAll(newSteps)

        notifyDataSetChanged()
    }

    inner class RoutineViewHolder(
        private val binding:
        ItemRoutineStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(step: RoutineStep) {

            // TITLE

            binding.stepTitle.text =
                step.title

            // DURATION

            binding.stepDuration.text =
                step.duration

            // STATUS

            binding.stepStatus.text =

                if (step.isCompleted)
                    "✅"
                else
                    "⏳"

            // STEP NUMBER

            binding.stepIcon.text =
                (adapterPosition + 1)
                    .toString()

            // COMPLETED STATE

            if (step.isCompleted) {

                binding.stepTitle.paintFlags =
                    Paint.STRIKE_THRU_TEXT_FLAG

                binding.root.alpha = 0.55f

                binding.stepStatus.scaleX = 1.15f
                binding.stepStatus.scaleY = 1.15f

            } else {

                binding.stepTitle.paintFlags = 0

                binding.root.alpha = 1f

                binding.stepStatus.scaleX = 1f
                binding.stepStatus.scaleY = 1f
            }

            // CLICK

            binding.root.setOnClickListener {

                binding.root.animate()
                    .scaleX(0.97f)
                    .scaleY(0.97f)
                    .setDuration(90)
                    .withEndAction {

                        binding.root.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .duration = 90
                    }

                onStepClicked(step)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RoutineViewHolder {

        val binding =
            ItemRoutineStepBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RoutineViewHolder,
        position: Int
    ) {

        holder.bind(steps[position])
    }

    override fun getItemCount(): Int =
        steps.size
}
