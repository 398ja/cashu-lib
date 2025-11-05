Review of Simple Gift Card Nostr Plan

I've completed a comprehensive review of your implementation plan. Here are my findings and recommendations:

Overall Assessment: Strong Foundation with Areas for Improvement

The plan is well-structured and comprehensive, but needs refinement in several critical areas before implementation.

  ---
Critical Issues to Address

1. NIP Selection Inconsistency ⚠️

Problem: The plan mixes NIP-17 (§4, §6 Phase 2) with NIP-78 (§10) without clarifying which is primary.

- Section 4: Describes NIP-17 (kind 14 private DMs) for wallet backups
- Section 10: Describes NIP-33/NIP-78 (kind 30078) for voucher ledger
- VOUCHER-REDEMPTION-AND-BACKUP.md (line 840-1150): Recommends NIP-78 as primary

Recommendation:
## Clarified Nostr Strategy

### Wallet Backups (User Private Data)
- **Use NIP-78** (kind 30078) with encryption
- Replaceable, queryable, simple implementation
- Stores voucher proofs for recovery

### Voucher Ledger (Public Audit Trail)
- **Use NIP-33** (kind 30000-39999) parameterized replaceable
- Public state transitions (issued → redeemed)
- Mint-operated for voucher lifecycle tracking

2. cashu-voucher Project Scope Unclear ⚠️

Problem: Phase 0 proposes creating a new cashu-voucher project, but:
- No clear boundaries between cashu-voucher, cashu-lib-entities, and cashu-lib-common
- Risk of circular dependencies
- Current project structure: /home/eric/IdeaProjects/cashu-lib/pom.xml:11-15

Recommendation:
### Option A: New Module Within cashu-lib (Recommended)
cashu-lib/
├── cashu-lib-entities/     # Core Cashu types (Proof, BlindedMessage)
├── cashu-lib-crypto/       # Cryptographic primitives
├── cashu-lib-common/       # Common utilities
└── cashu-lib-vouchers/     # NEW: Voucher-specific types
├── VoucherSecret
├── VoucherSignatureService
├── VoucherValidator
└── VoucherBackupService

### Option B: Standalone (If Separate Versioning Needed)
cashu-voucher/             # Separate repository
└── depends on cashu-lib-entities, cashu-lib-crypto

Justification: Given you already have 3 modules in cashu-lib, adding a 4th is cleaner than creating a new top-level project.

3. Security: Key Management Missing 🔴

Problem: Section 5 mentions key protection but lacks concrete implementation details.

Add to Phase 2:
### 2.1 Nostr Key Management
- **Storage**: Encrypted keystore using AES-256-GCM
    - Derive encryption key from user password via PBKDF2 (600k iterations)
    - Store in platform-secure location (KeyStore on Android, Keychain on iOS)

- **Key Derivation**: Option to derive Nostr key from Cashu mnemonic
  m/purpose'/coin_type'/account'/nostr'/0'

- **Backup**: Nostr private key must be backed up separately
- NOT recoverable from Cashu mnemonic alone (unless derived)
- Export as nsec1... (NIP-19) for user backup

4. Regulatory Model Decision Required 🔴

Problem: Plan doesn't commit to Model A or Model B (section 3.2 lists both).

Decision Required Before Phase 1:
## Recommended Path

### Phase 0-1: Implement Model B (No Redemption)
- Vouchers ONLY spendable at issuing merchant
- Mint REJECTS vouchers in swap/melt
- Minimal regulation (gift card treatment)
- Fast time to market

### Phase 2+: Optional Evolution to Model A
- IF business case requires redemption
- AFTER securing appropriate licenses (EMI/MSB)
- Clear migration path for existing vouchers

Justification: Model B aligns with §8 Risks & Mitigations ("user loses Nostr key") and provides clearest legal framework.

  ---
Technical Improvements

5. NUT-13 Integration ✅

Observation: Based on recent commits (fa2ad01, 8e50999), you're implementing NUT-13 (deterministic secrets). Excellent alignment!

