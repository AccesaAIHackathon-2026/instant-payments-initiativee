# Hackathon POC Plan — Instant Payments + Digital Euro Wallet

**Format:** One-day hackathon
**Goal:** End-to-end demo of SEPA Instant (FIPS) payment flow with Digital Euro wallet support, across four simulated components.

---

## Components Overview

```
+------------------+          +-------------------+
|   User App       |          |   Retailer App    |
|  (mobile/web)    |          |  (merchant POS)   |
+--------+---------+          +--------+----------+
         |                             |
         |    pain.001 / REST          |    RTP (pain.013) / QR
         v                             v
+--------+-----------------------------+----------+
|                Simulated Bank                   |
|   Accounts · Proxy Lookup · VoP · SCA stub      |
+------------------------+------------------------+
                         |
                         | pacs.008 / pacs.002
                         v
+------------------------+------------------------+
|            Simulated FIPS Network               |
|  SCT Inst routing · Settlement · Status         |
+-------------------------------------------------+
```

---

## Component 1 — Simulated FIPS Network

**Role:** The clearing and settlement backbone. Receives `pacs.008` messages from the bank, routes to the beneficiary bank, returns `pacs.002` confirmation.

**What it simulates:**
- TIPS / RT1 routing logic
- Instant settlement (sub-second in simulation)
- Transaction status lifecycle: `RCVD → ACSP → ACSC` (or `RJCT`)
- 10-second timeout enforcement

**Tech stack:**
- Java 21 + Spring Boot 3.x REST service
- In-memory transaction store (`ConcurrentHashMap`, no persistence needed for POC)
- Endpoints:
  - `POST /fips/submit` — receive `pacs.008`, return `pacs.002`
  - `GET  /fips/status/{uetr}` — transaction status
  - `GET  /fips/transactions` — admin view (all transactions)

**Key data model:**
```
Transaction {
  uetr: UUID
  debtorIBAN: String
  creditorIBAN: String
  amount: BigDecimal
  currency: String ("EUR")
  status: RCVD | ACSP | ACSC | RJCT
  createdAt: Instant
  settledAt: Instant?
}
```

**Scope for hackathon:** Happy path + one rejection scenario (insufficient funds).

---

## Component 2 — Simulated Bank

**Role:** Acts as both debtor PSP and creditor PSP. Holds accounts, validates payments, performs proxy lookup (phone → IBAN), handles SCA stub, and bridges user/retailer apps to the FIPS network.

**What it simulates:**
- Account management (pre-seeded test accounts)
- Balance checking
- Proxy lookup: phone number or email → IBAN
- Verification of Payee (VoP): name match on IBAN
- SCA stub: any PIN "1234" is accepted
- **Two distinct payment modes (important architectural distinction):**
  - **SEPA Instant / A2A** — debits the commercial bank balance only; Digital Euro wallet is never touched
  - **DE P2P Transfer (stretch)** — debits the DE custody wallet; reverse waterfall auto-tops-up from bank if DE insufficient; forward waterfall caps DE holding on the receiving side
- Translates SEPA Instant calls from apps into `pacs.008` toward FIPS network
- Settles DE transfers directly between custody wallets (no FIPS involvement)

**Tech stack:**
- Java 21 + Spring Boot 3.x REST service
- In-memory account store (`ConcurrentHashMap`, pre-seeded)
- Endpoints:
  - `POST /bank/pay` — initiate payment (triggers pacs.008 to FIPS)
  - `GET  /bank/accounts/{iban}` — balance + transaction history
  - `GET  /bank/vop?iban=&name=` — Verification of Payee
  - `GET  /bank/proxy?alias=` — resolve phone/email to IBAN
  - `POST /bank/sca` — SCA challenge (stub: PIN = 1234)
  - `POST /bank/request-to-pay` — receive RTP from retailer, notify user
  - `GET  /bank/accounts/{iban}/digital-euro` — Digital Euro balance

**Pre-seeded test accounts:**

| Name | IBAN | Phone | Bank Balance | Digital Euro |
|------|------|-------|-------------|--------------|
| Alice Consumer | DE89370400440532013001 | +49111000001 | €1,000 | €50 |
| Bob Consumer | DE89370400440532013002 | +49111000002 | €500 | €20 |
| Retail Store GmbH | DE89370400440532013099 | — | €0 | €0 |

---

## Component 3 — User App (Consumer)

**Role:** Mobile-first wallet app for the end user. Supports P2P payments and paying a retailer via QR code scan (MVP) or incoming Request-to-Pay notification (stretch).

