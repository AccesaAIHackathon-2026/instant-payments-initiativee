# Digital Euro Project -- Comprehensive Research

**Research Date:** 2026-03-23
**Status:** Preparation phase (since November 2023)

---

## 1. Project Timeline

### Investigation Phase (October 2021 -- October 2023)
- Explored design options, use cases, and technical feasibility
- Concluded October 2023 with decision to proceed

### Preparation Phase (November 2023 -- present)
- Began 1 November 2023, initially planned for two years (through October 2025), with possible extension
- Key activities:
  - Finalizing the Digital Euro rulebook
  - Selecting providers for platform and infrastructure development
  - Running experiments and testing
  - Engaging with market stakeholders

> **Important:** The preparation phase does NOT pre-commit to issuance. A formal decision can only come after the EU legislative framework is adopted. Earliest possible issuance: **~2028-2029**.

### Governance
- Digital Euro Scheme Rulebook Development Group (RDG)
- Euro Retail Payments Board (ERPB)
- Dedicated Market Advisory Group

---

## 2. EU Legislative Proposal -- COM/2023/369

Adopted 28 June 2023 by the European Commission.

### Key Provisions

| Aspect | Detail |
|--------|--------|
| **Legal tender** | Mandatory acceptance throughout euro area (limited exceptions for micro-enterprises) |
| **Holding limits** | ECB to set; discussions centered on **EUR 3,000 per person** |
| **Universal access** | All euro area residents, businesses, government entities can open accounts |
| **Distribution** | Credit institutions in euro area **obligated** to provide digital euro services |
| **No interest** | Neither positive nor negative -- means of payment, not store of value |
| **Free basic use** | Basic services free for individuals; merchant fees capped below card interchange |
| **Privacy** | Elevated privacy for low-value/offline; online subject to AML/CFT |
| **Offline** | Mandatory offline payment capability with cash-like privacy |
| **Complementary to cash** | Companion regulation (COM/2023/364) ensures cash remains available |

### Legislative Progress
- European Parliament ECON committee produced reports
- Council of the EU debating key issues
- Trilogue negotiations expected; full adoption not before late 2025 at earliest

---

## 3. Technical Architecture

### Two-Tier Model
- **ECB/Eurosystem:** Operates the backend settlement infrastructure (core ledger)
- **Intermediaries (banks/PSPs):** Manage customer relationships, KYC, onboarding, front-end wallets

### Online Payments
- Settlement via Eurosystem infrastructure, building on **TIPS**
- Transactions validated in real time
- ECB acts as backend; intermediaries provide user-facing layer

### Offline Payments
- Value stored locally on device in a **secure element / hardware wallet**
- Transfers peer-to-peer via **NFC, Bluetooth**
- Eurosystem does NOT see individual offline transactions
- Separate offline holding and transaction limits
- Re-synchronization when device reconnects

### Privacy Model (Waterfall / Tiered Privacy)

| Transaction Type | Privacy Level | Intermediary Sees | ECB Sees |
|---|---|---|---|
| **Offline, low-value** | Highest (cash-like) | None (until re-sync) | None (aggregate only) |
| **Online, low-value** | High | Limited (for AML) | Pseudonymized |
| **Online, high-value** | Standard | Full KYC/AML data | Pseudonymized / No personal data |

**Privacy firewall:** ECB has NO access to personal transaction data. Only pseudonymized/aggregated data. Intermediaries hold personal data under GDPR.

### Holding Limits & Waterfall Mechanism
- **Holding limit:** ~EUR 3,000 per person (to be confirmed post-legislation)
- **Waterfall:** If payment pushes holdings above limit, excess flows to linked bank account
- **Reverse waterfall:** If balance insufficient, funds automatically pulled from linked bank account

---

## 4. Role of Intermediaries

### Supervised intermediaries (banks, PSPs, EMIs):
- Customer onboarding, KYC/AML, identity verification
- Provide wallets (apps, cards, banking app integration)
- Process transactions, interface with ECB settlement
- Handle customer support, disputes, fraud prevention

### Compensation model:
- No charge to individuals for basic services
- Revenue from merchant fees (capped below card interchange)
- ECB explored various models to ensure sufficient economic incentive

---

## 5. Digital Euro Wallet Specifications

### Expected Features
- P2P payments
- POS payments (NFC/QR)
- E-commerce payments
- Government-to-person payments
- Waterfall/reverse waterfall mechanism
- Accessibility (disability, elderly, digitally less literate)
- Physical card form factor for users without smartphones

### ECB Procurement (2024) -- Five Workstreams

| # | Workstream | Description |
|---|-----------|-------------|
| 1 | **Front-end app** | Digital euro app and UX components |
| 2 | **Offline component** | Secure element integration |
| 3 | **Alias lookup** | Directory service |
| 4 | **Fraud & risk** | Fraud and risk management module |
| 5 | **Settlement integration** | TIPS-based backend integration |

