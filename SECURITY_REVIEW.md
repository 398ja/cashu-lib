# Security Review: cashu-lib

## Overview

This security review evaluates the cashu-lib codebase against the [Oracle Java Secure Coding Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html). The review identifies areas for improvement and provides actionable recommendations organized by guideline category.

---

## Executive Summary

The cashu-lib codebase demonstrates solid cryptographic engineering practices with proper use of established libraries (BouncyCastle), thread-safe implementations, and appropriate input validation in core crypto modules. However, several areas could benefit from improvements aligned with Oracle's secure coding guidelines:

| Priority | Category | Issues Found |
|----------|----------|--------------|
| **High** | Confidential Information | Private key exposure in memory, no zeroing |
| **High** | Input Validation | REST DTOs lack validation annotations |
| **Medium** | Mutability | Mutable arrays returned without defensive copying |
| **Medium** | Object Construction | No protection against unsafe subclassing |
| **Low** | Error Handling | Generic exceptions used, information leakage |
| **Low** | Denial of Service | No explicit resource limits on collections |

---

## Detailed Findings

### 1. FUNDAMENTALS (Guidelines 0-0 through 0-8)

#### 0-1: Design for Security from the Start

**Current State:** Most classes are not declared `final`, allowing potentially unsafe subclassing.

**Affected Classes:**
- `BaseKey.java` - Can be subclassed to bypass validation
- `PrivateKey.java` - Sensitive class, should be final
- `PublicKey.java` - Should prevent malicious subclassing
- `Signature.java` - Should prevent override of signature handling

**Recommendation:**
```java
// Before
public class PrivateKey extends BaseKey { ... }

// After
public final class PrivateKey extends BaseKey { ... }
```

#### 0-6: Encapsulation

**Current State:** `BaseKey.getBytes()` returns the internal array directly.

**Location:** `BaseKey.java:90`
```java
@EqualsAndHashCode.Include
private byte[] bytes;
// ...
public byte[] getBytes() { return bytes; } // Lombok-generated
```

**Impact:** Callers can modify the internal state of supposedly immutable key objects.

**Recommendation:**
```java
public byte[] getBytes() {
    return bytes.clone();
}
```

---

### 2. DENIAL OF SERVICE (Guidelines 1-1 through 1-5)

#### 1-1: Resource Exhaustion

**Current State:** REST DTOs accept unbounded lists without size limits.

**Location:** `PostSwapRequest.java:30-31`
```java
@JsonProperty("outputs")
private List<BlindedMessage> blindedMessages;
```

**Impact:** Malicious requests with extremely large lists could exhaust memory.

**Recommendation:** Add validation constraints:
```java
@JsonProperty("outputs")
@Size(max = 1000, message = "Maximum 1000 outputs allowed")
@NotEmpty
private List<BlindedMessage> blindedMessages;
```

#### 1-2: Resource Release

**Current State:** No explicit resource cleanup for sensitive data in memory.

**Location:** `PrivateKey.java`, `BaseKey.java`

**Recommendation:** Implement `AutoCloseable` for sensitive key classes:
```java
public final class PrivateKey extends BaseKey implements AutoCloseable {
    @Override
    public void close() {
        Arrays.fill(getBytes(), (byte) 0);
    }
}
```

#### 1-5: HashMap with User-Controlled Keys

**Current State:** `SecretUtil.java:280-281` creates HashMap from user-controlled Map:
```java
private static <T extends Secret> T mapToSecret(Map<?, ?> src) {
    Map<String, Object> map = new HashMap<>();
    src.forEach((k, v) -> map.put(String.valueOf(k), v));
```

**Impact:** Hash collision attacks possible if processing untrusted input.

**Recommendation:** Use `LinkedHashMap` or limit map size before processing.

---

### 3. CONFIDENTIAL INFORMATION (Guidelines 2-1 through 2-3)

#### 2-1: Exception Messages

**Current State:** Some exceptions reveal implementation details.

**Location:** `DLEQUtils.java:57-58`
```java
if (privateKey.signum() <= 0 || privateKey.compareTo(n) >= 0) {
    throw new IllegalArgumentException("Private key must be in range [1, n-1]. Got: " + privateKey);
}
```

**Impact:** The private key value is included in the exception message!

**Recommendation:**
```java
throw new IllegalArgumentException("Private key must be in range [1, n-1]");
```

#### 2-3: Zeroing Sensitive Data

**Current State:** Private keys are never cleared from memory after use.

