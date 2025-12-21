# Phone-Based Mint Architecture with NUT-12 DLEQ Proofs

**Date**: 2025-12-12
**Status**: Conceptual Design
**Related Documents**:
- `gift-card-plan-final.md`
- `NUT-12-IMPLEMENTATION-PLAN.md`
- `voucher-api-specification.md`
- `simple-gift-card-nostr-plan.md`

---

## Executive Summary

This document explores the architectural implications of running Cashu mints on mobile devices, enabled by NUT-12's offline signature verification. With DLEQ proofs, the trust model shifts from "trust the mint's availability" to "trust the mint's public key" - making lightweight, phone-based mints viable for gift card and voucher scenarios.

---

## Table of Contents

1. [Core Insight: NUT-12 Enables Decoupled Validation](#1-core-insight-nut-12-enables-decoupled-validation)
2. [Phone-Based Mint Architecture](#2-phone-based-mint-architecture)
3. [Nostr as Transport Layer](#3-nostr-as-transport-layer)
4. [Gift Card / E-Voucher Integration](#4-gift-card--e-voucher-integration)
5. [Trust Model Analysis](#5-trust-model-analysis)
6. [Technical Feasibility](#6-technical-feasibility)
7. [Implementation Considerations](#7-implementation-considerations)
8. [Use Case Scenarios](#8-use-case-scenarios)
9. [Limitations and Mitigations](#9-limitations-and-mitigations)
10. [Relationship to Existing Plans](#10-relationship-to-existing-plans)

---

## 1. Core Insight: NUT-12 Enables Decoupled Validation

### Before NUT-12: Online Trust Model

```
┌─────────────┐         ┌─────────────┐
│   Wallet    │────────▶│    Mint     │
│  (Client)   │◀────────│  (Server)   │
└─────────────┘         └─────────────┘
      │                       │
      │  "Is this token      │
      │   valid?"            │
      └──────────────────────┘

Trust: Mint must be online and honest for validation
```

**Limitations**:
- Mint must be online for token validation
- Users must trust mint won't issue fake signatures
- Receiving tokens requires either trusting sender or contacting mint
- Mint is single point of failure

### After NUT-12: Cryptographic Trust Model

```
┌─────────────┐                      ┌─────────────┐
│   Wallet    │  Verify DLEQ Proof   │    Mint     │
│  (Client)   │◀ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─│  (Offline)  │
└─────────────┘                      └─────────────┘
      │
      │  Using mint's PUBLIC KEY only:
      │  • Verify signature mathematically
      │  • No network call needed
      │  • Cryptographic proof of authenticity
      ▼
   ✓ Token Valid (Offline Verification)

Trust: Mathematical proof that mint's private key signed the token
```

**Capabilities Unlocked**:
- **Offline verification** using only mint's public keys
- **Trustless P2P transfers** - Carol can verify tokens from Alice without contacting mint
- **Decoupled validation from issuance/redemption**

### The Key Shift

| Operation | Pre-NUT-12 | Post-NUT-12 |
|-----------|------------|-------------|
| **Validation** | Online, requires mint | Offline, public keys only |
| **Issuance** | Online, requires mint | Online, requires mint |
| **Redemption** | Online, requires mint | Online, requires mint |

**Validation is now independent** - opening the door to lightweight mint deployments.

---

## 2. Phone-Based Mint Architecture

### What Makes a Mint?

A Cashu mint fundamentally requires:

| Component | Description | Resource Intensity |
|-----------|-------------|-------------------|
| **Keypair** | Private key (signing), Public key (verification) | Minimal - 32 bytes |
| **State Tracking** | Which tokens are spent | Moderate - depends on volume |
| **Signing Logic** | BDHKE blind signatures | Minimal - EC operations |
| **DLEQ Proofs** | NUT-12 proof generation | Minimal - ~3 EC ops + SHA256 |

**None of this requires heavy infrastructure.** Modern smartphones can handle thousands of EC operations per second.

### Phone Mint Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   MERCHANT'S PHONE                         │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │                    Cashu Mint App                     │ │
│  │                                                       │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐ │ │
│  │  │  Keypair    │  │   Token     │  │    DLEQ      │ │ │
│  │  │  Manager    │  │   Ledger    │  │  Generator   │ │ │
│  │  │             │  │  (SQLite)   │  │              │ │ │
│  │  │ • Ed25519   │  │             │  │ • Generate   │ │ │
│  │  │ • secp256k1 │  │ • Issued    │  │   proofs     │ │ │
│  │  │             │  │ • Spent     │  │ • ~3 EC ops  │ │ │
│  │  └─────────────┘  └─────────────┘  └──────────────┘ │ │
│  │                                                       │ │
│  │  ┌─────────────────────────────────────────────────┐ │ │
│  │  │              Nostr Interface                     │ │ │
│  │  │                                                  │ │ │
│  │  │  • Subscribe to mint requests (encrypted DMs)   │ │ │
│  │  │  • Publish signatures + DLEQ proofs             │ │ │
│  │  │  • Update ledger events (NIP-33)                │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
└────────────────────────────────────────────────────────────┘
                              │
                              │ WebSocket
                              ▼
                    ┌─────────────────┐
                    │   Nostr Relay   │
                    │   (Commodity)   │
                    └─────────────────┘
                              │
            ┌─────────────────┴─────────────────┐
            │                                   │
            ▼                                   ▼
   ┌─────────────────┐                ┌─────────────────┐
   │  Customer App   │                │  Customer App   │
   │                 │                │                 │
   │  • Request mint │                │  • Verify DLEQ  │
   │  • Verify DLEQ  │                │    (OFFLINE!)   │
   │  • Store tokens │                │  • Accept P2P   │
   └─────────────────┘                └─────────────────┘
```

### Resource Requirements

| Resource | Requirement | Phone Capability |
|----------|-------------|------------------|
| **CPU** | EC scalar multiplication | Thousands/sec |
| **Memory** | Keypair + active session | < 10 MB |
| **Storage** | Token ledger | SQLite, grows with usage |
| **Network** | WebSocket to relay | Background connection |
| **Battery** | Push notifications | Standard mobile pattern |

**Conclusion**: A phone-based mint is technically feasible.

---

## 3. Nostr as Transport Layer

### Why Nostr?

Your existing architecture already uses Nostr for:
- Voucher ledger (NIP-33 replaceable events)
- Wallet backups (NIP-17 encrypted DMs)

Extending to mint communication is natural:

| Benefit | Description |
|---------|-------------|
| **No server needed** | Relays are commodity infrastructure |
| **Asynchronous** | Mint doesn't need to be always online |
| **Decentralized** | Multiple relays for redundancy |
| **Identity built-in** | npub identifies the mint |
| **Encryption available** | NIP-44 for private messages |

### Communication Flow

```
                          Nostr Relay
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    │                    ▼
┌─────────────────┐           │          ┌─────────────────┐
│  Customer App   │           │          │  Merchant Phone │
│                 │           │          │     (Mint)      │
│ 1. Create       │           │          │                 │
│    BlindedMsg   │           │          │                 │
│                 │           │          │                 │
│ 2. Send DM ─────┼───────────┼──────────┼───▶ 3. Receive  │
│    (NIP-17)     │           │          │       request   │
│                 │           │          │                 │
│                 │           │          │    4. Sign      │
│                 │           │          │       + DLEQ    │
│                 │           │          │                 │
│ 6. Receive ◀────┼───────────┼──────────┼─── 5. Send DM   │
│    response     │           │          │       (NIP-17)  │
│                 │           │          │                 │
│ 7. Verify DLEQ  │           │          │                 │
│    (OFFLINE)    │           │          │                 │
│                 │           │          │                 │
│ 8. Store token  │           │          │                 │
└─────────────────┘           │          └─────────────────┘
                              │
                              │
                              ▼
                    ┌─────────────────┐
                    │   NIP-33 Event  │
                    │  (Public Ledger)│
                    │                 │
                    │ Voucher status: │
                    │ • issued        │
                    │ • redeemed      │
                    └─────────────────┘
```

### Message Types

**Mint Request (Customer → Mint)**:
```json
{
  "kind": 14,  // NIP-17 private DM
  "pubkey": "<customer_pubkey>",
  "tags": [
    ["p", "<mint_pubkey>"],
    ["subject", "cashu-mint-request"]
  ],
  "content": "<NIP-44 encrypted: {
    \"type\": \"mint\",
    \"quote_id\": \"...\",
    \"blinded_messages\": [...]
  }>"
}
```

**Mint Response (Mint → Customer)**:
```json
{
  "kind": 14,
  "pubkey": "<mint_pubkey>",
  "tags": [
    ["p", "<customer_pubkey>"],
    ["subject", "cashu-mint-response"],
    ["e", "<request_event_id>"]
  ],
  "content": "<NIP-44 encrypted: {
    \"type\": \"mint_response\",
    \"signatures\": [
      {
        \"id\": \"<keyset_id>\",
        \"amount\": 100,
        \"C_\": \"02...\",
        \"dleq\": {
          \"e\": \"...\",
          \"s\": \"...\"
        }
      }
    ]
  }>"
}
```

### Asynchronous Operation

Key advantage: **The mint (phone) doesn't need to be always online**.

```
Timeline:
─────────────────────────────────────────────────────────────────►

Customer                     Relay                      Merchant Phone
    │                          │                              │
    │──── Mint Request ───────▶│                              │
    │                          │     (Phone offline)          │
    │                          │         ...                  │
    │                          │         ...                  │
    │                          │◀──── Phone comes online ─────│
    │                          │                              │
    │                          │──── Deliver Request ────────▶│
    │                          │                              │
    │                          │◀──── Mint Response ──────────│
    │◀── Deliver Response ─────│                              │
    │                          │                              │
    │   Verify DLEQ            │                              │
    │   Store Token            │                              │
```

---

## 4. Gift Card / E-Voucher Integration

### How NUT-12 Enhances Your Voucher Architecture

Your existing plan (`gift-card-plan-final.md`) uses **Model B**:
- Vouchers NOT redeemable at mint
- Only spendable at issuing merchant
- Nostr for ledger and backups

NUT-12 strengthens this with **cryptographic validation**:

```
┌──────────────────────────────────────────────────────────────────┐
│                     GIFT CARD FLOW WITH NUT-12                   │
└──────────────────────────────────────────────────────────────────┘

1. ISSUANCE (Merchant's Phone Mint)
   ┌─────────────────┐
   │ Merchant Phone  │
   │                 │
   │ • Create voucher│
   │ • Sign (BDHKE)  │──────▶ BlindSignature + DLEQ Proof
   │ • Generate DLEQ │
   │ • Publish to    │──────▶ NIP-33 Event (status: issued)
   │   Nostr ledger  │
   └─────────────────┘

2. TRANSFER (Alice → Carol)
   ┌─────────────────┐        Token (with DLEQ)      ┌─────────────────┐
   │     Alice       │─────────────────────────────▶│      Carol      │
   │                 │                              │                 │
   │ Add blinding    │                              │ Verify DLEQ     │
   │ factor to DLEQ  │                              │ using:          │
   │ (for Carol)     │                              │ • Mint pubkey   │
   │                 │                              │ • Blinding factor│
   │                 │                              │ • No mint contact│
   └─────────────────┘                              └─────────────────┘
                                                            │
                                                            │
                                                    ✓ OFFLINE VERIFIED
                                                            │
3. REDEMPTION (Carol → Merchant)                            │
   ┌─────────────────┐                              ┌───────▼─────────┐
   │ Merchant Phone  │◀─────────────────────────────│      Carol      │
   │                 │         Present Token        │                 │
   │ • Verify DLEQ   │                              │                 │
   │ • Check ledger  │                              │                 │
   │ • Mark redeemed │──────▶ NIP-33 (status: redeemed)              │
   │ • Provide goods │                              │                 │
   └─────────────────┘                              └─────────────────┘
```

### NUT-12 Value Add for Vouchers

| Without NUT-12 | With NUT-12 |
|----------------|-------------|
| Carol must trust Alice or check with merchant | Carol verifies cryptographically |
| Merchant must be online for validation | Offline verification possible |
| Forwarding tokens requires trust chain | Each recipient can independently verify |
| Fraud detection reactive | Fraud detection proactive (invalid DLEQ = reject immediately) |

### Integration Points

**VoucherSecret with DLEQ** (extending your existing voucher model):

```java
// When Alice transfers voucher to Carol, she includes blinding factor in DLEQ
public class VoucherProofWithDLEQ {
    private VoucherSecret voucherSecret;
    private Signature unblindedSignature;  // C
    private DLEQProof dleq;  // {e, s, r} - r is blinding factor

    // Carol's verification
    public boolean verifyOffline(ECPoint mintPublicKey) {
        return DLEQUtils.verifyProofWithBlindingFactor(
            dleq.getE(),
            dleq.getS(),
            dleq.getR(),
            voucherSecret.toBytes(),
            unblindedSignature.toECPoint(),
            mintPublicKey
        );
    }
}
```

---

## 5. Trust Model Analysis

### Trust Layers

```
                    TRUST HIERARCHY

┌───────────────────────────────────────────────────────────┐
│                    CRYPTOGRAPHIC TRUST                     │
│                                                            │
│  "I trust the mathematics of elliptic curve cryptography"  │
│                                                            │
│  • DLEQ proof is valid → Mint's private key signed this   │
│  • Cannot forge without private key                        │
│  • Verification is deterministic                           │
└───────────────────────────────────────────────────────────┘
                           │
                           ▼
┌───────────────────────────────────────────────────────────┐
│                      KEY TRUST                             │
│                                                            │
│  "I trust this public key belongs to the merchant"         │
│                                                            │
│  • Nostr npub identifies the mint                          │
│  • Out-of-band verification (website, QR code, etc.)      │
│  • Web of trust possible via Nostr                         │
└───────────────────────────────────────────────────────────┘
                           │
                           ▼
┌───────────────────────────────────────────────────────────┐
│                   REDEMPTION TRUST                         │
│                                                            │
│  "I trust the merchant will honor valid tokens"            │
│                                                            │
│  • Merchant must be online for redemption                  │
│  • Merchant must not double-spend (ledger on Nostr)       │
│  • Merchant reputation/accountability                      │
└───────────────────────────────────────────────────────────┘
```

### What NUT-12 Guarantees

| Guarantee | Description |
|-----------|-------------|
| **Authenticity** | Token was signed by mint's private key |
| **Non-repudiation** | Mint cannot deny signing (DLEQ is proof) |
| **Offline Verification** | No network needed to verify |
| **Forward Security** | Each recipient can independently verify |

### What NUT-12 Does NOT Guarantee

| Not Guaranteed | Why | Mitigation |
|----------------|-----|------------|
| **Redemption** | Mint must be online | Reputation, Nostr ledger |
| **Double-spend prevention** | State tracking required | Nostr ledger (NIP-33) |
| **Key authenticity** | Out of band | Nostr identity, WoT |
| **Value backing** | Trust in issuer | Reputation, escrow |

### Suitable Use Cases

Given this trust model, phone-based mints work well for:

| Use Case | Why It Works |
|----------|--------------|
| **Closed ecosystems** | Single merchant, known parties |
| **Gift cards** | Short lifecycle, single redemption |
| **Loyalty points** | Internal to business |
| **Event tickets** | Single use, known issuer |
| **Small communities** | Trust relationships exist |
| **Family/friends** | Personal accountability |

---

## 6. Technical Feasibility

### Cryptographic Performance

**DLEQ Proof Generation** (single proof):
```
Operations:
1. Random scalar generation:     ~1 μs
2. R1 = r*G (EC multiplication): ~500 μs
3. R2 = r*B' (EC multiplication): ~500 μs
4. SHA256 hash:                   ~1 μs
5. s = r + e*a (scalar ops):      ~1 μs
───────────────────────────────────────
Total:                           ~1 ms per proof
```

**Modern smartphone**: Can generate 1000+ DLEQ proofs per second.

### Storage Estimates

**Token Ledger** (SQLite on phone):

| Volume | Storage | Performance |
|--------|---------|-------------|
| 1,000 tokens | ~100 KB | Instant queries |
| 10,000 tokens | ~1 MB | Sub-second |
| 100,000 tokens | ~10 MB | Indexed queries fast |

For a small merchant, even 10,000 vouchers is generous.

### Battery Impact

**Nostr Connection**:
- WebSocket: Minimal when idle (TCP keepalive)
- Push notifications: Standard mobile pattern
- Background processing: Wake on message

**Estimated Impact**: Similar to a messaging app.

### Network Considerations

**Latency Tolerant**:
- Minting is asynchronous (Nostr DM)
- No real-time requirements
- Store-and-forward model

**Offline Resilience**:
- Verification works offline (DLEQ)
- Ledger events cached locally
- Sync when connection available

---

## 7. Implementation Considerations

### Phone Mint App Components

```
┌─────────────────────────────────────────────────────────────┐
│                    PHONE MINT APP MODULES                   │
└─────────────────────────────────────────────────────────────┘

┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   Key Manager    │     │   Mint Engine    │     │ Ledger Manager   │
│                  │     │                  │     │                  │
│ • Generate keys  │     │ • Sign blinds    │     │ • Track issued   │
│ • Secure storage │     │ • Generate DLEQ  │     │ • Track spent    │
│ • Backup/restore │     │ • Verify proofs  │     │ • SQLite storage │
│ • Multiple       │     │ • Process swaps  │     │ • Nostr sync     │
│   keysets        │     │                  │     │                  │
└──────────────────┘     └──────────────────┘     └──────────────────┘
         │                        │                        │
         └────────────────────────┼────────────────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │     Nostr Interface       │
                    │                           │
                    │ • Subscribe to requests   │
                    │ • Publish responses       │
                    │ • Ledger events (NIP-33)  │
                    │ • Encrypted DMs (NIP-17)  │
                    └───────────────────────────┘
                                  │
                    ┌─────────────▼─────────────┐
                    │    Merchant UI/UX         │
                    │                           │
                    │ • Dashboard (issued/spent)│
                    │ • Redemption interface    │
                    │ • QR code scanning        │
                    │ • Notifications           │
                    └───────────────────────────┘
```

### Security Requirements

| Requirement | Implementation |
|-------------|----------------|
| **Key Protection** | Android Keystore / iOS Secure Enclave |
| **Backup** | Encrypted export, mnemonic derivation |
| **Authentication** | Biometric/PIN to unlock mint |
| **Audit Log** | Local + Nostr events |

### Integration with cashu-lib

The existing `cashu-lib` modules support phone-based deployment:

```
cashu-lib-crypto     → DLEQ proof generation (DLEQUtils)
cashu-lib-common     → Voucher secrets, proofs
cashu-lib-vouchers   → VoucherSecret, VoucherSignatureService
```

**Android/iOS Considerations**:
- Pure Java (cashu-lib) → Works on Android
- Need iOS wrapper or Kotlin Multiplatform for iOS
- BouncyCastle → Available on mobile

---

## 8. Use Case Scenarios

### Scenario 1: Coffee Shop Loyalty Program

```
┌─────────────────────────────────────────────────────────────────┐
│                    COFFEE SHOP LOYALTY                          │
└─────────────────────────────────────────────────────────────────┘

Setup:
• Coffee shop installs mint app on tablet (or phone)
• Generates keypair, publishes to Nostr

Daily Operation:
1. Customer buys 10 coffees → Earns 1 free coffee voucher
2. Shop's tablet mints voucher with DLEQ proof
3. Customer receives via Nostr DM or QR scan
4. Customer verifies DLEQ offline (cryptographic proof)
5. Later: Customer redeems voucher
6. Shop verifies DLEQ + checks not spent
7. Shop updates ledger on Nostr (redeemed)

Benefits:
• No server infrastructure
• Works offline (DLEQ verification)
• Tamper-proof (cryptographic)
• Auditable (Nostr ledger)
```

### Scenario 2: Event Tickets

```
┌─────────────────────────────────────────────────────────────────┐
│                      EVENT TICKETS                              │
└─────────────────────────────────────────────────────────────────┘

Setup:
• Event organizer creates phone mint
• Pre-generates batch of ticket vouchers
• Publishes keyset + event info

Ticket Sales:
1. Customer purchases ticket
2. Receives voucher with DLEQ proof
3. Can verify authenticity immediately (offline)
4. Can gift/transfer to friend
5. Friend verifies DLEQ (no trust in original buyer needed)

Event Day:
6. Present voucher at door
7. Staff scans, verifies DLEQ
8. Marks as redeemed (Nostr update)
9. Prevents re-entry (double-spend check)

Benefits:
• No centralized ticketing system
• Secure transfer between attendees
• Works with spotty venue WiFi
• Fraud resistant
```

### Scenario 3: Family Gift Cards

```
┌─────────────────────────────────────────────────────────────────┐
│                    FAMILY GIFT ECONOMY                          │
└─────────────────────────────────────────────────────────────────┘

Setup:
• Parent runs mint on phone
• Kids have wallet apps

Allowance:
1. Weekly: Parent mints "chore dollars" for kids
2. Each token has DLEQ proof
3. Kids verify (trust but verify!)

Spending:
4. Kid presents token to parent for treat
5. Parent verifies + redeems

Trading:
6. Kids can trade tokens (verified by DLEQ)
7. No need to involve parent for verification

Benefits:
• Teaches digital currency concepts
• Tamper-proof allowance
• Kids learn cryptographic verification
• Fun family economy
```

---

## 9. Limitations and Mitigations

### Limitation 1: Phone Availability for Redemption

**Problem**: Redemption requires mint (phone) to be online.

**Analysis**: This is **not a significant problem** for the pre-paid voucher use case:

1. **Vouchers are for goods/services, not money** - Customers redeem against merchandise, not cash withdrawals
2. **Asynchronous redemption is natural** - Customer presents voucher → merchant provides goods → phone processes redemption when back online
3. **Nostr relay buffers requests** - When phone comes online, it receives queued redemption messages

```
┌─────────────────────────────────────────────────────────────────────────┐
│              ASYNCHRONOUS REDEMPTION FLOW (Phone Offline)               │
└─────────────────────────────────────────────────────────────────────────┘

Customer              Merchant POS           Relay           Merchant Phone
    │                      │                   │                   │
    │── Present Voucher ──▶│                   │                   │
    │                      │                   │                   │
    │                      │ Verify DLEQ       │                   │
    │                      │ (offline OK) ✓    │                   │
    │                      │                   │                   │
    │◀── Goods/Service ────│                   │          (offline)│
    │                      │                   │                   │
    │                      │── Queue Redeem ──▶│                   │
    │                      │   (NIP-17 DM)     │                   │
    │                      │                   │                   │
    │                      │                   │    (comes online) │
    │                      │                   │◀──────────────────│
    │                      │                   │                   │
    │                      │                   │── Deliver Msg ───▶│
    │                      │                   │                   │
    │                      │                   │    Mark redeemed  │
    │                      │                   │◀── Ledger Update ─│
    │                      │                   │   (NIP-33 event)  │
```

**Key Insight**: The merchant can provide goods immediately upon DLEQ verification. The actual "redemption" (marking spent in ledger) happens asynchronously. The cryptographic proof (DLEQ) provides sufficient trust for immediate service.

### Limitation 2: State Sync Across Devices

**Problem**: If merchant has multiple devices, ledger must sync.

**Mitigations**:
| Strategy | Description |
|----------|-------------|
| **Nostr as source of truth** | All devices sync from Nostr |
| **Single mint authority** | Only one device signs |
| **Eventual consistency** | Accept slight delays |

### Limitation 3: Key Loss

**Problem**: Phone lost = private key lost = cannot redeem tokens.

**Primary Mitigation: nsecbunker Integration**

[nsecbunker](https://github.com/kind-0/nsecbunker) provides a robust solution for Nostr key management:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    NSECBUNKER KEY PROTECTION                            │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────┐                          ┌─────────────────┐
│  Merchant Phone │                          │   nsecbunker    │
│     (Mint)      │                          │    (Remote)     │
│                 │                          │                 │
│  • App logic    │ ◀── NIP-46 requests ──▶ │  • Holds nsec   │
│  • UI/UX        │                          │  • Signs events │
│  • Token ledger │                          │  • Auth control │
│  • DLEQ logic   │                          │  • Audit log    │
│                 │                          │                 │
│  ❌ No nsec     │                          │  ✓ nsec safe    │
└─────────────────┘                          └─────────────────┘
         │                                            │
         │         Phone lost?                        │
         │              ↓                             │
         │         No problem!                        │
         │              ↓                             │
         │     New phone connects to                  │
         │     same nsecbunker ─────────────────────▶│
         │                                            │
         └────────────────────────────────────────────┘
```

**How it works**:
1. Mint's private key lives in nsecbunker (cloud-hosted or self-hosted)
2. Phone app uses NIP-46 (Nostr Connect) to request signatures
3. nsecbunker approves/rejects signing requests based on policy
4. Phone never holds the actual private key

**Benefits**:
| Benefit | Description |
|---------|-------------|
| **Phone loss recovery** | New device connects to same bunker |
| **Multi-device support** | Multiple phones can share mint key |
| **Audit trail** | All signing requests logged |
| **Policy control** | Approve/deny rules for signing |
| **Geographic redundancy** | Bunker can be hosted anywhere |

**Additional Mitigations**:
| Strategy | Description |
|----------|-------------|
| **Mnemonic derivation** | Derive mint keys from merchant mnemonic (backup recovery) |
| **Key rotation** | Periodic key rotation with transition period |
| **Encrypted backup** | Export encrypted key to secure storage as fallback |

### Limitation 4: Scalability

**Problem**: High volume may overwhelm phone.

**Mitigations**:
| Strategy | Description |
|----------|-------------|
| **Volume limits** | Set maximum tokens per period |
| **Graduate to server** | Move to traditional server when scaling |
| **Batch processing** | Process mints in batches |

### Limitation 5: Trust Bootstrap

**Problem**: How do users know the public key is legitimate?

**Mitigations**:
| Strategy | Description |
|----------|-------------|
| **Physical verification** | QR code at merchant location |
| **Nostr identity** | Verify npub matches known merchant |
| **Web of trust** | Endorsements from trusted parties |
| **Domain verification** | NIP-05 identifier |

---

## 10. Relationship to Existing Plans

### How This Fits with gift-card-plan-final.md

Your existing plan specifies:
- **Model B**: Vouchers only redeemable at issuing merchant ✓
- **Nostr ledger**: NIP-33 for voucher status ✓
- **No database**: Nostr is source of truth ✓

Phone-based mint **aligns perfectly**:
- Merchant's phone IS the mint
- Nostr transport already planned
- DLEQ adds offline verification layer

### Extension to NUT-12 Implementation Plan

The `NUT-12-IMPLEMENTATION-PLAN.md` covers core DLEQ implementation. This document extends it with:

| NUT-12 Plan | This Document |
|-------------|---------------|
| DLEQ proof generation | Phone mint generates DLEQ |
| DLEQ verification | Offline verification for vouchers |
| Data structure extensions | Integration with VoucherSecret |
| Wallet verification | Customer app verification |

### Integration with cashu-lib-vouchers

The planned `cashu-lib-vouchers` module should support:

```java
// Phone mint signing with DLEQ
public class PhoneMintService {

    private final DLEQProofGenerator dleqGenerator;

    public SignedVoucherWithDLEQ issueVoucher(VoucherSecret voucher, BigInteger privateKey) {
        // 1. Sign voucher (standard BDHKE)
        BlindSignature signature = signBlindedMessage(voucher, privateKey);

        // 2. Generate DLEQ proof
        DLEQProof dleq = dleqGenerator.generateProof(
            privateKey,
            voucher.getBlindedMessage(),
            signature.getBlindedSignature()
        );

        // 3. Publish to Nostr ledger
        publishToNostr(voucher, "issued");

        return new SignedVoucherWithDLEQ(voucher, signature, dleq);
    }
}
```

---

## 11. Summary

### Key Takeaways

1. **NUT-12 enables phone-based mints** by decoupling validation from the mint
2. **Nostr provides ideal transport** - asynchronous, decentralized, identity-aware
3. **Your voucher architecture benefits directly** - offline verification strengthens Model B
4. **Technical feasibility is high** - modern phones easily handle the cryptographic load
5. **Use cases are well-defined** - closed ecosystems, gift cards, events, communities

### Next Steps

1. **Complete NUT-12 implementation** in cashu-lib-crypto (`DLEQUtils`)
2. **Integrate DLEQ** into cashu-lib-vouchers (`SignedVoucher`)
3. **Prototype phone mint** - Android app with Nostr integration
4. **Test scenarios** - Coffee shop pilot, event ticket demo
5. **Document** - User guides for merchants and customers

### Architecture Benefits

```
                    PHONE MINT + NUT-12 + NOSTR

          No Servers           Offline Verification        Decentralized
              │                       │                        │
              │                       │                        │
              ▼                       ▼                        ▼
    ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
    │  Reduced Costs  │     │ Trustless P2P   │     │  Censorship     │
    │                 │     │    Transfers    │     │   Resistant     │
    │  • No hosting   │     │                 │     │                 │
    │  • No database  │     │  • DLEQ proves  │     │  • Multiple     │
    │  • Phone only   │     │    authenticity │     │    relays       │
    └─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

**Document Version**: 1.0
**Last Updated**: 2025-12-12
**Status**: Conceptual Design / Discussion Document
