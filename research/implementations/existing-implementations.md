# Existing Implementations & Open Source

**Research Date:** 2026-03-23

---

## 1. Open-Source SEPA / Instant Payment Libraries (JVM)

### Prowide (formerly WIFE)
- **prowide-core** (`prowide/prowide-core`) -- Java, Apache 2.0
  - Parsing, creation, validation of SWIFT messages (MT/MX)
  - Well-established, used in production by banks
- **prowide-iso20022** (`prowide/prowide-iso20022`) -- Java, Apache 2.0 (core)
  - ISO 20022 (MX) message Java model classes
  - Open-source covers subset; commercial "Prowide Integrator" covers full catalog

### ISO 20022 Java Bindings
- JAXB-based approach: compile official ISO 20022 XSD schemas into Java classes
- Official schemas published at iso20022.org
- Works with JAXB, Jackson XML, any XML binding tool

### jPOS
- `jpos/jPOS` -- Java, AGPL
- Financial transaction processing framework (primarily ISO 8583 / card payments)
- Extensible, used as payment switch backbone
- **jPOS-EE** -- Extended edition with database, crypto modules

### Apache Fineract
- Open-source core banking platform (Apache Foundation)
- Not SEPA-specific but provides payment processing, accounts, transaction management
- Used in emerging markets for mobile money / microfinance

### Key ISO 20022 Messages for Instant Payments

| Message | Purpose |
|---------|---------|
| `pacs.008.001.xx` | FIToFICustomerCreditTransfer (payment instruction) |
| `pacs.002.001.xx` | FIToFIPaymentStatusReport (confirmation/rejection) |
| `pacs.028.001.xx` | FIToFIPaymentStatusRequest |
| `pain.001.001.xx` | CustomerCreditTransferInitiation (customer-to-bank) |
| `pain.002.001.xx` | CustomerPaymentStatusReport |
| `camt.056.001.xx` | FIToFIPaymentCancellationRequest |

---

## 2. GNU Taler (Privacy-Preserving Payments)
- Website: taler.net, Git: `git.taler.net`
- Privacy-preserving electronic payment system
- **Key design:** payer anonymity + payee transparency (for tax compliance)
- Uses **blind signatures** (Chaum-style) -- cryptographic anonymity for payers
- **NOT a blockchain** -- centralized "exchange" with conventional database
- Components:
  - **Exchange** (C) -- issues and redeems digital coins (could be central bank)
  - **Wallet** (TypeScript/JS) -- client-side, Android + browser extension
  - **Merchant backend** (Python) -- accepts Taler payments
- Privacy model closely matches ECB's Digital Euro goals
- Limitation: requires online verification (no true offline double-spend prevention)
- Licensed under GNU AGPL v3+

---

## 3. ECB Digital Euro Prototype Partners

| Partner | Use Case | Focus |
|---------|----------|-------|
| **Amazon** | E-commerce | Online checkout integration |
| **CaixaBank** | P2P | Person-to-person transfers |
| **Worldline** | POS | In-store merchant terminal payments |
| **EPI** | P2P (initiator) | Payer-initiated transfers |
| **Nexi** | Offline POS | Offline payments via secure elements/NFC |

### Key Findings
- Digital Euro can integrate into existing payment infrastructure
- Offline capability feasible with secure elements and NFC
- Settlement via TIPS viable for real-time processing
- Waterfall mechanism demonstrated but needs careful UX

---

## 4. EPI (European Payments Initiative) & Wero

### Background
- Founded 2020 by consortium of major European banks
- Goal: unified European payment solution, reduce Visa/Mastercard dependence
- Core members from France, Germany, Belgium, Netherlands

### Wero Wallet (launched mid-2024)
- Built on **SEPA Instant (SCT Inst)** as underlying rail
- **Account-to-account (A2A)** -- no card network
- Features:
  - P2P payments (launched first in France, Germany, Belgium)
  - E-commerce (rolling out 2025-2026)
  - POS (planned for later phases)
- Technical approach:
  - Request-to-pay + proxy lookup (phone/email to IBAN)
  - Mobile-first, integrated into banking apps
  - QR codes for in-store and P2P
- Absorbed **iDEAL** (Netherlands), **Bancontact Payconiq** (Belgium)
- Potential distribution channel for Digital Euro

---

## 5. National Instant Payment Solutions

### iDEAL 2.0 (Netherlands)
- Dominant online payment (~70% e-commerce)
- Built on SCT Inst (vs. redirect-based iDEAL 1.0)
- Key changes: instant settlement, standardized API (Berlin Group NextGenPSD2), in-app payments, QR codes, recurring payments, IBAN-name check (SurePay)
- Integrated into EPI/Wero ecosystem

