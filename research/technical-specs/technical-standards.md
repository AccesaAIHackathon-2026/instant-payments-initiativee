# Technical Standards & Specifications

**Research Date:** 2026-03-23

---

## 1. ISO 20022 Messaging Standard

ISO 20022 is the universal financial messaging standard mandated for all SEPA payment schemes and the Digital Euro.

### Key Message Types for Instant Payments

| Message ID | Name | Purpose |
|-----------|------|---------|
| `pacs.008.001.xx` | FIToFICustomerCreditTransfer | The actual payment instruction between PSPs |
| `pacs.002.001.xx` | FIToFIPaymentStatusReport | Confirmation or rejection of a payment |
| `pacs.028.001.xx` | FIToFIPaymentStatusRequest | Status inquiry for a pending payment |
| `pacs.004.001.xx` | PaymentReturn | Return of funds |
| `pain.001.001.xx` | CustomerCreditTransferInitiation | Customer-to-bank payment order |
| `pain.002.001.xx` | CustomerPaymentStatusReport | Status report back to customer |
| `camt.056.001.xx` | FIToFIPaymentCancellationRequest | Recall/cancellation request |
| `camt.029.001.xx` | ResolutionOfInvestigation | Response to recall request |

### XML Structure
- All messages are XML-based with published XSD schemas
- Schemas available at: https://www.iso20022.org/catalogue-messages
- EPC publishes **Implementation Guidelines (IGs)** specifying field usage for SCT Inst
- Character encoding: UTF-8 with extended Latin character set

### Key Fields in pacs.008 (Instant Payment)

```xml
<!-- Simplified structure -->
<FIToFICstmrCdtTrf>
  <GrpHdr>
    <MsgId/>           <!-- Unique message identifier -->
    <CreDtTm/>         <!-- Creation date/time -->
    <NbOfTxs/>         <!-- Number of transactions -->
    <SttlmInf>
      <SttlmMtd/>      <!-- Settlement method (CLRG for clearing) -->
      <ClrSys/>        <!-- Clearing system (TIPS, RT1) -->
    </SttlmInf>
  </GrpHdr>
  <CdtTrfTxInf>
    <PmtId>
      <InstrId/>       <!-- Instruction ID -->
      <EndToEndId/>    <!-- End-to-end ID (visible to both parties) -->
      <TxId/>          <!-- Transaction ID -->
      <UETR/>          <!-- Unique End-to-end Transaction Reference (UUID) -->
    </PmtId>
    <IntrBkSttlmAmt Ccy="EUR"/>  <!-- Amount -->
    <ChrgBr/>          <!-- Charge bearer (SLEV for SCT Inst) -->
    <Dbtr/>            <!-- Debtor (payer) info -->
    <DbtrAcct>
      <Id><IBAN/></Id>
    </DbtrAcct>
    <DbtrAgt>
      <FinInstnId><BICFI/></FinInstnId>  <!-- Debtor's bank BIC -->
    </DbtrAgt>
    <CdtrAgt>
      <FinInstnId><BICFI/></FinInstnId>  <!-- Creditor's bank BIC -->
    </CdtrAgt>
    <Cdtr/>            <!-- Creditor (payee) info -->
    <CdtrAcct>
      <Id><IBAN/></Id>
    </CdtrAcct>
    <RmtInf/>          <!-- Remittance info (payment reference) -->
  </CdtTrfTxInf>
</FIToFICstmrCdtTrf>
```

---

## 2. SEPA Instant Credit Transfer (SCT Inst) Technical Specs

### Interoperability Framework

| Parameter | Specification |
|-----------|--------------|
| **Message standard** | ISO 20022 XML |
| **Maximum execution time** | 10 seconds (interbank) |
| **Maximum amount** | EUR 100,000 |
| **Availability** | 24/7/365, no scheduled downtime |
| **Settlement finality** | Immediate, irrevocable |
| **Unique ID** | UETR (UUID v4) for end-to-end tracking |
| **BIC directory** | EPC maintains participant directory |
| **Character set** | UTF-8 Latin (extended) |
| **Recall** | Supported via camt.056 (beneficiary PSP response not guaranteed) |

