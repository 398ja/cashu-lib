# Changelog

All notable changes to the Cashu Library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.8.0] - 2025-12-19

### Changed

- **PublicKey**: Consolidated into single unified class that auto-detects input format (33, 64, or 65 bytes)
  - Always stores and serializes as compressed format per NUT-00
  - Added `getUncompressedBytes()` for NUT-12 DLEQ proof hashing
  - Added `getSec1Uncompressed()` for SEC1 format output
- **Signature**: Refactored to be standalone, no longer uses PublicKey internally
  - Stores both compressed (33 bytes) and raw (64 bytes) formats
  - Preserves raw format for Schnorr signature verification
- **BaseKey**: Improved constant naming with clearer semantics
  - Added `PRIVATE_KEY_HEX_LENGTH`, `X_COORDINATE_HEX_LENGTH`, `COMPRESSED_KEY_HEX_LENGTH`

### Deprecated

- `CompressedPublicKey` - use `PublicKey` directly
- `UnCompressedPublicKey` - use `PublicKey` directly
- `PublicKey.fromBytes(bytes, boolean)` - use `PublicKey.fromBytes(bytes)` (auto-detects format)
- `PublicKey.fromString(str, boolean)` - use `PublicKey.fromString(str)` (auto-detects format)
- `PublicKey.fromPoint(ecPoint, boolean)` - use `PublicKey.fromPoint(ecPoint)`
- Old `BaseKey` constants (`PUBLIC_KEY_LENGTH_COMPRESSED`, `PUBLIC_KEY_LENGTH_UNCOMPRESSED`)

### Fixed

- **Signature.fromBytes**: Now correctly handles both 33-byte compressed and 64-byte Schnorr signatures

---

## [0.7.2] - 2025-12-17

### Fixed

- **NUT-10 BDHKE Verification**: Fixed `BDHKEUtils.hashToCurve()` and `verify()` to correctly handle NUT-10 well-known secrets (P2PK, HTLC, VOUCHER)
  - NUT-10 secrets are JSON arrays that should be UTF-8 encoded for `hash_to_curve`, not hex decoded
  - Added detection for NUT-10 format (strings starting with `[`)
  - Legacy hex secrets continue to use hex decoding for backward compatibility
  - Fixes "Invalid hexadecimal character found" errors when verifying voucher proofs

---

## [0.7.1] - 2025-12-16

### Fixed

- **SecretDeserializer**: Fixed double hex-decode issue in `WellKnownSecretDeserializer`
- **SecretUtil**: Direct construction of WellKnownSecret objects instead of using Jackson's `convertValue()`

---

## [0.7.0] - 2025-12-15

### Added

- Initial NUT-10 well-known secret support
- `VoucherWellKnownSecret` for Model B voucher tokens
- `P2PKSecret` for pay-to-public-key conditions
- `WellKnownSecret` base class with NUT-10 JSON serialization

---

## [0.6.0] and earlier

See git history for earlier changes.