**Flows to demo:**
1. **P2P payment (SEPA Instant)** — Alice sends €10 to Bob via phone number proxy; debits bank balance only
2. **QR merchant payment (MVP)** — Alice scans retailer QR code, confirms payment, pays instantly; debits bank balance only
3. **RTP merchant payment (stretch)** — Alice receives incoming payment request notification, approves
4. **Digital Euro balance** — Alice's wallet shows split view: bank balance + Digital Euro balance; user can manually top-up/redeem between them
5. **DE P2P Transfer (stretch)** — Alice taps Bob's phone (NFC simulated via QR/Wi-Fi); DE balance moves device-to-device with no server involved; waterfall only triggers at re-sync when back online

**Tech stack:**
- Native Android app (Kotlin + Jetpack Compose)
- Retrofit2 + OkHttp for REST calls to Bank Simulator
- ML Kit Barcode Scanning for QR code reading
- MVVM: `ViewModel` + `StateFlow` for UI state
- Navigation Component for screen routing

**Key screens:**
- Home: balances (bank + Digital Euro)
- Send: enter phone/email → resolve to name via VoP → confirm → SCA PIN
- QR Scanner: camera via ML Kit → parse payment details → confirm → SCA PIN
- History: recent transactions

---

## Component 4 — Retailer App (Merchant POS)

**Role:** Point-of-sale terminal simulation. Generates QR codes for customer-initiated payments (MVP) and can send Request-to-Pay for retailer-initiated payments (stretch).

**Flows to demo:**
1. **Generate QR (MVP)** — retailer enters amount, generates QR code displayed on screen; Alice scans it
2. **Request-to-Pay (stretch)** — retailer enters amount + customer phone, sends RTP; Alice approves in her app
3. **Settlement notification** — polls bank for settlement confirmation after payment

**Tech stack:**
- React (TypeScript) web app
- `qrcode.react` for QR code generation
- Calls Bank Simulator REST APIs only

**Key screens:**
- POS: enter amount → generate QR (large, scannable display)
- Awaiting payment: spinner + polling bank for settlement
- Confirmed: green screen with transaction reference + amount
- (Stretch) RTP screen: enter amount + customer phone → send request → await approval

---

## Payment Flows in Detail

### Flow A — P2P (Alice → Bob via phone number)

```
Alice App  →  Bank: POST /bank/pay { alias: "+49111000002", amount: 10 }
Bank       →  Bank: proxy lookup → Bob's IBAN
Bank       →  Bank: VoP check → "Bob Consumer" matches
Bank       →  Alice App: SCA challenge
Alice App  →  Bank: POST /bank/sca { pin: "1234" }
Bank       →  FIPS: POST /fips/submit { pacs.008 }
FIPS       →  Bank: pacs.002 { ACSC }
Bank       →  Alice App: { status: SETTLED, reference: "..." }
```

### Flow B1 — QR Merchant Payment / MVP (Alice-initiated)

Alice drives the payment by scanning the retailer's QR code. The retailer is passive after generating it.

```
Retailer App  →  cashier enters amount (€25)
Retailer App  →  generates QR: { amount: 25, creditorIBAN: "DE..99", merchantName: "Retail Store GmbH" }
Retailer App  →  displays QR on screen

Alice App     →  opens QR scanner (ML Kit camera)
Alice App     →  scans QR → parses payload
Alice App     →  displays: "Pay €25 to Retail Store GmbH?"
Alice App     →  Bank: POST /bank/pay { debtorIBAN: "DE..01", creditorIBAN: "DE..99", amount: 25 }
Bank          →  returns SCA challenge token
Alice App     →  Bank: POST /bank/sca { token, pin: "1234" }
Bank          →  FIPS: POST /fips/submit { pacs.008 }
FIPS          →  Bank: pacs.002 { ACSC }
Bank          →  Alice App: { status: SETTLED, uetr: "..." }

Retailer App  →  polling GET /bank/transactions/DE..99 (every 2s)
Retailer App  →  detects incoming credit → green confirmation screen
```

### Flow B2 — Request-to-Pay / Stretch (Retailer-initiated)

The retailer sends a payment request to Alice. Alice is notified and approves.

```
Retailer App  →  cashier enters amount (€25) + customer phone (+49111000001)
Retailer App  →  Bank: POST /bank/request-to-pay { amount: 25, debtorAlias: "+49111000001", creditorIBAN: "DE..99" }
Bank          →  stores pending RTP (status: PENDING)
Bank          →  returns { rtpId: "..." }

Alice App     →  polling GET /bank/incoming-rtp/DE..01 (every 2s)
Alice App     →  detects pending RTP → displays: "Pay €25 to Retail Store GmbH?" [Approve / Reject]
Alice App     →  Bank: POST /bank/sca { rtpId, pin: "1234" }
Bank          →  FIPS: POST /fips/submit { pacs.008 }
FIPS          →  Bank: pacs.002 { ACSC }
Bank          →  marks RTP status: SETTLED

Retailer App  →  polling GET /bank/rtp-status/{rtpId} (every 2s)
Retailer App  →  detects SETTLED → green confirmation screen
```

