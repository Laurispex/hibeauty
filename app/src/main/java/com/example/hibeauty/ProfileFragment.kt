package com.example.hibeauty

import com.example.hibeauty.data.model.Product
import com.example.hibeauty.data.model.Order
import com.example.hibeauty.data.model.CartItem
import com.example.hibeauty.data.model.RoutineStep
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.hibeauty.data.model.User
import com.example.hibeauty.databinding.FragmentProfileBinding
import com.example.hibeauty.ui.profile.AuthAction
import com.example.hibeauty.ui.profile.ProfileUiState
import com.example.hibeauty.ui.profile.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    // ─── LIFECYCLE ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
        viewModel.checkSession()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── OBSERVERS ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileState.collect { state ->
                    when (state) {
                        is ProfileUiState.Loading    -> showLoadingState()
                        is ProfileUiState.NotLoggedIn -> showLoginForm()
                        is ProfileUiState.LoggedIn   -> showUserPanel(state.user)
                        is ProfileUiState.Error      -> toast("Error: ${state.message}")
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authAction.collect { action ->
                    when (action) {
                        is AuthAction.Idle    -> Unit
                        is AuthAction.Loading -> setAuthButtonsEnabled(false)
                        is AuthAction.Success -> {
                            setAuthButtonsEnabled(true)
                            viewModel.resetAuthAction()
                            handleRoleNavigation(action.role)
                        }
                        is AuthAction.Failure -> {
                            setAuthButtonsEnabled(true)
                            toast(action.message)
                            viewModel.resetAuthAction()
                        }
                    }
                }
            }
        }
    }

    // ─── ROLE NAVIGATION ───────────────────────────────────────────────────────

    private fun handleRoleNavigation(role: String) {
        when (role.lowercase()) {
            "admin", "administrador", "tienda" -> {
                (requireActivity() as? MainActivity)?.showAdminNavigation()
            }
            "delivery", "repartidor" -> {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, DeliveryDashboardFragment())
                    .commit()
            }
            else -> {
                // Already handled by profileState.LoggedIn
                toast("Bienvenida a HiBeauty 💖")
            }
        }
    }

    // ─── UI STATES ─────────────────────────────────────────────────────────────

    private fun showLoadingState() {
        binding.loginContainer.visibility = View.GONE
        binding.registerContainer.visibility = View.GONE
        binding.userPanel.visibility = View.GONE
        binding.profileName.text = "Cargando..."
        binding.profileEmail.text = ""
        binding.profileOrdersCount.text = "-"
        binding.profilePoints.text = "-"
        binding.profileDiscounts.text = "-"
    }

    private fun showLoginForm() {
        binding.loginContainer.visibility = View.VISIBLE
        binding.registerContainer.visibility = View.GONE
        binding.userPanel.visibility = View.GONE
    }

    private fun showUserPanel(user: User) {
        binding.loginContainer.visibility = View.GONE
        binding.registerContainer.visibility = View.GONE
        binding.userPanel.visibility = View.VISIBLE

        binding.profileName.text = user.name.ifBlank { "Beauty Lover" }
        binding.profileEmail.text = user.email
        binding.profileOrdersCount.text = user.ordersCount.toString()
        binding.profilePoints.text = user.points.toString()
        binding.profileDiscounts.text = user.discounts.toString()
    }

    // ─── CLICK LISTENERS ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        // Form toggles
        binding.btnShowLogin.setOnClickListener {
            binding.loginContainer.visibility = View.VISIBLE
            binding.registerContainer.visibility = View.GONE
        }
        binding.btnShowRegister.setOnClickListener {
            binding.loginContainer.visibility = View.GONE
            binding.registerContainer.visibility = View.VISIBLE
        }

        // Delivery fields visibility
        binding.registerRoleGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.deliveryFieldsContainer.visibility =
                if (checkedId == R.id.radioRoleDelivery) View.VISIBLE else View.GONE
        }

        // Login
        binding.btnLogin.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                toast("Completa todos los campos"); return@setOnClickListener
            }
            viewModel.login(email, password)
        }

        // Register
        binding.btnRegister.setOnClickListener {
            val name = binding.registerName.text.toString().trim()
            val email = binding.registerEmail.text.toString().trim()
            val phone = binding.registerPhone.text.toString().trim()
            val identification = binding.registerIdentification.text.toString().trim()
            val password = binding.registerPassword.text.toString().trim()
            val isDelivery = binding.registerRoleGroup.checkedRadioButtonId == R.id.radioRoleDelivery
            val vehicle = binding.registerVehicle.text.toString().trim()
            val plate = binding.registerPlate.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || identification.isEmpty()) {
                toast("Completa todos los campos"); return@setOnClickListener
            }
            if (isDelivery && (vehicle.isEmpty() || plate.isEmpty())) {
                toast("Repartidor: ingresa el vehículo y la placa"); return@setOnClickListener
            }
            if (password.length < 6) {
                toast("La contraseña debe tener mínimo 6 caracteres"); return@setOnClickListener
            }

            val role = when {
                isDelivery -> "delivery"
                email.contains("admin", true) || email.contains("tienda", true) -> "tienda"
                else -> "user"
            }

            viewModel.register(name, email, password, phone, identification, role, vehicle, plate)
        }

        // Navigation
        binding.btnBackProfile.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnOrders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserOrdersFragment())
                .addToBackStack(null).commit()
        }

        // Feature stubs (toast placeholders for future features)
        binding.btnFavorites.setOnClickListener { toast("Próximamente: Tus favoritos ❤️") }
        binding.btnAddresses.setOnClickListener { toast("Próximamente: Gestionar direcciones 📍") }
        binding.btnPoints.setOnClickListener { toast("Beauty Points ⭐: ${viewModel.currentPoints} pts") }
        binding.btnNotifications.setOnClickListener { toast("Próximamente: Notificaciones 🔔") }
        binding.btnSettings.setOnClickListener { toast("Próximamente: Configuración ⚙️") }
        binding.btnSupport.setOnClickListener { toast("Próximamente: Centro de ayuda") }
        binding.btnCoupons.setOnClickListener { toast("Próximamente: Tus cupones 🎁") }

        // Logout
        binding.btnLogoutUser.setOnClickListener {
            viewModel.logout()
            toast("Sesión cerrada")
        }
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private fun setAuthButtonsEnabled(enabled: Boolean) {
        binding.btnLogin.isEnabled = enabled
        binding.btnRegister.isEnabled = enabled
        binding.btnRegister.text = if (enabled) "Crear cuenta" else "Registrando..."
        binding.btnLogin.text = if (enabled) "Iniciar sesión" else "Entrando..."
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
