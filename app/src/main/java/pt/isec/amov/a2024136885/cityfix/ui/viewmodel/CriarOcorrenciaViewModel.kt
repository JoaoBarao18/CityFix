package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Category
import pt.isec.amov.a2024136885.cityfix.data.repository.CategoryRepository
import pt.isec.amov.a2024136885.cityfix.data.repository.CriarOcorrenciaWorker
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRepository
import kotlinx.coroutines.flow.first
import java.util.UUID

internal val situacoesOcorrencia = mapOf(
    "dano" to R.string.report_situation_damage,
    "avaria" to R.string.report_situation_breakdown,
    "destruicao" to R.string.report_situation_destruction,
    "roubo" to R.string.report_situation_theft
)

// Dados já validados do formulário, com os anexos ainda no armazenamento local.
data class DadosAberturaOcorrencia(
    val categoriaId: String,
    val situacao: String,
    val descricao: String,
    val publica: Boolean,
    val latitude: Double,
    val longitude: Double,
    val fotoPath: String?,
    val videoPath: String?
)

data class ErrosCriarOcorrencia(
    val categoria: Int? = null,
    val situacao: Int? = null,
    val descricao: Int? = null,
    val localizacao: Int? = null,
    val anexos: Int? = null
) {
    val formularioValido: Boolean
        get() = categoria == null && situacao == null && descricao == null &&
            localizacao == null && anexos == null
}

internal class CriarOcorrenciaViewModel(
    private val ocorrenciaRepository: OcorrenciaRepository = OcorrenciaRepository(),
    private val categoryRepository: CategoryRepository = CategoryRepository(),
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val _estadoAgendamento = MutableStateFlow<Int?>(null)
    val estadoAgendamento = _estadoAgendamento.asStateFlow()
    val idSubmissao = savedStateHandle.getStateFlow<String?>("idSubmissao", null)

    fun observarEnvio(context: Context, id: String) =
        WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(CriarOcorrenciaWorker.nome(id))

    suspend fun carregarCategoriasAtivas(): Result<List<Category>> {
        return try {
            categoryRepository.getActiveCategoriesFlow().first()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun submeter(context: Context, autorId: String, dados: DadosAberturaOcorrencia) {
        if (_estadoAgendamento.value == R.string.report_submission_sending) return
        val id = idSubmissao.value ?: UUID.randomUUID().toString()

        savedStateHandle["idSubmissao"] = id
        _estadoAgendamento.value = R.string.report_submission_sending
        viewModelScope.launch {
            try {
                ocorrenciaRepository.criarOcorrencia(
                    context = context,
                    categoriaId = dados.categoriaId,
                    situacao = dados.situacao,
                    descricao = dados.descricao,
                    publica = dados.publica,
                    latitude = dados.latitude,
                    longitude = dados.longitude,
                    fotoPath = dados.fotoPath,
                    thumbPath = null,
                    videoPath = dados.videoPath
                )
                _estadoAgendamento.value = null
            } catch (erro: Exception) {
                if (erro is CancellationException) throw erro
                Log.w("CriarOcorrencia", "Falha ao agendar o envio", erro)
                _estadoAgendamento.value = R.string.report_submission_unavailable
            }
        }
    }

    fun prepararSubmissao(
        context: Context,
        categorias: List<Category>,
        categoriaId: String?,
        situacao: String?,
        descricao: String,
        publica: Boolean,
        latitude: Double?,
        longitude: Double?,
        fotoPath: String?,
        videoPath: String?
    ): DadosAberturaOcorrencia? {
        val erros = validar(
            categorias = categorias,
            categoriaId = categoriaId,
            situacao = situacao,
            descricao = descricao,
            latitude = latitude,
            longitude = longitude,
            temAnexo = !fotoPath.isNullOrBlank() || !videoPath.isNullOrBlank()
        )
        if (!erros.formularioValido) return null

        val dados = DadosAberturaOcorrencia(
            categoriaId = requireNotNull(categoriaId),
            situacao = requireNotNull(situacao),
            descricao = descricao.trim(),
            publica = publica,
            latitude = requireNotNull(latitude),
            longitude = requireNotNull(longitude),
            fotoPath = fotoPath,
            videoPath = videoPath
        )

        viewModelScope.launch {
            ocorrenciaRepository.criarOcorrencia(
                context = context,
                categoriaId = dados.categoriaId,
                situacao = dados.situacao,
                descricao = dados.descricao,
                publica = dados.publica,
                latitude = dados.latitude,
                longitude = dados.longitude,
                fotoPath = dados.fotoPath,
                thumbPath = null,
                videoPath = dados.videoPath
            )
        }

        return dados
    }

    fun validar(
        categorias: List<Category>,
        categoriaId: String?,
        situacao: String?,
        descricao: String,
        latitude: Double?,
        longitude: Double?,
        temAnexo: Boolean
    ): ErrosCriarOcorrencia {
        val categoriaAtiva = categorias.any { it.ativa && it.id == categoriaId }
        val situacaoValida = situacao in situacoesOcorrencia
        val localizacaoValida = latitude != null && latitude in -90.0..90.0 &&
            longitude != null && longitude in -180.0..180.0

        return ErrosCriarOcorrencia(
            categoria = R.string.report_required_category.takeUnless { categoriaAtiva },
            situacao = R.string.report_required_situation.takeUnless { situacaoValida },
            descricao = R.string.report_required_description.takeIf { descricao.isBlank() },
            localizacao = R.string.report_required_location.takeUnless { localizacaoValida },
            anexos = R.string.report_required_attachment.takeUnless { temAnexo }
        )
    }
}
