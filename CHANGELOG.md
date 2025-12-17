# Changelog

All notable changes to the Cashu Library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
