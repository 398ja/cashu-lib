# Audit Report

**Source:** [Optimizing Java Performance: Memory Management Guide](https://codezup.com/optimizing-java-performance-memory-management-guide/)
**Date:** 2026-02-02
**Codebase:** cashu-lib v0.16.0

## Executive Summary

- **Total Guidelines Evaluated:** 6
- **Applicable to Codebase:** 5
- **Findings:** 3 (0 critical, 0 high, 2 medium, 1 low)
- **Compliance Score:** 40% (2 compliant / 5 applicable)

## Codebase Capabilities Detected

| Capability | Status | Key Files |
|------------|--------|-----------|
| Collections | Present | 58 files - widespread use of `List`, `Map`, `HashMap`, `ArrayList` |
| Cryptography | Present | `cashu-lib-crypto/` - ECDSA, SHA-256, SecureRandom |
| File I/O | Present | `KeySetDerivation.java` - ByteArrayOutputStream |
| Serialization | Not Present | No ObjectInputStream/ObjectOutputStream usage |
| WeakReferences | Not Present | No WeakReference/SoftReference usage |
| Wrapper Types | Present | 61 files - Integer, Long, BigInteger used extensively |
| Resource Management | Present | `PrivateKey.java` implements AutoCloseable |
| Sensitive Data Handling | Present | `BaseKey.java`, `PrivateKey.java` with zeroing support |

## Findings

### Medium Severity

#### [GUIDE-001] Unbounded Collection Growth Risk

**Status:** NEEDS REVIEW
**Guideline:** Avoid indefinitely accumulating objects in collections without removal logic to prevent memory exhaustion.
**Source:** [Prevent Unbounded Collection Growth](https://codezup.com/optimizing-java-performance-memory-management-guide/)

**Analysis:**
The codebase contains static collection fields in test files that could grow unboundedly during test execution, though production code appears safe.

**Locations:**
- `cashu-lib-common/src/test/java/.../NUT00Tests.java` - static List fields
- `cashu-lib-common/src/test/java/.../NUT01Tests.java` - static Map fields
- `cashu-lib-common/src/test/java/.../NUT02Tests.java` - static collections

**Production Code Review:**
- `Keys.java:17` - Uses `HashMap` but size is bounded by mint keyset denominations (typically 20-30 entries)
- `SplittingService.java:47` - Creates `ArrayList` but size is bounded by amount splitting algorithm

**Risk Assessment:** Low risk in production. Test code could benefit from cleanup in `@AfterEach` methods to prevent memory accumulation during large test suites.

**Recommended Fix:**
For test classes with static collections, add cleanup:
```java
@AfterEach
void cleanup() {
    staticList.clear();
}
```

---

#### [GUIDE-002] Missing WeakReference for Optional Caching

**Status:** NON-COMPLIANT
**Guideline:** Use WeakReference for optional caching to allow GC to reclaim memory when objects are no longer strongly reachable.
**Source:** [Use WeakReferences for Optional Caching](https://codezup.com/optimizing-java-performance-memory-management-guide/)

**Locations:**
- `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/util/JsonUtils.java:14-17`

**Current Code:**
```java
public final class JsonUtils {
    /** Mapper configured for JSON serialization/deserialization. */
    public static final ObjectMapper JSON_MAPPER;

    /** Mapper configured for CBOR serialization/deserialization. */
    public static final ObjectMapper CBOR_MAPPER;
    // ... initialized in static block
}
```

**Analysis:**
The `ObjectMapper` instances are intentionally static singletons, which is the correct pattern for Jackson ObjectMapper (they are thread-safe and expensive to create). This is **intentional design**, not a memory leak.

**Status Revision:** COMPLIANT (False Positive)

This pattern is actually the recommended approach for Jackson ObjectMapper. No fix needed.

---

### Low Severity

#### [GUIDE-003] Wrapper Type Usage Where Primitives Could Suffice

**Status:** PARTIAL
**Guideline:** Prefer primitives over wrapper objects to minimize memory overhead and GC pressure.
**Source:** [Prefer Primitives Over Objects](https://codezup.com/optimizing-java-performance-memory-management-guide/)

**Locations:**
- `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/Keys.java:17`
- `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/util/SplittingService.java:30`

**Current Code (Keys.java:17):**
```java
private final Map<BigInteger, PublicKey> values = new HashMap<>();
```

**Current Code (SplittingService.java:30):**
```java
public List<Integer> split(long amount, Collection<Integer> availableDenominations) {
```

**Analysis:**
- `BigInteger` is required for keys because denomination amounts can exceed `Long.MAX_VALUE` in theory and must match the Cashu protocol specification.
- `Integer` for denominations is acceptable since denominations are typically small (1, 2, 4, 8, 16, ... up to 2^20).
- The boxing overhead is negligible given the small collection sizes (typically <30 entries for keysets).

**Status Revision:** COMPLIANT (Justified Design)

The use of wrapper types is justified by:
1. Protocol compatibility requirements (BigInteger for amounts)
2. Collection API requirements (generics require objects)
3. Small collection sizes making overhead negligible

---

## Compliant Areas

### Try-With-Resources for Resource Management (GUIDE-004)

**Status:** COMPLIANT

The codebase correctly uses try-with-resources for stream operations.

**Evidence:**
- `cashu-lib-crypto/src/main/java/.../KeySetDerivation.java:19-29`:
```java
try (ByteArrayOutputStream pubkeysConcat = new ByteArrayOutputStream()) {
    for (byte[] publicKey : sortedKeys.values()) {
        pubkeysConcat.write(publicKey);
    }
    byte[] hash = Utils.sha256(pubkeysConcat.toByteArray());
    // ...
} catch (IOException | NoSuchAlgorithmException e) {
    throw new RuntimeException(e);
}
```

- `cashu-lib-common/src/main/java/.../PrivateKey.java:43` implements `AutoCloseable` with proper `close()` method for key zeroing.

### Sensitive Data Nullification (GUIDE-006)

**Status:** COMPLIANT

The codebase implements proper sensitive data handling that goes beyond simple nullification.

**Evidence:**
- `cashu-lib-common/src/main/java/.../BaseKey.java:177-181`:
```java
protected void zeroBytes() {
    if (bytes != null) {
        Arrays.fill(bytes, (byte) 0);
    }
}
```

- `cashu-lib-common/src/main/java/.../PrivateKey.java:102-108`:
```java
@Override
public void close() {
    if (!closed) {
        zeroBytes();
        closed = true;
    }
}
```

This implementation:
- Uses `Arrays.fill()` to actively zero bytes rather than just setting to null
- Implements `AutoCloseable` for try-with-resources support
- Tracks closed state to prevent use-after-close
- Documents JVM limitations in class Javadoc

## Implementation Plan

### Phase 1: Critical Fixes (Immediate)

*No critical issues identified.*

### Phase 2: High Priority

*No high priority issues identified.*

### Phase 3: Medium Priority

| # | Task | Files | Effort |
|---|------|-------|--------|
| 1 | Add `@AfterEach` cleanup to test classes with static collections | `NUT00Tests.java`, `NUT01Tests.java`, `NUT02Tests.java` | Low |

### Phase 4: Low Priority / Nice-to-Have

| # | Task | Files | Effort |
|---|------|-------|--------|
| 1 | Consider using primitive collections library (e.g., Eclipse Collections) for performance-critical paths if profiling shows collection overhead | `Keys.java`, `SplittingService.java` | High |

## Guidelines Not Applicable

| Guideline | Reason |
|-----------|--------|
| GUIDE-005: Process Large Data in Chunks | This library doesn't process large files or datasets. All data operations are on small cryptographic values (32-64 bytes) and protocol messages. |
| WeakReference Caching | No caching layer exists in this library - it's a protocol implementation library, not an application with caching needs. |

## Summary

The cashu-lib codebase demonstrates **good memory management practices** overall:

1. **Excellent sensitive data handling** - The `PrivateKey` class properly implements `AutoCloseable` with byte zeroing, exceeding the guideline's recommendation of simple nullification.

2. **Correct resource management** - Try-with-resources is used appropriately for stream operations.

3. **Appropriate type choices** - While wrapper types are used, they are justified by protocol requirements and collection sizes are bounded.

4. **Minor improvements possible** - Test cleanup could be enhanced, but this has no production impact.

The codebase prioritizes security (proper key zeroing, defensive copying) over micro-optimizations, which is appropriate for a cryptographic library.

---
*Generated by `/audit` skill on 2026-02-02*
*Source: [Optimizing Java Performance: Memory Management Guide](https://codezup.com/optimizing-java-performance-memory-management-guide/)*
