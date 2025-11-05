# NUT-13 Implementation Plan

## Table of Contents

1. [Overview](#overview)
2. [Project Summary](#project-summary)
3. [Phase Summary](#phase-summary)
4. [NUT-13 Specification Summary](#nut-13-specification-summary)
   - [Purpose](#purpose)
   - [BIP32 Derivation Path Structure](#bip32-derivation-path-structure)
   - [Wallet Recovery Process](#wallet-recovery-process)
5. [Phase 1: Complete cashu-lib Foundation](#phase-1-complete-cashu-lib-foundation)
   - [1.1 Finish Derivation Path Classes](#11-finish-derivation-path-classes--already-started)
   - [1.2 Create Deterministic Secret Type](#12-create-deterministic-secret-type)
   - [1.3 Create Secret Factory Enhancement](#13-create-secret-factory-enhancement)
   - [1.4 Add Dependency on bip-utils](#14-add-dependency-on-bip-utils)
6. [Phase 2: Wallet Implementation (cashu-wallet)](#phase-2-wallet-implementation-cashu-wallet)
   - [2.1 Create Wallet Recovery Service](#21-create-wallet-recovery-service)
   - [2.2 Create Deterministic Secret Generator Task](#22-create-deterministic-secret-generator-task)
   - [2.3 Create Recovery Request Builder](#23-create-recovery-request-builder)
   - [2.4 Add Recovery Client Endpoint](#24-add-recovery-client-endpoint)
   - [2.5 Create Proof Recovery Service](#25-create-proof-recovery-service)
7. [Phase 3: Mint Implementation (cashu-mint)](#phase-3-mint-implementation-cashu-mint)
   - [3.1 Enhance Signature Vault Storage](#31-enhance-signature-vault-storage)
   - [3.2 Verify NUT-09 Compatibility](#32-verify-nut-09-compatibility)
   - [3.3 Update NUT-09 Documentation](#33-update-nut-09-documentation)
   - [3.4 Consider Database Schema Enhancement](#34-consider-database-schema-enhancement)
8. [Phase 4: Integration & Testing](#phase-4-integration--testing)
   - [4.1 Create Integration Tests](#41-create-integration-tests)
   - [4.2 Create End-to-End Recovery Test](#42-create-end-to-end-recovery-test)
   - [4.3 Update Documentation](#43-update-documentation)
9. [Phase 5: Optional Enhancements](#phase-5-optional-enhancements)
   - [5.1 Mnemonic Management Utilities](#51-mnemonic-management-utilities)
   - [5.2 Counter/State Management](#52-counterstate-management)
   - [5.3 Multi-Keyset Recovery Optimization](#53-multi-keyset-recovery-optimization)
10. [Phase 6: CLI Implementation (cashu-cli)](#phase-6-cli-implementation-cashu-cli)
    - [6.1 Create GenerateMnemonicCmd](#61-create-generatemnemonicCmd)
    - [6.2 Create RecoverWalletCmd](#62-create-recoverwalletcmd)
    - [6.3 Create ShowMnemonicCmd](#63-create-showmnemonicCmd)
    - [6.4 Create BackupWalletCmd](#64-create-backupwalletcmd)
    - [6.5 Enhance InitCmd for Deterministic Mode](#65-enhance-initcmd-for-deterministic-mode)
    - [6.6 Add Mnemonic Storage to WalletState](#66-add-mnemonic-storage-to-walletstate)
    - [6.7 Write CLI Tests](#67-write-cli-tests)
11. [Implementation & Supporting Documentation](#implementation--supporting-documentation)
    - [11.1 Implementation Order Recommendation](#implementation-order-recommendation)
    - [11.2 Key Design Decisions](#key-design-decisions)
    - [11.3 Dependencies & Prerequisites](#dependencies--prerequisites)
    - [11.4 bip-utils Integration Guide](#bip-utils-integration-guide)
      - [Overview](#overview-1)
      - [Integration by Project](#integration-by-project)
      - [Common Patterns](#common-patterns)
      - [Helper Utilities Available](#helper-utilities-available)
      - [Testing with bip-utils](#testing-with-bip-utils)
      - [Security Best Practices](#security-best-practices-1)
      - [Migration Path](#migration-path)
    - [11.5 Security Considerations](#security-considerations)
    - [11.6 Testing Strategy](#testing-strategy)
    - [11.7 Future Enhancements](#future-enhancements)
12. [References](#references)

---

## Overview
NUT-13 enables deterministic secret derivation for wallet recovery using BIP39 mnemonic phrases and BIP32 derivation paths. The implementation spans three projects:
- **bip-utils**: Already complete with `Nut13Derivation` utilities
- **cashu-lib**: Core entities and derivation path support (partially started)
- **cashu-wallet**: Deterministic secret generation for wallet operations
- **cashu-mint**: Recovery/restore endpoint enhancements

**Specification**: https://github.com/cashubtc/nuts/blob/main/13.md

---

## Project Summary

| Metric | Value |
|--------|-------|
| **Total Tasks** | 36 tasks (31 required + 5 optional) |
| **Completed Tasks** | 6 tasks (17%) |
| **In Progress** | Phase 1 - cashu-lib Foundation |
| **Estimated Duration** | 9-14 days |
| **Start Date** | 2025-11-03 |
| **Target Completion** | TBD |
| **Primary Developer** | TBD |
| **Reviewers** | TBD |
| **Branch** | NUT13 |
| **Related PRs** | TBD |
| **Blocked Items** | None |
| **Risk Level** | Low - Most infrastructure already exists |
| **Breaking Changes** | None - Backward compatible |
| **Documentation Required** | Yes - README, JavaDoc, Examples |
| **Testing Coverage Target** | 80%+ |

### Project Scope
- **In Scope**: BIP39/BIP32 deterministic secret derivation, wallet recovery via NUT-09, CLI commands, testing, documentation
- **Out of Scope**: Hardware wallet integration, multi-currency paths, advanced UI/UX, GUI applications
- **Dependencies**: bip-utils (complete), bitcoinj, existing NUT-09 implementation, PicoCLI framework

### Key Milestones
1. ✓ Phase 1 Started - Derivation path classes complete
2. ⏳ Phase 1 Complete - Core library foundation ready
3. ⏳ Phase 2 Complete - Wallet recovery implementation
4. ⏳ Phase 3 Complete - Mint enhancements
5. ⏳ Phase 4 Complete - Full test coverage
6. ⏳ Phase 5 Complete - Optional enhancements (if time permits)
7. ⏳ Phase 6 Complete - CLI commands implemented and tested
8. ⏳ Production Ready - All tests passing, documentation complete

---

## Phase Summary

| Phase | Focus Area | Tasks | Estimated Time | Status |
|-------|-----------|-------|----------------|--------|
| **Phase 1** | cashu-lib Foundation | 5 tasks | 1-2 days | In Progress (4/5 complete) |
| **Phase 2** | Wallet Implementation | 6 tasks | 2-3 days | Not Started |
| **Phase 3** | Mint Implementation | 5 tasks | 1 day | Partially Complete (1/5) |
| **Phase 4** | Integration & Testing | 8 tasks | 2-3 days | In Progress (1/8 complete) |
| **Phase 5** | Optional Enhancements | 5 tasks | 1-2 days | Not Started |
| **Phase 6** | CLI Implementation (cashu-cli) | 7 tasks | 1-2 days | Not Started |
| **Total** | All Phases | **36 tasks** | **9-14 days** | **17% Complete** |

### Task Table Legend

**ID Column:**
- Unique identifier for each task in format `{Phase}.{Task}`
- Used for cross-referencing in dependencies and discussions
- Example: "1.2" = Phase 1, Task 2

**Status Values:**
- ✓ Complete - Task finished and committed
- Pending - Not yet started
- In Progress - Currently being worked on
- Optional - Nice to have, not required for core functionality
- Blocked - Cannot proceed due to dependencies

**Task Size:**
- Small - < 4 hours
- Medium - 4-8 hours
- Large - > 8 hours (1+ days)

**Priority:**
- P0 - Critical, required for NUT-13 functionality
- P1 - Important, required for production quality
- P2 - Optional, enhancement only

**Dependency Format:**
- Task IDs (e.g., "1.1, 1.2") - Depends on specific tasks
- Phase names (e.g., "Phase 1") - Depends on entire phase completion
- "None" - No dependencies, can start immediately

**Commit:**
- Commit hash after task completion
- "TBD" - Complete but commit needs to be identified
- "-" - Not yet committed

---

## NUT-13 Specification Summary

### Purpose
Enables wallet recovery through deterministic secret generation. Allows users to recover their ecash balance with the help of the mint using a familiar 12-word seed phrase.

### BIP32 Derivation Path Structure
```
m/129372'/0'/{keyset_id_int}'/{counter}'/0  (for secrets)
m/129372'/0'/{keyset_id_int}'/{counter}'/1  (for blinding factors)
```

**Path Components:**
- **129372'** – Purpose identifier (UTF-8 for 🥜 peanut emoji)
- **0'** – Coin type (always zero, independent of ecash unit)
- **{keyset_id_int}'** – Integer representation of the keyset ID (hex to int, mod 2^31 - 1)
- **{counter}'** – Incremented counter per keyset
- **0 or 1** – Distinguishes secret from blinding factor

### Wallet Recovery Process
1. Generate secrets and blinding factors using derivation paths
2. Create BlindedMessages from derived secrets
3. Request BlindSignatures from mint via NUT-09 (/restore endpoint)
4. Unblind signatures to recover Proofs
5. Verify spent status using NUT-07
6. Restore in batches of 100 tokens
7. Continue until three successive batches are returned empty by the mint

---

## Phase 1: Complete cashu-lib Foundation

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 1.1 | Finish Derivation Path Classes | ✓ Complete | Small | P0 | None | `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/` | TBD | Foundation complete |
| 1.2 | Create DeterministicSecret | ✓ Complete | Medium | P0 | 1.1 | `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/DeterministicSecret.java` | TBD | Implements Secret interface with metadata support |
| 1.3 | Enhance SecretFactory | ✓ Complete | Small | P0 | 1.2, 1.4 | `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/util/SecretFactory.java` | TBD | Added 4 deterministic methods + SecretAndBlindingFactor record |
| 1.4 | Add bip-utils Dependency | ✓ Complete | Small | P0 | None | `cashu-lib/pom.xml` | TBD | Added bip-utils 2.0.0 + repository configuration |
| 1.5 | Write Unit Tests | Pending | Medium | P1 | 1.2, 1.3 | `cashu-lib-common/src/test/java/xyz/tcheeric/cashu/common/` | - | JUnit tests for new classes |

### 1.1 Finish Derivation Path Classes ✓ (Already Started)
**Location**: `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/`

Already implemented:
- `DerivationPath.java` - Base class with path formatting (m/129372'/0'/{keysetId}'/counter/suffix)
- `SecretDerivationPath.java` - For secrets (suffix=0)
- `RDerivationPath.java` - For blinding factors (suffix=1)
- `KeysetId.java` - Already has `toInt()` method for path conversion

**Status**: Foundation is complete ✓

**Optional Enhancement - bip-utils Integration**:
While the current implementation works correctly, it could be enhanced to use bip-utils constants and utilities for consistency:

```java
// DerivationPath.java - Optional enhancement
import xyz.tcheeric.bips.bip32.nut.Nut13Derivation;

public class DerivationPath {
    // Use bip-utils constants instead of hardcoded values
    private static final int CASHU_PURPOSE = Nut13Derivation.NUT13_PURPOSE;  // 129372
    private static final int CASHU_COIN_TYPE = Nut13Derivation.COIN_TYPE;    // 0

    @Override
    public String toString() {
        // Option 1: Keep current implementation (works fine)
        return String.format("m/%d'/%d'/%d'/%d'/%d",
            CASHU_PURPOSE, CASHU_COIN_TYPE, keysetId.toInt(), counter, suffix);

        // Option 2: Use bip-utils path builder (alternative)
        // if (suffix == 0) {
        //     return Nut13Derivation.buildSecretDerivationPath(keysetId.toInt(), counter);
        // } else {
        //     return Nut13Derivation.buildBlindingFactorDerivationPath(keysetId.toInt(), counter);
        // }
    }
}
```

**Note**: This enhancement is **optional** and not required for NUT-13 functionality. The current implementation is correct and functional. Using bip-utils constants would just ensure consistency if the NUT-13 specification constants ever change.

### 1.2 Create Deterministic Secret Type ✓ (Complete)
**New File**: `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/DeterministicSecret.java`

**Status**: Implementation complete ✓

Created a new `Secret` implementation that:
- Stores the deterministic secret bytes (derived from BIP32)
- Optionally stores metadata: derivation path, counter, keysetId
- Implements `Secret` interface (getData(), setData(), toBytes())
- Serializes to hex string (like RandomStringSecret)
- Includes factory methods for easy construction

**Implementation Details**:
- Extends `BaseKey` for hex encoding functionality
- Immutable once created (setData() throws UnsupportedOperationException)
- Includes derivation path information for debugging
- JSON serialization via `@JsonValue` annotation
- Comprehensive unit tests (21 tests, all passing ✓)

**bip-utils Integration**:
`DeterministicSecret` is a **data container only** and does not perform cryptographic derivation. It accepts pre-derived bytes from factory methods. The actual secret derivation using bip-utils will be implemented in:
- **SecretFactory** (Task 1.3) - Uses `Nut13Derivation.deriveSecret()`
- **DeriveSecretsTask** (Task 2.2) - Uses `Nut13Derivation.deriveSecretAndBlindingFactor()`

**Reference Implementation**: Similar to `RandomStringSecret.java` but:
- Constructor takes derived bytes + metadata instead of generating random
- Immutable once created
- Includes derivation path information for debugging

**Files Created**:
- `DeterministicSecret.java` - Main class
- `DeterministicSecretDeserializer.java` - JSON deserializer
- `DeterministicSecretTest.java` - 21 comprehensive tests
- Updated `Secret.java` - Added DeterministicSecret case to fromString()

### 1.3 Create Secret Factory Enhancement
**Modify**: `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/util/SecretFactory.java`

Add methods for deterministic secret generation using bip-utils:
```java
import xyz.tcheeric.bips.bip32.nut.Nut13Derivation;
import org.bitcoinj.crypto.DeterministicKey;

public class SecretFactory {

    /**
     * Creates a deterministic secret using NUT-13 derivation.
     *
     * @param masterKey BIP32 master key from mnemonic
     * @param keysetId Keyset ID (hex string)
     * @param counter Counter value for derivation
     * @return DeterministicSecret with derived bytes and metadata
     */
    public static DeterministicSecret createDeterministic(
        DeterministicKey masterKey,
        KeysetId keysetId,
        int counter
    ) {
        // Use Nut13Derivation to derive the secret bytes
        byte[] secretBytes = Nut13Derivation.deriveSecret(
            masterKey,
            keysetId.toString(),
            counter
        );

        return DeterministicSecret.create(secretBytes, keysetId, counter);
    }

    /**
     * Creates a batch of deterministic secrets for a keyset.
     *
     * @param masterKey BIP32 master key
     * @param keysetId Keyset ID
     * @param startCounter Starting counter value
     * @param count Number of secrets to generate
     * @return List of deterministic secrets with sequential counters
     */
    public static List<DeterministicSecret> createDeterministicBatch(
        DeterministicKey masterKey,
        KeysetId keysetId,
        int startCounter,
        int count
    ) {
        List<DeterministicSecret> secrets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            secrets.add(createDeterministic(masterKey, keysetId, startCounter + i));
        }
        return secrets;
    }

    /**
     * Creates a deterministic secret directly from mnemonic (convenience method).
     *
     * @param mnemonic BIP39 mnemonic phrase
     * @param passphrase Optional passphrase (use "" for none)
     * @param keysetId Keyset ID
     * @param counter Counter value
     * @return DeterministicSecret
     */
    public static DeterministicSecret createDeterministicFromMnemonic(
        String mnemonic,
        String passphrase,
        KeysetId keysetId,
        int counter
    ) {
        var params = Nut13Derivation.Nut13DerivationParams.builder()
            .mnemonicPhrase(mnemonic)
            .passphrase(passphrase)
            .keysetIdHex(keysetId.toString())
            .counter(counter)
            .build();

        byte[] secretBytes = Nut13Derivation.deriveSecretFromMnemonic(params);
        return DeterministicSecret.create(secretBytes, keysetId, counter);
    }
}
```

**Integration Points**:
- Uses `Nut13Derivation.deriveSecret()` for secret derivation
- Uses `Nut13Derivation.deriveSecretFromMnemonic()` for all-in-one derivation
- Uses `Nut13DerivationParams.builder()` for clean parameter passing
- Returns `DeterministicSecret` instances with proper metadata

### 1.4 Add Dependency on bip-utils
**Modify**: `cashu-lib/pom.xml` or `build.gradle`

Add dependency:
```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>bip-utils</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Integration Points**:
- Use `Nut13Derivation.deriveSecretAndBlindingFactor()`
- Use `Nut13Derivation.deriveSecretFromMnemonic()`
- Use `Bip39.mnemonicToMasterKey()` for master key derivation

---

## Phase 2: Wallet Implementation (cashu-wallet)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 2.1 | Create WalletRecoveryService | Pending | Large | P0 | Phase 1, 2.2, 2.3, 2.4, 2.5 | `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/service/WalletRecoveryService.java` | - | Orchestrates full recovery flow |
| 2.2 | Create DeriveSecretsTask | Pending | Medium | P0 | Phase 1 | `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/tasks/DeriveSecretsTask.java` | - | Uses Nut13Derivation |
| 2.3 | Create RestoreRequestBuilder | Pending | Medium | P0 | Phase 1, 2.2 | `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/builders/RestoreRequestBuilder.java` | - | Builds PostRestoreRequest |
| 2.4 | Add RequestRestore Client | Pending | Medium | P0 | 2.3 | `cashu-wallet-client/src/main/java/xyz/tcheeric/cashu/wallet/client/impl/RequestRestore.java` | - | REST client for /restore |
| 2.5 | Create ProofRecoveryService | Pending | Medium | P0 | 2.4 | `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/service/ProofRecoveryService.java` | - | Unblinds and verifies proofs |
| 2.6 | Write Unit Tests | Pending | Large | P1 | 2.1-2.5 | `cashu-wallet-protocol/src/test/java/xyz/tcheeric/cashu/wallet/proto/` | - | Test all wallet components |

### 2.1 Create Wallet Recovery Service
**New File**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/service/WalletRecoveryService.java`

Service to orchestrate wallet recovery:
- Takes BIP39 mnemonic + passphrase
- Derives master key
- Iterates through keysets and counters
- Generates deterministic secrets and blinding factors
- Creates BlindedMessages for restore requests
- Sends batches of 100 to mint via NUT-09
- Continues until 3 consecutive empty batches

**Key Methods**:
```java
public interface WalletRecoveryService {
    /**
     * Recovers wallet from mnemonic phrase.
     * @param mnemonic BIP39 mnemonic (12-24 words)
     * @param passphrase Optional passphrase (empty string if none)
     * @param keysets List of keysets to recover from
     * @return List of recovered proofs
     */
    List<Proof<DeterministicSecret>> recover(
        String mnemonic,
        String passphrase,
        List<KeysetId> keysets
    );

    /**
     * Recovers a single keyset with counter range.
     * @param masterKey Master key from mnemonic
     * @param keysetId Keyset to recover
     * @param startCounter Starting counter (usually 0)
     * @return List of recovered proofs for this keyset
     */
    List<Proof<DeterministicSecret>> recoverKeyset(
        DeterministicKey masterKey,
        KeysetId keysetId,
        int startCounter
    );
}
```

### 2.2 Create Deterministic Secret Generator Task
**New File**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/tasks/DeriveSecretsTask.java`

Task pattern implementation for batch secret generation using bip-utils:

**Implementation**:
```java
import xyz.tcheeric.bips.bip32.nut.Nut13Derivation;
import org.bitcoinj.crypto.DeterministicKey;

public class DeriveSecretsTask implements Task<DeriveSecretsResult> {

    private final DeterministicKey masterKey;
    private final KeysetId keysetId;
    private final int startCounter;
    private final int count;

    public DeriveSecretsTask(
        DeterministicKey masterKey,
        KeysetId keysetId,
        int startCounter,
        int count
    ) {
        this.masterKey = masterKey;
        this.keysetId = keysetId;
        this.startCounter = startCounter;
        this.count = count;
    }

    @Override
    public DeriveSecretsResult execute() {
        List<DeterministicSecret> secrets = new ArrayList<>(count);
        List<byte[]> blindingFactors = new ArrayList<>(count);

        String keysetIdHex = keysetId.toString();

        for (int i = 0; i < count; i++) {
            int counter = startCounter + i;

            // Use Nut13Derivation to derive both secret and blinding factor
            var pair = Nut13Derivation.deriveSecretAndBlindingFactor(
                masterKey,
                keysetIdHex,
                counter
            );

            // Create DeterministicSecret with metadata
            DeterministicSecret secret = DeterministicSecret.create(
                pair.secret(),
                keysetId,
                counter
            );

            secrets.add(secret);
            blindingFactors.add(pair.blindingFactor());
        }

        return new DeriveSecretsResult(secrets, blindingFactors);
    }
}

public record DeriveSecretsResult(
    List<DeterministicSecret> secrets,
    List<byte[]> blindingFactors
) {
    public int getCount() {
        return secrets.size();
    }
}
```

**Integration Points**:
- Uses `Nut13Derivation.deriveSecretAndBlindingFactor()` for efficient derivation
- Returns both secrets and blinding factors in one operation
- Properly associates metadata with each secret

### 2.3 Create Recovery Request Builder
**New File**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/builders/RestoreRequestBuilder.java`

Utility to build `PostRestoreRequest`:
- Takes list of `DeterministicSecret` + blinding factors
- Creates `BlindedMessage` using BDHKE
- Builds batched restore requests (100 at a time)

**Key Methods**:
```java
public class RestoreRequestBuilder {
    /**
     * Creates blinded messages from secrets and blinding factors.
     */
    public List<BlindedMessage> createBlindedMessages(
        List<DeterministicSecret> secrets,
        List<byte[]> blindingFactors,
        int amount
    );

    /**
     * Builds restore request with batch limit.
     */
    public PostRestoreRequest buildRequest(
        List<BlindedMessage> blindedMessages
    );
}
```

### 2.4 Add Recovery Client Endpoint
**New File**: `cashu-wallet-client/src/main/java/xyz/tcheeric/cashu/wallet/client/impl/RequestRestore.java`

REST client wrapper for:
- Sending `PostRestoreRequest` to mint's `/restore` endpoint
- Receiving `PostRestoreResponse` with signatures
- Handling batch iteration logic

**Implementation Pattern**: Follow existing request classes like `RequestMintToken.java`

### 2.5 Create Proof Recovery Service
**New File**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/service/ProofRecoveryService.java`

Service to:
- Unblind returned signatures using stored blinding factors
- Create `Proof<DeterministicSecret>` objects
- Verify proofs using NUT-07 (check spent status)
- Return recovered, unspent proofs to user

**Key Methods**:
```java
public interface ProofRecoveryService {
    /**
     * Unblinds signatures and creates proofs.
     */
    List<Proof<DeterministicSecret>> unblindAndCreateProofs(
        PostRestoreResponse response,
        List<DeterministicSecret> secrets,
        List<byte[]> blindingFactors
    );
}
```

---

## Phase 3: Mint Implementation (cashu-mint)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 3.1 | Verify Signature Vault Compatibility | Pending | Small | P0 | None | `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/service/impl/DefaultSignatureVaultService.java` | - | Review and test existing code |
| 3.2 | Verify NUT-09 Compatibility | ✓ Compatible | Small | P0 | None | `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/nut/NUT09.java` | TBD | Already works with NUT-13 |
| 3.3 | Update NUT-09 Documentation | Pending | Small | P1 | 3.2 | `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/nut/NUT09.java` | - | Add comprehensive JavaDoc |
| 3.4 | Optional: Database Schema Enhancement | Optional | Medium | P2 | None | Database migration scripts | - | Analytics/debugging only |
| 3.5 | Add Recovery Logging | Pending | Small | P1 | 3.1, 3.2 | Various mint classes | - | Add SLF4J logging statements |

### 3.1 Enhance Signature Vault Storage
**File**: `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/service/impl/DefaultSignatureVaultService.java`

**Current Implementation**:
- Storage key: `blindedMessage.toString()`
- In-memory storage: `ConcurrentHashMap<String, BlindSignature>`

**Analysis**:
The current implementation should already support NUT-13 without changes because:
- Blinded messages are deterministic if secrets are deterministic
- Same secret + same blinding factor = same blinded message
- Storage by blinded message hash naturally supports recovery

**Recommendation**: No changes required initially. Monitor during testing.

**Optional Enhancement**: Add metadata storage for analytics
```java
public class SignatureMetadata {
    private BlindSignature signature;
    private Optional<DerivationPath> derivationPath; // For auditing
    private Instant createdAt;
}
```

### 3.2 Verify NUT-09 Compatibility
**File**: `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/nut/NUT09.java`

**Current Implementation**:
```java
public static PostRestoreResponse restore(
    @NonNull PostRestoreRequest request,
    @NonNull SignatureVaultService signatureVaultService
)
```

Delegates to `RestoreSignaturesTask` which:
1. Iterates through blinded messages in request
2. Looks up signatures in vault
3. Returns matching outputs and signatures

**Status**: Already compatible with NUT-13! ✓

**Enhancement Needed**: Add JavaDoc explaining NUT-13 compatibility

### 3.3 Update NUT-09 Documentation
**Modify**: `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/nut/NUT09.java`

Add comprehensive JavaDoc:
```java
/**
 * NUT-09: Restore signatures (wallet recovery).
 *
 * <p>This implementation supports both random and deterministic (NUT-13) secret recovery.
 * For deterministic recovery:
 * <ul>
 *   <li>Wallets derive secrets from BIP39 mnemonic using BIP32 paths</li>
 *   <li>Wallets create blinded messages from derived secrets</li>
 *   <li>Mint returns signatures for any blinded messages found in vault</li>
 *   <li>Wallets should batch requests (recommended: 100 tokens per batch)</li>
 *   <li>Recovery continues until 3 consecutive empty batches</li>
 * </ul>
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/09.md">NUT-09 Specification</a>
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/13.md">NUT-13 Specification</a>
 */
```

### 3.4 Consider Database Schema Enhancement
**Optional**: Modify `DBProofVault` schema

**Current Schema** (approximate):
```sql
CREATE TABLE proofs (
    secret VARCHAR PRIMARY KEY,
    amount INT,
    keyset_id VARCHAR,
    signature VARCHAR,
    state VARCHAR, -- valid, spent, archived
    ...
);
```

**Optional Enhancement**:
```sql
ALTER TABLE proofs ADD COLUMN derivation_counter INT NULL;
ALTER TABLE proofs ADD COLUMN is_deterministic BOOLEAN DEFAULT FALSE;
```

**Benefits**:
- Analytics on deterministic vs. random secrets
- Gap analysis for recovery optimization
- Debugging and auditing

**Note**: Not required for NUT-13 functionality, only for observability.

---

## Phase 4: Integration & Testing

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 4.1 | Create cashu-lib Tests | ✓ Complete | Medium | P0 | Phase 1 | `cashu-lib-common/src/test/java/xyz/tcheeric/cashu/protocol/NUT13IntegrationTest.java` | TBD | 40 comprehensive tests - all passing ✓ |
| 4.2 | Create Wallet Tests | Pending | Large | P0 | Phase 2 | `cashu-wallet-protocol/src/test/java/xyz/tcheeric/cashu/wallet/proto/WalletRecoveryTest.java` | - | Test recovery services |
| 4.3 | Create E2E Recovery Test | Pending | Large | P0 | Phase 1-3 | `cashu-mint-protocol/src/test/java/xyz/tcheeric/cashu/mint/proto/nut/NUT13RecoveryIntegrationTest.java` | - | Full mint→recover flow |
| 4.4 | Test Batch Recovery Logic | Pending | Medium | P0 | 4.2, 4.3 | Integration tests | - | 100 token batch testing |
| 4.5 | Test Gap Handling | Pending | Medium | P0 | 4.3 | Integration tests | - | 3 empty batch termination |
| 4.6 | Test Multiple Keysets | Pending | Medium | P1 | 4.3 | Integration tests | - | Multi-keyset recovery |
| 4.7 | Test Spent Tokens | Pending | Medium | P1 | 4.3 | Integration tests | - | Verify NUT-07 integration |
| 4.8 | Create Test Vectors | Pending | Small | P1 | None | Test resources | - | Known mnemonic→secrets |

### 4.1 Create Integration Tests

#### cashu-lib Tests
**New File**: `cashu-lib-common/src/test/java/xyz/tcheeric/cashu/protocol/NUT13Tests.java`

Test scenarios:
```java
@Test
void testDeriveSecretFromMnemonic()
@Test
void testDeriveBlindingFactorFromMnemonic()
@Test
void testDerivationPathFormatting()
@Test
void testKeysetIdToInt()
@Test
void testDeterministicSecretSerialization()
@Test
void testSecretReproducibility() // Same mnemonic = same secrets
```

#### cashu-wallet Tests
**New File**: `cashu-wallet-protocol/src/test/java/xyz/tcheeric/cashu/wallet/proto/WalletRecoveryTest.java`

Test scenarios:
```java
@Test
void testDeriveSecretsTask()
@Test
void testBuildRestoreRequest()
@Test
void testUnblindRecoveredSignatures()
@Test
void testBatchRecoveryLogic()
@Test
void testEmptyBatchTermination()
@Test
void testMultiKeysetRecovery()
```

### 4.2 Create End-to-End Recovery Test
**New File**: `cashu-mint/cashu-mint-protocol/src/test/java/xyz/tcheeric/cashu/mint/proto/nut/NUT13RecoveryIntegrationTest.java`

Full flow test:
```java
@Test
void testFullRecoveryFlow() {
    // 1. Generate mnemonic
    String mnemonic = Bip39.generateMnemonic(12);

    // 2. Mint tokens using deterministic secrets
    DeterministicKey masterKey = Bip39.mnemonicToMasterKey(mnemonic, "");
    List<DeterministicSecret> secrets = deriveSecrets(masterKey, keysetId, 0, 10);
    List<Proof> mintedProofs = mintTokens(secrets);

    // 3. Simulate wallet loss (clear local state)

    // 4. Recover wallet from mnemonic
    List<Proof> recoveredProofs = walletRecoveryService.recover(mnemonic, "", List.of(keysetId));

    // 5. Verify all tokens recovered
    assertEquals(mintedProofs.size(), recoveredProofs.size());
    assertProofsEqual(mintedProofs, recoveredProofs);
}

@Test
void testRecoveryWithGaps()
@Test
void testRecoveryWithMultipleKeysets()
@Test
void testRecoveryWithSpentTokens()
```

### 4.3 Update Documentation

#### README Updates
**Files**:
- `/home/eric/IdeaProjects/cashu-lib/README.md`
- `/home/eric/IdeaProjects/cashu-wallet/README.md`
- `/home/eric/IdeaProjects/cashu-mint/README.md`

Add sections explaining:
- NUT-13 support
- Deterministic secret generation
- Wallet recovery procedures
- Code examples

#### API Documentation
**Files**: All new classes

Ensure comprehensive JavaDoc covering:
- Purpose and usage
- Parameter explanations
- Return value descriptions
- Example code snippets
- Links to relevant NUT specifications

#### Usage Examples
**New File**: `/home/eric/IdeaProjects/cashu-lib/docs/NUT-13-EXAMPLES.md`

Include examples for:
1. Generating a new deterministic wallet
2. Minting tokens with deterministic secrets
3. Recovering a wallet from mnemonic
4. Batch recovery optimization

---

## Phase 6: CLI Implementation (cashu-cli)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 6.1 | Create GenerateMnemonicCmd | Pending | Small | P0 | Phase 1 | `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/GenerateMnemonicCmd.java` | - | Generate BIP39 mnemonic |
| 6.2 | Create RecoverWalletCmd | Pending | Large | P0 | Phase 2, 6.1 | `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/RecoverWalletCmd.java` | - | Recover wallet from mnemonic |
| 6.3 | Create ShowMnemonicCmd | Pending | Small | P1 | 6.1 | `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/ShowMnemonicCmd.java` | - | Display stored mnemonic (encrypted) |
| 6.4 | Create BackupWalletCmd | Pending | Medium | P1 | Phase 2 | `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/BackupWalletCmd.java` | - | Export wallet state + mnemonic |
| 6.5 | Enhance InitCmd for Deterministic Mode | Pending | Medium | P0 | Phase 1, 6.1 | `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/Init.java` | - | Add --deterministic flag |
| 6.6 | Add Mnemonic Storage to WalletState | Pending | Medium | P0 | Phase 1 | `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/state/WalletState.java` | - | Store encrypted mnemonic |
| 6.7 | Write CLI Tests | Pending | Medium | P1 | 6.1-6.6 | `cashu-client-wallet-cli/src/test/java/xyz/tcheeric/cashu/client/wallet/cli/` | - | Test all new CLI commands |

### 6.1 Create GenerateMnemonicCmd
**New File**: `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/GenerateMnemonicCmd.java`

CLI command to generate a new BIP39 mnemonic phrase for deterministic wallet initialization.

**Command Pattern**: Simple WalletServiceCommand extension

**Signature**:
```java
@Command(
    name = "generate-mnemonic",
    description = "Generate a new BIP39 mnemonic phrase for wallet recovery",
    mixinStandardHelpOptions = true
)
public class GenerateMnemonicCmd extends WalletServiceCommand<Integer> {

    @Option(
        names = {"-w", "--words"},
        description = "Number of words (12, 15, 18, 21, 24)",
        defaultValue = "12"
    )
    private int wordCount;

    @Option(
        names = {"-s", "--show-entropy"},
        description = "Show entropy used for generation"
    )
    private boolean showEntropy;

    @Override
    protected Integer execute(WalletService walletService) throws Exception {
        // Generate mnemonic using bip-utils
        String mnemonic = xyz.tcheeric.bips.bip39.Bip39.generateMnemonic(wordCount);

        // Display mnemonic with security warning
        System.out.println("\nGenerated BIP39 Mnemonic:");
        System.out.println("━".repeat(60));
        System.out.println(mnemonic);
        System.out.println("━".repeat(60));
        System.out.println("\n⚠️  SECURITY WARNING:");
        System.out.println("   • Write down this phrase on paper");
        System.out.println("   • Never store it digitally unencrypted");
        System.out.println("   • Keep it in a secure location");
        System.out.println("   • Anyone with this phrase can access your wallet\n");

        return 0;
    }
}
```

**Features**:
- Generate 12/15/18/21/24 word mnemonics
- Display security warnings
- Optional entropy display for advanced users
- No network calls required

### 6.2 Create RecoverWalletCmd
**New File**: `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/RecoverWalletCmd.java`

CLI command to recover wallet from BIP39 mnemonic phrase using NUT-13 deterministic derivation.

**Command Pattern**: Complex with use cases (integrates with WalletRecoveryService from Phase 2)

**Signature**:
```java
@Command(
    name = "recover",
    description = "Recover wallet from BIP39 mnemonic phrase",
    mixinStandardHelpOptions = true
)
public class RecoverWalletCmd extends WalletServiceCommand<Integer> {

    @Option(
        names = {"-m", "--mnemonic"},
        description = "BIP39 mnemonic phrase (12-24 words)",
        required = true,
        interactive = true,
        arity = "1"
    )
    private String mnemonic;

    @Option(
        names = {"-p", "--passphrase"},
        description = "Optional BIP39 passphrase",
        interactive = true
    )
    private String passphrase = "";

    @Option(
        names = {"-k", "--keysets"},
        description = "Keyset IDs to recover from (comma-separated)",
        split = ","
    )
    private List<String> keysetIds;

    @Option(
        names = {"--batch-size"},
        description = "Batch size for recovery",
        defaultValue = "100"
    )
    private int batchSize;

    @Inject
    private WalletRecoveryService recoveryService;

    @Override
    protected Integer execute(WalletService walletService) throws Exception {
        // Validate mnemonic using bip-utils
        if (!xyz.tcheeric.bips.bip39.Bip39.isValidMnemonic(mnemonic)) {
            System.err.println("❌ Invalid mnemonic phrase");
            return 1;
        }

        // Get active keysets if not specified
        List<KeysetId> keysets = keysetIds != null
            ? keysetIds.stream().map(KeysetId::new).toList()
            : walletService.getActiveKeysets();

        System.out.println("🔄 Starting wallet recovery...");
        System.out.println("   Keysets: " + keysets.size());
        System.out.println("   Batch size: " + batchSize);

        // Perform recovery
        List<Proof<DeterministicSecret>> recoveredProofs =
            recoveryService.recover(mnemonic, passphrase, keysets);

        // Display results
        long totalAmount = recoveredProofs.stream()
            .mapToLong(Proof::getAmount)
            .sum();

        System.out.println("\n✅ Recovery complete!");
        System.out.println("   Proofs recovered: " + recoveredProofs.size());
        System.out.println("   Total amount: " + totalAmount + " sats");

        // Store mnemonic in wallet state (encrypted)
        walletService.storeMnemonic(mnemonic, passphrase);

        return 0;
    }
}
```

**Features**:
- Interactive mnemonic input (hidden)
- Optional passphrase support
- Progress display during recovery
- Automatic keyset detection
- Encrypted mnemonic storage
- Recovery statistics

### 6.3 Create ShowMnemonicCmd
**New File**: `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/ShowMnemonicCmd.java`

CLI command to display stored mnemonic (requires authentication).

**Signature**:
```java
@Command(
    name = "show-mnemonic",
    description = "Display stored mnemonic phrase (requires passphrase)",
    mixinStandardHelpOptions = true
)
public class ShowMnemonicCmd extends WalletServiceCommand<Integer> {

    @Option(
        names = {"-p", "--passphrase"},
        description = "Wallet passphrase",
        required = true,
        interactive = true
    )
    private String passphrase;

    @Override
    protected Integer execute(WalletService walletService) throws Exception {
        // Retrieve encrypted mnemonic
        String mnemonic = walletService.retrieveMnemonic(passphrase);

        if (mnemonic == null) {
            System.err.println("❌ No mnemonic found or incorrect passphrase");
            return 1;
        }

        // Display with security warning
        System.out.println("\n⚠️  Your BIP39 Mnemonic:");
        System.out.println("━".repeat(60));
        System.out.println(mnemonic);
        System.out.println("━".repeat(60));
        System.out.println("\n⚠️  Keep this phrase secure!\n");

        return 0;
    }
}
```

### 6.4 Create BackupWalletCmd
**New File**: `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/BackupWalletCmd.java`

CLI command to export complete wallet backup including mnemonic and state.

**Signature**:
```java
@Command(
    name = "backup",
    description = "Create encrypted wallet backup",
    mixinStandardHelpOptions = true
)
public class BackupWalletCmd extends WalletServiceCommand<Integer> {

    @Option(
        names = {"-o", "--output"},
        description = "Backup file path",
        required = true
    )
    private File outputFile;

    @Option(
        names = {"-p", "--passphrase"},
        description = "Encryption passphrase",
        required = true,
        interactive = true
    )
    private String passphrase;

    @Option(
        names = {"--include-spent"},
        description = "Include spent proofs in backup"
    )
    private boolean includeSpent;

    @Override
    protected Integer execute(WalletService walletService) throws Exception {
        // Create backup data
        WalletBackup backup = walletService.createBackup(includeSpent);

        // Encrypt and write to file
        walletService.writeEncryptedBackup(backup, outputFile, passphrase);

        System.out.println("✅ Wallet backup created:");
        System.out.println("   File: " + outputFile.getAbsolutePath());
        System.out.println("   Proofs: " + backup.getProofCount());
        System.out.println("   Size: " + outputFile.length() + " bytes");

        return 0;
    }
}
```

### 6.5 Enhance InitCmd for Deterministic Mode
**Modify**: `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/Init.java`

Add support for deterministic wallet initialization using BIP39 mnemonic.

**Changes**:
```java
@Command(name = "init", description = "Initialize a new wallet")
public class Init extends WalletServiceCommand<Integer> {

    // ... existing fields ...

    @Option(
        names = {"--deterministic"},
        description = "Initialize deterministic wallet with mnemonic"
    )
    private boolean deterministic;

    @Option(
        names = {"--mnemonic"},
        description = "Use existing mnemonic (otherwise generates new)",
        interactive = true
    )
    private String mnemonic;

    @Override
    protected Integer execute(WalletService walletService) throws Exception {
        // ... existing init logic ...

        if (deterministic) {
            // Generate or use provided mnemonic
            if (mnemonic == null) {
                mnemonic = xyz.tcheeric.bips.bip39.Bip39.generateMnemonic(12);
                System.out.println("\n📝 Generated new mnemonic:");
                System.out.println("━".repeat(60));
                System.out.println(mnemonic);
                System.out.println("━".repeat(60));
                System.out.println("\n⚠️  Write this down! You'll need it to recover your wallet.\n");
            }

            // Validate and store mnemonic
            if (!xyz.tcheeric.bips.bip39.Bip39.isValidMnemonic(mnemonic)) {
                System.err.println("❌ Invalid mnemonic");
                return 1;
            }

            walletService.storeMnemonic(mnemonic, passphrase);
            walletService.setDeterministicMode(true);

            System.out.println("✅ Deterministic wallet initialized");
        } else {
            walletService.setDeterministicMode(false);
            System.out.println("✅ Random wallet initialized");
        }

        return 0;
    }
}
```

### 6.6 Add Mnemonic Storage to WalletState
**Modify**: `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/state/WalletState.java`

Add encrypted mnemonic storage to wallet state.

**Changes**:
```java
public class WalletState {
    // ... existing fields ...

    @JsonProperty("encrypted_mnemonic")
    private String encryptedMnemonic; // AES-256 encrypted

    @JsonProperty("deterministic_mode")
    private boolean deterministicMode;

    @JsonProperty("derivation_counters")
    private Map<String, Integer> derivationCounters; // keyset_id -> counter

    // Getters and setters

    public void storeMnemonic(String mnemonic, String passphrase) throws Exception {
        this.encryptedMnemonic = AesEncryption.encrypt(mnemonic, passphrase);
        this.deterministicMode = true;
    }

    public String retrieveMnemonic(String passphrase) throws Exception {
        if (encryptedMnemonic == null) return null;
        return AesEncryption.decrypt(encryptedMnemonic, passphrase);
    }

    public int getDerivationCounter(KeysetId keysetId) {
        return derivationCounters.getOrDefault(keysetId.toString(), 0);
    }

    public void incrementDerivationCounter(KeysetId keysetId) {
        String key = keysetId.toString();
        derivationCounters.put(key, derivationCounters.getOrDefault(key, 0) + 1);
    }
}
```

### 6.7 Write CLI Tests
**New Files**: `cashu-client-wallet-cli/src/test/java/xyz/tcheeric/cashu/client/wallet/cli/`

Test all new CLI commands:

**Test Files**:
- `GenerateMnemonicCmdTest.java` - Test mnemonic generation
- `RecoverWalletCmdTest.java` - Test wallet recovery flow
- `ShowMnemonicCmdTest.java` - Test mnemonic display
- `BackupWalletCmdTest.java` - Test backup creation
- `InitCmdDeterministicTest.java` - Test deterministic init
- `WalletStateNUT13Test.java` - Test mnemonic storage

**Test Scenarios**:
```java
@Test
void testGenerateMnemonic12Words() {
    // Test 12-word mnemonic generation
}

@Test
void testRecoverWalletFromMnemonic() {
    // Test full recovery flow
}

@Test
void testMnemonicEncryptionDecryption() {
    // Test secure storage
}

@Test
void testDerivationCounterTracking() {
    // Test counter persistence
}

@Test
void testBackupAndRestore() {
    // Test backup creation and restoration
}
```

### Integration Points

**WalletMain Registration**:
```java
@Command(
    name = "wallet",
    subcommands = {
        // ... existing commands ...
        GenerateMnemonicCmd.class,
        RecoverWalletCmd.class,
        ShowMnemonicCmd.class,
        BackupWalletCmd.class
        // Init.class already exists, just enhanced
    }
)
public class WalletMain implements Callable<Integer> {
    // ...
}
```

**WalletCliConfiguration**:
```java
@Configuration
public class WalletCliConfiguration {

    @Bean
    public WalletRecoveryService walletRecoveryService() {
        return new DefaultWalletRecoveryService();
    }

    @Bean
    public MnemonicManager mnemonicManager() {
        return new MnemonicManager();
    }

    // ... existing beans ...
}
```

### CLI Usage Examples

**Generate Mnemonic**:
```bash
$ cashu-cli wallet generate-mnemonic
$ cashu-cli wallet generate-mnemonic --words 24
```

**Initialize Deterministic Wallet**:
```bash
$ cashu-cli wallet init --deterministic
$ cashu-cli wallet init --deterministic --mnemonic "word1 word2 ... word12"
```

**Recover Wallet**:
```bash
$ cashu-cli wallet recover
Enter mnemonic: [hidden input]
Enter passphrase (optional): [hidden input]
```

**Show Mnemonic**:
```bash
$ cashu-cli wallet show-mnemonic
Enter passphrase: [hidden input]
```

**Backup Wallet**:
```bash
$ cashu-cli wallet backup -o wallet-backup.json
Enter passphrase: [hidden input]
```

---

## Phase 5: Optional Enhancements

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 5.1 | Create MnemonicManager | Optional | Medium | P2 | Phase 1 | `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/mnemonic/MnemonicManager.java` | - | Generate/validate mnemonics |
| 5.2 | Create SecureStorageRecommendations | Optional | Small | P2 | 5.1 | `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/mnemonic/SecureStorageRecommendations.java` | - | Security best practices doc |
| 5.3 | Create DerivationStateManager | Optional | Large | P2 | Phase 2 | `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/state/DerivationStateManager.java` | - | Track counters, detect gaps |
| 5.4 | Create ParallelRecoveryService | Optional | Large | P2 | Phase 2 | `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/service/ParallelRecoveryService.java` | - | Concurrent keyset recovery |
| 5.5 | Enhance Database Schema | Optional | Medium | P2 | Phase 3 | Database migration scripts | - | Add derivation metadata |

### 5.1 Mnemonic Management Utilities
**New Package**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/mnemonic/`

**New Classes**:
```java
// MnemonicManager.java
public class MnemonicManager {
    public static String generateMnemonic(int wordCount);
    public static boolean validateMnemonic(String mnemonic);
    public static MasterKey loadFromMnemonic(String mnemonic, String passphrase);
}

// SecureStorageRecommendations.java
public class SecureStorageRecommendations {
    // Documentation and examples for secure mnemonic storage
}
```

### 5.2 Counter/State Management
**New Class**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/state/DerivationStateManager.java`

Track:
- Current counter per keyset
- Last successful mint operation
- Gap detection for recovery optimization

```java
public interface DerivationStateManager {
    /**
     * Gets the current counter for a keyset.
     */
    int getCurrentCounter(KeysetId keysetId);

    /**
     * Increments counter after successful mint.
     */
    void incrementCounter(KeysetId keysetId);

    /**
     * Records successful mint at counter position.
     */
    void recordMint(KeysetId keysetId, int counter);

    /**
     * Detects gaps in counter sequence for optimization.
     */
    List<Integer> detectGaps(KeysetId keysetId);
}
```

### 5.3 Multi-Keyset Recovery Optimization
**Enhancement**: Parallel recovery across keysets

**New File**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/service/ParallelRecoveryService.java`

Instead of sequential recovery per keyset, batch recovery across all active keysets simultaneously:
```java
public class ParallelRecoveryService {
    /**
     * Recovers from multiple keysets in parallel.
     */
    public CompletableFuture<Map<KeysetId, List<Proof>>> recoverParallel(
        DeterministicKey masterKey,
        List<KeysetId> keysets,
        ExecutorService executor
    );
}
```

---

## Implementation Order Recommendation

### Step 1: Complete cashu-lib Foundation (1-2 days)
- [ ] Add bip-utils dependency to pom.xml
- [ ] Create `DeterministicSecret.java`
- [ ] Enhance `SecretFactory.java`
- [ ] Write unit tests for deterministic secret generation
- [ ] Test derivation path formatting
- [ ] Test keyset ID conversion

### Step 2: Implement Wallet Recovery (2-3 days)
- [ ] Create `DeriveSecretsTask.java`
- [ ] Create `RestoreRequestBuilder.java`
- [ ] Create `RequestRestore.java` (REST client)
- [ ] Create `WalletRecoveryService.java`
- [ ] Create `ProofRecoveryService.java`
- [ ] Write unit tests for each component

### Step 3: Enhance Mint Support (1 day)
- [ ] Verify NUT-09 works with deterministic secrets
- [ ] Update NUT-09 JavaDoc
- [ ] Add logging for recovery operations
- [ ] Consider signature vault enhancements

### Step 4: Integration Testing (2-3 days)
- [ ] Write integration tests in cashu-lib
- [ ] Write integration tests in cashu-wallet
- [ ] Write end-to-end recovery test in cashu-mint
- [ ] Test batch recovery logic
- [ ] Test gap handling (3 empty batches)
- [ ] Test multiple keysets
- [ ] Test with spent tokens

### Step 5: Documentation (1 day)
- [ ] Update all README files
- [ ] Write usage examples
- [ ] Create NUT-13-EXAMPLES.md
- [ ] Document security best practices
- [ ] Add API documentation

### Step 6: CLI Implementation (1-2 days)
- [ ] Create GenerateMnemonicCmd
- [ ] Create RecoverWalletCmd
- [ ] Create ShowMnemonicCmd
- [ ] Create BackupWalletCmd
- [ ] Enhance InitCmd for deterministic mode
- [ ] Add mnemonic storage to WalletState
- [ ] Write CLI tests
- [ ] Update WalletMain command registration

### Step 7: Optional Enhancements (1-2 days)
- [ ] Create mnemonic management utilities
- [ ] Implement derivation state manager
- [ ] Add parallel recovery optimization
- [ ] Enhance mint database schema

**Total Estimated Time**: 9-14 days

---

## Key Design Decisions

### Decision 1: Separate DeterministicSecret vs. RandomStringSecret
**Rationale**: Keep clear distinction between random and deterministic generation methods. This makes the code intent explicit and allows different serialization/metadata handling.

**Alternative Considered**: Single `Secret` class with generation mode flag
**Rejected Because**: Violates single responsibility principle and makes code harder to reason about

### Decision 2: Recovery Service in Wallet, Not Mint
**Rationale**: The wallet drives recovery by deriving secrets and requesting signatures. The mint is stateless and simply returns matching signatures. This maintains proper separation of concerns.

**Benefits**:
- Mint remains simple and stateless
- Wallet has full control over recovery process
- Privacy: Mint doesn't learn about mnemonic or derivation paths

### Decision 3: No Breaking Changes to Mint Storage
**Rationale**: The current signature vault design (keyed by blinded message) already supports NUT-13 implicitly. Deterministically derived secrets produce the same blinded messages, so existing storage works.

**Benefits**:
- Backward compatible
- No migration needed
- Simpler implementation

### Decision 4: Optional Metadata Storage
**Rationale**: Storing derivation paths is useful for debugging but not required for functionality. Make it optional to avoid unnecessary storage overhead.

**Trade-off**: Analytics capabilities vs. storage/privacy costs

### Decision 5: Batch Size of 100
**Rationale**: Follows NUT-13 recommendation. Balances between:
- Request size (larger = fewer requests but bigger payloads)
- Gap detection (smaller = faster detection of end)
- Server load (reasonable batch size)

### Decision 6: CLI Commands for User Interaction
**Rationale**: Implement comprehensive CLI commands for NUT-13 features to provide easy user access to deterministic wallets. Users need simple commands for wallet generation, recovery, and backup without requiring code knowledge.

**Benefits**:
- User-friendly interface for non-developers
- Follows established PicoCLI patterns in cashu-client
- Enables scripting and automation
- Consistent with existing wallet commands
- Secure interactive input for sensitive data (mnemonics, passphrases)

**Security Features**:
- Interactive prompts hide sensitive input
- Encrypted mnemonic storage with AES-256
- Clear security warnings for users
- Passphrase-protected operations

---

## Dependencies & Prerequisites

### Required Dependencies

#### bip-utils Library (✓ Complete)
**Location**: `/home/eric/IdeaProjects/bip-utils`
**Maven Coordinates**:
```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>bip-utils</artifactId>
    <version>2.0.0</version>
</dependency>
```
**Repository**: https://maven.398ja.xyz/releases

**Key Classes**:
- `xyz.tcheeric.bips.bip39.Bip39` - BIP39 mnemonic operations
  - `generateMnemonic(int wordCount)` - Generate 12/15/18/21/24 word mnemonics
  - `isValidMnemonic(String mnemonic)` - Validate mnemonics
  - `validateMnemonic(String mnemonic)` - Detailed validation with error messages
  - `mnemonicToSeed(String mnemonic, String passphrase)` - Convert to 64-byte seed
  - `mnemonicToMasterKey(String mnemonic, String passphrase)` - Direct to master key
  - Multi-language support: English, French, Spanish, Japanese

- `xyz.tcheeric.bips.bip32.Bip32` - BIP32 hierarchical deterministic keys
  - `mnemonicToSeed(String mnemonic, String passphrase)` - Mnemonic to seed
  - `deriveMasterKey(byte[] seed)` - Derive master key from seed
  - `deriveKey(DeterministicKey master, String path)` - Derive child by path
  - `deriveKeyFromMnemonic(String mnemonic, String passphrase, String path)` - All-in-one
  - `getPrivateKeyBytes(DeterministicKey key)` - Extract private key (32 bytes)
  - `parsePath(String path)` - Parse BIP32 path strings

- `xyz.tcheeric.bips.bip32.nut.Nut13Derivation` - NUT-13 specific derivation
  - `keysetIdToInt(String keysetIdHex)` - Convert keyset ID to integer (mod 2^31-1)
  - `buildSecretDerivationPath(int keysetIdInt, int counter)` - Build secret path
  - `buildBlindingFactorDerivationPath(int keysetIdInt, int counter)` - Build blinding path
  - `deriveSecret(DeterministicKey masterKey, String keysetIdHex, int counter)` - Derive secret
  - `deriveBlindingFactor(DeterministicKey masterKey, String keysetIdHex, int counter)` - Derive blinding factor
  - `deriveSecretAndBlindingFactor(DeterministicKey masterKey, String keysetIdHex, int counter)` - Derive both
  - `deriveSecretFromMnemonic(Nut13DerivationParams params)` - All-in-one secret derivation
  - `deriveBlindingFactorFromMnemonic(Nut13DerivationParams params)` - All-in-one blinding derivation
  - `deriveSecretAndBlindingFactor(Nut13DerivationParams params)` - All-in-one both derivation
  - Parameter builder: `Nut13DerivationParams.builder()` for clean API

**Constants**:
- `Nut13Derivation.NUT13_PURPOSE = 129372` (🥜 peanut emoji)
- `Nut13Derivation.COIN_TYPE = 0`
- `Nut13Derivation.INDEX_SECRET = 0`
- `Nut13Derivation.INDEX_BLINDING_FACTOR = 1`

**Utility Classes**:
- `xyz.tcheeric.bips.util.HexUtils` - Hex conversion utilities
- `xyz.tcheeric.bips.util.CryptoUtils` - SHA-256 hashing
- `xyz.tcheeric.bips.util.MnemonicUtils` - Mnemonic normalization
- `xyz.tcheeric.bips.util.Bip39Constants` - Constants and validation

**Testing**: 94 comprehensive tests (62 BIP39, 15 BIP32, 17 NUT-13)

**Documentation**: `/home/eric/IdeaProjects/bip-utils/docs/`
- `tutorials/getting-started.md` - Quick start guide
- `how-to/implement-nut13.md` - NUT-13 implementation guide
- `reference/api-overview.md` - Complete API reference
- `explanation/nut13-protocol.md` - NUT-13 protocol details
- `explanation/security.md` - Security best practices

#### bitcoinj Library (via bip-utils)
- **Version**: 0.17
- **Purpose**: Provides `DeterministicKey` and underlying BIP32 implementation
- **Usage**: Transitively included via bip-utils dependency

#### cashu-lib Foundation (✓ Partially Complete)
- `DerivationPath`, `SecretDerivationPath`, `RDerivationPath` - Path representation
- `KeysetId.toInt()` - Keyset ID to integer conversion
- `DeterministicSecret` (✓ Complete) - Deterministic secret implementation

### Optional Dependencies
- Persistent storage library (if implementing `DerivationStateManager`)
- Testing frameworks (JUnit 5, Mockito)

### Project Structure
```
/home/eric/IdeaProjects/
├── bip-utils/              # BIP39/BIP32 utilities (complete)
├── cashu-lib/              # Core entities and common code
│   ├── cashu-lib-common/
│   ├── cashu-lib-crypto/
│   └── cashu-lib-entities/
├── cashu-wallet/           # Wallet client library
│   ├── cashu-wallet-client/
│   └── cashu-wallet-protocol/
├── cashu-mint/             # Mint server
│   ├── cashu-mint-protocol/
│   ├── cashu-mint-rest/
│   └── cashu-mint-tools/
└── cashu-cli/              # Command-line interface
    ├── cashu-client-wallet-cli/
    ├── cashu-client-identity-cli/
    └── cashu-client-cli-common/
```

---

## bip-utils Integration Guide

### Overview
The **bip-utils** library provides all the cryptographic primitives needed for NUT-13 implementation. This section details how to integrate bip-utils across all cashu projects.

### Integration by Project

#### cashu-lib-common

**Dependencies to Add**:
```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>bip-utils</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Import Statements**:
```java
import xyz.tcheeric.bips.bip39.Bip39;
import xyz.tcheeric.bips.bip32.Bip32;
import xyz.tcheeric.bips.bip32.nut.Nut13Derivation;
import xyz.tcheeric.bips.util.HexUtils;
import org.bitcoinj.crypto.DeterministicKey;
```

**Key Integration Points**:
1. **SecretFactory.java** - Use `Nut13Derivation` for secret generation
2. **DeterministicSecret.java** - Already created, ready for integration
3. **DerivationPath.java** - Can use `Nut13Derivation.buildSecretDerivationPath()`

**Example Usage**:
```java
// In SecretFactory
public static DeterministicSecret createDeterministic(
    DeterministicKey masterKey,
    KeysetId keysetId,
    int counter
) {
    byte[] secretBytes = Nut13Derivation.deriveSecret(
        masterKey,
        keysetId.toString(),
        counter
    );
    return DeterministicSecret.create(secretBytes, keysetId, counter);
}
```

#### cashu-wallet-protocol

**Key Classes to Implement**:
1. **WalletRecoveryService** - Orchestrates recovery using `Nut13Derivation`
2. **DeriveSecretsTask** - Uses `Nut13Derivation.deriveSecretAndBlindingFactor()`
3. **MnemonicManager** (Optional Phase 5) - Uses `Bip39` for mnemonic operations

**Example: Recovery Service**:
```java
public class DefaultWalletRecoveryService implements WalletRecoveryService {

    @Override
    public List<Proof<DeterministicSecret>> recover(
        String mnemonic,
        String passphrase,
        List<KeysetId> keysets
    ) {
        // Derive master key using bip-utils
        DeterministicKey masterKey = Bip39.mnemonicToMasterKey(mnemonic, passphrase);

        List<Proof<DeterministicSecret>> allProofs = new ArrayList<>();

        for (KeysetId keysetId : keysets) {
            allProofs.addAll(recoverKeyset(masterKey, keysetId, 0));
        }

        return allProofs;
    }

    @Override
    public List<Proof<DeterministicSecret>> recoverKeyset(
        DeterministicKey masterKey,
        KeysetId keysetId,
        int startCounter
    ) {
        List<Proof<DeterministicSecret>> proofs = new ArrayList<>();
        int emptyBatches = 0;
        int counter = startCounter;

        while (emptyBatches < 3) {
            // Derive batch of secrets using Nut13Derivation
            var task = new DeriveSecretsTask(masterKey, keysetId, counter, 100);
            var result = task.execute();

            // Request signatures from mint
            var response = mintClient.restore(
                buildRestoreRequest(result.secrets(), result.blindingFactors())
            );

            if (response.getSignatures().isEmpty()) {
                emptyBatches++;
            } else {
                emptyBatches = 0;
                proofs.addAll(unblindAndCreateProofs(response, result));
            }

            counter += 100;
        }

        return proofs;
    }
}
```

**Example: Derive Secrets Task**:
```java
public class DeriveSecretsTask implements Task<DeriveSecretsResult> {

    @Override
    public DeriveSecretsResult execute() {
        List<DeterministicSecret> secrets = new ArrayList<>();
        List<byte[]> blindingFactors = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int currentCounter = startCounter + i;

            // Use bip-utils to derive both values at once
            var pair = Nut13Derivation.deriveSecretAndBlindingFactor(
                masterKey,
                keysetId.toString(),
                currentCounter
            );

            secrets.add(DeterministicSecret.create(
                pair.secret(),
                keysetId,
                currentCounter
            ));
            blindingFactors.add(pair.blindingFactor());
        }

        return new DeriveSecretsResult(secrets, blindingFactors);
    }
}
```

#### cashu-client-wallet-cli

**Key Commands Using bip-utils**:

1. **GenerateMnemonicCmd**:
```java
@Override
protected Integer execute(WalletService walletService) throws Exception {
    // Use bip-utils to generate mnemonic
    String mnemonic = Bip39.generateMnemonic(wordCount);

    System.out.println("Generated BIP39 Mnemonic:");
    System.out.println(mnemonic);
    return 0;
}
```

2. **RecoverWalletCmd**:
```java
@Override
protected Integer execute(WalletService walletService) throws Exception {
    // Validate using bip-utils
    if (!Bip39.isValidMnemonic(mnemonic)) {
        System.err.println("Invalid mnemonic phrase");
        return 1;
    }

    // Detailed validation
    var validationResult = Bip39.validateMnemonic(mnemonic);
    if (!validationResult.isValid()) {
        System.err.println("Error: " + validationResult.getErrorMessage());
        return 1;
    }

    // Recover using wallet service
    var recoveredProofs = recoveryService.recover(mnemonic, passphrase, keysets);

    System.out.println("✅ Recovered " + recoveredProofs.size() + " proofs");
    return 0;
}
```

3. **InitCmd (Deterministic Mode)**:
```java
if (deterministic) {
    if (mnemonic == null) {
        // Generate new mnemonic using bip-utils
        mnemonic = Bip39.generateMnemonic(12);
        System.out.println("Generated mnemonic: " + mnemonic);
    }

    // Validate
    if (!Bip39.isValidMnemonic(mnemonic)) {
        System.err.println("Invalid mnemonic");
        return 1;
    }

    // Store in wallet
    walletService.storeMnemonic(mnemonic, passphrase);
    walletService.setDeterministicMode(true);
}
```

### Common Patterns

#### Pattern 1: Derive Master Key
```java
// From mnemonic
String mnemonic = "word1 word2 ... word12";
String passphrase = "";  // or user-provided

DeterministicKey masterKey = Bip39.mnemonicToMasterKey(mnemonic, passphrase);
```

#### Pattern 2: Derive Secrets and Blinding Factors
```java
// Using master key
String keysetIdHex = "009a1f293253e41e";
int counter = 0;

var pair = Nut13Derivation.deriveSecretAndBlindingFactor(
    masterKey,
    keysetIdHex,
    counter
);

byte[] secret = pair.secret();
byte[] blindingFactor = pair.blindingFactor();
```

#### Pattern 3: All-in-One Derivation from Mnemonic
```java
// Build parameters
var params = Nut13Derivation.Nut13DerivationParams.builder()
    .mnemonicPhrase(mnemonic)
    .passphrase(passphrase)
    .keysetIdHex(keysetIdHex)
    .counter(counter)
    .build();

// Derive both
var pair = Nut13Derivation.deriveSecretAndBlindingFactor(params);
```

#### Pattern 4: Batch Derivation
```java
// Derive 100 secrets at once
List<DeterministicSecret> secrets = new ArrayList<>();
List<byte[]> blindingFactors = new ArrayList<>();

for (int i = 0; i < 100; i++) {
    var pair = Nut13Derivation.deriveSecretAndBlindingFactor(
        masterKey,
        keysetIdHex,
        startCounter + i
    );

    secrets.add(DeterministicSecret.create(
        pair.secret(),
        keysetId,
        startCounter + i
    ));
    blindingFactors.add(pair.blindingFactor());
}
```

#### Pattern 5: Mnemonic Validation
```java
// Simple validation
boolean valid = Bip39.isValidMnemonic(mnemonic);

// Detailed validation for user feedback
var result = Bip39.validateMnemonic(mnemonic);
if (!result.isValid()) {
    System.out.println("Error: " + result.getErrorMessage());
    // Possible errors:
    // - "Mnemonic is empty"
    // - "Invalid word count: X (must be 12, 15, 18, 21, or 24)"
    // - "Invalid word at position X: 'word'"
    // - "Invalid checksum"
}
```

### Helper Utilities Available

#### HexUtils
```java
// Convert bytes to hex
String hex = HexUtils.toHex(bytes);

// Convert hex to bytes (accepts "0x" prefix)
byte[] bytes = HexUtils.fromHex("deadbeef");
byte[] bytes2 = HexUtils.fromHex("0xdeadbeef");  // Also works
```

#### CryptoUtils
```java
// SHA-256 hash
byte[] hash = CryptoUtils.sha256(data);

// First byte only (for checksums)
byte firstByte = CryptoUtils.sha256FirstByte(data);
```

#### MnemonicUtils
```java
// Normalize mnemonic (trim, lowercase, collapse whitespace)
String normalized = MnemonicUtils.normalizeMnemonic(" WORD1  word2   WORD3 ");
// Result: "word1 word2 word3"

// Split into words
String[] words = MnemonicUtils.splitMnemonic(mnemonic);
```

### Testing with bip-utils

**Test Dependencies**:
```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>bip-utils</artifactId>
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

**Example Test**:
```java
@Test
void testDeterministicSecretDerivation() {
    // Known test vector
    String mnemonic = "abandon abandon abandon abandon abandon abandon " +
                     "abandon abandon abandon abandon abandon about";
    String keysetId = "009a1f293253e41e";
    int counter = 0;

    // Derive secret
    var params = Nut13Derivation.Nut13DerivationParams.builder()
        .mnemonicPhrase(mnemonic)
        .passphrase("")
        .keysetIdHex(keysetId)
        .counter(counter)
        .build();

    byte[] secret = Nut13Derivation.deriveSecretFromMnemonic(params);

    // Verify reproducibility
    byte[] secret2 = Nut13Derivation.deriveSecretFromMnemonic(params);
    assertArrayEquals(secret, secret2);

    // Verify length
    assertEquals(32, secret.length);
}
```

### Security Best Practices

1. **Never log mnemonics or seeds**: Use bip-utils in production mode
2. **Clear sensitive data**: bip-utils handles defensive copying internally
3. **Validate all user input**: Use `Bip39.validateMnemonic()` for detailed errors
4. **Use strong passphrases**: Optional but recommended for additional security
5. **Secure counter storage**: Maintain accurate counter state to avoid key reuse

### Migration Path

For existing random secret wallets:
1. **Phase 1**: Implement deterministic secret support alongside random
2. **Phase 2**: Offer users option to upgrade to deterministic wallet
3. **Phase 3**: Generate recovery mnemonic for existing random wallet (cannot be deterministic but provides backup)

---

## Security Considerations

### Mnemonic Storage
- **Never store mnemonics in plaintext**
- **Never log mnemonics**
- Recommend encryption at rest
- Consider hardware security modules for production

### Passphrase Handling
- Support optional passphrases per BIP39
- Clear passphrases from memory after use
- Warn users about passphrase importance

### Derivation Path Privacy
- Don't send derivation paths to mint
- Only send blinded messages
- Mint cannot learn derivation strategy

### Counter Management
- Implement gap limits to prevent exhaustive search
- Use 3 consecutive empty batches as termination condition
- Consider maximum counter value (e.g., 10,000)

### CLI Security
- **Interactive Input**: Use PicoCLI's `interactive=true` to hide sensitive input (mnemonics, passphrases)
- **Terminal Clearing**: Consider clearing terminal after displaying mnemonics
- **Command History**: Warn users not to pass mnemonics as command-line arguments (shell history risk)
- **File Permissions**: Set restrictive permissions on wallet state files (600)
- **Backup Encryption**: Always encrypt backup files before writing to disk
- **In-Memory Cleanup**: Clear sensitive strings from memory after use

---

## Testing Strategy

### Unit Tests
- Test each component in isolation
- Mock external dependencies
- Focus on edge cases and error handling

### Integration Tests
- Test interaction between components
- Use test fixtures and known test vectors
- Verify deterministic behavior

### End-to-End Tests
- Full recovery flow from mnemonic to proofs
- Real HTTP communication (or mock server)
- Test against actual mint implementation

### Test Vectors
Consider creating official test vectors:
- Known mnemonic → expected secrets
- Known keyset ID → expected integer conversion
- Known derivation path → expected bytes

---

## Future Enhancements

### Multi-Currency Support
Extend derivation paths for different currencies/units while maintaining NUT-13 compatibility.

### Hardware Wallet Integration
Support for hardware wallets that can derive BIP32 keys without exposing master key.

### Recovery Progress Tracking
UI/API for showing recovery progress (keysets processed, tokens found, etc.).

### Advanced Gap Detection
Machine learning or heuristics to optimize gap detection and reduce recovery time.

### Hierarchical Wallet Structure
Support for multiple accounts under single mnemonic using additional derivation levels.

---

## References

- [NUT-13 Specification](https://github.com/cashubtc/nuts/blob/main/13.md)
- [NUT-09 Specification](https://github.com/cashubtc/nuts/blob/main/09.md) (Restore)
- [NUT-07 Specification](https://github.com/cashubtc/nuts/blob/main/07.md) (Token State Check)
- [BIP-39: Mnemonic Code](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki)
- [BIP-32: Hierarchical Deterministic Wallets](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki)

---

## Contact & Questions

For questions about this implementation plan, please refer to:
- Project maintainers
- NUT-13 specification authors
- Cashu protocol discussions

---

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-11-03 | Initial implementation plan created | TBD |
| 1.1 | 2025-11-03 | Added task tables, TOC, project summary, enhanced tracking | TBD |
| 1.2 | 2025-11-03 | Added separate ID column to all task tables for better referencing | TBD |
| 1.3 | 2025-11-03 | Added Phase 6 for CLI implementation (cashu-cli) with 7 tasks | TBD |
| 1.4 | 2025-11-03 | Comprehensive bip-utils integration guide with detailed API documentation and usage patterns | TBD |
| 1.5 | 2025-11-04 | Completed Phase 4.1 - NUT13IntegrationTest with 40 comprehensive tests | TBD |

*Last Updated: 2025-11-04*
*Status: Active Implementation Plan*
*Total Tasks: 36 (31 required + 5 optional)*
*Progress: 17% Complete (6/36 tasks)*
