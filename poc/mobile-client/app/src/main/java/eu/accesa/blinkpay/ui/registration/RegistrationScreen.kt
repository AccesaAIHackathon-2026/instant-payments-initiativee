package eu.accesa.blinkpay.ui.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.math.BigDecimal

@Composable
fun RegistrationScreen(
    onRegistered: () -> Unit,
    viewModel: RegistrationViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is RegistrationUiState.Success) onRegistered()
    }

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("+49") }
    var bankBalance by rememberSaveable { mutableStateOf("1000") }
    var deBalance by rememberSaveable { mutableStateOf("50") }

    val isLoading = uiState is RegistrationUiState.Loading

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "BlinkPay",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Create your account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                placeholder = { Text("+49111000003") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(24.dp))

            HorizontalDivider()
            Text(
                text = "Demo settings",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.padding(vertical = 8.dp),
            )

            OutlinedTextField(
                value = bankBalance,
                onValueChange = { bankBalance = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Starting bank balance (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = deBalance,
                onValueChange = { deBalance = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Starting Digital Euro balance (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))

            Spacer(Modifier.height(24.dp))

            if (uiState is RegistrationUiState.Error) {
                Text(
                    text = (uiState as RegistrationUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            Button(
                onClick = {
                    viewModel.register(
                        holderName = name,
                        phoneAlias = phone,
                        initialBankBalance = bankBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                        initialDeBalance = deBalance.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                else Text("Register")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Identity verification is simulated in this POC.\nIn production, registration uses eIDAS 2.0 / EUDI Wallet attestations.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
