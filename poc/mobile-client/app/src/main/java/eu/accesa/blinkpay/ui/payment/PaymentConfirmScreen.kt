package eu.accesa.blinkpay.ui.payment

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
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.accesa.blinkpay.data.model.QrPaymentData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmScreen(
    qrData: QrPaymentData,
    onPaymentComplete: (success: Boolean, uetr: String?, message: String) -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as FragmentActivity

    LaunchedEffect(state) {
        when (val s = state) {
            is PaymentState.Success -> {
                onPaymentComplete(true, s.uetr, "Payment settled successfully")
            }
            is PaymentState.Failed -> {
                onPaymentComplete(false, null, s.message)
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Confirm Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = state is PaymentState.Idle) {
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Merchant icon
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pay ${qrData.creditorName}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Amount
            Text(
                text = "€${"%.2f".format(qrData.amount)}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = qrData.currency,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Payment details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PaymentDetailRow("Merchant", qrData.creditorName)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    PaymentDetailRow("IBAN", qrData.creditorIban)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    PaymentDetailRow("Reference", qrData.reference.take(8) + "...")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Pay button or loading
            when (state) {
                is PaymentState.Idle -> {
                    Button(
                        onClick = { viewModel.confirmPayment(activity, qrData) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(
                            text = "Pay €${"%.2f".format(qrData.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                is PaymentState.Authenticating -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Verifying identity...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
                is PaymentState.Processing -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Processing payment...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaymentDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}
