# ADR 0002: a secret is stored as the UTF-8 bytes of its string, and need not be hex

- **Status:** Accepted
- **Date:** 2026-08-28
- **Issue:** [cashu-lib#253](https://github.com/398ja/cashu-lib/issues/253)
- **Spec:** [NUT-00](https://github.com/cashubtc/nuts/blob/main/00.md)
- **Follows:** [ADR 0001](0001-hash-to-curve-secret-encoding.md)

## Context

`RandomStringSecret.fromString` hex-decoded its argument, so reading a proof whose secret was not
valid hex failed outright with `exception decoding Hex string`. NUT-00 says:

> `x` UTF-8-encoded random string (secret message)

and separately *recommends*, without requiring, a 64-character hex string generated from 32 random
bytes. A secret is a string; hex is a convention for producing a good one. Any proof minted by
another implementation with a non-hex secret was therefore unreadable by this library, failing at
JSON parse time, before any protocol logic could run.

This is the same encoding confusion as ADR 0001, one layer up. ADR 0001 settled which bytes are
**hashed**; this settles which bytes are **stored** when a secret is read off the wire. Once ADR
0001 landed, the two layers actively disagreed: `hash_to_curve` consumed the 64 ASCII characters of
a hex secret while `RandomStringSecret` stored the 32 bytes they decode to. The same object then
answered "what are my bytes" differently depending on which layer asked.

## Decision

A `RandomStringSecret` stores **the UTF-8 bytes of the secret string, verbatim**, and accepts any
UTF-8 string. `toString()` returns exactly the string that was read off the wire.

This is deliberately the same convention as `SecretEncoding.SPEC`, rather than a third one:
`secret.getData()` now returns precisely the bytes `hash_to_curve` consumes, for every secret. The
two layers agree by construction, and `hashToCurve(secret.getData())` and
`hashToCurve(secret.toString())` are the same computation.

### Consequences for `getData()` / `getBytes()`

This is a semantic break, not merely a widening. For the recommended 64-character hex secret,
`getData()` previously returned 32 bytes and now returns 64. A caller that assumed hex and decoded
entropy out of `getData()` will silently get different bytes rather than an error.

The new meaning is the correct one, because it is the only meaning that is total: a non-hex secret
has no 32 decoded bytes to return, so "the decoded entropy" is not a property every secret has,
while "the UTF-8 bytes of the string" is. Any caller wanting the entropy of a hex secret must now
decode `toString()` itself, which forces it to state the hex assumption it was previously making
implicitly.

Two API changes make that distinction visible at the call site:

- `RandomStringSecret.fromEntropy(byte[])` replaces `fromBytes(byte[])`, which is deprecated. The
  old name was ambiguous exactly where the bug lived: it took *random bytes to hex-encode*, but read
  as if it took *the bytes of the secret*. Both meanings are now nameable and distinct, with
  `fromString` for the latter.
- `create(int)` names its argument `entropyLength`: it is the number of random bytes drawn, and the
  resulting secret string is twice that many characters.

### What is not changed

Case is preserved rather than normalised. The mint hashes the exact string, so lowercasing an
uppercase hex secret in transit would change `Y` and make the proof unspendable. Round-tripping a
proof through this library is now byte-exact in the secret field.

`SecretUtil.toSecret` still tries the NUT-10 well-known array first and only then treats the input
as a random string, so a well-known secret is unaffected. That fall-through is now more dangerous
than it was, because it can no longer fail loudly on a non-hex input; the guard is
`P2PKSecretBoundaryValidationTest.doesNotDegradeToBearerSecret`, which asserts a malformed P2PK lock
is refused rather than silently downgraded to a bearer secret.

## Evidence

`WitnessDeserializerTest` originally used the plain-text secret `"s"` and failed on the *secret*
field rather than on the witness it was testing; it had been changed to a hex secret to unblock the
build, which left the defect unexercised. It is reverted to plain text and now passes, so the wire
path is covered by a test that fails if the hex assumption returns.

`RandomStringSecretTest` covers the decision directly: a non-hex secret round-trips, uppercase hex
is preserved, `getData()` equals `SecretEncoding.SPEC.encode(...)`, and `Y` derived from the stored
bytes equals `Y` derived from the string.

`NUT00Tests.shouldHashMessagesToExpectedCurvePoints` no longer routes its vectors through
`RandomStringSecret`. As ADR 0001 records, those vectors are raw byte arrays printed as hex, not
secret strings, so they are hex-decoded explicitly. Passing them through a secret type conflated the
two and would now silently test the wrong thing.

## References

- [ADR 0001: hash_to_curve hashes the UTF-8 bytes of the secret string](0001-hash-to-curve-secret-encoding.md)
- [NUT-00](https://github.com/cashubtc/nuts/blob/main/00.md)