### Flow C — DE P2P Transfer / Stretch (simulated NFC Alice → Bob)

#### Real-world model (what we are simulating)

Digital Euro is stored **on the device** (secure element / hardware wallet), not on the bank's server.
Transfers are **fully offline, device-to-device via NFC** — no bank, no FIPS, no internet required.

```
Real-world lifecycle:

  [ONLINE — pre-load]
  Alice goes online → calls bank → bank debits her bank account, credits her device SE
  Alice's device now holds €50 DE locally.

  [OFFLINE — NFC transfer]
  Alice taps Charlie's phone.
  Alice's SE: -€30  (cryptographic debit, no server involved)
  Charlie's SE: +€30 (cryptographic credit, no server involved)
  Transaction is final. Neither bank nor FIPS is contacted.

  [ONLINE — re-sync, when device reconnects]
  Charlie's device syncs with his bank.
  If Charlie's DE balance > €3 000 (holding limit):
    Forward waterfall → excess automatically moved to Charlie's bank account.
  If Alice wants to reload her SE before the next offline payment:
    Reverse waterfall → Alice tops up from her bank account (requires connectivity).
```

**Key rules:**
- The bank server only tracks what was loaded onto / redeemed from the device. It does NOT track individual offline spends.
- Waterfall and reverse waterfall are **re-sync-time** operations, not transfer-time.
- Reverse waterfall is really just **pre-loading**: Alice must be online to top up her device before spending offline. If her device balance is insufficient she cannot pay — unlike SEPA Instant there is no automatic mid-transfer bank debit.
- **No FIPS, no inter-bank routing** — ever. Not for intra-bank, not for inter-bank. DE transfers never touch the SCT Inst rail.

#### POC simulation (what we actually build)

Real NFC / secure elements are out of scope. We simulate the offline transfer as a local balance exchange between the two apps over Wi-Fi, treating each app's `SharedPreferences` as the device's secure element:

```
POC Flow:

  [Top-up — already implemented]
  Alice app → POST /bank/wallet/{id}/topup { amount: 50 }
  Bank debits Alice's bank account, acknowledges the load.
  App stores DE balance locally (SharedPreferences).

  [NFC simulation — new, stretch]
  Alice app generates a signed transfer token (amount + sender IBAN + nonce).
  Charlie's app receives token (QR scan or direct Wi-Fi exchange).
  Both apps update their local DE balances immediately — no server call.

  [Re-sync — new, stretch]
  When Charlie's app goes online:
    POST /bank/wallet/{id}/sync { currentBalance }
    If balance > €3 000: bank auto-redeems excess → Charlie's bank account credited.
  When Alice's app goes online:
    POST /bank/wallet/{id}/sync { currentBalance }
    Bank records the spend (loaded amount vs current balance = spent amount).
```

**POC simplification for demo day:** if implementing the full local-storage model is too expensive,
treat `WalletStore` as the device ledger mirror and demonstrate the transfer via a direct API call
between the two apps — clearly labelling it as "NFC simulation" in the UI. The waterfall/re-sync
step can still be demonstrated separately as a manual "sync" button.

**No new FIPS interaction. No pacs.008. No inter-bank routing.**

---

## Day Schedule

| Time | Activity |
|------|----------|
| 09:00 – 09:30 | Kickoff: align on architecture, split into sub-teams |
| 09:30 – 12:00 | Build phase 1: FIPS Network + Bank backend (core payment loop) |
| 12:00 – 12:30 | Integration checkpoint: FIPS ↔ Bank end-to-end working |
| 12:30 – 13:00 | Lunch |
| 13:00 – 15:30 | Build phase 2: User App + Retailer App |
| 15:30 – 16:00 | Integration: all four components connected |
| 16:00 – 16:30 | Demo rehearsal + bug fixes |
| 16:30 – 17:00 | Final demo |

---

## Team Split (suggested)

| Sub-team | Owns |
|----------|------|
| **Backend (2 people)** | FIPS Network + Simulated Bank |
| **Frontend (2 people)** | User App + Retailer App |

---

## MVP vs Stretch Goals

### MVP (must work for demo)
- [ ] Flow A: P2P SEPA Instant payment — Alice → Bob via phone proxy (bank balance)
- [ ] Flow B1: QR merchant payment — Alice scans retailer QR, pays instantly (bank balance)
- [ ] Real-time settlement confirmation on retailer screen (polling)
- [ ] Digital Euro balance displayed separately from bank balance
- [ ] Manual top-up / redeem between bank and DE wallet
- [ ] Payment confirm screen labels payment as "via SEPA Instant · Bank balance"

