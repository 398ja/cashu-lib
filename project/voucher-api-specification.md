# Cashu Voucher - API Specification

**Version**: 0.1.0
**Base URL**: `https://mint.example.com`
**Date**: 2025-11-04

---

## Overview

The Cashu Voucher API provides endpoints for issuing, querying, and managing gift card vouchers following Model B (vouchers are NOT redeemable at mint, only at issuing merchant).

**Key Features**:
- NUT-13 compatible (deterministic recovery for regular proofs)
- Nostr-backed ledger (NIP-33) and backup (NIP-17)
- ED25519 signatures (Nostr-compatible)
- Model B: Vouchers rejected in swap/melt operations

---

## Authentication

All mint APIs require authentication (mechanism depends on mint implementation).

```http
Authorization: Bearer <mint_token>
```

---

## Endpoints

### 1. Issue Voucher

**POST** `/v1/vouchers`

Issue a new voucher.

#### Request

```json
{
  "amount": 5000,
  "unit": "sat",
  "memo": "Holiday gift card",
  "expiresInDays": 365
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| amount | integer | Yes | Face value in smallest unit |
| unit | string | Yes | Unit (e.g., "sat", "usd") |
| memo | string | No | Description/memo (max 500 chars) |
| expiresInDays | integer | No | Expiry in days from now (null = no expiry) |

#### Response (200 OK)

```json
{
  "voucher": {
    "voucherId": "123e4567-e89b-12d3-a456-426614174000",
    "issuerId": "merchant123",
    "unit": "sat",
    "faceValue": 5000,
    "expiresAt": 1743897600,
    "memo": "Holiday gift card",
    "issuerPublicKey": "9c2e42a9de0c5a00d7e1c9bb3d6fb06de8f5e3e76e3e7d6a5c4b3a2918171615",
    "issuerSignature": "5f3a8b2c..."
  },
  "proof": {
    "amount": 5000,
    "secret": "...",
    "C": "...",
    "id": "..."
  },
  "token": "cashuAeyJ0b2tlbiI6W3sicHJvb2ZzIjpbeyJpZCI6IjAwOWExZjI5...",
  "nostrEventId": "7c3f9e2a1b4d6e8f0a1c3d5e7f9b2c4e6a8d0f2a4c6e8f0b1d3e5f7a9c"
}
```

| Field | Type | Description |
|-------|------|-------------|
| voucher | object | Voucher details |
| proof | object | Cashu proof (Proof<VoucherSecret>) |
| token | string | Serialized Cashu token (v4 format) |
| nostrEventId | string | NIP-33 event ID (ledger) |

#### Error Responses

```json
// 400 Bad Request
{
  "error": "INVALID_AMOUNT",
  "message": "Amount must be positive"
}

// 403 Forbidden
{
  "error": "UNAUTHORIZED",
  "message": "Merchant not authorized to issue vouchers"
}

// 500 Internal Server Error
{
  "error": "NOSTR_PUBLISH_FAILED",
  "message": "Failed to publish voucher to Nostr ledger"
}
```

---

### 2. Query Voucher Status

**GET** `/v1/vouchers/{voucherId}/status`

Query current status of a voucher from Nostr ledger.

#### Request

```http
GET /v1/vouchers/123e4567-e89b-12d3-a456-426614174000/status
```

#### Response (200 OK)

```json
{
  "voucherId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "ISSUED",
  "issuedAt": 1736294400,
  "redeemedAt": null,
  "expiresAt": 1743897600,
  "isExpired": false,
  "nostrEventId": "7c3f9e2a1b4d6e8f0a1c3d5e7f9b2c4e6a8d0f2a4c6e8f0b1d3e5f7a9c"
}
```

**Status Values**:
- `ISSUED` - Voucher published to Nostr, ready for redemption
- `REDEEMED` - Voucher redeemed by merchant
- `REVOKED` - Voucher revoked by issuer
- `EXPIRED` - Expiry time passed

#### Error Responses

```json
// 404 Not Found
{
  "error": "VOUCHER_NOT_FOUND",
  "message": "Voucher not found in ledger"
}
```

---

### 3. Swap (Rejects Vouchers - Model B)

**POST** `/v1/swap`

Standard Cashu swap endpoint. **Vouchers are rejected** (Model B).

#### Request

```json
{
  "proofs": [
    {
      "amount": 5000,
      "secret": "<VoucherSecret hex>",
      "C": "...",
      "id": "..."
    }
  ],
  "outputs": [...]
}
```

#### Response (400 Bad Request)

```json
{
  "error": "VOUCHER_NOT_ACCEPTED",
  "message": "Vouchers cannot be redeemed at mint (Model B). Please redeem with issuing merchant.",
  "voucherId": "123e4567-e89b-12d3-a456-426614174000",
  "issuerId": "merchant123"
}
```

---

### 4. Melt (Rejects Vouchers - Model B)

**POST** `/v1/melt`

Standard Cashu melt endpoint. **Vouchers are rejected** (Model B).

#### Response (400 Bad Request)

```json
{
  "error": "VOUCHER_NOT_ACCEPTED",
  "message": "Vouchers cannot be redeemed at mint (Model B). Please redeem with issuing merchant."
}
```

---

## CLI Commands

### Issue Voucher

```bash
cashu voucher issue --amount 5000 --memo "Gift" --expires-in-days 365

