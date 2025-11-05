# Final Implementation Plan: Gift Card with Nostr (Revised)

**Date**: 2025-11-04
**Status**: Ready for Implementation
**NUT-13 Status**: ✅ Fully Complete (all 6 phases)

---

## Executive Summary

This is the **final implementation plan** incorporating three critical decisions:

1. ✅ **cashu-voucher as module within cashu-lib** (not standalone project)
2. ✅ **Model B only** (no redemption at mint - vouchers spendable only at issuing merchant)
3. ✅ **Full Nostr storage** (both ledger and wallet vouchers on Nostr, no database)

### Architecture Changes

**Before** (from previous analysis):
- Standalone `cashu-voucher` project (like bip-utils)
- Model A + Model B via config flag
- Database tables for voucher ledger
- Nostr for backups only

**After** (final decision):
- `cashu-lib/cashu-lib-vouchers/` module (4th module in cashu-lib)
- Model B only (mint rejects vouchers in swap/melt)
- **No database** - Nostr is source of truth for both ledger and wallet
- Simplified architecture

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [Architecture](#2-architecture)
3. [Nostr-First Design](#3-nostr-first-design)
4. [Model B Implementation](#4-model-b-implementation)
5. [Implementation Phases](#5-implementation-phases)
6. [Testing Strategy](#6-testing-strategy)
7. [Timeline](#7-timeline)

---

## 1. Project Structure

### 1.1 cashu-lib Module Layout

```
cashu-lib/
├── pom.xml                          # Parent POM
├── cashu-lib-entities/              # Core entities (Proof, BlindedMessage)
├── cashu-lib-crypto/                # Cryptographic primitives (BDHKE, DLEQ)
├── cashu-lib-common/                # Common utilities (Secret types, NUT-13 ✅)
└── cashu-lib-vouchers/              # NEW: Voucher domain module
    ├── pom.xml
    ├── src/main/java/xyz/tcheeric/cashu/voucher/
    │   ├── VoucherSecret.java       # Domain: extends BaseKey, implements Secret
    │   ├── SignedVoucher.java       # Domain: wrapper for secret + signature
    │   ├── VoucherSignatureService.java  # Signing/verification (ED25519)
    │   ├── VoucherValidator.java    # Validation logic (expiry, signature)
    │   ├── nostr/
    │   │   ├── VoucherLedgerEvent.java   # NIP-33 event wrapper
    │   │   └── VoucherBackupEvent.java   # NIP-17 event wrapper
    │   └── util/
    │       └── VoucherSerializationUtils.java
    └── src/test/java/
        └── 50+ unit tests
```

### 1.2 Parent POM Update

```xml
<!-- cashu-lib/pom.xml -->
<project>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-lib</artifactId>
    <version>0.5.0</version>
    <packaging>pom</packaging>

    <modules>
        <module>cashu-lib-entities</module>
        <module>cashu-lib-crypto</module>
        <module>cashu-lib-common</module>
        <module>cashu-lib-vouchers</module>  <!-- NEW -->
    </modules>
</project>
```

### 1.3 cashu-lib-vouchers POM

```xml
<!-- cashu-lib/cashu-lib-vouchers/pom.xml -->
<project>
    <parent>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-lib</artifactId>
        <version>0.5.0</version>
    </parent>

    <artifactId>cashu-lib-vouchers</artifactId>
    <name>Cashu Library - Vouchers</name>
    <description>Gift card voucher domain types and utilities</description>

    <dependencies>
        <!-- Internal dependencies -->
        <dependency>
            <groupId>xyz.tcheeric</groupId>
            <artifactId>cashu-lib-entities</artifactId>
        </dependency>
        <dependency>
            <groupId>xyz.tcheeric</groupId>
            <artifactId>cashu-lib-crypto</artifactId>
        </dependency>
        <dependency>
            <groupId>xyz.tcheeric</groupId>
            <artifactId>cashu-lib-common</artifactId>
        </dependency>

        <!-- Crypto -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk18on</artifactId>
        </dependency>

        <!-- Nostr (optional, for event wrappers) -->
        <dependency>
            <groupId>nostr</groupId>
            <artifactId>nostr-base</artifactId>
            <version>0.6.0</version>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 1.4 Dependency Graph

```
cashu-lib-vouchers
  ↓ depends on
cashu-lib-entities, cashu-lib-crypto, cashu-lib-common
  ↓ consumed by
cashu-wallet (VoucherService, VoucherBackupService)
  ↓ consumed by
cashu-mint (validation hooks - reject vouchers)
  ↓ consumed by
cashu-client (CLI commands)
```

**Key Benefits**:
- ✅ Single version number (0.5.0) for all cashu-lib modules
- ✅ Shared parent POM (dependency management, plugins)
- ✅ Published together as part of cashu-lib release
- ✅ No circular dependencies (vouchers is pure domain)

---

## 2. Architecture

### 2.1 Core Domain Types

#### VoucherSecret (Mirrors DeterministicSecret)

```java
package xyz.tcheeric.cashu.voucher;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import xyz.tcheeric.cashu.common.BaseKey;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.HexUtils;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gift card voucher secret.
 *
 * <p>Follows the same pattern as {@link xyz.tcheeric.cashu.common.DeterministicSecret}:
 * immutable, metadata-rich, implements {@link Secret} interface.
 *
 * <p>Vouchers are NOT deterministic (not derived from mnemonic) and MUST be backed up
 * to Nostr for recovery. They are spendable only at the issuing merchant (Model B).
 *
 * @see xyz.tcheeric.cashu.common.DeterministicSecret
 */
@Getter
public class VoucherSecret extends BaseKey implements Secret {
    private final String voucherId;
    private final String issuerId;
    private final String unit;
    private final long faceValue;
    private final Long expiresAt;
    private final String memo;

    /**
     * Private constructor - use factory methods.
     */
    private VoucherSecret(
        String voucherId,
        String issuerId,
        String unit,
        long faceValue,
        Long expiresAt,
        String memo
    ) {
        this.voucherId = voucherId;
        this.issuerId = issuerId;
        this.unit = unit;
        this.faceValue = faceValue;
        this.expiresAt = expiresAt;
        this.memo = memo;
    }

    /**
     * Factory method - creates voucher with generated ID.
     */
    public static VoucherSecret create(
        String issuerId,
        String unit,
        long faceValue,
        Long expiresAt,
        String memo
    ) {
        String voucherId = UUID.randomUUID().toString();
        return new VoucherSecret(voucherId, issuerId, unit, faceValue, expiresAt, memo);
    }

    /**
     * Factory method - creates voucher with explicit ID (for deserialization).
     */
    public static VoucherSecret create(
        String voucherId,
        String issuerId,
        String unit,
        long faceValue,
        Long expiresAt,
        String memo
    ) {
        return new VoucherSecret(voucherId, issuerId, unit, faceValue, expiresAt, memo);
    }

    /**
     * Canonical serialization for signing.
     *
     * <p>Uses deterministic JSON ordering (alphabetical keys) and CBOR encoding
     * to ensure identical signatures for identical vouchers.
     *
     * @return CBOR-encoded canonical bytes
     */
    public byte[] toCanonicalBytes() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (expiresAt != null) map.put("expiresAt", expiresAt);
        map.put("faceValue", faceValue);
        map.put("issuerId", issuerId);
        if (memo != null) map.put("memo", memo);
        map.put("unit", unit);
        map.put("voucherId", voucherId);

        return JsonUtils.toCbor(map);
    }

    @Override
    @JsonValue
    public String toHex() {
        return HexUtils.encode(toCanonicalBytes());
    }

    @Override
    public byte[] toBytes() {
        return toCanonicalBytes();
    }

    @Override
    public byte[] getData() {
        return toCanonicalBytes();
    }

    @Override
    public void setData(byte[] data) {
        throw new UnsupportedOperationException("VoucherSecret is immutable");
    }

    /**
     * Check if voucher is expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().getEpochSecond() > expiresAt;
    }

    /**
     * Check if voucher is valid (not expired).
     */
    public boolean isValid() {
        return !isExpired();
    }
}
```

#### SignedVoucher

```java
package xyz.tcheeric.cashu.voucher;

import lombok.Getter;

/**
 * Voucher with issuer signature.
 *
 * <p>Separates data (VoucherSecret) from authentication (signature).
 * This follows the same pattern as DeterministicSecret which stores
 * derivation path separately from secret data.
 */
@Getter
public class SignedVoucher {
    private final VoucherSecret secret;
    private final byte[] issuerSignature;
    private final String issuerPublicKey;

    public SignedVoucher(
        VoucherSecret secret,
        byte[] issuerSignature,
        String issuerPublicKey
    ) {
        this.secret = secret;
        this.issuerSignature = issuerSignature;
        this.issuerPublicKey = issuerPublicKey;
    }

    /**
     * Verify issuer signature.
     */
    public boolean verify() {
        return VoucherSignatureService.verify(
            secret,
            issuerSignature,
            issuerPublicKey
        );
    }

    /**
     * Check if voucher is expired.
     */
    public boolean isExpired() {
        return secret.isExpired();
    }

    /**
     * Check if voucher is valid (signature + expiry).
     */
    public boolean isValid() {
        return verify() && !isExpired();
    }
}
```

#### VoucherSignatureService

```java
package xyz.tcheeric.cashu.voucher;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import xyz.tcheeric.cashu.common.util.HexUtils;

/**
 * Voucher signature service using ED25519 (Nostr-compatible).
 */
public class VoucherSignatureService {

    /**
     * Sign a voucher secret with issuer private key.
     *
     * @param secret VoucherSecret to sign
     * @param issuerPrivateKeyHex Issuer private key (32 bytes hex, Nostr format)
     * @return Signature (64 bytes)
     */
    public static byte[] sign(VoucherSecret secret, String issuerPrivateKeyHex) {
        byte[] privateKeyBytes = HexUtils.decode(issuerPrivateKeyHex);
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(privateKeyBytes, 0);

        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);

        byte[] message = secret.toCanonicalBytes();
        signer.update(message, 0, message.length);

        return signer.generateSignature();
    }

    /**
     * Verify voucher signature.
     *
     * @param secret VoucherSecret
     * @param signature Signature bytes (64 bytes)
     * @param issuerPublicKeyHex Issuer public key (32 bytes hex, Nostr format)
     * @return true if signature is valid
     */
    public static boolean verify(
        VoucherSecret secret,
        byte[] signature,
        String issuerPublicKeyHex
    ) {
        try {
            byte[] publicKeyBytes = HexUtils.decode(issuerPublicKeyHex);
            Ed25519PublicKeyParameters publicKey = new Ed25519PublicKeyParameters(publicKeyBytes, 0);

            Ed25519Signer verifier = new Ed25519Signer();
            verifier.init(false, publicKey);

            byte[] message = secret.toCanonicalBytes();
            verifier.update(message, 0, message.length);

            return verifier.verifySignature(signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create a SignedVoucher.
     */
    public static SignedVoucher createSigned(
        VoucherSecret secret,
        String issuerPrivateKeyHex,
        String issuerPublicKeyHex
    ) {
        byte[] signature = sign(secret, issuerPrivateKeyHex);
        return new SignedVoucher(secret, signature, issuerPublicKeyHex);
    }
}
```

#### VoucherValidator

```java
package xyz.tcheeric.cashu.voucher;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates vouchers (expiry, signature).
 */
public class VoucherValidator {

    /**
     * Validation result.
     */
    @Getter
    @AllArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }

        public static ValidationResult failure(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new ValidationResult(false, errors);
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
    }

    /**
     * Validate a signed voucher.
     */
    public static ValidationResult validate(SignedVoucher voucher) {
        List<String> errors = new ArrayList<>();

        // Check signature
        if (!voucher.verify()) {
            errors.add("Invalid issuer signature");
        }

        // Check expiry
        if (voucher.isExpired()) {
            errors.add("Voucher has expired");
        }

        // Check face value
        if (voucher.getSecret().getFaceValue() <= 0) {
            errors.add("Invalid face value: must be positive");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(errors);
        }
    }
}
```

---

## 3. Nostr-First Design

### 3.1 Architecture Decision

**Decision**: Nostr is the **source of truth** for both voucher ledger and wallet storage.

**Rationale**:
- ✅ No database required (simpler deployment)
- ✅ Decentralized (no single point of failure)
- ✅ Censorship-resistant (multiple relays)
- ✅ Built-in replication (relays sync events)
- ✅ Backup is automatic (data stored on relays)

**Trade-offs**:
- ⚠️ Eventual consistency (not strongly consistent)
- ⚠️ Query performance (NIP-50 full-text search not universal)
- ⚠️ Relay dependency (need self-hosted relay for production)

### 3.2 Nostr Event Types

#### 3.2.1 Voucher Ledger Events (Public)

**NIP-33**: Parameterized Replaceable Events (kind 30000-39999)

```json
{
  "kind": 30078,
  "pubkey": "<mint_issuer_pubkey>",
  "created_at": 1736294400,
  "tags": [
    ["d", "voucher:<voucher-id>"],
    ["status", "issued"],
    ["issuer", "<issuer_id>"],
    ["unit", "sat"],
    ["value", "5000"],
    ["expires_at", "1743897600"],
    ["hash", "<sha256(voucher_secret)>"]
  ],
  "content": "{\"version\":1,\"memo\":\"Holiday gift\"}",
  "sig": "<signature>"
}
```

**State Transitions** (replaceable):
```json
// Initial issuance
{"tags": [["d", "voucher:123"], ["status", "issued"]]}

// After redemption (replaces previous event)
{"tags": [["d", "voucher:123"], ["status", "redeemed"]]}

// Revocation (replaces previous event)
{"tags": [["d", "voucher:123"], ["status", "revoked"]]}
```

**Query**:
```json
// Get current status of voucher
{
  "kinds": [30078],
  "authors": ["<mint_issuer_pubkey>"],
  "#d": ["voucher:123"]
}
```

#### 3.2.2 Wallet Voucher Backups (Private)

**NIP-17**: Private Direct Messages (kind 14) with NIP-44 encryption

```json
{
  "kind": 14,
  "pubkey": "<user_pubkey>",
  "created_at": 1736294400,
  "tags": [
    ["p", "<user_pubkey>"],
    ["subject", "cashu-voucher-backup"],
    ["e", "<previous_backup_event_id>"],
    ["client", "cashu-wallet"]
  ],
  "content": "<NIP-44 encrypted payload>",
  "sig": "<signature>"
}
```

**Encrypted Payload** (after decryption):
```json
{
  "version": 1,
  "timestamp": 1736294400,
  "backupCounter": 42,
  "vouchers": [
    {
      "voucherId": "123e4567-e89b-12d3-a456-426614174000",
      "issuerId": "merchant123",
      "unit": "sat",
      "faceValue": 5000,
      "expiresAt": 1743897600,
      "memo": "Holiday gift",
      "issuerSignature": "0x...",
      "issuerPublicKey": "0x...",
      "proof": { /* Full Proof<VoucherSecret> JSON */ },
      "blindingFactor": "0x...",
      "status": "ACTIVE",
      "receivedAt": 1736294000
    }
  ]
}
```

**Query**:
```json
// Get all backup events for user
{
  "kinds": [14],
  "authors": ["<user_pubkey>"],
  "#p": ["<user_pubkey>"],
  "#subject": ["cashu-voucher-backup"]
}
```

### 3.3 Nostr Event Wrappers (cashu-lib-vouchers)

```java
package xyz.tcheeric.cashu.voucher.nostr;

import lombok.Builder;
import lombok.Getter;
import nostr.event.impl.GenericEvent;
import nostr.event.impl.GenericTag;

import java.util.ArrayList;
import java.util.List;

/**
 * NIP-33 voucher ledger event wrapper.
 */
@Getter
@Builder
public class VoucherLedgerEvent {
    private String voucherId;
    private String status;  // issued, redeemed, revoked
    private String issuerId;
    private String unit;
    private long faceValue;
    private Long expiresAt;
    private String hash;
    private String memo;
    private String eventId;
    private long createdAt;

    /**
     * Convert to Nostr GenericEvent (kind 30078).
     */
    public GenericEvent toNostrEvent(String issuerPubkey) {
        GenericEvent event = new GenericEvent(issuerPubkey, 30078);

        List<GenericTag> tags = new ArrayList<>();
        tags.add(new GenericTag("d", "voucher:" + voucherId));
        tags.add(new GenericTag("status", status));
        tags.add(new GenericTag("issuer", issuerId));
        tags.add(new GenericTag("unit", unit));
        tags.add(new GenericTag("value", String.valueOf(faceValue)));
        if (expiresAt != null) {
            tags.add(new GenericTag("expires_at", String.valueOf(expiresAt)));
        }
        tags.add(new GenericTag("hash", hash));

        event.setTags(tags);

        // Content: memo as JSON
        String content = memo != null
            ? String.format("{\"version\":1,\"memo\":\"%s\"}", memo)
            : "{\"version\":1}";
        event.setContent(content);

        return event;
    }

    /**
     * Parse from Nostr GenericEvent.
     */
    public static VoucherLedgerEvent fromNostrEvent(GenericEvent event) {
        VoucherLedgerEventBuilder builder = VoucherLedgerEvent.builder();

        builder.eventId(event.getId());
        builder.createdAt(event.getCreatedAt());

        for (GenericTag tag : event.getTags()) {
            String key = tag.getCode();
            String value = tag.getValues().isEmpty() ? null : tag.getValues().get(0);

            switch (key) {
                case "d":
                    builder.voucherId(value.replace("voucher:", ""));
                    break;
                case "status":
                    builder.status(value);
                    break;
                case "issuer":
                    builder.issuerId(value);
                    break;
                case "unit":
                    builder.unit(value);
                    break;
                case "value":
                    builder.faceValue(Long.parseLong(value));
                    break;
                case "expires_at":
                    builder.expiresAt(Long.parseLong(value));
                    break;
                case "hash":
                    builder.hash(value);
                    break;
            }
        }

        // Parse memo from content JSON
        // TODO: Use Jackson to parse {"version":1,"memo":"..."}

        return builder.build();
    }
}
```

```java
package xyz.tcheeric.cashu.voucher.nostr;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * NIP-17 voucher backup payload (before encryption).
 */
@Getter
@Builder
public class VoucherBackupPayload {
    private int version;
    private long timestamp;
    private int backupCounter;
    private List<BackupVoucher> vouchers;

    @Getter
    @Builder
    public static class BackupVoucher {
        private String voucherId;
        private String issuerId;
        private String unit;
        private long faceValue;
        private Long expiresAt;
        private String memo;
        private String issuerSignature;
        private String issuerPublicKey;
        private String proofJson;  // Full Proof<VoucherSecret> as JSON
        private String blindingFactor;
        private String status;  // ACTIVE, SPENT, EXPIRED
        private long receivedAt;
    }

    /**
     * Serialize to JSON for encryption.
     */
    public String toJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    /**
     * Deserialize from JSON after decryption.
     */
    public static VoucherBackupPayload fromJson(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, VoucherBackupPayload.class);
    }
}
```

### 3.4 Relay Configuration

```yaml
# cashu-wallet/src/main/resources/application-voucher.yml
voucher:
  nostr:
    enabled: true
    relays:
      - wss://relay.damus.io
      - wss://relay.cashu.xyz
      - wss://relay.yourorg.com  # Self-hosted relay (recommended)
    backup:
      auto: true
      intervalMinutes: 60
    ledger:
      subscribeEnabled: true
      pollIntervalMinutes: 15
```

---

## 4. Model B Implementation

### 4.1 Model B Definition

**Model B**: Vouchers are **NOT redeemable at the mint**. They are **only spendable at the issuing merchant**.

**Characteristics**:
- ✅ Vouchers are rejected in mint swap/melt operations
- ✅ Merchant verifies vouchers offline (signature + expiry)
- ✅ Merchant marks vouchers as redeemed in their own system
- ✅ Mint acts as witness (publishes ledger events to Nostr)
- ✅ Regulatory: Gift card treatment (minimal compliance)

### 4.2 Mint Implementation (Reject Vouchers)

```java
// In cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/validators/

public class ProofValidator {

    /**
     * Validate proof for swap/melt operations.
     *
     * @throws VoucherNotAcceptedException if proof contains voucher (Model B)
     */
    public void validateProof(Proof proof) throws ValidationException {
        // Check if secret is a voucher
        if (proof.getSecret() instanceof VoucherSecret) {
            throw new VoucherNotAcceptedException(
                "Vouchers cannot be redeemed at mint (Model B). " +
                "Please redeem with issuing merchant."
            );
        }

        // ... existing validation logic ...
    }
}

public class VoucherNotAcceptedException extends ValidationException {
    public VoucherNotAcceptedException(String message) {
        super(message);
    }
}
```

### 4.3 Merchant Verification (Offline)

```java
// In cashu-client/src/main/java/xyz/tcheeric/wallet/merchant/

/**
 * Offline voucher verification tool for merchants.
 *
 * <p>Merchants can verify vouchers without contacting the mint:
 * 1. Verify issuer signature (cryptographic proof)
 * 2. Check expiry (timestamp)
 * 3. Optionally check Nostr ledger (online check for double-spend)
 */
public class MerchantVoucherVerifier {

    /**
     * Verify voucher offline (signature + expiry only).
     */
    public VerificationResult verifyOffline(SignedVoucher voucher, String expectedIssuerId) {
        List<String> errors = new ArrayList<>();

        // Check issuer matches
        if (!voucher.getSecret().getIssuerId().equals(expectedIssuerId)) {
            errors.add("Voucher issued by different merchant");
        }

        // Validate signature + expiry
        ValidationResult validation = VoucherValidator.validate(voucher);
        if (!validation.isValid()) {
            errors.addAll(validation.getErrors());
        }

        return new VerificationResult(errors.isEmpty(), errors);
    }

    /**
     * Verify voucher online (includes double-spend check via Nostr).
     */
    public VerificationResult verifyOnline(
        SignedVoucher voucher,
        String expectedIssuerId,
        NostrClient nostrClient
    ) {
        // First, offline verification
        VerificationResult offlineResult = verifyOffline(voucher, expectedIssuerId);
        if (!offlineResult.isValid()) {
            return offlineResult;
        }

        // Query Nostr ledger for current status
        VoucherLedgerEvent ledgerEvent = nostrClient.queryVoucherStatus(
            voucher.getSecret().getVoucherId()
        );

        if (ledgerEvent == null) {
            return VerificationResult.failure("Voucher not found in ledger");
        }

        if ("redeemed".equals(ledgerEvent.getStatus())) {
            return VerificationResult.failure("Voucher already redeemed (double-spend attempt)");
        }

        if ("revoked".equals(ledgerEvent.getStatus())) {
            return VerificationResult.failure("Voucher has been revoked");
        }

        return VerificationResult.success();
    }
}
```

### 4.4 Merchant Redemption Flow

```bash
# Merchant redeems voucher (updates local state + publishes to Nostr)
cashu merchant redeem <voucher-token>

# Steps:
# 1. Parse voucher token (Proof<VoucherSecret>)
# 2. Verify signature + expiry (offline)
# 3. Check Nostr ledger for status (online, optional)
# 4. Mark as redeemed in merchant's local database
# 5. Publish "redeemed" event to Nostr (if merchant has mint issuer key)
# 6. Display confirmation to customer
```

---

## 5. Implementation Phases

### Phase 0 – Foundation (Week 1, 5 days)

#### 0.1 Create cashu-lib-vouchers Module

**Tasks**:
1. ✅ Create `cashu-lib/cashu-lib-vouchers/` directory
2. ✅ Create `pom.xml` with dependencies
3. ✅ Update parent `cashu-lib/pom.xml` to include new module
4. ✅ Setup package structure: `xyz.tcheeric.cashu.voucher`
5. ✅ Configure CI/CD to build and test new module

**Deliverables**:
- [ ] `cashu-lib-vouchers/pom.xml`
- [ ] Parent POM updated with new module
- [ ] Empty package structure
- [ ] CI/CD passing (green build)

#### 0.2 Implement Core Domain Types

**Tasks**:
1. ✅ Implement `VoucherSecret` (following `DeterministicSecret` pattern)
2. ✅ Implement `SignedVoucher`
3. ✅ Implement `VoucherSignatureService` (ED25519)
4. ✅ Implement `VoucherValidator`
5. ✅ Write 50+ unit tests (80%+ coverage)

**Deliverables**:
- [ ] `VoucherSecret.java` (200 LOC)
- [ ] `SignedVoucher.java` (50 LOC)
- [ ] `VoucherSignatureService.java` (100 LOC)
- [ ] `VoucherValidator.java` (80 LOC)
- [ ] 50+ unit tests, 80%+ coverage

#### 0.3 Implement Nostr Event Wrappers

**Tasks**:
1. ✅ Implement `VoucherLedgerEvent` (NIP-33 wrapper)
2. ✅ Implement `VoucherBackupPayload` (NIP-17 payload)
3. ✅ Add serialization/deserialization tests
4. ✅ Test vectors for event parsing

**Deliverables**:
- [ ] `VoucherLedgerEvent.java` (150 LOC)
- [ ] `VoucherBackupPayload.java` (100 LOC)
- [ ] 20+ event parsing tests

**Total Phase 0**: 5 days, ~800 LOC, 70+ tests

---

### Phase 1 – Mint Integration (Week 2, 5 days)

#### 1.1 Voucher Issuance API

**Tasks**:
1. ✅ Create `POST /v1/vouchers` endpoint in `cashu-mint-rest`
2. ✅ Implement `VoucherIssuanceService` in `cashu-mint-protocol`
   - Generate voucher ID
   - Sign with mint's issuer key
   - Create `Proof<VoucherSecret>`
   - Publish to Nostr (NIP-33, status=issued)
3. ✅ Add request/response DTOs
4. ✅ Add OpenAPI/Swagger documentation

**API Specification**:
```http
POST /v1/vouchers
Content-Type: application/json

{
  "amount": 5000,
  "unit": "sat",
  "memo": "Holiday gift",
  "expiresInDays": 365
}

Response 200:
{
  "voucher": {
    "voucherId": "123e4567-e89b-12d3-a456-426614174000",
    "issuerId": "merchant123",
    "unit": "sat",
    "faceValue": 5000,
    "expiresAt": 1743897600,
    "memo": "Holiday gift"
  },
  "proof": { /* Proof<VoucherSecret> */ },
  "token": "cashuA..." // Serialized token
}
```

**Deliverables**:
- [ ] `/v1/vouchers` endpoint
- [ ] `VoucherIssuanceService.java`
- [ ] API integration tests (10 tests)

#### 1.2 Model B Validation (Reject Vouchers)

**Tasks**:
1. ✅ Update `ProofValidator` to detect `VoucherSecret` instances
2. ✅ Throw `VoucherNotAcceptedException` in swap/melt
3. ✅ Add error handling in REST layer
4. ✅ Add integration tests for rejection

**Deliverables**:
- [ ] Updated `ProofValidator.java`
- [ ] `VoucherNotAcceptedException.java`
- [ ] 5+ rejection tests (swap/melt)

#### 1.3 Nostr Ledger Service

**Tasks**:
1. ✅ Implement `NostrLedgerService` in `cashu-mint-protocol`
   - Publish voucher issued events (NIP-33)
   - Publish voucher redeemed events (NIP-33, merchant-triggered)
   - Query voucher status from Nostr
2. ✅ Add relay configuration
3. ✅ Add retry logic with exponential backoff

**Deliverables**:
- [ ] `NostrLedgerService.java`
- [ ] Relay configuration
- [ ] 10+ Nostr integration tests (mock relay)

**Total Phase 1**: 5 days, ~600 LOC, 25+ tests

---

### Phase 2 – Wallet Integration (Week 3, 5 days)

#### 2.1 Wallet Storage (Nostr-backed)

**Tasks**:
1. ✅ Extend `WalletState` with voucher fields (following NUT-13 pattern)
   ```java
   @JsonProperty("vouchers")
   private List<StoredVoucher> vouchers = new ArrayList<>();

   @JsonProperty("voucher_backup_state")
   private VoucherBackupState backupState;

   @JsonProperty("encrypted_nostr_privkey")
   private String encryptedNostrPrivkey;
   ```
2. ✅ Implement in-memory cache (local state syncs from Nostr on startup)
3. ✅ Add `StoredVoucher` DTO

**Deliverables**:
- [ ] `WalletState` voucher extensions
- [ ] `StoredVoucher.java`
- [ ] Storage tests (15 tests)

#### 2.2 Voucher Backup Service (Nostr)

**Tasks**:
1. ✅ Implement `VoucherBackupService` in `cashu-wallet-protocol`
   - `backup(SignedVoucher)` → encrypt + publish NIP-17 DM
   - `backupAll()` → backup all unsynced vouchers
   - `restore()` → query Nostr + decrypt + merge
   - `restoreWithKey(DeterministicKey)` → for NUT-13 integration
2. ✅ Use existing `DmCryptoNip44` from `wallet-infra-nip44`
3. ✅ Implement conflict resolution (latest timestamp wins)
4. ✅ Add monotonic backup counter for replay protection

**Deliverables**:
- [ ] `VoucherBackupService.java` (300 LOC)
- [ ] Backup/restore tests (20 tests)
- [ ] Mock Nostr relay tests

#### 2.3 Voucher Service (Business Logic)

**Tasks**:
1. ✅ Implement `VoucherService` in `cashu-wallet-protocol`
   - `issue(amount, memo, expiresAt)` → call mint API
   - `list(includeSpent)` → query local cache
   - `getStatus(voucherId)` → query Nostr ledger
   - Auto-backup after issuance
2. ✅ Add Nostr key management (derive from mnemonic or generate)

**Deliverables**:
- [ ] `VoucherService.java` (250 LOC)
- [ ] Service tests (15 tests)

**Total Phase 2**: 5 days, ~600 LOC, 50+ tests

---

### Phase 3 – CLI Commands (Week 4, 5 days)

#### 3.1 Voucher CLI Commands

**Commands** (in `cashu-client/wallet-cli`):

1. ✅ `IssueVoucherCmd` - Issue voucher + auto-backup
   ```bash
   cashu voucher issue --amount 5000 --memo "Gift" --expires-in-days 365
   ```

2. ✅ `ListVouchersCmd` - List vouchers with status
   ```bash
   cashu voucher list [--include-spent]
   ```

3. ✅ `ShowVoucherCmd` - Display voucher details + QR code
   ```bash
   cashu voucher show <voucher-id>
   ```

4. ✅ `BackupVouchersCmd` - Manual backup to Nostr
   ```bash
   cashu voucher backup
   ```

5. ✅ `RestoreVouchersCmd` - Restore from Nostr
   ```bash
   cashu voucher restore
   ```

6. ✅ `VoucherStatusCmd` - Query Nostr ledger status
   ```bash
   cashu voucher status <voucher-id>
   ```

7. ✅ `BackupStatusCmd` - Show backup status
   ```bash
   cashu voucher backup-status
   ```

**Deliverables**:
- [ ] 7 CLI command classes (50 LOC each)
- [ ] Command registration in `WalletMain`
- [ ] CLI tests (20 tests)

#### 3.2 Merchant CLI Commands

**Commands**:

1. ✅ `MerchantVerifyCmd` - Verify voucher (offline/online)
   ```bash
   cashu merchant verify <voucher-token> [--offline]
   ```

2. ✅ `MerchantRedeemCmd` - Mark voucher as redeemed
   ```bash
   cashu merchant redeem <voucher-token>
   ```

**Deliverables**:
- [ ] 2 merchant commands (80 LOC each)
- [ ] Merchant CLI tests (10 tests)

#### 3.3 NUT-13 Integration

**Task**: Enhance `RecoverWalletCmd` with `--include-vouchers` flag

```java
@Command(name = "recover")
public class RecoverWalletCmd extends WalletServiceCommand<Integer> {
    @Option(names = {"--include-vouchers"})
    private boolean includeVouchers;

    @Override
    protected Integer execute(WalletService walletService) throws Exception {
        // 1. Recover deterministic proofs (NUT-13)
        List<Proof> deterministicProofs = recoveryService.recover(mnemonic, passphrase, keysets);

        // 2. If requested, restore vouchers from Nostr
        if (includeVouchers) {
            DeterministicKey nostrKey = deriveNostrKey(mnemonic, passphrase);
            List<SignedVoucher> vouchers = voucherBackupService.restoreWithKey(nostrKey);
            System.out.println("✅ Restored " + vouchers.size() + " vouchers from Nostr");
        }

        return 0;
    }

    private DeterministicKey deriveNostrKey(String mnemonic, String passphrase) {
        // m/129372'/0'/1'/0 (NUT-13 base + branch 1 for Nostr)
        return Bip32.deriveKeyFromMnemonic(mnemonic, passphrase, "m/129372'/0'/1'/0");
    }
}
```

**Deliverables**:
- [ ] Enhanced `RecoverWalletCmd`
- [ ] Nostr key derivation from mnemonic
- [ ] Integration tests (5 tests)

**Total Phase 3**: 5 days, ~500 LOC, 35+ tests

---

### Phase 4 – Testing & Documentation (Week 5, 5 days)

#### 4.1 End-to-End Tests

**Test Scenarios**:
1. ✅ Issue voucher → auto-backup → verify Nostr event published
2. ✅ Delete local voucher → restore from Nostr → verify data integrity
3. ✅ Attempt to swap voucher at mint → verify rejection (Model B)
4. ✅ Merchant verify voucher → check signature + expiry
5. ✅ Query Nostr ledger → verify voucher status
6. ✅ NUT-13 recovery with `--include-vouchers` → verify vouchers restored

**Deliverables**:
- [ ] E2E test suite (Testcontainers + mock Nostr relay)
- [ ] 15+ E2E tests
- [ ] Test vector document

#### 4.2 Documentation

**Documents**:
1. ✅ User guide: "How to use vouchers"
2. ✅ Merchant guide: "Accepting vouchers (Model B)"
3. ✅ Developer guide: "cashu-lib-vouchers API reference"
4. ✅ Architecture document: "Nostr-first voucher design"
5. ✅ CLI reference: All voucher commands

**Deliverables**:
- [ ] 5 documentation files (Markdown)
- [ ] API JavaDoc (all public methods)
- [ ] README for cashu-lib-vouchers

#### 4.3 Performance Testing

**Tests**:
1. ✅ Backup 1000 vouchers → measure time
2. ✅ Restore 1000 vouchers → measure time
3. ✅ Query Nostr ledger → measure latency
4. ✅ Verify voucher signature → measure throughput

**Deliverables**:
- [ ] Performance test suite (JMH)
- [ ] Performance report

**Total Phase 4**: 5 days, 20+ E2E tests, 5 docs

---

## 6. Testing Strategy

### 6.1 Unit Tests (cashu-lib-vouchers)

**Coverage Target**: 80%+

**Test Categories**:
- Domain model (VoucherSecret, SignedVoucher)
- Signature service (sign/verify)
- Validation logic
- Serialization (JSON, CBOR, hex)
- Nostr event wrappers

**Tools**: JUnit 5, Mockito, AssertJ

### 6.2 Integration Tests

**Test Categories**:
- Mint API (issuance, rejection)
- Wallet services (backup, restore)
- Nostr relay interaction (mock relay)
- CLI commands (captured output)

**Tools**: Testcontainers, WireMock, mock Nostr relay

### 6.3 End-to-End Tests

**Test Flow**:
```java
@Test
void testFullVoucherLifecycle() {
    // 1. Issue voucher via REST API
    SignedVoucher voucher = mintClient.issueVoucher(5000, "Gift", 365);

    // 2. Verify Nostr ledger event published
    VoucherLedgerEvent ledgerEvent = nostrClient.queryVoucherStatus(voucher.getSecret().getVoucherId());
    assertEquals("issued", ledgerEvent.getStatus());

    // 3. Wallet auto-backups voucher to Nostr
    Thread.sleep(1000);  // Wait for async backup

    // 4. Delete local voucher
    walletService.deleteVoucher(voucher.getSecret().getVoucherId());

    // 5. Restore from Nostr
    List<SignedVoucher> restored = voucherBackupService.restore();
    assertEquals(1, restored.size());
    assertEquals(voucher.getSecret().getVoucherId(), restored.get(0).getSecret().getVoucherId());

    // 6. Attempt to swap at mint (Model B)
    assertThrows(VoucherNotAcceptedException.class, () -> {
        mintClient.swap(List.of(voucher.toProof()), ...);
    });

    // 7. Merchant verifies voucher
    VerificationResult result = merchantVerifier.verifyOnline(voucher, "merchant123", nostrClient);
    assertTrue(result.isValid());
}
```

---

## 7. Timeline

### 7.1 Phase Schedule

| Phase | Duration | Tasks | LOC | Tests | Deliverables |
|-------|----------|-------|-----|-------|--------------|
| **Phase 0** | Week 1 (5d) | Foundation | 800 | 70 | cashu-lib-vouchers module |
| **Phase 1** | Week 2 (5d) | Mint integration | 600 | 25 | Issuance API, Model B rejection |
| **Phase 2** | Week 3 (5d) | Wallet integration | 600 | 50 | Backup service, storage |
| **Phase 3** | Week 4 (5d) | CLI commands | 500 | 35 | 9 commands, NUT-13 integration |
| **Phase 4** | Week 5 (5d) | Testing & docs | - | 20 | E2E tests, documentation |
| **Total** | **5 weeks** | **25 days** | **2500** | **200** | Production-ready |

### 7.2 Detailed Schedule

**Week 1** (Phase 0 - Foundation):
- Day 1-2: Create module, implement VoucherSecret
- Day 3: Implement SignedVoucher, VoucherSignatureService
- Day 4: Implement VoucherValidator, unit tests
- Day 5: Implement Nostr event wrappers, tests

**Week 2** (Phase 1 - Mint):
- Day 1-2: Voucher issuance API
- Day 3: Model B validation (rejection)
- Day 4: Nostr ledger service
- Day 5: Integration tests

**Week 3** (Phase 2 - Wallet):
- Day 1-2: Wallet storage, WalletState extensions
- Day 3-4: Voucher backup service (Nostr)
- Day 5: Voucher service (business logic)

**Week 4** (Phase 3 - CLI):
- Day 1-2: Wallet CLI commands (7 commands)
- Day 3: Merchant CLI commands (2 commands)
- Day 4: NUT-13 integration
- Day 5: CLI tests

**Week 5** (Phase 4 - Polish):
- Day 1-2: E2E tests
- Day 3-4: Documentation
- Day 5: Performance testing, final review

---

## 8. Success Criteria

### 8.1 Phase 0 Complete

- [x] `cashu-lib-vouchers` module exists
- [x] VoucherSecret, SignedVoucher, VoucherSignatureService implemented
- [x] 70+ unit tests passing
- [x] 80%+ code coverage
- [x] Published as part of cashu-lib 0.5.0

### 8.2 Phase 1 Complete

- [x] `POST /v1/vouchers` API working
- [x] Model B rejection working (vouchers rejected in swap/melt)
- [x] Nostr ledger events published (NIP-33)
- [x] 25+ integration tests passing

### 8.3 Phase 2 Complete

- [x] Vouchers stored in WalletState
- [x] Auto-backup to Nostr working (NIP-17 + NIP-44)
- [x] Restore from Nostr working
- [x] 50+ tests passing

### 8.4 Phase 3 Complete

- [x] 9 CLI commands working
- [x] `cashu wallet recover --include-vouchers` working
- [x] Nostr key derived from mnemonic
- [x] 35+ CLI tests passing

### 8.5 Phase 4 Complete

- [x] 20+ E2E tests passing
- [x] 5 documentation files complete
- [x] Performance benchmarks documented
- [x] Production-ready

---

## 9. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| NIP-17/NIP-78 not in wallet-plugin | 🟡 Medium | Low | Implement as extension of existing Nostr infra (3 days) |
| Nostr relay downtime | 🟡 Medium | Medium | Self-hosted relay + offline queue + retry logic |
| User loses Nostr key | 🔴 High | Medium | Derive from mnemonic (m/129372'/0'/1'/0) |
| Voucher backup failures | 🟡 Medium | Low | Retry with exponential backoff, alert user |
| Model B merchant confusion | 🟢 Low | Medium | Clear documentation, error messages |
| cashu-lib version conflicts | 🟢 Low | Low | Single version for all modules (0.5.0) |

---

## 10. Appendix

### 10.1 Key Differences from Previous Plan

| Aspect | Previous Plan | Final Plan |
|--------|---------------|------------|
| **Project Structure** | Standalone cashu-voucher | Module within cashu-lib |
| **Database** | PostgreSQL tables | No database (Nostr only) |
| **Model** | Model A + Model B (config) | Model B only |
| **Voucher Storage** | Database + Nostr backup | Nostr only (source of truth) |
| **Complexity** | Higher (DB migrations, dual storage) | Lower (Nostr-first) |
| **Deployment** | Requires DB setup | Requires relay access only |

### 10.2 Nostr Relay Recommendations

**Production Deployment**:
1. ✅ Self-hosted relay (required for reliability)
   - Use `nostr-rs-relay` or `strfry`
   - Configure retention policies (keep voucher events indefinitely)
2. ✅ Public relays (redundancy)
   - wss://relay.damus.io
   - wss://relay.primal.net
3. ✅ Backup strategy
   - Periodic export of voucher events
   - Offline replay capability

### 10.3 Future Enhancements (Out of Scope)

- **Model A support** (voucher redemption at mint) - Requires regulatory compliance
- **Multi-merchant vouchers** (usable at multiple merchants)
- **Fractional redemption** (redeem part of voucher value)
- **Voucher transfer** (P2P voucher gifting)
- **Advanced queries** (NIP-50 full-text search)

---

**Document Status**: ✅ Final - Ready for Implementation
**Last Updated**: 2025-11-04
**Approvals**: Architecture review complete
**Next Action**: Begin Phase 0 (Week 1, Day 1)
