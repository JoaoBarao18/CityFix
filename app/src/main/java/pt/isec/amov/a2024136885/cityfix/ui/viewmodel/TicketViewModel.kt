package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRepository
import pt.isec.amov.a2024136885.cityfix.util.NotificationHelper

data class TicketUiState(
    val isLoading: Boolean = false,
    val userTickets: List<Ocorrencia> = emptyList(),
    val selectedTicket: Ocorrencia? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class TicketViewModel(
    context: Context,
    private val ocorrenciaRepository: OcorrenciaRepository = OcorrenciaRepository(context.applicationContext),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(TicketUiState())
    val uiState: StateFlow<TicketUiState> = _uiState.asStateFlow()

    private val notificationHelper = NotificationHelper(appContext)
    private val ticketStateCache = mutableMapOf<String, String>()

    init {
        observeUserTickets()
    }

    fun observeUserTickets() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            ocorrenciaRepository.getOcorrenciasByAutorFlow(uid).collect { result ->
                result.onSuccess { list ->
                    // Comparar estado atual de cada ocorrência com o estado anterior em cache
                    list.forEach { ticket ->
                        val estadoAnterior = ticketStateCache[ticket.id]
                        if (estadoAnterior != null && estadoAnterior != ticket.estado) {
                            notificationHelper.showTicketStatusNotification(ticket.id, ticket.estado)
                        }
                        ticketStateCache[ticket.id] = ticket.estado
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userTickets = list,
                            errorMessage = null
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Erro ao carregar ocorrências."
                        )
                    }
                }
            }
        }
    }

    fun observeSingleTicket(ticketId: String) {
        viewModelScope.launch {
            ocorrenciaRepository.getOcorrenciaFlow(ticketId).collect { result ->
                result.onSuccess { ticket ->
                    if (ticket != null) {
                        val estadoAnterior = ticketStateCache[ticket.id]
                        if (estadoAnterior != null && estadoAnterior != ticket.estado) {
                            notificationHelper.showTicketStatusNotification(ticket.id, ticket.estado)
                        }
                        ticketStateCache[ticket.id] = ticket.estado
                    }
                    _uiState.update { it.copy(selectedTicket = ticket) }
                }
            }
        }
    }

    fun reabrirOcorrencia(ticketId: String, justificacao: String) {
        viewModelScope.launch {
            val result = ocorrenciaRepository.reabrirOcorrencia(ticketId, justificacao, appContext)
            result.onSuccess {
                _uiState.update { it.copy(feedbackMessage = "Ocorrência reaberta com sucesso.") }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }
}

class TicketViewModelFactory(
    private val context: Context,
    private val ocorrenciaRepository: OcorrenciaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TicketViewModel::class.java)) {
            return TicketViewModel(context, ocorrenciaRepository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
