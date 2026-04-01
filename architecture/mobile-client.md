# Mobile Client

The mobile client is a native Android wallet app for consumers. It supports QR-based merchant payments, peer-to-peer transfers via phone alias, offline NFC Digital Euro transfers, and wallet management.

## Tech Stack

- Kotlin, Android API 26+ (Android 8.0)
- Jetpack Compose (Material 3) for UI
- MVVM architecture with ViewModel + StateFlow
- Retrofit 2 + OkHttp for REST API calls
- CameraX + ML Kit Barcode Scanning for QR
- NFC ISO-DEP (Host Card Emulation + Reader Mode)
- WorkManager for offline sync
- Jetpack Navigation for screen routing

## Architecture

```mermaid
graph TB
    subgraph UI Layer
        LS[Lock Screen]
        HS[Home Screen]
        QS[QR Scan Screen]
        PC[Payment Confirm]
        PR[Payment Result]
        NS[NFC Send Screen]
        NR[NFC Receive Screen]
        AS[Account Screen]
    end

    subgraph ViewModel Layer
        HVM[HomeViewModel]
        PVM[PaymentViewModel]
        NSVM[NfcSendViewModel]
        NRVM[NfcReceiveViewModel]
        AVM[AccountViewModel]
        BVM[BiometricLockViewModel]
    end

    subgraph Data Layer
        API[BankApi<br/>Retrofit]
        LED[DigitalEuroLedger<br/>In-Memory]
        SES[UserSession<br/>SharedPreferences]
        SW[OfflineSyncWorker<br/>WorkManager]
    end

    subgraph NFC Layer
        NB[NfcBridge<br/>Singleton]
        RC[NfcReaderCallback]
        HCE[NfcHostApduService]
        COD[NfcPayloadCodec]
    end

    HS --> HVM
    QS --> PVM
    PC --> PVM
    NS --> NSVM
    NR --> NRVM
    AS --> AVM
    LS --> BVM

    HVM --> API
    PVM --> API
    AVM --> API
    NSVM --> NB
    NRVM --> NB
    NSVM --> LED
    NRVM --> LED
    SW --> API
    SW --> LED

    NB --> RC
    NB --> HCE
    RC --> COD
    HCE --> COD

    API -->|HTTP| Bank[Bank Simulator :8080]
```

## Screens and Navigation

```mermaid
graph TD
    LOCK[Lock Screen<br/>PIN Entry] -->|Unlock| HOME[Home Screen<br/>Dashboard]
    HOME -->|Scan & Pay| QR[QR Scan Screen]
    HOME -->|NFC Send| NSEND[NFC Send Screen]
    HOME -->|NFC Receive| NRECV[NFC Receive Screen]
    HOME -->|Account| ACCT[Account Screen]
    QR -->|Barcode detected| CONFIRM[Payment Confirm Screen]
    CONFIRM -->|SCA complete| RESULT[Payment Result Screen]
    RESULT -->|Done| HOME
    NSEND -->|Done| HOME
    NRECV -->|Done| HOME
    ACCT -->|Back| HOME
```

### Screen Details

| Screen | Purpose | ViewModel |
|--------|---------|-----------|
| **Lock Screen** | PIN entry (1111=Alice, 2222=Bob, 4444=Charlie). Sets `UserSession` with IBAN, wallet ID, bank URL. | `BiometricLockViewModel` |
| **Home Screen** | Dashboard with bank balance, DE wallet balance, and action card grid (Scan & Pay, NFC Send, NFC Receive, Account). | `HomeViewModel` |
| **QR Scan Screen** | Camera preview with ML Kit barcode detection. Parses EPC069-12 GiroCode QR data. | — |
| **Payment Confirm** | Shows payment details (amount, creditor, reference). Calls `POST /bank/pay` then `POST /bank/sca`. | `PaymentViewModel` |
| **Payment Result** | Success (green check, UETR) or failure (red X, human-readable error). | — |
| **NFC Send Screen** | Tap to pay. NFC reader mode reads payment request, sender authenticates with biometrics, sends confirmation. | `NfcSendViewModel` |
| **NFC Receive Screen** | Enter amount, activate HCE card emulation, wait for sender tap. | `NfcReceiveViewModel` |
| **Account Screen** | View balances, top-up DE wallet from bank, redeem DE wallet to bank. | `AccountViewModel` |

## API Integration

All calls go through `BankApi` (Retrofit interface) via `ApiClient` singleton. API key is injected via OkHttp interceptor.

