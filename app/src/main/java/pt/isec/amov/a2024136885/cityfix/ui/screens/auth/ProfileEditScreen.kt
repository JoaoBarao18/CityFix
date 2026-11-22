package pt.isec.amov.a2024136885.cityfix.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthUiState

@Composable
fun ProfileEditScreen(
    uiState: AuthUiState,
    onSaveClick: (String, Boolean?) -> Unit,
    onBackClick: () -> Unit
) {
    val user = uiState.currentUser
    val isOperador = user?.perfil == "operador"
    val primaryGreen = MaterialTheme.colorScheme.primary

    var telefone by remember(user) { mutableStateOf(user?.telefone ?: "") }
    var impactos by remember(user) { mutableStateOf(user?.detecaoImpactos ?: false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text("Edit Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
        Text("Immutable fields are locked by municipal policy", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))

        // Campo Bloqueado: Nome
        OutlinedTextField(
            value = user?.nome ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Full Name (Locked)") },
            trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Campo Bloqueado: NIF
        OutlinedTextField(
            value = user?.nif ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("NIF (Locked)") },
            trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (!isOperador) {
            Spacer(modifier = Modifier.height(12.dp))
            // Campo Bloqueado: Georreferenciação
            OutlinedTextField(
                value = "${user?.latitude ?: 0.0}, ${user?.longitude ?: 0.0}",
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Initial Geolocation (Locked)") },
                trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        // Campo Editável: Telefone
        OutlinedTextField(
            value = telefone,
            onValueChange = { if (it.length <= 9) telefone = it },
            label = { Text("Phone Number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (!isOperador) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(
                    checked = impactos,
                    onCheckedChange = { impactos = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = primaryGreen, checkedTrackColor = primaryGreen.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Impact Detection", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (uiState.errorMessage != null) {
            Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        }

        if (uiState.successMessage != null) {
            Text(text = uiState.successMessage, color = primaryGreen, modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSaveClick(telefone, if (!isOperador) impactos else null) },
            enabled = !uiState.isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        TextButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
            Text("Back", color = Color.Gray)
        }
    }
}