# What the NUT test vectors cannot pin down

The vendored [cashubtc/nuts](https://github.com/cashubtc/nuts) test vectors, pinned at commit
`49a909ce4d0739824b3859d4b3da21e6c1abdaeb`, run on every build from
`cashu-lib-common/src/test/java/xyz/tcheeric/cashu/vectors/`. They are a regression net, not a
compliance certificate. This page records what they cover, what they leave open, and which of them
currently fail.

## Coverage

| NUT | Covered by the vectors | Not covered |
| --- | --- | --- |
| 00 | `hash_to_curve` points, `B_` blinding, `C_` signing, v3 and v4 token serialization, the raw v4 CBOR body | The secret **encoding** fed to `hash_to_curve` (see below); DLEQ fields inside tokens |
| 01 | Rejection of a truncated and an uncompressed key; acceptance of two valid keysets including a `2^63` amount | Key ordering, unit binding, `/v1/keys` response shape |
| 02 | Version 1 keyset id derivation from the published keys | Version 2 keyset ids (issue #247); `input_fee_ppk` and `final_expiry`, which the vectors publish as prose but the library has no type for (issue #246) |
| 11 | P2PK secret parsing and validation; BIP-340 verification of a valid and an invalid `SIG_INPUTS` signature; the two published `SIG_ALL` message digests | Whether a proof is *spendable*: locktime evaluation, threshold counting across the main and refund pathways, HTLC preimages. That logic lives in `cashu-mint`, so the vectors that exercise it cannot be driven from this repository |
| 12 | `hash_e`, the deterministic nonce vector, DLEQ on a `BlindSignature` and on a `Proof` | Mint-side proof *generation*, which is nondeterministic and has no published vector |
| 13 | Version 1 keyset id integer, secrets, blinding factors and derivation paths for counters 0–4 | Version 2 derivation and the P2PK derivation (issue #248); counter persistence and restore gap handling, which are wallet concerns |

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

This is evidence that the `else` branch of `BDHKEUtils.hashToCurve(String)` is wrong, and it is
stronger evidence than the audit expected to be available before the interoperability test of
Milestone 0 item 2. It is not, on its own, a licence to change the encoding: doing so invalidates
every proof already issued, so it stays a Milestone 1 decision with a migration path, tracked as
[cashu-lib#242](https://github.com/398ja/cashu-lib/issues/242). The test
`Nut12VectorTest.shouldVerifyWhenProofCarriesValidDleqProofWithBlindingFactor` is left failing as
the standing instrument for that decision.

## Currently failing vectors

Left failing deliberately. Each is a library defect the vectors were introduced to expose, and
fixing any of them changes production behaviour that Milestone 0 is explicitly not allowed to touch.

| Test | What it shows | Issue |
| --- | --- | --- |
| `Nut12VectorTest.shouldVerifyWhenProofCarriesValidDleqProofWithBlindingFactor` | The `hash_to_curve` secret encoding is hex-decode where the vector requires UTF-8 | [#242](https://github.com/398ja/cashu-lib/issues/242) |
| `Nut00VectorTest.shouldReproducePublishedSerializationWhenRoundTrippingTokenV4` (single and multi keyset) | `TokenV4CborEncoder` emits the top-level CBOR keys as `t, m, u, d`; NUT-00 orders them `t, d, m, u`, so our tokens are byte-different from the published ones even though they decode identically | new |
| `Nut00VectorTest.shouldReproducePublishedCborBodyWhenEncodingTokenV4` | The same ordering defect, seen against the published raw binary token | new |
| `Nut11VectorTest.shouldParseAndValidateWhenProofCarriesPublishedSpendingCondition` | Two distinct parsing defects: `n_sigs` written as a JSON **string** (`"2"`) is rejected as "not an integer", though NUT-11 §Tags writes tag values as strings throughout; and `Proof.witness` arriving as a **JSON-encoded string** (the `P2PKWitness` wire form) has no deserializer, so any real P2PK proof fails to parse | new |

The `Nut11VectorTest` failures matter most of the three: they mean this library cannot read a P2PK
proof produced by any other implementation, because every published example uses both of those wire
forms.

## Refreshing the vectors

See `cashu-lib-common/src/test/resources/vectors/cashubtc-nuts/README.md`.