# Output:
# ✅ Voucher issued: 123e4567-e89b-12d3-a456-426614174000
#    Amount: 5000 sats
#    Expires: 2026-01-03
#    Backed up to Nostr ✓
```

### List Vouchers

```bash
cashu voucher list

# Output:
# ID                                   | Amount | Status  | Memo
# -------------------------------------|--------|---------|------------
# 123e4567-e89b-12d3-a456-426614174000 | 5000   | ACTIVE  | Gift
# 234f5678-f90c-23e4-b567-537625285111 | 10000  | SPENT   | Birthday
```

### Show Voucher

```bash
cashu voucher show 123e4567-e89b-12d3-a456-426614174000

# Output:
# Voucher Details:
#   ID: 123e4567-e89b-12d3-a456-426614174000
#   Issuer: merchant123
#   Amount: 5000 sats
#   Status: ACTIVE
#   Issued: 2025-01-03 10:00:00
#   Expires: 2026-01-03 10:00:00
#   Memo: Holiday gift
#
#   [QR CODE]
```

### Backup Vouchers

```bash
cashu voucher backup

# Output:
# 🔄 Backing up vouchers to Nostr...
# ✅ Backed up 3 vouchers
#    Event ID: 7c3f9e2a...
#    Relays: relay.yourorg.com, relay.damus.io
```

### Restore Vouchers

```bash
cashu voucher restore

# Output:
# 🔄 Restoring vouchers from Nostr...
# ✅ Restored 3 vouchers
#    • 123e4567: 5000 sats
#    • 234f5678: 10000 sats
#    • 345g6789: 2000 sats
```

### Check Status

```bash
cashu voucher status 123e4567-e89b-12d3-a456-426614174000

# Output:
# Voucher Status:
#   Status: ISSUED
#   On Nostr: ✓ (event: 7c3f9e2a...)
#   Last updated: 2025-01-03 10:00:00
```

---

## Merchant Commands

### Verify Voucher (Offline)

```bash
cashu merchant verify <token> --offline

# Output:
# ✅ Voucher VALID
#    Signature: ✓ Valid
#    Expiry: ✓ Not expired
#    Issuer: merchant123
#    Amount: 5000 sats
```

### Verify Voucher (Online)

```bash
cashu merchant verify <token>

# Output:
# 🔄 Verifying voucher...
# ✅ Voucher VALID
#    Signature: ✓ Valid
#    Expiry: ✓ Not expired
#    Status: ✓ ISSUED (not redeemed)
#    Nostr ledger: ✓ Confirmed
```

### Redeem Voucher

```bash
cashu merchant redeem <token>

