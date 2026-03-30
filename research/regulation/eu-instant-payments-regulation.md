# EU Instant Payments Regulation & FIPS

**Research Date:** 2026-03-23
**Status:** Active regulation, implementation in progress

---

## 1. Regulation (EU) 2024/886 -- Overview

Published in the Official Journal on **19 March 2024**, entered into force **8 April 2024**. Amends:
- Regulation (EU) No 260/2012 (SEPA Regulation)
- Regulation (EU) 2021/1230 (Cross-Border Payments Regulation)

Core goal: make instant euro payments universally available, affordable, and safe across the EU/EEA.

### Key Provisions

| Provision | Detail |
|-----------|--------|
| **Universal send & receive** | All PSPs offering standard SEPA credit transfers in euro must also offer instant credit transfers |
| **Pricing parity** | Charges for instant transfers must not exceed charges for standard (non-instant) transfers |
| **10-second execution** | Funds credited to beneficiary's PSP within 10 seconds |
| **24/7/365 availability** | No maintenance windows, weekends, or holidays as excuses |
| **Verification of Payee (VoP)** | Payee-name-verification before execution to combat fraud |
| **Sanctions screening** | PSPs must screen customer base against EU sanctions lists (at least daily), not per-transaction |
| **Maximum amount** | Defers to SCT Inst scheme limit (currently EUR 100,000) |

---

## 2. Implementation Deadlines

### Euro-area PSPs

| Milestone | Deadline |
|-----------|----------|
| **Receive** instant payments | **9 January 2025** |
| **Send** instant payments | **9 October 2025** |
| **Verification of Payee** (send + receive) | **9 October 2025** |

### Non-euro-area EU PSPs (Sweden, Poland, Hungary, Czech Republic, Romania, Bulgaria, etc.)

| Milestone | Deadline |
|-----------|----------|
| **Receive** instant payments | **9 January 2027** |
| **Send** instant payments | **9 July 2027** |
| **Verification of Payee** | **9 July 2027** |

---

## 3. What "FIPS" Means

**FIPS = Full Instant Payments Settlement** (also: Full Instant Payment System/Service)

Refers to the end-state vision where:
- **All** PSPs in SEPA can send and receive instant payments
- Settlement is **immediate and final** (irrevocable)
- The entire chain completes within **10 seconds**
- Infrastructure operates **24/7/365**
- Instant is the **default** rail, not a premium service

> **Note:** FIPS is an industry/market term, not formally defined in the regulation or by the EPC. The acronym also stands for "Federal Information Processing Standards" in US IT security (unrelated).

---

## 4. The Shift From Bank-Only to Retail & Consumer

Historically SCT Inst was optional, premium-priced, and primarily interbank/P2P. Regulation 2024/886 changes this:

1. **Mandatory participation** -- universal for all retail customers
2. **Pricing parity** -- if standard SCT is free, instant must also be free
3. **Consumer protection via VoP** -- safer for individuals
4. **New retail use cases**:
   - Point-of-sale payments (competing with cards)
   - E-commerce checkout (Request-to-Pay + SCT Inst)
   - Bill payments and recurring payments
   - Government disbursements
   - Merchant payouts from platforms
5. **EPI/Wero** leverages SCT Inst as the underlying rail for consumer-to-merchant payments

---

## 5. Verification of Payee (VoP)

### What It Does
- Before executing a transfer, the payer's PSP sends the beneficiary's **IBAN and name** to the beneficiary's PSP
- Beneficiary's PSP returns **match / close match / no match**
- Payer is informed **before confirming** the payment

### Purpose
- **Fraud prevention** (APP fraud)
- **Misdirected payments** (typos in IBANs)

### Technical Details
- EPC developed a **VoP scheme rulebook** to standardize this
- Uses **ISO 20022** messaging
- Applies to both instant and non-instant credit transfers
- Close matches require alert to payer with option to proceed

---

## 6. Sanctions Screening (Paradigm Shift)

