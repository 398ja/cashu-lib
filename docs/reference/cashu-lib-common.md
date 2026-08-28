# cashu-lib-common

Core types for Cashu tokens and key management shared across modules.

- **Package**: `xyz.tcheeric.cashu.common`
- **Primary classes**:
  - `Token`, `TokenV3`, `TokenV4`
  - `Keys`, `KeysetId`, `KeySet`
  - `Secret`, `Proof`, `Mint`
  - `SecretUtil`, `HashToCurveSecret`
- **Javadoc**: [cashu-lib-common API Reference](https://javadoc.io/doc/xyz.tcheeric/cashu-lib-common)

## Hash-to-curve helpers

- `SecretUtil.toY(secret)` derives `Y = hash_to_curve(secret_string)` for proofs and `/checkstate`.
- Decoding rules:
  - NUT-00 random secrets (64-char hex) are **hex-decoded** before hashing.
  - NUT-10 well-known secrets (`["KIND","hexdata","nonce",[]]`) are hashed as their UTF-8 JSON string.
- `SecretUtil.toYFromString` performs the same derivation when only the serialized secret string is available.
- `HashToCurveSecret` wraps the resulting compressed public key for transport.

## NUT-02 input fees

A transaction may spend inputs from several keysets, because NUT-02 keeps proofs of inactive
keysets spendable. Fees are therefore resolved per proof, against the keyset that issued it.

- `KeySetResolver` maps a keyset id to its `KeySet`. `KeySetResolver.of(Map<String, KeySet>)`
  builds one from an in-memory map; a mint backs it with its keyset store.
- `InputFeeCalculator.calculateFee(Collection<Proof<?>>)` sums each proof's `input_fee_ppk` and
  rounds up: `(sum + 999) / 1000`.
- `UnknownKeySetException` (NUT-02 `CashuErrorCode.keyset_not_known`, code `12001`)
  is thrown when a proof names a keyset the resolver does not know.

`PostInputRequest.getFees(KeySetResolver)` in `cashu-lib-entities` is the request-level entry point.

## Keyset listing entries

`ActiveKeySet` models one entry of `GET /v1/keysets`: `id`, `unit`, `active`, `input_fee_ppk` and
the optional `final_expiry`, which is omitted from the JSON when absent. `ActiveKeySet.fromKeySet`
propagates the fee from the source `KeySet` and takes the expiry as an optional third argument.
