package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.repository.DetalheOperacional
import pt.isec.amov.a2024136885.cityfix.data.repository.GuardarResolucaoWorker
import pt.isec.amov.a2024136885.cityfix.data.repository.PainelOperacionalRepository
import pt.isec.amov.a2024136885.cityfix.domain.location.registarGeofenceOcorrencia
import pt.isec.amov.a2024136885.cityfix.domain.location.removerGeofenceOcorrencia

internal data class PainelOperacionalUiState(
    val ocorrencias: List<Ocorrencia> = emptyList(),
    val aCarregarLista: Boolean = true,
    val erroLista: Boolean = false,
    val detalhe: DetalheOperacional? = null,
    val aCarregarDetalhe: Boolean = true,
    val erroDetalhe: Boolean = false,
    val aGuardar: Boolean = false,
    val erroAcao: Int? = null,
    val mensagemProximidade: Int? = null,
    val geofencesAtivas: Set<String> = emptySet()
)

internal class PainelOperacionalViewModel : ViewModel() {
    private val repositorio by lazy { PainelOperacionalRepository() }
    private val _estado = MutableStateFlow(PainelOperacionalUiState())
    val estado = _estado.asStateFlow()
    private var pedidoDetalhe: Job? = null
    private var pedidoLista: Job? = null
    private val pedidosProximidade = Mutex()

    fun carregarLista(operadorId: String) {
        pedidoLista?.cancel()
        pedidoLista = viewModelScope.launch {
            _estado.update { it.copy(aCarregarLista = true, erroLista = false) }
            try {
                val lista = repositorio.listar(operadorId)
                _estado.update { it.copy(ocorrencias = lista, aCarregarLista = false) }
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                Log.w("PainelOperacional", "Falha ao obter ocorrências", erro)
                _estado.update { it.copy(aCarregarLista = false, erroLista = true) }
            }
        }
    }

    fun carregarDetalhe(operadorId: String, id: String) {
        pedidoDetalhe?.cancel()
        pedidoDetalhe = viewModelScope.launch {
            _estado.update {
                it.copy(
                    detalhe = null, aCarregarDetalhe = true, erroDetalhe = false,
                    erroAcao = null, mensagemProximidade = null
                )
            }
            try {
                val detalhe = repositorio.carregarDetalhe(operadorId, id)
                _estado.update { it.copy(detalhe = detalhe, aCarregarDetalhe = false) }
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                Log.w("PainelOperacional", "Falha ao obter detalhe", erro)
                _estado.update { it.copy(aCarregarDetalhe = false, erroDetalhe = true) }
            }
        }
    }

    fun iniciarResolucao(operadorId: String, id: String, aposGuardar: (Ocorrencia) -> Unit) {
        guardar(operadorId, id, R.string.operational_start_error, aposGuardar) {
            repositorio.iniciarResolucao(operadorId, id)
        }
    }

    fun resolver(context: Context, operadorId: String, id: String, fotoPath: String?, videoPath: String?) {
        if (_estado.value.aGuardar) return
        _estado.update { it.copy(aGuardar = true, erroAcao = null, mensagemProximidade = null) }
        viewModelScope.launch {
            try {
                GuardarResolucaoWorker.agendar(context, operadorId, id, fotoPath, videoPath)
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                Log.w("PainelOperacional", "Falha ao agendar a resolução", erro)
                _estado.update { it.copy(erroAcao = R.string.operational_resolve_error) }
            } finally {
                _estado.update { it.copy(aGuardar = false) }
            }
        }
    }

    fun atualizarResolucao(context: Context, operadorId: String, id: String) {
        carregarDetalhe(operadorId, id)
        removerProximidade(context, id)
    }

    private fun guardar(
        operadorId: String,
        id: String,
        erroRes: Int,
        aposGuardar: (Ocorrencia) -> Unit,
        acao: suspend () -> Ocorrencia
    ) {
        if (_estado.value.aGuardar) return
        _estado.update { it.copy(aGuardar = true, erroAcao = null, mensagemProximidade = null) }
        viewModelScope.launch {
            try {
                val ocorrencia = acao()
                _estado.update { anterior ->
                    anterior.copy(
                        ocorrencias = anterior.ocorrencias.map { if (it.id == id) ocorrencia else it },
                        detalhe = anterior.detalhe?.takeIf { it.ocorrencia.id == id }?.copy(ocorrencia = ocorrencia)
                            ?: anterior.detalhe
                    )
                }
                aposGuardar(ocorrencia)
                try {
                    val detalhe = repositorio.carregarDetalhe(operadorId, id)
                    _estado.update { anterior ->
                        if (anterior.detalhe?.ocorrencia?.id == id) anterior.copy(detalhe = detalhe) else anterior
                    }
                } catch (erro: Exception) {
                    if (erro is CancellationException) throw erro
                    Log.w("PainelOperacional", "Estado guardado, mas o histórico não foi atualizado no ecrã", erro)
                }
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                Log.w("PainelOperacional", "Falha ao guardar alteração", erro)
                _estado.update { it.copy(erroAcao = erroRes) }
            } finally {
                _estado.update { it.copy(aGuardar = false) }
            }
        }
    }

    suspend fun urlAnexo(operadorId: String, caminho: String): String =
        repositorio.urlAnexo(operadorId, caminho)

    fun registarProximidade(context: Context, ocorrencia: Ocorrencia) {
        if (ocorrencia.estado != "em_resolucao" || ocorrencia.id in _estado.value.geofencesAtivas) return
        val latitude = ocorrencia.latitude
        val longitude = ocorrencia.longitude
        if (latitude == null || longitude == null || latitude !in -90.0..90.0 ||
            longitude !in -180.0..180.0
        ) {
            _estado.update { it.copy(mensagemProximidade = R.string.operational_no_coordinates) }
            return
        }
        viewModelScope.launch {
            pedidosProximidade.withLock {
                if (ocorrencia.id in _estado.value.geofencesAtivas ||
                    _estado.value.detalhe?.ocorrencia?.takeIf { it.id == ocorrencia.id }?.estado != "em_resolucao"
                ) return@withLock
                registarPerimetro(context, ocorrencia.id, latitude, longitude)
            }
        }
    }

    private suspend fun registarPerimetro(context: Context, id: String, latitude: Double, longitude: Double) {
        try {
            registarGeofenceOcorrencia(context, id, latitude, longitude).await()
            _estado.update {
                it.copy(
                    mensagemProximidade = R.string.operational_geofence_active,
                    geofencesAtivas = it.geofencesAtivas + id
                )
            }
        } catch (erro: Exception) {
            if (erro is CancellationException) throw erro
            Log.w("PainelOperacional", "Falha ao registar a geofence", erro)
            _estado.update { it.copy(mensagemProximidade = R.string.operational_geofence_error) }
        }
    }

    fun removerProximidade(context: Context, id: String) {
        viewModelScope.launch {
            try {
                pedidosProximidade.withLock {
                    removerGeofenceOcorrencia(context, id).await()
                    _estado.update {
                        it.copy(mensagemProximidade = null, geofencesAtivas = it.geofencesAtivas - id)
                    }
                }
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                Log.w("PainelOperacional", "Falha ao remover a geofence", erro)
                _estado.update { it.copy(mensagemProximidade = R.string.operational_geofence_remove_error) }
            }
        }
    }
}
