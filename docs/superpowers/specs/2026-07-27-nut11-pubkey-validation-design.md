# NUT-11 P2PK Secret Validation

**Date**: 2026-07-27
**Status**: Design — not yet implemented
**Modules**: `cashu-lib-common`

---

## Problem

`P2PKSecret` accepts arbitrary bytes and arbitrary strings as public keys. Nothing in the parse
path checks length, compression prefix, or curve-point membership, and none of NUT-11's four
"malformed secret" rules are enforced. A proof can be constructed, serialized, and round-tripped
with a lock that no key can ever satisfy.

This matters because the failure is silent and terminal: **a proof locked to an unparseable
public key is unspendable by anyone.** The value is destroyed, and nothing in the library
objects at any point.

The gap was found by comparing behaviour against `cashu-ts`, which rejects hard —
`normalizeSecpPubkey` throws on bad length, bad prefix, or a point that fails to decompress.
Same specification, opposite behaviour.

### Current state (verified 2026-07-27)

**No validation at any ingress point:**

| Location | Code | Problem |
|---|---|---|
| `WellKnownSecretDeserializer:78-79` | `Hex.decode(dataHex)` → `setData(data)` | Any even-length hex accepted as the lock key |
| `WellKnownSecretDeserializer:137` | `secret.setData(Hex.decode(...))` | Same, legacy object format |
| `WellKnownSecretDeserializer:108` | `tag.addValue(valueNode.asText())` | `pubkeys` / `refund` entries stored as raw strings |
| `WellKnownSecret:55` | `setData(@NonNull byte[] data)` | Lombok `@NonNull` only |
| `P2PKSecret:34,40` | `P2PKSecret(@NonNull byte[] data)` | Passed straight to `super`, unchecked |
| `P2PKSecret:62-84` | `setPubKeys` / `addPubKey` / `setRefund` / `addRefund` | Unvalidated `List<String>` |
| `P2PKSecret:151` | `fromString` | Bare `JSON_MAPPER.readValue`, `@SneakyThrows`, deprecated |

**None of NUT-11's four malformed-secret rules are enforced.** The spec requires the Proof be
rejected as unspendable when: a tag appears more than once; `n_sigs` / `n_sigs_refund` is not a
positive integer or exceeds its pathway's key count; the sigflag is unrecognised; or a key
appears twice within one pathway.

**Three getters throw on a well-formed-but-empty tag:**

```java
// P2PKSecret:91-92  — getNSigs()
List<?> values = super.getTag(P2PKTag.n_sigs.name()).getValues();
return values != null ? (int) values.get(0) : -1;     // empty list -> IndexOutOfBoundsException
```

`getSigFlag()` (:118-119) and `getLockTime()` (:136-137) repeat the pattern. `getNSigsRefund()`
(:104-111) guards exactly this case and documents why — the guard was never copied to its three
siblings. `getPubKeys()` (:128) and `getRefund()` (:146) additionally perform an unchecked
`(List<String>)` cast, so a numeric tag value surfaces as `ClassCastException` downstream.

### What already exists and can be reused

`PublicKey`'s private constructor (`PublicKey:73-87`) already enforces two of the three checks:

```java
if (compressedBytes.length != COMPRESSED_BYTES)  // 33
    throw new IllegalArgumentException("Invalid compressed public key length: ...");
byte prefix = compressedBytes[0];
if (prefix != 0x02 && prefix != 0x03)
    throw new IllegalArgumentException("Invalid public key prefix: ...");
```

Two caveats, both load-bearing for this design:

1. **`PublicKey.fromString` is too permissive for NUT-11.** It accepts 66 *or* 128 hex chars
   (`PublicKey:178-191`), and `fromBytes` accepts 33, 64, or 65 bytes. NUT-11 mandates compressed
   only. Calling `fromString` alone would let an uncompressed key through.
2. **Curve-point membership is never checked at construction.** The constructor stores bytes
   after the prefix test; decompression happens lazily on first use of the uncompressed form. A
   syntactically valid `02`-prefixed value that is not a point on secp256k1 is accepted now and
   fails later, on an unrelated code path — or never.

