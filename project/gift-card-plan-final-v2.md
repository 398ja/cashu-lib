# Final Implementation Plan: Gift Card with Nostr (v2 - Multi-Module)

**Date**: 2025-11-04
**Status**: Ready for Implementation
**Architecture**: Top-level `cashu-voucher` project with 3 sub-modules
**NUT-13 Status**: ✅ Fully Complete (all 6 phases)

---

## Executive Summary

This is the **final implementation plan** with the improved architecture:

### Key Decisions

1. ✅ **cashu-voucher as top-level project** with 3 sub-modules (domain, app, nostr)
2. ✅ **Model B only** (no redemption at mint - vouchers spendable only at issuing merchant)
3. ✅ **Full Nostr storage** (both ledger and wallet vouchers on Nostr, no database)

### Architecture Philosophy

**Hexagonal Architecture** (Ports & Adapters):
- **Domain** = Core business logic (pure, no dependencies)
- **App** = Application services (ports/use cases)
- **Nostr** = Infrastructure adapter (storage/messaging)

This separation ensures:
- ✅ Domain is testable without infrastructure
- ✅ Nostr can be replaced with other adapters (IPFS, SQL, etc.)
- ✅ Application services are reusable across projects
- ✅ Clear dependency boundaries

---

## Project Summary

| Metric | Value |
|--------|-------|
| **Total Tasks** | 72 tasks (all required) |
| **Completed Tasks** | 6 tasks (8%) |
| **Current Phase** | Phase 1 - Domain Layer |
| **Estimated Duration** | 5 weeks (25 days) |
| **Start Date** | TBD |
| **Target Completion** | TBD |
| **Primary Developer** | TBD |
| **Reviewers** | TBD |
| **Branch** | TBD |
| **Related PRs** | TBD |
| **Blocked Items** | None |
| **Risk Level** | Medium - New infrastructure (Nostr) |
| **Breaking Changes** | None - New feature addition |
| **Documentation Required** | Yes - 5 guides created |
| **Testing Coverage Target** | 80%+ |

### Project Scope
- **In Scope**: Gift card vouchers, Nostr storage (NIP-33, NIP-17), Model B implementation, CLI commands, testing
- **Out of Scope**: Model A (mint redemption), hardware wallets, multi-currency paths, GUI applications
- **Dependencies**: cashu-lib (0.5.0), nostr-java (0.6.0), existing NUT-13 implementation, PicoCLI framework

### Key Milestones
1. ✅ Phase 0 Complete - Project structure ready
2. ⏳ Phase 1 Complete - Domain layer foundation
3. ⏳ Phase 2 Complete - Application services
4. ⏳ Phase 3 Complete - Nostr adapter
5. ⏳ Phase 4 Complete - Mint integration
6. ⏳ Phase 5 Complete - Wallet + CLI
7. ⏳ Phase 6 Complete - Testing & documentation
8. ⏳ Production Ready - v0.1.0 released

---

## Phase Summary

| Phase | Focus Area | Tasks | Estimated Time | Status |
|-------|-----------|-------|----------------|--------|
| **Phase 0** | Project Bootstrap | 6 tasks | 3 days | ✅ Complete (6/6) |
| **Phase 1** | Domain Layer | 11 tasks | 4 days | Not Started |
| **Phase 2** | Application Layer | 10 tasks | 3 days | Not Started |
| **Phase 3** | Nostr Layer | 11 tasks | 5 days | Not Started |
| **Phase 4** | Mint Integration | 8 tasks | 3 days | Not Started |
| **Phase 5** | Wallet & CLI | 14 tasks | 4 days | Not Started |
| **Phase 6** | Testing & Documentation | 12 tasks | 2 days | Not Started |
| **Total** | All Phases | **72 tasks** | **24 days** | **8% Complete** |

---

## Table of Contents

