# Cashu Voucher - Architecture Diagrams

**Project**: cashu-voucher (multi-module)
**Date**: 2025-11-04
**Architecture**: Hexagonal (Ports & Adapters)

---

## Table of Contents

1. [Component Diagram](#1-component-diagram)
2. [Module Dependency Diagram](#2-module-dependency-diagram)
3. [Sequence Diagrams](#3-sequence-diagrams)
4. [Deployment Diagram](#4-deployment-diagram)
5. [Data Flow Diagrams](#5-data-flow-diagrams)
6. [Class Diagrams](#6-class-diagrams)

---

## 1. Component Diagram

### 1.1 High-Level System View

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Cashu Ecosystem                             │
│                                                                     │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐    │
│  │              │      │              │      │              │    │
│  │  cashu-mint  │◄────►│ cashu-wallet │◄────►│ cashu-client │    │
│  │              │      │              │      │   (CLI)      │    │
│  └──────┬───────┘      └──────┬───────┘      └──────┬───────┘    │
│         │                     │                     │             │
│         │                     │                     │             │
│         └─────────────────────┼─────────────────────┘             │
│                               │                                   │
│                               ▼                                   │
│                  ┌─────────────────────────┐                      │
│                  │                         │                      │
│                  │    cashu-voucher        │                      │
│                  │  (3 modules)            │                      │
│                  │                         │                      │
│                  │  ┌─────────────────┐   │                      │
│                  │  │ domain          │   │                      │
│                  │  │ (pure logic)    │   │                      │
│                  │  └────────┬────────┘   │                      │
│                  │           │            │                      │
│                  │  ┌────────▼────────┐   │                      │
│                  │  │ app             │   │                      │
│                  │  │ (services/ports)│   │                      │
│                  │  └────────┬────────┘   │                      │
│                  │           │            │                      │
│                  │  ┌────────▼────────┐   │                      │
│                  │  │ nostr           │   │                      │
│                  │  │ (adapter)       │   │                      │
│                  │  └─────────────────┘   │                      │
│                  │           │            │                      │
│                  └───────────┼─────────────┘                      │
│                              │                                   │
│                              ▼                                   │
│                  ┌────────────────────┐                          │
│                  │  Nostr Network     │                          │
│                  │  (relays)          │                          │
│                  └────────────────────┘                          │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 cashu-voucher Internal Components

```
┌────────────────────────────────────────────────────────────────────┐
│                    cashu-voucher (top-level)                       │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  cashu-voucher-domain (Layer 1: Pure Domain)                │ │
│  │                                                              │ │
│  │  ├─ VoucherSecret (entity)                                  │ │
│  │  ├─ SignedVoucher (value object)                            │ │
│  │  ├─ VoucherSignatureService (domain service)                │ │
│  │  ├─ VoucherValidator (validation logic)                     │ │
│  │  ├─ VoucherStatus (enum)                                    │ │
│  │  └─ util/                                                   │ │
│  │     └─ VoucherSerializationUtils                            │ │
│  │                                                              │ │
│  │  Dependencies: cashu-lib (entities, crypto, common)         │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                              ▲                                     │
│                              │                                     │
│  ┌───────────────────────────┴──────────────────────────────────┐ │
│  │  cashu-voucher-app (Layer 2: Application Services)          │ │
│  │                                                              │ │
│  │  ┌─── Ports (interfaces) ───┐                               │ │
│  │  │  - VoucherLedgerPort      │                               │ │
│  │  │  - VoucherBackupPort      │                               │ │
│  │  └───────────────────────────┘                               │ │
│  │                                                              │ │
│  │  ┌─── Services ──────────────┐                               │ │
│  │  │  - VoucherService          │                               │ │
│  │  │  - VoucherIssuanceService  │                               │ │
│  │  │  - VoucherBackupService    │                               │ │
│  │  │  - MerchantVerificationSvc │                               │ │
│  │  └───────────────────────────┘                               │ │
│  │                                                              │ │
│  │  ┌─── DTOs ──────────────────┐                               │ │
│  │  │  - IssueVoucherRequest     │                               │ │
│  │  │  - IssueVoucherResponse    │                               │ │
│  │  │  - StoredVoucher           │                               │ │
│  │  └───────────────────────────┘                               │ │
│  │                                                              │ │
│  │  Dependencies: domain                                        │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                              ▲                                     │
│                              │ implements                          │
│  ┌───────────────────────────┴──────────────────────────────────┐ │
│  │  cashu-voucher-nostr (Layer 3: Nostr Adapter)               │ │
│  │                                                              │ │
│  │  ┌─── Repositories (implement ports) ───┐                    │ │
│  │  │  - NostrVoucherLedgerRepository      │                    │ │
│  │  │    (implements VoucherLedgerPort)    │                    │ │
│  │  │  - NostrVoucherBackupRepository      │                    │ │
│  │  │    (implements VoucherBackupPort)    │                    │ │
│  │  └──────────────────────────────────────┘                    │ │
│  │                                                              │ │
│  │  ┌─── Event Mappers ────────────────────┐                    │ │
│  │  │  - VoucherLedgerEvent (NIP-33)       │                    │ │
│  │  │  - VoucherBackupPayload (NIP-17)     │                    │ │
│  │  │  - NostrEventMapper                  │                    │ │
│  │  └──────────────────────────────────────┘                    │ │
│  │                                                              │ │
│  │  ┌─── Infrastructure ────────────────────┐                    │ │
│  │  │  - NostrClientAdapter                 │                    │ │
│  │  │  - NostrRelayConfig                   │                    │ │
│  │  └──────────────────────────────────────┘                    │ │
│  │                                                              │ │
│  │  Dependencies: domain, app (ports), nostr-java              │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## 2. Module Dependency Diagram

### 2.1 Internal Module Dependencies

```
                    ┌──────────────────────┐
                    │ cashu-voucher-domain │
                    │                      │
                    │  VoucherSecret       │
                    │  SignedVoucher       │
                    │  Validation          │
                    └──────────┬───────────┘
                               │
                   ┌───────────┴───────────┐
                   │                       │
                   ▼                       ▼
      ┌────────────────────┐   ┌──────────────────────┐
      │ cashu-voucher-app  │   │ cashu-voucher-nostr  │
      │                    │   │                      │
      │ Defines:           │   │ Implements:          │
      │ - VoucherLedgerPort│◄──│ - VoucherLedgerPort  │
      │ - VoucherBackupPort│◄──│ - VoucherBackupPort  │
      │                    │   │                      │
      │ Services:          │   │ Adapters:            │
      │ - VoucherService   │   │ - NostrRepository    │
      │ - BackupService    │   │ - EventMapper        │
      └────────────────────┘   └──────────────────────┘
```

**Key Principle**:
- **app** defines interfaces (ports)
- **nostr** implements interfaces (adapters)
- **domain** has no dependencies on app or nostr

### 2.2 External Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│                 External Dependencies                       │
└─────────────────────────────────────────────────────────────┘

cashu-voucher-domain
  ├─► cashu-lib-entities (Proof, BlindedMessage, Tag)
  ├─► cashu-lib-crypto (BDHKE)
  ├─► cashu-lib-common (BaseKey, Secret, HexUtils)
  └─► bcprov-jdk18on (ED25519 signing)

cashu-voucher-app
  └─► cashu-voucher-domain

cashu-voucher-nostr
  ├─► cashu-voucher-domain
  ├─► cashu-voucher-app (ports only)
  ├─► nostr-base (Event, Tag)
  └─► nostr-encryption (NIP-44)
```

### 2.3 Consumer Dependencies

```
┌───────────────┐
│  cashu-mint   │ (REST API, validation)
└───────┬───────┘
        │
        ├─► cashu-voucher-domain
        ├─► cashu-voucher-app
        └─► cashu-voucher-nostr (runtime)

┌───────────────┐
│ cashu-wallet  │ (backup, storage)
└───────┬───────┘
        │
        ├─► cashu-voucher-domain
        ├─► cashu-voucher-app
        └─► cashu-voucher-nostr (runtime)

┌───────────────┐
│ cashu-client  │ (CLI commands)
└───────┬───────┘
        │
        └─► cashu-voucher-app
            (nostr provided transitively via wallet)
```

---

## 3. Sequence Diagrams

### 3.1 Issue Voucher Flow

```
User        CLI            VoucherService    VoucherLedgerPort    NostrRepository    Nostr Relay
 │           │                    │                  │                   │               │
 │─issue────►│                    │                  │                   │               │
 │  5000 sat │                    │                  │                   │               │
 │           │                    │                  │                   │               │
 │           │─issue()───────────►│                  │                   │               │
 │           │                    │                  │                   │               │
 │           │                    │─create secret────┤                   │               │
 │           │                    │  (VoucherSecret) │                   │               │
 │           │                    │                  │                   │               │
 │           │                    │─sign secret──────┤                   │               │
 │           │                    │  (ED25519)       │                   │               │
 │           │                    │                  │                   │               │
 │           │                    │─publish()───────►│                   │               │
 │           │                    │  (ISSUED)        │                   │               │
 │           │                    │                  │                   │               │
 │           │                    │                  │─toNostrEvent()───►│               │
 │           │                    │                  │  (NIP-33)         │               │
 │           │                    │                  │                   │               │
 │           │                    │                  │                   │─publish()────►│
 │           │                    │                  │                   │  kind 30078   │
 │           │                    │                  │                   │               │
 │           │                    │                  │                   │◄──────ack────│
 │           │                    │                  │                   │               │
 │           │                    │                  │◄──────────────────┤               │
 │           │                    │◄─────────────────┤                   │               │
 │           │                    │                  │                   │               │
 │           │◄──response─────────│                  │                   │               │
 │           │  (voucher+token)   │                  │                   │               │
 │           │                    │                  │                   │               │
 │◄──success─┤                    │                  │                   │               │
 │  "✅ Issued"                   │                  │                   │               │
 │           │                    │                  │                   │               │
 │           │─backup()──────────►│                  │                   │               │
 │  (async)  │                    │                  │                   │               │
 │           │                    │─backup()────────►VoucherBackupPort───►NostrBackupRepo│
 │           │                    │  (NIP-17)        │                   │               │
 │           │                    │                  │                   │─encrypt()────►│
 │           │                    │                  │                   │  (NIP-44)     │
 │           │                    │                  │                   │               │
 │           │                    │                  │                   │─publish()────►│
 │           │                    │                  │                   │  kind 14 DM   │
 │           │                    │                  │                   │               │
```

### 3.2 Restore Vouchers from Nostr Flow

```
User        CLI            VoucherBackupSvc   VoucherBackupPort   NostrBackupRepo   Nostr Relay
 │           │                    │                  │                  │               │
 │─restore──►│                    │                  │                  │               │
 │           │                    │                  │                  │               │
 │           │─restore()─────────►│                  │                  │               │
 │           │                    │                  │                  │               │
 │           │                    │─restore()───────►│                  │               │
 │           │                    │  (nostrPrivkey)  │                  │               │
 │           │                    │                  │                  │               │
 │           │                    │                  │─query()─────────►│               │
 │           │                    │                  │  kind=14         │               │
 │           │                    │                  │  subject=backup  │               │
 │           │                    │                  │                  │               │
 │           │                    │                  │                  │◄──events[]───│
 │           │                    │                  │                  │  (encrypted)  │
 │           │                    │                  │                  │               │
 │           │                    │                  │◄─────────────────┤               │
 │           │                    │                  │  events[]        │               │
 │           │                    │                  │                  │               │
 │           │                    │                  │─decrypt()────────┤               │
 │           │                    │                  │  (NIP-44)        │               │
 │           │                    │                  │  for each event  │               │
 │           │                    │                  │                  │               │
 │           │                    │                  │─parse()──────────┤               │
 │           │                    │                  │  JSON→vouchers   │               │
 │           │                    │                  │                  │               │
 │           │                    │                  │─deduplicate()────┤               │
 │           │                    │                  │  (by ID+TS)      │               │
 │           │                    │                  │                  │               │
 │           │                    │◄─────vouchers[]──┤                  │               │
 │           │                    │                  │                  │               │
 │           │◄──vouchers[]───────┤                  │                  │               │
 │           │                    │                  │                  │               │
 │◄──success─┤                    │                  │                  │               │
 │  "✅ Restored 5 vouchers"      │                  │                  │               │
```

### 3.3 Merchant Verify Voucher Flow (Model B)

```
Merchant   MerchantCLI    MerchantVerifySvc   VoucherValidator   VoucherLedgerPort   NostrRepo   Relay
   │           │                 │                    │                  │              │         │
   │─verify───►│                 │                    │                  │              │         │
   │  token    │                 │                    │                  │              │         │
   │           │                 │                    │                  │              │         │
   │           │─parse token────►│                    │                  │              │         │
   │           │  →SignedVoucher │                    │                  │              │         │
   │           │                 │                    │                  │              │         │
   │           │─verifyOffline()►│                    │                  │              │         │
   │           │                 │                    │                  │              │         │
   │           │                 │─validate()────────►│                  │              │         │
   │           │                 │                    │                  │              │         │
   │           │                 │                    │─checkSignature() │              │         │
   │           │                 │                    │  (ED25519)       │              │         │
   │           │                 │                    │                  │              │         │
   │           │                 │                    │─checkExpiry()────┤              │         │
   │           │                 │                    │  (timestamp)     │              │         │
   │           │                 │                    │                  │              │         │
   │           │                 │◄───result──────────┤                  │              │         │
   │           │                 │                    │                  │              │         │
   │           │◄──offline OK────┤                    │                  │              │         │
   │           │                 │                    │                  │              │         │
   │           │─verifyOnline() ►│                    │                  │              │         │
   │           │  (optional)     │                    │                  │              │         │
   │           │                 │                    │                  │              │         │
   │           │                 │─queryStatus()─────►│                  │              │         │
   │           │                 │  (voucherId)       │                  │              │         │
   │           │                 │                    │                  │              │         │
   │           │                 │                    │─query()─────────►│              │         │
   │           │                 │                    │  NIP-33          │              │         │
   │           │                 │                    │                  │              │         │
   │           │                 │                    │                  │─query()─────►│         │
   │           │                 │                    │                  │  kind=30078  │         │
   │           │                 │                    │                  │  d=voucher:* │         │
   │           │                 │                    │                  │              │         │
   │           │                 │                    │                  │              │◄─event──│
   │           │                 │                    │                  │              │ status  │
   │           │                 │                    │                  │              │         │
   │           │                 │                    │◄─────status──────┤              │         │
   │           │                 │◄───ISSUED──────────┤  (not redeemed)  │              │         │
   │           │                 │                    │                  │              │         │
   │           │◄──✅ VALID──────┤                    │                  │              │         │
   │           │  "Accept voucher"                    │                  │              │         │
   │           │                 │                    │                  │              │         │
   │◄──accept──┤                 │                    │                  │              │         │
   │  customer │                 │                    │                  │              │         │
```

### 3.4 Model B Rejection at Mint Flow

```
User        CLI          MintSwap        ProofValidator      VoucherSecret
 │           │               │                  │                  │
 │─swap─────►│               │                  │                  │
 │  voucher  │               │                  │                  │
 │  proof    │               │                  │                  │
 │           │               │                  │                  │
 │           │─swap()───────►│                  │                  │
 │           │               │                  │                  │
 │           │               │─validate()──────►│                  │
 │           │               │  proofs[]        │                  │
 │           │               │                  │                  │
 │           │               │                  │─checkType()─────►│
 │           │               │                  │  instanceof      │
 │           │               │                  │  VoucherSecret?  │
 │           │               │                  │                  │
 │           │               │                  │◄─────YES─────────┤
 │           │               │                  │                  │
 │           │               │◄────❌ throw─────┤                  │
 │           │               │ VoucherNotAccepted                 │
 │           │               │ Exception                          │
 │           │               │                  │                  │
 │           │◄──❌ error────┤                  │                  │
 │           │  "Vouchers cannot be redeemed   │                  │
 │           │   at mint (Model B)"            │                  │
 │           │                                  │                  │
 │◄──error───┤                                  │                  │
 │  "Please redeem with merchant"              │                  │
```

---

## 4. Deployment Diagram

### 4.1 Production Deployment (with Nostr)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Production Environment                       │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                     cashu-mint (Server)                       │ │
│  │                                                               │ │
│  │  ┌─────────────────────────────────────────────────────────┐ │ │
│  │  │  cashu-mint-rest (Spring Boot)                          │ │ │
│  │  │  ├─ POST /v1/vouchers (issuance)                        │ │ │
│  │  │  ├─ GET /v1/vouchers/{id}/status                        │ │ │
│  │  │  └─ POST /v1/swap (rejects vouchers)                    │ │ │
│  │  └─────────────────────────────────────────────────────────┘ │ │
│  │                             │                                 │ │
│  │  ┌──────────────────────────▼──────────────────────────────┐ │ │
│  │  │  cashu-mint-protocol                                    │ │ │
│  │  │  ├─ VoucherService (app layer)                          │ │ │
│  │  │  ├─ ProofValidator (Model B rejection)                  │ │ │
│  │  │  └─ NostrVoucherLedgerRepository (adapter)              │ │ │
│  │  └─────────────────────────────────────────────────────────┘ │ │
│  │                             │                                 │ │
│  │  Dependencies:              │                                 │ │
│  │  ├─ cashu-voucher-domain                                     │ │
│  │  ├─ cashu-voucher-app                                        │ │
│  │  └─ cashu-voucher-nostr                                      │ │
│  │                             │                                 │ │
│  └─────────────────────────────┼─────────────────────────────────┘ │
│                                │                                   │
│                                │ WebSocket                         │
│                                ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │             Nostr Relay Infrastructure                     │   │
│  │                                                            │   │
│  │  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐  │   │
│  │  │ Self-Hosted  │   │ relay.damus  │   │ relay.primal │  │   │
│  │  │ Relay        │   │ .io          │   │ .net         │  │   │
│  │  │ (strfry)     │   │ (public)     │   │ (public)     │  │   │
│  │  └──────────────┘   └──────────────┘   └──────────────┘  │   │
│  │                                                            │   │
│  │  Stores:                                                   │   │
│  │  - NIP-33 events (voucher ledger)                          │   │
│  │  - NIP-17 events (wallet backups, encrypted)               │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                │                                   │
│                                │ WebSocket                         │
│                                ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │                  cashu-wallet (Client)                     │   │
│  │                                                            │   │
│  │  ┌──────────────────────────────────────────────────────┐ │   │
│  │  │  cashu-wallet-protocol                               │ │   │
│  │  │  ├─ WalletVoucherService                             │ │   │
│  │  │  ├─ VoucherBackupService (uses NIP-17)               │ │   │
│  │  │  └─ NostrVoucherBackupRepository                     │ │   │
│  │  └──────────────────────────────────────────────────────┘ │   │
│  │                                                            │   │
│  │  ┌──────────────────────────────────────────────────────┐ │   │
│  │  │  wallet-storage-file / wallet-storage-h2             │ │   │
│  │  │  ├─ WalletState (vouchers cached locally)            │ │   │
│  │  │  └─ Syncs from Nostr on startup                      │ │   │
│  │  └──────────────────────────────────────────────────────┘ │   │
│  │                                                            │   │
│  │  Dependencies:                                             │   │
│  │  ├─ cashu-voucher-domain                                  │   │
│  │  ├─ cashu-voucher-app                                     │   │
│  │  └─ cashu-voucher-nostr                                   │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Component Distribution

```
Server Side (cashu-mint)
├─ cashu-mint-rest.jar
│  └─ includes: cashu-voucher-domain, cashu-voucher-app, cashu-voucher-nostr
├─ Configuration:
│  ├─ application.yml (voucher.mint.issuerPrivateKey)
│  ├─ Nostr relay URLs
│  └─ Port 8080 (REST API)
└─ External:
   └─ Nostr relays (WebSocket connections)

Client Side (cashu-wallet + cashu-client)
├─ cashu-client-wallet-cli.jar
│  └─ includes: cashu-voucher-app (domain + nostr transitively via wallet)
├─ Configuration:
│  ├─ wallet-state.json (vouchers cached)
│  ├─ Nostr private key (encrypted)
│  └─ Nostr relay URLs
└─ External:
   └─ Nostr relays (WebSocket connections)
```

### 4.3 Network Topology

```
                    Internet
                       │
       ┌───────────────┼───────────────┐
       │               │               │
       ▼               ▼               ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ Self-Hosted │ │ Public      │ │ Public      │
│ Nostr Relay │ │ Relay 1     │ │ Relay 2     │
│ (Primary)   │ │ (Backup)    │ │ (Backup)    │
└──────┬──────┘ └──────┬──────┘ └──────┬──────┘
       │               │               │
       │  wss://       │  wss://       │  wss://
       │  relay.       │  relay.       │  relay.
       │  yourorg.com  │  damus.io     │  primal.net
       │               │               │
       └───────────────┼───────────────┘
                       │
       ┌───────────────┼───────────────────────┐
       │               │                       │
       ▼               ▼                       ▼
┌─────────────┐ ┌─────────────┐       ┌─────────────┐
│ cashu-mint  │ │ cashu-wallet│       │  Users      │
│ (Server)    │ │ (Client)    │       │  (Mobile)   │
│             │ │             │       │             │
│ :8080       │ │ CLI         │       │ Future      │
└─────────────┘ └─────────────┘       └─────────────┘
```

---

## 5. Data Flow Diagrams

### 5.1 Voucher Issuance Data Flow

```
┌──────┐     ┌─────────┐     ┌────────────┐     ┌──────────┐     ┌───────┐
│ User │────►│  CLI    │────►│ VoucherSvc │────►│ NostrRepo│────►│ Relay │
└──────┘     └─────────┘     └────────────┘     └──────────┘     └───────┘
   │              │                 │                  │              │
   │ amount       │ IssueVoucher    │ SignedVoucher    │ NIP-33       │
   │ memo         │ Request         │                  │ Event        │
   │ expiry       │                 │                  │              │
   │              │                 │                  │              │
   │              │                 ▼                  │              │
   │              │            VoucherSecret           │              │
   │              │            (domain)                │              │
   │              │                 │                  │              │
   │              │                 │ sign             │              │
   │              │                 │ (ED25519)        │              │
   │              │                 ▼                  │              │
   │              │            SignedVoucher           │              │
   │              │                 │                  │              │
   │              │                 │ publish          │              │
   │              │                 ▼                  │              │
   │              │            VoucherLedgerPort       │              │
   │              │                 │                  │              │
   │              │                 │ toNostrEvent     │              │
   │              │                 ▼                  │              │
   │              │            GenericEvent            │              │
   │              │            (kind: 30078)           │              │
   │              │            tags: [d, status, ...]  │              │
   │              │                 │                  │              │
   │              │                 └─────────────────►│              │
   │              │                                    │              │
   │              │                                    └─────────────►│
   │              │                                                   │
   │              │◄──────────────────────────────────────────────────┤
   │              │ IssueVoucherResponse                              │
   │              │ (voucher + token)                                 │
   │              │                                                   │
   │◄─────────────┤                                                   │
   │ Display      │                                                   │
   │ "✅ Issued"  │                                                   │
```

### 5.2 Voucher Backup Data Flow

```
┌──────────┐     ┌────────────┐     ┌──────────┐     ┌───────┐
│ Wallet   │────►│ BackupSvc  │────►│ NostrRepo│────►│ Relay │
└──────────┘     └────────────┘     └──────────┘     └───────┘
     │                  │                  │              │
     │ SignedVoucher[]  │ VoucherBackup    │ NIP-17       │
     │                  │ Payload          │ Event        │
     │                  │                  │              │
     │                  ▼                  │              │
     │            Serialize to JSON        │              │
     │            {version, vouchers[]}    │              │
     │                  │                  │              │
     │                  │ encrypt          │              │
     │                  │ (NIP-44)         │              │
     │                  ▼                  │              │
     │            Encrypted string         │              │
     │            (ChaCha20-Poly1305)      │              │
     │                  │                  │              │
     │                  │ create DM        │              │
     │                  ▼                  │              │
     │            GenericEvent             │              │
     │            kind: 14                 │              │
     │            tags: [p, subject]       │              │
     │            content: encrypted       │              │
     │                  │                  │              │
     │                  └─────────────────►│              │
     │                                     │              │
     │                                     └─────────────►│
     │                                                    │
     │◄───────────────────────────────────────────────────┤
     │ Backup confirmed                                   │
     │ Event ID stored                                    │
```

### 5.3 Voucher Restore Data Flow

```
┌───────┐     ┌──────────┐     ┌────────────┐     ┌──────────┐
│ Relay │────►│ NostrRepo│────►│ BackupSvc  │────►│ Wallet   │
└───────┘     └──────────┘     └────────────┘     └──────────┘
    │              │                  │                  │
    │ Query        │ GenericEvent[]   │ Encrypted        │ SignedVoucher[]
    │ kind=14      │ (NIP-17 DMs)     │ payloads         │
    │ subject=     │                  │                  │
    │ backup       │                  │                  │
    │              │                  │                  │
    ◄──────────────┤                  │                  │
    │ Events[]     │                  │                  │
    │              │                  │                  │
    │              └─────────────────►│                  │
    │                                 │                  │
    │                                 │ decrypt each     │
    │                                 │ (NIP-44)         │
    │                                 ▼                  │
    │                            JSON payloads           │
    │                            {vouchers[]}            │
    │                                 │                  │
    │                                 │ parse            │
    │                                 ▼                  │
    │                            VoucherBackupPayload[]  │
    │                                 │                  │
    │                                 │ deduplicate      │
    │                                 │ (by ID + TS)     │
    │                                 ▼                  │
    │                            SignedVoucher[]         │
    │                            (unique, latest)        │
    │                                 │                  │
    │                                 └─────────────────►│
    │                                                    │
    │                                          Merge     │
    │                                          with      │
    │                                          local     │
    │                                          state     │
```

---

## 6. Class Diagrams

### 6.1 Domain Layer Classes

```
┌─────────────────────────────────────────────────────────────┐
│                     <<interface>>                           │
│                        Secret                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ + getData(): byte[]                                  │   │
│  │ + setData(byte[]): void                              │   │
│  │ + toBytes(): byte[]                                  │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         △
                         │ implements
                         │
┌────────────────────────┴────────────────────────────────────┐
│                     BaseKey                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ # data: byte[]                                       │   │
│  │ + toHex(): String                                    │   │
│  │ + fromHex(String): BaseKey                           │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         △
                         │ extends
                         │
┌────────────────────────┴────────────────────────────────────┐
│                   VoucherSecret                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - voucherId: String                                  │   │
│  │ - issuerId: String                                   │   │
│  │ - unit: String                                       │   │
│  │ - faceValue: long                                    │   │
│  │ - expiresAt: Long                                    │   │
│  │ - memo: String                                       │   │
│  │ ─────────────────────────────────────────────────────│   │
│  │ + create(String, String, long, Long, String):        │   │
│  │   VoucherSecret (factory)                            │   │
│  │ + toCanonicalBytes(): byte[]                         │   │
│  │ + isExpired(): boolean                               │   │
│  │ + isValid(): boolean                                 │   │
│  │ + toHex(): String                                    │   │
│  │ + getData(): byte[]                                  │   │
│  │ + setData(byte[]): void (throws exception)           │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ used by
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   SignedVoucher                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - secret: VoucherSecret                              │   │
│  │ - issuerSignature: byte[]                            │   │
│  │ - issuerPublicKey: String                            │   │
│  │ ─────────────────────────────────────────────────────│   │
│  │ + verify(): boolean                                  │   │
│  │ + isExpired(): boolean                               │   │
│  │ + isValid(): boolean                                 │   │
│  │ + getSecret(): VoucherSecret                         │   │
│  │ + getSignature(): byte[]                             │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Application Layer Classes (Ports & Services)

```
┌────────────────────────────────────────────────────────────┐
│                <<interface>>                               │
│              VoucherLedgerPort                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ + publish(SignedVoucher, VoucherStatus): void        │  │
│  │ + queryStatus(String): Optional<VoucherStatus>       │  │
│  │ + updateStatus(String, VoucherStatus): void          │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────────────┬─────────────────────────────────┘
                           △
                           │ implements
                           │
          ┌────────────────┴────────────────┐ (in nostr module)
          │ NostrVoucherLedgerRepository    │
          └─────────────────────────────────┘


┌────────────────────────────────────────────────────────────┐
│                <<interface>>                               │
│              VoucherBackupPort                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ + backup(List<SignedVoucher>, String): void          │  │
│  │ + restore(String): List<SignedVoucher>               │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────────────┬─────────────────────────────────┘
                           △
                           │ implements
                           │
          ┌────────────────┴────────────────┐ (in nostr module)
          │ NostrVoucherBackupRepository    │
          └─────────────────────────────────┘


┌─────────────────────────────────────────────────────────────┐
│                   VoucherService                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - ledgerPort: VoucherLedgerPort                      │   │
│  │ - backupPort: VoucherBackupPort                      │   │
│  │ - mintIssuerPrivateKey: String                       │   │
│  │ - mintIssuerPublicKey: String                        │   │
│  │ ─────────────────────────────────────────────────────│   │
│  │ + issue(IssueVoucherRequest):                        │   │
│  │   IssueVoucherResponse                               │   │
│  │ + queryStatus(String): Optional<VoucherStatus>       │   │
│  │ + backup(List<SignedVoucher>, String): void          │   │
│  │ + restore(String): List<SignedVoucher>               │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 Nostr Layer Classes

```
┌─────────────────────────────────────────────────────────────┐
│           NostrVoucherLedgerRepository                      │
│           (implements VoucherLedgerPort)                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - nostrClient: NostrClientAdapter                    │   │
│  │ - mintIssuerPubkey: String                           │   │
│  │ ─────────────────────────────────────────────────────│   │
│  │ + publish(SignedVoucher, VoucherStatus): void        │   │
│  │ + queryStatus(String): Optional<VoucherStatus>       │   │
│  │ + updateStatus(String, VoucherStatus): void          │   │
│  │ ─────────────────────────────────────────────────────│   │
│  │ - toNostrEvent(SignedVoucher): GenericEvent          │   │
│  │ - fromNostrEvent(GenericEvent): VoucherLedgerEvent   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                         │ uses
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                VoucherLedgerEvent                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - voucherId: String                                  │   │
│  │ - status: String                                     │   │
│  │ - issuerId: String                                   │   │
│  │ - unit: String                                       │   │
│  │ - faceValue: long                                    │   │
│  │ - expiresAt: Long                                    │   │
│  │ - hash: String                                       │   │
│  │ - memo: String                                       │   │
│  │ - eventId: String                                    │   │
│  │ - createdAt: long                                    │   │
│  │ ─────────────────────────────────────────────────────│   │
│  │ + toNostrEvent(String): GenericEvent (NIP-33)        │   │
│  │ + fromNostrEvent(GenericEvent): VoucherLedgerEvent   │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. State Diagram

### 7.1 Voucher Lifecycle States

```
                     ┌────────────┐
                     │  Created   │
                     │ (in memory)│
                     └──────┬─────┘
                            │
                            │ mint.issue()
                            │ + sign
                            ▼
                     ┌────────────┐
              ┌─────►│   ISSUED   │◄─────┐
              │      │ (published │      │
              │      │  to Nostr) │      │
              │      └──────┬─────┘      │
              │             │            │
              │             │            │ query
     revoke() │             │ redeem()   │ status
              │             │            │
              │             ▼            │
              │      ┌────────────┐      │
              │      │  REDEEMED  │──────┘
              │      │ (merchant) │
              │      └────────────┘
              │
              │
              ▼
       ┌────────────┐
       │  REVOKED   │
       │ (issuer)   │
       └────────────┘


       ┌────────────┐
       │  EXPIRED   │ (time-based transition)
       │ (automatic)│
       └────────────┘
```

### 7.2 Backup State Transitions

```
┌─────────────┐     issue      ┌─────────────┐
│  No Backup  │───────────────►│  Unsynced   │
└─────────────┘                └──────┬──────┘
                                      │
                                      │ backup()
                                      │ (async)
                                      ▼
                               ┌─────────────┐
                               │  Synced     │
                               │ (on Nostr)  │
                               └──────┬──────┘
                                      │
                                      │ spend/update
                                      │
                                      ▼
                               ┌─────────────┐
                               │  Unsynced   │
                               │ (modified)  │
                               └──────┬──────┘
                                      │
                                      │ backup()
                                      │
                                      ▼
                               ┌─────────────┐
                               │  Synced     │
                               └─────────────┘
```

---

## 8. Summary

This document provides comprehensive architectural diagrams for the cashu-voucher multi-module project:

1. **Component Diagrams** - Show internal module structure and dependencies
2. **Sequence Diagrams** - Illustrate key flows (issue, restore, verify, reject)
3. **Deployment Diagram** - Show production topology with Nostr relays
4. **Data Flow Diagrams** - Trace data through system layers
5. **Class Diagrams** - Detail domain, app, and nostr layer classes
6. **State Diagrams** - Show voucher lifecycle and backup states

**Key Architectural Principles**:
- ✅ Hexagonal architecture (ports & adapters)
- ✅ Clear separation of concerns (domain, app, nostr)
- ✅ Dependency inversion (app defines interfaces, nostr implements)
- ✅ Testable at each layer (pure domain, mocked services, mock relay)

---

**Document Version**: 1.0
**Last Updated**: 2025-11-04
**Related**: gift-card-plan-final-v2.md