| Method | Endpoint | Used By |
|--------|----------|---------|
| `GET` | `/bank/accounts/{iban}` | HomeViewModel, AccountViewModel |
| `GET` | `/bank/wallet/{walletId}` | HomeViewModel, AccountViewModel |
| `POST` | `/bank/wallet/{walletId}/topup` | AccountViewModel |
| `POST` | `/bank/wallet/{walletId}/redeem` | AccountViewModel |
| `POST` | `/bank/pay` | PaymentViewModel |
| `POST` | `/bank/sca` | PaymentViewModel |
| `POST` | `/bank/wallet/{walletId}/sync-offline-transactions` | OfflineSyncWorker |

## NFC Peer-to-Peer Architecture

The NFC subsystem enables offline Digital Euro transfers between two phones without network connectivity.

### Protocol

- Transport: ISO-DEP (ISO 14443-4) over NFC
- Custom AID: `F0424C4E4B504159` ("BlinkPay" in hex)
- Payload encoding: JSON via Gson, wrapped in APDU commands
- Status words: `0x9000` (success), `0x6A82` (no data available)

### Flow

```mermaid
sequenceDiagram
    participant S as Sender Phone<br/>(Reader Mode)
    participant R as Receiver Phone<br/>(HCE)

    Note over R: User enters amount,<br/>activates HCE
    S->>R: SELECT AID command
    R-->>S: Payment request JSON<br/>{amount, receiverName, receiverIban, walletId}
    Note over S: User sees amount +<br/>receiver details
    S->>S: Biometric authentication
    S->>R: CONFIRM command<br/>{senderName, senderIban}
    R-->>S: ACK (0x9000)
    S->>S: Debit local ledger
    R->>R: Credit local ledger
    Note over S,R: Both schedule<br/>bank sync
```

### Components

| Component | Role |
|-----------|------|
| `NfcBridge` | Thread-safe singleton bridging HCE/Reader binder threads to UI coroutines via StateFlow |
| `NfcHostApduService` | Android HCE service responding to reader commands as an emulated card |
| `NfcReaderCallback` | NFC reader mode callback that discovers tags and exchanges APDUs |
| `NfcPayloadCodec` | JSON serialization/deserialization of APDU payloads |
| `BiometricHelper` | AndroidX Biometric wrapper for fingerprint/device credential prompts |

## Offline Sync

After each NFC transfer, the app schedules a bank sync via WorkManager:

```mermaid
sequenceDiagram
    participant NFC as NFC Transfer
    participant LED as DigitalEuroLedger
    participant WM as WorkManager
    participant API as Bank API

    NFC->>LED: Record transfer (synced=false)
    NFC->>WM: schedule(5s delay, REPLACE)

    Note over WM: Waits for network

    WM->>LED: getUnsyncedTransfers()
    LED-->>WM: [transfer1, transfer2, ...]
    WM->>API: POST /bank/wallet/{id}/sync-offline-transactions
    API-->>WM: {acceptedIds, duplicateIds}
    WM->>LED: markSynced(acceptedIds + duplicateIds)
```

### DigitalEuroLedger

In-memory singleton tracking all offline transfers:

| Field | Type | Description |
|-------|------|-------------|
| `transactionId` | String (UUID) | Unique identifier, used as idempotency key |
| `counterpartyName` | String | Other party's display name |
| `counterpartyIban` | String | Other party's IBAN |
| `amount` | BigDecimal | Transfer amount |
| `isSend` | Boolean | true = debit, false = credit |
| `synced` | Boolean | Whether bank has acknowledged this transfer |
| `timestamp` | Long | Epoch millis |

### OfflineSyncWorker

- **Trigger**: Enqueued after each NFC send/receive with 5-second initial delay
- **Policy**: `ExistingWorkPolicy.REPLACE` (coalesces rapid transfers)
- **Constraints**: Requires network connectivity
- **Retry**: Exponential backoff (30s base) on failure
- **Idempotency**: Server deduplicates by `transactionId`; both accepted and duplicate IDs are marked as synced

## Session Management

`UserSession` persists the active user identity in SharedPreferences:

| Field | Source |
|-------|--------|
| `iban` | Set on PIN login (hardcoded per demo user) |
| `holderName` | Set on PIN login |
| `phoneAlias` | Set on PIN login |
| `walletId` | Set on PIN login |
| `bankBaseUrl` | Set on PIN login (Bank A or Bank B URL) |

The app locks when returning from background, requiring PIN re-entry.