Enhancement:
### 3.3 Wallet Layer - Hybrid Recovery Strategy

1. **Deterministic Proofs** (NUT-13): Recoverable from mnemonic
2. **Voucher Proofs**: NOT deterministic, require Nostr backup
3. **Redemption Flow**:
    - Swap voucher → deterministic proofs (NUT-13)
    - Result: User has recoverable balance after redemption

6. Database Schema Needed ⚠️

Problem: Phase 0 mentions DB schema but doesn't provide it.

Add to Phase 0:
-- Mint voucher ledger
CREATE TABLE voucher_ledger (
voucher_id VARCHAR(64) PRIMARY KEY,
issuer_id VARCHAR(255) NOT NULL,
unit VARCHAR(10) NOT NULL,
face_value BIGINT NOT NULL,
issued_at BIGINT NOT NULL,
expires_at BIGINT,
status VARCHAR(20) NOT NULL, -- ISSUED, REDEEMED, REVOKED
proof_y_hash VARCHAR(64) UNIQUE, -- Y value hash for double-spend check
redeemed_at BIGINT,
nostr_event_id VARCHAR(64), -- Ledger event ID
INDEX idx_issuer_status (issuer_id, status),
INDEX idx_expires_at (expires_at)
);

-- Wallet voucher store
CREATE TABLE wallet_vouchers (
id SERIAL PRIMARY KEY,
voucher_id VARCHAR(64) NOT NULL,
proof_json TEXT NOT NULL, -- Full proof including VoucherSecret
blinding_factor BYTEA, -- For unblinding
status VARCHAR(20) NOT NULL, -- ACTIVE, SPENT, EXPIRED
last_backup_event_id VARCHAR(64),
last_backup_at BIGINT,
received_at BIGINT NOT NULL,
INDEX idx_voucher_id (voucher_id),
INDEX idx_status (status)
);

7. Testing Strategy Incomplete ⚠️

Enhancement:
### 7. Testing Strategy (Enhanced)

#### Unit Tests (cashu-lib-vouchers)
- VoucherSecret canonical serialization
- Signature generation/verification
- Expiry validation
- NIP-44 encryption/decryption

#### Integration Tests (Testcontainers)
- **Scenario 1**: Issue → Backup → Restore → Redeem
- **Scenario 2**: Issue → Redeem → Verify ledger updated
- **Scenario 3**: Expired voucher rejection
- **Scenario 4**: Double-spend prevention
- **Scenario 5**: Model B rejection (voucher in swap)

#### Test Vectors (from NUT-13)
- Reuse NUT-13 test vector pattern
- Provide deterministic voucher test cases
- Cross-implementation compatibility

  ---
Implementation Phase Recommendations

8. Phase Sequencing Refinement

Current Phase 0 is Too Large. Split into:

### Phase 0A – Project Structure (Week 1)
1. Decision: cashu-lib-vouchers module vs. standalone
2. Create module structure with Maven config
3. CI pipeline extension (GitHub Actions)
4. Dependency management (publish to maven.398ja.xyz)

### Phase 0B – Domain Model (Week 2)
1. VoucherSecret implementation
2. Signing utilities (reuse bcprov-jdk18on from pom.xml:94)
3. Canonical serialization (CBOR via jackson, pom.xml:53)
4. Unit tests (JUnit 5, pom.xml:107)

### Phase 0C – Configuration & DB (Week 3)
1. Config flags (voucher.redemption.mode)
2. DB schema + Flyway migrations
3. Integration test setup (Testcontainers)

9. Missing: Nostr Library Selection 🔴

Problem: Phase 2 says "select Nostr relay library" (§9) but this blocks Phase 0 planning.

Action Required Now:
## Nostr Library Recommendation (JVM)

### Option 1: nostr-java (Recommended)
- Repository: github.com/tcheeric/nostr-java (or similar)
- Pros: Native Java, NIP-01/NIP-04/NIP-44 support
- Cons: May need NIP-78 implementation

