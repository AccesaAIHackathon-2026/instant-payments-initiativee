# CLAUDE.md — Instant Payments Initiative

## Project Overview

This project builds a POC for a **SEPA Instant Payments (FIPS)** solution with **Digital Euro wallet** support.
It consists of four components under `poc/`, backed by research in `research/` and a plan in `plan/`.

The immediate goal is a **one-day hackathon POC** demonstrating an end-to-end instant payment flow.
The longer-term goal is a productizable platform for tier-2/3 banks and merchants across the EU.

---

## Repository Structure

```
research/          # Domain research — do not modify unless updating research findings
  OVERVIEW.md      # Start here for domain context
  regulation/      # EU Regulation 2024/886, FIPS, SCT Inst deadlines
  digital-euro/    # ECB Digital Euro project, prototype partners, timeline
  technical-specs/ # ISO 20022, PSD3, NFC, FIDO2, EUDI Wallet, Berlin Group
  implementations/ # EPI/Wero, GNU Taler, Mojaloop, BIS projects

plan/
  hackathon-poc-plan.md  # Full hackathon plan: flows, schedule, risks, tech decisions

poc/
  fips-simulator/   # Simulated TIPS/SCT Inst clearing network (Kotlin + Spring Boot)
  bank-simulator/   # Simulated PSP/bank: accounts, proxy lookup, VoP, SCA (Kotlin + Spring Boot)
  retail-web-app/   # Merchant POS web app: QR generation, RTP, settlement confirmation (React)
  mobile-client/    # Consumer wallet: balances, P2P send, QR scan, SCA (React web or Kotlin Android)
```

---

## Tech Stack

| Component | Stack | Notes |
|-----------|-------|-------|
| `fips-simulator` | Java 21 + Spring Boot 3.x | REST service, in-memory store |
| `bank-simulator` | Java 21 + Spring Boot 3.x | REST service, in-memory store |
| `retail-web-app` | React (TypeScript) | Web only, runs in browser |
| `mobile-client` | Android + Kotlin | Native Android app, targets API 26+ |

**Java style:** Use Java 21 features — records for DTOs, sealed classes for status types, pattern matching. Prefer constructor injection in Spring. No Lombok — records cover most of it. No XML config.

**Spring Boot:** Spring Boot 3.x. Use `@RestController` + `@Service` layering. All state in-memory via `ConcurrentHashMap`. No JPA/DB.

**Android (Kotlin):** MVVM with `ViewModel` + `StateFlow`. Use Retrofit2 + OkHttp for API calls. Jetpack Compose for UI. ML Kit Barcode Scanning for QR. Navigation Component for screen routing. Target API 26+ (Android 8.0).

---

## Domain Concepts — Read Before Coding

### Payment Flow
```
User App → Bank Simulator → FIPS Simulator → Bank Simulator → Retailer App
           (payer PSP)      (routing/settle)  (payee PSP)
```

### Key Message Types (simplified as JSON for POC, ISO 20022 in production)
- `pacs.008` — the actual payment instruction (debit payer, credit beneficiary)
- `pacs.002` — payment status report (ACSC = settled, RJCT = rejected)
- `pain.013` — Request-to-Pay (merchant → customer)

### Transaction Status Lifecycle
`RCVD → ACSP → ACSC` (happy path) or `RCVD → RJCT` (rejection)

### UETR
Every transaction has a **UETR (Unique End-to-end Transaction Reference)** — a UUID v4. This is the primary transaction identifier across all components. Always generate it at payment initiation in the bank simulator.

### Proxy Lookup
Users pay via phone number or email, not IBAN. The bank simulator resolves aliases to IBANs. Never expose raw IBANs in the user-facing app unless confirmed via VoP.

### VoP (Verification of Payee)
Before executing a payment, the bank verifies the IBAN-to-name match. Return `MATCH`, `CLOSE_MATCH`, or `NO_MATCH`. The UI must show a warning on `CLOSE_MATCH` and block on `NO_MATCH`.

