## Summary
Related issue: #____

Introduce hash-to-curve secret handling and enhance public key compression support across the common library. Replace legacy secret handling in entity models (PostCheckState) to use the new abstraction. Remove the deprecated `Confidential` interface, update tests accordingly, and bump module versions. Minor CI/workflow housekeeping.

## What changed?
- Common crypto/model updates:
  - Added `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/HashToCurveSecret.java` to encapsulate secret transformations used for hash-to-curve operations.
  - Enhanced key types:
    - `CompressedPublicKey` now supports construction from a byte array.
      - cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/CompressedPublicKey.java
    - `PublicKey` and `UnCompressedPublicKey` updated to improve handling and validation of compressed/uncompressed formats.
      - cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/PublicKey.java
      - cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/UnCompressedPublicKey.java
    - `Signature` updated alongside key changes.
      - cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/Signature.java
  - `PrivateKey` refactor and cleanup; removed legacy interface usage.
    - cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/PrivateKey.java
  - Removed deprecated `Confidential` interface.
    - cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/Confidential.java
  - Minor util adjustments in `SecretUtil`.
    - cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/util/SecretUtil.java
- Entities (REST models):
  - Updated `PostCheckStateRequest` and `PostCheckStateResponse` to use `HashToCurveSecret`.
    - cashu-lib-entities/src/main/java/xyz/tcheeric/cashu/entities/rest/PostCheckStateRequest.java
    - cashu-lib-entities/src/main/java/xyz/tcheeric/cashu/entities/rest/PostCheckStateResponse.java
- Tests:
  - Adjusted tests for new key handling and secret abstraction.
    - cashu-lib-common/src/test/java/xyz/tcheeric/cashu/common/BaseKeyJsonCreatorTest.java
    - cashu-lib-common/src/test/java/xyz/tcheeric/cashu/common/PrivateKeyTest.java
    - cashu-lib-common/src/test/java/xyz/tcheeric/cashu/common/PublicKeyTest.java
    - cashu-lib-entities/src/test/java/xyz/tcheeric/cashu/entities/rest/PostCheckStateRequestTest.java
- Build/versions:
  - Version bumps across modules and parent:
    - cashu-lib-common/pom.xml
    - cashu-lib-crypto/pom.xml
    - cashu-lib-entities/pom.xml
    - pom.xml
- CI/workflows:
  - Removed `.github/workflows/codex.yml`
  - Tweaked `.github/workflows/google-java-format.yml` (removed develop from config)

## BREAKING
- Removal of `xyz.tcheeric.cashu.common.Confidential` interface.
  - Migration: Remove references to `Confidential` in dependent code. Use the updated key/secret abstractions (`HashToCurveSecret`, updated `PrivateKey`/`PublicKey`) as applicable.

## Review focus
- Crypto correctness and alignment of `HashToCurveSecret` with existing hash-to-curve usage.
- Backward compatibility and API shape for public key compression/uncompressed handling.
- Serialization/JSON behavior of `PostCheckState*` models after switching to `HashToCurveSecret`.
- Any downstream impact from removing `Confidential`.

## Build and Tests
- Command: `mvn -q verify`
- Result: Succeeded locally (Java build). Observed SLF4J provider warnings; no functional failures.

Output:
```
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
```

Note: If CI has network access, it can run with the same command. No network dependency issues observed locally.

## Checklist
- [ ] Scope ≤ 300 lines (or split/stack)
- [ ] Title is verb + object (e.g., “Add hash-to-curve secret abstraction”)
- [ ] Description links the issue and answers “why now?”
- [ ] BREAKING flagged (removal of `Confidential`)
- [ ] Tests/docs updated (tests updated; docs N/A)
