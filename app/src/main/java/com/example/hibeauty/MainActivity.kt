package com.example.hibeauty

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isAdminMode = false

    private var cartListener: ListenerRegistration? = null

    // PRODUCT DETAIL

    fun openProductDetail(product: Product) {

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                ProductDetailFragment(product)
            )
            .addToBackStack(null)
            .commit()
    }

    // ROUTINE

    fun openRoutine() {

        binding.bottomNavigation.selectedItemId =
            R.id.nav_routine

        showFragment(RoutineFragment())
    }

    // PROFILE

    fun openProfile() {

        binding.bottomNavigation.selectedItemId =
            R.id.nav_profile

        showFragment(ProfileFragment())
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // 🔥 FORZAR LOGOUT TEMPORAL
        // SOLO PARA PRUEBAS DE BLOQUEO

        FirebaseAuth.getInstance()
            .signOut()

        binding =
            ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.main
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                0,
                systemBars.right,
                0
            )

            insets
        }

        showUserNavigation()

        if (savedInstanceState == null) {

            binding.bottomNavigation.selectedItemId =
                R.id.nav_home

            showFragment(HomeFragment())
        }
    }

    // USER NAVIGATION

    fun showUserNavigation() {

        isAdminMode = false

        observeCartBadge()

        binding.bottomNavigation.menu.clear()

        binding.bottomNavigation.inflateMenu(
            R.menu.bottom_nav_menu
        )

        binding.bottomNavigation
            .setOnItemSelectedListener { item ->

                when (item.itemId) {

                    R.id.nav_home -> {

                        showFragment(HomeFragment())
                        true
                    }

                    R.id.nav_discover -> {

                        showFragment(DiscoverFragment())
                        true
                    }

                    R.id.nav_cart -> {

                        showFragment(CartFragment())
                        true
                    }

                    R.id.nav_routine -> {

                        showFragment(RoutineFragment())
                        true
                    }

                    R.id.nav_profile -> {

                        showFragment(ProfileFragment())
                        true
                    }

                    else -> false
                }
            }
    }

    // ADMIN NAVIGATION

    fun showAdminNavigation() {

        isAdminMode = true

        binding.bottomNavigation.menu.clear()

        binding.bottomNavigation.inflateMenu(
            R.menu.admin_bottom_nav_menu
        )

        binding.bottomNavigation
            .setOnItemSelectedListener { item ->

                when (item.itemId) {

                    R.id.nav_admin_dashboard -> {

                        showFragment(
                            AdminDashboardFragment()
                        )

                        true
                    }

                    R.id.nav_admin_publish -> {

                        showFragment(
                            AdminPublishFragment()
                        )

                        true
                    }

                    R.id.nav_admin_inventory -> {

                        showFragment(
                            AdminInventoryFragment()
                        )

                        true
                    }

                    R.id.nav_admin_orders -> {

                        showFragment(
                            AdminOrdersFragment()
                        )

                        true
                    }

                    R.id.nav_profile -> {

                        showFragment(ProfileFragment())
                        true
                    }

                    else -> false
                }
            }

        binding.bottomNavigation.selectedItemId =
            R.id.nav_admin_publish

        showFragment(AdminPublishFragment())
    }

    // CART BADGE

    private fun observeCartBadge() {

        val currentUser =
            FirebaseAuth.getInstance()
                .currentUser
                ?: return

        cartListener?.remove()

        cartListener =
            FirebaseFirestore.getInstance()
                .collection("carts")
                .document(currentUser.uid)
                .collection("items")

                .addSnapshotListener { value, _ ->

                    val count =
                        value?.documents?.size ?: 0

                    val badge =
                        binding.bottomNavigation
                            .getOrCreateBadge(
                                R.id.nav_cart
                            )

                    if (count <= 0) {

                        badge.isVisible = false

                    } else {

                        badge.isVisible = true

                        badge.number = count

                        badge.backgroundColor =
                            getColor(
                                R.color.hib_hot_pink
                            )

                        badge.badgeTextColor =
                            getColor(
                                android.R.color.white
                            )
                    }
                }
    }

    // SHOW FRAGMENT

    private fun showFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment
            )
            .commit()
    }

    override fun onDestroy() {

        super.onDestroy()

        cartListener?.remove()
    }
}