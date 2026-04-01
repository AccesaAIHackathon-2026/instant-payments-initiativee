package eu.accesa.blinkpay.ui.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.accesa.blinkpay.biometric.BiometricHelper
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcSendScreen(
    viewModel: NfcSendViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Enable NFC reader mode while this screen is active
    DisposableEffect(Unit) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (nfcAdapter != null && activity != null) {
            nfcAdapter.enableReaderMode(
                activity,
                viewModel.readerCallback,
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null,
            )
        }
        onDispose {
            val adapter = NfcAdapter.getDefaultAdapter(context)
            if (adapter != null && activity != null) {
                adapter.disableReaderMode(activity)
            }
            viewModel.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send Digital Euro") },
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
                is NfcSendUiState.WaitingForTap -> WaitingForTapContent()

                is NfcSendUiState.Confirming -> ConfirmingContent(
                    amount = state.amount,
                    receiverName = state.receiverName,
                    receiverIban = state.receiverIban,
                    onConfirm = {
                        val fragmentActivity = activity as? FragmentActivity
                        if (fragmentActivity != null) {
                            scope.launch {
                                val helper = BiometricHelper(fragmentActivity)
                                val authenticated = helper.authenticateWithDeviceCredential(
                                    title = "Confirm NFC Transfer",
                                    subtitle = "€${state.amount.toPlainString()} to ${state.receiverName}",
                                )
                                if (authenticated) {
                                    viewModel.onAuthenticationSuccess()
                                } else {
                                    viewModel.onAuthenticationFailed()
                                }
                            }
                        }
                    },
                    onCancel = {
                        viewModel.reset()
                        onBack()
                    },
                )

                is NfcSendUiState.Authenticating -> ProcessingContent()

                is NfcSendUiState.Success -> SuccessContent(
                    amount = state.amount,
                    receiverName = state.receiverName,
                    onDone = {
                        viewModel.reset()
                        onDone()
                    },
                )

                is NfcSendUiState.InsufficientBalance -> InsufficientBalanceContent(
                    amount = state.amount,
                    available = state.available,
                    onBack = {
                        viewModel.reset()
                        onBack()
                    },
                )

                is NfcSendUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.reset() },
                    onBack = {
                        viewModel.reset()
                        onBack()
                    },
                )
            }
        }
    }
}

@Composable
private fun WaitingForTapContent() {
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
        text = "Tap phone to pay",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Hold your phone near the receiver's device",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ConfirmingContent(
    amount: BigDecimal,
    receiverName: String,
    receiverIban: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Icon(
        imageVector = Icons.Default.Nfc,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Confirm Transfer",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "€${amount.toPlainString()}",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "To: $receiverName",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = receiverIban,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "via NFC  ·  Digital Euro",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Keep phones together until transfer completes",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(48.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
        Button(onClick = onConfirm) {
            Text("Confirm & Authenticate")
        }
    }
}

@Composable
private fun ProcessingContent() {
    CircularProgressIndicator(modifier = Modifier.size(48.dp))

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Completing transfer...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Keep phones together",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
    )
}

@Composable
private fun SuccessContent(
    amount: BigDecimal,
    receiverName: String,
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
        text = "Transfer Successful",
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
        text = "Sent to $receiverName",
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
private fun InsufficientBalanceContent(
    amount: BigDecimal,
    available: BigDecimal,
    onBack: () -> Unit,
) {
    Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.error,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Insufficient Balance",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Requested: €${amount.toPlainString()}",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Text(
        text = "Available: €${available.toPlainString()}",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
    )

    Spacer(modifier = Modifier.height(48.dp))

    Button(onClick = onBack) {
        Text("Go Back")
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Icon(
        imageVector = Icons.Default.Error,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.error,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(32.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Go Back")
        }
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