### Regulatory Approach (Article 5d)
- PSPs verify **at least once per calendar day** whether customers are subject to EU sanctions
- **Customer-list screening** replaces real-time transaction screening as primary method
- If customer found sanctioned: immediately **freeze account** and reject all transactions
- PSPs **may** additionally perform transaction-level screening (not mandated as primary)

### Implications
- Dramatically reduces per-transaction processing time
- Reduces false positives
- Requires robust customer onboarding and ongoing due diligence

---

## 7. SCT Inst Scheme Rulebook

| Parameter | Value |
|-----------|-------|
| **Managed by** | European Payments Council (EPC) |
| **Live since** | 21 November 2017 |
| **Max execution time** | 10 seconds (interbank) |
| **Maximum amount** | EUR 100,000 |
| **Availability** | 24/7/365 |
| **Currency** | Euro |
| **Geographic scope** | 36 SEPA countries |
| **Message standard** | ISO 20022 (pacs.008, pacs.002, pacs.004) |
| **Settlement finality** | Immediate and irrevocable |
| **Character set** | UTF-8 (extended Latin) |

### Settlement Infrastructure
Two main CSMs:
1. **TIPS (TARGET Instant Payment Settlement)** -- Eurosystem/ECB, settles in central bank money
2. **RT1** -- EBA CLEARING, settles in commercial bank money with end-of-day central bank settlement

---

## 8. EPC Role

The EPC is the **scheme manager** for SEPA payment schemes:
- Develops and maintains SCT, SCT Inst, SDD Core, SDD B2B rulebooks
- Manages annual change cycles with stakeholder consultation
- Publishes implementation guidelines and clarification papers
- Developed VoP scheme and SEPA Request to Pay (SRTP) scheme
- Does NOT operate infrastructure -- sets the rules only

### Key Deliverables
- SCT Inst Rulebook
- SCT Inst Inter-PSP Implementation Guidelines
- VoP Scheme Rulebook
- SRTP Scheme Rulebook
- Clarification Papers

---

## 9. Adoption Rates (as of late 2024 / early 2025)

### Overall SEPA
- ~15-20% of all SEPA credit transfers were instant (by volume)
- Up from ~11-12% in 2023 and ~5% in 2021
- ~60-65% PSP reachability, expected to rise to near 100% by mandate deadlines
- ~1-1.5 million transactions per day across SEPA

### Country Variation

| Country | SCT Inst Share | Notes |
|---------|---------------|-------|
| Finland | ~50-60% | Exceptionally high, early adopter |
| Estonia | ~40-50% | Strong Baltic adoption |
| Lithuania | ~35-45% | Strong Baltic adoption |
| Spain | ~15-20% | Iberpay infrastructure |
| Austria | ~15-20% | Solid adoption |
| Germany | ~12-15% | Large market, growing |
| Portugal | ~10-15% | MB Way ecosystem |
| Italy | ~10-12% | Growing from low base |
| France | ~8-10% | STET, accelerating |
| Belgium | ~10-12% | Growing |
| Netherlands | ~5-8% | Historically strong with iDEAL |
| Ireland | ~5-8% | Lower adoption |
| Greece | ~3-5% | Still building out |

### Post-Regulation Expectations
- Adoption expected to rise toward 50%+ of credit transfers within 1-2 years of October 2025 mandate
- Instant payments could become the dominant transfer method by 2027-2028

---

## Key Sources

- Regulation (EU) 2024/886: https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32024R0886
- EPC SCT Inst scheme: https://www.europeanpaymentscouncil.eu/what-we-do/sepa-instant-credit-transfer
- EPC VoP scheme: https://www.europeanpaymentscouncil.eu/what-we-do/other-schemes/verification-payee
- ECB TIPS: https://www.ecb.europa.eu/paym/target/tips/html/index.en.html
- ECB payment statistics: https://www.ecb.europa.eu/stats/payment_statistics/
- EBA CLEARING RT1: https://www.ebaclearing.eu/services/rt1/overview/
