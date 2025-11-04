# Cashu Voucher - Deployment Guide

**Project**: cashu-voucher
**Version**: 0.1.0
**Date**: 2025-11-04

---

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Nostr relay access (self-hosted recommended)

### Installation

```bash
# Build all modules
cd cashu-voucher
mvn clean install

# Publish to local Maven repo
mvn install

# Publish to maven.398ja.xyz
mvn deploy -P release
```

---

## Configuration

### 1. Mint Configuration

**File**: `cashu-mint/src/main/resources/application-voucher.yml`

```yaml
voucher:
  mint:
    enabled: true
    issuerPrivateKey: ${MINT_VOUCHER_ISSUER_PRIVKEY}  # ED25519 private key (32 bytes hex)
    issuerPublicKey: ${MINT_VOUCHER_ISSUER_PUBKEY}    # ED25519 public key (32 bytes hex)

  model:
    type: MODEL_B  # Vouchers not redeemable at mint

  nostr:
    enabled: true
    relays:
      - wss://relay.yourorg.com     # Self-hosted (primary)
      - wss://relay.damus.io         # Public (backup)
      - wss://relay.primal.net       # Public (backup)
    publishTimeout: 10s
    retryAttempts: 3
    retryBackoff: exponential
```

### 2. Wallet Configuration

**File**: `cashu-wallet/src/main/resources/application.yml`

```yaml
voucher:
  wallet:
    enabled: true
    autoBackup: true
    backupIntervalMinutes: 60

  nostr:
    relays:
      - wss://relay.yourorg.com
      - wss://relay.damus.io
    encryptionEnabled: true  # NIP-44
```

### 3. Generate Issuer Keys

```bash
# Using openssl
openssl rand -hex 32  # Private key
# Derive public key using nostr-java or bip-utils
```

---

## Deployment Architectures

### Architecture 1: Self-Hosted Relay (Recommended)

```
┌─────────────────┐
│  cashu-mint     │
│  (your server)  │
│  Port: 8080     │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│  strfry Nostr Relay         │
│  (self-hosted)              │
│  wss://relay.yourorg.com    │
│  - Guaranteed uptime        │
│  - Full control             │
│  - No censorship            │
└─────────────────────────────┘
```

**Setup strfry relay**:
```bash
# Install
git clone https://github.com/hoytech/strfry.git
cd strfry && make setup-golpe && make

# Configure
cat > strfry.conf <<EOF
relay {
    bind = "0.0.0.0"
    port = 7777
}
storage {
    dbParams {
        path = "/var/lib/strfry/strfry.db"
    }
}
EOF

# Run
./strfry relay
```

### Architecture 2: Public Relays Only

```
┌─────────────────┐
│  cashu-mint     │
└────────┬────────┘
         │
         ├─────► relay.damus.io
         ├─────► relay.primal.net
         └─────► relay.nostr.band
```

**Trade-offs**:
- ✅ No infrastructure to maintain
- ❌ Dependent on public relay uptime
- ❌ Potential censorship
- ❌ Rate limiting

---

## Docker Deployment

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/cashu-mint-*.jar app.jar
COPY application-voucher.yml /app/config/

ENV SPRING_PROFILES_ACTIVE=voucher
ENV MINT_VOUCHER_ISSUER_PRIVKEY=${MINT_VOUCHER_ISSUER_PRIVKEY}
ENV MINT_VOUCHER_ISSUER_PUBKEY=${MINT_VOUCHER_ISSUER_PUBKEY}

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.additional-location=file:/app/config/"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  cashu-mint:
    build: .
    ports:
      - "8080:8080"
    environment:
      - MINT_VOUCHER_ISSUER_PRIVKEY=${MINT_VOUCHER_ISSUER_PRIVKEY}
      - MINT_VOUCHER_ISSUER_PUBKEY=${MINT_VOUCHER_ISSUER_PUBKEY}
    depends_on:
      - strfry-relay

  strfry-relay:
    image: ghcr.io/hoytech/strfry:latest
    ports:
      - "7777:7777"
    volumes:
      - strfry-data:/var/lib/strfry
      - ./strfry.conf:/etc/strfry.conf

volumes:
  strfry-data:
```

---

## Monitoring

### Health Checks

```yaml
# Spring Boot Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus

  metrics:
    export:
      prometheus:
        enabled: true
```

### Key Metrics

```
# Voucher issuance rate
voucher_issued_total
voucher_issued_rate

# Nostr publish success/failure
nostr_publish_success_total
nostr_publish_failure_total
nostr_publish_latency_seconds

# Backup metrics
voucher_backup_total
voucher_backup_latency_seconds
voucher_restore_total
```

### Alerting Rules (Prometheus)

```yaml
groups:
  - name: voucher_alerts
    rules:
      - alert: NostrPublishFailureHigh
        expr: rate(nostr_publish_failure_total[5m]) > 0.1
        annotations:
          summary: "High Nostr publish failure rate"

      - alert: VoucherBackupFailing
        expr: rate(voucher_backup_failure_total[10m]) > 0
        annotations:
          summary: "Voucher backups failing"
```

---

## Security Hardening

### 1. Key Protection

```bash
# Store keys in environment variables (not config files)
export MINT_VOUCHER_ISSUER_PRIVKEY=$(cat /secure/path/mint-privkey.hex)

# Or use Docker secrets
docker secret create mint_privkey /secure/path/mint-privkey.hex
```

### 2. Network Security

```yaml
# Restrict API access
server:
  ssl:
    enabled: true
    key-store: /path/to/keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
```

### 3. Rate Limiting

```yaml
voucher:
  rateLimit:
    enabled: true
    maxIssuancePerMinute: 100
    maxIssuancePerHour: 1000
```

---

## Backup & Recovery

### Relay Data Backup

```bash
# Backup strfry database
sqlite3 /var/lib/strfry/strfry.db ".backup /backup/strfry-$(date +%Y%m%d).db"

# Restore
cp /backup/strfry-20250104.db /var/lib/strfry/strfry.db
```

### Mint Key Backup

```bash
# Backup issuer keys (CRITICAL!)
echo $MINT_VOUCHER_ISSUER_PRIVKEY > /secure/backup/mint-privkey-$(date +%Y%m%d).hex

# Test recovery
export MINT_VOUCHER_ISSUER_PRIVKEY=$(cat /secure/backup/mint-privkey-20250104.hex)
```

---

## Troubleshooting

### Issue: Nostr publish failures

```bash
# Check relay connectivity
wscat -c wss://relay.yourorg.com

# Test event publishing
curl -X POST wss://relay.yourorg.com \
  -H "Content-Type: application/json" \
  -d '{"kind":1,"content":"test"}'
```

### Issue: Voucher not found in ledger

```bash
# Query relay directly
wscat -c wss://relay.yourorg.com
> ["REQ", "test", {"kinds":[30078],"#d":["voucher:123"]}]
```

### Issue: Backup restoration failing

```bash
# Check Nostr private key
cashu wallet nostr-key-info

# Verify events exist
cashu voucher backup-status
```

---

## Production Checklist

- [ ] Self-hosted Nostr relay configured
- [ ] Issuer keys generated and backed up securely
- [ ] SSL/TLS enabled for API
- [ ] Rate limiting configured
- [ ] Monitoring and alerting set up
- [ ] Backup procedures tested
- [ ] Recovery procedures documented
- [ ] Security audit completed

---

**Document Version**: 1.0
**Last Updated**: 2025-11-04
