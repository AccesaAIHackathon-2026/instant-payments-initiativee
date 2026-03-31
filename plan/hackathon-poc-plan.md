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
- Waterfall: if Digital Euro balance insufficient, top up from linked bank account
- Translates REST calls from apps into `pacs.008` toward FIPS network

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
1. **P2P payment** — Alice sends €10 to Bob via phone number proxy lookup
2. **QR merchant payment (MVP)** — Alice scans retailer QR code, confirms payment, pays instantly
3. **RTP merchant payment (stretch)** — Alice receives incoming payment request notification, approves
4. **Digital Euro balance** — Alice's wallet shows split view: bank balance + Digital Euro balance
5. **Waterfall** — Alice pays €60 (more than her €50 DE balance), waterfall tops up from bank

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

### Flow C — Digital Euro Waterfall

```
Alice's Digital Euro balance: €20
Payment amount: €50

Bank: DE balance (€20) < amount (€50)
Bank: waterfall → debit €30 from bank account, top-up DE balance to €50
Bank: proceed with payment from DE balance
Bank → FIPS: pacs.008 (full €50)
```

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
- [ ] Flow A: P2P payment — Alice → Bob via phone proxy
- [ ] Flow B1: QR merchant payment — Alice scans retailer QR, pays instantly
- [ ] Real-time settlement confirmation on retailer screen (polling)
- [ ] Digital Euro balance displayed separately from bank balance

### Stretch (build only if core flows done before 15:00)
- [ ] Flow B2: Request-to-Pay — retailer sends RTP, Alice approves in app
- [ ] Flow C: Waterfall top-up
- [ ] VoP mismatch warning in UI
- [ ] Transaction history screen
- [ ] Rejection scenario (insufficient funds)
- [ ] Animated 10-second settlement countdown

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
- Offline payments (NFC/SE)
- Multi-bank routing (single bank simulates both sides)
- Sanctions screening
- Production-grade security
