# NUT-20: Signature on Mint Quote

Package `xyz.tcheeric.cashu.common.nut20`. Two classes, both final utility types
with private constructors.

Specification: [NUT-20](https://github.com/cashubtc/nuts/blob/main/20.md).

## The problem it solves

Without NUT-20, **a quote id is a bearer token**. NUT-04 warns that anyone who
learns the id of a paid quote can take its ecash, and a quote id travels through
logs, webhooks, and traces on its way through a system. Any of those is a place it
can leak.

NUT-20 locks a quote to a public key. Only the holder of the matching private key
can mint against it, so a leaked quote id is no longer enough.

## `MintQuoteSignature`

```java
public static boolean isValid(String quoteId,
                              List<BlindedMessage> outputs,
                              String pubkey,
                              String signature)
```

Verifies a BIP-340 Schnorr signature over the quote id and its outputs. Returns
whether the signature was made by the key the quote is locked to.

### Malformed input is an invalid signature, not an error

A malformed key or signature returns `false` rather than throwing. The caller
refuses to issue either way, and the difference between "wrong signature" and
"wrong encoding" is the client's to fix, not a mint failure to propagate.
Rejections are logged at warn with the quote id and reason.

### The x-only key conversion

BIP-340 verifies against a 32-byte x-only key, while NUT-20 carries the 33-byte
compressed form. `isValid` strips the parity prefix before verifying.

This is not incidental. Passing the compressed form straight through makes **every
spec-conformant key fail verification**, and the failure looks like a wrong
signature rather than a wrong encoding, which is a genuinely difficult bug to read
from the outside.

## `MintQuoteSignatureMessage`

```java
public static byte[] forQuote(String quoteId, List<BlindedMessage> outputs)
```

Builds the exact bytes a wallet signs.

### The signature commits to the outputs, not just the quote

Committing to the quote id alone would leave a captured signature usable with
**substituted outputs**: the same value, redirected to someone else's blinded
messages. The outputs are part of the signed message for that reason, in request
order.

### The encoding is unambiguous by construction

```
"Cashu_MintQuoteSig_v1"          domain tag, raw ASCII, not length-prefixed
<len:4><quoteId UTF-8 bytes>
for each output, in request order:
  <len:4><amount, minimal big-endian>
  <len:4><blinded message bytes>
```

Every variable-length part is preceded by its 32-bit big-endian length, so **no
combination of quote id and outputs can encode to the same bytes as a different
combination**. Without length prefixes, a quote id ending in digits and an amount
beginning with them could produce a colliding message, and a signature over one
would verify against the other.

The domain tag `Cashu_MintQuoteSig_v1` is written as raw ASCII and deliberately not
length-prefixed. It keeps a signature made for a mint quote from ever being valid
in another context that signs a similar-looking byte string.

Amounts use a minimal big-endian encoding: no leading zero bytes, so one amount has
exactly one representation.

## Usage

Mint side, verifying before issuing against a locked quote:

```java
if (!MintQuoteSignature.isValid(quoteId, outputs, quotePubkey, signature)) {
    // refuse to issue; the quote is locked and this caller cannot open it
}
```

Wallet side, producing the signature:

```java
byte[] message = MintQuoteSignatureMessage.forQuote(quoteId, outputs);
byte[] hash = MessageDigest.getInstance("SHA-256").digest(message);
// sign hash with BIP-340 Schnorr using the key the quote is locked to
```

The message is hashed with SHA-256 before signing; `isValid` performs the same hash
before verification.

## Implementation notes

- Both classes are stateless and final with private constructors.
- A fresh `MessageDigest` is obtained per call, so the code is safe under virtual
  threads with no shared mutable state. See
  [virtual thread compatibility](../explanation/virtual-thread-compatibility.md).
- Ordering matters: outputs must be signed in the order they appear in the request,
  and verified the same way.

## Related

- [NUT-04](https://github.com/cashubtc/nuts/blob/main/04.md), which documents the
  bearer-token weakness this addresses
- [NUT-11 P2PK](cashu-lib-common.md), a different locking mechanism, applied to
  proofs rather than quotes
- [`cashu-lib-crypto`](cashu-lib-crypto.md) for the `Schnorr` primitives used here