## Goals

1. Reject a P2PK secret whose public keys are not valid compressed secp256k1 points, at parse
   time, before it can be stored or acted upon.
2. Enforce NUT-11's four specified malformed-secret conditions.
3. Fix the three unguarded getters.
4. Fail closed, and fail with a typed exception the mint layer can map to a protocol error rather
   than a crash.

## Non-goals

- Changing `WellKnownSecret.data` from `byte[]` to a typed `PublicKey` field. Correct in
  principle, breaking in practice: `data` is also the HTLC hash and the VOUCHER payload, neither
  of which is a public key. Validation belongs in the P2PK layer, not the shared base.
- Validating HTLC or VOUCHER secrets. Out of scope.
- Any change to signature verification or the spend path.
- Removing the *used* deprecated API (`CompressedPublicKey`, `UnCompressedPublicKey`,
  `VoucherWellKnownSecret`, `P2PKSecret.fromString`, `BaseKey.toBytes`). Deferred — see
  [Follow-up: deprecated API cleanup](#follow-up-deprecated-api-cleanup-separate-release).
  Only the five *dead and hazardous* members in §6 are removed here.

## Design

### 1. `P2PKPublicKeys` — a NUT-11-specific validator

New utility in `xyz.tcheeric.cashu.common.nut11`. Single responsibility: decide whether a value
is a public key NUT-11 will accept, and normalise it for comparison.

```java
public final class P2PKPublicKeys {

    /** @throws MalformedP2PKSecretException if not a valid compressed secp256k1 point. */
    public static PublicKey requireValid(byte[] key);
    public static PublicKey requireValid(String hex);

    /** Lowercase x-coordinate, parity prefix dropped. NUT-11 comparison form. */
    public static String toComparisonForm(String hex);
}
```

`requireValid` MUST check, in order:

1. Length is exactly 33 bytes / 66 hex chars. **Uncompressed forms are rejected** — this is why
   `PublicKey.fromString` cannot be used directly.
2. Prefix is `0x02` or `0x03`.
3. The point decompresses to a valid point on secp256k1. This is the check that does not exist
   today; implement by forcing decompression eagerly (BouncyCastle `decodePoint`) and converting
   any failure into `MalformedP2PKSecretException`.

Hex input is case-insensitive: NUT-11 states that uppercase and mixed-case hex are *valid and
equivalent*, so case must be normalised, not rejected.

`toComparisonForm` drops the parity byte and lowercases. **This is not defensive padding** — it
is required by NUT-11, which specifies that keys "are compared using their lowercase
x-coordinate (`02` or `03` y-parity prefix ignored)". `02‖x` and `03‖x` are the same signing key
under BIP-340, so duplicate detection that compares full compressed hex will not detect the
duplicate the spec declares fatal.

### 2. `P2PKSecret.validate()` — the four NUT-11 rules

```java
/** @throws MalformedP2PKSecretException if this secret violates NUT-11. */
public void validate();
```

Checks:

| Rule | Condition |
|---|---|
| Key validity | `data`, every `pubkeys` entry, every `refund` entry passes `requireValid` |
| Duplicate tags | No `P2PKTag` appears more than once in `tags` |
| Duplicate keys | Within each pathway, no two keys share a `toComparisonForm` value |
| Signature counts | `n_sigs` and `n_sigs_refund` are positive integers not exceeding their pathway's key count |
| Sigflag | Parses to a known `SignatureFlag` |

Pathway definition, per NUT-11: the **main** pathway is `data` plus the `pubkeys` tag; the
**refund** pathway is the `refund` tag. A key may appear in both pathways; it may not appear
twice within one.

### 3. Wire the validator in at the boundary

**Deserialization is the enforcement point.** `WellKnownSecretDeserializer`, after building a
`P2PKSecret` in both `deserializeNut10Format` and `deserializeLegacyFormat`, calls `validate()`
before returning. A malformed secret never becomes a live object.

**Constructors and setters validate their own input** — `P2PKSecret(byte[] data, ...)`,
`setPubKeys`, `addPubKey`, `setRefund`, `addRefund` each call `requireValid` on what they are
given. This catches construction-side errors at the point of the mistake rather than at
serialization.

Note `P2PKSecret.fromString` is already `@Deprecated(forRemoval = true)`; route it through the
deserializer rather than adding a second validation path.

### 4. Fix the three getters

Copy the `getNSigsRefund` guard to `getNSigs`, `getSigFlag`, and `getLockTime`: null-check the
tag, null-check the values, **and** empty-check the values before `get(0)`. Preserve each
method's existing absent-tag default (`-1`, `null`, `0` respectively) — this is a crash fix, not
a semantics change.

For `getPubKeys` and `getRefund`, replace the unchecked `(List<String>)` cast with an element-wise
`String::valueOf` map, so a numeric tag value degrades to a validation failure rather than a
`ClassCastException` at an arbitrary call site.

### 5. Exception type

```java
public class MalformedP2PKSecretException extends IllegalArgumentException
```

Extends `IllegalArgumentException` so existing catch sites keep working. Carries the failing
rule and — for key failures — the position (`data`, `pubkeys[2]`, `refund[0]`).

**Never include key material in the message.** The repo already has this rule for private keys
(`CHANGELOG`: "Private key values are never included in exception messages"); apply the same
discipline here. Report the position and the reason, not the bytes.

### 6. Remove five dead deprecated members

These are in scope because they are the *same failure class* the rest of this spec addresses: a
wrong key-length constant, or a constructor that silently drops a compression prefix, is how a
validation bypass gets written in the first place. Leaving them in place while adding a validator
invites someone to reach for the wrong one.

All five are verified dead inside `cashu-lib` (2026-07-27): the four constants have zero
references outside their own declaration in `BaseKey.java`, and no subclass — `PublicKey`,
`PrivateKey`, `RandomStringSecret`, `DeterministicSecret` — calls the deprecated constructor.

| Member | Location | Why it is a hazard, not just clutter |
|---|---|---|
| `PUBLIC_KEY_LENGTH_UNCOMPRESSED` | `BaseKey:95` | Value is **66; should be 128**. Its own javadoc says so. Any length check written against it is wrong |
| `PUBLIC_KEY_LENGTH_COMPRESSED` | `BaseKey:83` | Named "compressed" but holds **64** (`X_COORDINATE_HEX_LENGTH`). A compressed key is 66. Misleading name over a wrong value |
| `PRIVATE_KEY_LENGTH` | `BaseKey:64` | Dead alias for `PRIVATE_KEY_HEX_LENGTH` |
| `SECRET_LENGTH` | `BaseKey:106` | Dead alias for `SECRET_HEX_LENGTH` |
| `BaseKey(String hexStr)` | `BaseKey:124-127` | `Hex.decode(hexStr.substring(2))` — strips the first byte by position, with no check that it *is* a parity prefix. Precisely the bug this spec exists to stop |

Replacements already exist and are in use: `PRIVATE_KEY_HEX_LENGTH`, `X_COORDINATE_HEX_LENGTH`,
`COMPRESSED_KEY_HEX_LENGTH`, `UNCOMPRESSED_XY_HEX_LENGTH`, `SECRET_HEX_LENGTH`, and the
`BaseKey(byte[])` constructor.

**Residual external risk.** All five are `protected` on a public abstract class, so an
out-of-tree subclass could reference them. `PublicKey` is `sealed`, and the other subclasses ship
in this library, so the exposure is theoretical — but it is technically source-breaking for a
downstream subclass. Note it in the changelog under Removed; do not let it block the change.

## Compatibility

**This is a fail-closed change and it will reject input that previously parsed.** That is the
intent, but it has an operational consequence worth stating plainly: if any already-minted proof
in a live vault carries a malformed P2PK lock, it will now fail to deserialize where before it
loaded and silently misbehaved.

Two mitigations:

1. `MalformedP2PKSecretException` extends `IllegalArgumentException`, so callers that already
   handle bad input keep working.
2. A mint MUST be able to distinguish "this proof is unspendable" from "the server broke". NUT-11
   frames these conditions as *rejection*, not as a parse crash — so the mint layer should catch
   `MalformedP2PKSecretException` and return the protocol's unspendable-proof error, not a 500.
   That mapping lives in the mint, not in this library, but this library must make it possible
   by throwing one identifiable type.

Before release, survey existing stored proofs for malformed locks. If any exist, they are already
unspendable — the exception surfaces a loss that has already happened rather than causing one.

## Testing

Unit tests in `cashu-lib-common`, one per rule. Minimum set:

**Key validation** — reject: 32-byte x-only; 64-byte uncompressed x‖y; 65-byte SEC1; `04` prefix;
33 bytes with a `05` prefix; 33 bytes, `02`-prefixed, x not on the curve; odd-length hex;
non-hex characters. Accept: valid `02`, valid `03`, uppercase hex, mixed-case hex.

**Comparison form** — `02‖x` and `03‖x` produce the same value; uppercase and lowercase produce
the same value.

**Malformed secrets** — duplicate `sigflag` tag; `n_sigs` = 0; `n_sigs` = 3 with 2 main keys;
unknown sigflag string; `02‖x` in `data` and `03‖x` in `pubkeys` (the spec's own example of a
fatal duplicate); the same key in `data` and in `refund` (**must be accepted** — cross-pathway
duplication is explicitly permitted).

**Getter guards** — a tag present with an empty value list returns the default for `getNSigs`,
`getSigFlag`, `getLockTime` instead of throwing.

**Round-trip** — a valid P2PK secret serializes and deserializes unchanged, with `validate()`
passing at both ends.

**Interop** — the existing `cashu-ts` golden-vector approach (see the definite-length CBOR work)
extended with a P2PK case, so accept/reject agrees with `cashu-ts` on the same inputs.

## Follow-up: deprecated API cleanup (separate release)

**Not part of this change.** The remaining deprecated API has real callers, and `cashu-lib` is a
published artifact — `cashu-mint`, `cashu-vault`, `cashu-gateway`, `cashu-client`,
`cashu-wallet`, `cashu-voucher`, `cashu-ledger` and `imani-bom` all sit downstream. Removing it
is a coordinated multi-repo bump. Folding that into a fail-closed security fix would mean the fix
cannot ship until every consumer is updated, which is the wrong trade: the validation change
should land fast and be backportable.

In-library reference counts (2026-07-27, `cashu-lib` only — **consumers not yet surveyed**):

| Symbol | Refs in cashu-lib |
|---|---|
| `CompressedPublicKey` | 18 |
| `UnCompressedPublicKey` | 8 |
| `VoucherWellKnownSecret` | 6 (superseded by `VoucherSecret`) |
| `P2PKSecret.fromString` | 2 |
| `BaseKey.toBytes()` | needs a narrower grep — a naive `toBytes()` search returns 23, but most are `Secret.toBytes()`, a live interface method |

**First step of that spec is a grep across every downstream repo**, not a code change. The
consumer survey decides whether this is an afternoon or a fortnight, and it is the only thing
that can size it honestly. Target a major version bump.

## Upstream follow-up

NUT-11 does not actually specify this. The spec enumerates four malformed conditions, each
closing "the Proof **MUST** be rejected as unspendable" — none covers a structurally invalid
public key. "Public keys MUST use the compressed Secp256k1 public key format" binds whoever
constructs the secret and carries no paired rejection rule.

That omission is why this library and `cashu-ts` diverged. Once this work lands, file a PR
against `cashubtc/nuts` adding the fifth case, in the same shape as the existing four:

> If any public key in `data`, `pubkeys`, or `refund` is not a valid compressed secp256k1 point,
> the P2PK secret is malformed and the Proof **MUST** be rejected as unspendable.

Worth tracing first: whether any implementation conflates an *invalid key* with an *unsupported
condition*, which the spec permits to be treated as anyone-can-spend. If that path is reachable,
this stops being a wording fix and becomes a security note.
