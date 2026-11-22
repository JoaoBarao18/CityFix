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
import pt.isec.amov.a2024136885.cityfix.data.model.Message
import pt.isec.amov.a2024136885.cityfix.data.repository.ChatRepository

data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val currentUserId: String = "",
    val isSending: Boolean = false,
    val mensagensEnviadas: Int = 0,
    val isEncerrada: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val ticketId: String,
    private val chatRepository: ChatRepository = ChatRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(currentUserId = auth.currentUser?.uid ?: "")
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeMessages()
    }

    private fun observeMessages() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            chatRepository.getMessagesFlow(ticketId).collect { result ->
                result.onSuccess { msgs ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            messages = msgs,
                            errorMessage = null
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Erro ao carregar mensagens."
                        )
                    }
                }
            }
        }
    }

    fun setTicketEncerrado(encerrado: Boolean) {
        _uiState.update { it.copy(isEncerrada = encerrado) }
    }

    fun sendMessage(texto: String) {
        if (_uiState.value.isEncerrada || _uiState.value.isSending || texto.isBlank()) return

        _uiState.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            val result = chatRepository.sendMessage(ticketId, texto)
            result.onSuccess {
                _uiState.update { it.copy(isSending = false, mensagensEnviadas = it.mensagensEnviadas + 1) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = err.message ?: "Erro ao enviar mensagem."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class ChatViewModelFactory(
    private val ticketId: String,
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(ticketId, chatRepository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}