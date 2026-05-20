package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminPromosFragment : Fragment(R.layout.fragment_admin_promos) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        // BACK

        view.findViewById<View>(
            R.id.btnBackPromos
        ).setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()

        activity
            ?.findViewById<BottomNavigationView>(
                R.id.bottom_navigation
            )
            ?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()

        activity
            ?.findViewById<BottomNavigationView>(
                R.id.bottom_navigation
            )
            ?.visibility = View.VISIBLE
    }
}
