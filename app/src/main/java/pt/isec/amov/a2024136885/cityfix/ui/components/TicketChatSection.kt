package pt.isec.amov.a2024136885.cityfix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.model.Message
import pt.isec.amov.a2024136885.cityfix.data.repository.ChatRepository
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.ChatUiState
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.ChatViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.ChatViewModelFactory
import java.text.SimpleDateFormat

@Composable
internal fun ChatOcorrencia(id: String, encerrada: Boolean) {
    val factory = remember(id) { ChatViewModelFactory(id, ChatRepository()) }
    val chat: ChatViewModel = viewModel(key = "chat:$id", factory = factory)
    val estado by chat.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(encerrada) { chat.setTicketEncerrado(encerrada) }
    TicketChatSection(
        uiState = estado.copy(isEncerrada = encerrada),
        onSendMessage = chat::sendMessage
    )
}

@Composable
fun TicketChatSection(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var ultimoEnvioConfirmado by rememberSaveable { mutableIntStateOf(uiState.mensagensEnviadas) }
    LaunchedEffect(uiState.mensagensEnviadas) {
        if (ultimoEnvioConfirmado != uiState.mensagensEnviadas) {
            inputText = ""
            ultimoEnvioConfirmado = uiState.mensagensEnviadas
        }
    }
    val listState = rememberLazyListState()
    val primaryGreen = MaterialTheme.colorScheme.primary

    // Faz scroll automático para a mensagem mais recente
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 450.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = stringResource(R.string.chat_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = primaryGreen,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (uiState.errorMessage != null) {
            Text(stringResource(R.string.chat_error), color = MaterialTheme.colorScheme.error)
        }

        // Lista de Mensagens
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.isLoading) {
                item { CircularProgressIndicator(modifier = Modifier.size(20.dp)) }
            } else if (uiState.messages.isEmpty() && uiState.errorMessage == null) {
                item {
                    Text(
                        text = stringResource(R.string.chat_empty),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(uiState.messages, key = { it.id }) { msg ->
                    val isMine = msg.autorId == uiState.currentUserId
                    MessageBubble(message = msg, isMine = isMine)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barra de Envio ou Mensagem de Bloqueio
        if (uiState.isEncerrada) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.chat_locked),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(stringResource(R.string.chat_input)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true,
                    enabled = !uiState.isSending
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                        }
                    },
                    enabled = inputText.isNotBlank() && !uiState.isSending
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = primaryGreen)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_send),
                            tint = if (inputText.isNotBlank()) primaryGreen else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isMine: Boolean) {
    val primaryGreen = MaterialTheme.colorScheme.primary
    val bubbleColor = if (isMine) primaryGreen else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val alignment = if (isMine) Alignment.End else Alignment.Start

    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(locale) { SimpleDateFormat("HH:mm", locale) }
    val formattedTime = message.enviadoEm?.toDate()?.let { timeFormat.format(it) } ?: ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isMine) {
            Text(
                text = if (message.perfil == "operador") {
                    stringResource(R.string.chat_operator, message.autorNome)
                } else message.autorNome,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isMine) 14.dp else 2.dp,
                        bottomEnd = if (isMine) 2.dp else 14.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(text = message.texto, color = textColor, fontSize = 14.sp)
                if (formattedTime.isNotBlank()) {
                    Text(
                        text = formattedTime,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                    )
                }
            }
        }
    }
}