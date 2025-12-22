# cashu-lib-common

Core types for Cashu tokens and key management shared across modules.

- **Package**: `xyz.tcheeric.cashu.common`
- **Primary classes**:
  - `Token`, `TokenV3`, `TokenV4`
  - `Keys`, `KeysetId`, `KeySet`
  - `Secret`, `Proof`, `Mint`
  - `SecretUtil`, `HashToCurveSecret`
- **Javadoc**: [cashu-lib-common API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-common)

## Hash-to-curve helpers

- `SecretUtil.toY(secret)` derives `Y = hash_to_curve(secret_string)` for proofs and `/checkstate`.
- Decoding rules:
  - NUT-00 random secrets (64-char hex) are **hex-decoded** before hashing.
  - NUT-10 well-known secrets (`["KIND","hexdata","nonce",[]]`) are hashed as their UTF-8 JSON string.
- `SecretUtil.toYFromString` performs the same derivation when only the serialized secret string is available.
- `HashToCurveSecret` wraps the resulting compressed public key for transport.
