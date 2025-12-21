# NUT-12 Implementation Plan

## Table of Contents

1. [Overview](#overview)
2. [Project Summary](#project-summary)
3. [Phase Summary](#phase-summary)
4. [NUT-12 Specification Summary](#nut-12-specification-summary)
   - [Purpose](#purpose)
   - [DLEQ Proof Mechanism](#dleq-proof-mechanism)
   - [Hash Function Specification](#hash-function-specification)
   - [Data Structure Extensions](#data-structure-extensions)
   - [Verification Procedures](#verification-procedures)
5. [Phase 1: Core DLEQ Cryptographic Primitives](#phase-1-core-dleq-cryptographic-primitives)
   - [1.1 Create DLEQProof Class](#11-create-dleqproof-class)
   - [1.2 Create DLEQUtils Cryptographic Utilities](#12-create-dlequtils-cryptographic-utilities)
   - [1.3 Implement DLEQ Hash Function](#13-implement-dleq-hash-function)
   - [1.4 Write Unit Tests for DLEQ Primitives](#14-write-unit-tests-for-dleq-primitives)
6. [Phase 2: Extend Existing Data Structures](#phase-2-extend-existing-data-structures)
   - [2.1 Create BlindSignatureDLEQ Class](#21-create-blindsignaturedleq-class)
   - [2.2 Create ProofDLEQ Class](#22-create-proofdleq-class)
   - [2.3 Update JSON Serializers/Deserializers](#23-update-json-serializersdeserializers)
   - [2.4 Write Unit Tests for Data Structures](#24-write-unit-tests-for-data-structures)
7. [Phase 3: Mint-Side Implementation](#phase-3-mint-side-implementation)
   - [3.1 Create DLEQProofGenerator Service](#31-create-dleqproofgenerator-service)
   - [3.2 Update Signing Flow to Include DLEQ](#32-update-signing-flow-to-include-dleq)
   - [3.3 Add NUT-12 Support to Mint Info](#33-add-nut-12-support-to-mint-info)
   - [3.4 Write Mint Unit Tests](#34-write-mint-unit-tests)
8. [Phase 4: Wallet-Side Implementation](#phase-4-wallet-side-implementation)
   - [4.1 Create DLEQVerificationService](#41-create-dleqverificationservice)
   - [4.2 Implement Alice Verification (Minting User)](#42-implement-alice-verification-minting-user)
   - [4.3 Implement Carol Verification (Receiving User)](#43-implement-carol-verification-receiving-user)
   - [4.4 Integrate DLEQ Verification into Token Processing](#44-integrate-dleq-verification-into-token-processing)
   - [4.5 Write Wallet Unit Tests](#45-write-wallet-unit-tests)
9. [Phase 5: Integration & Testing](#phase-5-integration--testing)
   - [5.1 Create End-to-End DLEQ Tests](#51-create-end-to-end-dleq-tests)
   - [5.2 Test Cross-Wallet Token Transfer](#52-test-cross-wallet-token-transfer)
   - [5.3 Test Offline Verification](#53-test-offline-verification)
   - [5.4 Create Test Vectors](#54-create-test-vectors)
10. [Phase 6: CLI Implementation](#phase-6-cli-implementation)
    - [6.1 Add DLEQ Verification to Token Commands](#61-add-dleq-verification-to-token-commands)
    - [6.2 Add Verify Token Command](#62-add-verify-token-command)
    - [6.3 Write CLI Tests](#63-write-cli-tests)
11. [Implementation Details](#implementation-details)
    - [11.1 Mathematical Operations](#mathematical-operations)
    - [11.2 Key Design Decisions](#key-design-decisions)
    - [11.3 Security Considerations](#security-considerations)
    - [11.4 Testing Strategy](#testing-strategy)
12. [References](#references)

---

## Overview

NUT-12 is an optional Cashu protocol extension that enables **offline signature verification** using **Discrete Log Equality (DLEQ) proofs**. This allows:

1. **Alice** (minting user) to verify the mint's signature using only the mint's public keys, without contacting the mint
2. **Carol** (receiving user) to verify signatures from tokens she receives, enabling **trustless peer-to-peer ecash transfers**

**Specification**: https://github.com/cashubtc/nuts/blob/main/12.md

---

## Project Summary

| Metric | Value |
|--------|-------|
| **Total Tasks** | 24 tasks (21 required + 3 optional) |
| **Completed Tasks** | 4 tasks (17%) |
| **In Progress** | Phase 1 complete; Phase 2 pending |
| **Start Date** | TBD |
| **Target Completion** | TBD |
| **Primary Developer** | TBD |
| **Reviewers** | TBD |
| **Branch** | NUT12 |
| **Related PRs** | TBD |
| **Blocked Items** | None |
| **Risk Level** | Medium - Cryptographic implementation requires careful review |
| **Breaking Changes** | None - Backward compatible (DLEQ is optional) |
| **Documentation Required** | Yes - README, JavaDoc, Examples |
| **Testing Coverage Target** | 90%+ (Critical for cryptographic code) |

### Project Scope

- **In Scope**: DLEQ proof generation, DLEQ verification (Alice and Carol), data structure extensions, CLI verification commands
- **Out of Scope**: Hardware acceleration, alternative proof systems, GUI applications
- **Dependencies**: cashu-lib-crypto (BDHKEUtils, Point utilities), BouncyCastle
- **REST Endpoints**: **No new endpoints required** - NUT-12 only adds optional `dleq` field to existing `BlindSignature` and `Proof` payloads

### Key Milestones

1. Phase 1 Complete - Core DLEQ cryptographic primitives
2. Phase 2 Complete - Extended data structures
3. Phase 3 Complete - Mint-side DLEQ generation
4. Phase 4 Complete - Wallet-side DLEQ verification
5. Phase 5 Complete - Full integration testing
6. Phase 6 Complete - CLI commands
7. Production Ready - All tests passing, documentation complete

---

## Phase Summary

| Phase | Focus Area | Tasks | Status |
|-------|-----------|-------|--------|
| **Phase 1** | Core DLEQ Primitives | 4 tasks | Complete |
| **Phase 2** | Data Structure Extensions | 4 tasks | Not Started |
| **Phase 3** | Mint Implementation | 4 tasks | Not Started |
| **Phase 4** | Wallet Implementation | 5 tasks | Not Started |
| **Phase 5** | Integration & Testing | 4 tasks | Not Started |
| **Phase 6** | CLI Implementation | 3 tasks | Not Started |
| **Total** | All Phases | **24 tasks** | **17% Complete** |

### Task Table Legend

**Status Values:**
- Complete - Task finished and committed
- Pending - Not yet started
- In Progress - Currently being worked on
- Optional - Nice to have, not required for core functionality
- Blocked - Cannot proceed due to dependencies

**Priority:**
- P0 - Critical, required for NUT-12 functionality
- P1 - Important, required for production quality
- P2 - Optional, enhancement only

---

## NUT-12 Specification Summary

### Purpose

DLEQ proofs enable users to verify mint signatures **offline** using only the mint's public keys. This is achieved by proving that the mint used the **same private key `a`** for:
1. Creating its public key `A = a*G`
2. Signing the blinded message `C' = a*B'`

### DLEQ Proof Mechanism

**Mint Bob's Proof Generation:**
```
1. Generate random nonce: r
2. Compute: R1 = r*G
3. Compute: R2 = r*B'
4. Compute hash: e = hash(R1, R2, A, C')
5. Compute: s = r + e*a (mod n)
6. Return: (e, s) as the DLEQ proof
```

**Verification by Alice (Minting User):**
```
1. Reconstruct: R1 = s*G - e*A
2. Reconstruct: R2 = s*B' - e*C'
3. Verify: e == hash(R1, R2, A, C')
```

**Verification by Carol (Receiving User):**
```
1. Reconstruct blinded values from unblinded proof + blinding factor:
   - Y = hash_to_curve(secret)
   - C' = C + r*A
   - B' = Y + r*G
2. Perform same verification as Alice
```

### Hash Function Specification

The hash function `hash(R1, R2, A, C')` generates a deterministic SHA256 hash:

```java
// Pseudocode
public static byte[] dleqHash(ECPoint R1, ECPoint R2, ECPoint A, ECPoint C_) {
    // Serialize each point in UNCOMPRESSED format (130 hex characters = 65 bytes)
    String concatenated =
        toUncompressedHex(R1) +
        toUncompressedHex(R2) +
        toUncompressedHex(A) +
        toUncompressedHex(C_);

    // SHA256 of UTF-8 encoded concatenation
    return SHA256(concatenated.getBytes(UTF_8));
}
```

**Important**: Points must be serialized in **uncompressed** format (04 || X || Y = 65 bytes = 130 hex chars).

### Data Structure Extensions

**BlindSignature (Mint-to-User) - Extended:**
```json
{
  "id": "<keyset_id>",
  "amount": <int>,
  "C_": "<hex_point>",
  "dleq": {
    "e": "<hex_scalar>",
    "s": "<hex_scalar>"
  }
}
```

**Proof (User-to-User) - Extended:**
```json
{
  "id": "<keyset_id>",
  "amount": <int>,
  "secret": "<string>",
  "C": "<hex_point>",
  "dleq": {
    "e": "<hex_scalar>",
    "s": "<hex_scalar>",
    "r": "<hex_scalar>"
  }
}
```

**Critical Security Note**: The blinding factor `r` is included in the Proof DLEQ to enable Carol's verification, but it **MUST NOT** be shared with the mint, as this would allow the mint to link `BlindSignature` to `Proof`.

### Verification Procedures

**Mandatory Requirements:**
1. If a DLEQ proof is included in the mint's `BlindSignature` response, wallets **MUST** verify the DLEQ proof
2. If a DLEQ proof is included in a received token, wallets **MUST** verify the proof

**Verification Failure Handling:**
- If verification fails, the wallet MUST reject the token
- Tokens with invalid DLEQ proofs should be considered potentially fraudulent

---

## Phase 1: Core DLEQ Cryptographic Primitives

| ID | Task | Status | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|----------|------------|----------|--------|-------|
| 1.1 | Create DLEQProof Class | Complete | P0 | None | `cashu-lib-common` | 6537f6d | Data class for (e, s, r) |
| 1.2 | Create DLEQUtils | Complete | P0 | 1.1 | `cashu-lib-crypto` | 6537f6d | Core DLEQ operations |
| 1.3 | Implement DLEQ Hash Function | Complete | P0 | 1.2 | `cashu-lib-crypto` | 6537f6d | SHA256 of uncompressed points |
| 1.4 | Write Unit Tests | Complete | P0 | 1.1-1.3 | `cashu-lib-crypto/test` | 6537f6d | Test vectors and edge cases |

### 1.1 Create DLEQProof Class

**New File**: `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/DLEQProof.java`

```java
package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Discrete Log Equality (DLEQ) proof for NUT-12 offline signature verification.
 *
 * <p>Contains the challenge {@code e} and response {@code s} scalars that prove
 * the mint used the same private key for creating its public key and signing
 * the blinded message.
 *
 * <p>For Proof objects (user-to-user transfer), also includes the blinding factor
 * {@code r} to enable the receiving user to verify the signature.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/12.md">NUT-12 Specification</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"e", "s", "r"})
public class DLEQProof {

    /**
     * The challenge scalar (32 bytes as hex string).
     * Computed as: e = SHA256(R1 || R2 || A || C')
     */
    @JsonProperty("e")
    private String e;

    /**
     * The response scalar (32 bytes as hex string).
     * Computed as: s = r + e*a (mod n)
     */
    @JsonProperty("s")
    private String s;

    /**
     * The blinding factor (32 bytes as hex string).
     * Only included in Proof objects for user-to-user verification.
     * MUST NOT be shared with the mint.
     */
    @JsonProperty("r")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String r;

    /**
     * Creates a DLEQProof for BlindSignature (without blinding factor).
     */
    public static DLEQProof forBlindSignature(String e, String s) {
        return DLEQProof.builder()
                .e(e)
                .s(s)
                .build();
    }

    /**
     * Creates a DLEQProof for Proof (with blinding factor for Carol verification).
     */
    public static DLEQProof forProof(String e, String s, String r) {
        return DLEQProof.builder()
                .e(e)
                .s(s)
                .r(r)
                .build();
    }

    /**
     * Checks if this proof includes the blinding factor (for Carol verification).
     */
    public boolean hasBlindingFactor() {
        return r != null && !r.isEmpty();
    }
}
```

### 1.2 Create DLEQUtils Cryptographic Utilities

**New File**: `cashu-lib-crypto/src/main/java/xyz/tcheeric/cashu/crypto/DLEQUtils.java`

```java
package xyz.tcheeric.cashu.crypto;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.math.ec.ECPoint;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Discrete Log Equality (DLEQ) proof utilities for NUT-12.
 *
 * <p>Provides methods for generating and verifying DLEQ proofs that demonstrate
 * the mint used the same private key for creating its public key and signing
 * blinded messages.
 *
 * <p>Mathematical basis:
 * <ul>
 *   <li>Given: A = a*G (mint's public key)</li>
 *   <li>Given: C' = a*B' (blind signature)</li>
 *   <li>Prove: log_G(A) == log_B'(C')</li>
 * </ul>
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/12.md">NUT-12 Specification</a>
 */
@Slf4j
public class DLEQUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a DLEQ proof for a blind signature.
     *
     * <p>The mint calls this when creating a BlindSignature to prove it used
     * the same private key for both the public key and the signature.
     *
     * @param privateKey The mint's private key (a)
     * @param B_ The blinded message point
     * @param C_ The blind signature point (C' = a*B')
     * @return DLEQProofResult containing (e, s) scalars
     */
    public static DLEQProofResult generateProof(
            @NonNull BigInteger privateKey,
            @NonNull ECPoint B_,
            @NonNull ECPoint C_
    ) {
        // Get curve parameters
        var spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
        ECPoint G = spec.getG();
        BigInteger n = spec.getN();

        // Compute public key A = a*G
        ECPoint A = G.multiply(privateKey).normalize();

        // Generate random nonce r
        BigInteger r = generateRandomScalar(n);

        // Compute R1 = r*G
        ECPoint R1 = G.multiply(r).normalize();

        // Compute R2 = r*B'
        ECPoint R2 = B_.multiply(r).normalize();

        // Compute challenge e = hash(R1, R2, A, C')
        byte[] eBytes = dleqHash(R1, R2, A, C_);
        BigInteger e = new BigInteger(1, eBytes).mod(n);

        // Compute response s = r + e*a (mod n)
        BigInteger s = r.add(e.multiply(privateKey)).mod(n);

        return new DLEQProofResult(
                Utils.bytesToHexString(Utils.bytesFromBigInteger(e)),
                Utils.bytesToHexString(Utils.bytesFromBigInteger(s))
        );
    }

    /**
     * Verifies a DLEQ proof for a BlindSignature (Alice's verification).
     *
     * <p>Alice verifies immediately after receiving a BlindSignature from the mint.
     * She has access to:
     * <ul>
     *   <li>B' - her original blinded message</li>
     *   <li>C' - the blind signature from mint</li>
     *   <li>A - mint's public key (from keyset)</li>
     *   <li>e, s - the DLEQ proof</li>
     * </ul>
     *
     * @param e The challenge scalar (hex string)
     * @param s The response scalar (hex string)
     * @param B_ The blinded message point
     * @param C_ The blind signature point
     * @param A The mint's public key point
     * @return true if the proof is valid
     */
    public static boolean verifyProof(
            @NonNull String e,
            @NonNull String s,
            @NonNull ECPoint B_,
            @NonNull ECPoint C_,
            @NonNull ECPoint A
    ) {
        try {
            var spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
            ECPoint G = spec.getG();
            BigInteger n = spec.getN();

            // Parse scalars
            BigInteger eBigInt = Utils.bigIntFromBytes(Utils.hexStringToBytes(e));
            BigInteger sBigInt = Utils.bigIntFromBytes(Utils.hexStringToBytes(s));

            // Reconstruct R1 = s*G - e*A
            ECPoint sG = G.multiply(sBigInt).normalize();
            ECPoint eA = A.multiply(eBigInt).normalize();
            ECPoint R1 = sG.subtract(eA).normalize();

            // Reconstruct R2 = s*B' - e*C'
            ECPoint sB_ = B_.multiply(sBigInt).normalize();
            ECPoint eC_ = C_.multiply(eBigInt).normalize();
            ECPoint R2 = sB_.subtract(eC_).normalize();

            // Compute expected challenge
            byte[] expectedEBytes = dleqHash(R1, R2, A, C_);
            BigInteger expectedE = new BigInteger(1, expectedEBytes).mod(n);

            // Verify e == hash(R1, R2, A, C')
            boolean valid = eBigInt.equals(expectedE);

            log.debug("DLEQ verification result: {}", valid);
            return valid;

        } catch (Exception ex) {
            log.error("DLEQ verification failed with exception", ex);
            return false;
        }
    }

    /**
     * Verifies a DLEQ proof from a received Proof (Carol's verification).
     *
     * <p>Carol verifies tokens received from another user. She must first
     * reconstruct the blinded values using the included blinding factor r:
     * <ul>
     *   <li>Y = hash_to_curve(secret)</li>
     *   <li>B' = Y + r*G</li>
     *   <li>C' = C + r*A</li>
     * </ul>
     *
     * @param e The challenge scalar (hex string)
     * @param s The response scalar (hex string)
     * @param r The blinding factor (hex string)
     * @param secret The proof secret (for hash_to_curve)
     * @param C The unblinded signature point
     * @param A The mint's public key point
     * @return true if the proof is valid
     */
    public static boolean verifyProofWithBlindingFactor(
            @NonNull String e,
            @NonNull String s,
            @NonNull String r,
            @NonNull byte[] secret,
            @NonNull ECPoint C,
            @NonNull ECPoint A
    ) {
        try {
            var spec = org.bouncycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
            ECPoint G = spec.getG();

            // Parse blinding factor
            BigInteger rBigInt = Utils.bigIntFromBytes(Utils.hexStringToBytes(r));

            // Reconstruct Y = hash_to_curve(secret)
            ECPoint Y = BDHKEUtils.hashToCurve(secret);

            // Reconstruct B' = Y + r*G
            ECPoint rG = G.multiply(rBigInt).normalize();
            ECPoint B_ = Y.add(rG).normalize();

            // Reconstruct C' = C + r*A
            ECPoint rA = A.multiply(rBigInt).normalize();
            ECPoint C_ = C.add(rA).normalize();

            // Now verify using standard verification
            return verifyProof(e, s, B_, C_, A);

        } catch (Exception ex) {
            log.error("DLEQ verification with blinding factor failed", ex);
            return false;
        }
    }

    /**
     * Computes the DLEQ hash: SHA256(R1 || R2 || A || C')
     *
     * <p>Points are serialized in UNCOMPRESSED format (130 hex characters each)
     * and concatenated as a string before hashing.
     *
     * @param R1 First nonce commitment
     * @param R2 Second nonce commitment
     * @param A Mint's public key
     * @param C_ Blind signature
     * @return 32-byte SHA256 hash
     */
    public static byte[] dleqHash(ECPoint R1, ECPoint R2, ECPoint A, ECPoint C_) {
        try {
            // Serialize points in uncompressed format (04 || X || Y)
            String concatenated =
                    pointToUncompressedHex(R1) +
                    pointToUncompressedHex(R2) +
                    pointToUncompressedHex(A) +
                    pointToUncompressedHex(C_);

            // SHA256 of UTF-8 encoded concatenation
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(concatenated.getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Converts an EC point to uncompressed hex format (130 characters).
     * Format: 04 || X (32 bytes) || Y (32 bytes)
     */
    public static String pointToUncompressedHex(@NonNull ECPoint point) {
        byte[] encoded = point.normalize().getEncoded(false); // false = uncompressed
        return Utils.bytesToHexString(encoded);
    }

    /**
     * Generates a cryptographically secure random scalar in range [1, n-1].
     */
    private static BigInteger generateRandomScalar(BigInteger n) {
        BigInteger r;
        do {
            byte[] bytes = new byte[32];
            SECURE_RANDOM.nextBytes(bytes);
            r = new BigInteger(1, bytes).mod(n);
        } while (r.equals(BigInteger.ZERO));
        return r;
    }

    /**
     * Result of DLEQ proof generation.
     */
    public record DLEQProofResult(String e, String s) {}
}
```

### 1.3 Implement DLEQ Hash Function

The hash function is implemented within `DLEQUtils.dleqHash()` above. Key implementation details:

1. **Uncompressed point format**: Each point is 65 bytes (04 prefix + 32 byte X + 32 byte Y)
2. **Hex encoding**: Points are converted to 130 hex characters each
3. **Concatenation**: Points are concatenated as strings: `R1_hex + R2_hex + A_hex + C'_hex`
4. **SHA256**: Applied to UTF-8 bytes of the concatenated string

### 1.4 Write Unit Tests for DLEQ Primitives

**New File**: `cashu-lib-crypto/src/test/java/xyz/tcheeric/cashu/crypto/DLEQUtilsTest.java`

```java
package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DLEQUtils Tests")
class DLEQUtilsTest {

    private static ECNamedCurveParameterSpec spec;
    private static ECPoint G;

    @BeforeAll
    static void setup() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        spec = ECNamedCurveTable.getParameterSpec("secp256k1");
        G = spec.getG();
    }

    @Nested
    @DisplayName("Proof Generation")
    class ProofGenerationTests {

        @Test
        @DisplayName("should generate valid DLEQ proof")
        void shouldGenerateValidProof() {
            // Generate private key
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);

            // Create a blinded message B'
            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint B_ = decodePoint(blindResult[0]);

            // Sign: C' = a*B'
            ECPoint C_ = B_.multiply(privateKey).normalize();

            // Generate DLEQ proof
            var proof = DLEQUtils.generateProof(privateKey, B_, C_);

            assertThat(proof).isNotNull();
            assertThat(proof.e()).isNotEmpty();
            assertThat(proof.s()).isNotEmpty();
            assertThat(proof.e()).hasSize(64); // 32 bytes = 64 hex chars
            assertThat(proof.s()).hasSize(64);
        }

        @Test
        @DisplayName("should generate different proofs for same inputs (randomized)")
        void shouldGenerateDifferentProofs() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint B_ = decodePoint(blindResult[0]);
            ECPoint C_ = B_.multiply(privateKey).normalize();

            var proof1 = DLEQUtils.generateProof(privateKey, B_, C_);
            var proof2 = DLEQUtils.generateProof(privateKey, B_, C_);

            // Proofs should be different (random nonce)
            assertThat(proof1.e()).isNotEqualTo(proof2.e());
            assertThat(proof1.s()).isNotEqualTo(proof2.s());
        }
    }

    @Nested
    @DisplayName("Alice Verification (BlindSignature)")
    class AliceVerificationTests {

        @Test
        @DisplayName("should verify valid DLEQ proof")
        void shouldVerifyValidProof() {
            // Setup
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint A = G.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint B_ = decodePoint(blindResult[0]);
            ECPoint C_ = B_.multiply(privateKey).normalize();

            // Generate proof
            var proof = DLEQUtils.generateProof(privateKey, B_, C_);

            // Verify
            boolean valid = DLEQUtils.verifyProof(proof.e(), proof.s(), B_, C_, A);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should reject proof with wrong challenge")
        void shouldRejectWrongChallenge() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint A = G.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint B_ = decodePoint(blindResult[0]);
            ECPoint C_ = B_.multiply(privateKey).normalize();

            var proof = DLEQUtils.generateProof(privateKey, B_, C_);

            // Tamper with challenge
            String wrongE = "00" + proof.e().substring(2);

            boolean valid = DLEQUtils.verifyProof(wrongE, proof.s(), B_, C_, A);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject proof with wrong public key")
        void shouldRejectWrongPublicKey() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint A = G.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint B_ = decodePoint(blindResult[0]);
            ECPoint C_ = B_.multiply(privateKey).normalize();

            var proof = DLEQUtils.generateProof(privateKey, B_, C_);

            // Use different public key
            byte[] wrongKeyBytes = KeysUtils.generatePrivateKey();
            ECPoint wrongA = G.multiply(Utils.bigIntFromBytes(wrongKeyBytes)).normalize();

            boolean valid = DLEQUtils.verifyProof(proof.e(), proof.s(), B_, C_, wrongA);

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("Carol Verification (Proof with blinding factor)")
    class CarolVerificationTests {

        @Test
        @DisplayName("should verify proof with blinding factor")
        void shouldVerifyWithBlindingFactor() {
            // Setup mint
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint A = G.multiply(privateKey).normalize();

            // Alice creates blinded message
            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint B_ = decodePoint(blindResult[0]);
            BigInteger r = Utils.bigIntFromBytes(blindResult[1]);

            // Mint signs
            ECPoint C_ = B_.multiply(privateKey).normalize();

            // Generate DLEQ proof
            var proof = DLEQUtils.generateProof(privateKey, B_, C_);

            // Alice unblinds to get C
            ECPoint rA = A.multiply(r).normalize();
            ECPoint C = C_.subtract(rA).normalize();

            // Carol verifies using blinding factor
            String rHex = Utils.bytesToHexString(blindResult[1]);
            boolean valid = DLEQUtils.verifyProofWithBlindingFactor(
                    proof.e(), proof.s(), rHex, secret, C, A
            );

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should reject proof with wrong blinding factor")
        void shouldRejectWrongBlindingFactor() {
            byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
            BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
            ECPoint A = G.multiply(privateKey).normalize();

            byte[] secret = "test_secret".getBytes();
            byte[][] blindResult = BDHKEUtils.blindMessage(secret);
            ECPoint B_ = decodePoint(blindResult[0]);
            BigInteger r = Utils.bigIntFromBytes(blindResult[1]);

            ECPoint C_ = B_.multiply(privateKey).normalize();
            var proof = DLEQUtils.generateProof(privateKey, B_, C_);

            ECPoint rA = A.multiply(r).normalize();
            ECPoint C = C_.subtract(rA).normalize();

            // Use wrong blinding factor
            byte[] wrongR = KeysUtils.generatePrivateKey();
            String wrongRHex = Utils.bytesToHexString(wrongR);

            boolean valid = DLEQUtils.verifyProofWithBlindingFactor(
                    proof.e(), proof.s(), wrongRHex, secret, C, A
            );

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("Hash Function")
    class HashFunctionTests {

        @Test
        @DisplayName("should produce deterministic hash")
        void shouldProduceDeterministicHash() {
            ECPoint R1 = G.multiply(BigInteger.valueOf(123)).normalize();
            ECPoint R2 = G.multiply(BigInteger.valueOf(456)).normalize();
            ECPoint A = G.multiply(BigInteger.valueOf(789)).normalize();
            ECPoint C_ = G.multiply(BigInteger.valueOf(101112)).normalize();

            byte[] hash1 = DLEQUtils.dleqHash(R1, R2, A, C_);
            byte[] hash2 = DLEQUtils.dleqHash(R1, R2, A, C_);

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should produce 32-byte hash")
        void shouldProduce32ByteHash() {
            ECPoint R1 = G.multiply(BigInteger.valueOf(1)).normalize();
            ECPoint R2 = G.multiply(BigInteger.valueOf(2)).normalize();
            ECPoint A = G.multiply(BigInteger.valueOf(3)).normalize();
            ECPoint C_ = G.multiply(BigInteger.valueOf(4)).normalize();

            byte[] hash = DLEQUtils.dleqHash(R1, R2, A, C_);

            assertThat(hash).hasSize(32);
        }
    }

    private ECPoint decodePoint(byte[] raw64) {
        // Add uncompressed prefix if needed
        byte[] sec1 = (raw64.length == 64)
                ? concat(new byte[]{0x04}, raw64)
                : raw64;
        return spec.getCurve().decodePoint(sec1);
    }

    private byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
```

---

## Phase 2: Extend Existing Data Structures

| ID | Task | Status | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|----------|------------|----------|--------|-------|
| 2.1 | Create BlindSignatureDLEQ | Pending | P0 | 1.1 | `cashu-lib-common` | - | Extends BlindSignature with DLEQ |
| 2.2 | Create ProofDLEQ | Pending | P0 | 1.1 | `cashu-lib-common` | - | Extends Proof with DLEQ + r |
| 2.3 | Update JSON Serializers | Pending | P0 | 2.1, 2.2 | `cashu-lib-common/json` | - | Handle optional DLEQ field |
| 2.4 | Write Unit Tests | Pending | P1 | 2.1-2.3 | `cashu-lib-common/test` | - | Serialization tests |

### 2.1 Create BlindSignatureDLEQ Class

**Option A: Extend BlindSignature**

Modify existing `BlindSignature.java` to include optional DLEQ field:

```java
package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"amount", "id", "C_", "dleq"})
public class BlindSignature {

    @JsonProperty
    private int amount;

    @JsonProperty("id")
    private KeysetId keySetId;

    @JsonProperty("C_")
    private Signature blindedSignature;

    /**
     * Optional DLEQ proof for NUT-12 offline verification.
     * Contains (e, s) challenge and response scalars.
     */
    @JsonProperty("dleq")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DLEQProof dleq;

    /**
     * Checks if this BlindSignature includes a DLEQ proof.
     */
    public boolean hasDLEQProof() {
        return dleq != null;
    }
}
```

### 2.2 Create ProofDLEQ Class

Modify existing `Proof.java` to include optional DLEQ field:

```java
package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.tcheeric.cashu.common.json.deserializer.SecretDeserializer;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"amount", "id", "secret", "C", "dleq", "witness"})
public class Proof<T extends Secret> {

    @JsonProperty
    private int amount;

    @JsonProperty
    @JsonDeserialize(using = SecretDeserializer.class)
    private T secret;

    @JsonProperty("id")
    private String keySetId;

    @JsonProperty("C")
    private Signature unblindedSignature;

    /**
     * Optional DLEQ proof for NUT-12 offline verification.
     * For Proof objects, includes (e, s, r) where r is the blinding factor.
     */
    @JsonProperty("dleq")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DLEQProof dleq;

    @JsonProperty("witness")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Witness witness;

    public void setSecretData(byte[] data) {
        this.secret.setData(data);
    }

    /**
     * Checks if this Proof includes a DLEQ proof.
     */
    public boolean hasDLEQProof() {
        return dleq != null;
    }

    /**
     * Checks if this Proof has a DLEQ proof with blinding factor (for Carol verification).
     */
    public boolean hasDLEQWithBlindingFactor() {
        return dleq != null && dleq.hasBlindingFactor();
    }
}
```

### 2.3 Update JSON Serializers/Deserializers

**New File**: `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/json/deserializer/DLEQProofDeserializer.java`

```java
package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import xyz.tcheeric.cashu.common.DLEQProof;

import java.io.IOException;

public class DLEQProofDeserializer extends JsonDeserializer<DLEQProof> {

    @Override
    public DLEQProof deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        String e = node.has("e") ? node.get("e").asText() : null;
        String s = node.has("s") ? node.get("s").asText() : null;
        String r = node.has("r") ? node.get("r").asText() : null;

        if (e == null || s == null) {
            return null;
        }

        return DLEQProof.builder()
                .e(e)
                .s(s)
                .r(r)
                .build();
    }
}
```

### 2.4 Write Unit Tests for Data Structures

**New File**: `cashu-lib-common/src/test/java/xyz/tcheeric/cashu/common/DLEQProofTest.java`

```java
package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DLEQProof Tests")
class DLEQProofTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    @DisplayName("Serialization")
    class SerializationTests {

        @Test
        @DisplayName("should serialize BlindSignature DLEQ without r")
        void shouldSerializeBlindSignatureDLEQ() throws Exception {
            var proof = DLEQProof.forBlindSignature(
                    "a" .repeat(64),
                    "b".repeat(64)
            );

            String json = mapper.writeValueAsString(proof);

            assertThat(json).contains("\"e\"");
            assertThat(json).contains("\"s\"");
            assertThat(json).doesNotContain("\"r\"");
        }

        @Test
        @DisplayName("should serialize Proof DLEQ with r")
        void shouldSerializeProofDLEQ() throws Exception {
            var proof = DLEQProof.forProof(
                    "a".repeat(64),
                    "b".repeat(64),
                    "c".repeat(64)
            );

            String json = mapper.writeValueAsString(proof);

            assertThat(json).contains("\"e\"");
            assertThat(json).contains("\"s\"");
            assertThat(json).contains("\"r\"");
        }
    }

    @Nested
    @DisplayName("Deserialization")
    class DeserializationTests {

        @Test
        @DisplayName("should deserialize DLEQ without r")
        void shouldDeserializeWithoutR() throws Exception {
            String json = """
                {
                    "e": "%s",
                    "s": "%s"
                }
                """.formatted("a".repeat(64), "b".repeat(64));

            DLEQProof proof = mapper.readValue(json, DLEQProof.class);

            assertThat(proof.getE()).hasSize(64);
            assertThat(proof.getS()).hasSize(64);
            assertThat(proof.getR()).isNull();
            assertThat(proof.hasBlindingFactor()).isFalse();
        }

        @Test
        @DisplayName("should deserialize DLEQ with r")
        void shouldDeserializeWithR() throws Exception {
            String json = """
                {
                    "e": "%s",
                    "s": "%s",
                    "r": "%s"
                }
                """.formatted("a".repeat(64), "b".repeat(64), "c".repeat(64));

            DLEQProof proof = mapper.readValue(json, DLEQProof.class);

            assertThat(proof.getE()).hasSize(64);
            assertThat(proof.getS()).hasSize(64);
            assertThat(proof.getR()).hasSize(64);
            assertThat(proof.hasBlindingFactor()).isTrue();
        }
    }

    @Nested
    @DisplayName("BlindSignature with DLEQ")
    class BlindSignatureWithDLEQTests {

        @Test
        @DisplayName("should serialize BlindSignature with optional DLEQ")
        void shouldSerializeWithDLEQ() throws Exception {
            var blindSig = BlindSignature.builder()
                    .amount(100)
                    .keySetId(new KeysetId("009a1f293253e41e"))
                    .blindedSignature(Signature.fromString("02" + "a".repeat(64)))
                    .dleq(DLEQProof.forBlindSignature("e".repeat(64), "s".repeat(64)))
                    .build();

            String json = mapper.writeValueAsString(blindSig);

            assertThat(json).contains("\"dleq\"");
            assertThat(json).contains("\"e\"");
            assertThat(json).contains("\"s\"");
        }

        @Test
        @DisplayName("should serialize BlindSignature without DLEQ when null")
        void shouldSerializeWithoutDLEQ() throws Exception {
            var blindSig = BlindSignature.builder()
                    .amount(100)
                    .keySetId(new KeysetId("009a1f293253e41e"))
                    .blindedSignature(Signature.fromString("02" + "a".repeat(64)))
                    .build();

            String json = mapper.writeValueAsString(blindSig);

            assertThat(json).doesNotContain("\"dleq\"");
        }
    }
}
```

---

## Phase 3: Mint-Side Implementation

| ID | Task | Status | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|----------|------------|----------|--------|-------|
| 3.1 | Create DLEQProofGenerator | Pending | P0 | Phase 1 | `cashu-mint-protocol` | - | Service for generating DLEQ proofs |
| 3.2 | Update Signing Flow | Pending | P0 | 3.1 | `cashu-mint-protocol` | - | Include DLEQ in BlindSignature |
| 3.3 | Add NUT-12 to Mint Info | Pending | P1 | 3.1 | `cashu-mint-protocol` | - | Advertise NUT-12 support |
| 3.4 | Write Mint Unit Tests | Pending | P1 | 3.1-3.3 | `cashu-mint-protocol/test` | - | Test DLEQ generation |

### 3.1 Create DLEQProofGenerator Service

**New File**: `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/service/DLEQProofGenerator.java`

```java
package xyz.tcheeric.cashu.mint.proto.service;

import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.DLEQProof;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * Service for generating DLEQ proofs for NUT-12 compliance.
 */
public interface DLEQProofGenerator {

    /**
     * Generates a DLEQ proof for a blind signature.
     *
     * @param privateKey The mint's private key for the keyset
     * @param blindedMessage The blinded message (B')
     * @param blindSignature The resulting blind signature (C')
     * @return DLEQProof containing (e, s) challenge and response
     */
    DLEQProof generateProof(
            BigInteger privateKey,
            ECPoint blindedMessage,
            ECPoint blindSignature
    );

    /**
     * Creates a BlindSignature with DLEQ proof attached.
     *
     * @param amount Token amount
     * @param keysetId Keyset identifier
     * @param privateKey Mint's private key
     * @param blindedMessage Original blinded message
     * @return BlindSignature with DLEQ proof
     */
    BlindSignature signWithDLEQ(
            int amount,
            String keysetId,
            BigInteger privateKey,
            BlindedMessage blindedMessage
    );
}
```

**Implementation**: `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/service/impl/DefaultDLEQProofGenerator.java`

```java
package xyz.tcheeric.cashu.mint.proto.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.math.ec.ECPoint;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.BlindedMessage;
import xyz.tcheeric.cashu.common.DLEQProof;
import xyz.tcheeric.cashu.common.KeysetId;
import xyz.tcheeric.cashu.common.Signature;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;
import xyz.tcheeric.cashu.crypto.DLEQUtils;
import xyz.tcheeric.cashu.mint.proto.service.DLEQProofGenerator;

import java.math.BigInteger;

@Slf4j
public class DefaultDLEQProofGenerator implements DLEQProofGenerator {

    @Override
    public DLEQProof generateProof(
            BigInteger privateKey,
            ECPoint blindedMessage,
            ECPoint blindSignature
    ) {
        var result = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);
        return DLEQProof.forBlindSignature(result.e(), result.s());
    }

    @Override
    public BlindSignature signWithDLEQ(
            int amount,
            String keysetId,
            BigInteger privateKey,
            BlindedMessage blindedMessage
    ) {
        // Get B' as ECPoint
        ECPoint B_ = decodeBlindedMessage(blindedMessage);

        // Sign: C' = a * B'
        ECPoint C_ = B_.multiply(privateKey).normalize();

        // Generate DLEQ proof
        DLEQProof dleqProof = generateProof(privateKey, B_, C_);

        // Create BlindSignature with DLEQ
        return BlindSignature.builder()
                .amount(amount)
                .keySetId(new KeysetId(keysetId))
                .blindedSignature(Signature.fromBytes(C_.getEncoded(true)))
                .dleq(dleqProof)
                .build();
    }

    private ECPoint decodeBlindedMessage(BlindedMessage message) {
        // Implementation depends on BlindedMessage structure
        // Convert to ECPoint
        byte[] bytes = message.toBytes();
        return BDHKEUtils.decodePoint(bytes);
    }
}
```

### 3.2 Update Signing Flow to Include DLEQ

Modify the existing signing task/service to optionally include DLEQ proofs:

**Modify**: `cashu-mint-protocol/src/main/java/xyz/tcheeric/cashu/mint/proto/tasks/SignBlindedMessagesTask.java`

```java
// Add DLEQ proof generation to signing flow
public class SignBlindedMessagesTask implements Task<List<BlindSignature>> {

    private final boolean includeDLEQ; // Configuration flag
    private final DLEQProofGenerator dleqGenerator;

    @Override
    public List<BlindSignature> execute() {
        List<BlindSignature> signatures = new ArrayList<>();

        for (BlindedMessage message : blindedMessages) {
            // Sign the message
            ECPoint C_ = signBlindedMessage(message);

            BlindSignature.BlindSignatureBuilder builder = BlindSignature.builder()
                    .amount(message.getAmount())
                    .keySetId(keysetId)
                    .blindedSignature(Signature.fromBytes(C_.getEncoded(true)));

            // Add DLEQ proof if enabled
            if (includeDLEQ) {
                DLEQProof dleq = dleqGenerator.generateProof(
                        privateKey,
                        decodeBlindedMessage(message),
                        C_
                );
                builder.dleq(dleq);
            }

            signatures.add(builder.build());
        }

        return signatures;
    }
}
```

### 3.3 Add NUT-12 Support to Mint Info

Update mint info response to advertise NUT-12 support:

```java
// In mint info configuration
{
    "12": {
        "supported": true
    }
}
```

### 3.4 Write Mint Unit Tests

**New File**: `cashu-mint-protocol/src/test/java/xyz/tcheeric/cashu/mint/proto/service/DLEQProofGeneratorTest.java`

```java
package xyz.tcheeric.cashu.mint.proto.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.DLEQUtils;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;
import xyz.tcheeric.cashu.mint.proto.service.impl.DefaultDLEQProofGenerator;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DLEQProofGenerator Tests")
class DLEQProofGeneratorTest {

    private final DLEQProofGenerator generator = new DefaultDLEQProofGenerator();

    @Test
    @DisplayName("should generate verifiable DLEQ proof")
    void shouldGenerateVerifiableProof() {
        // Setup
        byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
        BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);

        // Create test blinded message and signature
        // ... (test implementation)

        // Verify the proof is valid
        // ... (verification)
    }
}
```

---

## Phase 4: Wallet-Side Implementation

| ID | Task | Status | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|----------|------------|----------|--------|-------|
| 4.1 | Create DLEQVerificationService | Pending | P0 | Phase 1 | `cashu-wallet-protocol` | - | Service for verifying DLEQ proofs |
| 4.2 | Implement Alice Verification | Pending | P0 | 4.1 | `cashu-wallet-protocol` | - | Verify BlindSignature DLEQ |
| 4.3 | Implement Carol Verification | Pending | P0 | 4.1 | `cashu-wallet-protocol` | - | Verify Proof DLEQ with r |
| 4.4 | Integrate into Token Processing | Pending | P0 | 4.2, 4.3 | `cashu-wallet-protocol` | - | Automatic verification |
| 4.5 | Write Wallet Unit Tests | Pending | P1 | 4.1-4.4 | `cashu-wallet-protocol/test` | - | Test all verification paths |

### 4.1 Create DLEQVerificationService

**New File**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/service/DLEQVerificationService.java`

```java
package xyz.tcheeric.cashu.wallet.proto.service;

import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import org.bouncycastle.math.ec.ECPoint;

/**
 * Service for verifying NUT-12 DLEQ proofs.
 *
 * <p>Provides verification for two scenarios:
 * <ul>
 *   <li>Alice: Verifying BlindSignature received from mint</li>
 *   <li>Carol: Verifying Proof received from another user</li>
 * </ul>
 */
public interface DLEQVerificationService {

    /**
     * Verifies DLEQ proof in a BlindSignature (Alice's verification).
     *
     * <p>Called immediately after receiving a BlindSignature from the mint.
     *
     * @param blindSignature The BlindSignature with DLEQ proof
     * @param blindedMessage The original blinded message (B')
     * @param mintPublicKey The mint's public key (A)
     * @return true if verification passes or no DLEQ proof present
     * @throws DLEQVerificationException if DLEQ proof is present and invalid
     */
    boolean verifyBlindSignature(
            BlindSignature blindSignature,
            ECPoint blindedMessage,
            ECPoint mintPublicKey
    ) throws DLEQVerificationException;

    /**
     * Verifies DLEQ proof in a Proof (Carol's verification).
     *
     * <p>Called when receiving tokens from another user.
     *
     * @param proof The Proof with DLEQ proof (including blinding factor r)
     * @param mintPublicKey The mint's public key (A)
     * @return true if verification passes or no DLEQ proof present
     * @throws DLEQVerificationException if DLEQ proof is present and invalid
     */
    <T extends Secret> boolean verifyProof(
            Proof<T> proof,
            ECPoint mintPublicKey
    ) throws DLEQVerificationException;

    /**
     * Creates a Proof with DLEQ data for sending to another user.
     *
     * <p>Adds the blinding factor (r) to the DLEQ proof so the recipient
     * can verify the signature.
     *
     * @param proof The original Proof
     * @param blindingFactor The blinding factor used when creating the proof
     * @param dleqE The challenge from BlindSignature DLEQ
     * @param dleqS The response from BlindSignature DLEQ
     * @return Proof with DLEQ proof including blinding factor
     */
    <T extends Secret> Proof<T> addDLEQToProof(
            Proof<T> proof,
            byte[] blindingFactor,
            String dleqE,
            String dleqS
    );
}
```

**Exception Class**:

```java
package xyz.tcheeric.cashu.wallet.proto.service;

/**
 * Exception thrown when DLEQ verification fails.
 */
public class DLEQVerificationException extends RuntimeException {

    public DLEQVerificationException(String message) {
        super(message);
    }

    public DLEQVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 4.2 Implement Alice Verification (Minting User)

**Implementation**: `cashu-wallet-protocol/src/main/java/xyz/tcheeric/cashu/wallet/proto/service/impl/DefaultDLEQVerificationService.java`

```java
package xyz.tcheeric.cashu.wallet.proto.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.math.ec.ECPoint;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.DLEQProof;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;
import xyz.tcheeric.cashu.crypto.DLEQUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;
import xyz.tcheeric.cashu.wallet.proto.service.DLEQVerificationException;
import xyz.tcheeric.cashu.wallet.proto.service.DLEQVerificationService;

@Slf4j
public class DefaultDLEQVerificationService implements DLEQVerificationService {

    @Override
    public boolean verifyBlindSignature(
            BlindSignature blindSignature,
            ECPoint blindedMessage,
            ECPoint mintPublicKey
    ) throws DLEQVerificationException {

        if (!blindSignature.hasDLEQProof()) {
            log.debug("No DLEQ proof in BlindSignature, skipping verification");
            return true;
        }

        DLEQProof dleq = blindSignature.getDleq();
        ECPoint C_ = decodeSignature(blindSignature.getBlindedSignature());

        boolean valid = DLEQUtils.verifyProof(
                dleq.getE(),
                dleq.getS(),
                blindedMessage,
                C_,
                mintPublicKey
        );

        if (!valid) {
            throw new DLEQVerificationException(
                    "DLEQ verification failed for BlindSignature with keyset " +
                    blindSignature.getKeySetId()
            );
        }

        log.debug("DLEQ verification passed for BlindSignature");
        return true;
    }

    @Override
    public <T extends Secret> boolean verifyProof(
            Proof<T> proof,
            ECPoint mintPublicKey
    ) throws DLEQVerificationException {

        if (!proof.hasDLEQProof()) {
            log.debug("No DLEQ proof in Proof, skipping verification");
            return true;
        }

        DLEQProof dleq = proof.getDleq();

        if (!dleq.hasBlindingFactor()) {
            throw new DLEQVerificationException(
                    "DLEQ proof in Proof is missing blinding factor (r)"
            );
        }

        byte[] secret = proof.getSecret().toBytes();
        ECPoint C = decodeSignature(proof.getUnblindedSignature());

        boolean valid = DLEQUtils.verifyProofWithBlindingFactor(
                dleq.getE(),
                dleq.getS(),
                dleq.getR(),
                secret,
                C,
                mintPublicKey
        );

        if (!valid) {
            throw new DLEQVerificationException(
                    "DLEQ verification failed for Proof with keyset " +
                    proof.getKeySetId()
            );
        }

        log.debug("DLEQ verification passed for Proof");
        return true;
    }

    @Override
    public <T extends Secret> Proof<T> addDLEQToProof(
            Proof<T> proof,
            byte[] blindingFactor,
            String dleqE,
            String dleqS
    ) {
        DLEQProof dleq = DLEQProof.forProof(
                dleqE,
                dleqS,
                Utils.bytesToHexString(blindingFactor)
        );

        return Proof.<T>builder()
                .amount(proof.getAmount())
                .secret(proof.getSecret())
                .keySetId(proof.getKeySetId())
                .unblindedSignature(proof.getUnblindedSignature())
                .dleq(dleq)
                .witness(proof.getWitness())
                .build();
    }

    private ECPoint decodeSignature(Signature signature) {
        return BDHKEUtils.decodePoint(signature.toBytes());
    }
}
```

### 4.3 Implement Carol Verification (Receiving User)

Carol's verification is handled by `verifyProof()` in the service above. The key steps are:

1. Extract DLEQ proof (e, s, r) from received Proof
2. Get the unblinded signature C and secret from the Proof
3. Get mint's public key A (from keyset)
4. Call `DLEQUtils.verifyProofWithBlindingFactor()`

### 4.4 Integrate DLEQ Verification into Token Processing

**Modify**: Token receive/processing flow to automatically verify DLEQ proofs:

```java
// In token processing service
public List<Proof<T>> processReceivedToken(Token token) {
    List<Proof<T>> verifiedProofs = new ArrayList<>();

    for (Proof<T> proof : token.getProofs()) {
        // Get mint's public key for this keyset
        ECPoint mintPublicKey = getMintPublicKey(proof.getKeySetId());

        // Verify DLEQ if present (mandatory per NUT-12)
        dleqVerificationService.verifyProof(proof, mintPublicKey);

        verifiedProofs.add(proof);
    }

    return verifiedProofs;
}
```

### 4.5 Write Wallet Unit Tests

**New File**: `cashu-wallet-protocol/src/test/java/xyz/tcheeric/cashu/wallet/proto/service/DLEQVerificationServiceTest.java`

```java
package xyz.tcheeric.cashu.wallet.proto.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.BlindSignature;
import xyz.tcheeric.cashu.common.DLEQProof;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.wallet.proto.service.impl.DefaultDLEQVerificationService;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DLEQVerificationService Tests")
class DLEQVerificationServiceTest {

    private DLEQVerificationService service;

    @BeforeEach
    void setup() {
        service = new DefaultDLEQVerificationService();
    }

    @Nested
    @DisplayName("Alice Verification (BlindSignature)")
    class AliceVerificationTests {

        @Test
        @DisplayName("should pass verification for valid DLEQ proof")
        void shouldPassForValidProof() {
            // Setup valid BlindSignature with DLEQ
            // ... test implementation
        }

        @Test
        @DisplayName("should throw exception for invalid DLEQ proof")
        void shouldThrowForInvalidProof() {
            // Setup BlindSignature with invalid DLEQ
            // ... test implementation

            assertThatThrownBy(() -> service.verifyBlindSignature(/* ... */))
                    .isInstanceOf(DLEQVerificationException.class);
        }

        @Test
        @DisplayName("should skip verification when no DLEQ proof")
        void shouldSkipWhenNoProof() {
            // BlindSignature without DLEQ should pass
            // ... test implementation
        }
    }

    @Nested
    @DisplayName("Carol Verification (Proof)")
    class CarolVerificationTests {

        @Test
        @DisplayName("should pass verification for valid DLEQ proof with r")
        void shouldPassForValidProofWithR() {
            // Setup valid Proof with DLEQ including r
            // ... test implementation
        }

        @Test
        @DisplayName("should throw exception when r is missing")
        void shouldThrowWhenRMissing() {
            // Proof with DLEQ but no blinding factor
            // ... test implementation

            assertThatThrownBy(() -> service.verifyProof(/* ... */))
                    .isInstanceOf(DLEQVerificationException.class)
                    .hasMessageContaining("missing blinding factor");
        }
    }
}
```

---

## Phase 5: Integration & Testing

| ID | Task | Status | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|----------|------------|----------|--------|-------|
| 5.1 | Create E2E DLEQ Tests | Pending | P0 | Phase 3, 4 | Integration tests | - | Full mint→verify flow |
| 5.2 | Test Cross-Wallet Transfer | Pending | P0 | 5.1 | Integration tests | - | Alice→Carol verification |
| 5.3 | Test Offline Verification | Pending | P0 | 5.1 | Integration tests | - | Verify without mint |
| 5.4 | Create Test Vectors | Pending | P1 | Phase 1 | Test resources | - | Known input→output vectors |

### 5.1 Create End-to-End DLEQ Tests

**New File**: `cashu-mint-protocol/src/test/java/xyz/tcheeric/cashu/protocol/NUT12IntegrationTest.java`

```java
package xyz.tcheeric.cashu.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NUT-12 Integration Tests")
class NUT12IntegrationTest {

    @Test
    @DisplayName("should complete full mint→verify flow with DLEQ")
    void shouldCompleteMintVerifyFlow() {
        // 1. Generate mint keypair
        // 2. Create blinded message
        // 3. Mint signs with DLEQ
        // 4. Alice verifies DLEQ
        // 5. Alice unblinds
        // 6. Alice creates Proof with DLEQ+r
        // 7. Carol verifies Proof
    }

    @Test
    @DisplayName("should reject forged signature")
    void shouldRejectForgedSignature() {
        // Test that invalid signatures fail DLEQ verification
    }

    @Test
    @DisplayName("should reject wrong public key")
    void shouldRejectWrongPublicKey() {
        // Test that verification fails with different public key
    }
}
```

### 5.2 Test Cross-Wallet Token Transfer

```java
@Test
@DisplayName("should verify token transferred between users")
void shouldVerifyTransferredToken() {
    // 1. Alice mints token with DLEQ
    // 2. Alice verifies BlindSignature DLEQ
    // 3. Alice creates Proof with DLEQ+r
    // 4. Alice sends token to Carol
    // 5. Carol verifies Proof DLEQ using r
    // 6. Carol accepts token
}
```

### 5.3 Test Offline Verification

```java
@Test
@DisplayName("should verify token offline using only public keys")
void shouldVerifyOffline() {
    // 1. Pre-create valid token with DLEQ
    // 2. Serialize token
    // 3. Load only mint's public keys (no mint connection)
    // 4. Deserialize token
    // 5. Verify DLEQ offline
}
```

### 5.4 Create Test Vectors

Create known test vectors for reproducible testing:

```java
public class NUT12TestVectors {

    // Known private key
    public static final String PRIVATE_KEY = "0000...";

    // Known blinded message
    public static final String BLINDED_MESSAGE = "04...";

    // Expected blind signature
    public static final String BLIND_SIGNATURE = "02...";

    // Expected DLEQ proof (for specific random nonce)
    public static final String DLEQ_E = "...";
    public static final String DLEQ_S = "...";
}
```

---

## Phase 6: CLI Implementation

| ID | Task | Status | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|----------|------------|----------|--------|-------|
| 6.1 | Add DLEQ to Token Commands | Pending | P1 | Phase 4 | `cashu-client-wallet-cli` | - | Auto-verify on receive |
| 6.2 | Add Verify Token Command | Pending | P2 | 6.1 | `cashu-client-wallet-cli` | - | Manual verification command |
| 6.3 | Write CLI Tests | Pending | P1 | 6.1, 6.2 | `cashu-client-wallet-cli/test` | - | Test CLI DLEQ features |

### 6.1 Add DLEQ Verification to Token Commands

**Modify**: Existing token receive/redeem commands to automatically verify DLEQ proofs.

```java
@Command(name = "receive", description = "Receive a Cashu token")
public class ReceiveTokenCmd extends WalletServiceCommand<Integer> {

    @Option(names = {"--skip-dleq"}, description = "Skip DLEQ verification (not recommended)")
    private boolean skipDLEQ;

    @Override
    protected Integer execute(WalletService walletService) throws Exception {
        Token token = parseToken(tokenString);

        if (!skipDLEQ) {
            // Verify DLEQ proofs
            for (Proof<?> proof : token.getProofs()) {
                if (proof.hasDLEQProof()) {
                    ECPoint mintPubKey = getMintPublicKey(proof.getKeySetId());
                    dleqService.verifyProof(proof, mintPubKey);
                    System.out.println("DLEQ verification passed for proof");
                }
            }
        }

        // Continue with normal receive flow
        return walletService.receiveToken(token);
    }
}
```

### 6.2 Add Verify Token Command

**New File**: `cashu-client-wallet-cli/src/main/java/xyz/tcheeric/cashu/client/wallet/cli/command/VerifyTokenCmd.java`

```java
package xyz.tcheeric.cashu.client.wallet.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "verify",
    description = "Verify DLEQ proofs in a Cashu token offline",
    mixinStandardHelpOptions = true
)
public class VerifyTokenCmd extends WalletServiceCommand<Integer> {

    @Parameters(index = "0", description = "The Cashu token to verify")
    private String tokenString;

    @Option(names = {"-v", "--verbose"}, description = "Show detailed verification info")
    private boolean verbose;

    @Override
    protected Integer execute(WalletService walletService) throws Exception {
        Token token = parseToken(tokenString);

        int totalProofs = token.getProofs().size();
        int proofsWithDLEQ = 0;
        int verifiedProofs = 0;

        for (Proof<?> proof : token.getProofs()) {
            if (proof.hasDLEQProof()) {
                proofsWithDLEQ++;

                try {
                    ECPoint mintPubKey = getMintPublicKey(proof.getKeySetId());
                    dleqService.verifyProof(proof, mintPubKey);
                    verifiedProofs++;

                    if (verbose) {
                        System.out.printf("Proof %s: DLEQ valid%n", proof.getKeySetId());
                    }
                } catch (DLEQVerificationException e) {
                    System.err.printf("Proof %s: DLEQ INVALID - %s%n",
                            proof.getKeySetId(), e.getMessage());
                }
            } else if (verbose) {
                System.out.printf("Proof %s: No DLEQ proof%n", proof.getKeySetId());
            }
        }

        System.out.println();
        System.out.printf("Total proofs: %d%n", totalProofs);
        System.out.printf("Proofs with DLEQ: %d%n", proofsWithDLEQ);
        System.out.printf("Valid DLEQ proofs: %d%n", verifiedProofs);

        if (proofsWithDLEQ > 0 && verifiedProofs == proofsWithDLEQ) {
            System.out.println("All DLEQ proofs verified successfully!");
            return 0;
        } else if (proofsWithDLEQ == 0) {
            System.out.println("No DLEQ proofs to verify");
            return 0;
        } else {
            System.err.println("Some DLEQ proofs failed verification!");
            return 1;
        }
    }
}
```

**Usage**:
```bash
# Verify a token offline
$ cashu-cli wallet verify "cashuAeyJ..."

# Verbose output
$ cashu-cli wallet verify -v "cashuAeyJ..."
```

### 6.3 Write CLI Tests

```java
@DisplayName("VerifyTokenCmd Tests")
class VerifyTokenCmdTest {

    @Test
    @DisplayName("should verify valid token with DLEQ")
    void shouldVerifyValidToken() {
        // Test with valid token containing DLEQ proofs
    }

    @Test
    @DisplayName("should report invalid DLEQ proof")
    void shouldReportInvalidDLEQ() {
        // Test with token containing invalid DLEQ
    }

    @Test
    @DisplayName("should handle token without DLEQ")
    void shouldHandleNoDLEQ() {
        // Test with token without DLEQ proofs
    }
}
```

---

## Implementation Details

### 11.1 Mathematical Operations

**Cryptographic Variables Reference:**
| Variable | Description |
|----------|-------------|
| `a` | Mint's private key (scalar) |
| `A` | Mint's public key: `A = a*G` |
| `G` | secp256k1 generator point |
| `n` | secp256k1 curve order |
| `B'` | Blinded message (point) |
| `C'` | Blind signature: `C' = a*B'` |
| `C` | Unblinded signature |
| `r` | User's blinding factor (scalar) |
| `Y` | `hash_to_curve(secret)` |
| `e` | DLEQ challenge (scalar) |
| `s` | DLEQ response (scalar) |

**Key Equations:**
1. Blinding: `B' = Y + r*G`
2. Signing: `C' = a*B'`
3. Unblinding: `C = C' - r*A`
4. DLEQ hash: `e = SHA256(R1 || R2 || A || C')`
5. DLEQ response: `s = r_nonce + e*a (mod n)`

### 11.2 Key Design Decisions

**Decision 1: Extend Existing Classes vs. New Classes**
- **Choice**: Extend existing `BlindSignature` and `Proof` classes with optional `dleq` field
- **Rationale**: Maintains backward compatibility; null DLEQ is valid (NUT-12 is optional)

**Decision 2: DLEQ in Proof includes Blinding Factor**
- **Choice**: Include `r` in Proof DLEQ for Carol verification
- **Rationale**: Required by spec; enables trustless P2P transfer
- **Security Note**: `r` must NEVER be shared with mint

**Decision 3: Verification is Mandatory When DLEQ Present**
- **Choice**: If DLEQ proof exists, verification MUST pass
- **Rationale**: Per spec requirement; prevents accepting forged tokens

**Decision 4: Uncompressed Point Format for Hash**
- **Choice**: Use uncompressed (04 prefix) points in hash function
- **Rationale**: Per spec; ensures interoperability

### 11.3 Security Considerations

1. **Private Key Protection**: DLEQ generation requires mint's private key; ensure key security
2. **Blinding Factor Confidentiality**: Never send `r` to mint; only include in user-to-user Proofs
3. **Verification Failure Handling**: Reject tokens with invalid DLEQ proofs
4. **Random Nonce Security**: Use cryptographically secure random for DLEQ nonce generation
5. **Timing Attacks**: Consider constant-time operations for scalar arithmetic

### 11.4 Testing Strategy

1. **Unit Tests**: Individual function correctness (hash, scalar ops, point ops)
2. **Integration Tests**: Full flow from mint signing to user verification
3. **Cross-Implementation Tests**: Verify against other Cashu implementations
4. **Test Vectors**: Known input→output pairs for reproducibility
5. **Fuzzing**: Random inputs to find edge cases
6. **Security Tests**: Verify rejection of invalid/forged proofs

---

## REST Endpoint Analysis

### No New Endpoints Required

NUT-12 is a **payload-only** extension. It does not introduce any new HTTP endpoints.

**Existing endpoints that return BlindSignature (automatically gain DLEQ support):**
| Endpoint | Response Class | Effect |
|----------|---------------|--------|
| `POST /v1/mint/{method}` | `PostMintResponse` | `signatures` array now includes optional `dleq` |
| `POST /v1/swap` | `PostSwapResponse` | `signatures` array now includes optional `dleq` |
| `POST /v1/restore` | `PostRestoreResponse` | Restored signatures include optional `dleq` |

**Why no REST changes needed:**
1. `PostMintResponse`, `PostSwapResponse`, etc. contain `List<BlindSignature>`
2. Adding `dleq` field to `BlindSignature` class automatically includes it in REST responses
3. Jackson's `@JsonInclude(NON_NULL)` ensures backward compatibility (field omitted when null)

**Existing DLEQ Structure:**
The `TokenV4.java` class already contains a nested `DLEQProof` class for V4 token format serialization. The implementation will extract this into a standalone reusable class in `cashu-lib-common`.

---

## References

- [NUT-12 Specification](https://github.com/cashubtc/nuts/blob/main/12.md) - Official spec
- [NUT-00 Notation](https://github.com/cashubtc/nuts/blob/main/00.md) - Base protocol
- [Schnorr DLEQ](https://cypherpunks.venona.com/date/1998/01/msg00058.html) - DLEQ background
- [secp256k1 Parameters](https://en.bitcoin.it/wiki/Secp256k1) - Curve parameters
- [cashu-lib BDHKEUtils](cashu-lib-crypto/src/main/java/xyz/tcheeric/cashu/crypto/BDHKEUtils.java) - Existing crypto

---

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.1 | 2025-12-21 | Phase 1 completed; added DLEQ primitives and tests | eric |
| 1.0 | 2025-12-12 | Initial implementation plan created | Claude |

*Last Updated: 2025-12-21*
*Status: Phase 1 complete*
*Total Tasks: 24 (21 required + 3 optional)*
*Progress: 17% Complete (4/24 tasks)*
