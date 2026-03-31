package eu.accesa.blinkpay.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private fun humanReadableReason(reason: String?): String = when (reason) {
    "AM04" -> "Insufficient funds"
    "AC01" -> "Invalid creditor account"
    "AG01" -> "Payment blocked by receiving bank"
    "AG02" -> "Invalid payment reference"
    "FOCR" -> "Payment returned by creditor"
    "NARR" -> "Payment rejected — no reason given"
    null   -> "Payment rejected"
    else   -> reason
}

@Composable
fun PaymentResultScreen(
    success: Boolean,
    uetr: String?,
    reason: String?,
    onDone: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = if (success) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (success) "Payment Successful" else "Payment Failed",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (success && uetr != null) {
                Text(
                    text = "Transaction ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Text(
                    text = uetr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            if (!success) {
                Text(
                    text = humanReadableReason(reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(onClick = onDone) {
                Text("Done")
            }
        }
    }
}
