package com.example.hibeauty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hibeauty.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    private lateinit var firestore: FirebaseFirestore

    // Registration flow variables

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentProfileBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        auth =
            FirebaseAuth.getInstance()

        firestore =
            FirebaseFirestore.getInstance()

        setupButtons()

        validateSession()
    }

    // VALIDATE SESSION

    private fun validateSession() {

        val currentUser =
            auth.currentUser

        if (currentUser == null) {

            binding.loginContainer.visibility =
                View.VISIBLE

            binding.registerContainer.visibility =
                View.GONE

            binding.userPanel.visibility =
                View.GONE

        } else {

            binding.loginContainer.visibility =
                View.GONE

            binding.registerContainer.visibility =
                View.GONE

            binding.userPanel.visibility =
                View.VISIBLE

            loadUserData()
        }
    }

    // LOAD USER DATA

    private fun loadUserData() {

        val currentUser =
            auth.currentUser ?: return

        val uid =
            currentUser.uid

        firestore.collection("users")
            .document(uid)
            .get()

            .addOnSuccessListener { document ->

                val name =
                    document.getString("name")
                        ?: "Beauty Lover ✨"

                val email =
                    currentUser.email
                        ?: "correo@hibeauty.com"

                val role =
                    document.getString("role")
                        ?: "user"

                // DEBUG ROLE

                Toast.makeText(
                    requireContext(),
                    "ROL: $role",
                    Toast.LENGTH_SHORT
                ).show()

                // ADMIN

                // ADMIN

                if (
                    role.lowercase() == "admin" ||
                    role.lowercase() == "administrador"
                ) {

                    (requireActivity() as? MainActivity)?.showAdminNavigation()
                    return@addOnSuccessListener
                }

                // DELIVERY

                if (
                    role.lowercase() == "delivery" ||
                    role.lowercase() == "repartidor"
                ) {

                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragment_container,
                            DeliveryDashboardFragment()
                        )
                        .commit()

                    return@addOnSuccessListener
                }

                // NORMAL USER

                binding.profileName.text =
                    name

                binding.profileEmail.text =
                    email

                binding.profileOrdersCount.text =
                    "12"

                binding.profilePoints.text =
                    "450"

                binding.profileDiscounts.text =
                    "5"
            }

            .addOnFailureListener { e ->

                Toast.makeText(
                    requireContext(),
                    "Error perfil: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // BUTTONS

    private fun setupButtons() {

        // SHOW LOGIN

        binding.btnShowLogin
            .setOnClickListener {

                binding.loginContainer.visibility =
                    View.VISIBLE

                binding.registerContainer.visibility =
                    View.GONE
            }

        // SHOW REGISTER

        binding.btnShowRegister
            .setOnClickListener {

                binding.loginContainer.visibility =
                    View.GONE

                binding.registerContainer.visibility =
                    View.VISIBLE
            }

        // ROLE SELECTOR
        binding.registerRoleGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioRoleDelivery) {
                binding.deliveryFieldsContainer.visibility = View.VISIBLE
            } else {
                binding.deliveryFieldsContainer.visibility = View.GONE
            }
        }

        // LOGIN

        binding.btnLogin
            .setOnClickListener {

                val email =
                    binding.loginEmail.text
                        .toString()
                        .trim()

                val password =
                    binding.loginPassword.text
                        .toString()
                        .trim()

                if (
                    email.isEmpty() ||
                    password.isEmpty()
                ) {

                    Toast.makeText(
                        requireContext(),
                        "Completa todos los campos 💕",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                auth.signInWithEmailAndPassword(
                    email,
                    password
                )

                    .addOnSuccessListener {

                        Toast.makeText(
                            requireContext(),
                            "Bienvenida a HiBeauty ✨",
                            Toast.LENGTH_SHORT
                        ).show()

                        validateSession()
                    }

                    .addOnFailureListener { e ->

                        Toast.makeText(
                            requireContext(),
                            "Error login: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

        // REGISTER

        binding.btnRegister
            .setOnClickListener {

                val name =
                    binding.registerName.text
                        .toString()
                        .trim()

                val email =
                    binding.registerEmail.text
                        .toString()
                        .trim()

                val phone =
                    binding.registerPhone.text
                        .toString()
                        .trim()

                val identification =
                    binding.registerIdentification.text
                        .toString()
                        .trim()

                val password =
                    binding.registerPassword.text
                        .toString()
                        .trim()

                val isDelivery = binding.registerRoleGroup.checkedRadioButtonId == R.id.radioRoleDelivery
                val vehicle = binding.registerVehicle.text.toString().trim()
                val plate = binding.registerPlate.text.toString().trim()

                if (
                    name.isEmpty() ||
                    email.isEmpty() ||
                    password.isEmpty() ||
                    phone.isEmpty() ||
                    identification.isEmpty()
                ) {

                    Toast.makeText(
                        requireContext(),
                        "Completa todos los campos 💕",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                if (isDelivery && (vehicle.isEmpty() || plate.isEmpty())) {
                    Toast.makeText(
                        requireContext(),
                        "Repartidor: Debes ingresar el vehículo y la placa 🏍️",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (password.length < 6) {

                    Toast.makeText(
                        requireContext(),
                        "La contraseña debe tener mínimo 6 caracteres",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                binding.btnRegister.text = "Registrando..."
                binding.btnRegister.isEnabled = false

                val selectedRole = when {
                    isDelivery -> "delivery"
                    email.contains("admin", ignoreCase = true) -> "admin"
                    else -> "user"
                }

                val userMap = hashMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "phone" to phone,
                    "identification" to identification,
                    "role" to selectedRole,
                    "address" to "Calle 85 # 11-53, Bogotá", // Premium default delivery address
                    "createdAt" to System.currentTimeMillis()
                )

                if (isDelivery) {
                    userMap["vehicle"] = vehicle
                    userMap["plate"] = plate
                    userMap["completedDeliveries"] = 0L
                    userMap["earnings"] = 0L
                }

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: ""
                        firestore.collection("users")
                            .document(uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Cuenta creada exitosamente ✨",
                                    Toast.LENGTH_LONG
                                ).show()
                                binding.btnRegister.text = "Crear cuenta"
                                binding.btnRegister.isEnabled = true
                                validateSession()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Error Firestore: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                binding.btnRegister.text = "Crear cuenta"
                                binding.btnRegister.isEnabled = true
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Error Registro: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.btnRegister.text = "Crear cuenta"
                        binding.btnRegister.isEnabled = true
                    }
                // Register flow end
            }

        // BACK

        binding.btnBackProfile
            .setOnClickListener {

                requireActivity()
                    .onBackPressedDispatcher
                    .onBackPressed()
            }

        // ORDERS

        binding.btnOrders
            .setOnClickListener {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragment_container,
                        UserOrdersFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }

        // FAVORITES

        binding.btnFavorites
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Tus favoritos 💖",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // ADDRESSES

        binding.btnAddresses
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Gestionar direcciones 📍",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // POINTS

        binding.btnPoints
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Beauty Points ⭐",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // NOTIFICATIONS

        binding.btnNotifications
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Configurando notificaciones 🔔",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // SETTINGS

        binding.btnSettings
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Configuración ⚙",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // SUPPORT

        binding.btnSupport
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Centro de ayuda 💕",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // COUPONS

        binding.btnCoupons
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "Tus cupones 🎁",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // LOGOUT

        binding.btnLogoutUser
            .setOnClickListener {

                auth.signOut()

                Toast.makeText(
                    requireContext(),
                    "Sesión cerrada 💕",
                    Toast.LENGTH_SHORT
                ).show()

                validateSession()
            }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}