package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.a2024136885.cityfix.data.model.User
import pt.isec.amov.a2024136885.cityfix.data.repository.AuthRepository
import pt.isec.amov.a2024136885.cityfix.data.repository.UserRepository
import pt.isec.amov.a2024136885.cityfix.util.Validators

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pendingMfaUid: String? = null,
    val currentUser: User? = null,
    val isLoginSuccessful: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun initiateLogin(email: String, pass: String) {
        val emailErr = Validators.validateEmail(email)
        if (emailErr != null) {
            _uiState.update { it.copy(errorMessage = emailErr) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.initiateLogin(email, pass)
            result.onSuccess { uid ->
                _uiState.update { it.copy(isLoading = false, pendingMfaUid = uid) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Erro no login.") }
            }
        }
    }

    fun submitMfaCode(uid: String, code: String) {
        if (code.trim().length != 6) {
            _uiState.update { it.copy(errorMessage = "O código deve conter 6 dígitos.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = authRepository.verifyMfaCode(uid, code)
            result.onSuccess { utilizador ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoginSuccessful = true,
                        pendingMfaUid = null,
                        currentUser = utilizador
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun registerMunicipe(
        nome: String,
        email: String,
        pass: String,
        nif: String,
        telefone: String,
        lat: Double,
        lng: Double,
        impactos: Boolean
    ) {
        val nomeErr = Validators.validateName(nome)
        val emailErr = Validators.validateEmail(email)
        val passErr = Validators.validatePassword(pass)
        val telErr = Validators.validatePhone(telefone)
        val nifFormatoErr = Validators.validateNifFormat(nif)

        val firstErr = nomeErr ?: emailErr ?: passErr ?: telErr ?: nifFormatoErr
        if (firstErr != null) {
            _uiState.update { it.copy(errorMessage = firstErr) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val nifUnicoErr = Validators.validateUniqueNif(nif)
            if (nifUnicoErr != null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = nifUnicoErr) }
                return@launch
            }

            val regResult = authRepository.registerMunicipe(
                nome = nome,
                email = email,
                password = pass,
                nif = nif,
                telefone = telefone,
                latitude = lat,
                longitude = lng,
                detecaoImpactos = impactos
            )

            regResult.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = user,
                        successMessage = "Conta criada com sucesso! Aguarda validação camarária do NIF."
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun registerOperador(
        nome: String,
        email: String,
        pass: String,
        nif: String,
        telefone: String
    ) {
        val nomeErr = Validators.validateName(nome)
        val emailErr = Validators.validateOperatorEmail(email)
        val passErr = Validators.validatePassword(pass)
        val telErr = Validators.validatePhone(telefone)
        val nifFormatoErr = Validators.validateNifFormat(nif)

        val firstErr = nomeErr ?: emailErr ?: passErr ?: telErr ?: nifFormatoErr
        if (firstErr != null) {
            _uiState.update { it.copy(errorMessage = firstErr) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val nifUnicoErr = Validators.validateUniqueNif(nif)
            if (nifUnicoErr != null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = nifUnicoErr) }
                return@launch
            }

            val regResult = authRepository.registerOperador(
                nome = nome,
                email = email,
                password = pass,
                nif = nif,
                telefone = telefone
            )

            regResult.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentUser = user,
                        successMessage = "Conta de Operador ativa com sucesso!"
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun fetchCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUserProfile().onSuccess { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun updateProfile(telefone: String, fotoUrl: String?, impactos: Boolean?) {
        val user = _uiState.value.currentUser ?: return
        val telErr = Validators.validatePhone(telefone)
        if (telErr != null) {
            _uiState.update { it.copy(errorMessage = telErr) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val isOperador = user.perfil == "operador"
            val result = userRepository.updateProfile(user.uid, isOperador, telefone, fotoUrl, impactos)
            result.onSuccess {
                fetchCurrentUser()
                _uiState.update { it.copy(isLoading = false, successMessage = "Perfil atualizado!") }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val emailErr = Validators.validateEmail(email)
        if (emailErr != null) {
            _uiState.update { it.copy(errorMessage = emailErr) }
            return
        }

        viewModelScope.launch {
            authRepository.sendPasswordReset(email).onSuccess {
                _uiState.update { it.copy(successMessage = "E-mail de recuperação enviado!") }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun logout() {
        authRepository.logout()
        _uiState.value = AuthUiState()
    }
}