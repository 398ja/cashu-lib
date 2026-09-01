# Changelog

All notable changes to the Cashu Library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.28.0] - 2026-08-29

### Fixed

- **`Proof.secret` is serialized as a JSON string** (#254), which is what NUT-00 defines it to be.
  A NUT-10 well-known secret is itself JSON, so Jackson inlined it as a nested JSON *array* rather
  than as a string containing that array. This is the remaining half of #254: correcting the shape
  of the secret in 0.27.0 left the field around it still wrong, and NUT-11 prints the escaped
  string form explicitly. A mint reading the array form derives a different `hash_to_curve`
  preimage, or cannot parse the proof at all.

### Added

- `WellKnownSecretSerializationTest` pins the NUT-10 secret encoding decided in ADR 0003, which
  previously had no dedicated test: the two-element shape, string-valued tags, byte-exact replay of
  a received secret, agreement with the published `Y`, and the pre-0.24.0 flattened form still
  parsing and keeping its original `Y`.

## [0.27.0] - 2026-08-29

### Fixed

- **NUT-18 payment requests are encoded as definite-length CBOR** (#255), matching the published
  vectors, and carry only the fields NUT-18 defines. Jackson wrote indefinite-length maps and
  serialized every public getter, so each transport gained `type`, `nostr` and `httpPost` entries
  from methods that exist for callers rather than for the wire. Both survived decoding, so only a
  comparison against the published bytes could see them.

### Added

- **NUT-18 and NUT-20 test vectors are vendored and driven**, completing the vector coverage for
  every NUT this library implements. The NUT-20 vectors publish the exact `msg_to_sign` bytes and
  their hash, which confirms our mint-quote signature encoding against the spec rather than only
  against itself.
- The NUT-18 vectors immediately found a real defect: payment requests serialize as
  indefinite-length CBOR where the spec uses definite-length, and emit an empty transport array
  where the spec omits the field. Both decode correctly, so only a byte comparison catches them.
  Fixed by `PaymentRequestCborEncoder` (#255).

## [0.26.0] - 2026-08-29

### Added

- **NUT-02 keyset ID v2 derivation** (#247). `KeySetIdV2Derivation` derives the 33-byte `01`-prefixed
  id over the keys *and* the keyset metadata. That is the point of the version: under v1 a mint
  could change `input_fee_ppk` while keeping the same keyset id, so a wallet holding a proof could
  not tell which fee it had agreed to. Under v2 a changed fee is a different keyset by construction.
  All three published v2 vectors pass, including the zero-fee case the spec says MUST be omitted
  from the preimage and the vector that publishes no final expiry at all.
- **NUT-13 HMAC-SHA256 derivation for v2 keysets** (#247). v1 keysets derive secrets through BIP32
  and v2 keysets through this KDF; `SecretFactory.createDeterministic(seed, masterKey, ...)`
  dispatches on the version. Without it, issuing a v2 keyset would leave every wallet unable to
  recover from it, and a wrongly-derived recovery is indistinguishable from an empty wallet. Driven
  by the published NUT-13 v2 vectors, which were already vendored but never exercised.
- A TokenV4 carries a 33-byte v2 keyset id without truncating it, now asserted rather than assumed.

## [0.25.0] - 2026-08-29

### Added

- **NUT-20 signed mint quotes.** `PostMintQuoteRequest`/`Response` carry an optional `pubkey` and
  `PostMintRequest` an optional `signature`. `MintQuoteSignatureMessage` builds the message the
  spec defines and `MintQuoteSignature` verifies the BIP-340 signature over it. Without this a
  quote id is a bearer token: NUT-04 warns that anyone who learns the id of a paid quote can take
  its ecash, and quote ids travel through logs, webhooks and traces.
- The signature commits to the quote id **and** every output in request order, so a captured
  signature cannot be replayed with substituted or reordered outputs to redirect the ecash.

## [0.24.0] - 2026-08-29

### Fixed

- **BREAKING CHANGE: NUT-10 secrets are serialized in the spec's `[kind, {object}]` form**, not the
  four-element `[kind, data, nonce, tags]` array releases up to 0.23.0 emitted and no other
  implementation reads. All 30 published NUT-11 vectors now pass; every one of them failed before
  (cashu-lib#254, found by cashu-mint#383).
- **A secret parsed from the wire is never re-encoded.** NUT-11 requires the signed message to be
  built from the unescaped secret string and NUT-00 derives `Y` from that same string, so both
  commit to the bytes that arrived. `WellKnownSecret` now replays the original string and clears it
  on mutation. The four-element bug was one symptom of re-encoding; this removes the class.
- NUT-10 tag values are stored as strings, the only type the spec's tags hold, instead of being
  converted to enums and ints on the way in. A constructed secret is now equal to the same secret
  parsed back.
- The four-element form is still read, so proofs issued under it stay spendable. The deserializer no
  longer calls the spec form "legacy", which was backwards.

See [ADR 0003](docs/explanation/adr/0003-nut10-secret-serialization.md), including which callers
must re-derive secrets before upgrading.

## [0.28.0] - 2026-08-29

---

## [0.23.0] - 2026-08-29

> ## ⚠️ FUND-LOSS FIX — UPGRADE IMMEDIATELY, SKIP 0.22.0
>
> **Do not run 0.22.0.** On 0.22.0 a NUT-13 deterministic wallet blinded against one curve
> point and told the mint about another. The consequences were silent, not loud:
> `/checkstate` **reported already-SPENT proofs as spendable balance**, and NUT-13 restore
> **recovered nothing** while appearing to succeed. Both failure modes lose funds without
> raising an error.
>
> **Required consumer action:** upgrade to 0.23.0, then re-derive and re-check every
> deterministic proof produced under 0.22.0. Any balance figure computed on 0.22.0 must be
> treated as untrusted until re-verified against the mint.

### Fixed

- **BREAKING CHANGE / FUND LOSS: `DeterministicSecret.getData()` now returns the UTF-8 bytes of
  the hex string the secret is transmitted as, rather than the 32 raw derived bytes** (`6e23f51`).
  A proof commits to `hash_to_curve(secret_string)`, and for a NUT-13 secret that string is the
  hex encoding returned by `toString()`. Feeding the raw derived bytes to `hash_to_curve` produced
  a different `Y` than the one the mint computes from the transmitted secret, so the wallet's
  view of proof state and the mint's view disagreed: spent proofs looked spendable to
  `/checkstate`, and `/restore` matched nothing. See
  [cashu-wallet#40](https://github.com/398ja/cashu-wallet/issues/40) and the audit in
  `cashu-wallet` at `docs/explanation/cashu-lib-0.22.0-secret-encoding-audit.md`.
- **This break is silent at compile time.** `getData()` keeps its signature and returns different
  bytes, so a caller deriving anything from it keeps compiling and starts producing different
  results. Every call site must be reviewed rather than assumed correct.

### Added

- `DeterministicSecret.getDerivedBytes()` returns the raw NUT-13 derivation output, before hex
  encoding. This is the only supported way to reach the pre-0.23.0 `getData()` value, and it must
  not be used for anything cryptographic on a proof.

- **Five extension error codes reserved for mint-specific conditions** in the 90000 range:
  `voucher_not_accepted` (90025), `iou_not_meltable` (90026), `unsupported_proof_type` (90027),
  `invalid_blind_signature` (90028) and `payment_unknown` (90029). Without them a mint has to
  collapse five distinct failures into `internal_error`, which tells a client nothing about what
  went wrong or whether retrying helps. Purely additive; no existing code or HTTP status changes.
  Needed by the `cashu-mint` error-code migration
  ([#398](https://github.com/398ja/cashu-mint/issues/398),
  [#388](https://github.com/398ja/cashu-mint/issues/388)).

### Changed

- **BREAKING CHANGE: `RandomStringSecret.getData()` and `getBytes()` now return the UTF-8 bytes of
  the secret string, not the bytes a hex secret decodes to.** For the 64-character hex secret NUT-00
  recommends, a caller that previously received 32 decoded bytes now receives 64 ASCII bytes. **This
  break is silent: it changes the bytes returned rather than failing to compile**, so a caller that
  derives anything from `getData()` will keep running and produce different results. The new value is
  the one `hash_to_curve` actually consumes, matching `SecretEncoding.SPEC` (ADR 0001), so the
  storage and hashing layers now agree by construction. A caller that genuinely wants the entropy
  behind a hex secret must hex-decode `toString()` itself.
- `RandomStringSecret.fromBytes(byte[])` is deprecated in favour of `fromEntropy(byte[])`. The old
  name was ambiguous in exactly the place the defect lived: it takes *random bytes to hex-encode*,
  not *the bytes of a secret string*. Use `fromString(String)` for the latter.

### Fixed

- **NUT-00 secrets are no longer required to be hex.** `RandomStringSecret.fromString` hex-decoded
  its argument, so deserializing a proof whose secret was not valid hex failed with
  `exception decoding Hex string` before any protocol logic ran. NUT-00 only *recommends* a
  64-character hex string; a secret is a UTF-8 string. The secret string is now stored verbatim, so
  proofs minted by other implementations with non-hex secrets parse, and a secret round-trips
  byte-exactly, preserving case. Uppercase hex is no longer normalised to lowercase, which would
  have changed `Y` and made the proof unspendable. Proofs issued under the pre-ADR-0001 legacy hex
  encoding still verify through `SecretEncoding.verificationOrder()`, covered by a regression test.
  See ADR 0002 (`docs/explanation/adr/0002-secret-string-storage-encoding.md`).

---

## [0.22.0] - 2026-08-28

> ## ⛔ SKIP THIS VERSION
>
> 0.22.0 loses funds on NUT-13 deterministic wallets: `DeterministicSecret.getData()` returned
> the raw derived bytes instead of the transmitted secret string, so spent proofs were reported
> as spendable balance and restore recovered nothing. Anyone on 0.22.0 is exposed. Go straight
> to 0.23.0.

Milestones 1 and 2 of the NUT compliance plan. This release changes wire formats in
five places so that tokens, proofs, keysets, quotes and errors this library produces
match what every other Cashu implementation produces. **It contains a breaking change
to the error type and error wire format — see *Removed* below.**

### Removed

- **BREAKING CHANGE: `xyz.tcheeric.cashu.common.util.Error` and
  `xyz.tcheeric.cashu.common.json.deserializer.ErrorDeserializer` are removed**, replaced by
  `xyz.tcheeric.cashu.entities.rest.ErrorResponse`. The error wire format changes shape:
  it is now the NUT-00 body `{"detail": <string>, "code": <int>}`, serialized through Jackson.
  **Any consumer that constructs, parses or asserts on the old error format will not compile
  or will not parse this format.** In particular a consumer that declares its own class at
  `xyz.tcheeric.cashu.entities.rest.ErrorResponse` now collides with the library's, and a
  consumer reading a `code`/`message` string pair must be changed to read `detail`/`code`.

### Added

- **NUT-00 error responses now carry the spec's numeric codes.** `ErrorResponse` serializes
  `{"detail": <str>, "code": <int>}` through Jackson, so a detail containing quotes or
  backslashes stays valid JSON — it must never be built with string formatting. `CashuErrorCode`
  carries the numeric codes from the spec's `error_codes.md`, and each code also carries the HTTP
  status a mint returns with it, so a REST layer can map a failure directly rather than
  reconstructing the mapping. The previous string keys survive as the enum constant names, so
  downstream switch sites over the names keep compiling. `CashuErrorException` now carries the
  `CashuErrorCode` as a field, removing the need to re-parse an exception message
  ([#243](https://github.com/398ja/cashu-lib/issues/243)).

- `SecretEncoding` names the two byte encodings a secret can be fed to `hash_to_curve` under, and
  makes the migration explicit: `SecretEncoding.forIssuance()` returns the spec (UTF-8) encoding and
  is the only encoding used to issue a proof, while `SecretEncoding.verificationOrder()` is tried in
  order on verification so proofs issued under the legacy hex-decode encoding keep verifying.
  `BDHKEUtils.hashToCurve(String, SecretEncoding)` and a string-secret overload of
  `DLEQUtils.verifyProofWithBlindingFactor` expose the dual path. Mints must also key their
  spent-proof store on both `Y` values; ADR 0001 records what `cashu-mint` has to do.

- **Official NUT test vectors now run on every build.** The cashubtc/nuts vectors are vendored at
  pinned commit `49a909c` under `cashu-lib-common/src/test/resources/vectors/cashubtc-nuts/` and
  driven by parameterized tests for NUT-00, 01, 02, 11, 12 and 13, so a mismatch fails
  `mvn verify`. The remaining failures are left failing deliberately: they are the instrument
  for the encoding and wire-format decisions they expose, documented in
  [What the NUT test vectors cannot pin down](docs/reference/nut-test-vector-coverage.md), which
  also records the properties the vectors cannot adjudicate.

- `KeysetIdVersion` resolves the NUT-02 keyset id version from the leading version byte and id
  length, and `KeysetId.getVersion()` exposes it. `KeysetId` now accepts version 2 ids
  (66 hex characters) in addition to version 1 ids (16 hex characters).
- `UnsupportedKeysetVersionException` carries the offending keyset id and version.

- **NUT-02 input fees are now resolved per proof.** `InputFeeCalculator` takes a
  `KeySetResolver` (keyset id to keyset) and yields the total fee for a collection of proofs,
  summing each proof's own `input_fee_ppk` and rounding up to whole units. `UnknownKeySetException`
  carries `CashuErrorCode.keyset_not_known` (NUT-02 `12001`) for an unresolvable keyset id.
- `ActiveKeySet` — the `GET /v1/keysets` entry — now carries `input_fee_ppk` and the optional
  `final_expiry`. `fromKeySet` propagates the fee from the source keyset, and `final_expiry` is
  omitted from the JSON when absent rather than serialized as `null`.

- **NUT-04 and NUT-05 quote responses are complete.** `PostMintQuoteResponse` and
  `PostMeltQuoteResponse` now carry the fields NUT-04, NUT-05 and NUT-23 require, and
  `MeltQuoteState` names the states a melt quote can hold — `PENDING` among them, so a wallet
  can finally tell an in-flight payment from a settled one
  ([#244](https://github.com/398ja/cashu-lib/issues/244)).

### Fixed

- **NUT-00 `hash_to_curve` now hashes the UTF-8 bytes of the secret string.** It previously
  hex-decoded any secret that was not a NUT-10 well-known secret, so every proof this library issued
  committed to a different curve point `Y` than Nutshell or cashu-ts computes for the same secret.
  The NUT-12 vectors and the Nutshell interoperability harness both adjudicated the encoding; see
  [ADR 0001](docs/explanation/adr/0001-hash-to-curve-secret-encoding.md)
  ([#242](https://github.com/398ja/cashu-lib/issues/242)).

- **TokenV4 CBOR wrote its top-level keys in the wrong order.** `TokenV4CborEncoder` emitted
  `t,m,u,d` where NUT-00 orders them `t,d,m,u`, so our tokens decoded identically but were
  byte-different from the published ones and any byte-level comparison against another
  implementation disagreed. Decoding is unaffected and stays order-tolerant, so no
  already-issued token stops parsing
  ([#250](https://github.com/398ja/cashu-lib/issues/250)).

- **P2PK proofs from other implementations could not be read.** Every published example uses
  both of the wire forms this library rejected: `n_sigs` written as a JSON string (`"2"`) was
  refused as not-an-integer, though NUT-11 writes tag values as strings throughout, and a
  witness arriving as a JSON-encoded string had no deserializer at all. `WitnessDeserializer`
  now accepts the JSON-encoded string form alongside the nested object this library emits, and
  reads an empty string as a null witness rather than an empty one
  ([#251](https://github.com/398ja/cashu-lib/issues/251)).

- **Input fees were computed from a single caller-supplied keyset.**
  `PostInputRequest.getFees(KeySet)` applied one keyset's fee to every input and checked the
  keyset id with an `assert`, which is disabled at runtime by default: a proof from another
  keyset was silently priced from the wrong one. Since NUT-02 keeps inactive keysets spendable,
  multi-keyset input sets are the routine case. The method now takes a `KeySetResolver` and
  throws `UnknownKeySetException` (`12001`) for an unknown keyset id.

- **NUT-13 derivation silently produced unrecoverable secrets for a version 2 keyset.**
  `SecretFactory` applied version 1 derivation to any keyset id, so a version 2 keyset yielded
  secrets the mint never signed: restore returned nothing and was indistinguishable from an
  empty wallet. Deterministic derivation now dispatches on the keyset id version and throws
  `UnsupportedKeysetVersionException` for versions it cannot derive. Version 2 derivation itself
  is still unimplemented.

---

## [0.21.0] - 2026-07-27

NUT-11 P2PK secrets are now validated. **This release rejects input that previously
parsed** — that is the intent, but see *Changed* for the operational consequence.

### Security

- **A malformed P2PK lock could silently degrade into an unlocked bearer secret.**
  `SecretUtil.toSecret` caught `Exception` around NUT-10 array parsing and fell through to
  `RandomStringSecret` — a NUT-00 random-string secret, which carries **no spending
  condition at all**. Any P2PK secret rejected during parsing therefore came back as a
  proof spendable by anyone holding it: the lock was dropped rather than refused, which is
  strictly worse than the malformed lock itself. `MalformedP2PKSecretException` now
  propagates instead of being swallowed.

  NUT-11 permits treating an *unsupported spending condition* as anyone-can-spend; it says
  nothing about an *invalid key*. This code extended the former to the latter.

### Added

- **`P2PKPublicKeys`** — NUT-11 key validation and comparison.
  - `requireValid(String|byte[], String position)` enforces compressed-only (33 bytes,
    `02`/`03` prefix) and **eager** secp256k1 curve membership. Deliberately stricter than
    `PublicKey`, which accepts 66 *or* 128 hex chars (33, 64 or 65 bytes) and stores bytes
    without decompressing, so an off-curve key was accepted and only failed later on an
    unrelated path.
  - `toComparisonForm(String)` yields the lowercase x-coordinate. BIP-340 signs on the
    x-coordinate alone, so `02||x` and `03||x` are one signing key — NUT-11 declares them
    duplicates of each other, and comparing full compressed hex misses the spec's own
    worked example.
  - Uppercase and mixed-case hex are normalised, not rejected: NUT-11 lists them as valid
    equivalent encodings.
- **`MalformedP2PKSecretException`** — extends `IllegalArgumentException`, so existing
  callers that handle bad input keep working. Carries the failing position (`data`,
  `pubkeys[2]`, `refund[0]`) and reason, **never key material**.
- **`P2PKSecret.validate()`** — enforces NUT-11's four malformed-secret rules: each tag at
  most once; `n_sigs` / `n_sigs_refund` a positive integer not exceeding its pathway's key
  count; a recognised sigflag; no duplicate key within a pathway. Cross-pathway key reuse
  stays permitted, as the spec requires.

### Changed

- **Validation is enforced at both parse boundaries and at construction.** `SecretUtil` and
  `WellKnownSecretDeserializer` are two *independent* P2PK ingress paths — `SecretUtil`
  reimplements secret construction to avoid Jackson's double-hex-decode cycle — so both now
  validate. `P2PKSecret`'s byte-array constructors and `setPubKeys` / `addPubKey` /
  `setRefund` / `addRefund` validate their input at the point of the mistake.
- **Operational note for mints:** an already-stored proof carrying a malformed P2PK lock
  will now fail to deserialize where it previously loaded and silently misbehaved. Catch
  `MalformedP2PKSecretException` and map it to the protocol's unspendable-proof error
  rather than letting it surface as a server fault — NUT-11 frames these conditions as
  rejection, not as a parse crash. Such proofs were already unspendable; the exception
  surfaces a loss that had already happened rather than causing one.

### Fixed

- **`getNSigs`, `getSigFlag` and `getLockTime` threw out of a getter on a crafted secret.**
  All three called `values.get(0)` after a null check only, so a tag present with an empty
  value list threw `IndexOutOfBoundsException`. Both deserializers accept such a tag.
  `getNSigsRefund` already guarded this and documented why; the guard had never been copied
  to its three siblings.
- **`getNSigs` and `getLockTime` threw `ClassCastException` on the wire path.** Both cast to
  `Integer`, but `deserializeNut10Format` stores integral JSON as `longValue()`, so any wire
  secret carrying `n_sigs` or `locktime` failed. They now accept any `Number`.
- **`getSigFlag` threw `ClassCastException` on a raw string value.** Now tolerated; rejecting
  an unrecognised flag is `validate()`'s job.
- **`getPubKeys` and `getRefund` used an unchecked `(List<String>)` cast**, so a numeric tag
  value surfaced as `ClassCastException` at an arbitrary call site. Now mapped element-wise,
  degrading to a validation failure instead.

### Removed

- **Five dead deprecated members of `BaseKey`**, all verified zero-reference and all the same
  failure class as the bugs above — a wrong key-length constant or a prefix-stripping
  constructor is how a validation bypass gets written:
  - `PUBLIC_KEY_LENGTH_UNCOMPRESSED` — value was **66, should have been 128** (its own
    javadoc said so); any length check against it was wrong.
  - `PUBLIC_KEY_LENGTH_COMPRESSED` — named "compressed" but held **64**, the x-only length;
    a compressed key is 66.
  - `PRIVATE_KEY_LENGTH`, `SECRET_LENGTH` — dead aliases.
  - `BaseKey(String)` — `Hex.decode(hexStr.substring(2))`, stripping the first byte by
    position with no check that it *is* a parity prefix.

  Replacements already existed and were already in use: `PRIVATE_KEY_HEX_LENGTH`,
  `X_COORDINATE_HEX_LENGTH`, `COMPRESSED_KEY_HEX_LENGTH`, `UNCOMPRESSED_XY_HEX_LENGTH`,
  `SECRET_HEX_LENGTH`, and `BaseKey(byte[])`.

  **Source-breaking** for an out-of-tree `BaseKey` subclass — all five are `protected` on a
  public abstract class. `PublicKey` is `sealed` and the other subclasses ship here, so the
  exposure is theoretical; hence a minor rather than a major bump.

  The remaining deprecated API (`CompressedPublicKey`, `UnCompressedPublicKey`,
  `VoucherWellKnownSecret`, `P2PKSecret.fromString`, `BaseKey.toBytes`) has real callers and
  downstream consumers, and is deferred to its own change beginning with a cross-repo survey.

### Upstream

NUT-11 enumerates four malformed-secret conditions, each closing "the Proof **MUST** be
rejected as unspendable" — none covers a structurally invalid public key. "Public keys MUST
use the compressed Secp256k1 public key format" binds whoever *constructs* the secret and
carries no paired rejection rule. That omission is why this library and cashu-ts diverged,
cashu-ts rejecting hard where this library accepted anything. A clarification adding the
fifth case is pending upstream.

---

## [0.20.0] - 2026-07-21

### Fixed
- **V4 token CBOR is now definite-length** (`TokenV4CborEncoder`). `TokenV4.serialize()`
  previously used Jackson's CBOR `ObjectMapper`, which emits indefinite-length maps
  (`0xbf … 0xff`). cashu-ts and other standard wallets only decode definite-length CBOR
  and threw `Unsupported length: 31`, making every issued token un-redeemable by
  browser/JS wallets. A hand-rolled definite-length encoder replaces the Jackson encode
  path (decode via Jackson is unchanged — it reads both). Proven: cashu-ts 4.7.2
  `getDecodedToken()` decodes the Java output. No token-content/format change.

---

## [0.19.0] - 2026-07-13

Backward-compatible release (additive only; no source/binary breaks for
existing consumers).

### Added

- **NUT-11 refund-path signature threshold (`n_sigs_refund`) on `P2PKSecret`**
  — a new optional tag mirroring the existing `n_sigs` tag, but scoped to the
  refund (locktime) path instead of the primary spend path.
  - `P2PKTag.n_sigs_refund` — new enum constant.
  - `setNSigsRefund(Integer)` — sets the tag, mirroring `setNSigs`.
  - `getNSigsRefund()` — reads the tag; **defaults to `1`** (not `-1`) when
    the tag is absent, since NUT-11 specifies the refund path requires a
    single signature by default and existing escrows minted before this tag
    existed must keep their current 1-of-N refund behavior unchanged.
  - `WellKnownSecretDeserializer`/`TagDeserializer` updated to coerce
    `n_sigs_refund` values to `int`, matching `n_sigs`/`locktime` handling.

---

## [0.18.1] - 2026-06-06

Backward-compatible release (no source/binary breaks for existing
consumers; addresses PR #237 review).

### Added

- **NUT-04 v1 wire fields on `PostMintQuoteResponse`** — required by
  modern Cashu wallets (cashu-ts `>= 4.x`), which normalize the
  mint-quote response and reject one that lacks `amount`.
  - `amount` (`int`, `@JsonProperty`) — the amount the quote was
    created for. Its absence caused cashu-ts to throw
    `AmountError: Unsupported amount input type`, blocking every
    client-side mint (imani spec 041).
  - `unit` (`String`, `@JsonProperty`) — the unit the quote
    transacts in (e.g. `"sat"`).
  - `state` (`String`, `@JsonProperty`) — NUT-04 v1 lifecycle state
    (`UNPAID` / `PAID` / `ISSUED`), superseding the boolean `paid`.
- Explicit `@Deprecated` `PostMintQuoteResponse(String, String, boolean, int)`
  constructor preserving the pre-0.18 Lombok all-args descriptor, so
  consumers compiled against 0.16/0.17 don't hit `NoSuchMethodError`
  after the new fields widened the generated all-args constructor.

### Deprecated

- `PostMintQuoteResponse.paid` (boolean) — retained on the wire for
  NUT-04 v0 consumers; new clients should read `state` instead.

> `expiry` stays `int` (Unix seconds fit until 2038) to keep this a
> non-breaking release — the earlier int→long widening was reverted
> per review.

---

## [0.17.0] - 2026-05-23

### Added

- **NUT-08 (Lightning fee return) wire fields on the melt DTOs** —
  required by cashu-mint spec 002 (`002-melt-burn-ordering`, FR-013)
  overpaid-melt change return implementation.
  - `PostMeltRequest.outputs` (`List<BlindedMessage>`,
    `@JsonProperty("outputs")`, `@JsonInclude(NON_NULL)`,
    `@Size(max = MAX_OUTPUTS = 1000)`) — wallet-supplied blinded
    messages the mint signs with the overpayment difference when
    `sum(proofs) > invoice + exactFeeReserve`.
  - `PostMeltResponse.change` (`List<BlindSignature>`,
    `@JsonProperty("change")`, `@JsonInclude(NON_NULL)`) — the
    signed change outputs; omitted from JSON when null so legacy
    melt responses serialise unchanged.
  - New 3-arg `PostMeltRequest(quoteId, proofs, outputs)`
    constructor. New 2-arg back-compat
    `PostMeltResponse(paid, paymentPreimage)` constructor.

### Changed

- `bip-utils` dependency: excluded `slf4j-simple` transitive binding
  so downstream consumers using logback own the SLF4J binding
  without a conflicting `SimpleLoggerFactory`.

---

## [0.16.0] - 2026-02-02

### Added

- **Key Zeroing Support**: `PrivateKey` now implements `AutoCloseable` for secure key disposal.
  - Use try-with-resources for automatic zeroing: `try (PrivateKey key = ...) { ... }`
  - Added `close()` method that zeros internal byte arrays
  - Added `isClosed()` method to check key state
  - Added `zeroBytes()` protected method in `BaseKey` for subclass use
  - Documented JVM memory limitations in Javadoc
- **Custom Crypto Exceptions**: New `xyz.tcheeric.cashu.crypto.exception` package.
  - `CashuCryptoException`: Base exception for all crypto errors
  - `InvalidKeyException`: For invalid or malformed keys (with `KeyType` enum)
  - `SignatureException`: For signature operation failures (with `OperationType` enum)
  - Factory methods for common error scenarios
- **REST DTO Validation**: Jakarta Bean Validation (JSR-380) annotations on all REST DTOs.
  - `@NotNull`, `@NotEmpty`, `@Size(max=1000)`, `@Valid` on collection fields
  - `@NotBlank` on required string fields (quoteId)
  - Resource limits prevent DoS attacks via unbounded collections
  - Added `jakarta.validation-api` dependency to cashu-lib-entities

### Changed

- **Utils.xor()**: Now throws `IllegalArgumentException` on length mismatch instead of returning null
- **DLEQUtils**: `dleqHash()` and `pointToUncompressedHex()` reduced to package-private visibility
- **Schnorr**: Class now `final` with private constructor (utility class pattern)
- **BDHKEUtils**: Class now `final` with private constructor (utility class pattern)

### Security

- **PrivateKey Serialization Blocked**: Added `@JsonIgnoreType` annotation to prevent accidental JSON serialization
- **NaN/Infinity Validation**: `SecretUtil` now rejects NaN and Infinity floating-point values in tags
- **Exception Safety**: Private key values are never included in exception messages
- **Security Documentation**: Added comprehensive security Javadoc to `BaseKey`, `PrivateKey`, `Schnorr`, `BDHKEUtils`, and `DLEQUtils`

---

## [0.15.0] - 2026-01-28

### Changed

- **Package Reorganization**: Classes reorganized into NUT-specific packages for better organization.
  - `nut10/`: Spending conditions (WellKnownSecret, Nut10Option, serializers)
  - `nut11/`: Pay-to-Pubkey (P2PKSecret)
  - `nut12/`: DLEQ Proofs (DLEQProof, DLEQProofDeserializer)
  - `nut13/`: Deterministic Secrets (DeterministicSecret, DeterministicSecretDeserializer)
  - `nut18/`: Payment Requests (PaymentRequest, Transport, Voucher* classes)
  - REST entities in cashu-lib-entities reorganized to `rest/nut03/`, `rest/nut04/`, `rest/nut05/`, `rest/nut07/`, `rest/nut09/`

**Migration Note**: Import paths have changed. Update imports from:
- `xyz.tcheeric.cashu.common.WellKnownSecret` → `xyz.tcheeric.cashu.common.nut10.WellKnownSecret`
- `xyz.tcheeric.cashu.common.P2PKSecret` → `xyz.tcheeric.cashu.common.nut11.P2PKSecret`
- `xyz.tcheeric.cashu.common.DLEQProof` → `xyz.tcheeric.cashu.common.nut12.DLEQProof`
- `xyz.tcheeric.cashu.common.DeterministicSecret` → `xyz.tcheeric.cashu.common.nut13.DeterministicSecret`
- `xyz.tcheeric.cashu.common.PaymentRequest` → `xyz.tcheeric.cashu.common.nut18.PaymentRequest`
- `xyz.tcheeric.cashu.entities.rest.PostSwapRequest` → `xyz.tcheeric.cashu.entities.rest.nut03.PostSwapRequest`
- (and similar for other REST entities)

---

## [0.14.0] - 2026-01-28

### Added

- **NUT-17 WebSocket Subscriptions**: Complete implementation of real-time subscription protocol per [NUT-17 specification](https://github.com/cashubtc/nuts/blob/main/17.md).
  - `SubscriptionKind`: Enum for subscription types (bolt11_mint_quote, bolt11_melt_quote, proof_state)
  - `SubscriptionFilter`: Filter for subscription requests
  - `SubscriptionParams`: Parameters for JSON-RPC subscription requests
  - `SubscriptionResult`: Result payload for subscription responses
  - `JsonRpcRequest`: Generic JSON-RPC 2.0 request wrapper
  - `JsonRpcResponse`: Generic JSON-RPC 2.0 response wrapper
  - `JsonRpcNotification`: Server-to-client notification for state changes
  - `JsonRpcError`: Error response structure
  - `NotificationParams`: Parameters for subscription notifications
  - `ProofStatePayload`: Proof state change notification payload
  - `QuoteStatePayload`: Quote state change notification payload

---

## [0.13.1] - 2026-01-26

### Fixed

- **TokenFingerprint**: V3 multi-mint token fingerprints are now deterministic
  - Previously, iteration over `HashSet<MintProof>` caused non-deterministic fingerprints across JVM runs
  - Each secret is now qualified with its mint URL (`mintUrl:secret`) and sorted lexicographically
  - Tokens with different mint compositions no longer collide if they share the same secrets

### Changed

- **TokenFingerprint**: V3 fingerprint format now includes mint URL per secret (breaking change for stored fingerprints)

---

## [0.13.0] - 2026-01-26

### Added

- **Token Fingerprinting Utilities**: Security utilities for collision-resistant token duplicate detection.
  - `ProofFingerprint`: Computes SHA-256 fingerprints from sorted proof secrets with mint URL
  - `TokenFingerprint`: Parses cashuA (V3/JSON) and cashuB (V4/CBOR) tokens to compute fingerprints
  - Deterministic output regardless of proof ordering (secrets are sorted lexicographically)
  - Fallback to string hashing when token parsing fails
  - Thread-safe implementation with `@ThreadSafe` annotation
  - Comprehensive unit tests for both utilities (24 tests)

---

## [0.12.0] - 2026-01-21

### Added

- **Virtual Thread Compatibility**: Full audit and documentation for Java 21+ Virtual Thread support.
  - `@ThreadSafe` annotations on `BDHKEUtils`, `DLEQUtils`, `Schnorr`, and `KeysUtils`
  - VT compatibility documentation in `docs/explanation/virtual-thread-compatibility.md`
  - Concurrent crypto tests with 100+ Virtual Threads (`VirtualThreadConcurrencyTest`)
  - CI pinning detection with `-Djdk.tracePinnedThreads=short`
  - README section on Virtual Thread compatibility
- Test coverage for `expiresAt` field in `VoucherPaymentRequest`

---

## [0.11.1] - 2026-01-10

### Added

- **NUT-18V Voucher Payment Requests**: Extension for Model B gift card vouchers.
  - `VoucherPaymentRequest`: CBOR-encoded request with `vreqA` prefix and required `issuerId` field
  - `VoucherTransport`: Transport with MERCHANT type for direct merchant API integration
  - `VoucherTransportType`: Enum with NOSTR, POST, and MERCHANT types
  - `VoucherPaymentPayload`: Payment fulfillment with issuer ID and DLEQ for offline verification
  - Offline verification support with `offlineVerification` flag
  - `expiresAt` field for request expiration
  - `allProofsHaveDLEQWithBlindingFactor()` for proper offline verification validation
- **NUT-18V Tests**: Comprehensive test coverage for voucher payment requests

### Fixed

- **VoucherPaymentPayload**: `validateForOfflineVerification()` now requires DLEQ with blinding factor (r), not just any DLEQ

---

## [0.11.0] - 2026-01-09

### Added

- **NUT-18 Payment Requests**: Complete implementation of receiver-initiated payment requests per [NUT-18 specification](https://github.com/cashubtc/nuts/blob/main/18.md).
  - `PaymentRequest`: CBOR-encoded payment request with `creqA` prefix
  - `Transport`: Transport method with type, target address, and optional tags
  - `TransportType`: Enum for transport types (NOSTR, POST)
  - `PaymentPayload`: Payment fulfillment structure with proofs and DLEQ
  - `PaymentPayloadProof`: Proof structure for offline verification
  - `Nut10Option`: NUT-10 locking conditions (P2PK, HTLC, VOUCHER)
- **NUT-18 Tests**: Comprehensive test coverage including official test vectors

### Fixed

- **VoucherSecret**: Improved error handling for invalid UUID in Builder
- **WellKnownSecret**: Fixed null nonce serialization and deserialization
- **VoucherWellKnownSecret**: Backward compatibility and nonce handling

---

## [0.10.0] - 2025-01-07

### Added

- **VoucherSecret**: NUT-10 compliant tag-based voucher secret storage with builder pattern for creating Model B gift card voucher tokens.
- **VoucherTags**: Standard tag keys interface for VOUCHER secrets (issuer, unit, face_value, expires_at, memo, face_decimals, backing_strategy, issuance_ratio, issuer_sig, issuer_pubkey, merchant_metadata).
- **VoucherSecretTest**: Comprehensive unit tests for VoucherSecret serialization and tag-based storage.

### Changed

- **SecretUtil**: Updated to use `VoucherSecret` instead of deprecated `VoucherWellKnownSecret`.
- **WellKnownSecretDeserializer**: Updated to support tag-based voucher secret deserialization.

### Deprecated

- **VoucherWellKnownSecret**: Deprecated in favor of `VoucherSecret`. Marked for removal in a future version.

---

## [0.9.1] - 2025-12-22

### Added

- Unit tests covering `SecretUtil.toY` for NUT-00 hex secrets, NUT-10 JSON secrets, and string/secret parity to guard hash_to_curve consistency.

### Changed

- Bumped parent and module versions to `0.9.1`.

### Fixed

- `SecretUtil.toY` now matches `BDHKEUtils.hashToCurve(String)` decoding rules (hex decode for NUT-00, UTF-8 for NUT-10), keeping Y values consistent with mint verification and `/checkstate`.

---

## [0.9.0] - 2025-12-21

### Added

- **NUT-12 DLEQ primitives**: Introduced `DLEQProof` model and `DLEQUtils` generation/verification helpers for offline signature verification.
- **DLEQ token payloads**: Blind signatures and proofs now carry optional DLEQ data (`e`, `s`, `r`) with JSON/CBOR serialization and validation.
- **Test coverage**: Added unit tests for DLEQ proof serialization and cryptographic verification paths.

### Changed

- Bumped parent and module versions to `0.9.0`.

---

## [0.8.0] - 2025-12-19

### Changed

- **PublicKey**: Consolidated into single unified class that auto-detects input format (33, 64, or 65 bytes)
  - Always stores and serializes as compressed format per NUT-00
  - Added `getUncompressedBytes()` for NUT-12 DLEQ proof hashing
  - Added `getSec1Uncompressed()` for SEC1 format output
- **Signature**: Refactored to be standalone, no longer uses PublicKey internally
  - Stores both compressed (33 bytes) and raw (64 bytes) formats
  - Preserves raw format for Schnorr signature verification
- **BaseKey**: Improved constant naming with clearer semantics
  - Added `PRIVATE_KEY_HEX_LENGTH`, `X_COORDINATE_HEX_LENGTH`, `COMPRESSED_KEY_HEX_LENGTH`

### Deprecated

- `CompressedPublicKey` - use `PublicKey` directly
- `UnCompressedPublicKey` - use `PublicKey` directly
- `PublicKey.fromBytes(bytes, boolean)` - use `PublicKey.fromBytes(bytes)` (auto-detects format)
- `PublicKey.fromString(str, boolean)` - use `PublicKey.fromString(str)` (auto-detects format)
- `PublicKey.fromPoint(ecPoint, boolean)` - use `PublicKey.fromPoint(ecPoint)`
- Old `BaseKey` constants (`PUBLIC_KEY_LENGTH_COMPRESSED`, `PUBLIC_KEY_LENGTH_UNCOMPRESSED`)

### Fixed

- **Signature.fromBytes**: Now correctly handles both 33-byte compressed and 64-byte Schnorr signatures

---

## [0.7.2] - 2025-12-17

### Fixed

- **NUT-10 BDHKE Verification**: Fixed `BDHKEUtils.hashToCurve()` and `verify()` to correctly handle NUT-10 well-known secrets (P2PK, HTLC, VOUCHER)
  - NUT-10 secrets are JSON arrays that should be UTF-8 encoded for `hash_to_curve`, not hex decoded
  - Added detection for NUT-10 format (strings starting with `[`)
  - Legacy hex secrets continue to use hex decoding for backward compatibility
  - Fixes "Invalid hexadecimal character found" errors when verifying voucher proofs

---

## [0.7.1] - 2025-12-16

### Fixed

- **SecretDeserializer**: Fixed double hex-decode issue in `WellKnownSecretDeserializer`
- **SecretUtil**: Direct construction of WellKnownSecret objects instead of using Jackson's `convertValue()`

---

## [0.7.0] - 2025-12-15

### Added

- Initial NUT-10 well-known secret support
- `VoucherWellKnownSecret` for Model B voucher tokens
- `P2PKSecret` for pay-to-public-key conditions
- `WellKnownSecret` base class with NUT-10 JSON serialization

---

## [0.6.0] and earlier

See git history for earlier changes.
