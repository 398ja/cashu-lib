# Cashu Voucher - Comprehensive Test Plan

**Project**: cashu-voucher (multi-module)
**Date**: 2025-11-04
**Test Coverage Target**: 80%+
**Total Test Count**: 200+ tests

---

## Table of Contents

1. [Testing Strategy](#1-testing-strategy)
2. [Test Pyramid](#2-test-pyramid)
3. [Unit Tests](#3-unit-tests)
4. [Integration Tests](#4-integration-tests)
5. [End-to-End Tests](#5-end-to-end-tests)
6. [Performance Tests](#6-performance-tests)
7. [Security Tests](#7-security-tests)
8. [Test Data & Fixtures](#8-test-data--fixtures)
9. [CI/CD Integration](#9-cicd-integration)

---

## 1. Testing Strategy

### 1.1 Test Levels

| Level | Purpose | Tools | Count | Duration |
|-------|---------|-------|-------|----------|
| **Unit** | Test individual classes/methods | JUnit 5, Mockito | 130 | <10s |
| **Integration** | Test module interactions | Testcontainers, MockRelay | 50 | <60s |
| **E2E** | Test complete workflows | Testcontainers, real Nostr | 20 | <5min |
| **Performance** | Measure throughput/latency | JMH | 5 | varies |
| **Security** | Verify cryptographic correctness | Test vectors | 10 | <30s |

### 1.2 Testing Philosophy

**Hexagonal Architecture Benefits**:
```
Domain Layer → Pure unit tests (no mocks)
App Layer → Service tests (mock ports)
Nostr Layer → Integration tests (mock relay)
```

### 1.3 Test Naming Convention

```java
// Pattern: [method]_[scenario]_[expectedBehavior]

@Test
void create_withValidInputs_returnsVoucherSecret()

@Test
void verify_withInvalidSignature_returnsFalse()

@Test
void publish_whenRelayDown_retriesWithExponentialBackoff()
```

---

## 2. Test Pyramid

```
                    ▲
                   /│\
                  / │ \
                 /  │  \
                / 20│E2E\              5 min (full system)
               /────┴────\
              /    50    \
             / Integration \            1 min (mock relay)
            /───────────────\
           /                 \
          /       130         \         10 sec (pure logic)
         /        Unit         \
        /_______________________\
```

**Distribution**:
- 65% Unit tests (130 tests)
- 25% Integration tests (50 tests)
- 10% E2E tests (20 tests)

---

## 3. Unit Tests

### 3.1 Domain Layer Tests (cashu-voucher-domain)

#### 3.1.1 VoucherSecret Tests (20 tests)

**File**: `cashu-voucher-domain/src/test/java/xyz/tcheeric/cashu/voucher/domain/VoucherSecretTest.java`

```java
package xyz.tcheeric.cashu.voucher.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VoucherSecretTest {

    @Test
    void create_withValidInputs_returnsVoucherSecret() {
        VoucherSecret secret = VoucherSecret.create(
            "merchant123",
            "sat",
            5000L,
            null,
            "Holiday gift"
        );

        assertNotNull(secret);
        assertNotNull(secret.getVoucherId());
        assertEquals("merchant123", secret.getIssuerId());
        assertEquals(5000L, secret.getFaceValue());
    }

    @Test
    void create_withExplicitId_usesProvidedId() {
        String voucherId = "test-voucher-123";
        VoucherSecret secret = VoucherSecret.create(
            voucherId,
            "merchant123",
            "sat",
            5000L,
            null,
            null
        );

        assertEquals(voucherId, secret.getVoucherId());
    }

    @Test
    void toCanonicalBytes_withSameInputs_returnsIdenticalBytes() {
        VoucherSecret secret1 = VoucherSecret.create(
            "id1", "issuer", "sat", 5000L, null, "memo"
        );
        VoucherSecret secret2 = VoucherSecret.create(
            "id1", "issuer", "sat", 5000L, null, "memo"
        );

        assertArrayEquals(secret1.toCanonicalBytes(), secret2.toCanonicalBytes());
    }

    @Test
    void toCanonicalBytes_withDifferentInputs_returnsDifferentBytes() {
        VoucherSecret secret1 = VoucherSecret.create(
            "id1", "issuer", "sat", 5000L, null, "memo1"
        );
        VoucherSecret secret2 = VoucherSecret.create(
            "id2", "issuer", "sat", 5000L, null, "memo2"
        );

        assertFalse(Arrays.equals(secret1.toCanonicalBytes(), secret2.toCanonicalBytes()));
    }

    @Test
    void toHex_returnsValidHexString() {
        VoucherSecret secret = VoucherSecret.create(
            "merchant", "sat", 5000L, null, null
        );

        String hex = secret.toHex();

        assertNotNull(hex);
        assertTrue(hex.matches("^[0-9a-f]+$"));
    }

    @Test
    void isExpired_whenNotExpired_returnsFalse() {
        long futureTimestamp = Instant.now().plus(365, ChronoUnit.DAYS).getEpochSecond();
        VoucherSecret secret = VoucherSecret.create(
            "merchant", "sat", 5000L, futureTimestamp, null
        );

        assertFalse(secret.isExpired());
    }

    @Test
    void isExpired_whenExpired_returnsTrue() {
        long pastTimestamp = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
        VoucherSecret secret = VoucherSecret.create(
            "merchant", "sat", 5000L, pastTimestamp, null
        );

        assertTrue(secret.isExpired());
    }

    @Test
    void isExpired_whenNoExpiry_returnsFalse() {
        VoucherSecret secret = VoucherSecret.create(
            "merchant", "sat", 5000L, null, null
        );

        assertFalse(secret.isExpired());
    }

    @Test
    void setData_throwsUnsupportedOperationException() {
        VoucherSecret secret = VoucherSecret.create(
            "merchant", "sat", 5000L, null, null
        );

        assertThrows(UnsupportedOperationException.class, () -> {
            secret.setData(new byte[32]);
        });
    }

    @Test
    void getData_returnsCanonicalBytes() {
        VoucherSecret secret = VoucherSecret.create(
            "merchant", "sat", 5000L, null, null
        );

        assertArrayEquals(secret.toCanonicalBytes(), secret.getData());
    }

    // Additional tests:
    // - Serialization/deserialization (JSON)
    // - Edge cases (empty memo, zero value, etc.)
    // - Unicode in memo field
    // - Very large face values
    // - Boundary conditions for expiry timestamps
    // ... (10 more tests)
}
```

#### 3.1.2 SignedVoucher Tests (15 tests)

```java
class SignedVoucherTest {

    private static final String PRIVATE_KEY = "5c0c523f52a5b6fad39ed2403092df8cebc36318b39383bca6c00808626fab3a";
    private static final String PUBLIC_KEY = "9c2e42a9de0c5a00d7e1c9bb3d6fb06de8f5e3e76e3e7d6a5c4b3a2918171615";

    @Test
    void constructor_withValidInputs_createsSignedVoucher() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] signature = new byte[64];
        SignedVoucher voucher = new SignedVoucher(secret, signature, PUBLIC_KEY);

        assertNotNull(voucher);
        assertEquals(secret, voucher.getSecret());
        assertArrayEquals(signature, voucher.getSignature());
    }

    @Test
    void verify_withValidSignature_returnsTrue() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);
        SignedVoucher voucher = new SignedVoucher(secret, signature, PUBLIC_KEY);

        assertTrue(voucher.verify());
    }

    @Test
    void verify_withInvalidSignature_returnsFalse() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] invalidSignature = new byte[64];
        Arrays.fill(invalidSignature, (byte) 0xFF);
        SignedVoucher voucher = new SignedVoucher(secret, invalidSignature, PUBLIC_KEY);

        assertFalse(voucher.verify());
    }

    @Test
    void isExpired_delegatesToSecret() {
        long pastTimestamp = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, pastTimestamp, null);
        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);
        SignedVoucher voucher = new SignedVoucher(secret, signature, PUBLIC_KEY);

        assertTrue(voucher.isExpired());
    }

    @Test
    void isValid_whenValidAndNotExpired_returnsTrue() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);
        SignedVoucher voucher = new SignedVoucher(secret, signature, PUBLIC_KEY);

        assertTrue(voucher.isValid());
    }

    @Test
    void isValid_whenExpired_returnsFalse() {
        long pastTimestamp = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, pastTimestamp, null);
        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);
        SignedVoucher voucher = new SignedVoucher(secret, signature, PUBLIC_KEY);

        assertFalse(voucher.isValid());
    }

    // Additional tests:
    // - Invalid public key format
    // - Null signature handling
    // - Signature tampering detection
    // ... (9 more tests)
}
```

#### 3.1.3 VoucherSignatureService Tests (20 tests)

```java
class VoucherSignatureServiceTest {

    private static final String PRIVATE_KEY = "5c0c523f52a5b6fad39ed2403092df8cebc36318b39383bca6c00808626fab3a";
    private static final String PUBLIC_KEY = "9c2e42a9de0c5a00d7e1c9bb3d6fb06de8f5e3e76e3e7d6a5c4b3a2918171615";

    @Test
    void sign_withValidInputs_returns64ByteSignature() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);

        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);

        assertNotNull(signature);
        assertEquals(64, signature.length);
    }

    @Test
    void sign_withSameSecret_returnsDifferentSignatures() {
        // Note: ED25519 is deterministic, so same secret SHOULD return same signature
        VoucherSecret secret = VoucherSecret.create(
            "id1", "merchant", "sat", 5000L, null, null
        );

        byte[] sig1 = VoucherSignatureService.sign(secret, PRIVATE_KEY);
        byte[] sig2 = VoucherSignatureService.sign(secret, PRIVATE_KEY);

        assertArrayEquals(sig1, sig2);  // Deterministic
    }

    @Test
    void verify_withValidSignature_returnsTrue() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);

        boolean valid = VoucherSignatureService.verify(secret, signature, PUBLIC_KEY);

        assertTrue(valid);
    }

    @Test
    void verify_withTamperedSecret_returnsFalse() {
        VoucherSecret secret1 = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] signature = VoucherSignatureService.sign(secret1, PRIVATE_KEY);

        VoucherSecret secret2 = VoucherSecret.create("merchant", "sat", 6000L, null, null);
        boolean valid = VoucherSignatureService.verify(secret2, signature, PUBLIC_KEY);

        assertFalse(valid);
    }

    @Test
    void verify_withWrongPublicKey_returnsFalse() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);

        String wrongPubKey = "0000000000000000000000000000000000000000000000000000000000000000";
        boolean valid = VoucherSignatureService.verify(secret, signature, wrongPubKey);

        assertFalse(valid);
    }

    @Test
    void createSigned_returnsSignedVoucher() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);

        SignedVoucher signedVoucher = VoucherSignatureService.createSigned(
            secret, PRIVATE_KEY, PUBLIC_KEY
        );

        assertNotNull(signedVoucher);
        assertTrue(signedVoucher.verify());
    }

    // Additional tests:
    // - Invalid private key format
    // - Invalid public key format
    // - Empty signature
    // - Signature length verification
    // - Cross-verify with known test vectors
    // ... (14 more tests)
}
```

#### 3.1.4 VoucherValidator Tests (15 tests)

```java
class VoucherValidatorTest {

    private static final String PRIVATE_KEY = "5c0c523f52a5b6fad39ed2403092df8cebc36318b39383bca6c00808626fab3a";
    private static final String PUBLIC_KEY = "9c2e42a9de0c5a00d7e1c9bb3d6fb06de8f5e3e76e3e7d6a5c4b3a2918171615";

    @Test
    void validate_withValidVoucher_returnsSuccess() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        SignedVoucher voucher = VoucherSignatureService.createSigned(secret, PRIVATE_KEY, PUBLIC_KEY);

        ValidationResult result = VoucherValidator.validate(voucher);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validate_withInvalidSignature_returnsFailure() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] invalidSignature = new byte[64];
        SignedVoucher voucher = new SignedVoucher(secret, invalidSignature, PUBLIC_KEY);

        ValidationResult result = VoucherValidator.validate(voucher);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Invalid issuer signature"));
    }

    @Test
    void validate_withExpiredVoucher_returnsFailure() {
        long pastTimestamp = Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond();
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, pastTimestamp, null);
        SignedVoucher voucher = VoucherSignatureService.createSigned(secret, PRIVATE_KEY, PUBLIC_KEY);

        ValidationResult result = VoucherValidator.validate(voucher);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Voucher has expired"));
    }

    @Test
    void validate_withZeroValue_returnsFailure() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 0L, null, null);
        SignedVoucher voucher = VoucherSignatureService.createSigned(secret, PRIVATE_KEY, PUBLIC_KEY);

        ValidationResult result = VoucherValidator.validate(voucher);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Invalid face value: must be positive"));
    }

    @Test
    void validate_withNegativeValue_returnsFailure() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", -100L, null, null);
        SignedVoucher voucher = VoucherSignatureService.createSigned(secret, PRIVATE_KEY, PUBLIC_KEY);

        ValidationResult result = VoucherValidator.validate(voucher);

        assertFalse(result.isValid());
    }

    // Additional tests:
    // - Multiple validation errors
    // - Empty issuer ID
    // - Invalid unit
    // - Edge case timestamps
    // ... (10 more tests)
}
```

### 3.2 Application Layer Tests (cashu-voucher-app) - 40 tests

#### 3.2.1 VoucherService Tests (25 tests)

```java
class VoucherServiceTest {

    private VoucherLedgerPort mockLedgerPort;
    private VoucherBackupPort mockBackupPort;
    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        mockLedgerPort = mock(VoucherLedgerPort.class);
        mockBackupPort = mock(VoucherBackupPort.class);
        voucherService = new VoucherService(
            mockLedgerPort,
            mockBackupPort,
            PRIVATE_KEY,
            PUBLIC_KEY
        );
    }

    @Test
    void issue_withValidRequest_publishesToLedger() {
        IssueVoucherRequest request = IssueVoucherRequest.builder()
            .issuerId("merchant123")
            .amount(5000L)
            .unit("sat")
            .expiresInDays(365)
            .memo("Holiday gift")
            .build();

        IssueVoucherResponse response = voucherService.issue(request);

        assertNotNull(response);
        assertNotNull(response.getVoucher());
        verify(mockLedgerPort).publish(any(SignedVoucher.class), eq(VoucherStatus.ISSUED));
    }

    @Test
    void issue_calculatesExpiryCorrectly() {
        IssueVoucherRequest request = IssueVoucherRequest.builder()
            .issuerId("merchant123")
            .amount(5000L)
            .unit("sat")
            .expiresInDays(365)
            .build();

        IssueVoucherResponse response = voucherService.issue(request);

        Long expiresAt = response.getVoucher().getSecret().getExpiresAt();
        assertNotNull(expiresAt);

        long expectedExpiry = Instant.now().plus(365, ChronoUnit.DAYS).getEpochSecond();
        assertTrue(Math.abs(expiresAt - expectedExpiry) < 60);  // Within 1 minute
    }

    @Test
    void queryStatus_delegatesToPort() {
        String voucherId = "test-id";
        when(mockLedgerPort.queryStatus(voucherId))
            .thenReturn(Optional.of(VoucherStatus.ISSUED));

        Optional<VoucherStatus> status = voucherService.queryStatus(voucherId);

        assertTrue(status.isPresent());
        assertEquals(VoucherStatus.ISSUED, status.get());
        verify(mockLedgerPort).queryStatus(voucherId);
    }

    @Test
    void backup_delegatesToPort() {
        List<SignedVoucher> vouchers = List.of(
            createTestVoucher("id1"),
            createTestVoucher("id2")
        );
        String nostrKey = "test-nostr-key";

        voucherService.backup(vouchers, nostrKey);

        verify(mockBackupPort).backup(vouchers, nostrKey);
    }

    @Test
    void restore_delegatesToPort() {
        String nostrKey = "test-nostr-key";
        List<SignedVoucher> expectedVouchers = List.of(createTestVoucher("id1"));
        when(mockBackupPort.restore(nostrKey)).thenReturn(expectedVouchers);

        List<SignedVoucher> vouchers = voucherService.restore(nostrKey);

        assertEquals(expectedVouchers, vouchers);
        verify(mockBackupPort).restore(nostrKey);
    }

    // Additional tests:
    // - Null expiry handling
    // - Invalid amount handling
    // - Ledger port throws exception
    // - Backup port throws exception
    // - Token serialization
    // ... (20 more tests)
}
```

#### 3.2.2 MerchantVerificationService Tests (15 tests)

```java
class MerchantVerificationServiceTest {

    private VoucherLedgerPort mockLedgerPort;
    private MerchantVerificationService service;

    @BeforeEach
    void setUp() {
        mockLedgerPort = mock(VoucherLedgerPort.class);
        service = new MerchantVerificationService(mockLedgerPort);
    }

    @Test
    void verifyOffline_withValidVoucher_returnsSuccess() {
        SignedVoucher voucher = createTestVoucher("merchant123");

        VerificationResult result = service.verifyOffline(voucher, "merchant123");

        assertTrue(result.isValid());
    }

    @Test
    void verifyOffline_withWrongIssuer_returnsFailure() {
        SignedVoucher voucher = createTestVoucher("merchant123");

        VerificationResult result = service.verifyOffline(voucher, "merchant456");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Voucher issued by different merchant"));
    }

    @Test
    void verifyOnline_withValidVoucher_queriesLedger() {
        SignedVoucher voucher = createTestVoucher("merchant123");
        when(mockLedgerPort.queryStatus(voucher.getSecret().getVoucherId()))
            .thenReturn(Optional.of(VoucherStatus.ISSUED));

        VerificationResult result = service.verifyOnline(voucher, "merchant123");

        assertTrue(result.isValid());
        verify(mockLedgerPort).queryStatus(voucher.getSecret().getVoucherId());
    }

    @Test
    void verifyOnline_withRedeemedVoucher_returnsFailure() {
        SignedVoucher voucher = createTestVoucher("merchant123");
        when(mockLedgerPort.queryStatus(voucher.getSecret().getVoucherId()))
            .thenReturn(Optional.of(VoucherStatus.REDEEMED));

        VerificationResult result = service.verifyOnline(voucher, "merchant123");

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Voucher already redeemed (double-spend attempt)"));
    }

    // Additional tests:
    // - Revoked voucher
    // - Ledger not found
    // - Ledger query throws exception
    // - Offline check fails, online not called
    // ... (11 more tests)
}
```

### 3.3 Nostr Layer Tests (cashu-voucher-nostr) - 40 tests

#### 3.3.1 NostrVoucherLedgerRepository Tests (20 tests)

```java
class NostrVoucherLedgerRepositoryTest {

    private MockNostrClient mockNostrClient;
    private NostrVoucherLedgerRepository repository;

    @BeforeEach
    void setUp() {
        mockNostrClient = new MockNostrClient();
        repository = new NostrVoucherLedgerRepository(mockNostrClient, PUBLIC_KEY);
    }

    @Test
    void publish_createsNIP33Event() {
        SignedVoucher voucher = createTestVoucher("merchant123");

        repository.publish(voucher, VoucherStatus.ISSUED);

        List<GenericEvent> published = mockNostrClient.getPublishedEvents();
        assertEquals(1, published.size());

        GenericEvent event = published.get(0);
        assertEquals(30078, event.getKind());
        assertTrue(event.getTags().stream()
            .anyMatch(tag -> tag.getCode().equals("d") &&
                             tag.getValues().get(0).contains("voucher:")));
    }

    @Test
    void queryStatus_returnsCorrectStatus() {
        String voucherId = "test-id";
        GenericEvent mockEvent = createMockLedgerEvent(voucherId, "issued");
        mockNostrClient.addEvent(mockEvent);

        Optional<VoucherStatus> status = repository.queryStatus(voucherId);

        assertTrue(status.isPresent());
        assertEquals(VoucherStatus.ISSUED, status.get());
    }

    @Test
    void queryStatus_whenNotFound_returnsEmpty() {
        Optional<VoucherStatus> status = repository.queryStatus("nonexistent");

        assertTrue(status.isEmpty());
    }

    // Additional tests:
    // - Update status (replaceable event)
    // - Multiple events for same voucher (latest wins)
    // - Event parsing errors
    // - Relay connection failures
    // ... (17 more tests)
}
```

#### 3.3.2 Nostr VoucherBackupRepository Tests (20 tests)

```java
class NostrVoucherBackupRepositoryTest {

    private MockNostrClient mockNostrClient;
    private NostrVoucherBackupRepository repository;

    @BeforeEach
    void setUp() {
        mockNostrClient = new MockNostrClient();
        repository = new NostrVoucherBackupRepository(mockNostrClient);
    }

    @Test
    void backup_createsEncryptedNIP17Event() {
        List<SignedVoucher> vouchers = List.of(createTestVoucher("id1"));

        repository.backup(vouchers, NOSTR_PRIVATE_KEY);

        List<GenericEvent> published = mockNostrClient.getPublishedEvents();
        assertEquals(1, published.size());

        GenericEvent event = published.get(0);
        assertEquals(14, event.getKind());  // NIP-17 DM
        assertNotNull(event.getContent());  // Encrypted
    }

    @Test
    void restore_decryptsAndReturnsVouchers() {
        List<SignedVoucher> originalVouchers = List.of(
            createTestVoucher("id1"),
            createTestVoucher("id2")
        );

        // Backup first
        repository.backup(originalVouchers, NOSTR_PRIVATE_KEY);

        // Then restore
        List<SignedVoucher> restored = repository.restore(NOSTR_PRIVATE_KEY);

        assertEquals(2, restored.size());
        assertEquals("id1", restored.get(0).getSecret().getVoucherId());
    }

    @Test
    void restore_withMultipleBackups_deduplicates() {
        // Create two backups with overlapping vouchers
        repository.backup(List.of(createTestVoucher("id1")), NOSTR_PRIVATE_KEY);
        repository.backup(List.of(createTestVoucher("id1"), createTestVoucher("id2")), NOSTR_PRIVATE_KEY);

        List<SignedVoucher> restored = repository.restore(NOSTR_PRIVATE_KEY);

        assertEquals(2, restored.size());  // Deduplicated
    }

    // Additional tests:
    // - Empty backup
    // - Large backup (1000 vouchers)
    // - Encryption/decryption round-trip
    // - Invalid encryption key
    // - Corrupted encrypted payload
    // ... (17 more tests)
}
```

---

## 4. Integration Tests

### 4.1 Mint Integration Tests (15 tests)

**File**: `cashu-mint-protocol/src/test/java/xyz/tcheeric/cashu/mint/proto/VoucherIntegrationTest.java`

```java
@SpringBootTest
@Testcontainers
class VoucherIntegrationTest {

    @Container
    static MockNostrRelayContainer nostrRelay = new MockNostrRelayContainer();

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void issueVoucher_publishesToNostr_returnsToken() {
        IssueVoucherRequest request = new IssueVoucherRequest(
            "merchant123", 5000L, "sat", 365, "Gift"
        );

        ResponseEntity<IssueVoucherResponse> response = restTemplate.postForEntity(
            "/v1/vouchers",
            request,
            IssueVoucherResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getToken());

        // Verify Nostr event was published
        List<GenericEvent> events = nostrRelay.getPublishedEvents();
        assertEquals(1, events.size());
        assertEquals(30078, events.get(0).getKind());
    }

    @Test
    void swapWithVoucher_rejectsWithModelBError() {
        // Issue voucher
        SignedVoucher voucher = issueTestVoucher();
        Proof voucherProof = convertToProof(voucher);

        // Attempt swap
        SwapRequest swapRequest = new SwapRequest(
            List.of(voucherProof),
            List.of(new BlindedMessage(...))
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
            "/v1/swap",
            swapRequest,
            ErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Model B"));
    }

    // Additional tests:
    // - Query voucher status
    // - Multiple voucher issuance
    // - Concurrent issuance
    // - Relay failure handling
    // ... (13 more tests)
}
```

### 4.2 Wallet Integration Tests (20 tests)

```java
@SpringBootTest
@Testcontainers
class WalletVoucherIntegrationTest {

    @Container
    static MockNostrRelayContainer nostrRelay = new MockNostrRelayContainer();

    @Autowired
    private VoucherBackupService backupService;

    @Test
    void backupAndRestore_fullRoundTrip_preservesData() {
        List<SignedVoucher> originalVouchers = List.of(
            createTestVoucher("id1"),
            createTestVoucher("id2")
        );

        // Backup
        backupService.backup(originalVouchers, NOSTR_PRIVATE_KEY);

        // Wait for async completion
        await().atMost(5, SECONDS).until(() ->
            nostrRelay.getPublishedEvents().size() == 1
        );

        // Delete local state (simulate wallet loss)
        clearLocalStorage();

        // Restore
        List<SignedVoucher> restored = backupService.restore(NOSTR_PRIVATE_KEY);

        // Verify
        assertEquals(2, restored.size());
        assertEquals(originalVouchers.get(0).getSecret().getVoucherId(),
                     restored.get(0).getSecret().getVoucherId());
    }

    // Additional tests:
    // - Incremental backups
    // - Conflict resolution (timestamp)
    // - Backup failure retry
    // - Restore with no backups
    // - Multiple wallet restore
    // ... (19 more tests)
}
```

### 4.3 CLI Integration Tests (15 tests)

```java
@SpringBootTest
class VoucherCLIIntegrationTest {

    @Test
    void issueVoucherCommand_printsSuccess() {
        int exitCode = new CommandLine(new IssueVoucherCmd())
            .execute("--amount", "5000", "--memo", "Test");

        assertEquals(0, exitCode);
        // Verify output contains "✅ Voucher issued"
    }

    @Test
    void listVouchersCommand_displaysVouchers() {
        // Issue vouchers first
        issueTestVouchers(3);

        int exitCode = new CommandLine(new ListVouchersCmd())
            .execute();

        assertEquals(0, exitCode);
        // Verify output contains 3 vouchers
    }

    // Additional tests:
    // - Backup command
    // - Restore command
    // - Merchant verify command
    // - Error handling
    // ... (13 more tests)
}
```

---

## 5. End-to-End Tests

### 5.1 Full Lifecycle Test (5 tests)

```java
@SpringBootTest
@Testcontainers
class VoucherE2ETest {

    @Container
    static NostrRelayContainer nostrRelay = NostrRelayContainer.create();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Test
    void fullVoucherLifecycle_issueToRedemption() {
        // 1. Mint issues voucher
        IssueVoucherResponse issued = mintClient.issueVoucher(5000L, "Gift");
        String voucherId = issued.getVoucher().getSecret().getVoucherId();

        // 2. Verify published to Nostr ledger
        await().atMost(10, SECONDS).until(() ->
            nostrRelay.hasEvent(30078, "d", "voucher:" + voucherId)
        );

        // 3. Wallet auto-backups voucher
        await().atMost(10, SECONDS).until(() ->
            nostrRelay.hasEvent(14, "subject", "cashu-voucher-backup")
        );

        // 4. Simulate wallet loss
        walletService.clearLocalStorage();

        // 5. Restore from Nostr
        List<SignedVoucher> restored = walletService.restoreFromNostr();
        assertEquals(1, restored.size());
        assertEquals(voucherId, restored.get(0).getSecret().getVoucherId());

        // 6. Merchant verifies voucher
        VerificationResult verification = merchantService.verifyOnline(
            restored.get(0),
            "merchant123"
        );
        assertTrue(verification.isValid());

        // 7. Merchant redeems (updates Nostr ledger)
        merchantService.redeem(restored.get(0));

        // 8. Query status from Nostr
        await().atMost(10, SECONDS).until(() -> {
            VoucherStatus status = mintClient.queryStatus(voucherId);
            return status == VoucherStatus.REDEEMED;
        });
    }

    // Additional E2E tests:
    // - Multi-voucher scenario
    // - Concurrent redemption (double-spend attempt)
    // - Expired voucher rejection
    // - NUT-13 recovery with vouchers
    // ... (4 more tests)
}
```

---

## 6. Performance Tests

### 6.1 JMH Benchmarks

```java
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class VoucherPerformanceBenchmark {

    private static final String PRIVATE_KEY = "...";
    private static final String PUBLIC_KEY = "...";

    @Benchmark
    public void benchmarkSignature() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        VoucherSignatureService.sign(secret, PRIVATE_KEY);
    }

    @Benchmark
    public void benchmarkVerification() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);
        VoucherSignatureService.verify(secret, signature, PUBLIC_KEY);
    }

    @Benchmark
    public void benchmarkSerialization() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        secret.toCanonicalBytes();
    }

    @Benchmark
    public void benchmarkBackup_100Vouchers() {
        List<SignedVoucher> vouchers = createTestVouchers(100);
        VoucherBackupPayload payload = VoucherBackupPayload.fromVouchers(vouchers);
        payload.toJson();
    }

    @Benchmark
    public void benchmarkNIP44Encryption() {
        String payload = createTestPayload(100);
        MessageCipher44 cipher = new MessageCipher44(privateKey, publicKey);
        cipher.encrypt(payload);
    }
}
```

**Performance Targets**:
- Signature: > 1000 ops/sec
- Verification: > 2000 ops/sec
- Serialization: > 10000 ops/sec
- Backup (100 vouchers): > 100 ops/sec
- NIP-44 encryption: > 500 ops/sec

---

## 7. Security Tests

### 7.1 Cryptographic Test Vectors

```java
class VoucherSecurityTest {

    @Test
    void signatureVerification_withKnownTestVector_matches() {
        // Test vector from NUT-13 spec or RFC 8032
        String expectedSecret = "...";
        String expectedSignature = "...";
        String publicKey = "...";

        VoucherSecret secret = VoucherSecret.create(...);
        assertEquals(expectedSecret, secret.toHex());

        byte[] signature = HexUtils.decode(expectedSignature);
        assertTrue(VoucherSignatureService.verify(secret, signature, publicKey));
    }

    @Test
    void tampering_detectedBySignature() {
        VoucherSecret secret = VoucherSecret.create("merchant", "sat", 5000L, null, null);
        byte[] signature = VoucherSignatureService.sign(secret, PRIVATE_KEY);

        // Tamper with secret
        VoucherSecret tamperedSecret = VoucherSecret.create("merchant", "sat", 6000L, null, null);

        // Signature verification should fail
        assertFalse(VoucherSignatureService.verify(tamperedSecret, signature, PUBLIC_KEY));
    }

    @Test
    void nip44Encryption_withKnownTestVector_matches() {
        // Test vector from NIP-44
        String plaintext = "test message";
        String expectedCiphertext = "...";

        MessageCipher44 cipher = new MessageCipher44(privateKey, publicKey);
        String encrypted = cipher.encrypt(plaintext);

        assertEquals(expectedCiphertext, encrypted);
    }

    // Additional security tests:
    // - Key derivation test vectors
    // - Replay attack prevention
    // - Signature malleability
    // - Timing attack resistance
    // ... (7 more tests)
}
```

---

## 8. Test Data & Fixtures

### 8.1 Test Data Builders

```java
public class VoucherTestFixtures {

    public static final String TEST_PRIVATE_KEY = "5c0c523f52a5b6fad39ed2403092df8cebc36318b39383bca6c00808626fab3a";
    public static final String TEST_PUBLIC_KEY = "9c2e42a9de0c5a00d7e1c9bb3d6fb06de8f5e3e76e3e7d6a5c4b3a2918171615";

    public static VoucherSecret createTestSecret(String issuerId) {
        return VoucherSecret.create(issuerId, "sat", 5000L, null, "Test memo");
    }

    public static SignedVoucher createTestVoucher(String issuerId) {
        VoucherSecret secret = createTestSecret(issuerId);
        return VoucherSignatureService.createSigned(
            secret,
            TEST_PRIVATE_KEY,
            TEST_PUBLIC_KEY
        );
    }

    public static List<SignedVoucher> createTestVouchers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createTestVoucher("merchant" + i))
            .collect(Collectors.toList());
    }
}
```

### 8.2 Mock Nostr Relay

```java
public class MockNostrRelay implements NostrClientAdapter {

    private List<GenericEvent> publishedEvents = new ArrayList<>();
    private Map<Integer, List<GenericEvent>> eventsByKind = new HashMap<>();

    @Override
    public void publishEvent(GenericEvent event) {
        publishedEvents.add(event);
        eventsByKind
            .computeIfAbsent(event.getKind(), k -> new ArrayList<>())
            .add(event);
    }

    @Override
    public GenericEvent queryEvent(int kind, String author, String dTag) {
        return eventsByKind.getOrDefault(kind, List.of())
            .stream()
            .filter(e -> e.getTags().stream()
                .anyMatch(tag -> tag.getCode().equals("d") &&
                                 tag.getValues().get(0).equals(dTag)))
            .findFirst()
            .orElse(null);
    }

    public List<GenericEvent> getPublishedEvents() {
        return publishedEvents;
    }

    public void clear() {
        publishedEvents.clear();
        eventsByKind.clear();
    }
}
```

---

## 9. CI/CD Integration

### 9.1 GitHub Actions Workflow

```yaml
name: Voucher Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run Unit Tests
        run: mvn test -Dgroups="unit"
        timeout-minutes: 5

      - name: Run Integration Tests
        run: mvn test -Dgroups="integration"
        timeout-minutes: 15

      - name: Run E2E Tests
        run: mvn test -Dgroups="e2e"
        timeout-minutes: 30

      - name: Generate Coverage Report
        run: mvn jacoco:report

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml

      - name: Check Coverage Threshold
        run: |
          coverage=$(grep -oP 'Total.*?(\d+)%' target/site/jacoco/index.html | grep -oP '\d+')
          if [ $coverage -lt 80 ]; then
            echo "Coverage $coverage% is below 80% threshold"
            exit 1
          fi
```

### 9.2 Test Groups

```java
// JUnit 5 tags for test categorization

@Tag("unit")
class VoucherSecretTest { ... }

@Tag("integration")
class VoucherIntegrationTest { ... }

@Tag("e2e")
class VoucherE2ETest { ... }

@Tag("performance")
class VoucherPerformanceBenchmark { ... }

@Tag("security")
class VoucherSecurityTest { ... }
```

---

## Summary

**Test Distribution**:
- **130 Unit Tests** (65%) - Fast, pure logic, no I/O
- **50 Integration Tests** (25%) - Module interactions, mock relay
- **20 E2E Tests** (10%) - Full system, real Nostr
- **5 Performance Tests** - Benchmarking
- **10 Security Tests** - Cryptographic correctness

**Coverage Target**: 80%+ across all modules

**Execution Time**:
- Unit: <10 seconds
- Integration: <60 seconds
- E2E: <5 minutes
- **Total**: <7 minutes

**CI/CD**: Automated on every push, coverage enforced

---

**Document Version**: 1.0
**Last Updated**: 2025-11-04
**Related**: gift-card-plan-final-v2.md, voucher-architecture-diagrams.md
