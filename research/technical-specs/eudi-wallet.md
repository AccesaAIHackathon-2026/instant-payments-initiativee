# EUDI Wallet -- EU Digital Identity for Payment Wallet Integration

**Research Date:** 2026-03-24
**Relevance:** KYC onboarding, Strong Customer Authentication (SCA), identity verification

---

## 1. What is the EUDI Wallet?

The **European Digital Identity Wallet** is mandated by **Regulation (EU) 2024/1183** (eIDAS 2.0), published 30 April 2024, in force since 20 May 2024.

It is an **identity wallet** (not a payment wallet) -- a mobile app that stores and presents digital identity credentials. Our payment wallet integrates with it as a **Relying Party** to verify user identity.

### What It Mandates
- Every EU Member State **must** offer at least one EUDI Wallet to citizens/residents
- Free of charge for natural persons
- Relying parties in regulated sectors (banking, telecom, healthcare) **must accept** it for identification
- Must support both online (remote) and offline (proximity) use cases
- Very Large Online Platforms (VLOPs) must accept it for login

### Timeline

| Milestone | Date |
|-----------|------|
| Regulation in force | 20 May 2024 |
| Implementing acts adoption | Rolling, started late 2024 |
| Member States must provide wallets | ~Q4 2026 to mid-2027 (24 months after implementing acts) |
| Relying parties must accept wallets | Same deadline |

---

## 2. What the Wallet Holds

### PID (Person Identification Data)
The core identity credential -- like a "digital ID card":
- **Issued by:** PID Provider (designated/authorized by Member State)
- **Mandatory attributes:** family name, given name, date of birth, unique persistent identifier
- **Optional:** nationality, address, gender, place of birth
- **Issued in both** SD-JWT-VC and mdoc formats (dual format requirement)

### EAA (Electronic Attestation of Attributes)
Additional credentials from any attestation provider:
- Diploma, professional license, age-over-18 attestation, health insurance card, driving license
- Lower assurance than QEAA

### QEAA (Qualified Electronic Attestation of Attributes)
Same as EAA but issued by a **Qualified Trust Service Provider (QTSP)**:
- Highest legal weight (equivalent to paper attestation)
- Subject to conformity assessment and supervision
- Critical for high-assurance use cases like bank onboarding

---

## 3. Architecture (ARF)

The Architecture Reference Framework is published at:
- **GitHub:** https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework
- Latest version: **ARF v1.5** (living document)

### Core Components

```
+------------------+      OpenID4VCI      +------------------+
|  PID Provider    | ------------------> |                  |
| (Member State)   |                      |   EUDI Wallet    |
+------------------+                      |   (User's Phone) |
                                          |                  |
+------------------+      OpenID4VCI      |  Stores:         |
| QEAA Provider   | ------------------> |  - PID            |
| (QTSP)          |                      |  - EAA/QEAA      |
+------------------+                      |  - QES keys       |
                                          +--------+---------+
                                                   |
                                          OpenID4VP / ISO 18013-5
                                                   |
                                          +--------v---------+
                                          | Relying Party    |
                                          | (OUR PAYMENT     |
                                          |  WALLET BACKEND) |
                                          +------------------+
```

### Roles

| Role | Who | What They Do |
|------|-----|-------------|
| **Wallet Provider** | Member State or authorized entity | Develops/deploys wallet app, issues Wallet Instance Attestations |
| **PID Provider** | Designated by Member State | Issues PID credentials after authenticating user via national eID |
| **Attestation Provider** | Public or private entity | Issues EAA/QEAA credentials |
| **Relying Party** | **Us (payment wallet)** | Requests and verifies credentials for KYC/SCA |

### Trust Model

- **Trusted Lists:** Each Member State maintains lists of Wallet Providers, PID Providers, QTSPs, registered Relying Parties
- **Relying Party Registration:** RPs must hold access certificates specifying what attributes they can request and their legal basis
- **Issuer Trust:** Credentials are cryptographically signed; RP verifies against trusted list
- **No phone-home:** Issuer is NOT contacted during presentation (local verification only)

### Wallet Instance Attestation (WIA)
- Cryptographic attestation from Wallet Provider proving the wallet app is genuine and unmodified
- Short-lived JWT signed by Wallet Provider
- Presented during credential issuance (so issuers know the wallet is legitimate)
- Relies on device integrity checks (Android Play Integrity / Apple App Attest)

