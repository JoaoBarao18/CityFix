package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestoreException
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Category
import pt.isec.amov.a2024136885.cityfix.data.repository.CategoryRepository

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

internal class CriarOcorrenciaViewModel : ViewModel() {
    private val categoryRepository by lazy { CategoryRepository() }

    suspend fun carregarCategoriasAtivas(): Result<List<Category>> {
        return try {
            Result.success(categoryRepository.listarAtivas())
        } catch (erro: FirebaseFirestoreException) {
            Result.failure(erro)
        }
    }

    fun prepararSubmissao(
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

        return DadosAberturaOcorrencia(
            categoriaId = requireNotNull(categoriaId),
            situacao = requireNotNull(situacao),
            descricao = descricao.trim(),
            publica = publica,
            latitude = requireNotNull(latitude),
            longitude = requireNotNull(longitude),
            fotoPath = fotoPath,
            videoPath = videoPath
        )
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
