# Audit Report

**Source:** [Java Performance Tuning](https://techoral.com/java/java-performance-tuning.html)
**Date:** 2026-02-02
**Codebase:** cashu-lib v0.16.0

## Executive Summary

- **Total Guidelines Evaluated:** 12
- **Applicable to Codebase:** 5
- **Findings:** 2 (0 critical, 0 high, 1 medium, 1 low)
- **Compliance Score:** 60% (3 compliant / 5 applicable)

## Codebase Capabilities Detected

| Capability | Status | Key Files |
|------------|--------|-----------|
| Collections | Present | 30+ files using `ArrayList`, `HashMap`, `List.of()` |
| StringBuilder | Present | `SignatureJsonSerializer.java`, `SignatureJsonDeserializer.java` |
| Static Mappers | Present | `JsonUtils.java` - ObjectMapper singletons |
| Try-with-resources | Present | `PrivateKey.java`, `WellKnownSecret.java`, `KeySetDerivation.java` |
| SecureRandom | Present | `DLEQUtils.java`, `Schnorr.java`, `KeysUtils.java` |
| Bulk Operations | Present | `addAll()` in `TokenV4.java`, `TokenV3.java`, `MintInformation.java` |
| Thread Pool | Test Only | `VirtualThreadConcurrencyTest.java` |
| Connection Pool | Not Present | - |
| LRU Cache | Not Present | - |
| Object Pool | Not Present | - |
| WeakHashMap | Not Present | - |

## Findings

### Medium Severity

#### [GUIDE-006] Collection Initialization Without Capacity

**Status:** PARTIAL
**Guideline:** Initialize collections with anticipated capacity to prevent internal resizing operations.
**Source:** [Collection Initialization with Capacity](https://techoral.com/java/java-performance-tuning.html)

**Locations:**
- `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/Keys.java:17`
- `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/util/SecretUtil.java:285`
- `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/nut10/WellKnownSecret.java:37`

**Current Code (Keys.java:17):**
```java
private final Map<BigInteger, PublicKey> values = new HashMap<>();
```

**Current Code (SecretUtil.java:285):**
```java
Map<String, Object> map = new HashMap<>();
```

**Current Code (WellKnownSecret.java:37):**
```java
this.tags = new ArrayList<>();
```

**Analysis:**
These collections are initialized without capacity hints. However:
- `Keys` typically holds ~20-30 entries (keyset denominations) - minor impact
- `SecretUtil` creates temporary maps for secret parsing - short-lived
- `WellKnownSecret.tags` typically has 0-3 tags - minimal impact

**Recommended Fix (where beneficial):**
```java
// Keys.java - mint keysets typically have 20-30 denominations
private final Map<BigInteger, PublicKey> values = new HashMap<>(32);

// WellKnownSecret - tags are usually few
this.tags = new ArrayList<>(4);
```

**Impact Assessment:** Low - collection sizes in this codebase are small and bounded.

---

#### [GUIDE-003] Good: SecretFactory Uses Capacity for Batch Operations

**Status:** COMPLIANT

**Evidence (SecretFactory.java:105):**
```java
List<DeterministicSecret> secrets = new ArrayList<>(count);
```

The batch creation method correctly pre-allocates the list with the expected count.

---

### Low Severity

#### [GUIDE-001] No WeakHashMap for Caching

**Status:** NEEDS REVIEW
**Guideline:** Use WeakHashMap for caching scenarios to prevent memory leaks.
**Source:** [Memory Leak Prevention with WeakHashMap](https://techoral.com/java/java-performance-tuning.html)

**Analysis:**
The codebase has static `ObjectMapper` instances in `JsonUtils.java`:
```java
public static final ObjectMapper JSON_MAPPER;
public static final ObjectMapper CBOR_MAPPER;
```

These are **intentionally static singletons** - they are not caches that accumulate entries. ObjectMapper instances are thread-safe and designed for reuse throughout application lifetime.

**Status Revision:** COMPLIANT (Not Applicable)

This is the recommended pattern for Jackson ObjectMapper - no WeakHashMap needed.

---

## Compliant Areas

### Try-With-Resources (GUIDE-002)

**Status:** COMPLIANT

The codebase properly uses try-with-resources for `AutoCloseable` resources.

**Evidence:**
- `cashu-lib-common/.../WellKnownSecret.java:43-45`:
```java
try (PrivateKey randomKey = PrivateKey.generateRandom()) {
    this.nonce = randomKey.toString();
}
```

- `cashu-lib-crypto/.../KeySetDerivation.java:19`:
```java
try (ByteArrayOutputStream pubkeysConcat = new ByteArrayOutputStream()) {
```

- `PrivateKey` implements `AutoCloseable` with proper `close()` method for key zeroing

### StringBuilder Usage (GUIDE-005)

**Status:** COMPLIANT

String concatenation in the codebase does not have problematic loop-based concatenation patterns.

**Evidence:**
- `SignatureJsonSerializer.java` and `SignatureJsonDeserializer.java` use `StringBuilder`
- No `+=` string concatenation in loops detected
- The `+=` operators found (`sum_fees +=`, `totalLength +=`, `currentIndex +=`) operate on primitives, not Strings

### Bulk Collection Operations (GUIDE-012)

**Status:** COMPLIANT

The codebase uses bulk operations where appropriate.

**Evidence:**
- `cashu-lib-common/.../TokenV4.java`: uses `addAll()`
- `cashu-lib-common/.../TokenV3.java`: uses `addAll()`
- `cashu-lib-common/.../MintInformation.java`: uses `addAll()`
- `cashu-lib-common/.../WellKnownSecret.java:67,88`: `tag.getValues().addAll(values)`

## Implementation Plan

### Phase 1: Critical Fixes (Immediate)

*No critical issues identified.*

### Phase 2: High Priority

*No high priority issues identified.*

### Phase 3: Medium Priority

| # | Task | Files | Effort |
|---|------|-------|--------|
| 1 | Add initial capacity to `Keys` HashMap (32) | `Keys.java` | Low |
| 2 | Add initial capacity to `WellKnownSecret.Tag` ArrayList (4) | `WellKnownSecret.java` | Low |

### Phase 4: Low Priority / Nice-to-Have

| # | Task | Files | Effort |
|---|------|-------|--------|
| 1 | Consider adding capacity hints to temporary HashMaps in `SecretUtil` | `SecretUtil.java` | Low |

## Guidelines Not Applicable

| Guideline | Reason |
|-----------|--------|
| GUIDE-003: LRU Cache | No caching layer exists - this is a protocol library |
| GUIDE-004: Object Pool | No expensive object creation patterns requiring pooling |
| GUIDE-007: Connection Pool | No database connectivity |
| GUIDE-008: Batch Database Operations | No database connectivity |
| GUIDE-009: Thread Pool Configuration | No application-level thread pools (only test code) |
| GUIDE-010: Read-Write Lock | Only one `synchronized` usage in test code; no read-heavy scenarios |
| GUIDE-011: GC Configuration | JVM settings - outside code scope |

## Summary

The cashu-lib codebase demonstrates **good performance practices** for a library:

1. **Excellent resource management** - Proper try-with-resources for `AutoCloseable` types
2. **Correct string handling** - StringBuilder used where appropriate, no string concatenation in loops
3. **Bulk operations** - Uses `addAll()` for collection population
4. **Smart pre-allocation** - `SecretFactory.createDeterministicBatch()` correctly pre-sizes lists

The minor findings (collection capacity hints) have negligible impact because:
- Collection sizes are small and bounded by protocol design
- Collections are short-lived (method-local)
- The overhead of HashMap/ArrayList resizing for <30 elements is minimal

**Recommendation:** The current implementation is appropriate for the codebase size and usage patterns. Adding capacity hints is optional and would provide marginal improvement.

---
*Generated by `/audit` skill on 2026-02-02*
*Source: [Java Performance Tuning](https://techoral.com/java/java-performance-tuning.html)*
