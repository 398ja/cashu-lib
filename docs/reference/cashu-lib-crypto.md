# cashu-lib-crypto

Cryptographic primitives and utilities for the Cashu protocol.

- **Package**: `xyz.tcheeric.cashu.crypto`
- **Primary classes**:
  - `Schnorr`
  - `BDHKEUtils`
  - Utilities in `xyz.tcheeric.cashu.crypto.util` such as `KeySetDerivation` and `KeysUtils`
- **Javadoc**: [cashu-lib-crypto API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-crypto)

## BDHKE hash-to-curve

- `BDHKEUtils.hashToCurve(String secret)` follows Cashu’s rules for deriving Y:
  - Secrets starting with `[` (NUT-10 JSON arrays) are UTF-8 encoded as-is.
  - All other secrets are treated as hex strings and decoded to bytes before hashing.
- Use the string overload when working with serialized secrets; use the byte overload when you already hold the raw bytes.
