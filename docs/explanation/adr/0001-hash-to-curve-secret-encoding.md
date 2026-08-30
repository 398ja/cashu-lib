# ADR 0001: hash_to_curve hashes the UTF-8 bytes of the secret string

- **Status:** Accepted
- **Date:** 2026-08-28
- **Issue:** [cashu-lib#242](https://github.com/398ja/cashu-lib/issues/242)
- **Spec:** [NUT-00](https://github.com/cashubtc/nuts/blob/49a909ce4d0739824b3859d4b3da21e6c1abdaeb/00.md), pinned at `49a909c`

## Context

`BDHKEUtils.hashToCurve(String)` hex-decoded any secret that did not begin with `[` before hashing
it, and UTF-8 encoded the ones that did. NUT-00 says otherwise:

> `x` UTF-8-encoded random string (secret message), corresponds to point `Y = hash_to_curve(x)` on curve

A realistic Cashu secret is a 64-character hex *string*. The spec hashes its 64 ASCII bytes; this
library hashed the 32 bytes those characters decode to. Every proof it issued therefore committed to
a different curve point `Y` than Nutshell or cashu-ts computes for the same secret, making our ecash
and everybody else's mutually unspendable.

The spec text alone was judged ambiguous, so the decision was deliberately deferred until a
measurement could settle it.

## Evidence

Two independent instruments answered the question the same way.

**1. The NUT-12 vectors.** NUT-00's own vectors cannot adjudicate this: their messages are raw byte
arrays printed as hex, so hex-decoding and UTF-8 encoding are two different operations on two
different inputs, and both readings pass. The NUT-12 published `Proof`, by contrast, carries a
secret, a blinding factor and a mint public key, and DLEQ verification reconstructs `B_` from
`hash_to_curve(secret)`. That reconstruction succeeds under exactly one encoding:

| Encoding fed to `hash_to_curve` | DLEQ verifies |
| --- | --- |
| Hex-decode (the previous behaviour) | no |
| UTF-8 encode the secret string | **yes** |

`Nut12VectorTest.shouldVerifyWhenProofCarriesValidDleqProofWithBlindingFactor` was left failing as
the standing instrument for this decision, and now passes.

**2. The Nutshell interoperability harness** ([cashu-mint#392](https://github.com/398ja/cashu-mint/issues/392)).
The mint leg succeeds and the swap leg then fails with `verify_proof_failed_error`: proofs Nutshell
verifies for itself did not verify for us.

## Decision

`hash_to_curve` hashes the **UTF-8 bytes of the secret string**, for every secret, NUT-10 well-known
or not.

Because that invalidates every proof this library has already issued, the correction is a migration
rather than a patch. The two encodings are modelled as a first-class named concept,
`xyz.tcheeric.cashu.crypto.SecretEncoding`, rather than a boolean threaded through call sites:

- `SecretEncoding.SPEC` encodes the secret as UTF-8. It is what `SecretEncoding.forIssuance()`
  returns, and it is the **only** encoding used to issue a proof.
- `SecretEncoding.LEGACY_HEX` reproduces the pre-migration behaviour: hex-decode a plain secret,
  UTF-8 encode a well-known one. It exists solely so that already-issued proofs keep verifying.
- `SecretEncoding.verificationOrder()` is the ordered list tried on verification: `SPEC` first, then
  `LEGACY_HEX`. `BDHKEUtils.verify` and the string-secret overload of
  `DLEQUtils.verifyProofWithBlindingFactor` walk that list and accept a proof that satisfies any
  entry.

`LEGACY_HEX.supports(secret)` is false for a secret that is neither hex nor a well-known secret, so
the fallback never turns into a blanket accept and never widens what verification will take beyond
the two commitments a proof could actually have been issued under.

### Why order, not detection

The encoding a proof was issued under is not recorded anywhere on the wire, so it cannot be read off
the proof. Trying the encodings in a defined order is the only available discriminator, and putting
`SPEC` first means the legacy path is exercised only by genuinely old proofs and decays naturally as
they are spent.

## Consequences

- Newly issued proofs interoperate with Nutshell, cashu-ts and every other implementation.
- Proofs issued by a mint that ran the previous behaviour continue to verify, and are spendable
  exactly once more: the outputs of that spend are issued under `SPEC`. The legacy population is
  therefore self-draining.
- Verification of a proof that is invalid under both encodings costs two `hash_to_curve`
  computations instead of one. `hash_to_curve` is a pair of SHA-256 digests, so this is immaterial.
- `LEGACY_HEX` can be removed once no unspent legacy proof remains. That is a follow-up, gated on
  operators confirming their legacy proof count has reached zero.

### What cashu-mint must do

`cashu-mint` is not changed by this ADR and must be updated separately. Concretely:

1. **Take the dependency.** Upgrade to the cashu-lib release carrying `SecretEncoding`.
2. **Verify through the dual path.** Any mint-side check of `C = k*Y` must call
   `BDHKEUtils.verify(String secret, ...)`, which already walks `SecretEncoding.verificationOrder()`.
   Any place that instead computes `Y` itself and compares must be changed to try both encodings, in
   the same order, or to delegate to `BDHKEUtils`.
3. **Key the spent-proof store on both `Y` values.** This is the sharp edge. A double-spend check
   that stores `Y = hash_to_curve(secret)` will compute a different key after this change, so a
   legacy proof already spent would look unspent. On lookup the mint must check both
   `SPEC` and `LEGACY_HEX` values of `Y`, and on insert it must record the `Y` under the encoding the
   proof actually verified under. Failing to do this is a double-spend hole, not a compatibility
   inconvenience.
4. **Issue under `SPEC` only.** Blind signing derives `Y` from the wallet's `B_`, so issuance needs
   no change, but any mint-side derivation of `Y` from a secret string must use
   `SecretEncoding.forIssuance()`.
5. **Re-run the interop harness.** cashu-mint#392's swap leg is the acceptance test: it must move
   past `verify_proof_failed_error`.

## References

- Finding L1, `docs/explanations/nut-compliance-audit.md` in `cashu-mint`
- [What the NUT test vectors cannot pin down](../../reference/nut-test-vector-coverage.md)
