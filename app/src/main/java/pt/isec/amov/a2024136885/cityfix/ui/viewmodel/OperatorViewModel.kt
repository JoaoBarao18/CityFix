package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.a2024136885.cityfix.data.model.User
import pt.isec.amov.a2024136885.cityfix.data.repository.UserRepository

data class OperatorUiState(
    val isLoading: Boolean = false,
    val pendingMunicipes: List<User> = emptyList(),
    val validatingUid: String? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class OperatorViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorUiState())
    val uiState: StateFlow<OperatorUiState> = _uiState.asStateFlow()

    init {
        observePendingMunicipes()
    }

    private fun observePendingMunicipes() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            userRepository.getPendingMunicipesFlow().collect { result ->
                result.onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingMunicipes = list,
                            errorMessage = null
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Erro ao carregar pedidos pendentes."
                        )
                    }
                }
            }
        }
    }

    fun approveMunicipe(municipeUid: String) {
        val currentOperadorUid = auth.currentUser?.uid ?: return

        _uiState.update { it.copy(validatingUid = municipeUid, errorMessage = null) }
        viewModelScope.launch {
            val result = userRepository.validateMunicipe(municipeUid, currentOperadorUid)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        validatingUid = null,
                        feedbackMessage = "Munícipe ativado com sucesso!"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        validatingUid = null,
                        errorMessage = err.message ?: "Erro ao ativar o munícipe."
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }
}

class OperatorViewModelFactory(
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OperatorViewModel::class.java)) {
            return OperatorViewModel(userRepository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}