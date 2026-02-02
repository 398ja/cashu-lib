# Claude Code Instructions

This file contains project-specific instructions for Claude Code when working with the cashu-lib codebase.

## Project Structure

This is a multi-module Maven project implementing the Cashu protocol:

```
cashu-lib/
├── cashu-lib-crypto    # Cryptographic primitives (BDHKE, DLEQ, Schnorr)
├── cashu-lib-common    # Common models, secrets, and payment structures
└── cashu-lib-entities  # REST API request/response entities
```

## Package Organization by NUT Specification

Classes in this codebase are organized by their corresponding Cashu NUT (Notation, Usage, and Terminology) specification. When adding new classes, place them in the appropriate NUT-specific package.

### cashu-lib-common Packages

| Package | NUT | Description | Example Classes |
|---------|-----|-------------|-----------------|
| `common/nut10/` | NUT-10 | Spending Conditions | `WellKnownSecret`, `Nut10Option`, serializers |
| `common/nut11/` | NUT-11 | Pay-to-Pubkey (P2PK) | `P2PKSecret` |
| `common/nut12/` | NUT-12 | DLEQ Proofs | `DLEQProof`, `DLEQProofDeserializer` |
| `common/nut13/` | NUT-13 | Deterministic Secrets | `DeterministicSecret`, `DeterministicSecretDeserializer` |
| `common/nut17/` | NUT-17 | WebSocket Subscriptions | `SubscriptionKind`, `JsonRpcRequest`, etc. |
| `common/nut18/` | NUT-18 | Payment Requests | `PaymentRequest`, `Transport`, `VoucherSecret`, etc. |
| `common/` | Core | Foundational classes | `Token`, `Proof`, `KeySet`, `PublicKey`, etc. |

### cashu-lib-entities Packages

| Package | NUT | Description | Example Classes |
|---------|-----|-------------|-----------------|
| `rest/nut03/` | NUT-03 | Swap Operations | `PostSwapRequest`, `PostSwapResponse` |
| `rest/nut04/` | NUT-04 | Mint Quotes | `PostMintQuoteRequest`, `PostMintQuoteResponse` |
| `rest/nut05/` | NUT-05 | Melt Quotes | `PostMeltQuoteRequest`, `PostMeltQuoteResponse` |
| `rest/nut07/` | NUT-07 | Token State Check | `PostCheckStateRequest`, `PostCheckStateResponse` |
| `rest/nut09/` | NUT-09 | Restore Signatures | `PostRestoreRequest`, `PostRestoreResponse` |
| `rest/` | General | Keyset responses | `ActiveKeySetResponse`, `KeySetResponse` |

## Adding New Classes

When adding a new class related to a specific NUT specification:

1. **Identify the NUT**: Determine which NUT specification the class implements
2. **Choose the correct package**: Place the class in the corresponding `nutXX/` package
3. **Create package if needed**: If implementing a new NUT, create a new `nutXX/` package
4. **Update imports**: Ensure existing classes properly import from the new location

### Example: Adding a NUT-14 class

```java
// File: cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/nut14/MyNut14Class.java
package xyz.tcheeric.cashu.common.nut14;

public class MyNut14Class {
    // Implementation
}
```

### Core Classes (No NUT Package)

Some classes are foundational and don't belong to a specific NUT:
- `Token`, `TokenV3`, `TokenV4` - Token structures (used across multiple NUTs)
- `Proof`, `BlindSignature` - Core cryptographic structures
- `KeySet`, `Keys`, `KeysetId` - Keyset management
- `PublicKey`, `PrivateKey`, `Signature` - Key and signature types
- `Secret`, `RandomStringSecret` - Base secret types

These remain in the root `common/` package.

## Versioning

This project uses Semantic Versioning (SemVer). When making changes:
- **major**: Breaking API changes, incompatible modifications
- **minor**: New features, backward-compatible additions, package reorganization
- **patch**: Bug fixes, documentation updates

Use the `/bumpup` command to automatically bump versions, update changelogs, and deploy.

## Testing

- All tests are in `src/test/java/` mirroring the main source structure
- Run tests with `mvn test`
- Ensure tests pass before committing changes

## Security Guidelines

This project follows the [Oracle Java Secure Coding Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html). See `SECURITY_REVIEW.md` for the full security audit.

### Key Handling

- **PrivateKey** is `final` and uses `@JsonIgnoreType` to prevent accidental serialization
- Never include key values in exception messages or logs

#### When to Use Key Zeroing

Use try-with-resources for **short-lived keys** that should be cleared after use:

```java
// Signing operations (NUT-11 P2PK)
try (PrivateKey signingKey = PrivateKey.fromString(keyHex)) {
    Signature sig = Signature.sign(message, signingKey);
} // Key is zeroed here

// One-time key generation
try (PrivateKey ephemeralKey = PrivateKey.generateRandom()) {
    PublicKey pubKey = PrivateKey.derivePublicKey(ephemeralKey);
} // Ephemeral key is zeroed

// Processing key material from external sources
try (PrivateKey importedKey = PrivateKey.fromBytes(decryptedKeyBytes)) {
    // Perform operations with imported key
} // Clear imported key when done
```

#### When NOT to Use Key Zeroing

- **Long-lived mint keys**: Keyset private keys that persist for the application lifetime
- **Keys stored in configuration**: Keys loaded at startup and reused throughout

#### Zeroing Limitations

Due to JVM behavior, zeroing has limitations:
- GC may retain copies in freed memory
- JIT may optimize away zeroing operations
- String representations may be interned
- Copies made during crypto operations persist

For highest security, consider hardware security modules (HSMs).

### Defensive Copying

- `BaseKey.getBytes()` returns a defensive copy - callers cannot modify internal state
- When accepting byte arrays, always copy: `this.bytes = Arrays.copyOf(bytes, bytes.length)`
- When returning byte arrays, always copy: `return Arrays.copyOf(bytes, bytes.length)`

### Input Validation

- REST DTOs use Jakarta Validation annotations (`@NotNull`, `@NotEmpty`, `@Size`, `@Valid`)
- Collection fields are limited to 1000 elements to prevent DoS attacks
- Validate floating-point values for `NaN` and `Infinity` before processing

### Exception Handling

- Use exceptions from `xyz.tcheeric.cashu.crypto.exception`:
  - `CashuCryptoException` - base class for all crypto errors
  - `InvalidKeyException` - invalid or malformed keys
  - `SignatureException` - signature operation failures
- Never return `null` to indicate errors - throw descriptive exceptions
- Exception messages must not contain sensitive data (keys, secrets)

### Class Design

- Cryptographic classes should be `final` or `sealed` to prevent unsafe subclassing
- Utility classes should have private constructors and be `final`
- Internal helper methods should be package-private, not public

### Serialization

- `PrivateKey` uses `@JsonIgnoreType` to block JSON serialization
- `PublicKey` and `Signature` serialize to compressed hex format (66 chars)
- Verify DLEQ proofs (NUT-12) when receiving blind signatures

## References

- [Cashu NUT Specifications](https://github.com/cashubtc/nuts)
- [Oracle Java Secure Coding Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html)
- [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
- [Semantic Versioning](https://semver.org/)