1. [Project Structure](#1-project-structure)
2. [Module Descriptions](#2-module-descriptions)
3. [Dependency Graph](#3-dependency-graph)
4. [Domain Layer (cashu-voucher-domain)](#4-domain-layer)
5. [Nostr Layer (cashu-voucher-nostr)](#5-nostr-layer)
6. [Application Layer (cashu-voucher-app)](#6-application-layer)
7. [Integration Points](#7-integration-points)
8. [Implementation Phases](#8-implementation-phases)
   - [Phase 0 – Project Bootstrap](#phase-0--project-bootstrap-week-1-3-days)
   - [Phase 1 – Domain Layer](#phase-1--domain-layer-week-1-days-4-5--week-2-days-1-2--4-days)
   - [Phase 2 – Application Layer](#phase-2--application-layer-week-2-days-3-5--3-days)
   - [Phase 3 – Nostr Layer](#phase-3--nostr-layer-week-3-5-days)
   - [Phase 4 – Mint Integration](#phase-4--mint-integration-week-4-days-1-3--3-days)
   - [Phase 5 – Wallet & CLI](#phase-5--wallet--cli-week-4-days-4-5--week-5-days-1-3--4-days)
   - [Phase 6 – Testing & Documentation](#phase-6--testing--documentation-week-5-days-4-5--2-days)
9. [Testing Strategy](#9-testing-strategy)
10. [Timeline](#10-timeline)

---

## 1. Project Structure

### 1.1 Repository Layout

```
/home/eric/IdeaProjects/
├── cashu-voucher/                    # NEW: Top-level project
│   ├── pom.xml                       # Parent POM
│   ├── README.md
│   ├── LICENSE
│   ├── .github/
│   │   └── workflows/
│   │       ├── build.yml
│   │       └── publish.yml
│   │
│   ├── cashu-voucher-domain/         # Module 1: Pure domain
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/xyz/tcheeric/cashu/voucher/domain/
│   │       │   ├── VoucherSecret.java
│   │       │   ├── SignedVoucher.java
│   │       │   ├── VoucherSignatureService.java
│   │       │   ├── VoucherValidator.java
│   │       │   ├── VoucherStatus.java (enum)
│   │       │   └── util/
│   │       │       └── VoucherSerializationUtils.java
│   │       └── test/java/
│   │           └── 50+ domain tests
│   │
│   ├── cashu-voucher-nostr/          # Module 2: Nostr adapter
│   │   ├── pom.xml
│   │   └── src/
│   │       ├── main/java/xyz/tcheeric/cashu/voucher/nostr/
│   │       │   ├── VoucherLedgerRepository.java (NIP-33)
│   │       │   ├── VoucherBackupRepository.java (NIP-17)
│   │       │   ├── NostrEventMapper.java
│   │       │   ├── NostrClientAdapter.java
│   │       │   ├── events/
│   │       │   │   ├── VoucherLedgerEvent.java
│   │       │   │   └── VoucherBackupPayload.java
│   │       │   └── config/
│   │       │       └── NostrRelayConfig.java
│   │       └── test/java/
│   │           └── 40+ Nostr integration tests
│   │
│   └── cashu-voucher-app/            # Module 3: Application services
│       ├── pom.xml
│       └── src/
│           ├── main/java/xyz/tcheeric/cashu/voucher/app/
│           │   ├── VoucherService.java
│           │   ├── VoucherIssuanceService.java
│           │   ├── VoucherRedemptionService.java
│           │   ├── VoucherBackupService.java
│           │   ├── MerchantVerificationService.java
│           │   ├── dto/
│           │   │   ├── IssueVoucherRequest.java
│           │   │   ├── IssueVoucherResponse.java
│           │   │   ├── RedeemVoucherRequest.java
│           │   │   └── StoredVoucher.java
│           │   └── ports/
│           │       ├── VoucherLedgerPort.java (interface)
│           │       └── VoucherBackupPort.java (interface)
│           └── test/java/
│               └── 50+ service tests
│
├── cashu-lib/                        # Existing project
├── cashu-wallet/                     # Existing project
├── cashu-mint/                       # Existing project
└── cashu-client/                     # Existing project
```

### 1.2 Parent POM

```xml
<!-- cashu-voucher/pom.xml -->
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-voucher</artifactId>
    <version>0.1.0</version>
    <packaging>pom</packaging>

    <name>Cashu Voucher</name>
    <description>Gift card voucher system with Nostr storage</description>

    <modules>
        <module>cashu-voucher-domain</module>
        <module>cashu-voucher-nostr</module>
        <module>cashu-voucher-app</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Dependency versions -->
        <cashu-lib.version>0.5.0</cashu-lib.version>
        <bip-utils.version>2.0.0</bip-utils.version>
        <nostr.version>0.6.0</nostr.version>
        <bouncycastle.version>1.78</bouncycastle.version>
        <jackson.version>2.17.0</jackson.version>
        <lombok.version>1.18.30</lombok.version>
        <junit.version>5.10.2</junit.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Internal modules -->
            <dependency>
                <groupId>xyz.tcheeric</groupId>
                <artifactId>cashu-voucher-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>xyz.tcheeric</groupId>
                <artifactId>cashu-voucher-nostr</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>xyz.tcheeric</groupId>
                <artifactId>cashu-voucher-app</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- Cashu dependencies -->
            <dependency>
                <groupId>xyz.tcheeric</groupId>
                <artifactId>cashu-lib-entities</artifactId>
                <version>${cashu-lib.version}</version>
            </dependency>
            <dependency>
                <groupId>xyz.tcheeric</groupId>
                <artifactId>cashu-lib-crypto</artifactId>
                <version>${cashu-lib.version}</version>
            </dependency>
            <dependency>
                <groupId>xyz.tcheeric</groupId>
                <artifactId>cashu-lib-common</artifactId>
                <version>${cashu-lib.version}</version>
            </dependency>

            <!-- Crypto -->
            <dependency>
                <groupId>org.bouncycastle</groupId>
                <artifactId>bcprov-jdk18on</artifactId>
                <version>${bouncycastle.version}</version>
            </dependency>

            <!-- Nostr -->
            <dependency>
                <groupId>nostr</groupId>
                <artifactId>nostr-base</artifactId>
                <version>${nostr.version}</version>
            </dependency>

            <!-- Utilities -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
                <scope>provided</scope>
            </dependency>

            <!-- Testing -->
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.11</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <distributionManagement>
        <repository>
            <id>maven-398ja-xyz</id>
            <url>https://maven.398ja.xyz/releases</url>
        </repository>
        <snapshotRepository>
            <id>maven-398ja-xyz</id>
            <url>https://maven.398ja.xyz/snapshots</url>
        </snapshotRepository>
    </distributionManagement>
</project>
```

---

## 2. Module Descriptions

### 2.1 cashu-voucher-domain

**Purpose**: Pure domain logic (no external dependencies except cashu-lib)

**Responsibilities**:
- Define `VoucherSecret` entity (extends `BaseKey`, implements `Secret`)
- Define `SignedVoucher` wrapper
- Provide signature service (ED25519)
- Provide validation logic (expiry, signature)
- Canonical serialization utilities

**Key Characteristic**: **Zero infrastructure dependencies** (no Nostr, no HTTP, no database)

**Dependencies**:
```xml
<dependencies>
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
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk18on</artifactId>
    </dependency>
</dependencies>
```

**Artifact**: `xyz.tcheeric:cashu-voucher-domain:0.1.0`

[↑ Back to top](#table-of-contents)

---

### 2.2 cashu-voucher-nostr

**Purpose**: Nostr infrastructure adapter (implements storage/messaging)

**Responsibilities**:
- Implement `VoucherLedgerPort` using NIP-33 (public ledger)
- Implement `VoucherBackupPort` using NIP-17 + NIP-44 (private backups)
- Provide Nostr event mappers (domain ↔ Nostr events)
- Manage relay connections (health checks, failover)
- Provide Nostr-specific configuration

**Key Characteristic**: **Adapter for Nostr protocol** (could be replaced with SQL adapter)

**Dependencies**:
```xml
<dependencies>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-app</artifactId>
        <scope>provided</scope>  <!-- Only needs port interfaces -->
    </dependency>
    <dependency>
        <groupId>nostr</groupId>
        <artifactId>nostr-base</artifactId>
    </dependency>
    <dependency>
        <groupId>nostr</groupId>
        <artifactId>nostr-encryption</artifactId>
    </dependency>
</dependencies>
```

**Artifact**: `xyz.tcheeric:cashu-voucher-nostr:0.1.0`

[↑ Back to top](#table-of-contents)

---

### 2.3 cashu-voucher-app

**Purpose**: Application services (use cases / business workflows)

**Responsibilities**:
- Define port interfaces (`VoucherLedgerPort`, `VoucherBackupPort`)
- Implement `VoucherService` (issue, list, status)
- Implement `VoucherBackupService` (backup, restore)
- Implement `VoucherIssuanceService` (mint integration)
- Implement `MerchantVerificationService` (Model B)
- Provide DTOs for API boundaries

**Key Characteristic**: **Infrastructure-agnostic** (depends on ports, not adapters)

**Dependencies**:
```xml
<dependencies>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-domain</artifactId>
    </dependency>
    <!-- No direct dependency on cashu-voucher-nostr! -->
    <!-- Nostr adapter is injected at runtime -->
</dependencies>
```

**Artifact**: `xyz.tcheeric:cashu-voucher-app:0.1.0`

[↑ Back to top](#table-of-contents)

---

## 3. Dependency Graph

### 3.1 Internal Module Dependencies

```
┌─────────────────────────┐
│  cashu-voucher-domain   │  ← Pure domain (VoucherSecret, SignedVoucher)
│  (no infrastructure)    │
└───────────┬─────────────┘
            │
            ├──────────────────────────────┐
            │                              │
            ▼                              ▼
┌─────────────────────────┐   ┌────────────────────────┐
│  cashu-voucher-app      │   │  cashu-voucher-nostr   │
│  (ports/use cases)      │   │  (Nostr adapter)       │
│                         │   │                        │
│  Defines:               │   │  Implements:           │
│  - VoucherLedgerPort    │◄──│  - VoucherLedgerPort   │
│  - VoucherBackupPort    │◄──│  - VoucherBackupPort   │
└─────────────────────────┘   └────────────────────────┘
```

**Key Principle**: App defines interfaces (ports), Nostr implements them (adapter).

### 3.2 External Dependencies

```
┌──────────────────────────────────────────────────────────────┐
│                     cashu-voucher                            │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐                   │
│  │ domain  │   │  app    │   │ nostr   │                   │
│  └────┬────┘   └────┬────┘   └────┬────┘                   │
└───────┼─────────────┼─────────────┼────────────────────────┘
        │             │             │
        ▼             ▼             ▼
   cashu-lib    cashu-lib      nostr-java
   (entities,   (entities)    (base, encryption)
    crypto,
    common)
```

### 3.3 Consumer Dependencies

**cashu-mint** (issuance + validation):
```xml
<dependencies>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-app</artifactId>
    </dependency>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-nostr</artifactId>
        <scope>runtime</scope>  <!-- Adapter injected at runtime -->
    </dependency>
</dependencies>
```

**cashu-wallet** (backup + storage):
```xml
<dependencies>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-app</artifactId>
    </dependency>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-nostr</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

**cashu-client** (CLI commands):
```xml
<dependencies>
    <dependency>
        <groupId>xyz.tcheeric</groupId>
        <artifactId>cashu-voucher-app</artifactId>
    </dependency>
    <!-- Nostr adapter provided by cashu-wallet transitively -->
</dependencies>
```

[↑ Back to top](#table-of-contents)

---

## 4. Domain Layer (cashu-voucher-domain)

### 4.1 VoucherSecret

**Location**: `cashu-voucher-domain/src/main/java/xyz/tcheeric/cashu/voucher/domain/VoucherSecret.java`

```java
package xyz.tcheeric.cashu.voucher.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import xyz.tcheeric.cashu.common.BaseKey;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.HexUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gift card voucher secret (Model B - spendable only at issuing merchant).
 *
 * <p>Follows the same immutable pattern as {@link xyz.tcheeric.cashu.common.DeterministicSecret}.
 * Vouchers are NOT deterministic and MUST be backed up to Nostr for recovery.
 */
@Getter
public class VoucherSecret extends BaseKey implements Secret {
    private final String voucherId;
    private final String issuerId;
    private final String unit;
    private final long faceValue;
    private final Long expiresAt;
    private final String memo;

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

    public static VoucherSecret create(
        String issuerId,
        String unit,
        long faceValue,
        Long expiresAt,
        String memo
    ) {
        return new VoucherSecret(
            UUID.randomUUID().toString(),
            issuerId,
            unit,
            faceValue,
            expiresAt,
            memo
        );
    }

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
     * Canonical serialization for deterministic signing.
     */
    public byte[] toCanonicalBytes() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (expiresAt != null) map.put("expiresAt", expiresAt);
        map.put("faceValue", faceValue);
        map.put("issuerId", issuerId);
        if (memo != null) map.put("memo", memo);
        map.put("unit", unit);
        map.put("voucherId", voucherId);

        return VoucherSerializationUtils.toCbor(map);
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

    public boolean isExpired() {
        return expiresAt != null && Instant.now().getEpochSecond() > expiresAt;
    }

    public boolean isValid() {
        return !isExpired();
    }
}
```

### 4.2 SignedVoucher

```java
package xyz.tcheeric.cashu.voucher.domain;

import lombok.Getter;

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

    public boolean verify() {
        return VoucherSignatureService.verify(
            secret,
            issuerSignature,
            issuerPublicKey
        );
    }

    public boolean isExpired() {
        return secret.isExpired();
    }

    public boolean isValid() {
        return verify() && !isExpired();
    }
}
```

### 4.3 VoucherSignatureService

```java
package xyz.tcheeric.cashu.voucher.domain;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import xyz.tcheeric.cashu.common.util.HexUtils;

public class VoucherSignatureService {

    public static byte[] sign(VoucherSecret secret, String issuerPrivateKeyHex) {
        byte[] privateKeyBytes = HexUtils.decode(issuerPrivateKeyHex);
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(privateKeyBytes, 0);

        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);

        byte[] message = secret.toCanonicalBytes();
        signer.update(message, 0, message.length);

        return signer.generateSignature();
    }

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

### 4.4 VoucherValidator

```java
package xyz.tcheeric.cashu.voucher.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class VoucherValidator {

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

    public static ValidationResult validate(SignedVoucher voucher) {
        List<String> errors = new ArrayList<>();

        if (!voucher.verify()) {
            errors.add("Invalid issuer signature");
        }

        if (voucher.isExpired()) {
            errors.add("Voucher has expired");
        }

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

### 4.5 VoucherStatus (Enum)

```java
package xyz.tcheeric.cashu.voucher.domain;

public enum VoucherStatus {
    ISSUED,      // Published to Nostr, ready for redemption
    REDEEMED,    // Redeemed by merchant
    REVOKED,     // Revoked by issuer
    EXPIRED      // Expiry time passed
}
```

[↑ Back to top](#table-of-contents)

---

## 5. Nostr Layer (cashu-voucher-nostr)

### 5.1 Port Interfaces (defined in app layer)

**Location**: `cashu-voucher-app/src/main/java/xyz/tcheeric/cashu/voucher/app/ports/`

```java
package xyz.tcheeric.cashu.voucher.app.ports;

import xyz.tcheeric.cashu.voucher.domain.SignedVoucher;
import xyz.tcheeric.cashu.voucher.domain.VoucherStatus;

import java.util.Optional;

/**
 * Port for voucher ledger operations (public audit trail).
 */
public interface VoucherLedgerPort {

    /**
     * Publish voucher to ledger (NIP-33 replaceable event).
     */
    void publish(SignedVoucher voucher, VoucherStatus status);

    /**
     * Query current status of voucher from ledger.
     */
    Optional<VoucherStatus> queryStatus(String voucherId);

    /**
     * Update voucher status (e.g., issued → redeemed).
     */
    void updateStatus(String voucherId, VoucherStatus newStatus);
}
```

```java
package xyz.tcheeric.cashu.voucher.app.ports;

import xyz.tcheeric.cashu.voucher.domain.SignedVoucher;

import java.util.List;

/**
 * Port for voucher backup operations (private user storage).
 */
public interface VoucherBackupPort {

    /**
     * Backup vouchers to Nostr (NIP-17 encrypted DM).
     */
    void backup(List<SignedVoucher> vouchers, String userNostrPrivateKey);

    /**
     * Restore vouchers from Nostr.
     */
    List<SignedVoucher> restore(String userNostrPrivateKey);
}
```

### 5.2 Nostr Adapter Implementation

**Location**: `cashu-voucher-nostr/src/main/java/xyz/tcheeric/cashu/voucher/nostr/`

```java
package xyz.tcheeric.cashu.voucher.nostr;

import xyz.tcheeric.cashu.voucher.app.ports.VoucherLedgerPort;
import xyz.tcheeric.cashu.voucher.domain.SignedVoucher;
import xyz.tcheeric.cashu.voucher.domain.VoucherStatus;
import nostr.event.impl.GenericEvent;

import java.util.Optional;

/**
 * Nostr implementation of VoucherLedgerPort using NIP-33.
 */
public class NostrVoucherLedgerRepository implements VoucherLedgerPort {

    private final NostrClientAdapter nostrClient;
    private final String mintIssuerPubkey;

    public NostrVoucherLedgerRepository(
        NostrClientAdapter nostrClient,
        String mintIssuerPubkey
    ) {
        this.nostrClient = nostrClient;
        this.mintIssuerPubkey = mintIssuerPubkey;
    }

    @Override
    public void publish(SignedVoucher voucher, VoucherStatus status) {
        // Convert to NIP-33 event (kind 30078, parameterized replaceable)
        VoucherLedgerEvent ledgerEvent = VoucherLedgerEvent.builder()
            .voucherId(voucher.getSecret().getVoucherId())
            .status(status.name().toLowerCase())
            .issuerId(voucher.getSecret().getIssuerId())
            .unit(voucher.getSecret().getUnit())
            .faceValue(voucher.getSecret().getFaceValue())
            .expiresAt(voucher.getSecret().getExpiresAt())
            .memo(voucher.getSecret().getMemo())
            .build();

        GenericEvent nostrEvent = ledgerEvent.toNostrEvent(mintIssuerPubkey);

        // Publish to configured relays
        nostrClient.publishEvent(nostrEvent);
    }

    @Override
    public Optional<VoucherStatus> queryStatus(String voucherId) {
        // Query NIP-33 event by 'd' tag
        GenericEvent event = nostrClient.queryEvent(
            30078,
            mintIssuerPubkey,
            "voucher:" + voucherId
        );

        if (event == null) {
            return Optional.empty();
        }

        VoucherLedgerEvent ledgerEvent = VoucherLedgerEvent.fromNostrEvent(event);
        return Optional.of(VoucherStatus.valueOf(ledgerEvent.getStatus().toUpperCase()));
    }

    @Override
    public void updateStatus(String voucherId, VoucherStatus newStatus) {
        // Query current voucher to get full details
        // Then publish updated event (NIP-33 replaces previous)
        // Implementation details...
    }
}
```

```java
package xyz.tcheeric.cashu.voucher.nostr;

import xyz.tcheeric.cashu.voucher.app.ports.VoucherBackupPort;
import xyz.tcheeric.cashu.voucher.domain.SignedVoucher;
import nostr.encryption.MessageCipher44;
import nostr.event.impl.GenericEvent;

import java.util.List;

/**
 * Nostr implementation of VoucherBackupPort using NIP-17 + NIP-44.
 */
public class NostrVoucherBackupRepository implements VoucherBackupPort {

    private final NostrClientAdapter nostrClient;

    public NostrVoucherBackupRepository(NostrClientAdapter nostrClient) {
        this.nostrClient = nostrClient;
    }

    @Override
    public void backup(List<SignedVoucher> vouchers, String userNostrPrivateKey) {
        // Serialize vouchers to JSON
        VoucherBackupPayload payload = VoucherBackupPayload.builder()
            .version(1)
            .timestamp(System.currentTimeMillis() / 1000)
            .vouchers(mapToBackupVouchers(vouchers))
            .build();

        String json = payload.toJson();

        // Encrypt with NIP-44
        MessageCipher44 cipher = new MessageCipher44(
            HexUtils.decode(userNostrPrivateKey),
            HexUtils.decode(userNostrPrivateKey)  // Self-encryption
        );
        String encrypted = cipher.encrypt(json);

        // Create NIP-17 DM event (kind 14)
        GenericEvent event = new GenericEvent(userNostrPubkey(userNostrPrivateKey), 14);
        event.addTag("p", userNostrPubkey(userNostrPrivateKey));  // Self DM
        event.addTag("subject", "cashu-voucher-backup");
        event.setContent(encrypted);

        // Publish to relays
        nostrClient.publishEvent(event);
    }

    @Override
    public List<SignedVoucher> restore(String userNostrPrivateKey) {
        // Query NIP-17 events (kind 14, self DM)
        List<GenericEvent> events = nostrClient.queryEvents(
            14,
            userNostrPubkey(userNostrPrivateKey),
            "cashu-voucher-backup"
        );

        // Decrypt and merge
        List<SignedVoucher> allVouchers = new ArrayList<>();
        for (GenericEvent event : events) {
            String encrypted = event.getContent();

            MessageCipher44 cipher = new MessageCipher44(
                HexUtils.decode(userNostrPrivateKey),
                HexUtils.decode(userNostrPrivateKey)
            );
            String json = cipher.decrypt(encrypted);

            VoucherBackupPayload payload = VoucherBackupPayload.fromJson(json);
            allVouchers.addAll(mapFromBackupVouchers(payload.getVouchers()));
        }

        // Deduplicate by voucher ID (latest timestamp wins)
        return deduplicateVouchers(allVouchers);
    }

    private String userNostrPubkey(String privateKey) {
        // Derive public key from private key
        // Implementation...
    }
}
```

[↑ Back to top](#table-of-contents)

---

## 6. Application Layer (cashu-voucher-app)

### 6.1 VoucherService

**Location**: `cashu-voucher-app/src/main/java/xyz/tcheeric/cashu/voucher/app/VoucherService.java`

```java
package xyz.tcheeric.cashu.voucher.app;

import xyz.tcheeric.cashu.voucher.app.ports.VoucherLedgerPort;
import xyz.tcheeric.cashu.voucher.app.ports.VoucherBackupPort;
import xyz.tcheeric.cashu.voucher.domain.*;
import xyz.tcheeric.cashu.voucher.app.dto.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Main voucher service (orchestrates use cases).
 */
public class VoucherService {

    private final VoucherLedgerPort ledgerPort;
    private final VoucherBackupPort backupPort;
    private final String mintIssuerPrivateKey;
    private final String mintIssuerPublicKey;

    public VoucherService(
        VoucherLedgerPort ledgerPort,
        VoucherBackupPort backupPort,
        String mintIssuerPrivateKey,
        String mintIssuerPublicKey
    ) {
        this.ledgerPort = ledgerPort;
        this.backupPort = backupPort;
        this.mintIssuerPrivateKey = mintIssuerPrivateKey;
        this.mintIssuerPublicKey = mintIssuerPublicKey;
    }

    /**
     * Issue a new voucher.
     */
    public IssueVoucherResponse issue(IssueVoucherRequest request) {
        // Calculate expiry
        Long expiresAt = null;
        if (request.getExpiresInDays() != null) {
            expiresAt = Instant.now()
                .plus(request.getExpiresInDays(), ChronoUnit.DAYS)
                .getEpochSecond();
        }

        // Create voucher secret
        VoucherSecret secret = VoucherSecret.create(
            request.getIssuerId(),
            request.getUnit(),
            request.getAmount(),
            expiresAt,
            request.getMemo()
        );

        // Sign voucher
        SignedVoucher signedVoucher = VoucherSignatureService.createSigned(
            secret,
            mintIssuerPrivateKey,
            mintIssuerPublicKey
        );

        // Publish to Nostr ledger
        ledgerPort.publish(signedVoucher, VoucherStatus.ISSUED);

        return IssueVoucherResponse.builder()
            .voucher(signedVoucher)
            .token(serializeToToken(signedVoucher))
            .build();
    }

    /**
     * Query voucher status from Nostr ledger.
     */
    public Optional<VoucherStatus> queryStatus(String voucherId) {
        return ledgerPort.queryStatus(voucherId);
    }

    /**
     * Backup vouchers to Nostr.
     */
    public void backup(List<SignedVoucher> vouchers, String userNostrPrivateKey) {
        backupPort.backup(vouchers, userNostrPrivateKey);
    }

    /**
     * Restore vouchers from Nostr.
     */
    public List<SignedVoucher> restore(String userNostrPrivateKey) {
        return backupPort.restore(userNostrPrivateKey);
    }

    private String serializeToToken(SignedVoucher voucher) {
        // Serialize to Cashu token format (v4)
        // Implementation...
        return "cashuA...";
    }
}
```

### 6.2 MerchantVerificationService

```java
package xyz.tcheeric.cashu.voucher.app;

import xyz.tcheeric.cashu.voucher.app.ports.VoucherLedgerPort;
import xyz.tcheeric.cashu.voucher.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Merchant-side voucher verification (Model B).
 */
public class MerchantVerificationService {

    private final VoucherLedgerPort ledgerPort;

    public MerchantVerificationService(VoucherLedgerPort ledgerPort) {
        this.ledgerPort = ledgerPort;
    }

    /**
     * Verify voucher offline (signature + expiry only).
     */
    public VerificationResult verifyOffline(SignedVoucher voucher, String expectedIssuerId) {
        List<String> errors = new ArrayList<>();

        // Check issuer
        if (!voucher.getSecret().getIssuerId().equals(expectedIssuerId)) {
            errors.add("Voucher issued by different merchant");
        }

        // Validate signature + expiry
        VoucherValidator.ValidationResult validation = VoucherValidator.validate(voucher);
        if (!validation.isValid()) {
            errors.addAll(validation.getErrors());
        }

        return new VerificationResult(errors.isEmpty(), errors);
    }

    /**
     * Verify voucher online (includes Nostr ledger check).
     */
    public VerificationResult verifyOnline(SignedVoucher voucher, String expectedIssuerId) {
        // First, offline verification
        VerificationResult offlineResult = verifyOffline(voucher, expectedIssuerId);
        if (!offlineResult.isValid()) {
            return offlineResult;
        }

        // Query Nostr ledger
        Optional<VoucherStatus> status = ledgerPort.queryStatus(
            voucher.getSecret().getVoucherId()
        );

        if (status.isEmpty()) {
            return VerificationResult.failure("Voucher not found in ledger");
        }

        if (status.get() == VoucherStatus.REDEEMED) {
            return VerificationResult.failure("Voucher already redeemed (double-spend attempt)");
        }

        if (status.get() == VoucherStatus.REVOKED) {
            return VerificationResult.failure("Voucher has been revoked");
        }

        return VerificationResult.success();
    }

    public static class VerificationResult {
        private final boolean valid;
        private final List<String> errors;

        public VerificationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static VerificationResult success() {
            return new VerificationResult(true, new ArrayList<>());
        }

        public static VerificationResult failure(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new VerificationResult(false, errors);
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }
}
```

[↑ Back to top](#table-of-contents)

---

## 7. Integration Points

### 7.1 cashu-mint Integration

**Dependencies**:
```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-voucher-domain</artifactId>
</dependency>
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-voucher-app</artifactId>
</dependency>
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-voucher-nostr</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Configuration** (`application.yml`):
```yaml
voucher:
  mint:
    issuerPrivateKey: ${MINT_VOUCHER_ISSUER_PRIVKEY}
    issuerPublicKey: ${MINT_VOUCHER_ISSUER_PUBKEY}
  nostr:
    relays:
      - wss://relay.damus.io
      - wss://relay.cashu.xyz
```

**Bean Configuration**:
```java
@Configuration
public class VoucherConfiguration {

    @Bean
    public VoucherLedgerPort voucherLedgerPort(NostrClientAdapter nostrClient) {
        return new NostrVoucherLedgerRepository(
            nostrClient,
            voucherProperties.getMint().getIssuerPublicKey()
        );
    }

    @Bean
    public VoucherBackupPort voucherBackupPort(NostrClientAdapter nostrClient) {
        return new NostrVoucherBackupRepository(nostrClient);
    }

    @Bean
    public VoucherService voucherService(
        VoucherLedgerPort ledgerPort,
        VoucherBackupPort backupPort
    ) {
        return new VoucherService(
            ledgerPort,
            backupPort,
            voucherProperties.getMint().getIssuerPrivateKey(),
            voucherProperties.getMint().getIssuerPublicKey()
        );
    }
}
```

**REST Endpoint**:
```java
@RestController
@RequestMapping("/v1/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    public ResponseEntity<IssueVoucherResponse> issueVoucher(
        @RequestBody IssueVoucherRequest request
    ) {
        IssueVoucherResponse response = voucherService.issue(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{voucherId}/status")
    public ResponseEntity<VoucherStatusResponse> getStatus(
        @PathVariable String voucherId
    ) {
        Optional<VoucherStatus> status = voucherService.queryStatus(voucherId);
        return status
            .map(s -> ResponseEntity.ok(new VoucherStatusResponse(s)))
            .orElse(ResponseEntity.notFound().build());
    }
}
```

**Validation Hook** (Model B rejection):
```java
@Component
public class ProofValidator {

    public void validateProof(Proof proof) {
        if (proof.getSecret() instanceof VoucherSecret) {
            throw new VoucherNotAcceptedException(
                "Vouchers cannot be redeemed at mint (Model B). " +
                "Please redeem with issuing merchant."
            );
        }

        // ... existing validation ...
    }
}
```

### 7.2 cashu-wallet Integration

**Dependencies**: Same as mint

**WalletState Extension**:
```java
public class WalletState {
    // ... existing fields ...

    @JsonProperty("vouchers")
    private List<StoredVoucher> vouchers = new ArrayList<>();

    @JsonProperty("voucher_backup_state")
    private VoucherBackupState backupState = new VoucherBackupState();

    @JsonProperty("encrypted_nostr_privkey")
    private String encryptedNostrPrivkey;
}
```

**Service Usage**:
```java
@Service
public class WalletVoucherService {

    private final VoucherService voucherService;
    private final WalletState walletState;

    public void issueAndBackup(IssueVoucherRequest request) {
        // Issue voucher
        IssueVoucherResponse response = voucherService.issue(request);

        // Store locally
        walletState.getVouchers().add(StoredVoucher.from(response.getVoucher()));

        // Auto-backup to Nostr
        voucherService.backup(
            List.of(response.getVoucher()),
            walletState.getNostrPrivateKey()
        );
    }

    public void restoreFromNostr() {
        List<SignedVoucher> restored = voucherService.restore(
            walletState.getNostrPrivateKey()
        );

        // Merge with local state
        for (SignedVoucher voucher : restored) {
            addOrUpdate(voucher);
        }
    }
}
```

### 7.3 cashu-client Integration (CLI)

**Dependency**: Only `cashu-voucher-app` (Nostr adapter via wallet transitively)

**Commands**:
```java
@Command(name = "voucher-issue")
public class IssueVoucherCmd extends WalletServiceCommand<Integer> {

    @Inject
    private WalletVoucherService voucherService;

    @Override
    protected Integer execute(WalletService walletService) {
        IssueVoucherRequest request = IssueVoucherRequest.builder()
            .amount(amount)
            .memo(memo)
            .expiresInDays(expiryDays)
            .build();

        voucherService.issueAndBackup(request);

        System.out.println("✅ Voucher issued and backed up to Nostr");
        return 0;
    }
}
```

[↑ Back to top](#table-of-contents)

---

## 8. Implementation Phases

### Task Table Legend

**ID Column**: Unique identifier in format `{Phase}.{Task}` for cross-referencing

**Status Values**:
- ✓ Complete - Task finished and committed
- Pending - Not yet started
- In Progress - Currently being worked on
- Blocked - Cannot proceed due to dependencies

**Task Size**:
- Small - < 4 hours
- Medium - 4-8 hours
- Large - > 8 hours (1+ days)

**Priority**:
- P0 - Critical, required for core functionality
- P1 - Important, required for production quality
- P2 - Optional, enhancement only

---

### Phase 0 – Project Bootstrap (Week 1, 3 days)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 0.1 | Create cashu-voucher repository | ✓ Complete | Small | P0 | None | `/home/eric/IdeaProjects/cashu-voucher/` | 3e8bac0 | Initialize git, add .gitignore, README, LICENSE |
| 0.2 | Create parent POM | ✓ Complete | Small | P0 | 0.1 | `cashu-voucher/pom.xml` | c42d2de | Define 3 modules, dependency management, JaCoCo 80% |
| 0.3 | Create module skeletons | ✓ Complete | Medium | P0 | 0.2 | All 3 modules | 3f72178 | Maven structure, src directories, module POMs |
| 0.4 | Setup GitHub Actions CI/CD | ✓ Complete | Medium | P0 | 0.3 | `.github/workflows/build.yml` | 0dfe22f | Build, test, JaCoCo coverage, artifact upload |
| 0.5 | Configure Maven publishing | ✓ Complete | Small | P0 | 0.4 | `pom.xml` distributionManagement | 7a16572 | Deploy plugin, source/javadoc JARs |
| 0.6 | Verify build pipeline | ✓ Complete | Small | P1 | 0.4, 0.5 | BUILD.md | 3065444 | All modules compile, artifacts generated |

**Deliverables**:
- [x] Repository structure complete
- [x] Parent POM configured
- [x] CI/CD pipeline green
- [x] Empty modules building successfully

[↑ Back to top](#table-of-contents)

---

### Phase 1 – Domain Layer (Week 1, Days 4-5 + Week 2, Days 1-2 = 4 days)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 1.1 | Implement VoucherSecret | Pending | Large | P0 | Phase 0 | `cashu-voucher-domain/src/main/java/.../VoucherSecret.java` | - | Extends BaseKey, implements Secret |
| 1.2 | Implement SignedVoucher | Pending | Medium | P0 | 1.1 | `cashu-voucher-domain/src/main/java/.../SignedVoucher.java` | - | Wrapper with signature |
| 1.3 | Implement VoucherSignatureService | Pending | Large | P0 | 1.1, 1.2 | `cashu-voucher-domain/src/main/java/.../VoucherSignatureService.java` | - | ED25519 sign/verify |
| 1.4 | Implement VoucherValidator | Pending | Medium | P0 | 1.2, 1.3 | `cashu-voucher-domain/src/main/java/.../VoucherValidator.java` | - | Validation logic |
| 1.5 | Implement VoucherStatus enum | Pending | Small | P0 | None | `cashu-voucher-domain/src/main/java/.../VoucherStatus.java` | - | ISSUED, REDEEMED, REVOKED, EXPIRED |
| 1.6 | Implement VoucherSerializationUtils | Pending | Medium | P0 | 1.1 | `cashu-voucher-domain/src/main/java/.../util/VoucherSerializationUtils.java` | - | CBOR serialization |
| 1.7 | Write VoucherSecret unit tests | Pending | Large | P1 | 1.1, 1.6 | `cashu-voucher-domain/src/test/java/` | - | 20+ tests: creation, serialization, expiry |
| 1.8 | Write SignedVoucher unit tests | Pending | Medium | P1 | 1.2 | `cashu-voucher-domain/src/test/java/` | - | 10+ tests: verification, validity |
| 1.9 | Write VoucherSignatureService tests | Pending | Large | P1 | 1.3 | `cashu-voucher-domain/src/test/java/` | - | 15+ tests: sign/verify, test vectors |
| 1.10 | Write VoucherValidator tests | Pending | Medium | P1 | 1.4 | `cashu-voucher-domain/src/test/java/` | - | 10+ tests: validation rules |
| 1.11 | Verify 80%+ code coverage | Pending | Small | P1 | 1.7-1.10 | JaCoCo report | - | Run mvn jacoco:report |

**Deliverables**:
- [ ] `cashu-voucher-domain-0.1.0.jar`
- [ ] 50+ unit tests passing
- [ ] 80%+ code coverage
- [ ] Published to Maven

[↑ Back to top](#table-of-contents)

---

### Phase 2 – Application Layer (Week 2, Days 3-5 = 3 days)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 2.1 | Define VoucherLedgerPort interface | Pending | Small | P0 | Phase 1 | `cashu-voucher-app/src/main/java/.../ports/VoucherLedgerPort.java` | - | publish, queryStatus, updateStatus |
| 2.2 | Define VoucherBackupPort interface | Pending | Small | P0 | Phase 1 | `cashu-voucher-app/src/main/java/.../ports/VoucherBackupPort.java` | - | backup, restore |
| 2.3 | Create DTOs | Pending | Medium | P0 | Phase 1 | `cashu-voucher-app/src/main/java/.../dto/` | - | Request/Response classes |
| 2.4 | Implement VoucherService | Pending | Large | P0 | 2.1, 2.2, 2.3 | `cashu-voucher-app/src/main/java/.../VoucherService.java` | - | Orchestrates use cases |
| 2.5 | Implement VoucherIssuanceService | Pending | Medium | P0 | 2.4 | `cashu-voucher-app/src/main/java/.../VoucherIssuanceService.java` | - | Mint integration logic |
| 2.6 | Implement VoucherBackupService | Pending | Medium | P0 | 2.2 | `cashu-voucher-app/src/main/java/.../VoucherBackupService.java` | - | Backup orchestration |
| 2.7 | Implement MerchantVerificationService | Pending | Large | P0 | 2.1 | `cashu-voucher-app/src/main/java/.../MerchantVerificationService.java` | - | Offline + online verify |
| 2.8 | Write VoucherService tests | Pending | Large | P1 | 2.4 | `cashu-voucher-app/src/test/java/` | - | 20+ tests with mocked ports |
| 2.9 | Write MerchantVerificationService tests | Pending | Medium | P1 | 2.7 | `cashu-voucher-app/src/test/java/` | - | 15+ tests: offline/online verify |
| 2.10 | Write DTO serialization tests | Pending | Small | P1 | 2.3 | `cashu-voucher-app/src/test/java/` | - | 5+ tests: JSON round-trip |

**Deliverables**:
- [ ] `cashu-voucher-app-0.1.0.jar`
- [ ] 40+ service tests passing
- [ ] Port interfaces defined

[↑ Back to top](#table-of-contents)

---

### Phase 3 – Nostr Layer (Week 3, 5 days)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 3.1 | Implement NostrClientAdapter | Pending | Large | P0 | Phase 2 | `cashu-voucher-nostr/src/main/java/.../NostrClientAdapter.java` | - | Relay connection management |
| 3.2 | Implement VoucherLedgerEvent | Pending | Medium | P0 | 3.1 | `cashu-voucher-nostr/src/main/java/.../events/VoucherLedgerEvent.java` | - | NIP-33 event mapper |
| 3.3 | Implement VoucherBackupPayload | Pending | Medium | P0 | 3.1 | `cashu-voucher-nostr/src/main/java/.../events/VoucherBackupPayload.java` | - | NIP-17 payload format |
| 3.4 | Implement NostrVoucherLedgerRepository | Pending | Large | P0 | 3.1, 3.2 | `cashu-voucher-nostr/src/main/java/.../NostrVoucherLedgerRepository.java` | - | Implements VoucherLedgerPort |
| 3.5 | Implement NostrVoucherBackupRepository | Pending | Large | P0 | 3.1, 3.3 | `cashu-voucher-nostr/src/main/java/.../NostrVoucherBackupRepository.java` | - | Implements VoucherBackupPort, NIP-44 |
| 3.6 | Create NostrRelayConfig | Pending | Small | P0 | 3.1 | `cashu-voucher-nostr/src/main/java/.../config/NostrRelayConfig.java` | - | Relay URLs, timeouts |
| 3.7 | Create MockNostrRelay for testing | Pending | Large | P1 | Phase 2 | `cashu-voucher-nostr/src/test/java/.../MockNostrRelay.java` | - | In-memory relay simulator |
| 3.8 | Write NostrClientAdapter tests | Pending | Medium | P1 | 3.1, 3.7 | `cashu-voucher-nostr/src/test/java/` | - | 10+ tests: connection, publishing |
| 3.9 | Write NostrVoucherLedgerRepository tests | Pending | Large | P1 | 3.4, 3.7 | `cashu-voucher-nostr/src/test/java/` | - | 15+ tests: publish, query, update |
| 3.10 | Write NostrVoucherBackupRepository tests | Pending | Large | P1 | 3.5, 3.7 | `cashu-voucher-nostr/src/test/java/` | - | 15+ tests: backup, restore, encryption |
| 3.11 | Write event mapper tests | Pending | Medium | P1 | 3.2, 3.3 | `cashu-voucher-nostr/src/test/java/` | - | 10+ tests: domain ↔ Nostr conversion |

**Deliverables**:
- [ ] `cashu-voucher-nostr-0.1.0.jar`
- [ ] 40+ integration tests passing
- [ ] Mock Nostr relay for testing

[↑ Back to top](#table-of-contents)

---

### Phase 4 – Mint Integration (Week 4, Days 1-3 = 3 days)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 4.1 | Add cashu-voucher dependencies | Pending | Small | P0 | Phase 1-3 | `cashu-mint/pom.xml` | - | domain, app, nostr modules |
| 4.2 | Create VoucherConfiguration | Pending | Medium | P0 | 4.1 | `cashu-mint/.../config/VoucherConfiguration.java` | - | Spring beans for voucher services |
| 4.3 | Create VoucherController | Pending | Large | P0 | 4.2 | `cashu-mint/.../controller/VoucherController.java` | - | POST /v1/vouchers, GET /v1/vouchers/{id}/status |
| 4.4 | Update ProofValidator | Pending | Medium | P0 | 4.1 | `cashu-mint/.../validator/ProofValidator.java` | - | Model B rejection: instanceof VoucherSecret |
| 4.5 | Create application-voucher.yml | Pending | Small | P0 | 4.2 | `cashu-mint/src/main/resources/application-voucher.yml` | - | Nostr relay URLs, issuer keys |
| 4.6 | Write VoucherController tests | Pending | Large | P1 | 4.3 | `cashu-mint/src/test/java/` | - | 10+ tests: issue, query status |
| 4.7 | Write ProofValidator tests | Pending | Medium | P1 | 4.4 | `cashu-mint/src/test/java/` | - | 5+ tests: Model B rejection |
| 4.8 | Write Nostr integration tests | Pending | Medium | P1 | 4.3 | `cashu-mint/src/test/java/` | - | 5+ tests: verify Nostr publishing |

**Deliverables**:
- [ ] Voucher issuance API working
- [ ] Model B rejection working
- [ ] 20+ integration tests passing

[↑ Back to top](#table-of-contents)

---

### Phase 5 – Wallet & CLI (Week 4, Days 4-5 + Week 5, Days 1-3 = 4 days)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 5.1 | Add cashu-voucher dependencies | Pending | Small | P0 | Phase 1-3 | `cashu-wallet/pom.xml` | - | domain, app, nostr modules |
| 5.2 | Extend WalletState | Pending | Medium | P0 | 5.1 | `cashu-wallet/.../state/WalletState.java` | - | Add vouchers, backupState fields |
| 5.3 | Create StoredVoucher DTO | Pending | Small | P0 | 5.2 | `cashu-wallet/.../dto/StoredVoucher.java` | - | Wallet-specific voucher representation |
| 5.4 | Implement WalletVoucherService | Pending | Large | P0 | 5.2, 5.3 | `cashu-wallet/.../service/WalletVoucherService.java` | - | issueAndBackup, restoreFromNostr |
| 5.5 | Create IssueVoucherCmd | Pending | Medium | P0 | 5.4 | `cashu-client/.../command/IssueVoucherCmd.java` | - | CLI: cashu voucher issue |
| 5.6 | Create ListVouchersCmd | Pending | Small | P0 | 5.4 | `cashu-client/.../command/ListVouchersCmd.java` | - | CLI: cashu voucher list |
| 5.7 | Create ShowVoucherCmd | Pending | Small | P0 | 5.4 | `cashu-client/.../command/ShowVoucherCmd.java` | - | CLI: cashu voucher show |
| 5.8 | Create BackupVouchersCmd | Pending | Medium | P0 | 5.4 | `cashu-client/.../command/BackupVouchersCmd.java` | - | CLI: cashu voucher backup |
| 5.9 | Create RestoreVouchersCmd | Pending | Medium | P0 | 5.4 | `cashu-client/.../command/RestoreVouchersCmd.java` | - | CLI: cashu voucher restore |
| 5.10 | Create MerchantVerifyCmd | Pending | Medium | P0 | 5.4 | `cashu-client/.../command/MerchantVerifyCmd.java` | - | CLI: cashu merchant verify |
| 5.11 | Create MerchantRedeemCmd | Pending | Medium | P0 | 5.4 | `cashu-client/.../command/MerchantRedeemCmd.java` | - | CLI: cashu merchant redeem |
| 5.12 | Enhance RecoverWalletCmd | Pending | Medium | P1 | 5.4 | `cashu-client/.../command/RecoverWalletCmd.java` | - | Add --include-vouchers flag |
| 5.13 | Write WalletVoucherService tests | Pending | Large | P1 | 5.4 | `cashu-wallet/src/test/java/` | - | 15+ tests: issue, backup, restore |
| 5.14 | Write CLI command tests | Pending | Large | P1 | 5.5-5.12 | `cashu-client/src/test/java/` | - | 15+ tests: command execution, output |

**Deliverables**:
- [ ] Wallet voucher storage working
- [ ] 9 CLI commands functional
- [ ] NUT-13 integration complete
- [ ] 30+ CLI tests passing

[↑ Back to top](#table-of-contents)

---

### Phase 6 – Testing & Documentation (Week 5, Days 4-5 = 2 days)

| ID | Task | Status | Task Size | Priority | Dependency | Location | Commit | Notes |
|----|------|--------|-----------|----------|------------|----------|--------|-------|
| 6.1 | Write E2E test: Issue → Verify Nostr | Pending | Medium | P0 | Phase 4, 5 | `cashu-mint/src/test/java/` | - | Full issuance flow |
| 6.2 | Write E2E test: Delete → Restore | Pending | Medium | P0 | Phase 5 | `cashu-wallet/src/test/java/` | - | Backup/restore flow |
| 6.3 | Write E2E test: Model B rejection | Pending | Small | P0 | Phase 4 | `cashu-mint/src/test/java/` | - | Swap/melt rejection |
| 6.4 | Write E2E test: Merchant verify | Pending | Medium | P0 | Phase 5 | Integration tests | - | Offline + online verify |
| 6.5 | Write E2E test: NUT-13 integration | Pending | Large | P0 | Phase 5 | Integration tests | - | Deterministic recovery with vouchers |
| 6.6 | Create README.md | Pending | Medium | P1 | Phase 0-5 | `cashu-voucher/README.md` | - | Project overview, quick start |
| 6.7 | Update NUT-13 recovery docs | Pending | Small | P1 | 6.6 | Documentation | - | Add voucher recovery section |
| 6.8 | Write JMH performance benchmarks | Pending | Large | P1 | Phase 1-3 | `cashu-voucher-domain/src/jmh/` | - | Signature, serialization benchmarks |
| 6.9 | Write performance report | Pending | Medium | P1 | 6.8 | `docs/PERFORMANCE.md` | - | Benchmark results, analysis |
| 6.10 | Final code review | Pending | Large | P1 | Phase 0-5 | All modules | - | Code quality, consistency |
| 6.11 | Update CHANGELOG | Pending | Small | P1 | 6.10 | `CHANGELOG.md` | - | Document all changes |
| 6.12 | Tag v0.1.0 release | Pending | Small | P0 | 6.11 | Git tag | - | Create release tag |

**Deliverables**:
- [ ] 20+ E2E tests passing
- [ ] Complete documentation
- [ ] Performance report
- [ ] Production-ready release

[↑ Back to top](#table-of-contents)

---

## 9. Testing Strategy

### 9.1 Unit Tests

**cashu-voucher-domain** (50+ tests):
- VoucherSecret serialization
- SignedVoucher validation
- Signature generation/verification
- Edge cases (expiry, invalid values)

**cashu-voucher-app** (40+ tests):
- VoucherService use cases (mocking ports)
- MerchantVerificationService logic
- DTO serialization

**cashu-voucher-nostr** (40+ tests):
- Event mapping (domain ↔ Nostr)
- Encryption/decryption (NIP-44)
- Mock Nostr relay interactions

### 9.2 Integration Tests

**cashu-mint** (20+ tests):
- Voucher issuance API
- Model B rejection (swap/melt)
- Nostr ledger publishing

**cashu-wallet** (20+ tests):
- Backup to Nostr
- Restore from Nostr
- Conflict resolution

**cashu-client** (30+ tests):
- CLI command execution
- Output verification

### 9.3 End-to-End Tests (20+ tests)

**Scenarios**:
1. Issue voucher → verify Nostr event published
2. Delete local → restore from Nostr → verify integrity
3. Attempt swap at mint → verify rejection
4. Merchant verify voucher → check online
5. NUT-13 recovery with `--include-vouchers`

[↑ Back to top](#table-of-contents)

---

## 10. Timeline

| Week | Phase | Module | LOC | Tests | Deliverables |
|------|-------|--------|-----|-------|--------------|
| 1 (Days 1-3) | Phase 0 | All | - | - | Project structure, CI/CD |
| 1 (Days 4-5) + 2 (Days 1-2) | Phase 1 | domain | 800 | 50 | Domain types, validation |
| 2 (Days 3-5) | Phase 2 | app | 600 | 40 | Services, ports |
| 3 | Phase 3 | nostr | 700 | 40 | Nostr adapter |
| 4 (Days 1-3) | Phase 4 | mint | 400 | 20 | Mint integration |
| 4 (Days 4-5) + 5 (Days 1-3) | Phase 5 | wallet/client | 600 | 30 | Wallet + CLI |
| 5 (Days 4-5) | Phase 6 | All | - | 20 | E2E tests, docs |
| **Total** | **5 weeks** | **3 modules** | **3100** | **200** | **Production-ready** |

[↑ Back to top](#table-of-contents)

---

## 11. Success Criteria

**Phase 0 Complete**:
- [x] Repository created with 3 modules
- [x] CI/CD pipeline passing
- [x] Published to maven.398ja.xyz

**Phase 1 Complete**:
- [x] cashu-voucher-domain-0.1.0 published
- [x] 50+ unit tests passing
- [x] 80%+ code coverage

**Phase 2 Complete**:
- [x] cashu-voucher-app-0.1.0 published
- [x] Port interfaces defined
- [x] 40+ service tests passing

**Phase 3 Complete**:
- [x] cashu-voucher-nostr-0.1.0 published
- [x] NIP-33 + NIP-17 implemented
- [x] 40+ integration tests passing

**Phase 4 Complete**:
- [x] Mint voucher API working
- [x] Model B rejection working
- [x] 20+ integration tests passing

**Phase 5 Complete**:
- [x] Wallet voucher storage working
- [x] 9 CLI commands functional
- [x] NUT-13 integration working
- [x] 30+ CLI tests passing

**Phase 6 Complete**:
- [x] 20+ E2E tests passing
- [x] Documentation complete (5 guides)
- [x] Performance benchmarks documented
- [x] Production-ready (v0.1.0 released)

[↑ Back to top](#table-of-contents)

---

## 12. Key Advantages of This Architecture

### 12.1 Testability

**Domain Layer**:
```java
// Pure unit tests, no mocks needed
@Test
void testVoucherSignature() {
    VoucherSecret secret = VoucherSecret.create("issuer", "sat", 5000, null, null);
    byte[] signature = VoucherSignatureService.sign(secret, privateKey);
    assertTrue(VoucherSignatureService.verify(secret, signature, publicKey));
}
```

**Application Layer**:
```java
// Service tests with port mocks
@Test
void testIssueVoucher() {
    VoucherLedgerPort mockLedger = mock(VoucherLedgerPort.class);
    VoucherService service = new VoucherService(mockLedger, mockBackup, privKey, pubKey);

    IssueVoucherResponse response = service.issue(request);

    verify(mockLedger).publish(any(), eq(VoucherStatus.ISSUED));
}
```

**Nostr Layer**:
```java
// Integration tests with mock relay
@Test
void testNostrPublish() {
    MockNostrRelay relay = new MockNostrRelay();
    NostrVoucherLedgerRepository repo = new NostrVoucherLedgerRepository(relay, pubkey);

    repo.publish(voucher, VoucherStatus.ISSUED);

    assertEquals(1, relay.getPublishedEvents().size());
}
```

### 12.2 Flexibility

**Swap Nostr for SQL**:
```java
// Create new adapter implementing same ports
public class SqlVoucherLedgerRepository implements VoucherLedgerPort {
    // PostgreSQL implementation
}

public class SqlVoucherBackupRepository implements VoucherBackupPort {
    // PostgreSQL implementation
}

// No changes to domain or app layers!
```

### 12.3 Reusability

**Mint uses**: domain + app + nostr (full stack)
**Wallet uses**: domain + app + nostr (full stack)
**Client uses**: app only (services via wallet)
**Merchant tool**: domain only (offline verification)

### 12.4 Maintainability

**Clear boundaries**:
- Domain changes → bump major version (breaking)
- App changes → bump minor version (new features)
- Nostr changes → bump patch version (bug fixes)

**Independent testing**:
- Domain tests run in <1s (no I/O)
- App tests run in <5s (mocked ports)
- Nostr tests run in <30s (mock relay)

[↑ Back to top](#table-of-contents)

---

## Appendix

### A.1 Comparison: cashu-lib-vouchers vs cashu-voucher (multi-module)

| Aspect | cashu-lib-vouchers (v1) | cashu-voucher (v2 multi-module) |
|--------|-------------------------|----------------------------------|
| **Structure** | 4th module in cashu-lib | Standalone project with 3 modules |
| **Version** | Tied to cashu-lib (0.5.0) | Independent (0.1.0) |
| **Dependency** | Monolithic (all or nothing) | Granular (domain, app, nostr) |
| **Testing** | Mixed concerns | Clear separation |
| **Reusability** | Limited | High (pick modules you need) |
| **Flexibility** | Fixed to Nostr | Pluggable adapters |
| **Maintainability** | Single version | Per-module versions |
| **Best Practice** | Functional grouping | Hexagonal architecture |

### A.2 Why Hexagonal Architecture Matters

**Problem without it**:
```java
// ❌ Tight coupling
public class VoucherService {
    private NostrClient nostrClient;  // Directly coupled to Nostr!

    public void issue(VoucherSecret secret) {
        nostrClient.publish(...);  // Can't test without Nostr
    }
}
```

**Solution with hexagonal**:
```java
// ✅ Loose coupling
public class VoucherService {
    private VoucherLedgerPort ledgerPort;  // Port (interface)

    public void issue(VoucherSecret secret) {
        ledgerPort.publish(...);  // Can mock in tests
    }
}

// Runtime: inject Nostr adapter
VoucherService service = new VoucherService(
    new NostrVoucherLedgerRepository(...)  // Adapter
);
```

[↑ Back to top](#table-of-contents)

---

## Document History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2025-11-04 | Initial plan with multi-module architecture | TBD |
| 1.1 | 2025-11-04 | Added comprehensive task tables similar to NUT-13 plan | TBD |
| 1.2 | 2025-11-04 | Added Phase Summary and Project Summary sections | TBD |

---

**Document Status**: ✅ Final v2 - Implementation In Progress
**Last Updated**: 2025-11-04
**Architecture**: Hexagonal (Ports & Adapters)
**Total Tasks**: 72 (3% complete - 2/72)
**Progress**: Phase 0 in progress (2/6 tasks complete)
**Latest Commit**: c42d2de (Task 0.2 - Parent POM created)
**Next Action**: Task 0.3 - Create module skeletons
