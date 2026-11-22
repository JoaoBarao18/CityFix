package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRepository
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRespository
import pt.isec.amov.a2024136885.cityfix.data.repository.temParticipante

internal data class MinhaOcorrencia(
    val dados: Ocorrencia,
    val categoria: String?,
    val imagem: String?,
    val video: String? = null
) {
    val emGrupo get() = dados.seguidoresIds.any { it != dados.autorId }
}

internal data class EstadoReabertura(val aReabrir: Boolean = false, val erro: Boolean = false)

internal class MinhasOcorrenciasViewModel : ViewModel() {
    private val repositorio by lazy { OcorrenciaRespository() }
    private val ciclo by lazy { OcorrenciaRepository() }
    private val _reabertura = MutableStateFlow(EstadoReabertura())
    val reabertura = _reabertura.asStateFlow()

    fun observar(autorId: String) = repositorio.observarMinhas(autorId)
        .map { lista ->
            val categorias = lista.map { it.categoriaId }.distinct().associateWith { id ->
                tentar { repositorio.nomeCategoria(id) }
            }
            val dados = coroutineScope {
                lista.map { ocorrencia ->
                    async {
                        val foto = ocorrencia.ficheiros.firstOrNull { it.tipo == "foto" }
                        MinhaOcorrencia(
                            ocorrencia,
                            categorias[ocorrencia.categoriaId],
                            obterUrl(foto?.thumbnailPath ?: foto?.storagePath)
                        )
                    }
                }.awaitAll()
            }
            Result.success(dados)
        }.catch { emit(Result.failure(it)) }

    fun observarDetalhe(autorId: String, id: String) = ciclo.getOcorrenciaFlow(id).map { resultado ->
        val ocorrencia = resultado.getOrThrow()
            ?.takeIf { it.temParticipante(autorId) }
            ?: error("Ocorrência indisponível")
        val foto = ocorrencia.ficheiros.firstOrNull { it.tipo == "foto" }
        val video = ocorrencia.ficheiros.firstOrNull { it.tipo == "video" }
        Result.success(MinhaOcorrencia(
            ocorrencia,
            tentar { repositorio.nomeCategoria(ocorrencia.categoriaId) },
            obterUrl(foto?.storagePath),
            obterUrl(video?.storagePath)
        ))
    }.catch { emit(Result.failure(it)) }

    fun reabrir(id: String, justificacao: String) {
        if (_reabertura.value.aReabrir || justificacao.trim().length < 10) return
        _reabertura.value = EstadoReabertura(aReabrir = true)
        viewModelScope.launch {
            _reabertura.value = EstadoReabertura(erro = ciclo.reabrirOcorrencia(id, justificacao).isFailure)
        }
    }

    fun limparErroReabertura() {
        _reabertura.value = EstadoReabertura()
    }

    private suspend fun obterUrl(caminho: String?): String? {
        if (caminho.isNullOrBlank()) return null
        if (caminho.startsWith("http://") || caminho.startsWith("https://") || caminho.startsWith("file://") || caminho.startsWith("content://")) {
            return caminho
        }
        return tentar { repositorio.urlAnexo(caminho) }
    }

    private suspend fun <T> tentar(acao: suspend () -> T): T? = try {
        withTimeoutOrNull(2_000) { acao() }
    } catch (erro: Exception) {
        if (erro is CancellationException) throw erro
        null
    }
}

internal val estadosOcorrencia = mapOf(
    "aberto" to R.string.report_state_open,
    "pedido_informacoes" to R.string.report_state_info_requested,
    "em_resolucao" to R.string.report_state_in_progress,
    "resolvido" to R.string.report_state_resolved,
    "encerrado" to R.string.report_state_closed
)
