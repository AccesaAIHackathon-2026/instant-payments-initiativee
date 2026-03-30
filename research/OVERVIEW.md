# Research Overview -- Instant Payments & Digital Euro Initiative

**Last Updated:** 2026-03-23

---

## Project Goal

Build a compatible **Digital Euro wallet** and **instant payments solution** (mobile-first) that supports:
- SEPA Instant Credit Transfers (SCT Inst / FIPS)
- The upcoming Digital Euro (ECB CBDC)
- Consumer-to-consumer (P2P) and consumer-to-merchant payments

---

## Research Summary

### 1. Regulatory Landscape

**EU Instant Payments Regulation (2024/886)** mandates universal, affordable instant payments:
- All euro-area PSPs must receive (Jan 2025) and send (Oct 2025) instant payments
- Pricing parity with standard transfers (instant = same cost or free)
- Verification of Payee (VoP) mandatory by Oct 2025
- 10-second execution, 24/7/365 availability
- Shifts instant payments from bank-only to universal retail/consumer use

**Digital Euro Legislation (COM/2023/369)** under negotiation:
- Legal tender status, mandatory merchant acceptance
- Free basic services for individuals
- Privacy-by-design with tiered model
- Mandatory offline capability
- EUR 3,000 holding limit (preliminary)

**PSD3/PSR** replacing PSD2 -- enhanced SCA, improved Open Banking APIs.

**eIDAS 2.0** -- EUDI Wallet for digital identity, directly relevant to KYC for payment wallets.

-> Full details: [regulation/eu-instant-payments-regulation.md](regulation/eu-instant-payments-regulation.md)

### 2. Digital Euro Project Status

- **Preparation phase** since Nov 2023 (ECB), earliest issuance ~2028-2029
- Two-tier model: ECB settlement (TIPS-based) + PSP frontends (wallets)
- Online + offline payments (offline via secure elements, NFC)
- Waterfall mechanism linking to bank accounts
- Programmable payments (conditions) but NOT programmable money
- No public APIs/SDKs yet
- Prototype partners: Amazon (e-commerce), CaixaBank (P2P), Worldline (POS), EPI (wallet), Nexi (offline)

-> Full details: [digital-euro/digital-euro-project.md](digital-euro/digital-euro-project.md)

### 3. Technical Standards

| Layer | Standard |
|-------|----------|
| Messaging | ISO 20022 (pacs.008, pacs.002, pain.001/013, camt.056) |
| Settlement | TIPS / RT1 |
| Open Banking | Berlin Group NextGenPSD2 |
| Authentication | FIDO2/WebAuthn + PSD3 SCA |
| Identity | EUDI Wallet (OpenID4VC) |
| NFC | ISO 14443 (POS), ISO 18092 (P2P), Android HCE |
| QR Codes | EPC069-12, EMV QR |
| Request to Pay | EPC SRTP (pain.013/014) |

-> Full details: [technical-specs/technical-standards.md](technical-specs/technical-standards.md)
-> EUDI Wallet deep-dive: [technical-specs/eudi-wallet.md](technical-specs/eudi-wallet.md)

### 4. Existing Implementations & Open Source

**Key platforms:**
- **GNU Taler** -- privacy-preserving payment system (blind signatures, centralized, AGPL)
- **Mojaloop** -- open-source instant payment platform (Node.js)
- **prowide-core/iso20022** -- ISO 20022 Java libraries (most mature)

**Market solutions:**
- **EPI/Wero** -- pan-European wallet on SCT Inst (launched mid-2024)
- **Bizum** (Spain) -- 28M users, phone-number P2P, bank-integrated
- **iDEAL 2.0** (Netherlands) -- migrating to SCT Inst, joining Wero

**BIS research:**
- **Project Rosalind** -- 33 CBDC API endpoints (reference blueprint)
- **Project Aurum** -- full-stack two-tier CBDC prototype

-> Full details: [implementations/existing-implementations.md](implementations/existing-implementations.md)

---

## Key Architectural Decisions for Our Wallet

Based on research findings, the consensus architecture is:

```
+-------------------+
|  Mobile App (UI)  |  Kotlin Multiplatform / Native
+-------------------+
|  Wallet SDK       |  Key mgmt, signing, offline store, SCA
+-------------------+
|  API Client       |  REST/gRPC to PSP backend
+-------------------+
         |
+-------------------+
|  PSP Backend      |  KYC (EUDI), AML, accounts, proxy lookup, VoP
+-------------------+
         |
    +----+----+
    |         |
+-------+ +----------+
| TIPS  | | Digital  |
| (SCT  | | Euro     |
| Inst) | | Module   |
+-------+ +----------+
```

### Critical Design Choices to Make
1. **Kotlin Multiplatform vs. Flutter vs. Native** for mobile
2. **ISO 20022 library** -- prowide-core (Java) or generate from XSD
3. **Offline approach** -- Secure Element integration (Android HCE, Apple NFC access)
4. **Authentication** -- FIDO2/WebAuthn + biometric (recommended)
5. **Identity integration** -- EUDI Wallet for KYC
6. **Privacy model** -- GNU Taler's blind signatures vs. account-based with privacy firewall
7. **Backend technology** -- JVM (Kotlin/Java) aligns with ISO 20022 tooling

### Next Steps
1. Deep-dive into EPC SCT Inst rulebook (obtain latest version)
2. Review Digital Euro Scheme Rulebook draft
3. Prototype ISO 20022 message handling (pacs.008/002)
4. Evaluate Kotlin Multiplatform for wallet SDK
5. Research EUDI Wallet integration (OpenID4VC)
6. Design offline payment architecture (secure element options)
7. Set up project skeleton with build system