**Affected Classes:**
- `PrivateKey.java` - stores key bytes indefinitely
- `Schnorr.java:59` - `secKey0` BigInteger persists
- `DLEQUtils.java:62` - nonce not cleared

**Recommendation:** Implement key zeroing (acknowledging this is difficult with Java's immutable types):
```java
// For byte arrays
try {
    // use the key
} finally {
    Arrays.fill(keyBytes, (byte) 0);
}
```

---

### 4. INJECTION AND INCLUSION (Guidelines 3-1 through 3-9)

#### 3-9: Floating-Point Validation

**Current State:** `SecretUtil.java:191-201` handles floating-point values without NaN/Infinity checks:
```java
if (value instanceof Double || value instanceof Float) {
    double d = n.doubleValue();
    if (d != Math.floor(d)) {
        tag.addValue(d);
```

**Recommendation:**
```java
if (value instanceof Double || value instanceof Float) {
    double d = n.doubleValue();
    if (Double.isNaN(d) || Double.isInfinite(d)) {
        throw new IllegalArgumentException("Invalid floating-point value");
    }
    // ...
}
```

---

### 5. ACCESSIBILITY AND EXTENSIBILITY (Guidelines 4-1 through 4-6)

#### 4-1: Limit Accessibility

**Current State:** Several internal utility methods are public.

**Examples:**
- `DLEQUtils.dleqHash()` - Should be package-private
- `DLEQUtils.pointToUncompressedHex()` - Should be package-private
- `Utils.bytesToHexString()` - Consider package-private

**Recommendation:** Reduce visibility of non-API methods:
```java
// Before
public static byte[] dleqHash(...) { ... }

// After
static byte[] dleqHash(...) { ... }
```

#### 4-5: Design for Inheritance or Declare Final

**Current State:** Key classes are not final.

**Recommendation:** Declare all cryptographic classes as `final`:
```java
public final class PrivateKey extends BaseKey { ... }
public final class PublicKey extends BaseKey { ... }
public final class Signature extends BaseKey { ... }
public final class DLEQUtils { ... }  // Already has private constructor
public final class Schnorr { ... }    // Missing private constructor
```

Add private constructor to utility classes:
```java
public final class Schnorr {
    private Schnorr() {}  // Prevent instantiation
    // ...
}
```

---

### 6. INPUT VALIDATION (Guidelines 5-1 through 5-4)

#### 5-1: Validate Inputs

**Current State:** REST DTOs lack JSR-380 Bean Validation annotations.

**Location:** All classes in `cashu-lib-entities`

**Recommendation:** Add validation annotations:
```java
@Data
public class PostSwapRequest<T extends Secret> extends PostInputRequest<T> {

    @JsonProperty("outputs")
    @NotNull(message = "Outputs are required")
    @NotEmpty(message = "At least one output required")
    @Size(max = 1000, message = "Maximum 1000 outputs")
    @Valid
    private List<BlindedMessage> blindedMessages;
}
```

**Also validate:**
- `BlindedMessage.amount` - positive integers only
- `Proof.keySetId` - length and format validation
- `PublicKey` inputs - length validation

#### 5-3: Native Method Wrappers

**Current State:** Not applicable - no JNI code found.

#### 5-4: API Input Validation

**Current State:** `Utils.hexStringToBytes()` properly validates input:
```java
if (len % 2 != 0) {
    throw new IllegalArgumentException("Hex string must have an even length");
}
// ...
if (digit1 == -1 || digit2 == -1) {
    throw new IllegalArgumentException("Invalid hexadecimal character found");
}
```

**Status:** Good - This is properly implemented.

---

### 7. MUTABILITY (Guidelines 6-1 through 6-12)

#### 6-1: Prefer Immutability

**Current State:** Key classes are mutable through setters.

**Location:** `BaseKey.java:28-29`
```java
@Setter(AccessLevel.PROTECTED)
```

**Recommendation:** Remove setters, make fields final:
```java
@Getter
public abstract class BaseKey {
    private final byte[] bytes;

    protected BaseKey(byte[] bytes) {
        this.bytes = bytes.clone();  // Defensive copy
    }
}
```

#### 6-2: Create Copies for Output

**Current State:** `BaseKey.getBytes()` returns internal array reference.

**Recommendation:** Already covered in 0-6 above.

#### 6-3: Create Copies of Mutable Inputs

**Current State:** `BaseKey` stores input arrays directly:
```java
protected BaseKey(byte[] bytes) {
    this.bytes = bytes;  // No defensive copy!
}
```

**Impact:** Caller can modify the key after construction.

**Recommendation:**
```java
protected BaseKey(byte[] bytes) {
    this.bytes = bytes.clone();
}
```

#### 6-9: Public Static Final Fields

**Current State:** Some static fields are mutable.

**Location:** `Utils.java:11`
```java
private static final char[] HEX_ARRAY = "0123456789ABCDEF".toLowerCase().toCharArray();
```

**Status:** Good - This is properly encapsulated as private.

#### 6-10: Static Fields Should Be Unmodifiable

**Location:** `SecretUtil.java:26`
```java
private static final ObjectMapper MAPPER = JsonUtils.JSON_MAPPER;
```

**Note:** ObjectMapper is mutable. Ensure `JsonUtils.JSON_MAPPER` is never modified after initialization.

**Recommendation:** Verify JsonUtils creates a properly configured, unmodifiable mapper.

---

### 8. OBJECT CONSTRUCTION (Guidelines 7-1 through 7-5)

#### 7-1: Avoid Public Constructors for Sensitive Classes

**Current State:** `PrivateKey` uses protected constructor with factory methods.
```java
protected PrivateKey(@NonNull String value) { ... }

public static PrivateKey fromString(@NonNull String s) {
    return new PrivateKey(s);
}
```

**Status:** Good pattern, but should also make class final.

#### 7-4: Don't Call Overridable Methods in Constructors

**Current State:** Need to verify no overridable methods called in constructors.

**Analysis:** `BaseKey` constructor does not call overridable methods. `PrivateKey` constructor calls `super(value)` which is safe.

**Status:** Good - No violations found.

---

### 9. SERIALIZATION AND DESERIALIZATION (Guidelines 8-1 through 8-6)

#### 8-1: Avoid Serializing Sensitive Classes

**Current State:** `PrivateKey` extends `BaseKey` which uses Jackson serialization via `@JsonValue`.

**Location:** `BaseKey.java:128-131`
```java
@JsonValue
@Override
public String toString() {
    return Hex.toHexString(bytes);
}
```

**Concern:** Private keys could be accidentally serialized to JSON.

**Recommendation:** Consider removing `@JsonValue` from `BaseKey` and adding it only to classes where serialization is intended:
```java
// BaseKey.java - Remove @JsonValue
@Override
public String toString() {
    return Hex.toHexString(bytes);
}

// PublicKey.java - Add explicitly
@JsonValue
@Override
public String toString() {
    return super.toString();
}
```

Or prevent serialization of PrivateKey:
```java
@JsonIgnoreType  // Prevent Jackson serialization
public final class PrivateKey extends BaseKey { ... }
```

#### 8-6: Deserialization Filters

**Current State:** Using Jackson (not Java serialization), which is safer.

**Recommendation:** Ensure Jackson is configured to reject unknown properties:
```java
objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
```

---

### 10. ACCESS CONTROL (Guidelines 9-1 through 9-19)

#### 9-2: Run with Minimal Privileges

**Current State:** Not directly applicable to a library, but consumers should be advised.

**Recommendation:** Document in README that applications using cashu-lib should run with minimal file system and network privileges.

---

## Additional Findings

### Error Handling Improvements

**Current State:** `Schnorr.java` throws generic `Exception`:
```java
public static byte[] sign(byte[] msg, byte[] secKey) throws Exception
```

**Recommendation:** Create specific exception types:
```java
public class CashuCryptoException extends RuntimeException { ... }
public class InvalidKeyException extends CashuCryptoException { ... }
public class SignatureVerificationException extends CashuCryptoException { ... }
```

### Null Return Values

**Current State:** `Utils.xor()` returns `null` on length mismatch:
```java
public static byte[] xor(byte[] b0, byte[] b1) {
    if (b0.length != b1.length) {
        return null;
    }
```

**Impact:** Caller must handle null, risk of NullPointerException.

**Recommendation:**
```java
public static byte[] xor(byte[] b0, byte[] b1) {
    if (b0.length != b1.length) {
        throw new IllegalArgumentException(
            "Arrays must have equal length for XOR operation");
    }
```

### Silent Error Handling

**Current State:** `SecretUtil.listToSecret()` silently returns empty byte array on hex decode failure:
```java
try {
    data = org.bouncycastle.util.encoders.Hex.decode(hexData);
} catch (Exception e) {
    log.warn("...");
    data = new byte[0];  // Silent failure
}
```

**Recommendation:** Fail fast or make the behavior explicit:
```java
try {
    data = org.bouncycastle.util.encoders.Hex.decode(hexData);
} catch (Exception e) {
    throw new IllegalArgumentException("Invalid hex data in secret", e);
}
```

---

## Prioritized Action Items

### High Priority

1. ✅ **Add `final` keyword to sensitive classes** (`PrivateKey`, `PublicKey`, `Signature`)
   - Completed: PrivateKey is `final`, PublicKey is `sealed`, Signature is `final`
   - CompressedPublicKey and UnCompressedPublicKey are `final` (deprecated)
2. ✅ **Remove private key value from exception messages** in `DLEQUtils.java`
   - Completed: Exception message no longer includes the key value
3. ✅ **Implement defensive copying** in `BaseKey` constructor and getter
   - Completed: `getBytes()` and `setBytes()` now use `Arrays.copyOf()`
   - Constructor also makes defensive copy
4. ✅ **Add validation annotations** to REST DTOs (JSR-380)
   - Completed: Added Jakarta Validation API dependency and annotations
   - `@NotNull`, `@NotEmpty`, `@Size(max=1000)`, `@Valid` on all collection fields
   - `@NotBlank` on required string fields (quoteId)

### Medium Priority

5. ✅ **Prevent accidental serialization** of `PrivateKey`
   - Completed: Added `@JsonIgnoreType` annotation
6. ✅ **Change null returns to exceptions** in `Utils.xor()`
   - Completed: Now throws `IllegalArgumentException` on length mismatch
7. ✅ **Reduce visibility** of internal utility methods
   - Completed: `dleqHash()` and `pointToUncompressedHex()` are now package-private
8. ✅ **Validate floating-point values** for NaN/Infinity
   - Completed: Added validation in `SecretUtil.addTagsToSecret()`

### Low Priority

9. ✅ **Implement key zeroing** with AutoCloseable
   - Completed: `PrivateKey` implements `AutoCloseable` with `close()` method
   - Added `zeroBytes()` method in `BaseKey`
   - Documented JVM memory limitations in Javadoc
10. ✅ **Add resource limits** to collection-accepting methods
    - Completed: All REST DTOs now have `MAX_INPUTS/MAX_OUTPUTS = 1000` limits
    - Applied via `@Size(max=1000)` annotations
11. ✅ **Create specific exception types** for crypto operations
    - Completed: Created `xyz.tcheeric.cashu.crypto.exception` package
    - `CashuCryptoException` (base), `InvalidKeyException`, `SignatureException`
    - Factory methods for common error scenarios
12. ✅ **Document security considerations** in Javadoc
    - Completed: Added security sections to `BaseKey`, `PrivateKey`, `Schnorr`,
      `BDHKEUtils`, and `DLEQUtils`

---

## Compliance Summary

| Guideline Category | Compliance | Notes |
|-------------------|------------|-------|
| 0 - Fundamentals | Good | Classes are final/sealed, encapsulation implemented |
| 1 - Denial of Service | Good | Resource limits added to all collection fields |
| 2 - Confidential Information | Good | Key exposure fixed, zeroing implemented, serialization blocked |
| 3 - Injection | Good | Proper input validation, NaN/Infinity validation added |
| 4 - Accessibility | Good | Internal methods now package-private |
| 5 - Input Validation | Good | JSR-380 annotations on REST DTOs, crypto validation complete |
| 6 - Mutability | Good | Defensive copies implemented |
| 7 - Object Construction | Good | Factory methods used |
| 8 - Serialization | Good | PrivateKey blocked from serialization |
| 9 - Access Control | N/A | Library context |

---

## References

- [Oracle Secure Coding Guidelines for Java SE](https://www.oracle.com/java/technologies/javase/seccodeguide.html)
- [Cashu NUT Specifications](https://github.com/cashubtc/nuts)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)

---

## Implementation Log

| Date | Items Completed | Commit |
|------|-----------------|--------|
| 2026-02-01 | Initial review | 4d5809e |
| 2026-02-02 | Items 1-3: final classes, exception message fix, defensive copying | (pending) |
| 2026-02-02 | Items 5-8: @JsonIgnoreType, xor() exception, method visibility, NaN validation | (pending) |
| 2026-02-02 | Item 4: JSR-380 validation annotations on REST DTOs | (pending) |
| 2026-02-02 | Items 9-12: Key zeroing, resource limits, custom exceptions, security Javadoc | (pending) |

---

*Review conducted: 2026-02-01*
*Last updated: 2026-02-02*
*Codebase version: 0.15.0*
*Status: All items complete*
