package com.example.hibeauty.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hibeauty.data.model.User
import com.example.hibeauty.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object NotLoggedIn : ProfileUiState()
    data class LoggedIn(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class AuthAction {
    object Idle : AuthAction()
    object Loading : AuthAction()
    data class Success(val role: String) : AuthAction()
    data class Failure(val message: String) : AuthAction()
}

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState

    private val _authAction = MutableStateFlow<AuthAction>(AuthAction.Idle)
    val authAction: StateFlow<AuthAction> = _authAction

    fun checkSession() {
        val uid = userRepo.currentFirebaseUser?.uid
        if (uid == null) { _profileState.value = ProfileUiState.NotLoggedIn; return }
        loadProfile(uid)
    }

    private fun loadProfile(uid: String) {
        viewModelScope.launch {
            _profileState.value = ProfileUiState.Loading
            userRepo.getUserProfile(uid).fold(
                onSuccess = { _profileState.value = ProfileUiState.LoggedIn(it) },
                onFailure = { _profileState.value = ProfileUiState.Error("No se pudo cargar el perfil") }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authAction.value = AuthAction.Loading
            userRepo.login(email, password).fold(
                onSuccess = { uid ->
                    userRepo.getUserProfile(uid).fold(
                        onSuccess = { user ->
                            _profileState.value = ProfileUiState.LoggedIn(user)
                            _authAction.value = AuthAction.Success(user.role)
                        },
                        onFailure = { _authAction.value = AuthAction.Failure("Perfil no encontrado") }
                    )
                },
                onFailure = { _authAction.value = AuthAction.Failure("Correo o contraseña incorrectos") }
            )
        }
    }

    fun register(
        name: String, email: String, password: String,
        phone: String, identification: String,
        role: String, vehicle: String, plate: String
    ) {
        viewModelScope.launch {
            _authAction.value = AuthAction.Loading
            userRepo.register(email, password).fold(
                onSuccess = { uid ->
                    val profileData = mapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "identification" to identification,
                        "role" to role,
                        "address" to "",
                        "vehicle" to vehicle,
                        "plate" to plate,
                        "ordersCount" to 0L,
                        "points" to 0L,
                        "discounts" to 0L,
                        "completedDeliveries" to 0L,
                        "earnings" to 0L,
                        "createdAt" to System.currentTimeMillis()
                    )
                    userRepo.createUserProfile(uid, profileData).fold(
                        onSuccess = { _authAction.value = AuthAction.Success(role) },
                        onFailure = { _authAction.value = AuthAction.Failure("Cuenta creada pero perfil falló") }
                    )
                },
                onFailure = { _authAction.value = AuthAction.Failure(it.message ?: "Error al registrar") }
            )
        }
    }

    fun logout() {
        userRepo.logout()
        _profileState.value = ProfileUiState.NotLoggedIn
        _authAction.value = AuthAction.Idle
    }

    fun resetAuthAction() { _authAction.value = AuthAction.Idle }

    /** Convenience for one-off reads in the Fragment (e.g. toast content). */
    val currentPoints: Long
        get() = (_profileState.value as? ProfileUiState.LoggedIn)?.user?.points ?: 0L
}
