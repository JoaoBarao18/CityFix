package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pt.isec.amov.a2024136885.cityfix.data.repository.AuthRepository
import pt.isec.amov.a2024136885.cityfix.data.repository.UserRepository

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository, userRepository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}