### Transaction Flow (Interbank)

```
Payer -> Payer's PSP -> CSM (TIPS/RT1) -> Beneficiary's PSP -> Beneficiary
  |          |              |                    |                  |
  | initiate | pacs.008     | route+settle       | pacs.008         | notify
  |          |              |                    |                  |
  |          | <------------|--------------------| pacs.002         |
  |  confirm |              |                    | (accept/reject)  |
  |<---------|              |                    |                  |

  Total: <= 10 seconds end-to-end
```

---

## 3. TIPS (TARGET Instant Payment Settlement)

### Architecture
- Operated by the **Eurosystem** (ECB + national central banks)
- Settles in **central bank money** (risk-free)
- Single shared platform across euro area
- Built as an extension of the TARGET Services framework

### Technical Details

| Aspect | Detail |
|--------|--------|
| **Protocol** | ISO 20022 XML messages over ESMIG (Eurosystem Single Market Infrastructure Gateway) |
| **Access** | Direct (via dedicated cash account) or indirect (via direct participant) |
| **Reachability** | All euro-area PSPs must be reachable via TIPS (ECB mandate) |
| **Processing** | Real-time, individual transaction settlement |
| **Capacity** | Designed for >43,000 transactions per second peak |
| **Availability** | 24/7/365, target 99.99%+ uptime |
| **Settlement** | Instant, final, irrevocable in central bank money |
| **Liquidity** | Pre-funded via dedicated cash accounts (DCAs) |

### Access Models
1. **Direct participant** -- own DCA at central bank, connects directly via ESMIG
2. **Reachable party** -- reached through a direct participant (most PSPs)
3. **Instructing party** -- submits on behalf of a reachable party
4. **CSM as participant** -- RT1 and national ACHs can connect to TIPS

### TIPS and Digital Euro
- TIPS will serve as the settlement backbone for the Digital Euro
- A new "digital euro settlement" module is being developed within TIPS
- Both SCT Inst and Digital Euro will share the same infrastructure

---

## 4. RT1 (EBA CLEARING)

- Operated by **EBA CLEARING** (private sector, owned by ~50 banks)
- Settles in **commercial bank money** with end-of-day settlement in central bank money
- Processes SCT Inst transactions alongside TIPS
- Connected to TIPS for interoperability (since 2022)
- Pan-European reach via direct participants and reachable parties

---

## 5. PSD2 / PSD3 / PSR (Payment Services)

### PSD2 (Current -- Directive 2015/2366)
- **Strong Customer Authentication (SCA)** mandatory for electronic payments
- **Open Banking** -- banks must provide APIs for Third Party Providers (TPPs)
- Two TPP types:
  - **AISP** (Account Information Service Provider) -- read-only account access
  - **PISP** (Payment Initiation Service Provider) -- initiate payments on behalf of user
- API access is a right; banks cannot block it

### PSD3 + PSR (Upcoming -- proposed June 2023)
The Commission proposed to split PSD2 into:
- **PSD3** (Directive) -- authorization and licensing of PSPs
- **PSR** (Payment Services Regulation) -- directly applicable rules

Key changes:
- **Enhanced SCA** -- more flexibility, risk-based approach
- **Open Banking improvements** -- dedicated API performance standards, dashboard for consent management
- **IBAN/name verification** -- aligned with Regulation 2024/886 VoP
- **Fraud liability** -- stronger consumer protection, PSP liability for certain fraud types
- **Non-bank PSPs** -- clearer rules for e-money and payment institutions
- **Digital Euro integration** -- PSR framework applies to Digital Euro intermediaries