---

## 4. Technical Protocols

### OpenID4VC Family

The EUDI Wallet uses protocols from the OpenID Foundation, profiled via **HAIP** (High Assurance Interoperability Profile).

#### OpenID4VCI -- Credential Issuance (Issuer -> Wallet)

How a PID Provider or QEAA Provider issues credentials to the wallet:

1. Wallet discovers issuer metadata (`.well-known/openid-credential-issuer`)
2. User authenticates to the issuer (e.g., via national eID)
3. Issuer returns authorization code
4. Wallet exchanges code for access token
5. Wallet sends credential request with proof of key possession
6. Issuer returns signed credential (SD-JWT-VC and/or mdoc)
7. Wallet stores credential locally

Supports batch issuance (both formats in one flow) and deferred issuance.

#### OpenID4VP -- Credential Presentation (Wallet -> Relying Party)

**This is the protocol we implement.** How our payment wallet requests identity from the user:

1. We generate an authorization request with a **presentation definition** (what attributes we need)
2. Request encoded as URI (deep link or QR code)
3. Wallet receives request, shows user what's being asked and by whom
4. User consents, authenticates (biometric/PIN)
5. Wallet constructs VP Token with only the consented attributes (selective disclosure)
6. Wallet POSTs VP Token to our **response endpoint** (`direct_post` mode)
7. We verify the presentation (signature, issuer trust, revocation)

#### SIOPv2 -- Self-Issued OpenID Provider
- Wallet acts as its own OpenID Provider
- Combined with OpenID4VP: wallet presents `id_token` + `vp_token`
- For most KYC flows, OpenID4VP alone is sufficient

### ISO 18013-5 -- Proximity Presentation (In-Person)

For in-person identity verification (e.g., at a branch or agent):

1. Verifier device displays QR code or initiates NFC handshake
2. Wallet scans/taps, establishing secure BLE/NFC channel (ECDH key agreement)
3. Verifier sends mdoc request (CBOR-encoded)
4. User consents in wallet
5. Wallet sends mdoc response with only consented data elements
6. Verifier validates issuer signature + device signature

---

## 5. Credential Formats

### SD-JWT-VC (for Remote/Online Flows)

Structure: `<Issuer-signed JWT> ~ <Disclosure 1> ~ <Disclosure 2> ~ ... ~ <Key Binding JWT>`

- JWT body contains `_sd` arrays with SHA-256 hashes of disclosures
- Each disclosure: `[random_salt, attribute_name, attribute_value]` (Base64url-encoded)
- At presentation, wallet includes only disclosures for consented attributes
- Key Binding JWT added at presentation time (prevents replay, binds to nonce)

```
Content-Type: application/vc+sd-jwt
Header: { "typ": "vc+sd-jwt", "alg": "ES256" }
Body: {
  "iss": "https://pid-provider.example.eu",
  "iat": 1709222400,
  "exp": 1740758400,
  "vct": "eu.europa.ec.eudi.pid.1",
  "cnf": { "jwk": { ... holder's public key ... } },
  "_sd": [ "hash1...", "hash2...", ... ]
}
```

### mdoc (ISO 18013-5, for Proximity Flows)

- CBOR-encoded binary format (more compact than JWT)
- MSO (Mobile Security Object) contains digests of all data elements
- Data organized into namespaces (e.g., `eu.europa.ec.eudi.pid.1`)
- Device signature added at presentation time

### Comparison

| Feature | SD-JWT-VC | mdoc |
|---------|-----------|------|
| Encoding | JSON/JWT (Base64url) | CBOR (binary) |
| Best for | Web/remote flows | Proximity/in-person |
| Size | Larger | More compact |
| Selective disclosure | Via hashed disclosures | Via separate data elements + MSO |

### Cryptographic Requirements
- **Mandatory:** ES256 (ECDSA with P-256 + SHA-256)
- **Optional:** ES384, ES512
- **NOT supported:** RSA (explicitly excluded)
- **Key storage:** Must be hardware-backed (Android StrongBox/TEE Keystore, iOS Secure Enclave)

---

## 6. Integration with Our Payment Wallet

### We Are a Relying Party

Our payment wallet backend requests PID from the user's EUDI Wallet for KYC. We do NOT issue credentials -- we verify them.

### KYC Onboarding Flow