### Bizum (Spain)
- Spain's dominant P2P platform, launched 2016
- ~70 banks, **28M+ users** (in 47M population)
- Phone number as proxy for IBAN
- Integrated into banking apps (not standalone)
- Expanding to e-commerce and in-store QR
- Demonstrates bank-integrated instant payments can achieve massive adoption

---

## 6. BIS Innovation Hub Projects

### Project Rosalind (BIS + Bank of England, 2023)
- Designed **33 API endpoints** for retail CBDC
- API categories: Provision, Payment, Information, Programmability
- Two-tier model: central bank core + PSP frontends
- Privacy by design: central bank sees minimal personal data
- Programmable payments, not programmable money
- Full report: bis.org

### Project Aurum (BIS + HKMA, 2022)
- Full-stack two-tier retail CBDC prototype
- Conventional database for retail layer (not DLT) -- chosen for performance
- Two CBDC types: intermediated (direct claim) + backing-funded
- Privacy via separation: central bank sees wholesale, not retail

---

## 7. Swedish e-Krona -- Key Learnings

The Riksbank's e-Krona project (2020-2024) produced valuable design insights:
1. **Centralized ledger works** -- DLT offered no clear advantage over centralized architecture for CBDC
2. **Offline is hard** -- requires secure hardware, limits, risk management
3. **Privacy is the hardest design choice**
4. **Interoperability with existing payment systems** is essential

---

## 8. Mojaloop
- `mojaloop/mojaloop` -- Open-source instant payment platform
- Linux Foundation, originally Gates Foundation funded
- **Node.js/JavaScript** microservices
- Components: Central Ledger, Account Lookup, Quoting, Transfer, Settlement
- Deployed in Africa/Asia: Mowali (West Africa), Tanzania, Myanmar
- Proxy lookup architecture relevant to P2P flows

---

## 9. Mobile Wallet SDKs & Frameworks

### Cross-Platform
- **Kotlin Multiplatform (KMP)** -- shared logic across Android/iOS
- **Flutter** (Dart) -- used by Nubank, some Google Pay markets
- **React Native** -- many fintech apps

### Security Components
- Android Keystore / iOS Keychain -- hardware-backed key storage
- Android StrongBox / iOS Secure Enclave -- HSM on device
- GlobalPlatform SE APIs -- SIM/embedded SE for offline CBDC
- Android TEE (ARM TrustZone)

### Payment SDKs
- Google Pay API / Apple Pay SDK -- NFC tap-to-pay
- EMVCo 3DS SDK -- SCA in e-commerce
- FIDO2/WebAuthn -- biometric authentication

### Relevant JVM Libraries
- **Bouncy Castle** -- cryptographic library (Java/Kotlin)
- **Tink** (Google) -- high-level crypto library
- **SQLCipher** -- encrypted SQLite for secure local storage
- **OkHttp / Ktor** -- HTTP clients
- **Protocol Buffers / gRPC** -- efficient serialization and RPC

### NFC
- **Android HCE** -- phone acts as contactless payment card
- **Apple Core NFC** -- more limited but allows NFC tag reading

---

## 10. Key Takeaways

1. **Two-tier model is consensus** -- central bank infra (TIPS) + private wallets (PSPs)
2. **SCT Inst is the foundation** -- EPI/Wero, iDEAL 2.0, Digital Euro all build on SEPA Instant
3. **ISO 20022 is non-negotiable** -- prowide-core/prowide-iso20022 most mature Java libraries
4. **Offline payments are hardest** -- require hardware security (SEs, TEEs)
5. **Privacy requires deliberate architecture** -- GNU Taler (blind signatures) vs. Project Rosalind (separation of concerns)
6. **Proxy lookup is essential** -- users need simple identifiers (phone, email), not IBANs
7. **Centralized architecture preferred** -- e-Krona and Project Aurum both concluded centralized DB outperforms DLT for retail payments

### Wallet Reference Architecture
```
+------------------+
|   Mobile App UI  |  (Native iOS/Android or KMP/Flutter)
+------------------+
|   Wallet SDK     |  (Key management, signing, offline store)
+------------------+
|   API Client     |  (REST/gRPC to PSP backend)
+------------------+
        |
+------------------+
|   PSP Backend    |  (KYC, AML, accounts, proxy lookup)
+------------------+
        |
+------------------+
| TIPS / Digital   |  (Settlement, finality)
| Euro Settlement  |
+------------------+
```