### SCA Technical Requirements
- **Two of three factors:**
  - Knowledge (password, PIN)
  - Possession (phone, card, hardware token)
  - Inherence (fingerprint, face, voice)
- **Dynamic linking** -- for remote payments, SCA must be linked to amount and payee
- **Exemptions:** low-value (<EUR 30, cumulative <EUR 100), trusted beneficiaries, recurring payments, merchant-initiated transactions
- **Session timeout:** 5 minutes for SCA challenge

---

## 6. Berlin Group NextGenPSD2 API Framework

### Overview
- Industry standard API specification for PSD2/PSD3 compliance
- Adopted by most European banks for Open Banking APIs
- RESTful APIs with JSON payloads (some XML support)

### Key API Groups

| API | Purpose |
|-----|---------|
| **Account Information (AIS)** | Read balances, transactions, account details |
| **Payment Initiation (PIS)** | Initiate SCT, SCT Inst, cross-border payments |
| **Confirmation of Funds (CoF)** | Check if payer has sufficient funds |
| **Signing Baskets** | Bundle multiple payment initiations |

### Payment Initiation Flow (PIS)

```
TPP (PISP) -> ASPSP (Bank)
1. POST /v1/payments/instant-sepa-credit-transfers
   -> Returns paymentId + SCA redirect/decoupled link
2. PSU authenticates with SCA at ASPSP
3. GET /v1/payments/{paymentId}/status
   -> Returns transaction status (ACSC, RJCT, etc.)
```

### Authentication Approaches
- **Redirect** -- user redirected to bank's authentication page
- **Decoupled** -- user authenticates in bank's own app (push notification)
- **Embedded** -- credentials entered directly in TPP's app (less common, more restricted)

### Key Specification Links
- NextGenPSD2 specification: https://www.berlin-group.org/nextgenpsd2-downloads

---

## 7. EMVCo Specifications (Mobile/Contactless Payments)

### Relevant Standards

| Standard | Purpose |
|----------|---------|
| **EMV Contactless** | NFC payment terminal communication (based on ISO 14443) |
| **EMV QR Code** | QR code-based payment initiation |
| **EMV 3-D Secure (3DS)** | Online payment authentication (SCA for e-commerce) |
| **EMV Tokenization** | Replace card/account data with tokens for security |
| **EMV SRC (Secure Remote Commerce)** | Standardized online checkout experience |

### For Digital Euro Wallet
- POS payments would follow EMV Contactless protocols at the physical layer
- QR codes would follow EMV QR code specification for merchant-presented codes
- 3DS could be used for e-commerce Digital Euro payments requiring SCA

---

## 8. EPC QR Code Standard

### SEPA Credit Transfer QR Code (EPC069-12)
- Standardized QR code format for initiating SEPA credit transfers
- Contains: BIC, IBAN, amount, payee name, remittance info

### QR Code Payload Format
```
BCD                          -- Service tag
002                          -- Version
1                            -- Encoding (UTF-8)
SCT                          -- Function (SEPA Credit Transfer)
BICOFBANK                    -- BIC of beneficiary bank
Payee Name                   -- Name
DE89370400440532013000       -- IBAN
EUR12.50                     -- Amount
                             -- Purpose
                             -- Remittance (structured)
Payment reference text       -- Remittance (unstructured)
                             -- Information
```

### For Instant Payments / Digital Euro
- Same format can trigger SCT Inst if payer's app supports it
- Wero/EPI uses QR codes for P2P and POS initiation
- Digital Euro wallet would scan/generate these QR codes

---

## 9. NFC Payment Standards

### Physical Layer
- **ISO 14443** -- Proximity cards (Type A and B), used by contactless payment cards/phones
- **ISO 18092 (NFCIP-1)** -- NFC peer-to-peer communication
- **ISO 7816** -- Smart card commands (APDU format)

### For Mobile Payments
- **Android HCE (Host Card Emulation)** -- Phone emulates a contactless card
  - Android 4.4+, no secure element required for HCE
  - Uses ISO 14443-4 (ISO-DEP) protocol
  - Application selection via AID (Application Identifier)