```
User                    Our Payment App          Our Backend           EUDI Wallet
  |                          |                       |                      |
  | 1. "Verify identity"     |                       |                      |
  |------------------------->|                       |                      |
  |                          | 2. Request auth URI    |                      |
  |                          |---------------------->|                      |
  |                          |    (presentation_def)  |                      |
  |                          |<----------------------|                      |
  |                          |    (openid4vp://...)   |                      |
  |                          |                       |                      |
  |                          | 3. Open deep link / show QR                  |
  |                          |--------------------------------------------->|
  |                          |                       |                      |
  |                          |                       |    4. "Payment App X |
  |                          |                       |    requests: name,   |
  |                          |                       |    DOB, nationality" |
  |                          |                       |                      |
  | 5. User consents + biometric auth                |<---------------------|
  |                          |                       |                      |
  |                          |                       | 6. VP Token (POST)   |
  |                          |                       |<---------------------|
  |                          |                       |                      |
  |                          |                       | 7. Verify signature, |
  |                          |                       |    extract attributes|
  |                          | 8. KYC complete        |                      |
  |                          |<----------------------|                      |
  | 9. Account ready         |                       |                      |
  |<-------------------------|                       |                      |
```

### Presentation Definition (What We Request for KYC)

```json
{
  "id": "payment-wallet-kyc",
  "input_descriptors": [
    {
      "id": "eu-pid",
      "format": {
        "vc+sd-jwt": { "alg": ["ES256"] }
      },
      "constraints": {
        "fields": [
          { "path": ["$.family_name"] },
          { "path": ["$.given_name"] },
          { "path": ["$.birth_date"] },
          { "path": ["$.nationality"] },
          { "path": ["$.resident_address"] }
        ]
      }
    }
  ]
}
```

### Same-Device vs. Cross-Device

| Flow | When | How |
|------|------|-----|
| **Same-device** | User onboarding in our mobile app | Deep link (`eudi-openid4vp://...`) opens EUDI Wallet, redirects back |
| **Cross-device** | User on our website (desktop) | QR code displayed, user scans with phone, backend receives VP Token |
| **Proximity** | In-person at branch/agent | ISO 18013-5 via QR/NFC + BLE, mdoc format |

### SCA Under PSD3

The EUDI Wallet can serve as an SCA authentication factor:
- **Possession:** Wallet instance bound to device keys
- **Inherence:** Biometric unlock
- **Knowledge:** PIN/password
- The wallet's qualified electronic signature can **sign payment transactions**

---

## 7. Implementation Guide

### Step 1: Register as a Relying Party
- Obtain a Relying Party Access Certificate from a trusted CA
- Register with relevant Member State(s) or EU-level registry
- Certificate specifies what attributes we can request and legal basis

### Step 2: Implement OpenID4VP Verifier Backend

Use the EU reference implementation as a starting point:
- **`eudi-srv-web-verifier-endpoint-23220-4-kt`** -- complete Kotlin/Spring Boot verifier service

Key endpoints to implement:
1. **Create authorization request** -- generate presentation definition, nonce, response_uri
2. **Response endpoint (`direct_post`)** -- receive VP Token from wallet
3. **Verification logic** -- validate signatures, check issuer trust, extract attributes

### Step 3: Support Both Credential Formats

| Format | Library | Purpose |
|--------|---------|---------|
| SD-JWT-VC | `eudi-lib-jvm-sdjwt-kt` | Parse, verify, extract disclosed claims |
| mdoc | `eudi-lib-jvm-mdoc-cbor` | Parse CBOR, verify MSO signatures |
| Presentation Exchange | `eudi-lib-jvm-presentation-exchange-kt` | Build presentation definitions |
| OpenID4VP | `eudi-lib-jvm-openid4vp-kt` | Verifier-side protocol handling |

Alternative libraries:
- `com.nimbusds:nimbus-jose-jwt` -- widely used, has SD-JWT support
- `com.authlete:sd-jwt` -- Authlete's SD-JWT for Java
- `walt.id` -- Kotlin SSI libraries, EUDI-compatible

### Step 4: Verification Logic

When we receive a VP Token:
1. **Parse** the SD-JWT-VC or mdoc credential
2. **Verify issuer signature** against PID Provider's public key (from trusted list)
3. **Verify Key Binding JWT** (check nonce, audience, freshness) -- prevents replay
4. **Extract selectively disclosed attributes** (hash each disclosure, match against `_sd` digests)
5. **Check revocation status** (Token Status Lists)
6. **Map attributes to KYC data** in our system

