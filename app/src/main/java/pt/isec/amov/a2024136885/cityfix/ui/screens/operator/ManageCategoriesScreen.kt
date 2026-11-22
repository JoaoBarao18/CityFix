package pt.isec.amov.a2024136885.cityfix.ui.screens.operator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov.a2024136885.cityfix.data.model.Category
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.CategoryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    uiState: CategoryUiState,
    onAddCategory: (String) -> Unit,
    onEditCategory: (String, String) -> Unit,
    onToggleCategoryStatus: (String, Boolean) -> Unit,
    onBackClick: () -> Unit,
    onClearFeedback: () -> Unit
) {
    val primaryGreen = MaterialTheme.colorScheme.primary
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(uiState.feedbackMessage, uiState.errorMessage) {
        val msg = uiState.feedbackMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onClearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configuração de Tipologias",
                        fontWeight = FontWeight.Bold,
                        color = primaryGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = primaryGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = primaryGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Categoria")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.categories.isEmpty()) {
                CircularProgressIndicator(
                    color = primaryGreen,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.categories.isEmpty()) {
                Text(
                    text = "Nenhuma categoria configurada.\nCrie a primeira com o botão +",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.categories, key = { it.id }) { cat ->
                        CategoryCard(
                            category = cat,
                            onEditClick = { categoryToEdit = cat },
                            onToggleStatus = { novoEstado ->
                                onToggleCategoryStatus(cat.id, novoEstado)
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo Adicionar
    if (showAddDialog) {
        CategoryInputDialog(
            dialogTitle = "Nova Tipologia",
            initialValue = "",
            confirmButtonLabel = "Criar",
            onDismiss = { showAddDialog = false },
            onConfirm = { nome ->
                onAddCategory(nome)
                showAddDialog = false
            }
        )
    }

    // Diálogo Editar
    categoryToEdit?.let { cat ->
        CategoryInputDialog(
            dialogTitle = "Editar Tipologia",
            initialValue = cat.nome,
            confirmButtonLabel = "Guardar",
            onDismiss = { categoryToEdit = null },
            onConfirm = { novoNome ->
                onEditCategory(cat.id, novoNome)
                categoryToEdit = null
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: Category,
    onEditClick: () -> Unit,
    onToggleStatus: (Boolean) -> Unit
) {
    val primaryGreen = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (category.ativa) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (category.ativa) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
                Text(
                    text = if (category.ativa) "Ativa (visível a munícipes)" else "Inativa",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (category.ativa) primaryGreen else Color.Gray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = primaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Switch(
                    checked = category.ativa,
                    onCheckedChange = onToggleStatus,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryGreen,
                        checkedTrackColor = primaryGreen.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun CategoryInputDialog(
    dialogTitle: String,
    initialValue: String,
    confirmButtonLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    val primaryGreen = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nome da Tipologia") },
                placeholder = { Text("Ex: Vias Públicas, Água, Iluminação") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}