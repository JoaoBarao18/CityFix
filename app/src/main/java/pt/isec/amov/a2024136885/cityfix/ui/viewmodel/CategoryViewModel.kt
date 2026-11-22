package pt.isec.amov.a2024136885.cityfix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.isec.amov.a2024136885.cityfix.data.model.Category
import pt.isec.amov.a2024136885.cityfix.data.repository.CategoryRepository

data class CategoryUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class CategoryViewModel(
    private val categoryRepository: CategoryRepository = CategoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        observeCategories()
    }

    private fun observeCategories() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            categoryRepository.getAllCategoriesFlow().collect { result ->
                result.onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            categories = list,
                            errorMessage = null
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Erro ao carregar categorias."
                        )
                    }
                }
            }
        }
    }

    fun addCategory(nome: String) {
        if (nome.isBlank()) {
            _uiState.update { it.copy(errorMessage = "O nome da categoria não pode ser vazio.") }
            return
        }

        viewModelScope.launch {
            val result = categoryRepository.createCategory(nome)
            result.onSuccess {
                _uiState.update { it.copy(feedbackMessage = "Categoria criada com sucesso!") }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Erro ao criar categoria.") }
            }
        }
    }

    fun editCategory(id: String, novoNome: String) {
        if (novoNome.isBlank()) {
            _uiState.update { it.copy(errorMessage = "O nome da categoria não pode ser vazio.") }
            return
        }

        viewModelScope.launch {
            val result = categoryRepository.updateCategoryName(id, novoNome)
            result.onSuccess {
                _uiState.update { it.copy(feedbackMessage = "Categoria atualizada com sucesso!") }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Erro ao atualizar categoria.") }
            }
        }
    }

    fun toggleCategoryStatus(id: String, novoEstado: Boolean) {
        viewModelScope.launch {
            val result = categoryRepository.toggleCategoryStatus(id, novoEstado)
            result.onSuccess {
                val statusText = if (novoEstado) "ativada" else "desativada"
                _uiState.update { it.copy(feedbackMessage = "Categoria $statusText com sucesso!") }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message ?: "Erro ao alterar estado.") }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }
}

class CategoryViewModelFactory(
    private val categoryRepository: CategoryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
            return CategoryViewModel(categoryRepository) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}