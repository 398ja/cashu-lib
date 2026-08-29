# What the NUT test vectors cannot pin down

The vendored [cashubtc/nuts](https://github.com/cashubtc/nuts) test vectors, pinned at commit
`49a909ce4d0739824b3859d4b3da21e6c1abdaeb`, run on every build from
`cashu-lib-common/src/test/java/xyz/tcheeric/cashu/vectors/`. They are a regression net, not a
compliance certificate. This page records what they cover, what they leave open, and which of them
currently fail.

## Coverage

| NUT | Covered by the vectors | Not covered |
| --- | --- | --- |
| 00 | `hash_to_curve` points, `B_` blinding, `C_` signing, v3 and v4 token serialization, the raw v4 CBOR body | The secret **encoding** fed to `hash_to_curve`, settled instead by the NUT-12 vectors (see below); DLEQ fields inside tokens |
| 01 | Rejection of a truncated and an uncompressed key; acceptance of two valid keysets including a `2^63` amount | Key ordering, unit binding, `/v1/keys` response shape |
| 02 | Version 1 and version 2 keyset id derivation, including the zero-fee case the spec omits from the preimage and a keyset with no final expiry | Nothing published |
| 11 | P2PK secret parsing and validation; BIP-340 verification of a valid and an invalid `SIG_INPUTS` signature; the two published `SIG_ALL` message digests | Whether a proof is *spendable*: locktime evaluation, threshold counting across the main and refund pathways, HTLC preimages. That logic lives in `cashu-mint`, which drives those vectors from its own suite |
| 12 | `hash_e`, the deterministic nonce vector, DLEQ on a `BlindSignature` and on a `Proof` | Mint-side proof *generation*, which is nondeterministic and has no published vector |
| 13 | Version 1 and version 2 secret and blinding-factor derivation for counters 0–4, and the version 1 derivation paths | The NUT-20 P2PK derivation path; counter persistence and restore gap handling, which are wallet concerns |
| 18 | Decoding all seven published payment requests, re-encoding each to the same CBOR structure, and definite-length map encoding | Byte-exact re-encoding, since NUT-18 fixes no key order and the published vectors do not agree on one |
| 20 | The published `msg_to_sign` bytes, its SHA-256 hash, and verification of the published signature | The deterministic quote-locking key derivation `m/129373'/20'/0'/0'/{counter}`, which is a wallet concern (cashu-wallet#41) |

## NUTs with no vendored vectors

Upstream publishes vectors for NUT-26, 27, 28 and 29. None are vendored, because none of those NUTs
are implemented: Bech32m payment request encoding, Nostr mint backup, Pay-to-Blinded-Key and batched
minting respectively. Vendoring vectors for unimplemented specifications would add failing tests that
say nothing except that the feature is absent, which the absent code already says.

## The `hash_to_curve` secret encoding

NUT-00's own vectors **cannot** adjudicate the encoding question raised as finding L1 in the
[NUT compliance audit](https://github.com/398ja/cashu-mint/blob/main/docs/explanations/nut-compliance-audit.md).
Their messages are raw byte arrays printed as hex, so hex-decoding them and UTF-8 encoding them are
two different operations on two different inputs, and the vector fixes only the first. Both readings
of a realistic 64-character secret pass.

**The NUT-12 vectors do adjudicate it.** The published `Proof` with a valid DLEQ proof carries a
secret, a blinding factor and a mint public key, and DLEQ verification reconstructs `B_` from
`hash_to_curve(secret)`. That reconstruction only succeeds under one encoding. Running both:

| Encoding of the secret fed to `hash_to_curve` | NUT-12 proof DLEQ verifies |
| --- | --- |
| Hex-decode (what `BDHKEUtils` does for non-NUT-10 secrets) | **no** |
| UTF-8 encode the secret string | **yes** |

**This has been settled.** `hash_to_curve` now hashes the UTF-8 bytes of the secret string, and
already-issued proofs keep verifying under the legacy hex-decode encoding through the
`SecretEncoding` strategy. The reasoning, the evidence, and what `cashu-mint` must do are recorded in
[ADR 0001](../explanation/adr/0001-hash-to-curve-secret-encoding.md)
([cashu-lib#242](https://github.com/398ja/cashu-lib/issues/242)). The test
`Nut12VectorTest.shouldVerifyWhenProofCarriesValidDleqProofWithBlindingFactor`, left failing as the
standing instrument for that decision, now passes.

## Currently failing vectors

None. Every vendored vector passes.

The NUT-00 CBOR key ordering, the NUT-11 P2PK wire forms, the NUT-12 secret encoding and the NUT-18
payment request encoding were all found this way and have since been fixed; see issues #250, #251,
#242 and #255. Each was invisible to a round-trip test, because each produced output this library
decoded perfectly and no other implementation reproduced.

## Refreshing the vectors

See `cashu-lib-common/src/test/resources/vectors/cashubtc-nuts/README.md`.
