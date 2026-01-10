# Changelog

All notable changes to the Cashu Library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.11.0] - 2026-01-10

### Added

- **NUT-18 Payment Requests**: Complete implementation of receiver-initiated payment requests per [NUT-18 specification](https://github.com/cashubtc/nuts/blob/main/18.md).
  - `PaymentRequest`: CBOR-encoded payment request with NOSTR and HTTP POST transports
  - `Transport`: Transport method with type, target address, and optional tags
  - `TransportType`: Enum for transport types (NOSTR, POST)
  - `PaymentPayload`: Payment fulfillment structure with proofs and DLEQ
  - `PaymentPayloadProof`: Proof structure for offline verification
  - `Nut10Option`: NUT-10 locking conditions (P2PK, HTLC, VOUCHER)
- **NUT-18 Tests**: Comprehensive test coverage including official test vectors

### Fixed

- **VoucherSecret**: Improved error handling for invalid UUID in Builder
- **WellKnownSecret**: Fixed null nonce serialization and deserialization
- **VoucherWellKnownSecret**: Backward compatibility and nonce handling

---

## [0.10.0] - 2025-01-07

### Added

- **VoucherSecret**: NUT-10 compliant tag-based voucher secret storage with builder pattern for creating Model B gift card voucher tokens.
- **VoucherTags**: Standard tag keys interface for VOUCHER secrets (issuer, unit, face_value, expires_at, memo, face_decimals, backing_strategy, issuance_ratio, issuer_sig, issuer_pubkey, merchant_metadata).
- **VoucherSecretTest**: Comprehensive unit tests for VoucherSecret serialization and tag-based storage.

### Changed

- **SecretUtil**: Updated to use `VoucherSecret` instead of deprecated `VoucherWellKnownSecret`.
- **WellKnownSecretDeserializer**: Updated to support tag-based voucher secret deserialization.

### Deprecated

- **VoucherWellKnownSecret**: Deprecated in favor of `VoucherSecret`. Marked for removal in a future version.

---

## [0.9.1] - 2025-12-22

### Added

- Unit tests covering `SecretUtil.toY` for NUT-00 hex secrets, NUT-10 JSON secrets, and string/secret parity to guard hash_to_curve consistency.

### Changed

- Bumped parent and module versions to `0.9.1`.

### Fixed

- `SecretUtil.toY` now matches `BDHKEUtils.hashToCurve(String)` decoding rules (hex decode for NUT-00, UTF-8 for NUT-10), keeping Y values consistent with mint verification and `/checkstate`.

---

## [0.9.0] - 2025-12-21

### Added

- **NUT-12 DLEQ primitives**: Introduced `DLEQProof` model and `DLEQUtils` generation/verification helpers for offline signature verification.
- **DLEQ token payloads**: Blind signatures and proofs now carry optional DLEQ data (`e`, `s`, `r`) with JSON/CBOR serialization and validation.
- **Test coverage**: Added unit tests for DLEQ proof serialization and cryptographic verification paths.

### Changed

- Bumped parent and module versions to `0.9.0`.

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
