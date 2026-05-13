package com.example.hibeauty

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.FragmentDiscoverBinding

class DiscoverFragment :
    Fragment(R.layout.fragment_discover) {

    private var _binding: FragmentDiscoverBinding? = null

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
            FragmentDiscoverBinding.bind(view)

        setupButtons()
    }

    // BUTTONS

    private fun setupButtons() {

        // BACK

        binding.btnBackDiscover
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }

        // SEARCH

        binding.btnSearchDiscover
            .setOnClickListener {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        SearchFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }

        // JOIN CHALLENGE

        binding.btnJoinGlow
            .setOnClickListener {

                AlertDialog.Builder(requireContext())

                    .setTitle(
                        "✨ Únete al desafío"
                    )

                    .setMessage(
                        "Para completar con éxito tu desafío, ten a la mano tu kit Piel Radiante disponible en HiBeauty 💖"
                    )

                    .setPositiveButton(
                        "Ver productos"
                    ) { _, _ ->

                        parentFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.fragment_container,
                                CategoryProductsFragment(
                                    "Skincare"
                                )
                            )
                            .addToBackStack(null)
                            .commit()
                    }

                    .setNegativeButton(
                        "Después",
                        null
                    )

                    .show()
            }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}