### Prototype Partners (Investigation Phase)

| Partner | Use Case |
|---------|----------|
| **Amazon** | E-commerce online payments |
| **CaixaBank** | P2P payments |
| **Worldline** | POS terminal integration |
| **EPI** | Front-end wallet prototype |
| **Nexi** | Offline payments using secure elements |

---

## 6. Comparison with Other CBDC Projects

### vs. China e-CNY

| Aspect | Digital Euro | e-CNY |
|--------|-------------|-------|
| Status | Preparation phase | Live pilots since 2020 |
| Privacy | Strong; ECB no personal data; GDPR | "Controllable anonymity" -- PBoC has backdoor |
| Holding limits | ~EUR 3,000 | Tiered wallet system |
| Offline | Secure hardware element | SIM-card-based (limited) |
| Programmability | Conditional payments, not programmable money | Smart contracts supported |
| Scale | 350M+ potential users | 1.4B potential; ~260M wallets by late 2023 |

### vs. Other CBDCs
- **Bahamas (Sand Dollar):** First national CBDC (Oct 2020), very small scale, low adoption
- **Nigeria (eNaira):** Launched 2021, very low adoption (~0.5%), cautionary tale
- **Sweden (e-krona):** Investigation phase, closest to digital euro in privacy emphasis
- **UK (Digital Pound):** Design phase, similar two-tier model, holding limit discussed 10-20K GBP
- **India (Digital Rupee):** Wholesale and retail pilots since 2022

**Key differentiator:** Digital Euro's emphasis on privacy-by-design and GDPR compliance is significantly stronger than most other CBDCs globally.

---

## 7. Investigation Phase Results (2021-2023)

### Market Research
- Citizens prioritized **privacy as #1** desired feature
- Broad support for easy-to-use, universally accepted payment method
- Merchants concerned about mandatory acceptance and cost

### Technical Findings
- Offline payments via secure elements are **technically feasible** but require interoperability standards
- Settlement via **TIPS is viable** for real-time processing
- Waterfall mechanism works but needs careful UX design
- Sub-5-second end-to-end transaction time is **achievable**

---

## 8. Integration with Existing Infrastructure

### TIPS (TARGET Instant Payment Settlement)
- Primary infrastructure for digital euro online settlement
- Already processes SEPA Instant 24/7/365
- New "digital euro settlement" capability to be added
- Leverages existing infrastructure, reducing cost

### SEPA
- Digital euro complements (not replaces) SEPA schemes
- Additional pan-euro payment rail alongside SCT, SCT Inst, SDD
- Waterfall mechanism triggers SCT Inst for auto-funding

### EPI/Wero
- Natural synergy; EPI/Wero could be a distribution channel
- ECB states digital euro should be "scheme-agnostic" and interoperable

---

## 9. Programmability

### ECB Position
- **Programmable payments:** SUPPORTED -- conditional payments triggered by predefined conditions
- **Programmable money:** NOT SUPPORTED -- money cannot have embedded restrictions

### Features
- Payment automation and conditional logic via intermediary APIs
- Use cases: IoT payments, pay-per-use, automated government disbursements
- "Reserved settlement" -- funds earmarked pending fulfillment of conditions
- NO smart contracts on the ledger itself

---

## 10. Offline Payment Capabilities

### Technical Approach
- **Secure Element (SE):** Tamper-resistant hardware in device (smartphone SE, SIM-based, or card)
- **NFC:** Primary proximity protocol for POS and P2P
- **Bluetooth Low Energy:** Alternative for slightly longer range
- **QR Codes:** Fallback for devices without NFC

### Payment Flowl
1. Pre-load digital euro from online balance to offline SE ("loading")
2. Devices communicate via NFC/BLE at POS or P2P
3. Value cryptographically transferred between SEs
4. No network connectivity required during transaction
5. Sync with online ledger when device reconnects

### Security
- Separate offline holding limits (lower than online)
- Cumulative offline transaction limits before mandatory re-sync
- Tamper-resistant hardware prevents double-spending
- Cryptographic verification performed locally

### Challenges
- Fragmentation of SE ecosystems across device manufacturers
- Apple restricted NFC/SE access (EU DMA pressure changing this)
- Cost of deploying and certifying SEs at scale

---

## Key Sources

- ECB Digital Euro: https://www.ecb.europa.eu/paym/digital_euro/html/index.en.html
- Commission proposal: https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:52023PC0369
- ECB "A stocktake on the digital euro" report series
- ECB "Report on the digital euro" (October 2020)
- ECB "Progress on the investigation phase" reports (2022, 2023)
- Regulation (EU) 2024/886 on instant payments: https://eur-lex.europa.eu/eli/reg/2024/886
