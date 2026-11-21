package pt.isec.amov.a2024136885.cityfix.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthUiState

@Composable
fun MfaChallengeScreen(
    uid: String,
    uiState: AuthUiState,
    onVerifyClick: (String, String) -> Unit,
    onCancelClick: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val primaryGreen = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Two-Factor Authentication",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = primaryGreen
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A 6-digit security code was generated for your device notification. Enter it below to proceed.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Security Code (6 digits)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(16.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 22.sp, letterSpacing = 4.sp),
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onVerifyClick(uid, code) },
            enabled = code.length == 6 && !uiState.isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Confirm", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        TextButton(onClick = onCancelClick, modifier = Modifier.padding(top = 12.dp)) {
            Text("Cancel", color = Color.Gray)
        }
    }
}