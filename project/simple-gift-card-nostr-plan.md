# Implementation Plan – Use Case 1 (Simple Gift Card) with Nostr NIP-17 Backup

## Table of Contents
- [1. Summary](#1-summary)
- [2. Functional Scope](#2-functional-scope)
- [3. Architecture & Components](#3-architecture--components)
  - [3.1 Voucher Domain](#31-voucher-domain)
  - [3.2 Mint Layer](#32-mint-layer)
  - [3.3 Wallet Layer](#33-wallet-layer)
- [4. Nostr Backup Design (NIP-17)](#4-nostr-backup-design-nip-17)
  - [4.1 Keys & Encryption](#41-keys--encryption)
  - [4.2 Backup Flow](#42-backup-flow)
  - [4.3 Restore Flow](#43-restore-flow)
  - [4.4 Operational Notes](#44-operational-notes)
- [5. Security Considerations](#5-security-considerations)
- [6. Implementation Phases](#6-implementation-phases)
  - [Phase 0 – Foundations](#phase-0--foundations)
  - [Phase 1 – Mint & Wallet Voucher Flows](#phase-1--mint--wallet-voucher-flows)
  - [Phase 2 – Nostr Backup (NIP-17)](#phase-2--nostr-backup-nip-17)
  - [Phase 3 – UX, Ops & Documentation](#phase-3--ux-ops--documentation)
- [7. Testing Strategy](#7-testing-strategy)
- [8. Risks & Mitigations](#8-risks--mitigations)
- [9. Dependencies & Next Steps](#9-dependencies--next-steps)
- [10. Using Nostr as the Voucher Ledger](#10-using-nostr-as-the-voucher-ledger)
  - [10.1 Event Schema](#101-event-schema)
  - [10.2 State Transitions](#102-state-transitions)
  - [10.3 Integration Touch Points](#103-integration-touch-points)
  - [10.4 Relay Strategy & Durability](#104-relay-strategy--durability)
  - [10.5 Privacy & Security](#105-privacy--security)
  - [10.6 Next Actions](#106-next-actions)
- [11. Project Integration Overview](#11-project-integration-overview)

## 1. Summary
- Implement the “simple gift card” voucher described in `VOUCHER-SPENDING-CONDITIONS-ANALYSIS.md` Use Case 1.
- Ensure every issued voucher can be backed up and restored through Nostr using **NIP-17 private DMs** (as outlined in `VOUCHER-REDEMPTION-AND-BACKUP.md`).
- Keep scope limited to vouchers without additional spending conditions, but make the design extensible for future use cases.

## 2. Functional Scope
- **Voucher issuance**: Merchant creates a voucher backed by its issuer signature and stored in the mint’s voucher ledger.
- **Voucher redemption**: Wallet presents voucher proof to mint (Model A) or to merchant (Model B) with validation logic for signature, expiry, and double-spend prevention.
- **Nostr backup (NIP-17)**:
  - Wallet encrypts voucher proofs using NIP-44 (ChaCha20-Poly1305) and sends them to self as NIP-17 DM (kind 14). 
  - Include `subject` tag (e.g., `cashu-voucher-backup`) and `p` tag pointing to user pubkey.
  - Support restore flow that scans DMs, decrypts backups, and reconciles against local wallet state.

## 3. Architecture & Components
### 3.1 Voucher Domain
- `VoucherSecret` enhancements:
  - Fields: `voucherId`, `issuerId`, `unit`, `faceValue`, `expiresAt`, optional `memo`, `issuerSignature`.
  - `toCanonicalBytes()` helper for deterministic signing.
  - Jackson/CBOR integration for tokens (reuse `JsonUtils`).
- Signing utilities:
  - `VoucherSignatureService.sign(VoucherSecret, IssuerPrivateKey)`.
  - `VoucherSignatureService.verify(VoucherSecret, IssuerPublicKey)`.

### 3.2 Mint Layer
- **Issuance API** (`POST /v1/vouchers` or extension of `/v1/mint`):
  - Validate merchant auth, amount range, expiry, memo length.
  - Persist voucher metadata + hash in `voucher_ledger` table (status: `ISSUED`, `REDEEMED`).
  - Return standard proofs embedding `VoucherSecret`.
- **Redemption (Model A)**:
  - Extend swap/melt validators to call `VoucherValidator`:
    - Confirm issuer signature, expiry.
    - Check voucher is issued by this mint and not yet redeemed.
    - Atomically mark voucher record as `REDEEMED` on success.
- **Redemption (Model B)**:
  - Merchant POS verifier to validate voucher locally and mark spent.
  - Mint rejects vouchers in swap/melt routes when Model B enabled.
- **Configuration flag** (`voucher.redemption.mode`): `INTEGRATED` (Model A) vs `MERCHANT_ONLY` (Model B).

### 3.3 Wallet Layer
- Storage for vouchers (`voucher_store`): metadata, proof, blinding factors, redemption status, last backup event id.
- UI/API flows:
  - Issue: call mint issuance endpoint, display voucher metadata.
  - Redeem: present voucher to mint or merchant, update state.
  - Backup: automatic after issuance/update or user-triggered manual.

## 4. Nostr Backup Design (NIP-17)
### 4.1 Keys & Encryption
- Wallet maintains Nostr keypair (store in secure enclave / encrypted disk).
- Encrypt backup payload using NIP-44 (shared secret derived from wallet’s Nostr private key and same public key).
- Payload schema (`VoucherBackupPayload`): version, timestamp, list of vouchers (voucher secret + proof + status + metadata).

### 4.2 Backup Flow
1. Gather voucher proofs needing backup (new or updated since last backup).
2. Serialize payload (JSON) and encrypt using NIP-44 with optional user passphrase salt.
3. Construct NIP-17 DM event (kind 14):
   - `pubkey`: wallet pubkey.
   - `tags`: 
     - `p` tag with wallet pubkey (self DM).
     - `subject` tag `cashu-voucher-backup`.
     - optional `client` tag `cashu-wallet`.
   - `content`: encrypted payload.
4. Sign event with wallet Nostr key, publish to configured relays.
5. Record event id + timestamp in `voucher_store` for audit.

### 4.3 Restore Flow
1. Query relays for kind 14 events authored by wallet pubkey with `subject=cashu-voucher-backup`.
2. Sort events by `created_at` descending; deduplicate by event id.
3. Decrypt content, deserialize payload.
4. Merge vouchers into wallet store:
   - If voucher already present and states differ, choose latest timestamp.
   - Track `restored_from_event_id` for audit.
5. Display restore summary to user.

### 4.4 Operational Notes
- Maintain relay list (configurable) and exponential backoff if publication fails.
- Persist minimal metadata locally (event ids) to avoid duplicate backups.
- Provide CLI commands: `voucher backup`, `voucher restore`, `voucher backup-status`.
- Consider optional passphrase prompt for encryption key at backup/restore time.

## 5. Security Considerations
- **Backup encryption**: mandatory; never send raw vouchers over Nostr.
- **Key protection**: store Nostr private key encrypted; require unlock for backups.
- **Metadata leakage**: DM reveals sender/recipient/time – acceptable for this iteration; warn users.
- **Replay protection**: include monotonically increasing `backup_counter` in payload to detect stale restores.
- **Voucher redemption race**: mark voucher as backed up only after DM publish success.

## 6. Implementation Phases
### Phase 0 – Foundations
1. Bootstrap the new `cashu-voucher` project (Java 21, Maven) with CI, Jacoco, code style, and publishing pipeline.
2. Extract/implement voucher domain primitives (`VoucherSecret`, signing, canonical bytes) inside `cashu-voucher` with unit tests.
3. Publish snapshot artifacts and wire dependency management so `cashu-lib`, `cashu-mint`, and `cashu-wallet` can consume `cashu-voucher`.
4. Introduce config flags (`voucher.redemption.mode`, `voucher.nostr.backup.enabled`) in `cashu-mint`/`cashu-wallet` (off by default).
5. Draft DB schema/migration scripts for voucher ledger + blinding-factor storage; store in `project/db`.
6. Update architecture docs/ADR referencing `cashu-voucher` and Nostr ledger usage (§10).

### Phase 1 – Mint & Wallet Voucher Flows
1. In `cashu-mint`, expose voucher issuance API (e.g. `POST /v1/vouchers`) leveraging `cashu-voucher`; persist vouchers; emit `status=issued` ledger events.
2. Extend swap/melt validators to call `VoucherValidator`, atomically mark vouchers `redeemed`, and publish ledger updates.
3. Add voucher storage/issuance/redemption flows in `cashu-wallet` (CLI/UI) using `cashu-voucher` DTOs and QR support.
4. Update `cashu-lib` serializers/tests to ensure vouchers round-trip in proofs.
5. Extend `cashu-client` SDK for merchant issuance/redemption helpers.
6. Build integration tests (Testcontainers) covering issue→redeem→ledger + double-spend negative path.

### Phase 2 – Nostr Backup (NIP-17)
1. Add shared Nostr client adapter (NIP-17 + NIP-44) available to `cashu-wallet`/`cashu-mint`.
2. Implement wallet backup engine: encrypt payloads, send self DM, track event IDs/status.
3. Build restore workflow with conflict resolution, CLI commands (`voucher backup`, `voucher restore`, `voucher backup-status`).
4. Implement mint relay service for ledger events (publish + subscribe) with retry/monitoring hooks.
5. Create automated tests using mock relays for backup/restore/ledger flows.
6. Document backup procedures and key handling guidance in docs.

### Phase 3 – UX, Ops & Documentation
1. Polish wallet UX/CLI (voucher dashboard, redemption prompts, backup status).
2. Enhance merchant tooling (`cashu-client`, admin UI) for issuance control and ledger reporting.
3. Publish ops runbooks (relay config, ledger replay handling, reconciliation).
4. Produce tutorials for merchants and users (issue/redeem/backup/restore).
5. Configure monitoring/alerts for publish failures, ledger drift, backup errors.
6. Coordinate release comms, migrations, and feature flag rollout notes.

## 7. Testing Strategy
- Unit tests: voucher signing/verification, ledger updates, NIP-44 encryption/decryption, DM event creation.
- Integration tests (Testcontainers + mock Nostr relay): issue voucher → auto backup DM → delete local entry → restore from Nostr.
- Regression tests: ensure vouchers without backup still function when feature disabled.
- Manual QA: cross-wallet restore (export private key + DM restore).

## 8. Risks & Mitigations
- **Relay downtime**: queue backups locally, retry with exponential backoff.
- **Encrypted payload growth**: enforce max number of vouchers per DM; roll over with sequence tags.
- **User loses Nostr key**: document requirement to back up Nostr keys alongside wallet mnemonic.
- **Future spending conditions**: keep payload schema extensible (versioned, include optional spending fields).

## 9. Dependencies & Next Steps
- Select Nostr relay library for JVM (e.g., `nostr-java`).
- Confirm NIP-44 implementation availability or implement in-house.
- Coordinate with merchant tooling for voucher issuance UI.
- Schedule follow-up to evaluate move to NIP-78 once replaceable backups become priority.


## 10. Using Nostr as the Voucher Ledger

### 10.1 Event Schema
- Represent each voucher state using a parameterised replaceable event (NIP-33) to ensure the most recent status is authoritative.
- Suggested kind: `30078` (NIP-78 application data) or custom (e.g. `30300`).
- Mandatory tags:
  - `d`: unique voucher id (`voucher:<uuid>`).
  - `status`: `issued`, `redeemed`, `revoked`.
  - `issuer`: issuer identifier or pubkey.
  - `hash`: blinded hash of voucher secret (prevents disclosure of bearer value).
  - `unit`, `value`, `expires_at`, `memo` as needed.
- `content`: optional JSON payload for memo or additional fields (consider encrypting if confidential).

**Example**
```json
{
  "kind": 30078,
  "pubkey": "<issuer nostr pubkey>",
  "created_at": 1736294400,
  "tags": [
    ["d", "voucher:1234abcd"],
    ["status", "issued"],
    ["unit", "sat"],
    ["value", "5000"],
    ["expires_at", "1743897600"],
    ["hash", "hash160(voucher_secret)"]
  ],
  "content": "{"version":1,"memo":"Holiday bonus"}",
  "sig": "<signature>"
}
```

### 10.2 State Transitions
| Transition | Actors | Nostr event |
|------------|--------|-------------|
| Issue voucher | Issuing service | publish `status=issued` |
| Deliver voucher | Issuer → wallet | continue using NIP-17 DM (encrypted) |
| Redeem voucher | Mint | publish `status=redeemed` for same `d` tag |
| Revoke/adjust | Issuer | publish `status=revoked` or replacement metadata |

Wallet validation sequence: resolve voucher ID, fetch latest event by `d` tag, verify issuer signature and `status=issued`, confirm hash matches before presenting proof. Mint must publish `redeemed` event after successful swap/melt.

### 10.3 Integration Touch Points
- **Issuer backend**: on issuance and redemption, publish corresponding Nostr event and persist event id alongside internal voucher record.
- **Mint**: subscribe to issuer relays or query on demand; enforce that a voucher had an `issued` event prior to redemption; after redemption emit `redeemed` event.
- **Wallet**: display ledger status (valid/redeemed); keep last seen event id; allow user to trigger refresh. Continue using NIP-17 DM backups for full proofs.
- **Auditors**: consume events to compute outstanding liability.

### 10.4 Relay Strategy & Durability
- Publish to a curated relay set (issuer-operated plus public relays). Handle failures with retries and log anomalies.
- Maintain an internal mirror of events (event id, status, timestamp) to withstand relay pruning and to republish if needed.
- Consider operating a dedicated voucher relay for guaranteed retention.

### 10.5 Privacy & Security
- Public ledger entries leak metadata (amount, expiry). Mitigation: hash voucher secret, encrypt content, or only publish minimal beacons.
- Ownership remains private because the bearer proof stays in NIP-17 DM backup.
- Eventual consistency: Nostr is not transactional; still rely on mint’s double-spend checks. Treat ledger as audit trail rather than trust anchor.

### 10.6 Next Actions
1. Define `VoucherLedgerEvent` DTO and signing helpers.
2. Extend issuer/mint services to emit/subsribe to ledger events.
3. Add wallet sync module to fetch latest voucher state on demand.
4. Update documentation for operational practices (relay configuration, replay handling).

## 11. Project Integration Overview
- **cashu-voucher** (new): houses voucher domain primitives, signing, canonical serialization; published via Maven for reuse.
- **cashu-lib**: consumes `cashu-voucher` to embed voucher secrets in tokens/proofs.
- **cashu-mint**: depends on `cashu-voucher` for issuance/redemption; persists ledger state and emits Nostr ledger events; exposes APIs.
- **cashu-wallet**: depends on `cashu-voucher`/`cashu-lib`; manages voucher store, redemption, NIP-17 backups, NIP-33 ledger sync UI.
- **cashu-client**: SDK for merchants/operators wrapping voucher issuance/redemption and optional ledger queries.
- **Merchant/admin tooling**: builds on `cashu-client` for voucher management dashboards and audits.
