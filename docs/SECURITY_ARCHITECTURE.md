# Imani Wallet Security Architecture

**Version:** 1.0
**Date:** January 26, 2026
**Audience:** Engineering, Security Operations, Auditors

---

## 1. Executive Summary

This document details the security architecture of the Imani Wallet, a non-custodial Cashu client integrated with the Nostr protocol. The system is designed around a **Defense-in-Depth** philosophy, utilizing multiple redundant layers of control—Client UI, API Gateway, and Database—to prevent value inflation (double-spending), ensure data integrity, and protect user secrets in a hostile browser environment.

## 2. Threat Model & Core Defenses

The architecture specifically addresses the following critical threat vectors:

| Threat | Description | Primary Defense | Secondary Defense |
| :--- | :--- | :--- | :--- |
| **Token Inflation** | Replaying a token to redeem it multiple times. | **Global Redemption Mutex** (Client) | **Idempotency Keys** (API) & **Unique Constraints** (DB) |
| **Mint Collision** | Different tokens from the same mint treated as duplicates. | **Cryptographic Fingerprinting** (SHA-256 of proof secrets) | - |
| **Race Conditions** | Concurrent Auto-Redeem vs. Manual-Redeem. | **Global Mutex** (Lock acquisition) | **SERIALIZABLE** DB Isolation |
| **Key Exfiltration** | XSS attacks stealing private keys. | **Encrypted IndexedDB** (AES-GCM) | **Strict CSP** & Input Sanitization |
| **Replay Attacks** | Re-sending intercepted API requests. | **NIP-98 Auth** (Timestamp + Payload Hash) | **Idempotency Caching** |

---

## 3. Financial Integrity (Anti-Inflation)

To prevent double-spending and inflation, the system enforces a strict "Three-Layer" validation model.

### 3.1 Layer 1: Client-Side Concurrency Control
The frontend implements a **Global Redemption Mutex** singleton (`RedemptionLocks`).
*   **Mechanism:** Before any network call, the client computes a **Strong Fingerprint** of the token.
*   **Locking:** It attempts to acquire a lock for that fingerprint. If the lock is held (e.g., by a background Auto-Redeem process), the manual action (e.g., "Paste Token") is rejected immediately.
*   **Fingerprinting Algorithm:**
    ```javascript
    // Prevents collisions between tokens from the same mint
    Fingerprint = SHA-256( Sort(Proof_Secrets) + "||" + Mint_URL )
    ```

### 3.2 Layer 2: API Idempotency
Every state-changing request (Redeem, Split, Send) requires a deterministic **Idempotency Key**.
*   **Header:** `X-Idempotency-Key`
*   **Derivation:** `SHA-256(Token_Fingerprint + User_Pubkey + Operation)`
*   **Behavior:** The backend caches responses for 24 hours. If a request is retried (due to network timeout), the backend returns the *cached result* without re-processing the transaction.

### 3.3 Layer 3: Database Integrity
The backend serves as the final authority using strict ACID compliance.
*   **Isolation Level:** `TRANSACTION_SERIALIZABLE`. This prevents "Read-Modify-Write" race conditions where two concurrent transactions both read a proof as "Unspent" before writing.
*   **Unique Constraints:** The database enforces a `UNIQUE(mint_url, commitment)` constraint on the `proofs` table.
*   **Atomic Swaps:** The "Mark Spent" and "Credit Balance" operations occur within a single atomic transaction.

---

## 4. Key Management & Secure Storage

### 4.1 Storage Hierarchy
We explicitly avoid `localStorage` for high-value secrets due to its vulnerability to XSS.

| Data Class | Storage Mechanism | Encryption |
| :--- | :--- | :--- |
| **Nostr Private Keys (nsec)** | `IndexedDB` | **AES-GCM** (Wrapping Key) |
| **Cashu Tokens** | `IndexedDB` | **AES-GCM** (Recommended) |
| **Session/Preferences** | `localStorage` | Plaintext / Base64 |
| **Encryption Keys** | Web Crypto API (Memory) | **Non-Exportable** |

### 4.2 Cryptographic Implementation
*   **Key Derivation:** Master keys are derived from the user's PIN/Password using **PBKDF2** (SHA-256, 600,000+ iterations) and a random salt.
*   **Encryption:** `AES-GCM` (256-bit) with a unique 12-byte IV for every write operation. The IV is stored alongside the ciphertext.
*   **Hardware Backing (Roadmap):** Integration of WebAuthn PRF extension to allow hardware-backed key derivation on supported devices (TouchID/FaceID).

---

## 5. Network & Transport Security

### 5.1 NIP-98 HTTP Authentication
All API requests to the wallet backend are authenticated using the **NIP-98** standard.
*   **Event Kind:** `27235` (HTTP Auth).
*   **Validation:**
    1.  **Signature:** Verifies the Schnorr signature of the event.
    2.  **Timestamp:** Must be within ±60 seconds of server time (Anti-Replay).
    3.  **URL Binding:** The `u` tag must match the exact request URL.
    4.  **Payload Integrity:** For POST/PUT requests, the `payload` tag must contain the `SHA-256` hash of the request body.

### 5.2 Content Security Policy (CSP)
Strict CSP headers are enforced to mitigate XSS and Data Exfiltration.
```nginx
default-src 'self';
script-src 'self' 'wasm-unsafe-eval'; # WASM required for Argon2/Cashu crypto
connect-src 'self' https://<trusted-mints> wss://<trusted-relays>;
img-src 'self' data: https:;
object-src 'none';
```

---

## 6. Input Validation & Sanitization

### 6.1 Token Parsing
*   **Strict Mode:** The parser enforces `cashuA` (v3) or `cashuB` (v4) prefixes.
*   **Schema Validation:** All external inputs (DMs, QRs) are validated against a strict schema (e.g., Zod) before processing. Malformed JSON fails closed.
*   **Amount Verification:** The server **ignores** client-supplied `face_value`. Amounts are derived exclusively by summing the values of the valid proofs provided.

### 6.2 Sanitization
*   **User Content:** Decrypted DMs (NIP-17) and Memos are passed through `DOMPurify` before rendering to strip `<script>`, `<iframe>`, and `javascript:` URIs.

---

## 7. Protocol Compliance

### 7.1 Cashu (NUTs)
*   **NUT-13 (Keysets):** The wallet validates keyset IDs to prevent the "31-bit collision" vulnerability. Secret derivation uses the full keyset hash.
*   **NUT-07 (Check State):** Before deleting any token from local storage, the wallet performs a `check_state` call to the mint to confirm the proofs are actually spent.

### 7.2 Nostr (NIPs)
*   **NIP-17 (Private DMs):** Used for secure token transport.
*   **NIP-60 (Wallet Events):** Wallet state is backed up to relays using encrypted events.
*   **NIP-44 (Encryption):** Used for all payload encryption on the wire (Nostr).

---

## 8. Security Auditing

*   **Audit Logging:** Critical failures (e.g., "Duplicate Fingerprint Detected", "NIP-98 Hash Mismatch") are logged with high severity, redacting sensitive token data.
*   **Traceability:** Every transaction is tagged with a correlation ID that persists across Client, API Gateway, and Database logs.