- **Apple Pay NFC** -- Uses Secure Element, restricted access
  - Apple Pay uses EMV contactless specifications
  - NFC access opening up under EU DMA pressure
- **Offline Digital Euro** -- Would use NFC for device-to-device value transfer
  - Likely ISO 18092 (peer-to-peer) or custom protocol over ISO 14443

### APDU Communication

```
Terminal -> Phone:  SELECT AID (00 A4 04 00 [length] [AID])
Phone -> Terminal:  Response with application data
Terminal -> Phone:  Application-specific commands
Phone -> Terminal:  Transaction authorization
```

---

## 10. Strong Customer Authentication (SCA) -- Technical

### Implementation Patterns

| Pattern | Description | Use Case |
|---------|-------------|----------|
| **App-based PIN + biometric** | User opens wallet app, authenticates with fingerprint/face + confirms | Standard mobile payment |
| **Push notification** | Bank sends push, user confirms in banking app | Decoupled SCA |
| **Hardware token** | Physical device generates OTP | High-security / legacy |
| **FIDO2/WebAuthn** | Passwordless authentication using device biometrics | Modern web/app auth |
| **Device binding** | Device itself is the possession factor | Combined with biometric |

### FIDO2/WebAuthn for Payments
- **Passkeys** -- device-bound or synced credentials
- Combines possession (device) + inherence (biometric) in single gesture
- Supported by Android, iOS, all major browsers
- Eliminates phishing risk (origin-bound credentials)
- Recommended for Digital Euro wallet SCA implementation
- Spec: https://www.w3.org/TR/webauthn/

### Dynamic Linking Requirements
- For remote payments, SCA must bind to:
  - **Amount** of the transaction
  - **Payee** identity (name or IBAN)
- Ensures user confirms the exact payment, not a modified one
- Implementation: transaction details shown on SCA screen, signed by device

---

## 11. eIDAS 2.0 and EU Digital Identity Wallet

### Overview
- **eIDAS 2.0** (Regulation 2024/1183) establishes the **EU Digital Identity (EUDI) Wallet**
- Every EU Member State must offer at least one EUDI Wallet to citizens by 2026
- The wallet holds **electronic identification** and **verifiable credentials** (diplomas, licenses, etc.)

### Relevance to Payment Wallets

| Aspect | Impact on Digital Euro Wallet |
|--------|------------------------------|
| **Identity verification** | EUDI Wallet can provide KYC for Digital Euro onboarding |
| **Strong authentication** | EUDI Wallet SCA can satisfy PSD3 requirements |
| **Age/attribute verification** | Enable conditional payments (e.g., age-restricted purchases) |
| **Cross-border** | Single digital identity across all EU states |
| **Technical standards** | ARF (Architecture Reference Framework) defines protocols |

### Architecture Reference Framework (ARF)
- Defines the technical architecture for EUDI Wallets
- Based on **W3C Verifiable Credentials** and **ISO 18013-5 (mDL)** for proximity presentation
- Communication: **OpenID4VC** (OpenID for Verifiable Credentials) protocols
  - **OpenID4VCI** -- credential issuance
  - **OpenID4VP** -- credential presentation/verification
  - **SIOPv2** -- self-issued OpenID provider
- Transport: BLE, NFC, QR code + online
- Privacy: selective disclosure, zero-knowledge proofs possible

### Integration Scenario
```
Digital Euro Wallet <-> EUDI Wallet
  |
  | 1. User opens Digital Euro wallet
  | 2. Wallet requests identity verification via EUDI Wallet
  | 3. EUDI Wallet presents verified identity credential (selective disclosure)
  | 4. Digital Euro wallet completes KYC using verified attributes
  | 5. User can now transact with Digital Euro
```

---

## 12. SEPA Request to Pay (SRTP)

