package com.example.travelplanner.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.travelplanner.security.BiometricAuthResult
import com.example.travelplanner.security.BiometricAuthState
import com.example.travelplanner.security.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Result<Unit>,
    onOpenRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    showRegisterOnly: Boolean = false,
    securityViewModel: SecurityViewModel? = null,
    onBiometricLogin: () -> Result<Unit> = {
        Result.failure(IllegalStateException("Біометричний вхід недоступний"))
    }
) {
    if (showRegisterOnly) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Увійдіть або зареєструйтеся") }) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Схоже, ви ще не зареєструвалися. Створіть профіль, щоб продовжити.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenRegister,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Зареєструватися")
                }
            }
        }
        return
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val securityState by securityViewModel?.uiState?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val canUseBiometrics = securityState?.biometricEnabled == true &&
        securityState?.availability?.isSupported == true
    val biometricBusy = securityState?.authState == BiometricAuthState.Authenticating

    Scaffold(
        topBar = { TopAppBar(title = { Text("Увійти в профіль") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Введіть дані профіля",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = false },
                label = { Text("Email") },
                isError = emailError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = false },
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            authError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    emailError = email.isBlank() || !email.contains("@")
                    passwordError = password.length < 6
                    authError = null
                    if (!emailError && !passwordError) {
                        onLoginClick(email, password)
                            .onSuccess { onLoginSuccess() }
                            .onFailure { authError = it.message ?: "Помилка входу" }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Увійти")
            }
            if (canUseBiometrics) {
                OutlinedButton(
                    onClick = {
                        authError = null
                        securityViewModel?.authenticateAsync("Увійдіть у профіль без введення пароля") { result ->
                            when (result) {
                                BiometricAuthResult.Success -> {
                                    onBiometricLogin()
                                        .onSuccess { onLoginSuccess() }
                                        .onFailure {
                                            authError = it.message ?: "Не вдалося відкрити профіль"
                                        }
                                }
                                BiometricAuthResult.UserCancelled -> {
                                    authError = "Біометричний вхід скасовано"
                                }
                                is BiometricAuthResult.Failed -> authError = result.message
                                is BiometricAuthResult.SystemError -> authError = result.message
                                is BiometricAuthResult.Unavailable -> authError = result.message
                            }
                        }
                    },
                    enabled = !biometricBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (biometricBusy) "Перевірка..." else "Увійти через біометрію")
                }
            } else {
                securityState?.availability?.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(
                onClick = onOpenRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Зареєструватися")
            }
        }
    }
}