### Option 2: Implement Minimal Client
- Websocket: Java 11+ HttpClient
- NIP-01: Event signing via bcprov (already in deps)
- NIP-44: Implement using existing crypto module
- ~500 LOC for basic backup functionality

### Decision Required: Phase 0A

10. Operational Considerations Missing

Add to Phase 3:
### 3.6 – Operational Procedures

#### Relay Management
- Health checks for configured relays
- Automatic failover on relay downtime
- Backup queue for offline publish
- Monitoring: backup success rate, restore latency

#### Voucher Reconciliation
- Daily: Compare mint ledger vs. Nostr events
- Alert on discrepancies (missing events)
- Retry failed publishes from archive

#### Emergency Procedures
- Voucher revocation (issuer compromise)
- Mass expiry extension (force majeure)
- Nostr key rotation (security incident)

  ---
Documentation Improvements

11. Section 11 (Project Integration) Needs Expansion

Add:
## 11. Project Integration Overview (Enhanced)

### Dependency Graph
cashu-lib-vouchers
↓ depends on
cashu-lib-entities (Proof, Secret, Tag)
cashu-lib-crypto (BDHKE, signing)
↓ consumed by
cashu-mint (issuance, validation)
cashu-wallet (backup, restore)
cashu-client (merchant SDK)

### API Contracts
- **Mint Issuance**: POST /v1/vouchers
- **Mint Validation**: Internal (swap/melt hooks)
- **Wallet Backup**: NostrBackupService.backup()
- **Ledger Query**: GET /v1/vouchers/{id}/status

12. Risk: Nostr Relay Centralization ⚠️

Add to Section 8:
### Risk: Relay Censorship/Failure
- **Threat**: All configured relays ban/filter voucher events
- **Mitigation**:
    1. Operate self-hosted relay (high priority)
    2. Encrypted content prevents keyword filtering
    3. Use generic kind (30078) not voucher-specific
    4. Fallback: Export to encrypted file

  ---
Positive Aspects ✅

1. Excellent regulatory analysis (§8, VOUCHER-REDEMPTION-AND-BACKUP.md)
2. Clear Model B recommendation aligns with minimal viable product
3. Nostr backup strategy is well-researched (NIP comparison matrix)
4. Security considerations mention key points (§5)
5. References VOUCHER-SPENDING-CONDITIONS-ANALYSIS.md shows thorough design thinking
6. Integration with existing work (NUT-13 from commits)

  ---
Final Recommendations

● Before Starting Implementation

1. Decide NIP Strategy: NIP-78 for backups + NIP-33 for ledger (recommended)
2. Decide Project Structure: cashu-lib-vouchers module (recommended)
3. Commit to Business Model: Start with Model B (no redemption)
4. Select Nostr Library: Evaluate nostr-java or minimal implementation
5. Add DB Schema: Include complete DDL in Phase 0 deliverables
6. Expand Testing: Add test vectors and Model B rejection tests
7. Key Management: Design Nostr key derivation/storage strategy
8. Operational Plan: Add relay monitoring and reconciliation procedures

Priority Order

HIGH PRIORITY (Blocking):
- [1] Business model decision (Model A vs B)
- [2] Project structure (module vs standalone)
- [3] Nostr library selection
- [4] NIP clarification (17 vs 78)

MEDIUM PRIORITY (Phase 0):
- [5] Database schema
- [6] Key management design
- [7] Split Phase 0 into 0A/0B/0C

LOW PRIORITY (Phase 2+):
- [8] Operational procedures
- [9] Enhanced testing strategy
- [10] Relay redundancy planning

Estimated Timeline (After Decisions)

- Phase 0: 3 weeks (split into A/B/C)
- Phase 1: 4 weeks (mint + wallet + client)
- Phase 2: 3 weeks (Nostr backup implementation)
- Phase 3: 2 weeks (UX, ops, docs)
- Total: ~12 weeks for MVP (Model B)

  ---
The plan is ambitious but achievable. Address the critical issues (NIP selection, project structure, business model) before writing code, and you'll have a solid foundation for
implementation.