### Overview
- EPC-managed scheme enabling payees to request payment from payers
- Complements SCT Inst -- the "request" triggers an instant payment
- Key enabler for POS and e-commerce instant payments

### Technical Flow

```
Merchant -> Merchant's PSP -> RTP Service Provider -> Payer's PSP -> Payer
   |             |                    |                    |            |
   | create RTP  | RTP message        | route              | notify     |
   |             | (pain.013)         |                    |            |
   |             |                    |                    |   approve  |
   |             |                    |                    |<-----------|
   |             |                    |                    |            |
   |             | <----- SCT Inst (pacs.008) ------------|            |
   | settled     |                    |                    |            |
```

### Message Types
- `pain.013` -- CreditorPaymentActivationRequest (the "request to pay")
- `pain.014` -- CreditorPaymentActivationRequestStatusReport
- Links to `pain.001` / `pacs.008` for the actual payment execution

### Use Cases
- **E-commerce checkout** -- merchant sends RTP, customer approves in app
- **POS payment** -- QR code contains RTP, customer scans and approves
- **Bill payment** -- utility sends RTP, customer pays from app
- **Subscriptions** -- recurring RTP at defined intervals

---

## 13. Digital Euro Technical Standards (Known/Expected)

### Published/In Development
- **Digital Euro Scheme Rulebook** (draft published mid-2024, under consultation)
- Settlement built on TIPS ISO 20022 messaging
- Expected to define:
  - Transaction message formats (ISO 20022 based)
  - Alias/proxy lookup protocol
  - Waterfall/reverse waterfall message flows
  - Offline transaction protocol (secure element communication)
  - VoP integration
  - Fraud/risk data exchange formats

### Expected API Model (based on Project Rosalind blueprint)
- RESTful APIs between intermediaries and ECB settlement layer
- API categories:
  - **Provisioning** -- wallet creation, KYC handoff, limit management
  - **Payments** -- P2P, P2M, request-to-pay, bulk
  - **Information** -- balance, transaction history, limits
  - **Programmability** -- conditional payments, locks, earmarks
  - **Funding** -- waterfall/reverse waterfall operations

### No public SDK yet -- expected after legislation adoption and rulebook finalization.

---

## 14. Summary: Technology Stack for a Digital Euro / Instant Payments Wallet

### Core Standards

| Layer | Standard/Technology |
|-------|-------------------|
| **Messaging** | ISO 20022 (pacs.008, pacs.002, pain.001, pain.013, camt.056) |
| **Settlement** | TIPS / RT1 (via ESMIG or PSP gateway) |
| **Open Banking** | Berlin Group NextGenPSD2 (PIS, AIS, CoF) |
| **Authentication** | FIDO2/WebAuthn, PSD3 SCA, eIDAS 2.0 |
| **Identity** | EUDI Wallet (OpenID4VC, ISO 18013-5) |
| **NFC (POS)** | ISO 14443 (EMV Contactless), Android HCE |
| **NFC (Offline P2P)** | ISO 18092 (NFCIP-1) |
| **QR Codes** | EPC069-12 (SEPA QR), EMV QR |
| **E-commerce auth** | EMV 3-D Secure 2.x |
| **Fraud** | EPC VoP scheme, sanctions screening, risk scoring |
| **Request to Pay** | EPC SRTP scheme (pain.013/014) |

### Key References
- ISO 20022 catalog: https://www.iso20022.org/catalogue-messages
- EPC SCT Inst rulebook: https://www.europeanpaymentscouncil.eu/
- Berlin Group NextGenPSD2: https://www.berlin-group.org/
- EMVCo specifications: https://www.emvco.com/specifications/
- W3C WebAuthn: https://www.w3.org/TR/webauthn/
- eIDAS 2.0 ARF: https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework
- FIDO Alliance: https://fidoalliance.org/specifications/
- ECB TIPS: https://www.ecb.europa.eu/paym/target/tips/