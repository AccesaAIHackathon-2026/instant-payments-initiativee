# BlinkPay Mobile App — Implementation Plan

## Context

The BlinkPay Android app is the consumer wallet for the SEPA Instant Payments POC. The current project is a bare Android skeleton (AGP 9.1.0, Gradle 9.3.1) with no Kotlin plugin, no Compose, and no source code. We need to build a Compose-based wallet app supporting QR scan-to-pay, offline NFC P2P digital euro transfers, biometric authentication, and white-label theming.

---

## Screens (8 total)

| # | Screen | Purpose |
|---|--------|---------|
| 1 | **BiometricLockScreen** | App-level biometric gate on launch |
| 2 | **HomeScreen** | Dashboard: bank + digital euro balances, quick action buttons |
| 3 | **QrScanScreen** | CameraX viewfinder + ML Kit barcode scanning |
| 4 | **PaymentConfirmScreen** | "Pay €X to Y?" + biometric trigger |
| 5 | **PaymentResultScreen** | Success/failure result |
| 6 | **NfcSendScreen** | Enter amount → tap to send digital euros offline |
| 7 | **NfcReceiveScreen** | Listen for NFC tap → receive digital euros offline |
| 8 | **TransactionsScreen** | Transaction history list |

**Navigation:**
```
Launch → BiometricLock → Home
                          ├── Scan & Pay → QrScan → PaymentConfirm → PaymentResult
                          ├── Send NFC → NfcSend → PaymentResult
                          ├── Receive NFC → NfcReceive → PaymentResult
                          └── History → Transactions
```

---

## Package Structure

```
eu.accesa.blinkpay/
├── BlinkPayApp.kt                     # Application class
├── MainActivity.kt                    # Single-Activity Compose host
├── navigation/
│   └── NavGraph.kt                    # NavHost with all routes
├── ui/
│   ├── theme/
│   │   ├── Color.kt                   # Two color palettes (BlinkPay + WhiteLabel)
│   │   ├── Type.kt                    # Typography
│   │   ├── Theme.kt                   # BlinkPayTheme composable + switching
│   │   └── ThemeConfig.kt             # AppTheme enum + CompositionLocal
│   ├── lock/
│   │   ├── BiometricLockScreen.kt
│   │   └── BiometricLockViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── qr/
│   │   ├── QrScanScreen.kt
│   │   └── QrScanViewModel.kt
│   ├── payment/
│   │   ├── PaymentConfirmScreen.kt
│   │   ├── PaymentResultScreen.kt
│   │   └── PaymentViewModel.kt
│   ├── nfc/
│   │   ├── NfcSendScreen.kt
│   │   ├── NfcReceiveScreen.kt
│   │   └── NfcViewModel.kt
│   └── transactions/
│       ├── TransactionsScreen.kt
│       └── TransactionsViewModel.kt
├── data/
│   ├── api/
│   │   ├── BankApi.kt                 # Retrofit interface (6 endpoints)
│   │   ├── ApiClient.kt              # Retrofit/OkHttp singleton → http://10.0.2.2:8080
│   │   └── dto/                       # PaymentRequest, ScaRequest, AccountResponse, etc.
│   ├── model/                         # Domain models: Account, Transaction, QrPaymentData
│   └── repository/
│       ├── AccountRepository.kt
│       ├── PaymentRepository.kt
│       └── DigitalEuroLedger.kt       # Local DE balance + offline NFC sync queue
├── nfc/
│   ├── NfcHostApduService.kt          # HCE sender service
│   ├── NfcReaderCallback.kt           # Reader mode receiver
│   └── NfcPayloadCodec.kt            # JSON ↔ bytes for NFC payload
├── biometric/
│   └── BiometricHelper.kt            # Wraps BiometricPrompt, exposes suspend fun
└── util/
    └── ServiceLocator.kt             # Manual DI container (POC-appropriate)
```

---

## Theming Strategy

**Runtime toggle** via CompositionLocal — no build flavors needed for the POC.

- `AppTheme` enum: `BLINKPAY`, `WHITELABEL`
- `BlinkPayTheme` composable selects Material3 `ColorScheme` based on current theme
- **BlinkPay palette**: Primary `#1A73E8` (vibrant blue), Secondary `#00C9A7` (mint)
- **WhiteLabel palette**: Primary `#455A64` (blue-grey), Secondary `#26A69A` (teal)
- Theme state held in `ServiceLocator.themeState: MutableStateFlow<AppTheme>`
- Toggle via icon button on HomeScreen top bar

---

## Offline NFC P2P Design

**API choice:** HCE (Host Card Emulation) + Reader Mode. Android Beam is deprecated/removed.

