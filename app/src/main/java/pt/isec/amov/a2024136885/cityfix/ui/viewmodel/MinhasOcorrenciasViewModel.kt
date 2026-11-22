package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Ocorrencia
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRespository

internal data class MinhaOcorrencia(
    val dados: Ocorrencia,
    val categoria: String?,
    val imagem: String?,
    val video: String? = null
) {
    val emGrupo get() = dados.seguidoresIds.any { it != dados.autorId }
}

internal class MinhasOcorrenciasViewModel : ViewModel() {
    private val repositorio by lazy { OcorrenciaRespository() }

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

    suspend fun carregarDetalhe(autorId: String, id: String): Result<MinhaOcorrencia> =
        try {
            val ocorrencia = repositorio.carregarMinha(autorId, id)
            val foto = ocorrencia.ficheiros.firstOrNull { it.tipo == "foto" }
            val video = ocorrencia.ficheiros.firstOrNull { it.tipo == "video" }
            Result.success(MinhaOcorrencia(
                ocorrencia,
                tentar { repositorio.nomeCategoria(ocorrencia.categoriaId) },
                obterUrl(foto?.storagePath),
                obterUrl(video?.storagePath)
            ))
        } catch (erro: Exception) {
            if (erro is CancellationException) throw erro
            Result.failure(erro)
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
