# ADR 0003: NUT-10 secrets keep the string they arrived as

## Status

Accepted, 2026-08-29.

## Context

`WellKnownSecretSerializer` emitted a four-element NUT-10 secret:

```
["P2PK","<datahex>","<nonce>",[["sigflag","SIG_INPUTS"]]]
```

NUT-10 specifies two elements, the second an object:

```
["P2PK",{"nonce":"...","data":"...","tags":[["sigflag","SIG_INPUTS"]]}]
```

The deserializer accepted both but named the spec form "legacy", which is
backwards. Every published NUT-11 vector failed against this library, and any
NUT-10 secret it emitted was unreadable by every other implementation.

The shape was only the visible half. A proof commits to
`Y = hash_to_curve(secret)`, and NUT-11 requires that "the message to sign MUST
be constructed using the unescaped secret string". Both therefore commit to the
bytes that *arrived*. This library parsed a secret into an object and then
re-encoded it whenever it needed the string again, so key order, whitespace, hex
case and escaping could each turn a valid proof into an unspendable one. The
four-element bug was one instance of that larger defect.

## Decision

**A secret that was parsed from the wire replays its original string and is
never re-encoded.** `WellKnownSecret` keeps the exact string it was parsed from
and `toString()` returns it verbatim. Every mutator clears it, because a mutated
secret is no longer the one that arrived and replaying the original would
misdescribe it.

**Secrets this library constructs are encoded in the NUT-10 form**, with key
order `nonce`, `data`, `tags`, and every tag value written as a string, which is
the only type NUT-10 tags hold.

**`Proof.secret` is written as a JSON string.** NUT-00 defines the field as a
string, and NUT-11 prints the escaped string form explicitly. A NUT-10 secret is
itself JSON, so without an explicit serializer Jackson inlined it as a nested
JSON *array*. That is a distinct defect from the shape of the secret, and it
survived the first correction: a mint reading the array form computes a
different `hash_to_curve` preimage, or fails to parse the proof at all. The
secret written is `Secret.toString()`, so a received secret is replayed
byte-for-byte here too.

**Both wire forms are still read.** The four-element form is what releases up to
0.23.0 emitted, and proofs issued under it must stay spendable.

Tag values are also *stored* as strings rather than converted to enums and ints
on the way in. That keeps one representation everywhere and makes a constructed
secret equal to the same secret parsed back, which is what a round-trip test is
really asserting. `P2PKSecret` already casts on read, as NUT-11 expects a wallet
to.

## Consequences

Every published NUT-11 vector now passes: 30 of 30 in `cashu-mint`'s
`Nut11TestVectorsTest`, which was disabled pending this change and is now the
standing interoperability gate.

A secret constructed by this library and serialized now produces a different
string than before, and therefore a different `Y`. That does not strand existing
proofs, because a received secret replays its original string rather than being
re-encoded, which is precisely the property that makes this change safe. It does
mean a proof whose secret this library *constructed* and stored only as an object,
never as the received string, would resolve to a different `Y` than it did on
0.23.0. Callers holding such secrets must re-derive them before the upgrade.

This is the third encoding correction in the same family, after
[ADR 0001](0001-hash-to-curve-secret-encoding.md) (which bytes are hashed) and
[ADR 0002](0002-secret-string-storage-encoding.md) (which bytes are stored). The
common cause each time was the library deciding for itself what a secret's bytes
were, rather than deferring to what arrived. Preserving the wire string removes
that class of defect rather than another instance of it.

## Evidence

`WellKnownSecretSerializationTest` pins each half of this decision separately,
because they fail independently: the emitted two-element shape, string-valued
tags, byte-exact replay of a received secret, agreement with the published `Y`
`02561ea0...a9`, `Proof.secret` round-tripping as an unchanged JSON string, and
the flattened form still parsing and keeping its original `Y`.

## References

- [NUT-10](https://github.com/cashubtc/nuts/blob/main/10.md)
- [NUT-11](https://github.com/cashubtc/nuts/blob/main/11.md)
- Reported as cashu-lib#254, found by cashu-mint#383
