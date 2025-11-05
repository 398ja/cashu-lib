# Voucher Redemption Process and Nostr Backup Strategy

**Last Updated**: 2025-11-03 (Updated with regulatory analysis)

---

## Table of Contents

1. [Critical Understanding: What Redemption Means](#critical-understanding-what-redemption-means)
2. [Voucher Business Models](#voucher-business-models)
3. [Voucher Redemption Process](#voucher-redemption-process)
4. [User Experience Flows](#user-experience-flows)
5. [Nostr Backup Strategy](#nostr-backup-strategy)
6. [Implementation Examples](#implementation-examples)
7. [Security Considerations](#security-considerations)
8. [Regulatory Considerations](#regulatory-considerations)
9. [Best Practices](#best-practices)

---

## Critical Understanding: What Redemption Means

### The Key Question: Can I Still Use the Voucher at the Merchant After Redemption?

**Answer: NO** - Once you redeem (convert to ecash), the voucher becomes **spent** and cannot be used at the merchant anymore.

### Before Redemption

```
┌─────────────────────────────────────┐
│ Merchant A Voucher                  │
│ Value: £10.00                       │
│                                     │
│ • ✅ Valid at Merchant A            │
│ • ⚠️ Has expiry date                │
│ • ⚠️ NOT recoverable from seed      │
│ • ✅ Contains issuer metadata       │
└─────────────────────────────────────┘
```

### After Redemption (Swap to E-cash)

```
┌─────────────────────────────────────┐
│ Regular E-cash                      │
│ Value: £10.00                       │
│                                     │
│ • ✅ Spendable anywhere*            │
│ • ✅ No expiry date                 │
│ • ✅ Recoverable from seed (if det.)│
│ • ⚠️ No issuer metadata             │
│ • ❌ NOT usable at Merchant A       │
└─────────────────────────────────────┘

* Subject to merchant accepting this mint's ecash
```

### The Trade-off

| Aspect | Voucher (Before) | E-cash (After) |
|--------|------------------|----------------|
| **Use at issuer** | ✅ Yes | ⚠️ Maybe (if they accept ecash) |
| **Use elsewhere** | ❌ No | ✅ Yes (if mint trusted) |
| **Expiry** | ⚠️ Yes | ✅ None |
| **Recoverable** | ❌ No | ✅ Yes (if deterministic) |
| **Special benefits** | ✅ Possible | ❌ None |

**Think of redemption like cashing a gift card** - you get cash that works everywhere, but you can't use that specific gift card anymore.

---

## Voucher Business Models

### Model A: Integrated Merchant-Mint (Recommended for Redemption)

**Structure**: Single entity operates both merchant and mint.

```
┌─────────────────────────────────────┐
│  CoffeeShop Ltd (Single Entity)     │
│                                     │
│  ├─ Retail Operations               │
│  │   ├─ Sells coffee                │
│  │   └─ Issues vouchers             │
│  │                                  │
│  └─ Cashu Mint Operations           │
│      ├─ Issues ecash                │
│      └─ Accepts own vouchers        │
└─────────────────────────────────────┘

When user redeems voucher:
├─ Internal accounting only
├─ Move reserves: voucher pool → ecash pool
└─ No external settlement needed
```

**Characteristics**:
- ✅ **No trust issues**: Same entity backs both
- ✅ **Simple accounting**: Internal transfers
- ✅ **Clear regulatory path**: Single jurisdiction
- ✅ **No counterparty risk**: Can't defraud yourself
- ✅ **Voucher redemption allowed**: Internal operation

**Regulatory Implications**:
- Single entity with multiple business lines
- Money transmitter license needed for ecash operations
- Voucher-to-ecash conversion is internal (not separately regulated)
- May qualify for gift card exemptions in some jurisdictions

**Implementation**:
```java
class IntegratedMerchantMint {

    private BigDecimal voucherReserves;  // £10,000 vouchers outstanding
    private BigDecimal ecashReserves;    // £15,000 ecash outstanding

    void redeemVoucher(VoucherSecret voucher) {
        // Validate it's our own voucher
        if (!voucher.getIssuerId().equals(OUR_ENTITY_ID)) {
            throw new CashuErrorException("External vouchers not accepted");
        }

        // Mark Cashu proof as spent
        proofVault.markSpent(voucherProof);

        // Internal accounting: move reserves
        voucherReserves = voucherReserves.subtract(voucher.getValue());
        ecashReserves = ecashReserves.add(voucher.getValue());

        // Issue new ecash from our reserves
        issueEcash(voucher.getValue());
    }
}
```

---

### Model B: No Redemption - Direct Payment Only (Recommended for Simplicity)

**Structure**: Vouchers can ONLY be spent at the issuing merchant, never converted to ecash.

```
┌─────────────────────────────────────┐
│  Voucher Lifecycle                  │
│                                     │
│  Issue → Hold → Spend at Merchant   │
│                    ↓                │
│                  DONE               │
│                                     │
│  (Mint never involved in redemption)│
└─────────────────────────────────────┘
```

**Characteristics**:
- ✅ **No mint trust required**: Mint never handles redemption
- ✅ **Minimal regulation**: Traditional gift card treatment
- ✅ **Simple model**: Clear merchant-customer relationship
- ✅ **No solvency concerns**: Merchant backs own vouchers
- ⚠️ **Limited liquidity**: Can't convert to ecash
- ⚠️ **Tied to merchant**: Only usable at one place

**Regulatory Implications**:
- Traditional gift card issuer (retailer)
- Consumer protection laws apply
- NO money transmission (critical!)
- NO payment services regulation
- Clearest legal framework
- Minimal compliance burden

**Implementation**:
```java
// Mint configuration: Reject vouchers in swap
class VoucherOnlyMint extends Mint {

    @Override
    public PostSwapResponse swap(PostSwapRequest request) {

        for (Proof proof : request.getInputs()) {
            if (proof.getSecret() instanceof VoucherSecret voucher) {
                throw new CashuErrorException(
                    VOUCHER_NOT_REDEEMABLE,
                    "This voucher can only be spent at " +
                    voucher.getIssuerId() + ". " +
                    "Cannot be converted to ecash."
                );
            }
        }

        return super.swap(request);
    }
}

// Merchant accepts vouchers for payment
class Merchant {

    public Receipt acceptVoucherPayment(Proof<VoucherSecret> voucherProof) {

        VoucherSecret voucher = voucherProof.getSecret();

        // Only accept OUR vouchers
        if (!voucher.getIssuerId().equals(OUR_ISSUER_ID)) {
            throw new PaymentRejectedException("Not our voucher");
        }

        // Validate signature, expiry, Cashu proof
        validateVoucher(voucher);
        validateProof(voucherProof);

        // Mark Cashu proof as spent
        markProofSpent(voucherProof);

        // Process payment
        return createReceipt(voucher.getFaceValue());
    }
}
```

---

### Model C: Third-Party Voucher Acceptance (NOT RECOMMENDED)

**Structure**: Mint accepts vouchers from external merchants.

```
┌────────────┐           ┌──────────┐
│ Merchant A │           │  Mint X  │
│ (External) │─ trust? ─→│          │
└────────────┘           └──────────┘
      │                        │
      └──── settlement? ───────┘
```

**Why This Model is Problematic**:

#### 1. The Mint Cannot Actually Use the Voucher

**Critical insight**: When you redeem, the Cashu proof becomes **SPENT**.

```java
// What happens when user redeems:
mint.swap(voucherProof);  // User gives voucher to mint

// Mint marks it spent:
proofVault.markSpent(voucherProof);  // ✓ Proof is now unusable

// Mint issues new ecash:
issueEcash(voucherAmount);  // From mint's own reserves

// Later, can mint use the voucher at Merchant A?
merchantA.acceptPayment(voucherProof);
// ❌ REJECTED: "Proof already spent in Cashu network"
```

**The Cashu proof is spent** - it's like a check that's been cashed. You can't cash it twice.

#### 2. Mint Has No Settlement Mechanism

```
Traditional Gift Card Redemption:
┌──────────┐        ┌──────┐        ┌────────────┐
│   You    │───────>│ Bank │───────>│ Merchant A │
└──────────┘        └──────┘        └────────────┘
                       │                   │
                       └──────settles─────>│
                       (Bank pays merchant)

Cashu Voucher (No Settlement):
┌──────────┐        ┌──────┐        ┌────────────┐
│   You    │───────>│ Mint │   ❌   │ Merchant A │
└──────────┘        └──────┘        └────────────┘
                       │              (No settlement
                       │               mechanism)
                       └─ Issues ecash from own reserves
```

**The mint issues ecash from its own reserves**, trusting it can eventually settle with the merchant through some external arrangement.

#### 3. Regulatory Nightmare

If mint accepts external vouchers, it becomes:

**Money Transmitter**:
- Accepts value from one party (voucher holder)
- Transmits value to another (ecash)
- Requires licenses (expensive, complex)

**Payment Service Provider**:
- PSD2 compliance (Europe)
- FCA regulation (UK)
- State-by-state licensing (US)

**Counterparty Risk Holder**:
- What if merchant goes insolvent?
- What if merchant refuses settlement?
- What if merchant issues fraudulent vouchers?

**Regulatory Cost Estimate** (US):
```
Money Transmitter Licenses:
├─ Federal: FinCEN registration (~$5,000)
├─ State licenses: ~48 states × $50k-$500k each
├─ Total licensing cost: $2.4M - $24M
├─ Annual compliance: $100k - $500k per year
└─ Legal fees: $50k - $200k per year

Total first year: $2.55M - $24.7M 💸
```

#### 4. Trust and Solvency Issues

```java
class VoucherAcceptanceRisks {

    // Scenario 1: Merchant insolvency
    void merchantBankruptcy() {
        // Mint accepted £100,000 of Merchant A vouchers
        // Issued £100,000 ecash to users
        // Merchant declares bankruptcy
        // → Mint loses £100,000 (holding worthless vouchers)
        // → Mint is now undercapitalized
    }

    // Scenario 2: Fraudulent vouchers
    void merchantFraud() {
        // Merchant creates £1,000,000 fake vouchers
        // Users redeem at mint
        // Mint issues £1,000,000 ecash
        // Merchant disappears
        // → Mint is insolvent
    }

    // Scenario 3: Settlement dispute
    void settlementFailure() {
        // £50,000 vouchers redeemed this month
        // Mint requests payment from merchant
        // Merchant: "Sorry, cash flow problems"
        // → Mint has issued ecash but received no payment
    }
}
```

**Conclusion**: Model C creates regulatory complexity, counterparty risk, and operational challenges that make it impractical.

---

## When to Use Each Model

### Choose Model A (Integrated) If:
- ✅ You control both merchant and mint operations
- ✅ You want to offer voucher-to-ecash conversion
- ✅ You can handle money transmitter licensing for ecash
- ✅ You want maximum user flexibility
- ✅ You have resources for compliance

### Choose Model B (No Redemption) If:
- ✅ You want minimal regulatory burden
- ✅ You're just adding Cashu tech to existing gift cards
- ✅ You don't need voucher-to-ecash conversion
- ✅ Clearest legal path is your priority
- ✅ Traditional gift card model is sufficient

### Never Choose Model C (Third-Party) Because:
- ❌ Regulatory nightmare (money transmitter licensing)
- ❌ Counterparty risk (merchant insolvency)
- ❌ No settlement mechanism built into Cashu
- ❌ Trust assumptions unworkable
- ❌ Operational complexity too high

---

## Voucher Redemption Process

### Overview

Voucher redemption is the process of spending a voucher proof to receive either:
1. **Different proofs** (swap operation - only in Model A)
2. **Lightning payment** (melt operation - only in Model A)
3. **Direct payment at merchant** (Model B - no conversion)

**IMPORTANT**: This section describes redemption in **Model A (Integrated Merchant-Mint)** only. In **Model B (No Redemption)**, vouchers can ONLY be spent at the issuing merchant, never converted to ecash.

### Basic Redemption Flow (Model A Only)

```
┌──────────────┐
│ User Wallet  │
│              │
│ Has Voucher  │
│ Proof        │
└──────┬───────┘
       │
       │ 1. Create Swap/Melt Request
       │    (Include voucher proof as input)
       ▼
┌──────────────┐
│  Cashu Mint  │
│              │
│ Validates:   │
│ • Signature  │──────┐
│ • Issuer     │      │ 2. Validation
│ • Expiry     │      │
│ • Allowlist  │◄─────┘
└──────┬───────┘
       │
       │ 3. If valid: Sign new outputs
       │    If invalid: Return error
       ▼
┌──────────────┐
│ User Wallet  │
│              │
│ Receives:    │
│ • New proofs │
│   OR         │
│ • Lightning  │
│   payment    │
└──────────────┘
```

### Detailed Step-by-Step Process

#### Step 1: User Initiates Redemption

```java
// User has voucher proof
Proof<VoucherSecret> voucherProof = wallet.getVoucher(voucherId);

VoucherSecret voucher = voucherProof.getSecret();
System.out.println("Redeeming voucher:");
System.out.println("  Issuer: " + voucher.getIssuerId());
System.out.println("  Value: $" + voucher.getFaceValue() / 100.0);
System.out.println("  Expires: " + Instant.ofEpochSecond(voucher.getExpiresAt()));

// Check if expired
if (voucher.getExpiresAt() < Instant.now().getEpochSecond()) {
    throw new VoucherExpiredException("Voucher expired");
}

// User chooses redemption method
RedemptionMethod method = askUser(); // SWAP or MELT
```

#### Step 2: Create Blinded Outputs

```java
// Generate new secrets for outputs
List<BlindedMessage> outputs = new ArrayList<>();

if (method == RedemptionMethod.SWAP) {
    // Swap to new proofs (could be deterministic for recovery!)
    DeterministicKey masterKey = wallet.getMasterKey();

    for (int amount : splitAmounts(voucherProof.getAmount())) {
        // Derive deterministic secret (recoverable from mnemonic!)
        DeterministicSecret newSecret = DeterministicSecret.fromDerivation(
            masterKey,
            keysetId,
            wallet.getNextCounter()
        );

        // Blind the secret
        BigInteger r = generateBlindingFactor();
        PublicKey B_ = blind(newSecret, r);

        BlindedMessage output = BlindedMessage.builder()
            .amount(amount)
            .keySetId(keysetId)
            .blindedMessage(B_)
            .build();

        outputs.add(output);
        wallet.storeBlindingFactor(newSecret, r);
    }

} else if (method == RedemptionMethod.MELT) {
    // Melt to Lightning payment
    String invoice = askUserForInvoice();
    // No outputs needed for melt
}
```

#### Step 3: Create Swap/Melt Request

**For Swap (Voucher → New Proofs)**:

```java
PostSwapRequest<VoucherSecret> swapRequest = PostSwapRequest.builder()
    .inputs(List.of(voucherProof))  // Voucher as input
    .outputs(outputs)                // New blinded outputs
    .build();

// Send to mint
PostSwapResponse swapResponse = cashuClient.swap(swapRequest);
```

**For Melt (Voucher → Lightning)**:

```java
// First create melt quote
PostMeltQuoteRequest quoteRequest = PostMeltQuoteRequest.builder()
    .unit("sat")
    .request(lightningInvoice)
    .build();

PostMeltQuoteResponse quote = cashuClient.getMeltQuote(quoteRequest);

// Then melt voucher
PostMeltRequest<VoucherSecret> meltRequest = PostMeltRequest.builder()
    .quote(quote.getQuote())
    .inputs(List.of(voucherProof))  // Voucher as input
    .build();

PostMeltResponse meltResponse = cashuClient.melt(meltRequest);
```

#### Step 4: Mint Validates Voucher (Model A Only)

**IMPORTANT**: This only works if mint and merchant are the same entity (Model A).

**On the mint side**:

```java
@PostMapping("/swap")
public ResponseEntity<PostSwapResponse> swap(
    @RequestBody PostSwapRequest<? extends Secret> request
) throws CashuErrorException {

    // Validate inputs (including vouchers)
    for (Proof<? extends Secret> proof : request.getInputs()) {

        // Standard proof validation
        validateProofSignature(proof);
        validateNotSpent(proof);

        // Voucher-specific validation
        if (proof.getSecret() instanceof VoucherSecret voucher) {
            validateVoucher(voucher);
        }

        // Mark as spent
        markProofSpent(proof);
    }

    // Sign new outputs
    List<BlindSignature> signatures = signOutputs(request.getOutputs());

    return ResponseEntity.ok(new PostSwapResponse(signatures));
}

private void validateVoucher(VoucherSecret voucher) throws CashuErrorException {

    // 0. CRITICAL: Only accept our OWN vouchers
    if (!voucher.getIssuerId().equals(OUR_ENTITY_ID)) {
        throw new CashuErrorException(
            CashuErrorCode.EXTERNAL_VOUCHER_NOT_ACCEPTED,
            "We only accept vouchers we issued ourselves. " +
            "This prevents regulatory issues and counterparty risk."
        );
    }

    // 1. Check expiry
    if (voucher.getExpiresAt() < Instant.now().getEpochSecond()) {
        throw new CashuErrorException(
            CashuErrorCode.VOUCHER_EXPIRED,
            "Voucher expired at " + voucher.getExpiresAt()
        );
    }

    // 2. Check issuer allowlist
    if (!allowedIssuers.contains(voucher.getIssuerId())) {
        throw new CashuErrorException(
            CashuErrorCode.ISSUER_NOT_ALLOWED,
            "Issuer " + voucher.getIssuerId() + " not in allowlist"
        );
    }

    // 3. Verify issuer signature
    byte[] canonicalBytes = voucher.getCanonicalBytesWithoutSignature();
    PublicKey issuerPubKey = PublicKey.fromString(voucher.getIssuerPubKey());
    Signature issuerSig = Signature.fromString(voucher.getIssuerSignature());

    if (!issuerPubKey.verify(canonicalBytes, issuerSig)) {
        throw new CashuErrorException(
            CashuErrorCode.INVALID_ISSUER_SIGNATURE,
            "Voucher issuer signature invalid"
        );
    }

    // 4. Check currency (optional)
    if (!supportedCurrencies.contains(voucher.getCurrency())) {
        throw new CashuErrorException(
            CashuErrorCode.CURRENCY_NOT_SUPPORTED,
            "Currency " + voucher.getCurrency() + " not supported"
        );
    }

    // 5. Validate spending conditions (if any)
    if (hasSpendingConditions(voucher)) {
        validateSpendingConditions(voucher, proof.getWitness());
    }
}
```

#### Step 5: User Receives New Proofs

```java
// Unblind signatures
List<Proof<DeterministicSecret>> newProofs = new ArrayList<>();

for (int i = 0; i < swapResponse.getSignatures().size(); i++) {
    BlindSignature blindSig = swapResponse.getSignatures().get(i);
    DeterministicSecret secret = outputSecrets.get(i);
    BigInteger r = blindingFactors.get(i);

    // Unblind: C = C' - r*K
    Signature unblindedSig = unblind(blindSig, r, mintPubKey);

    Proof<DeterministicSecret> proof = Proof.builder()
        .amount(amounts.get(i))
        .keySetId(keysetId)
        .secret(secret)
        .unblindedSignature(unblindedSig)
        .build();

    newProofs.add(proof);
}

// Store new proofs (these are recoverable from mnemonic!)
wallet.storeProofs(newProofs);

// Delete old voucher proof (spent)
wallet.deleteProof(voucherProof);

System.out.println("Voucher redeemed successfully!");
System.out.println("New balance: $" + wallet.getBalance());
```

---

## User Experience Flows

### Flow 1: Simple Voucher Redemption (Swap to Recoverable)

**User Story**: "I received a $50 coffee shop gift card. I want to redeem it so I don't lose it if I lose my phone."

```
┌─────────────────────────────────────────────────┐
│ Coffee Shop Gift Card                           │
│                                                 │
│ Value: $50.00                                   │
│ Expires: 2025-12-31                             │
│ Issuer: CoffeeShop Network                     │
│                                                 │
│ ⚠️  This voucher is NOT recoverable from seed   │
│ ⚠️  Redeem now to convert to recoverable proofs │
│                                                 │
│ [Redeem Now] [Export Voucher] [Use at Store]   │
└─────────────────────────────────────────────────┘

User taps "Redeem Now"

┌─────────────────────────────────────────────────┐
│ Redeem Voucher                                  │
│                                                 │
│ Convert voucher to regular ecash?              │
│                                                 │
│ ✅ Pro: Recoverable from your seed phrase       │
│ ✅ Pro: No expiry date                          │
│ ⚠️  Con: Can't be used directly at coffee shop  │
│                                                 │
│ After redeeming, you'll have $50 in regular     │
│ ecash that can be recovered if you lose your    │
│ device.                                         │
│                                                 │
│ [Cancel] [Redeem to Recoverable Ecash]         │
└─────────────────────────────────────────────────┘

User confirms

┌─────────────────────────────────────────────────┐
│ ⏳ Redeeming voucher...                          │
│                                                 │
│ • Contacting mint...                            │
│ • Validating voucher...                         │
│ • Creating new proofs...                        │
└─────────────────────────────────────────────────┘

Success!

┌─────────────────────────────────────────────────┐
│ ✅ Voucher Redeemed!                             │
│                                                 │
│ Your $50 voucher has been converted to          │
│ recoverable ecash.                              │
│                                                 │
│ New balance: $50.00                             │
│ ✅ Recoverable from seed phrase                  │
│                                                 │
│ [Done]                                          │
└─────────────────────────────────────────────────┘
```

### Flow 2: Spending Voucher at Merchant

**User Story**: "I want to buy a $4 coffee using my $50 gift card."

```
┌─────────────────────────────────────────────────┐
│ Pay for Coffee                                  │
│                                                 │
│ Amount: $4.00                                   │
│ Merchant: CoffeeShop #42                        │
│                                                 │
│ Select payment method:                          │
│                                                 │
│ ⚪ Regular Ecash ($120.00 available)            │
│ 🔘 CoffeeShop Gift Card ($50.00 available)      │
│                                                 │
│ Change: $46.00 will be returned as new          │
│         gift card or regular ecash              │
│                                                 │
│ [Cancel] [Pay $4.00]                            │
└─────────────────────────────────────────────────┘

User taps "Pay"

┌─────────────────────────────────────────────────┐
│ ⏳ Processing payment...                         │
│                                                 │
│ • Creating payment proof...                     │
│ • Sending to merchant...                        │
│ • Receiving change...                           │
└─────────────────────────────────────────────────┘

Success!

┌─────────────────────────────────────────────────┐
│ ✅ Payment Successful!                           │
│                                                 │
│ Paid: $4.00                                     │
│ Change: $46.00 (returned as regular ecash)      │
│                                                 │
│ New balance:                                    │
│ • Regular ecash: $166.00 (recoverable)          │
│ • Gift cards: $0.00                             │
│                                                 │
│ [Done]                                          │
└─────────────────────────────────────────────────┘
```

### Flow 3: Voucher to Lightning

**User Story**: "I want to send my gift card value to my Lightning wallet."

```
┌─────────────────────────────────────────────────┐
│ Coffee Shop Gift Card                           │
│                                                 │
│ Value: $50.00                                   │
│                                                 │
│ [Redeem to Ecash] [Send to Lightning] [More]   │
└─────────────────────────────────────────────────┘

User taps "Send to Lightning"

┌─────────────────────────────────────────────────┐
│ Melt to Lightning                               │
│                                                 │
│ Amount: $50.00 (≈ 45,000 sats)                  │
│                                                 │
│ Paste Lightning invoice:                        │
│ ┌─────────────────────────────────────────────┐ │
│ │ lnbc450000n1p...                            │ │
│ └─────────────────────────────────────────────┘ │
│                                                 │
│ OR                                              │
│                                                 │
│ [Scan QR Code] [Generate Invoice]              │
│                                                 │
│ ⚠️  Mint fee: ~1,000 sats                       │
│                                                 │
│ [Cancel] [Send ⚡]                              │
└─────────────────────────────────────────────────┘

User confirms

┌─────────────────────────────────────────────────┐
│ ⏳ Melting to Lightning...                       │
│                                                 │
│ • Requesting melt quote...                      │
│ • Melting voucher...                            │
│ • Paying invoice...                             │
└─────────────────────────────────────────────────┘

Success!

┌─────────────────────────────────────────────────┐
│ ✅ Payment Sent!                                 │
│                                                 │
│ Amount: 45,000 sats                             │
│ Fee: 1,000 sats                                 │
│ Total: 46,000 sats                              │
│                                                 │
│ Payment preimage: abc123...                     │
│                                                 │
│ [Done]                                          │
└─────────────────────────────────────────────────┘
```

### Flow 4: Expiring Voucher Warning

```
┌─────────────────────────────────────────────────┐
│ ⚠️  Expiring Vouchers!                           │
│                                                 │
│ You have 2 vouchers expiring soon:              │
│                                                 │
│ • CoffeeShop Gift Card                          │
│   $50.00 - Expires in 3 days                    │
│   [Redeem Now]                                  │
│                                                 │
│ • Restaurant Voucher                            │
│   $25.00 - Expires in 7 days                    │
│   [Redeem Now]                                  │
│                                                 │
│ Redeem these vouchers to convert them to        │
│ regular ecash with no expiry.                   │
│                                                 │
│ [Remind Me Later] [Redeem All]                  │
└─────────────────────────────────────────────────┘
```

---

## Nostr Backup Strategy

### Recommended NIPs for Voucher Backup

Based on the analysis, here are the best options:

#### **Option 1: NIP-78 (Application-Specific Data) - RECOMMENDED**

**Why**: Purpose-built for application data storage, supports replaceable events.

```javascript
{
  "kind": 30078,  // Application-specific data
  "pubkey": "<user-nostr-pubkey>",
  "created_at": 1699564800,
  "tags": [
    ["d", "cashu-voucher-backup"],  // Unique identifier
    ["client", "cashu-wallet-v1.0"],
    ["encrypted"]  // Flag indicating encrypted payload
  ],
  "content": "<encrypted-voucher-data>"
}
```

**Advantages**:
- ✅ Replaceable (kind 30000-39999) - Latest backup overwrites old ones
- ✅ Application-specific - Clear separation from other data
- ✅ Supports multiple backups via different `d` tags
- ✅ Queryable by application

**Implementation**:

```java
public class NostrVoucherBackup {

    private static final int KIND_APP_DATA = 30078;
    private static final String D_TAG_VOUCHER_BACKUP = "cashu-voucher-backup";

    public NostrEvent backupVouchers(
        List<Proof<VoucherSecret>> vouchers,
        PrivateKey nostrKey,
        String encryptionPassword
    ) throws Exception {

        // 1. Serialize vouchers
        VoucherBackupData backup = VoucherBackupData.builder()
            .version(1)
            .timestamp(Instant.now().getEpochSecond())
            .vouchers(vouchers)
            .build();

        String json = JsonUtils.JSON_MAPPER.writeValueAsString(backup);

        // 2. Encrypt (NIP-44 or custom encryption)
        String encrypted = encrypt(json, encryptionPassword);

        // 3. Create Nostr event
        NostrEvent event = NostrEvent.builder()
            .kind(KIND_APP_DATA)
            .pubkey(nostrKey.getPublicKey().toString())
            .createdAt(Instant.now().getEpochSecond())
            .tags(List.of(
                Tag.of("d", D_TAG_VOUCHER_BACKUP),
                Tag.of("client", "cashu-wallet-v1.0"),
                Tag.of("encrypted"),
                Tag.of("voucher_count", String.valueOf(vouchers.size()))
            ))
            .content(encrypted)
            .build();

        // 4. Sign event
        event.sign(nostrKey);

        // 5. Publish to relays
        publishToRelays(event);

        return event;
    }

    public List<Proof<VoucherSecret>> restoreVouchers(
        PrivateKey nostrKey,
        String encryptionPassword
    ) throws Exception {

        // 1. Query Nostr for backup
        Filter filter = Filter.builder()
            .kinds(List.of(KIND_APP_DATA))
            .authors(List.of(nostrKey.getPublicKey().toString()))
            .tag("d", D_TAG_VOUCHER_BACKUP)
            .limit(1)  // Get most recent
            .build();

        List<NostrEvent> events = nostrClient.query(filter);

        if (events.isEmpty()) {
            throw new NoBackupFoundException("No voucher backup found");
        }

        NostrEvent latestBackup = events.get(0);

        // 2. Decrypt content
        String decrypted = decrypt(latestBackup.getContent(), encryptionPassword);

        // 3. Deserialize vouchers
        VoucherBackupData backup = JsonUtils.JSON_MAPPER.readValue(
            decrypted,
            VoucherBackupData.class
        );

        return backup.getVouchers();
    }
}

@Data
@Builder
class VoucherBackupData {
    private int version;
    private long timestamp;
    private List<Proof<VoucherSecret>> vouchers;
}
```

---

#### **Option 2: NIP-59 (Gift Wrap) + NIP-44 (Encrypted) - MAXIMUM PRIVACY**

**Why**: Maximum privacy - hides sender, recipient, timestamp.

```javascript
// Outer layer (Gift Wrap)
{
  "kind": 1059,  // Gift wrap
  "pubkey": "<random-pubkey>",  // Disposable key
  "created_at": "<random-timestamp>",
  "tags": [
    ["p", "<user-pubkey>"]  // Recipient (yourself)
  ],
  "content": "<sealed-rumor>"  // Inner event encrypted with NIP-44
}

// Inner layer (Sealed Rumor)
{
  "kind": 13059,  // Seal (contains the actual rumor)
  "content": "<encrypted-voucher-backup>"  // Your actual voucher data
}
```

**Advantages**:
- ✅ Maximum privacy - Metadata hidden
- ✅ Self-addressed - Send backup to yourself
- ✅ Unlinkable - Each backup uses different disposable key
- ✅ Future-proof - Modern Nostr privacy standard

**Disadvantages**:
- ⚠️ More complex implementation
- ⚠️ Harder to query (no clear `d` tag)
- ⚠️ Must track gift wrap events somehow

**Implementation**:

```java
public class NostrGiftWrapVoucherBackup {

    public NostrEvent backupVouchersPrivate(
        List<Proof<VoucherSecret>> vouchers,
        PrivateKey userNostrKey,
        String encryptionPassword
    ) throws Exception {

        // 1. Serialize vouchers
        String voucherJson = serializeVouchers(vouchers);

        // 2. Encrypt with password (additional layer)
        String encrypted = encrypt(voucherJson, encryptionPassword);

        // 3. Create inner rumor (the actual backup)
        NostrEvent rumor = NostrEvent.builder()
            .kind(13059)  // Seal kind
            .pubkey(userNostrKey.getPublicKey().toString())
            .createdAt(Instant.now().getEpochSecond())
            .content(encrypted)
            .build();
        // Note: Rumor is NOT signed (part of gift wrap protocol)

        // 4. Create gift wrap (outer layer)
        PrivateKey disposableKey = PrivateKey.generate();  // One-time use

        // Encrypt rumor with NIP-44
        String sealedRumor = nip44Encrypt(
            rumor.toJson(),
            disposableKey,
            userNostrKey.getPublicKey()
        );

        // 5. Create gift wrap event
        NostrEvent giftWrap = NostrEvent.builder()
            .kind(1059)  // Gift wrap kind
            .pubkey(disposableKey.getPublicKey().toString())
            .createdAt(randomTimestamp())  // Random for privacy
            .tags(List.of(
                Tag.of("p", userNostrKey.getPublicKey().toString())  // To yourself
            ))
            .content(sealedRumor)
            .build();

        giftWrap.sign(disposableKey);

        // 6. Publish to relays
        publishToRelays(giftWrap);

        return giftWrap;
    }

    public List<Proof<VoucherSecret>> restoreVouchersPrivate(
        PrivateKey userNostrKey,
        String encryptionPassword
    ) throws Exception {

        // 1. Query for gift wraps addressed to you
        Filter filter = Filter.builder()
            .kinds(List.of(1059))  // Gift wrap
            .tag("p", userNostrKey.getPublicKey().toString())
            .since(Instant.now().minus(90, ChronoUnit.DAYS).getEpochSecond())
            .build();

        List<NostrEvent> giftWraps = nostrClient.query(filter);

        // 2. Try to decrypt each gift wrap
        for (NostrEvent giftWrap : giftWraps) {
            try {
                // Extract disposable pubkey from gift wrap
                String disposablePubkey = giftWrap.getPubkey();

                // Decrypt seal with NIP-44
                String rumor = nip44Decrypt(
                    giftWrap.getContent(),
                    userNostrKey,
                    disposablePubkey
                );

                NostrEvent sealEvent = NostrEvent.fromJson(rumor);

                // Check if this is a voucher backup
                if (sealEvent.getKind() == 13059) {
                    // Decrypt voucher data with password
                    String voucherJson = decrypt(
                        sealEvent.getContent(),
                        encryptionPassword
                    );

                    VoucherBackupData backup = JsonUtils.JSON_MAPPER.readValue(
                        voucherJson,
                        VoucherBackupData.class
                    );

                    return backup.getVouchers();
                }

            } catch (Exception e) {
                // Skip this gift wrap, might not be a voucher backup
                continue;
            }
        }

        throw new NoBackupFoundException("No voucher backup found");
    }
}
```

---

#### **Option 3: NIP-17 (Private Direct Message) - SIMPLE**

**Why**: Simple encrypted messaging to yourself.

```javascript
{
  "kind": 14,  // Regular DM
  "pubkey": "<user-pubkey>",
  "created_at": 1699564800,
  "tags": [
    ["p", "<user-pubkey>"],  // To yourself
    ["subject", "cashu-voucher-backup"]  // Subject tag
  ],
  "content": "<nip44-encrypted-voucher-data>"
}
```

**Advantages**:
- ✅ Simple implementation
- ✅ Native Nostr encryption (NIP-44)
- ✅ Self-messaging pattern well understood
- ✅ Compatible with DM clients

**Disadvantages**:
- ⚠️ Metadata visible (sender = recipient = you)
- ⚠️ Must search through all DMs to find backup
- ⚠️ No replaceable event (old backups linger)

---

### Comparison Matrix

| Aspect | NIP-78 (App Data) | NIP-59 (Gift Wrap) | NIP-17 (Private DM) |
|--------|-------------------|--------------------|--------------------|
| **Privacy** | Medium | Very High | Low-Medium |
| **Complexity** | Low | High | Very Low |
| **Replaceability** | ✅ Yes | ❌ No | ❌ No |
| **Queryability** | ✅ Easy | ⚠️ Moderate | ⚠️ Moderate |
| **Metadata Hiding** | ❌ No | ✅ Yes | ❌ No |
| **Implementation** | 50 lines | 150 lines | 30 lines |
| **Relay Support** | ✅ Good | ✅ Good | ✅ Excellent |
| **Best For** | Standard backup | Maximum privacy | Quick prototype |

---

### Recommended Approach: NIP-78 with Encryption

**Why**:
1. ✅ **Replaceable** - Old backups automatically replaced
2. ✅ **Queryable** - Easy to find your backup
3. ✅ **Application-specific** - Clear semantic meaning
4. ✅ **Simple** - Straightforward implementation
5. ✅ **Encrypted** - Add your own encryption layer for security

**Encryption Strategy**: Layer NIP-44 encryption on top:

```java
public String encrypt(String plaintext, String password) {
    // Option 1: Password-based encryption (PBKDF2 + AES-GCM)
    byte[] salt = generateSalt();
    byte[] key = deriveKey(password, salt);
    byte[] encrypted = aesGcmEncrypt(plaintext.getBytes(), key);

    return Base64.encode(salt) + ":" + Base64.encode(encrypted);
}

public String decrypt(String ciphertext, String password) {
    String[] parts = ciphertext.split(":");
    byte[] salt = Base64.decode(parts[0]);
    byte[] encrypted = Base64.decode(parts[1]);

    byte[] key = deriveKey(password, salt);
    byte[] decrypted = aesGcmDecrypt(encrypted, key);

    return new String(decrypted);
}
```

---

## Implementation Examples

### Complete Backup & Restore Flow

```java
public class VoucherBackupService {

    private final NostrClient nostrClient;
    private final PrivateKey userNostrKey;

    public void backupVouchers(List<Proof<VoucherSecret>> vouchers, String password)
            throws Exception {

        System.out.println("Backing up " + vouchers.size() + " vouchers to Nostr...");

        // 1. Prepare backup data
        VoucherBackupData backup = VoucherBackupData.builder()
            .version(1)
            .timestamp(Instant.now().getEpochSecond())
            .vouchers(vouchers)
            .walletFingerprint(getWalletFingerprint())  // Identify which wallet
            .build();

        // 2. Serialize to JSON
        String json = JsonUtils.JSON_MAPPER.writeValueAsString(backup);
        System.out.println("Backup size: " + json.length() + " bytes");

        // 3. Compress (optional, for large backups)
        byte[] compressed = compress(json.getBytes());
        String base64 = Base64.encode(compressed);
        System.out.println("Compressed size: " + base64.length() + " bytes");

        // 4. Encrypt with password
        String encrypted = encrypt(base64, password);

        // 5. Create Nostr event (NIP-78)
        NostrEvent event = NostrEvent.builder()
            .kind(30078)
            .pubkey(userNostrKey.getPublicKey().toString())
            .createdAt(Instant.now().getEpochSecond())
            .tags(List.of(
                Tag.of("d", "cashu-voucher-backup"),
                Tag.of("client", "cashu-wallet"),
                Tag.of("version", "1"),
                Tag.of("encrypted"),
                Tag.of("compressed"),
                Tag.of("voucher_count", String.valueOf(vouchers.size())),
                Tag.of("total_value", String.valueOf(calculateTotalValue(vouchers)))
            ))
            .content(encrypted)
            .build();

        // 6. Sign event
        event.sign(userNostrKey);

        // 7. Publish to relays
        List<String> relays = getBackupRelays();
        int published = 0;

        for (String relay : relays) {
            try {
                nostrClient.publish(relay, event);
                published++;
                System.out.println("Published to " + relay);
            } catch (Exception e) {
                System.err.println("Failed to publish to " + relay + ": " + e.getMessage());
            }
        }

        if (published == 0) {
            throw new BackupFailedException("Failed to publish to any relay");
        }

        System.out.println("✅ Backup successful! Published to " + published + " relays");
    }

    public List<Proof<VoucherSecret>> restoreVouchers(String password)
            throws Exception {

        System.out.println("Restoring vouchers from Nostr...");

        // 1. Query Nostr for backup
        Filter filter = Filter.builder()
            .kinds(List.of(30078))
            .authors(List.of(userNostrKey.getPublicKey().toString()))
            .tag("d", "cashu-voucher-backup")
            .limit(1)  // Get most recent
            .build();

        List<NostrEvent> events = nostrClient.query(getBackupRelays(), filter);

        if (events.isEmpty()) {
            throw new NoBackupFoundException("No voucher backup found on Nostr");
        }

        NostrEvent backup = events.get(0);
        System.out.println("Found backup from " +
            Instant.ofEpochSecond(backup.getCreatedAt()));

        // 2. Verify signature
        if (!backup.verify()) {
            throw new InvalidSignatureException("Backup signature invalid");
        }

        // 3. Decrypt
        String encrypted = backup.getContent();
        String base64 = decrypt(encrypted, password);

        // 4. Decompress
        byte[] compressed = Base64.decode(base64);
        String json = new String(decompress(compressed));

        // 5. Deserialize
        VoucherBackupData backupData = JsonUtils.JSON_MAPPER.readValue(
            json,
            VoucherBackupData.class
        );

        // 6. Validate backup version
        if (backupData.getVersion() > 1) {
            System.err.println("Warning: Backup version " + backupData.getVersion() +
                " is newer than supported. Some features may not work.");
        }

        // 7. Check for expired vouchers
        List<Proof<VoucherSecret>> validVouchers = new ArrayList<>();
        int expiredCount = 0;
        long now = Instant.now().getEpochSecond();

        for (Proof<VoucherSecret> voucher : backupData.getVouchers()) {
            VoucherSecret secret = voucher.getSecret();

            if (secret.getExpiresAt() != null && secret.getExpiresAt() < now) {
                expiredCount++;
                System.out.println("⚠️  Skipping expired voucher: " +
                    secret.getIssuerId() + " ($" + secret.getFaceValue() / 100.0 + ")");
            } else {
                validVouchers.add(voucher);
            }
        }

        System.out.println("✅ Restored " + validVouchers.size() + " vouchers");
        if (expiredCount > 0) {
            System.out.println("⚠️  " + expiredCount + " vouchers expired");
        }

        return validVouchers;
    }

    private List<String> getBackupRelays() {
        return List.of(
            "wss://relay.damus.io",
            "wss://nos.lol",
            "wss://relay.primal.net"
        );
    }

    private String getWalletFingerprint() {
        // Derive wallet fingerprint from master key
        // Used to identify which wallet created the backup
        return sha256(userNostrKey.getPublicKey().toString()).substring(0, 8);
    }

    private long calculateTotalValue(List<Proof<VoucherSecret>> vouchers) {
        return vouchers.stream()
            .mapToLong(Proof::getAmount)
            .sum();
    }
}
```

### Automatic Backup on Voucher Receipt

```java
public class WalletService {

    private final VoucherBackupService backupService;
    private final UserPreferences preferences;

    public void receiveVoucher(Proof<VoucherSecret> voucher) {

        // Store voucher locally
        walletDb.storeProof(voucher);

        // Check if auto-backup enabled
        if (preferences.isAutoBackupEnabled()) {

            // Get all vouchers
            List<Proof<VoucherSecret>> allVouchers = walletDb.getVouchers();

            // Backup to Nostr
            try {
                backupService.backupVouchers(
                    allVouchers,
                    preferences.getBackupPassword()
                );

                showNotification("✅ Voucher backed up to Nostr");

            } catch (Exception e) {
                showNotification("⚠️ Voucher backup failed: " + e.getMessage());
                // Still store locally even if backup fails
            }
        } else {
            showNotification("💡 Tip: Enable auto-backup to protect your vouchers");
        }
    }
}
```

### Scheduled Backup Reminder

```java
public class BackupReminderService {

    public void checkBackupStatus() {

        // Get vouchers not backed up
        List<Proof<VoucherSecret>> vouchers = walletDb.getVouchers();
        long lastBackup = preferences.getLastBackupTimestamp();
        long now = Instant.now().getEpochSecond();

        // If no backup in 7 days and has vouchers
        if (now - lastBackup > 7 * 24 * 3600 && !vouchers.isEmpty()) {

            long totalValue = vouchers.stream()
                .mapToLong(Proof::getAmount)
                .sum();

            showNotification(
                "⚠️ Backup Reminder",
                "You have $" + (totalValue / 100.0) + " in vouchers not backed up. " +
                "Backup now to prevent loss if you lose your device.",
                List.of(
                    new Action("Backup Now", this::initiateBackup),
                    new Action("Remind Later", this::snoozeReminder)
                )
            );
        }

        // Warn about expiring vouchers
        long expiryThreshold = now + 7 * 24 * 3600;  // 7 days
        List<Proof<VoucherSecret>> expiringSoon = vouchers.stream()
            .filter(v -> {
                Long expiry = v.getSecret().getExpiresAt();
                return expiry != null && expiry < expiryThreshold;
            })
            .toList();

        if (!expiringSoon.isEmpty()) {
            showNotification(
                "⏰ Expiring Vouchers",
                expiringSoon.size() + " voucher(s) expiring within 7 days. " +
                "Redeem or backup now!",
                List.of(
                    new Action("View Vouchers", this::showExpiringVouchers),
                    new Action("Backup All", this::initiateBackup)
                )
            );
        }
    }
}
```

---

## Security Considerations

### 1. Encryption is Critical

**Never store vouchers unencrypted on Nostr** - they're bearer instruments!

```java
// ❌ NEVER DO THIS
NostrEvent event = new NostrEvent();
event.setContent(voucherJson);  // Plaintext voucher = anyone can steal!

// ✅ ALWAYS ENCRYPT
String encrypted = encrypt(voucherJson, userPassword);
event.setContent(encrypted);
```

### 2. Password Management

**Strong password required** for backup encryption:

```java
public class BackupPasswordValidator {

    public void validateBackupPassword(String password) {
        if (password.length() < 12) {
            throw new WeakPasswordException("Password must be at least 12 characters");
        }

        if (!hasUppercase(password) || !hasLowercase(password) ||
            !hasDigit(password) || !hasSpecialChar(password)) {
            throw new WeakPasswordException(
                "Password must contain uppercase, lowercase, digit, and special character"
            );
        }

        // Check against common passwords
        if (isCommonPassword(password)) {
            throw new WeakPasswordException("Password is too common");
        }
    }
}
```

### 3. Key Derivation

**Use proper key derivation** for password-based encryption:

```java
public byte[] deriveKey(String password, byte[] salt) {
    // PBKDF2 with SHA-256, 600,000 iterations (OWASP recommendation 2023)
    PBEKeySpec spec = new PBEKeySpec(
        password.toCharArray(),
        salt,
        600_000,  // Iterations
        256       // Key length
    );

    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    return factory.generateSecret(spec).getEncoded();
}
```

### 4. Metadata Leakage

Be aware of metadata visible on Nostr:

```java
// Visible metadata:
// - Your Nostr pubkey (who is backing up)
// - Timestamp (when backup happened)
// - Voucher count (how many vouchers)
// - Total value (how much money)

// To minimize metadata leakage:
// 1. Don't include voucher_count or total_value tags
// 2. Use gift wrap (NIP-59) for maximum privacy
// 3. Use generic d tag ("backup" instead of "cashu-voucher-backup")
```

### 5. Relay Selection

**Use multiple reliable relays** for redundancy:

```java
public List<String> getBackupRelays() {
    return List.of(
        "wss://relay.damus.io",      // Popular, reliable
        "wss://nos.lol",              // Fast, well-maintained
        "wss://relay.primal.net",     // High uptime
        "wss://relay.nostr.band",     // Archive relay
        "wss://your-personal-relay"   // Self-hosted for control
    );
}

// Publish to multiple relays for redundancy
for (String relay : relays) {
    nostrClient.publish(relay, event);
}
```

---

## Regulatory Considerations

### Overview

Voucher redemption has significant regulatory implications depending on which business model you choose.

### Model A: Integrated Merchant-Mint

#### What You Are

**Single legal entity operating two business lines**:
1. Retail business (merchant issuing vouchers)
2. Payment services (mint issuing ecash)

#### Regulatory Classification

**For Vouchers (Gift Cards)**:
- **UK**: Not regulated if single-purpose (only your merchant)
- **EU**: E-money license if multi-purpose (multiple merchants)
- **US**: Varies by state, often unregulated if <$10k/person

**For Ecash Minting**:
- **UK**: FCA authorization or EMI (E-Money Institution) license
- **EU**: EMI license under PSD2
- **US**: State money transmitter licenses (48+ states)

**For Voucher-to-Ecash Conversion**:
- ✅ **Internal operation** - not separately regulated
- ✅ **No money transmission** between entities (it's all you!)
- ✅ **Simple accounting** - just moving reserves

#### Advantages

```java
class IntegratedRegulatoryBenefits {

    // ✅ Single entity = single regulatory framework
    void singleJurisdiction() {
        String license = "EMI license covers both vouchers and ecash";
        String compliance = "One set of AML/KYC procedures";
        String audit = "Single audit scope";
    }

    // ✅ No counterparty risk
    void noCounterpartyRisk() {
        String reality = "You can't defraud yourself";
        String reserves = "Shared reserve pool = efficient capital use";
    }

    // ✅ No settlement complexity
    void noSettlement() {
        String accounting = "Internal transfers only";
        String noRisk = "No payment disputes with external parties";
    }
}
```

#### Required Licenses (Example: UK)

```
For Integrated Merchant-Mint in UK:
├─ EMI (E-Money Institution) License
│  ├─ Initial capital: £350,000
│  ├─ Application fee: ~£5,000
│  ├─ Processing time: 6-12 months
│  └─ Annual supervision fee: ~£10,000
│
├─ OR FCA Authorization
│  ├─ Alternative to EMI
│  ├─ Similar requirements
│  └─ May be simpler for small operations
│
└─ Business licenses
   ├─ Company registration
   ├─ VAT registration
   └─ Retail licenses
```

---

### Model B: No Redemption (Direct Payment Only)

#### What You Are

**Traditional retailer issuing gift cards**:
- Merchant issues vouchers
- Same merchant accepts vouchers for goods
- NO conversion to cash or ecash

#### Regulatory Classification

**Gift Card Issuer**:
- **UK**: Exempt from e-money regulation (single-purpose vouchers)
- **EU**: Exempt if single-purpose
- **US**: State gift card laws apply (not money transmission)

**NOT a Payment Service**:
- ❌ Not money transmitter
- ❌ Not payment processor
- ❌ Not e-money issuer

#### Why Exempt?

```java
class GiftCardExemption {

    boolean isRegulatedPaymentService() {

        // Traditional gift card characteristics:
        boolean singleIssuer = true;      // Only CoffeeShop
        boolean singlePurpose = true;     // Only buy coffee
        boolean notRedeemable = true;     // Can't get cash
        boolean notTransferable = false;  // ⚠️ Cashu is bearer

        // EU E-Money Directive exemption:
        // "Instruments that can be used only to acquire
        //  goods or services from the issuer"

        if (singleIssuer && singlePurpose && notRedeemable) {
            return false;  // ✅ Exempt!
        }

        return true;  // Regulated
    }
}
```

#### Required Compliance (Example: UK)

```
For Voucher-Only Merchant in UK:
├─ Consumer Protection
│  ├─ Fair trading practices
│  ├─ Clear terms and conditions
│  └─ Refund policy
│
├─ Accounting
│  ├─ Record voucher liabilities
│  ├─ VAT treatment (zero-rated until redemption)
│  └─ Financial reporting
│
├─ Expiry Rules
│  ├─ Min 12 months validity (best practice)
│  ├─ Clear expiry terms
│  └─ Grace period consideration
│
└─ Data Protection
   ├─ GDPR compliance (if collecting customer data)
   └─ Privacy policy

Total compliance cost: £5,000 - £20,000/year
(Compare to Model C: £2.5M - £24M!)
```

#### Advantages

```java
class VoucherOnlyBenefits {

    // ✅ Minimal regulation
    void minimalRegulation() {
        String classification = "Traditional gift card issuer";
        String licenses = "Business license only";
        String cost = "Low compliance cost";
    }

    // ✅ Clear legal framework
    void clearLegal() {
        String decades = "Gift cards regulated for 50+ years";
        String precedents = "Established case law";
        String guidance = "Clear regulatory guidance";
    }

    // ✅ Low risk
    void lowRisk() {
        String liability = "Only liable for own vouchers";
        String reserves = "Simple reserve calculation";
        String audit = "Standard retail audit";
    }
}
```

---

### Model C: Third-Party Voucher Acceptance

#### What You Are (Regulatory View)

**Money Transmitter**:
- Accept value from one party (voucher)
- Transmit value to another (ecash)
- Operate across payment systems

#### Regulatory Hell

```java
class RegulatoryNightmare {

    // US Regulatory Requirements
    class UnitedStates {
        int federalRegistration = 5_000;  // FinCEN MSB
        int stateLicenses = 48;           // Most states
        int costPerState = 150_000;       // Average
        int totalLicensing = 7_200_000;   // $7.2M!!
        int annualCompliance = 250_000;   // Per year
        int legalFees = 150_000;          // Per year

        String agenciesInvolved = "FinCEN, CFPB, State regulators";
        String timeToLicense = "2-3 years";
    }

    // EU Regulatory Requirements
    class EuropeanUnion {
        String license = "PSD2 Payment Institution license";
        int initialCapital = 125_000;     // EUR
        int applicationFee = 25_000;
        String timeToAuthorize = "9-18 months";
        String agenciesInvolved = "EBA, national regulators";
    }

    // UK Regulatory Requirements
    class UnitedKingdom {
        String license = "FCA Authorized Payment Institution";
        int safeguardingAccount = 500_000;  // GBP minimum
        int applicationFee = 5_000;
        int annualFee = 10_000;
        String timeToAuthorize = "6-12 months";
    }

    // Operational Risks
    class OperationalRisks {
        String merchantInsolvency = "Lose all accepted vouchers";
        String fraudRisk = "Merchant issues fake vouchers";
        String settlementRisk = "Merchant refuses to pay";
        String reputationRisk = "Blamed for merchant failures";
    }
}
```

#### Why This Fails

1. **No Settlement Mechanism**
   ```java
   // The Cashu proof is SPENT after redemption
   // Mint cannot use it to get money from merchant
   mint.swap(voucherProof);
   proofVault.markSpent(voucherProof);  // Now unusable

   // Mint has issued ecash but received nothing from merchant
   // Must rely on external settlement (not part of Cashu)
   ```

2. **Trust Assumptions**
   ```java
   // Mint must trust:
   boolean merchantIsSolvent = ???;       // Can they pay?
   boolean merchantIsHonest = ???;        // Will they pay?
   boolean vouchersAreValid = ???;        // Real or fake?
   boolean settlementWillWork = ???;      // How to enforce?
   ```

3. **Regulatory Classification**
   ```java
   // Regulators see:
   String activity = "Converting merchant IOUs to ecash";
   String classification = "Money transmission";
   String result = "Full payment services regulation";
   ```

---

### Recommended Regulatory Path

```
┌─────────────────────────────────────────────────┐
│ Start Here: Model B (No Redemption)             │
│                                                 │
│ • Launch as traditional gift card issuer        │
│ • Minimal regulation (consumer protection only) │
│ • Low cost (£5k-£20k/year compliance)          │
│ • Fast time to market (weeks, not years)       │
│ • Clear legal framework                         │
└─────────────────────────┬───────────────────────┘
                          │
                          │ If growth requires redemption
                          ▼
┌─────────────────────────────────────────────────┐
│ Evolve to: Model A (Integrated)                 │
│                                                 │
│ • Merge mint and merchant operations            │
│ • Get EMI/money transmitter license            │
│ • Medium cost (£350k capital + £50k/year)       │
│ • Slower (6-12 months licensing)                │
│ • Enables voucher-to-ecash conversion           │
└─────────────────────────────────────────────────┘

❌ NEVER: Model C (Third-Party Vouchers)
   • Regulatory nightmare
   • Multi-million dollar licensing
   • Years to launch
   • High operational risk
```

---

## Best Practices

### 1. Choose Your Model First

**Before implementing any code**, decide:

```java
enum VoucherBusinessModel {

    INTEGRATED_MERCHANT_MINT,  // Model A
    // - Same entity operates merchant and mint
    // - Voucher redemption allowed
    // - Need EMI/money transmitter license
    // - Medium regulatory burden

    VOUCHER_ONLY_NO_REDEMPTION,  // Model B (RECOMMENDED START)
    // - Vouchers only usable at merchant
    // - No ecash conversion
    // - Minimal regulation (gift cards)
    // - Lowest risk, fastest launch

    THIRD_PARTY_VOUCHERS  // Model C (NEVER USE)
    // - Accept external merchant vouchers
    // - Regulatory nightmare
    // - DO NOT IMPLEMENT
}
```

### 2. Backup Strategy

**Three-tier backup approach**:

```
Tier 1: Local Device
├─ Primary storage
├─ Fast access
└─ Lost if device lost

Tier 2: Nostr Backup (NIP-78)
├─ Encrypted automatic backup
├─ Recoverable from any device
└─ Requires password

Tier 3: Emergency Backup
├─ Encrypted file export
├─ Store in password manager
└─ Last resort recovery
```

### 3. User Education

Show clear warnings in UI:

```
┌─────────────────────────────────────────────────┐
│ ℹ️  About Voucher Recovery                       │
│                                                 │
│ ✅ Regular ecash: Recoverable from 12 words     │
│ ⚠️  Vouchers: NOT recoverable from 12 words     │
│                                                 │
│ Protect your vouchers:                          │
│ • ✅ Enable automatic Nostr backup              │
│ • ✅ Use strong backup password                 │
│ • ✅ Redeem vouchers before expiry              │
│ • ✅ Export high-value vouchers separately      │
│                                                 │
│ [Enable Auto-Backup] [Learn More]              │
└─────────────────────────────────────────────────┘
```

### 4. Backup Frequency

```java
public enum BackupFrequency {
    IMMEDIATE,     // Backup every voucher receipt
    DAILY,         // Backup once per day
    WEEKLY,        // Backup once per week
    MANUAL         // User-triggered only
}

// Recommended: IMMEDIATE for high-value vouchers
if (voucher.getAmount() > 10_000) {  // $100+
    backupImmediately(voucher);
} else {
    scheduleBackup(BackupFrequency.DAILY);
}
```

### 5. Recovery Testing

**Test recovery regularly**:

```java
@Test
public void testVoucherBackupAndRecovery() throws Exception {
    // 1. Create test vouchers
    List<Proof<VoucherSecret>> original = createTestVouchers(5);

    // 2. Backup to Nostr
    backupService.backupVouchers(original, "test-password");

    // 3. Simulate device loss (clear local storage)
    walletDb.clear();

    // 4. Restore from Nostr
    List<Proof<VoucherSecret>> restored =
        backupService.restoreVouchers("test-password");

    // 5. Verify all vouchers recovered
    assertEquals(original.size(), restored.size());

    for (int i = 0; i < original.size(); i++) {
        assertProofEquals(original.get(i), restored.get(i));
    }
}
```

### 6. Expiry Management

**Proactive expiry handling**:

```java
public class VoucherExpiryManager {

    public void monitorExpiry() {
        List<Proof<VoucherSecret>> vouchers = walletDb.getVouchers();
        long now = Instant.now().getEpochSecond();

        for (Proof<VoucherSecret> voucher : vouchers) {
            Long expiry = voucher.getSecret().getExpiresAt();

            if (expiry != null) {
                long daysUntilExpiry = (expiry - now) / (24 * 3600);

                if (daysUntilExpiry <= 7) {
                    // Urgent: Expires within 7 days
                    notifyUser(voucher, "Expires in " + daysUntilExpiry + " days!");

                } else if (daysUntilExpiry <= 30) {
                    // Warning: Expires within 30 days
                    scheduleReminder(voucher, 7);  // Remind in 7 days
                }
            }
        }
    }

    public void autoRedeemExpiring() {
        // Option: Auto-redeem vouchers approaching expiry
        List<Proof<VoucherSecret>> expiringSoon = getExpiringSoon(7);

        if (preferences.isAutoRedeemEnabled()) {
            for (Proof<VoucherSecret> voucher : expiringSoon) {
                try {
                    redeemToRecoverable(voucher);
                    notifyUser("Auto-redeemed expiring voucher: $" +
                        voucher.getAmount() / 100.0);
                } catch (Exception e) {
                    notifyUser("Failed to auto-redeem voucher: " + e.getMessage());
                }
            }
        }
    }
}
```

---

## Conclusion

### Critical Points Summary

#### 1. Redemption Means Spending the Voucher

- ❌ **Cannot use voucher at merchant after redemption**
- ✅ **Get ecash that works everywhere instead**
- ⚠️ **Trade-off**: Flexibility vs. merchant acceptance

#### 2. Choose the Right Business Model

**Model A (Integrated)**:
- Same entity = merchant + mint
- Can offer redemption
- Medium regulatory burden

**Model B (No Redemption)**:
- Traditional gift cards
- Minimal regulation
- **Recommended starting point**

**Model C (Third-Party)**:
- ❌ **Never use** - regulatory nightmare

#### 3. Understand What the Mint Actually Does

When you redeem:
- Mint **validates** the voucher
- Mint **marks Cashu proof as spent** (unusable)
- Mint **issues ecash from own reserves**
- Mint **cannot** use voucher at merchant (proof is spent)
- Settlement (if needed) happens **outside Cashu protocol**

#### 4. Regulatory Implications are Real

- **Model A**: £350k capital + EMI license
- **Model B**: £5k-£20k compliance (gift cards)
- **Model C**: $2.5M-$24M licensing (don't do it!)

### Voucher Redemption Summary (Model A Only)

**In integrated merchant-mint model**:
- **Swap**: Convert voucher → new proofs (use deterministic for recovery!)
- **Melt**: Convert voucher → Lightning payment
- **Direct**: Use voucher as payment to merchant (before redemption)

**In voucher-only model (Model B)**:
- **Direct payment only**: Use at merchant, no conversion
- **No swap/melt**: Redemption not supported

### Nostr Backup Recommendation

**Use NIP-78 (Application-Specific Data)** because:
- ✅ Purpose-built for app data
- ✅ Replaceable (auto-updates)
- ✅ Easy to query
- ✅ Simple implementation
- ✅ Add encryption layer for security

**Backup flow**:
1. User receives voucher
2. Auto-backup to Nostr (encrypted with password)
3. User can restore from any device
4. Encouragement to redeem vouchers quickly (convert to recoverable)

---

**Document Version**: 1.0
**Last Updated**: 2025-11-03
**Status**: Implementation Ready
