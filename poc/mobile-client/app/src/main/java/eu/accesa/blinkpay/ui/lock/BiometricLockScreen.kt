package eu.accesa.blinkpay.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BiometricLockScreen(
    onUnlocked: () -> Unit,
    viewModel: BiometricLockViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as FragmentActivity

    // Initialize and attempt biometric on first composition
    LaunchedEffect(Unit) {
        viewModel.initialize(activity)
        if (uiState.useBiometric) {
            viewModel.attemptBiometric(activity, onUnlocked)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // App logo / title
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "BlinkPay",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.useBiometric) {
            Text(
                text = "Use biometric to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        } else {
            Text(
                text = "Enter PIN to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PIN dots
        PinDots(filledCount = uiState.pinInput.length)

        Spacer(modifier = Modifier.height(8.dp))

        // Error messages
        if (uiState.pinError) {
            Text(
                text = "Incorrect PIN. Try again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (uiState.authFailed) {
            Text(
                text = "Authentication failed. Use PIN instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Number pad
        NumberPad(
            onDigit = { digit ->
                viewModel.onPinDigitEntered(digit)
                // Auto-submit when 4 digits entered
                if (uiState.pinInput.length == 3) {
                    // After this digit, length will be 4
                    viewModel.verifyPin(onUnlocked)
                }
            },
            onDelete = { viewModel.onPinDelete() },
            onBiometric = if (uiState.useBiometric) {
                { viewModel.attemptBiometric(activity, onUnlocked) }
            } else null,
        )

        // Fallback link when biometric is available
        if (uiState.useBiometric && !uiState.authFailed) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                viewModel.attemptBiometric(activity, onUnlocked)
            }) {
                Text("Tap to retry biometric")
            }
        }
    }
}

@Composable
private fun PinDots(filledCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < filledCount) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onBiometric: (() -> Unit)?,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                row.forEach { digit ->
                    NumberKey(digit = digit, onClick = { onDigit(digit) })
                }
            }
        }

        // Bottom row: biometric / 0 / delete
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Biometric button or empty space
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                if (onBiometric != null) {
                    IconButton(onClick = onBiometric) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Use biometric",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            NumberKey(digit = '0', onClick = { onDigit('0') })

            // Delete button
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKey(digit: Char, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}
