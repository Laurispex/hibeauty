package com.example.hibeauty

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.FragmentAuthRequiredBinding

class AuthRequiredFragment :
    Fragment(R.layout.fragment_auth_required) {

    private var _binding:
            FragmentAuthRequiredBinding? = null

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
            FragmentAuthRequiredBinding
                .bind(view)

        binding.btnOpenLogin
            .setOnClickListener {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        ProfileFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}