---

## 8. EU Reference Implementation Repositories

All at: https://github.com/eu-digital-identity-wallet

### Libraries We Need (JVM/Kotlin)

| Repository | What It Does |
|-----------|-------------|
| `eudi-lib-jvm-sdjwt-kt` | SD-JWT parsing and verification |
| `eudi-lib-jvm-openid4vci-kt` | OpenID4VCI client (if we ever issue credentials) |
| `eudi-lib-jvm-openid4vp-kt` | OpenID4VP verifier-side library |
| `eudi-lib-jvm-siop-openid4vp-kt` | SIOPv2 + OpenID4VP wallet-side (reference) |
| `eudi-lib-jvm-presentation-exchange-kt` | Presentation Exchange definitions |
| `eudi-lib-jvm-mdoc-cbor` | mdoc/CBOR handling |

### Reference Services

| Repository | What It Is |
|-----------|-----------|
| `eudi-srv-web-verifier-endpoint-23220-4-kt` | **Verifier backend (Kotlin/Spring Boot) -- our starting point** |
| `eudi-srv-web-issuer-eudiw-py` | Issuer service (Python) -- reference only |

### Reference Wallet Apps

| Repository | Platform |
|-----------|----------|
| `eudi-app-android-wallet-ui` | Android (Kotlin) |
| `eudi-app-ios-wallet-ui` | iOS (Swift) |
| `eudi-lib-android-wallet-core` | Android core library |
| `eudi-lib-ios-wallet-core` | iOS core library |
| `eudi-lib-android-iso18013-data-transfer` | Android proximity transfer |
| `eudi-lib-ios-iso18013-data-transfer` | iOS proximity transfer |

---

## 9. Privacy Features

### Selective Disclosure
- User shares only the attributes they consent to
- Each attribute independently disclosable (not all-or-nothing)
- Example: share name + DOB for KYC, but not address

### Unlinkability
- **Batch issuance:** PID Provider can issue multiple PID instances (same data, different signatures) so each presentation uses a different credential
- Prevents relying parties from colluding to track user across services

### User Consent
- **Explicit per presentation:** Wallet shows exactly what's requested, by whom, for what purpose
- **User can decline** any or all attributes
- **Active authentication required** (biometric/PIN) for every presentation
- **Audit log:** Wallet maintains local log of all presentations

### Data Minimization
- RPs must request minimum necessary attributes (enforced by access certificate)
- **Age-over attestations:** Request `age_over_18` boolean instead of full birth date
- **Pseudonymous auth:** Return visits can use SIOPv2 without re-requesting PID

---

## 10. Relevant Large Scale Pilots

### NOBID (Nordic-Baltic) -- Most Relevant to Us
- **Focus:** Payment/transaction services
- Tests wallet-based payment authorization and merchant interaction
- Website: https://nobidconsortium.com/

### POTENTIAL (France/Germany-led, 148 participants)
- Tests eIDAS-based KYC for bank account opening and payment authorization
- Website: https://potential-id.eu/

### EWC (Sweden-led, Nordic/Baltic)
- Tests wallet-based payment authorization and merchant KYC
- Website: https://eudiwalletconsortium.org/

### Key Findings from Pilots (as of early 2026)
- Dual format (SD-JWT-VC + mdoc) adds complexity but is manageable
- Same-device flows require careful deep-link handling per platform
- User consent UX is critical -- must be clear, non-technical
- Trust list infrastructure still maturing

---

## Key References

| Resource | URL |
|----------|-----|
| Regulation (EU) 2024/1183 | https://eur-lex.europa.eu/eli/reg/2024/1183 |
| ARF GitHub | https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework |
| OpenID4VCI spec | https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html |
| OpenID4VP spec | https://openid.net/specs/openid-4-verifiable-presentations-1_0.html |
| SD-JWT spec | https://datatracker.ietf.org/doc/draft-ietf-oauth-selective-disclosure-jwt/ |
| SD-JWT-VC spec | https://datatracker.ietf.org/doc/draft-ietf-oauth-sd-jwt-vc/ |
| HAIP profile | https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html |
| Reference verifier (Kotlin) | https://github.com/eu-digital-identity-wallet/eudi-srv-web-verifier-endpoint-23220-4-kt |
| EU reference repos | https://github.com/eu-digital-identity-wallet |