### SCA Stub
For the POC, any PIN `1234` is accepted. SCA is modeled as a two-step flow: initiate payment → receive challenge token → confirm with PIN → payment proceeds.

### Digital Euro Balance
Each account has two balances: `bankBalance` and `digitalEuroBalance`. These are separate fields, not separate accounts. The waterfall logic: if `digitalEuroBalance < amount`, top up from `bankBalance` first.

---

## API Contract (Bank Simulator)

All inter-component communication goes through the bank simulator. The FIPS simulator is not called directly by the apps.

```
POST   /bank/pay                          # Initiate payment
POST   /bank/sca                          # Confirm SCA challenge
GET    /bank/accounts/{iban}              # Account + balances
GET    /bank/proxy?alias={phone|email}    # Resolve alias → IBAN + name
GET    /bank/vop?iban={}&name={}          # Verification of Payee
GET    /bank/transactions/{iban}          # Transaction history (retailer polls this for settlement in Flow B1)

# Stretch — Request-to-Pay (Flow B2 only)
POST   /bank/request-to-pay              # Retailer sends RTP to customer by alias
GET    /bank/incoming-rtp/{iban}         # Alice polls for pending RTPs (2s interval)
GET    /bank/rtp-status/{rtpId}          # Retailer polls for RTP settlement (2s interval)
```

FIPS simulator internal endpoints (called only by bank simulator):
```
POST   /fips/submit                       # Submit pacs.008
GET    /fips/status/{uetr}               # Poll status
GET    /fips/transactions                 # Admin view
```

**Flow B1 (QR / MVP):** Alice scans QR → calls `POST /bank/pay` directly. Retailer polls `GET /bank/transactions/{iban}` every 2s to detect the incoming credit.

**Flow B2 (RTP / Stretch):** Retailer calls `POST /bank/request-to-pay`. Alice polls `GET /bank/incoming-rtp/{iban}`. Retailer polls `GET /bank/rtp-status/{rtpId}`. No WebSocket needed for either.

---

## Pre-seeded Test Accounts

| Name | IBAN | Phone | Bank Balance | Digital Euro |
|------|------|-------|-------------|--------------|
| Alice Consumer | DE89370400440532013001 | +49111000001 | €1,000.00 | €50.00 |
| Bob Consumer | DE89370400440532013002 | +49111000002 | €500.00 | €20.00 |
| Retail Store GmbH | DE89370400440532013099 | — | €0.00 | €0.00 |

---

## POC Scope Boundaries

### In scope
- P2P payment: Alice → Bob via phone number proxy
- Merchant QR / RTP payment: retailer → Alice → settled
- Digital Euro balance displayed separately from bank balance
- VoP warning on name mismatch
- Basic transaction history

### Out of scope — do not implement
- Real ISO 20022 XML (use simplified JSON)
- Real OAuth2, eIDAS certificates, or TLS mutual auth
- Offline NFC payments
- Multi-bank routing (single bank instance handles both sides)
- Sanctions screening
- Persistent storage (in-memory only)
- Waterfall top-up (stretch goal only — build last if time permits)

---

## CORS

Both backend services must allow all origins in the POC configuration. Add `@CrossOrigin` or a global CORS config bean. Do not block this with security config during the hackathon.

---

## Running Locally

Target ports:
- `fips-simulator`: **8081**
- `bank-simulator`: **8080**
- `retail-web-app`: **3001**
- `mobile-client`: Android emulator or physical device — points to `http://10.0.2.2:8080` (emulator localhost) or the host machine IP on a physical device

---

## Key Reference Documents

- Full hackathon plan (flows, risks, schedule): `plan/hackathon-poc-plan.md`
- Domain overview: `research/OVERVIEW.md`
- ISO 20022 message structures: `research/technical-specs/technical-standards.md`
- Regulatory context: `research/regulation/eu-instant-payments-regulation.md`
