# Definite-length V4 CBOR token encoding (cashu-ts interop)

- **Date:** 2026-07-21
- **Status:** Design — approved for planning
- **Repo:** `cashu-lib` (`cashu-lib-common`), primary. Flows to `cashu-mint` + gateways via BOM bump.
- **Scope:** the V4 token **encoder** only. DLEQ emission is explicitly out of scope (separate follow-up).

## Problem

The gateway/mint issues V4 Cashu tokens that **no cashu-ts–based wallet can decode**. cashu-ts (the standard JS Cashu library, used by the offline wallet and any external browser wallet) throws `Unsupported length: 31` on the first byte.

`31` is the CBOR additional-info value for **indefinite-length**. The token bytes begin with `0xbf` (major type 5 / map, info 31 = *indefinite-length map*), and every nested map is `bf … ff`. cashu-ts's CBOR decoder only supports **definite-length** maps/arrays, so it rejects the token before any mint-URL / keyset / DLEQ / secret logic runs. The hot wallet works only because it round-trips through the Java backend's own (Jackson) decoder.

### Root cause

`TokenV4.serialize()` (`cashu-lib-common/.../TokenV4.java:109`) encodes via Jackson data-binding:
```java
byte[] cborToken = JsonUtils.CBOR_MAPPER.writeValueAsBytes(this);
```
Jackson's `CBORGenerator` emits **indefinite-length** maps/arrays during data-binding (it calls `writeStartObject()` with no size). Jackson 2.20's `CBORGenerator.Feature` set is only `WRITE_MINIMAL_INTS` / `STRINGREF` / `WRITE_MINIMAL_DOUBLES` — there is **no definite-length option**. So this cannot be fixed by configuration; it needs a purpose-built encoder.

The decode path is fine: `TokenV4.deserialize()` uses `readValue`, and Jackson reads both definite and indefinite CBOR — so Java-side round-trips and cashu-mint/gateway decoding are unaffected by an encoder change.

## Goal

Newly-issued V4 tokens are encoded as **definite-length** CBOR matching the NUT-00 reference layout, so cashu-ts and every standard Cashu wallet can decode them. No change to the token's logical content, field names, or base64url framing — only the CBOR length encoding.

## Non-goals

- DLEQ (NUT-12) emission — the next blocker, but a separate spec (needs mint-side investigation + a wallet-relax option).
- Migrating already-issued indefinite-length tokens (staging is disposable).
- Changing the decode path (Jackson `readValue` stays; it already reads definite-length).

## Design — Option 2: hand-rolled definite-length encoder

Replace only the encode call with a small, isolated CBOR writer.

### The exact V4 structure (from `TokenV4.java`)

```
TokenV4              map { "m": tstr, "u": tstr, "d"?: tstr, "t": [ TokenData… ] }
  TokenData          map { "i": bstr(keysetId), "p": [ TokenProof… ] }
    TokenProof       map { "a": uint(amount), "s": tstr(secret), "c": bstr(C),
                           "d"?: DLEQ, "w"?: tstr(witness) }
      DLEQ           map { "e": bstr, "s": bstr, "r": bstr }
```
- **Byte strings** (`bstr`, major type 2): `i`, `c`, and DLEQ `e`/`s`/`r`.
- **Text strings** (`tstr`, major type 3): `m`, `u`, `d`, `s`, `w`.
- **Unsigned int** (major type 0, minimal encoding): `a`.
- **Optional keys** (`d` memo, proof `d` dleq, `w` witness) are **omitted entirely when null** — and the map's definite length counts only the keys actually present. (This mirrors cashu-ts: absent, not null.)

### Component

A new `TokenV4CborEncoder` (in `cashu-lib-common`, alongside `TokenV4`) with one entry point: `byte[] encode(TokenV4 token)`. It writes CBOR primitives directly to a `ByteArrayOutputStream`:
- `writeMapHeader(n)` → major type 5, definite length `n` (minimal-length header).
- `writeArrayHeader(n)` → major type 4, definite length `n`.
- `writeTextString`, `writeByteString`, `writeUint` → majors 3 / 2 / 0 with minimal-length headers.
- Compose the tree above, counting present keys for each map header.

`TokenV4.serialize()` calls `new TokenV4CborEncoder().encode(this)` instead of `CBOR_MAPPER.writeValueAsBytes(this)`. `TokenUtil.serialize(cborBytes, Version.V4, clickable)` (base64url + `cashuB` prefix) is unchanged. `deserialize()` is unchanged.

### Key ordering

The encoder emits keys in the NUT-00 conventional order (`t`, `m`, `u`, `d` at the top; `i`, `p`; `a`, `s`, `c`, `d`, `w`). CBOR maps are unordered for **decoding**, so any consistent order is interoperable; the fixed order also makes the output deterministic for byte-level test vectors.

## Testing (the interop anchor)

1. **cashu-ts decodes the Java output (binding test):** in `cashu-lib` tests, build a `TokenV4` with a representative proof set (incl. a proof with no DLEQ and a large VOUCHER-style secret string, and a multi-proof/multi-keyset case), `serialize()`, and assert the bytes start with a definite-length map header (**not** `0xbf`). Then a cross-check fixture: the same token, encoded, must be decodable by cashu-ts. Since cashu-ts is JS, capture a **golden vector** — a Java-encoded token string committed as a fixture and a companion cashu-ts test (or a documented manual check) proving `getDecodedToken()` succeeds and yields the same mint/unit/amounts/secrets. At minimum, a pure-Java assertion decodes the bytes with a **strict definite-length-only** CBOR reader (or a hand check) to prove no `0xbf`/`0x9f`/`0xff` markers remain.
2. **Java round-trip:** `deserialize(serialize(token))` equals the original (all fields, incl. optional-absent).
3. **Regression:** the specific real-token shape that failed (VOUCHER secrets, 4 proofs, no DLEQ, public mint URL) now produces a `0xa_`/`0x84`-style definite output.
4. **Minimal-int + optional-omission:** amounts use minimal uint headers; null memo/dleq/witness produce no key and a correctly-reduced map length.

## Rollout

`cashu-lib` (encoder + tests) → version bump → `imani-bom` bump → `cashu-mint` + `imani-wallet-lib`/gateways consume the new cashu-lib → rebuild + redeploy the issuer (staging `customer-wallet`, and cashu-mint if it also encodes tokens). Every newly-issued V4 token is then cashu-ts-decodable. Existing tokens remain indefinite (disposable on staging).

## Open questions for the plan

1. Whether any other caller relies on the current (indefinite) byte output or on `CBOR_MAPPER` for token encoding specifically (grep for `TokenV4` serialize callers + any other `writeValueAsBytes` on token types).
2. Does `cashu-mint` itself encode V4 tokens (e.g. in swap/mint responses) via the same `TokenV4.serialize`, or only the gateway? (Determines which services need the rebuild.)
3. Confirm the base64url framing in `TokenUtil.serialize` is unpadded url-safe (cashu-ts expects that) — likely already correct since decode works, but verify.
4. The cleanest way to run a cashu-ts decode check from a Java repo's CI (committed golden vector + a small node script, vs a documented one-time manual verification).
