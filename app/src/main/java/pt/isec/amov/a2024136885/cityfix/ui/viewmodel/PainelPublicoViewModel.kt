package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import pt.isec.amov.a2024136885.cityfix.data.repository.DadosPainelPublico
import pt.isec.amov.a2024136885.cityfix.data.repository.PainelPublicoRepository

internal data class PainelPublicoEstado(
    val dados: DadosPainelPublico? = null,
    val erro: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
internal class PainelPublicoViewModel : ViewModel() {
    private val repositorio by lazy { PainelPublicoRepository() }
    private val tentativa = MutableStateFlow(0)

    val estado = tentativa.flatMapLatest {
        repositorio.observar()
            .map { PainelPublicoEstado(dados = it) }
            .onStart { emit(PainelPublicoEstado()) }
            .catch { emit(PainelPublicoEstado(erro = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), PainelPublicoEstado())

    fun tentarNovamente() = tentativa.update { it + 1 }

    suspend fun urlAnexo(caminho: String): String? = try {
        withTimeoutOrNull(10_000) { repositorio.urlAnexo(caminho) }
    } catch (erro: Exception) {
        if (erro is CancellationException) throw erro
        null
    }
}