### Stretch (build only if core flows done before 15:00)
- [ ] Flow B2: Request-to-Pay — retailer sends RTP, Alice approves in app
- [ ] Flow C: DE P2P Transfer — Alice → Bob offline (NFC simulated); waterfall/reverse waterfall at re-sync only
- [ ] VoP mismatch warning in UI
- [ ] Transaction history screen
- [ ] Rejection scenario (insufficient funds)
- [ ] Animated 10-second settlement countdown

---

## Next Steps (post-hackathon / upcoming sprint)

### 1 — DE Direct Transfer with offline model + waterfall on re-sync

**Backend (`bank-simulator`)**
- `POST /bank/de-transfer` — accepts `{ debtorIBAN, creditorIBAN, amount, transferToken }`;
  no SCA step (offline transaction is pre-authorised on device); debits debtor DE wallet, credits creditor DE wallet
- `POST /bank/wallet/{id}/sync { currentBalance }` — called when device reconnects;
  if `currentBalance > 3000`: forward waterfall → redeem excess to bank account;
  if `currentBalance < loaded`: record spend delta (audit trail)
- No FIPS involvement — DE transfers settle entirely within the bank custody ledger

**Android (`mobile-client`)**
- Store DE balance locally in `SharedPreferences` (device = secure element proxy)
- NFC beam (primary): `NfcAdapter` Android Beam / HCE — Alice's phone sends a signed transfer token to Charlie's phone; both update local DE balance immediately
- QR fallback: Alice generates a DE transfer QR (amount + sender IBAN + nonce + amount); Charlie scans it; same local balance update
- "Sync" button on Account screen: calls `/bank/wallet/{id}/sync`; triggers forward waterfall display if over limit
- Reverse waterfall pre-load: if Alice's local DE balance < desired send amount, prompt to top-up from bank first (requires connectivity)

**Testing**
- Two real Android phones on same Wi-Fi — Alice taps Charlie, both balances update locally, then each syncs independently
- QR fallback tested on emulator (no NFC)

---

### 2 — Bank Admin Web App

New React (TypeScript) app at port `3002` (or a tab within the existing retail app).

**Pages:**
- **Accounts** — table of all accounts for the selected bank; shows IBAN, holder name, type, bank balance, DE wallet balance; filter by type (CONSUMER / MERCHANT)
- **Transactions** — full transaction ledger across all accounts; columns: UETR, timestamp, debtor, creditor, amount, status (ACSC / RJCT)
- **Wallets** — all DE custody wallets; wallet ID, owner IBAN, balance, loaded total vs current (spend delta)
- **FIPS log** — proxy view of `GET /fips/transactions` showing all inter-bank settlements

**Bank selector:** dropdown or URL param switches between bank-a (`:8080`) and bank-b (`:8082`).

**New endpoints needed (bank-simulator):**
```
GET /bank/admin/accounts        # all accounts
GET /bank/admin/transactions    # all transactions across all accounts
GET /bank/admin/wallets         # all DE wallets
```

---

### 3 — Retailer Web App: dual-bank pages

The existing `retail-web-app` currently hardcodes bank-a's API. Extend to serve both retailers:

- **Route `/bank-a`** — Retail Store GmbH (bank-a, IBAN `DE89370400440532013099`, port `8080`)
- **Route `/bank-b`** — Metro Market (bank-b, IBAN `DE89370400440532014099`, port `8082`)
- A landing page at `/` lets the cashier pick which retailer they are operating as
- Each retailer page is self-contained: generates QR for its own IBAN, polls its own bank for settlement confirmation
- Bank URL and merchant IBAN passed as config or derived from the route — no code duplication

---

## Tech Decisions 

| Decision | Choice | Reason |
|----------|--------|--------|
| Backend language | Java 21 + Spring Boot 3.x | Team expertise; Java records for clean DTOs |
| Consumer client | Android + Kotlin (Jetpack Compose) | Native UX; ML Kit QR scanning built-in |
| Retailer client | React (TypeScript) | Fastest for a web POS; no mobile needed |
| ISO 20022 messages | Simplified JSON representation | Full XML not needed for internal simulation |
| Inter-service communication | REST/HTTP | Simple, no message broker overhead for POC |
| Persistence | In-memory (ConcurrentHashMap) | Speed; data survives the demo session |
| SCA | PIN stub (1234) | Real biometrics out of scope for 1-day POC |
| Android network | 10.0.2.2:8080 (emulator) | Emulator localhost alias for host machine |

---

## Out of Scope

- Real TIPS/RT1 connectivity
- Real eIDAS certificates or OAuth2
- Real NFC / Secure Element (DE P2P transfer is simulated; NFC + SE is the real-world delivery mechanism)
- Multi-bank routing for SEPA Instant (single bank instance simulates both sides for A2A flows)
- Cryptographic signing of DE transfer tokens (POC uses plain exchange; real deployment requires SE-backed signing)
- Automatic re-sync on reconnect (POC exposes a manual "Sync" button for demo purposes)
- Sanctions screening
- Production-grade security
