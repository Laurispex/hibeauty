package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.FragmentSkinCareGuideBinding

class SkinCareGuideFragment :
    Fragment(R.layout.fragment_skin_care_guide) {

    private var _binding: FragmentSkinCareGuideBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        _binding =
            FragmentSkinCareGuideBinding.bind(view)

        binding.btnBackGuide.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