# Output:
# 🔄 Redeeming voucher...
# ✅ Voucher redeemed
#    Amount: 5000 sats
#    Voucher ID: 123e4567-e89b-12d3-a456-426614174000
#    Updated Nostr ledger: ✓
```

---

## Nostr Event Formats

### NIP-33 Ledger Event (kind 30078)

```json
{
  "kind": 30078,
  "pubkey": "<mint_issuer_pubkey>",
  "created_at": 1736294400,
  "tags": [
    ["d", "voucher:123e4567-e89b-12d3-a456-426614174000"],
    ["status", "issued"],
    ["issuer", "merchant123"],
    ["unit", "sat"],
    ["value", "5000"],
    ["expires_at", "1743897600"],
    ["hash", "a3f2b5c8..."]
  ],
  "content": "{\"version\":1,\"memo\":\"Holiday gift\"}",
  "sig": "..."
}
```

### NIP-17 Backup Event (kind 14)

```json
{
  "kind": 14,
  "pubkey": "<user_pubkey>",
  "created_at": 1736294400,
  "tags": [
    ["p", "<user_pubkey>"],
    ["subject", "cashu-voucher-backup"],
    ["client", "cashu-wallet"]
  ],
  "content": "<NIP-44 encrypted payload>",
  "sig": "..."
}
```

**Encrypted Payload** (after NIP-44 decryption):
```json
{
  "version": 1,
  "timestamp": 1736294400,
  "backupCounter": 42,
  "vouchers": [
    {
      "voucherId": "123e4567-e89b-12d3-a456-426614174000",
      "issuerId": "merchant123",
      "unit": "sat",
      "faceValue": 5000,
      "expiresAt": 1743897600,
      "memo": "Gift",
      "issuerSignature": "5f3a8b2c...",
      "issuerPublicKey": "9c2e42a9...",
      "proof": { /* Full Proof<VoucherSecret> */ },
      "blindingFactor": "a1b2c3...",
      "status": "ACTIVE",
      "receivedAt": 1736294000
    }
  ]
}
```

---

## SDK Usage (Java)

### Maven Dependency

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>cashu-voucher-app</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Issue Voucher

```java
import xyz.tcheeric.cashu.voucher.app.VoucherService;
import xyz.tcheeric.cashu.voucher.app.dto.*;

VoucherService voucherService = ...; // Injected

IssueVoucherRequest request = IssueVoucherRequest.builder()
    .issuerId("merchant123")
    .amount(5000L)
    .unit("sat")
    .expiresInDays(365)
    .memo("Holiday gift")
    .build();

IssueVoucherResponse response = voucherService.issue(request);
SignedVoucher voucher = response.getVoucher();
String token = response.getToken();
```

### Verify Voucher (Merchant)

```java
import xyz.tcheeric.cashu.voucher.app.MerchantVerificationService;

MerchantVerificationService merchantService = ...; // Injected

SignedVoucher voucher = parseToken(token);

// Offline verification
VerificationResult offlineResult = merchantService.verifyOffline(
    voucher,
    "merchant123"  // Expected issuer
);

if (offlineResult.isValid()) {
    // Online verification (check Nostr ledger)
    VerificationResult onlineResult = merchantService.verifyOnline(
        voucher,
        "merchant123"
    );

    if (onlineResult.isValid()) {
        // Accept voucher
        acceptPayment(voucher.getSecret().getFaceValue());
    }
}
```

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_AMOUNT` | 400 | Amount is zero or negative |
| `INVALID_UNIT` | 400 | Unsupported unit |
| `INVALID_EXPIRY` | 400 | Expiry date in the past |
| `UNAUTHORIZED` | 403 | Not authorized to issue vouchers |
| `VOUCHER_NOT_FOUND` | 404 | Voucher ID not found in ledger |
| `VOUCHER_NOT_ACCEPTED` | 400 | Voucher presented in swap/melt (Model B) |
| `VOUCHER_EXPIRED` | 400 | Voucher expiry passed |
| `VOUCHER_REDEEMED` | 400 | Voucher already redeemed |
| `VOUCHER_REVOKED` | 400 | Voucher revoked by issuer |
| `INVALID_SIGNATURE` | 400 | Issuer signature verification failed |
| `NOSTR_PUBLISH_FAILED` | 500 | Failed to publish to Nostr relay |

---

## Rate Limits

| Endpoint | Limit | Window |
|----------|-------|--------|
| `POST /v1/vouchers` | 100 requests | 1 minute |
| `GET /v1/vouchers/{id}/status` | 1000 requests | 1 minute |

---

## Versioning

API follows semantic versioning: `MAJOR.MINOR.PATCH`

**Current Version**: 0.1.0 (beta)

**Breaking Changes**: Will increment MAJOR version

---

**Document Version**: 1.0
**Last Updated**: 2025-11-04
**Related**: gift-card-plan-final-v2.md
