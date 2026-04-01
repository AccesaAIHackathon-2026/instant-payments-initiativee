package eu.accesa.blinkpay.ui.nfc

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcReceiveScreen(
    viewModel: NfcReceiveViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive Digital Euro") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.reset()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val state = uiState) {
                is NfcReceiveUiState.EnteringAmount -> EnterAmountContent(
                    onStart = { amount -> viewModel.startReceiving(amount) },
                )

                is NfcReceiveUiState.Waiting -> WaitingContent(amount = state.amount)

                is NfcReceiveUiState.Received -> ReceivedContent(
                    amount = state.amount,
                    senderName = state.senderName,
                    onDone = {
                        viewModel.reset()
                        onDone()
                    },
                )

                is NfcReceiveUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.reset() },
                )
            }
        }
    }
}

@Composable
private fun EnterAmountContent(onStart: (BigDecimal) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    val isValid = amountText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } ?: false

    Icon(
        imageVector = Icons.Default.Nfc,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Enter amount to receive",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = amountText,
        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text("Amount (EUR)") },
        prefix = { Text("€ ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { amountText.toBigDecimalOrNull()?.let(onStart) },
        enabled = isValid,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Start Receiving")
    }
}

@Composable
private fun WaitingContent(amount: BigDecimal) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Icon(
        imageVector = Icons.Default.Nfc,
        contentDescription = null,
        modifier = Modifier
            .size(96.dp)
            .alpha(alpha),
        tint = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "€${amount.toPlainString()}",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Waiting for sender to tap...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Hold your phone ready for NFC",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
    )
}

@Composable
private fun ReceivedContent(
    amount: BigDecimal,
    senderName: String,
    onDone: () -> Unit,
) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Transfer Received",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "€${amount.toPlainString()}",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "From: $senderName",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "via NFC  ·  Digital Euro",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
    )

    Spacer(modifier = Modifier.height(48.dp))

    Button(onClick = onDone) {
        Text("Done")
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(onClick = onRetry) {
        Text("Try Again")
    }
}
