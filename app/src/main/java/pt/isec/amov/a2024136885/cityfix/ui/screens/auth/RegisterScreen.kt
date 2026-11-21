package pt.isec.amov.a2024136885.cityfix.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthUiState

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    currentLatitude: Double = 40.2081, // Default Praça da República
    currentLongitude: Double = -8.4194,
    onRegisterMunicipe: (String, String, String, String, String, Double, Double, Boolean) -> Unit,
    onRegisterOperador: (String, String, String, String, String) -> Unit,
    onNavigateBack: () -> Unit,
    onClearError: () -> Unit
) {
    var isOperador by remember { mutableStateOf(false) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var nif by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var impactos by remember { mutableStateOf(true) }

    val primaryGreen = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryGreen)
        Text(
            if (isOperador) "Municipal Operator Profile" else "Citizen Registration (Requires validation)",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Seletor de Perfil
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isOperador,
                onClick = { isOperador = false; onClearError() },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Citizen")
            }
            SegmentedButton(
                selected = isOperador,
                onClick = { isOperador = true; onClearError() },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Operator")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it; onClearError() },
            label = { Text("Full Name") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; onClearError() },
            label = { Text(if (isOperador) "Municipal E-mail (@cm-*.pt)" else "E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it; onClearError() },
            label = { Text("Password (8+ chars, upper, lower, num, sym)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nif,
            onValueChange = { if (it.length <= 9) nif = it; onClearError() },
            label = { Text("NIF (9 digits)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = telefone,
            onValueChange = { if (it.length <= 9) telefone = it; onClearError() },
            label = { Text("Phone Number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (!isOperador) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = impactos,
                    onCheckedChange = { impactos = it },
                    colors = CheckboxDefaults.colors(checkedColor = primaryGreen)
                )
                Text("Enable Impact Detection", style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (uiState.successMessage != null) {
            Text(
                text = uiState.successMessage,
                color = primaryGreen,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (isOperador) {
                    onRegisterOperador(nome, email, pass, nif, telefone)
                } else {
                    onRegisterMunicipe(nome, email, pass, nif, telefone, currentLatitude, currentLongitude, impactos)
                }
            },
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
                Text("Register Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        TextButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp)) {
            Text("Already have an account? Sign In", color = primaryGreen)
        }
    }
}