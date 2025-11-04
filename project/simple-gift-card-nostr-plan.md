# Implementation Plan – Use Case 1 (Simple Gift Card) with Nostr NIP-17 Backup

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
- Finalize voucher schema & canonicalization.
- Add configuration toggles for redemption mode and Nostr backup.

### Phase 1 – Mint & Wallet Voucher Flows
- Implement issuance API, voucher ledger, redemption validation.
- Update wallet issuance/redeem flows, storage, tests.

### Phase 2 – Nostr Backup (NIP-17)
- Integrate Nostr client library with NIP-44 support.
- Implement backup serialization, encryption, DM construction, publishing.
- Add restore command/path with conflict resolution.

### Phase 3 – UX & Ops
- Add CLI/GUI prompts for backups, status indicators.
- Update docs (`docs/how-to/vouchers.md`, `docs/reference/voucher-backup.md`).
- Monitoring: log backup events, alert on failures.

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