**Protocol:**
1. **Sender (Alice)** activates HCE service with payment payload
2. **Receiver (Bob)** enters reader mode, sends SELECT AID (`F0424C4E4B504159`)
3. Sender responds with JSON payload: `{amount, senderIban, nonce, timestamp}`
4. Receiver ACKs, both update local `DigitalEuroLedger`
5. No network needed for the tap

**Local Ledger (`DigitalEuroLedger`):**
- In-memory balance + list of `OfflineTransfer` objects (pending sync)
- `debit()` / `credit()` for NFC operations
- `syncWithBank()` called when connectivity is restored (via `ConnectivityManager` callback in `HomeViewModel`)

**Required files:**
- `res/xml/nfc_apdu_service.xml` — AID registration
- HCE service declared in AndroidManifest

---

## Biometric Authentication

Two usage points, single helper:
1. **App lock**: On launch, `BiometricLockScreen` requires fingerprint/face ID. Failure = stays locked.
2. **Payment confirm**: Before SCA call, biometric prompt. Success → POST /bank/sca with PIN "1234".

`BiometricHelper` wraps `BiometricPrompt` and exposes `suspend fun authenticate(): Boolean` using `suspendCancellableCoroutine`. Works because `ComponentActivity` (Compose) extends `FragmentActivity`.

---

## Implementation Phases

### Phase 0 — Project Bootstrap
Modify build files, add all dependencies, create Application class, MainActivity, theme files, ServiceLocator. Update AndroidManifest with Activity + permissions.

**Modify:** `libs.versions.toml`, root `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, `AndroidManifest.xml`
**Create:** `BlinkPayApp.kt`, `MainActivity.kt`, `Color.kt`, `Type.kt`, `Theme.kt`, `ThemeConfig.kt`, `ServiceLocator.kt`
**Verify:** App compiles and launches showing themed placeholder.

### Phase 1 — Core Infrastructure
Retrofit API client, DTOs, repositories, BiometricHelper, NavGraph with placeholder screens.

**Create:** `BankApi.kt`, `ApiClient.kt`, all DTOs, domain models, repositories, `BiometricHelper.kt`, `NavGraph.kt`
**Verify:** Navigation works between empty screens. Biometric prompt shows on emulator.

### Phase 2 — QR Scan & Pay (MVP)
Full flow: BiometricLock → Home (balances) → QR scan → confirm → biometric → pay → result.

**Create:** All screen + ViewModel files for `lock/`, `home/`, `qr/`, `payment/`
**Verify:** End-to-end QR payment works against bank simulator.

### Phase 3 — Transactions
Transaction history screen, wire HomeViewModel to real API data.

**Create:** `TransactionsScreen.kt`, `TransactionsViewModel.kt`
**Verify:** Home shows real balances, history loads.

### Phase 4 — Offline NFC P2P
HCE service, reader mode, payload codec, digital euro ledger, NFC screens.

**Create:** All `nfc/` files, `DigitalEuroLedger.kt`, NFC screens, `nfc_apdu_service.xml`
**Verify:** Two physical devices can transfer digital euros via NFC tap. (Emulator cannot test NFC — mock the NFC layer for unit testing.)

### Phase 5 — Theming & Polish
Finalize both color schemes, add theme toggle, loading states, error handling.

---

## Key Dependencies

| Library | Purpose |
|---------|---------|
| Compose BOM 2025.04.00 | UI framework |
| Material3 | Design system |
| Navigation Compose | Screen routing |
| Lifecycle ViewModel Compose | MVVM |
| Retrofit2 + Gson converter | REST client |
| OkHttp + logging interceptor | HTTP layer |
| ML Kit Barcode Scanning | QR code reading |
| CameraX | Camera viewfinder for QR |
| AndroidX Biometric | Fingerprint/Face ID |

---

## Known Risks

1. **AGP 9.1.0 compatibility** — very new, may have Compose issues. Fallback: downgrade to 8.9.x
2. **NFC requires physical devices** — emulator has no NFC support. Dev/test NFC via mocked layer
3. **Biometric on emulator** — works via `adb -e emu finger touch 1` with enrolled fingerprint
4. **CameraX in Compose** — requires `AndroidView` wrapper with lifecycle management

---

## Verification Plan

1. **Phase 0**: `./gradlew assembleDebug` succeeds, app launches on emulator
2. **Phase 2**: Scan a test QR → confirm → biometric → payment settles (requires bank-simulator running on port 8080)
3. **Phase 4**: Two phones: Alice sends €5 DE via NFC → Bob receives → both ledgers update
4. **Phase 5**: Toggle theme on HomeScreen → colors change app-wide
