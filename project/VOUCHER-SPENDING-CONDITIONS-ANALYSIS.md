# Voucher Secret with Spending Conditions Analysis

**Question**: Can a voucher secret behave like an HTLC, P2PK, or RSS secret?

**Short Answer**: Yes - vouchers can have spending conditions in multiple ways.

---

## Table of Contents

1. [Overview](#overview)
2. [Option 1: Voucher WITH Spending Conditions](#option-1-voucher-with-spending-conditions)
3. [Option 2: P2PK/HTLC WITH Voucher Metadata](#option-2-p2pkhtlc-with-voucher-metadata)
4. [Option 3: Composite Secret Type](#option-3-composite-secret-type)
5. [Option 4: Voucher Metadata External](#option-4-voucher-metadata-external)
6. [Comparison Matrix](#comparison-matrix)
7. [Use Case Analysis](#use-case-analysis)
8. [Implementation Recommendations](#implementation-recommendations)
9. [Code Examples](#code-examples)

---

## Overview

### The Core Question

Vouchers have **issuer metadata** (who issued it, expiry, value, etc.).
Spending conditions have **spending rules** (who can spend it, when, how).

Can these be combined? **Yes, in several ways:**

1. **Voucher secret WITH spending condition tags**
2. **Spending condition secret WITH voucher metadata tags**
3. **New composite secret type combining both**
4. **Separate voucher metadata from secret entirely**

### Key Insight: Tags Are Extensible

The `WellKnownSecret` tag system is designed for exactly this kind of extension:

```java
public abstract class WellKnownSecret implements Secret {
    private Kind kind;
    private String nonce;
    private byte[] data;
    private List<Tag> tags;  // ← Extensible metadata system
}
```

**Any WellKnownSecret can have any tags** - there's no hard restriction.

---

## Option 1: Voucher WITH Spending Conditions

### Concept

Start with a `VoucherSecret` and add P2PK/HTLC tags to it.

### Implementation

```java
// Create voucher
VoucherSecret voucher = new VoucherSecret(voucherId);
voucher.setSchemaVersion(1);
voucher.setIssuerId("merchant123");
voucher.setIssuerPubKey(merchantKey.getPublicKey().toString());
voucher.setCurrency("USD");
voucher.setFaceValue(5000L);  // $50
voucher.setExpiresAt(expiryTimestamp);

// Sign voucher (issuer signs the voucher metadata)
byte[] voucherCanonical = voucher.getCanonicalBytesWithoutSignature();
Signature issuerSig = merchantKey.sign(voucherCanonical);
voucher.setIssuerSignature(issuerSig.toString());

// ADD P2PK SPENDING CONDITION
voucher.addTag(new Tag("p2pk_pubkeys", List.of(
    "02recipient1...",
    "03recipient2..."
)));
voucher.addTag(new Tag("p2pk_n_sigs", List.of(1)));  // 1-of-2 multisig
voucher.addTag(new Tag("p2pk_locktime", List.of(unlockTimestamp)));

// The voucher now requires:
// 1. Valid issuer signature (voucher validation)
// 2. Recipient signature to spend (P2PK validation)
```

### Validation Flow

**At mint time** (when user mints the voucher):
1. Validate voucher metadata (issuer signature, expiry, allowlist)
2. No P2PK validation yet (haven't tried to spend)
3. Mint signs and returns proof

**At spend time** (when user swaps/melts):
1. Validate voucher metadata again (might have expired since mint)
2. **Validate P2PK spending condition** (check recipient signature in witness)
3. Both must pass to spend

### Mint Validation Code

```java
public class VoucherSpendingCondition implements SpendingCondition<VoucherSecret> {

    @Override
    public void validate(Proof<VoucherSecret> proof, ValidationContext ctx)
            throws CashuErrorException {

        VoucherSecret secret = proof.getSecret();

        // 1. Validate voucher metadata
        validateVoucherMetadata(secret);

        // 2. Check if voucher has P2PK spending conditions
        if (hasP2PKTags(secret)) {
            validateP2PKSpendingCondition(secret, proof.getWitness());
        }

        // 3. Check if voucher has HTLC spending conditions
        if (hasHTLCTags(secret)) {
            validateHTLCSpendingCondition(secret, proof.getWitness());
        }
    }

    private void validateVoucherMetadata(VoucherSecret secret)
            throws CashuErrorException {
        // Standard voucher validation
        if (secret.getExpiresAt() < Instant.now().getEpochSecond()) {
            throw new CashuErrorException(VOUCHER_EXPIRED);
        }

        if (!verifyIssuerSignature(secret)) {
            throw new CashuErrorException(INVALID_ISSUER_SIGNATURE);
        }

        // ... other voucher checks
    }

    private boolean hasP2PKTags(VoucherSecret secret) {
        return secret.getTag("p2pk_pubkeys") != null;
    }

    private void validateP2PKSpendingCondition(VoucherSecret secret, Witness witness)
            throws CashuErrorException {
        // Reuse P2PK validation logic
        P2PKSpendingCondition p2pkValidator = new P2PKSpendingCondition();

        // Extract P2PK tags from voucher
        Tag pubkeysTag = secret.getTag("p2pk_pubkeys");
        Tag nSigsTag = secret.getTag("p2pk_n_sigs");
        Tag locktimeTag = secret.getTag("p2pk_locktime");

        // Validate recipient signatures
        if (!p2pkValidator.validateSignatures(pubkeysTag, nSigsTag, witness)) {
            throw new CashuErrorException(INVALID_P2PK_WITNESS);
        }

        // Validate locktime
        if (!p2pkValidator.validateLocktime(locktimeTag)) {
            throw new CashuErrorException(LOCKTIME_NOT_REACHED);
        }
    }
}
```

### Pros

✅ **Flexible**: Add any spending condition to any voucher
✅ **Composable**: Combine multiple spending conditions
✅ **No new classes**: Reuses existing `VoucherSecret`
✅ **Clear semantics**: "This is a voucher that ALSO has spending rules"
✅ **Backward compatible**: Vouchers without spending conditions still work

### Cons

⚠️ **Complex validation**: Mint must check multiple rule types
⚠️ **Tag namespace pollution**: P2PK tags mixed with voucher tags
⚠️ **Potential conflicts**: What if P2PK locktime conflicts with voucher expiry?
⚠️ **Signature scope unclear**: Does issuer signature cover P2PK tags?

### When to Use

Use this when:
- Merchant wants to issue locked vouchers
- Gift card should require recipient authentication
- Voucher should only be redeemable after a certain time
- You want maximum flexibility with minimal code changes

---

## Option 2: P2PK/HTLC WITH Voucher Metadata

### Concept

Start with a `P2PKSecret` or `HTLCSecret` and add voucher metadata tags.

### Implementation

```java
// Create P2PK secret (primary purpose: recipient locking)
P2PKSecret p2pk = new P2PKSecret(recipientPubKey);
p2pk.addTag(new Tag("n_sigs", List.of(1)));
p2pk.addTag(new Tag("locktime", List.of(unlockTimestamp)));

// ADD VOUCHER METADATA
p2pk.addTag(new Tag("voucher_issuer", List.of("merchant123")));
p2pk.addTag(new Tag("voucher_issuer_pubkey", List.of(merchantPubKey)));
p2pk.addTag(new Tag("voucher_currency", List.of("USD")));
p2pk.addTag(new Tag("voucher_face_value", List.of(5000L)));
p2pk.addTag(new Tag("voucher_expiry", List.of(expiryTimestamp)));

// Sign the entire secret (including voucher metadata)
byte[] canonical = p2pk.getCanonicalBytesWithoutSignature();
Signature issuerSig = merchantKey.sign(canonical);
p2pk.addTag(new Tag("voucher_issuer_sig", List.of(issuerSig.toString())));

// This P2PK secret now carries voucher information
```

### Validation Flow

**At spend time**:
1. **Primary validation**: P2PK spending condition (recipient signature required)
2. **Secondary validation**: Voucher metadata (if mint is configured to check)
3. P2PK is the primary mechanism, voucher is informational

### Mint Validation Code

```java
public class P2PKSpendingCondition implements SpendingCondition<P2PKSecret> {

    @Override
    public void validate(Proof<P2PKSecret> proof, ValidationContext ctx)
            throws CashuErrorException {

        P2PKSecret secret = proof.getSecret();

        // 1. PRIMARY: Validate P2PK spending condition
        validateP2PKSignatures(secret, proof.getWitness());
        validateP2PKLocktime(secret);

        // 2. OPTIONAL: Validate voucher metadata if present
        if (hasVoucherMetadata(secret)) {
            validateVoucherMetadata(secret, ctx);
        }
    }

    private boolean hasVoucherMetadata(P2PKSecret secret) {
        return secret.getTag("voucher_issuer") != null;
    }

    private void validateVoucherMetadata(P2PKSecret secret, ValidationContext ctx)
            throws CashuErrorException {

        // Extract voucher tags
        Tag issuerTag = secret.getTag("voucher_issuer");
        Tag expiryTag = secret.getTag("voucher_expiry");
        Tag issuerSigTag = secret.getTag("voucher_issuer_sig");

        String issuerId = String.valueOf(issuerTag.getValues().get(0));
        Long expiry = ((Number) expiryTag.getValues().get(0)).longValue();
        String issuerSig = String.valueOf(issuerSigTag.getValues().get(0));

        // Validate issuer is allowed
        if (!ctx.getAllowedIssuers().contains(issuerId)) {
            throw new CashuErrorException(ISSUER_NOT_ALLOWED);
        }

        // Validate expiry
        if (expiry < Instant.now().getEpochSecond()) {
            throw new CashuErrorException(VOUCHER_EXPIRED);
        }

        // Validate issuer signature
        if (!verifyIssuerSignature(secret, issuerSig)) {
            throw new CashuErrorException(INVALID_ISSUER_SIGNATURE);
        }
    }
}
```

### Pros

✅ **Clear primary mechanism**: P2PK is the main spending condition
✅ **Optional voucher validation**: Mint can ignore voucher metadata if desired
✅ **Reuses existing code**: P2PKSecret already exists
✅ **Natural fit**: P2PK is designed for recipient locking (gift card use case)

### Cons

⚠️ **Kind mismatch**: Secret kind is "P2PK", not "VOUCHER" (confusing?)
⚠️ **Secondary validation**: Voucher metadata is optional, not enforced
⚠️ **Discovery**: Hard to query for "all vouchers" if they're P2PK secrets
⚠️ **Signature scope**: Issuer signs entire P2PK secret (including P2PK tags)

### When to Use

Use this when:
- Primary goal is recipient locking (P2PK)
- Voucher metadata is informational, not strictly enforced
- You want voucher-like UX on top of existing P2PK functionality
- Merchant wants to track issued gift cards but enforcement is secondary

---

## Option 3: Composite Secret Type

### Concept

Create a new secret type that explicitly combines voucher and spending conditions.

### Implementation

```java
public class CompositeSecret extends WellKnownSecret {

    // Define composite kinds
    public enum CompositeKind {
        VOUCHER_P2PK,    // Voucher + P2PK
        VOUCHER_HTLC,    // Voucher + HTLC
        P2PK_HTLC,       // P2PK + HTLC (atomic swap with P2PK lock)
        VOUCHER_P2PK_HTLC // All three!
    }

    private CompositeKind compositeKind;

    // Components
    private VoucherMetadata voucherMetadata;
    private P2PKMetadata p2pkMetadata;
    private HTLCMetadata htlcMetadata;

    public CompositeSecret(CompositeKind kind) {
        super(Kind.COMPOSITE);  // New Kind enum value
        this.compositeKind = kind;
    }

    @Override
    public byte[] toBytes() {
        // Serialize all components for hash-to-curve
        return serializeComponents();
    }

    private byte[] serializeComponents() {
        JsonObject json = new JsonObject();
        json.put("compositeKind", compositeKind.name());

        if (voucherMetadata != null) {
            json.put("voucher", voucherMetadata.toJson());
        }

        if (p2pkMetadata != null) {
            json.put("p2pk", p2pkMetadata.toJson());
        }

        if (htlcMetadata != null) {
            json.put("htlc", htlcMetadata.toJson());
        }

        return json.toString().getBytes(StandardCharsets.UTF_8);
    }
}

// Usage
CompositeSecret composite = new CompositeSecret(CompositeKind.VOUCHER_P2PK);

// Set voucher component
VoucherMetadata voucher = VoucherMetadata.builder()
    .issuerId("merchant123")
    .currency("USD")
    .faceValue(5000L)
    .expiresAt(expiryTimestamp)
    .build();
composite.setVoucherMetadata(voucher);

// Set P2PK component
P2PKMetadata p2pk = P2PKMetadata.builder()
    .pubkeys(List.of(recipientPubKey))
    .nSigs(1)
    .locktime(unlockTimestamp)
    .build();
composite.setP2PKMetadata(p2pk);

// Sign both components
byte[] canonical = composite.getCanonicalBytesWithoutSignatures();
composite.setIssuerSignature(merchantKey.sign(canonical));
```

### Validation Flow

**At spend time**:
1. Deserialize composite secret
2. Validate each component based on `compositeKind`
3. All components must pass

```java
public class CompositeSpendingCondition implements SpendingCondition<CompositeSecret> {

    @Override
    public void validate(Proof<CompositeSecret> proof, ValidationContext ctx)
            throws CashuErrorException {

        CompositeSecret secret = proof.getSecret();

        switch (secret.getCompositeKind()) {
            case VOUCHER_P2PK -> {
                validateVoucher(secret.getVoucherMetadata(), ctx);
                validateP2PK(secret.getP2PKMetadata(), proof.getWitness());
            }
            case VOUCHER_HTLC -> {
                validateVoucher(secret.getVoucherMetadata(), ctx);
                validateHTLC(secret.getHTLCMetadata(), proof.getWitness());
            }
            case VOUCHER_P2PK_HTLC -> {
                validateVoucher(secret.getVoucherMetadata(), ctx);
                validateP2PK(secret.getP2PKMetadata(), proof.getWitness());
                validateHTLC(secret.getHTLCMetadata(), proof.getWitness());
            }
        }
    }
}
```

### Pros

✅ **Explicit design**: Clear intent to combine multiple mechanisms
✅ **Type safety**: Strongly typed components
✅ **Queryable**: Easy to find all composite secrets of a certain kind
✅ **Well-structured**: Clean separation of concerns
✅ **Extensible**: Easy to add new combinations

### Cons

⚠️ **New abstraction**: Requires new classes and types
⚠️ **Complexity**: More code to maintain
⚠️ **Serialization overhead**: More complex JSON structure
⚠️ **Migration**: Existing code needs updates
⚠️ **Over-engineering?**: May be overkill for simple use cases

### When to Use

Use this when:
- You expect many composite secret types
- Type safety and explicit modeling are important
- You have complex validation requirements
- You want a clean, maintainable architecture for the long term

---

## Option 4: Voucher Metadata External

### Concept

Keep voucher metadata completely separate from the secret itself. Store it in wallet database or external systems.

### Implementation

```java
// Proof has simple secret (any type)
Proof<RandomStringSecret> proof = new Proof<>();
proof.setSecret(new RandomStringSecret());
proof.setAmount(5000);
proof.setKeySetId(keysetId);
proof.setUnblindedSignature(signature);

// Voucher metadata stored separately in wallet
VoucherMetadata voucher = VoucherMetadata.builder()
    .proofId(proof.getId())  // Links to proof
    .issuerId("merchant123")
    .currency("USD")
    .faceValue(5000L)
    .expiresAt(expiryTimestamp)
    .issuerSignature(issuerSig)
    .build();

// Store in wallet database
walletDb.storeProof(proof);
walletDb.storeVoucherMetadata(voucher);

// When displaying in UI
VoucherMetadata metadata = walletDb.getVoucherMetadata(proof.getId());
if (metadata != null) {
    // Display as voucher with metadata
    displayVoucher(proof, metadata);
} else {
    // Display as regular proof
    displayProof(proof);
}
```

### Mint Validation

**Mint doesn't see voucher metadata at all** - it's purely client-side.

Alternatively, pass metadata in a separate field:

```java
// Wallet sends voucher metadata alongside proof
public class PostSwapRequest<T extends Secret> {
    private List<Proof<T>> inputs;
    private List<BlindedMessage> outputs;
    private List<VoucherMetadata> voucherMetadata;  // NEW: Optional metadata
}

// Mint validates if present
if (request.getVoucherMetadata() != null) {
    for (VoucherMetadata voucher : request.getVoucherMetadata()) {
        validateVoucherMetadata(voucher);
    }
}
```

### Pros

✅ **Simplest**: No changes to Secret types
✅ **Flexible**: Any secret can have voucher metadata
✅ **Optional**: Mint can choose whether to validate
✅ **Backward compatible**: Existing secrets unchanged
✅ **Database-friendly**: Natural storage model

### Cons

⚠️ **Not cryptographically bound**: Metadata can be changed without affecting proof
⚠️ **Synchronization**: Must keep metadata and proofs in sync
⚠️ **Privacy**: If sent to mint separately, mint learns correlation
⚠️ **Recovery**: Voucher metadata not recovered from mnemonic

### When to Use

Use this when:
- Voucher metadata is purely informational (UX, not validation)
- You don't need cryptographic binding of metadata to proof
- You want maximum flexibility with minimal code changes
- Mint doesn't need to enforce voucher rules strictly

---

## Comparison Matrix

| Aspect | Option 1: Voucher+Conditions | Option 2: P2PK+Voucher | Option 3: Composite | Option 4: External Metadata |
|--------|----------------------------|----------------------|-------------------|---------------------------|
| **Complexity** | Medium | Low | High | Very Low |
| **Type Safety** | Medium | Low | High | Low |
| **Cryptographic Binding** | ✅ Yes | ✅ Yes | ✅ Yes | ❌ No |
| **Code Changes** | ~50 lines | ~30 lines | ~200 lines | ~20 lines |
| **New Classes** | 0 | 0 | 3-5 | 0-1 |
| **Backward Compatible** | ✅ Yes | ✅ Yes | ⚠️ Partial | ✅ Yes |
| **Queryability** | Medium | Low | High | High |
| **Validation Complexity** | Medium | Medium | High | Low |
| **Mint Enforcement** | Strong | Optional | Strong | Optional/None |
| **Primary Use Case** | Locked vouchers | Gift cards with recipient lock | Complex multi-condition tokens | Informational vouchers |
| **Recovery from Mnemonic** | ❌ No (voucher metadata) | ❌ No | ❌ No | ❌ No |
| **Suitable for Production** | ✅ Yes | ✅ Yes | ⚠️ Maybe (if needed) | ✅ Yes (for simple cases) |

---

## Use Case Analysis

### Use Case 1: Simple Gift Card (No Spending Conditions)

**Requirement**: Merchant issues $50 gift card, anyone with the proof can redeem it.

**Best Option**: Standard `VoucherSecret` (no spending conditions)

```java
VoucherSecret voucher = new VoucherSecret(voucherId);
voucher.setIssuerId("merchant123");
voucher.setFaceValue(5000L);
voucher.setExpiresAt(expiryTimestamp);
// Sign and mint
```

**Why**: No spending conditions needed, voucher metadata is sufficient.

---

### Use Case 2: Locked Gift Card (Recipient Must Sign)

**Requirement**: Merchant issues gift card that can only be redeemed by specific recipient.

**Best Option**: **Option 1** (Voucher + P2PK tags) OR **Option 2** (P2PK + voucher tags)

**Option 1 (Recommended if voucher is primary)**:
```java
VoucherSecret voucher = new VoucherSecret(voucherId);
voucher.setIssuerId("merchant123");
voucher.setFaceValue(5000L);
// Add P2PK lock
voucher.addTag(new Tag("p2pk_pubkeys", List.of(recipientPubKey)));
```

**Option 2 (Recommended if P2PK is primary)**:
```java
P2PKSecret p2pk = new P2PKSecret(recipientPubKey);
// Add voucher metadata
p2pk.addTag(new Tag("voucher_issuer", List.of("merchant123")));
p2pk.addTag(new Tag("voucher_face_value", List.of(5000L)));
```

**Why**: Both work, choose based on which is conceptually primary (voucher vs. recipient lock).

---

### Use Case 3: HTLC Gift Card (Requires Preimage)

**Requirement**: Gift card that requires revealing a secret preimage to redeem (e.g., password, promo code).

**Best Option**: **Option 1** (Voucher + HTLC tags)

```java
VoucherSecret voucher = new VoucherSecret(voucherId);
voucher.setIssuerId("merchant123");
voucher.setFaceValue(5000L);
// Add HTLC lock
voucher.addTag(new Tag("htlc_hash", List.of(sha256(preimage))));
voucher.addTag(new Tag("htlc_locktime", List.of(refundTimestamp)));
```

**Why**: Voucher is the primary concept, HTLC adds spending condition.

---

### Use Case 4: Multi-Sig Voucher (Requires 2-of-3 Signatures)

**Requirement**: Corporate gift card that requires approval from 2 of 3 corporate officers to redeem.

**Best Option**: **Option 1** (Voucher + P2PK multisig tags)

```java
VoucherSecret voucher = new VoucherSecret(voucherId);
voucher.setIssuerId("corporate_treasury");
voucher.setFaceValue(100000L);  // $1,000
// Add multisig P2PK lock
voucher.addTag(new Tag("p2pk_pubkeys", List.of(
    officer1PubKey,
    officer2PubKey,
    officer3PubKey
)));
voucher.addTag(new Tag("p2pk_n_sigs", List.of(2)));  // 2-of-3
```

**Why**: Complex spending condition naturally expressed as tags.

---

### Use Case 5: Atomic Swap Gift Card

**Requirement**: Gift card that can be atomically swapped for another asset using HTLC.

**Best Option**: **Option 3** (Composite secret with VOUCHER_P2PK_HTLC) OR **Option 1** with both P2PK and HTLC tags

**Option 3**:
```java
CompositeSecret composite = new CompositeSecret(CompositeKind.VOUCHER_P2PK_HTLC);
composite.setVoucherMetadata(...);
composite.setP2PKMetadata(...);
composite.setHTLCMetadata(...);
```

**Option 1**:
```java
VoucherSecret voucher = new VoucherSecret(voucherId);
// Add voucher metadata
// Add P2PK tags
// Add HTLC tags
```

**Why**: Highly complex, composite type provides better structure (but Option 1 still works).

---

### Use Case 6: Informational Voucher (No Enforcement)

**Requirement**: Display gift card info in wallet UI, but mint doesn't enforce voucher rules.

**Best Option**: **Option 4** (External metadata)

```java
// Proof is just a regular secret
Proof<RandomStringSecret> proof = mintRegularProof();

// Voucher metadata stored separately
VoucherMetadata metadata = VoucherMetadata.builder()
    .proofId(proof.getId())
    .issuerId("merchant123")
    .displayName("Coffee Shop Gift Card")
    .faceValue(5000L)
    .build();

walletDb.storeVoucherMetadata(metadata);

// UI shows voucher info, but mint doesn't care
```

**Why**: Simplest, no mint changes needed, purely UX enhancement.

---

## Implementation Recommendations

### Recommended Approach for Most Use Cases

**Use Option 1: Voucher WITH Spending Condition Tags**

**Rationale**:
1. ✅ Flexible: Supports all spending conditions (P2PK, HTLC, custom)
2. ✅ Minimal code: Reuses existing `VoucherSecret` class
3. ✅ Extensible: Easy to add new spending condition types
4. ✅ Clear semantics: "This is a voucher (with optional spending rules)"
5. ✅ Backward compatible: Vouchers without spending conditions still work

**Implementation Steps**:

1. **Define standard tag names for spending conditions**:
```java
public interface SpendingConditionTags {
    // P2PK tags
    String P2PK_PUBKEYS = "p2pk_pubkeys";
    String P2PK_N_SIGS = "p2pk_n_sigs";
    String P2PK_LOCKTIME = "p2pk_locktime";
    String P2PK_REFUND = "p2pk_refund";

    // HTLC tags
    String HTLC_HASH = "htlc_hash";
    String HTLC_LOCKTIME = "htlc_locktime";
    String HTLC_REFUND_PUBKEY = "htlc_refund_pubkey";

    // Custom spending conditions
    String CUSTOM_CONDITION_TYPE = "custom_condition_type";
    String CUSTOM_CONDITION_DATA = "custom_condition_data";
}
```

2. **Extend VoucherSpendingCondition to check for spending condition tags**:
```java
public class VoucherSpendingCondition implements SpendingCondition<VoucherSecret> {

    private final P2PKSpendingCondition p2pkValidator = new P2PKSpendingCondition();
    private final HTLCSpendingCondition htlcValidator = new HTLCSpendingCondition();

    @Override
    public void validate(Proof<VoucherSecret> proof, ValidationContext ctx)
            throws CashuErrorException {

        VoucherSecret secret = proof.getSecret();

        // 1. Always validate voucher metadata
        validateVoucherMetadata(secret, ctx);

        // 2. Check for P2PK spending conditions
        if (hasP2PKTags(secret)) {
            validateP2PKCondition(secret, proof.getWitness());
        }

        // 3. Check for HTLC spending conditions
        if (hasHTLCTags(secret)) {
            validateHTLCCondition(secret, proof.getWitness());
        }

        // 4. Check for custom spending conditions
        if (hasCustomConditionTags(secret)) {
            validateCustomCondition(secret, proof.getWitness());
        }
    }

    private boolean hasP2PKTags(VoucherSecret secret) {
        return secret.getTag(SpendingConditionTags.P2PK_PUBKEYS) != null;
    }

    private void validateP2PKCondition(VoucherSecret secret, Witness witness)
            throws CashuErrorException {
        // Extract P2PK tags and validate using existing P2PK validator
        // ...
    }
}
```

3. **Helper methods for adding spending conditions to vouchers**:
```java
public class VoucherSecretBuilder {

    private VoucherSecret voucher;

    public VoucherSecretBuilder() {
        this.voucher = new VoucherSecret();
    }

    // Voucher metadata methods
    public VoucherSecretBuilder issuerId(String id) {
        voucher.setIssuerId(id);
        return this;
    }

    public VoucherSecretBuilder faceValue(long value) {
        voucher.setFaceValue(value);
        return this;
    }

    // ... other voucher methods

    // P2PK spending condition methods
    public VoucherSecretBuilder p2pkLock(String... pubkeys) {
        voucher.addTag(new Tag(
            SpendingConditionTags.P2PK_PUBKEYS,
            Arrays.asList(pubkeys)
        ));
        return this;
    }

    public VoucherSecretBuilder p2pkMultisig(int nSigs, String... pubkeys) {
        voucher.addTag(new Tag(
            SpendingConditionTags.P2PK_PUBKEYS,
            Arrays.asList(pubkeys)
        ));
        voucher.addTag(new Tag(
            SpendingConditionTags.P2PK_N_SIGS,
            List.of(nSigs)
        ));
        return this;
    }

    public VoucherSecretBuilder p2pkLocktime(long locktime) {
        voucher.addTag(new Tag(
            SpendingConditionTags.P2PK_LOCKTIME,
            List.of(locktime)
        ));
        return this;
    }

    // HTLC spending condition methods
    public VoucherSecretBuilder htlcLock(byte[] preimageHash) {
        voucher.addTag(new Tag(
            SpendingConditionTags.HTLC_HASH,
            List.of(Hex.toHexString(preimageHash))
        ));
        return this;
    }

    public VoucherSecretBuilder htlcLocktime(long locktime) {
        voucher.addTag(new Tag(
            SpendingConditionTags.HTLC_LOCKTIME,
            List.of(locktime)
        ));
        return this;
    }

    public VoucherSecret build(PrivateKey issuerKey) {
        // Sign voucher
        byte[] canonical = voucher.getCanonicalBytesWithoutSignature();
        Signature sig = issuerKey.sign(canonical);
        voucher.setIssuerSignature(sig.toString());

        return voucher;
    }
}
```

4. **Usage example**:
```java
// Create locked gift card
VoucherSecret lockedVoucher = new VoucherSecretBuilder()
    .voucherId(UUID.randomUUID().toString().getBytes())
    .issuerId("coffeeshop123")
    .currency("USD")
    .faceValue(5000L)
    .expiresAt(expiryTimestamp)
    .p2pkLock(recipientPubKey)  // Add P2PK lock
    .build(merchantSigningKey);

// Create HTLC gift card
VoucherSecret htlcVoucher = new VoucherSecretBuilder()
    .voucherId(UUID.randomUUID().toString().getBytes())
    .issuerId("coffeeshop123")
    .faceValue(5000L)
    .htlcLock(sha256(preimage))  // Requires preimage
    .htlcLocktime(refundTimestamp)
    .build(merchantSigningKey);

// Create multisig corporate voucher
VoucherSecret multisigVoucher = new VoucherSecretBuilder()
    .voucherId(UUID.randomUUID().toString().getBytes())
    .issuerId("corporate_treasury")
    .faceValue(100000L)
    .p2pkMultisig(2, officer1, officer2, officer3)  // 2-of-3 multisig
    .build(corporateKey);
```

---

### When to Use Other Options

**Use Option 2** (P2PK + Voucher tags) when:
- P2PK is the primary mechanism (recipient locking is main goal)
- Voucher metadata is secondary/informational
- You want to leverage existing P2PK infrastructure

**Use Option 3** (Composite) when:
- You have many complex multi-condition use cases
- Type safety and explicit modeling are critical
- You're building a long-term system with many secret types
- You want clear separation between different rule types

**Use Option 4** (External metadata) when:
- Voucher metadata is purely for UX (not cryptographically important)
- You don't need mint enforcement of voucher rules
- You want maximum simplicity with minimal code changes
- Wallet-side display is the only goal

---

## Code Examples

### Complete Example: Locked Gift Card with Expiry

```java
public class LockedGiftCardExample {

    public static void main(String[] args) throws Exception {

        // Setup
        PrivateKey merchantKey = PrivateKey.generate();
        PrivateKey recipientKey = PrivateKey.generate();
        String recipientPubKey = recipientKey.getPublicKey().toString();

        // Merchant creates locked gift card
        VoucherSecret voucher = new VoucherSecretBuilder()
            .voucherId(UUID.randomUUID().toString().getBytes())
            .issuerId("coffeeshop123")
            .issuerPubKey(merchantKey.getPublicKey().toString())
            .currency("USD")
            .faceValue(5000L)  // $50.00
            .issuedAt(Instant.now().getEpochSecond())
            .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond())
            .p2pkLock(recipientPubKey)  // Only recipient can redeem
            .build(merchantKey);

        System.out.println("Gift card created:");
        System.out.println("  Issuer: " + voucher.getIssuerId());
        System.out.println("  Value: $" + voucher.getFaceValue() / 100.0);
        System.out.println("  Expires: " + Instant.ofEpochSecond(voucher.getExpiresAt()));
        System.out.println("  Locked to: " +
            voucher.getTag("p2pk_pubkeys").getValues().get(0));

        // Merchant mints the voucher
        Proof<VoucherSecret> voucherProof = mintVoucher(voucher);

        // Give proof to recipient
        String tokenString = Token.serialize(List.of(voucherProof));
        System.out.println("\nGift card token (give to recipient):");
        System.out.println(tokenString);

        // Recipient receives gift card
        Token receivedToken = Token.deserialize(tokenString);
        Proof<VoucherSecret> receivedProof =
            (Proof<VoucherSecret>) receivedToken.getEntries().get(0).getProofs().get(0);

        VoucherSecret receivedVoucher = receivedProof.getSecret();
        System.out.println("\nRecipient received gift card:");
        System.out.println("  Value: $" + receivedVoucher.getFaceValue() / 100.0);
        System.out.println("  Can redeem: " +
            canRecipientRedeem(receivedVoucher, recipientKey));

        // Recipient redeems gift card
        Witness witness = createP2PKWitness(receivedProof, recipientKey);
        receivedProof.setWitness(witness);

        // Swap/melt at mint
        PostSwapRequest<VoucherSecret> swapRequest = new PostSwapRequest<>();
        swapRequest.setInputs(List.of(receivedProof));
        // ... add outputs

        PostSwapResponse response = mint.swap(swapRequest);
        System.out.println("\nGift card redeemed successfully!");
    }

    private static boolean canRecipientRedeem(VoucherSecret voucher, PrivateKey recipientKey) {
        Tag pubkeysTag = voucher.getTag("p2pk_pubkeys");
        if (pubkeysTag == null) {
            return true;  // No P2PK lock
        }

        String requiredPubKey = String.valueOf(pubkeysTag.getValues().get(0));
        String recipientPubKey = recipientKey.getPublicKey().toString();

        return requiredPubKey.equals(recipientPubKey);
    }

    private static Witness createP2PKWitness(Proof<VoucherSecret> proof, PrivateKey key) {
        // Create P2PK witness (sign proof)
        byte[] proofBytes = proof.getSecret().toBytes();
        Signature sig = key.sign(proofBytes);

        Witness witness = new Witness();
        witness.setSignatures(List.of(sig.toString()));
        return witness;
    }

    private static Proof<VoucherSecret> mintVoucher(VoucherSecret voucher) {
        // Simplified minting (actual implementation would use BDHKE)
        // ...
        return null;
    }
}
```

### Complete Example: HTLC Atomic Swap Voucher

```java
public class HTLCVoucherExample {

    public static void main(String[] args) throws Exception {

        // Setup
        PrivateKey merchantKey = PrivateKey.generate();
        byte[] preimage = generateRandomBytes(32);
        byte[] preimageHash = sha256(preimage);

        // Merchant creates HTLC-locked voucher
        // (Can be atomically swapped for another asset)
        VoucherSecret htlcVoucher = new VoucherSecretBuilder()
            .voucherId(UUID.randomUUID().toString().getBytes())
            .issuerId("merchant123")
            .issuerPubKey(merchantKey.getPublicKey().toString())
            .currency("USD")
            .faceValue(10000L)  // $100.00
            .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS).getEpochSecond())
            .htlcLock(preimageHash)  // Requires revealing preimage
            .htlcLocktime(Instant.now().plus(12, ChronoUnit.HOURS).getEpochSecond())
            .build(merchantKey);

        System.out.println("HTLC voucher created:");
        System.out.println("  Value: $" + htlcVoucher.getFaceValue() / 100.0);
        System.out.println("  Preimage hash: " +
            htlcVoucher.getTag("htlc_hash").getValues().get(0));

        // Mint the voucher
        Proof<VoucherSecret> voucherProof = mintVoucher(htlcVoucher);

        // Atomic swap scenario:
        // Alice has HTLC voucher, Bob has BTC
        // They can atomically swap using the same preimage

        // Bob reveals preimage to claim voucher
        Witness htlcWitness = createHTLCWitness(preimage);
        voucherProof.setWitness(htlcWitness);

        // Bob swaps voucher at mint
        PostSwapRequest<VoucherSecret> swapRequest = new PostSwapRequest<>();
        swapRequest.setInputs(List.of(voucherProof));
        // ... outputs

        // Mint validates:
        // 1. Voucher not expired
        // 2. Issuer signature valid
        // 3. Preimage hashes to htlc_hash
        // 4. Current time > htlc_locktime OR valid preimage

        PostSwapResponse response = mint.swap(swapRequest);
        System.out.println("\nAtomic swap completed!");

        // Alice sees preimage revelation on-chain and can claim Bob's BTC
    }

    private static Witness createHTLCWitness(byte[] preimage) {
        Witness witness = new Witness();
        witness.setPreimages(List.of(Hex.toHexString(preimage)));
        return witness;
    }

    private static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
```

---

## Conclusion

**Yes, voucher secrets can behave like HTLC, P2PK, or RSS secrets** by adding spending condition tags to the voucher.

### Recommended Approach

**Use Option 1: Add spending condition tags to VoucherSecret**

This provides:
- ✅ Maximum flexibility
- ✅ Minimal code changes
- ✅ Clear semantics
- ✅ Backward compatibility
- ✅ Support for all spending condition types

### Implementation Priority

1. **Phase 1**: Implement basic `VoucherSecret` (no spending conditions)
2. **Phase 2**: Add P2PK spending condition support to vouchers
3. **Phase 3**: Add HTLC spending condition support to vouchers
4. **Phase 4** (Optional): Consider Option 3 (Composite) if complexity grows

### Key Design Principle

**Vouchers are about WHAT (metadata), spending conditions are about WHO/WHEN/HOW (enforcement).**

These are orthogonal concerns that can be combined through the extensible tag system that `WellKnownSecret` already provides.

---

**Document Version**: 1.0
**Last Updated**: 2025-11-03
**Status**: Final
