package eu.accesa.blinkpay.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.accesa.blinkpay.data.api.ApiClient
import eu.accesa.blinkpay.data.api.dto.OfflineSyncRequest
import eu.accesa.blinkpay.data.api.dto.OfflineTransactionEntry
import eu.accesa.blinkpay.data.repository.DigitalEuroLedger
import eu.accesa.blinkpay.util.UserSession
import java.time.Instant
import java.util.concurrent.TimeUnit

class OfflineSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val walletId = UserSession.walletId
        if (walletId.isNullOrBlank()) {
            Log.w(TAG, "No walletId in session — skipping sync")
            return Result.failure()
        }

        val unsynced = DigitalEuroLedger.getUnsyncedTransfers()
        if (unsynced.isEmpty()) {
            Log.d(TAG, "No unsynced transfers — nothing to do")
            return Result.success()
        }

        Log.i(TAG, "Syncing ${unsynced.size} offline transaction(s) for wallet $walletId")

        val entries = unsynced.map { transfer ->
            OfflineTransactionEntry(
                transactionId = transfer.transactionId,
                counterpartyIban = transfer.counterpartyIban,
                amount = transfer.amount,
                direction = if (transfer.isSend) "SEND" else "RECEIVE",
                timestamp = Instant.ofEpochMilli(transfer.timestamp).toString(),
            )
        }

        return try {
            val response = ApiClient.bankApi.syncOfflineTransactions(
                walletId = walletId,
                request = OfflineSyncRequest(transactions = entries),
            )

            val allAcknowledged = response.acceptedTransactionIds + response.duplicateTransactionIds
            DigitalEuroLedger.markSynced(allAcknowledged)

            Log.i(TAG, "Sync complete: ${response.acceptedTransactionIds.size} accepted, " +
                    "${response.duplicateTransactionIds.size} duplicates, " +
                    "new wallet balance=€${response.walletBalance}")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed — will retry", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "OfflineSyncWorker"
        private const val UNIQUE_WORK_NAME = "offline_nfc_sync"

        /**
         * Schedule a sync with a short initial delay.
         * Uses [ExistingWorkPolicy.REPLACE] so multiple rapid NFC transfers
         * coalesce into a single sync batch.
         *
         * Requires network connectivity — WorkManager will hold the job
         * until the device is online.
         */
        fun schedule(context: Context, delaySeconds: Long = 5) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)

            Log.i(TAG, "Sync scheduled with ${delaySeconds}s delay")
        }
    }
}
