# Virtual Thread Compatibility

This document describes the thread-safety characteristics of cashu-lib modules for use with Java 21+ Virtual Threads (Project Loom).

## Summary

**cashu-lib is Virtual Thread compatible.** All public APIs are thread-safe and do not exhibit behaviors that would cause Virtual Thread pinning or resource contention issues.

| Module | VT Safe | Notes |
|--------|---------|-------|
| cashu-lib-common | Yes | No synchronized blocks or ThreadLocal usage |
| cashu-lib-entities | Yes | Immutable DTOs, no threading concerns |
| cashu-lib-crypto | Yes | Thread-safe crypto operations |

## Audit Results

### Synchronized Blocks

**Finding: None**

No `synchronized` blocks were found in any cashu-lib module. This means:
- No risk of Virtual Thread carrier pinning
- No contention on monitors during I/O operations

### ThreadLocal Usage

**Finding: None**

No `ThreadLocal` fields were found. This means:
- No unexpected state inheritance issues with Virtual Threads
- No memory leak risks from Virtual Thread pooling

### Bouncy Castle Usage

The crypto module uses Bouncy Castle for elliptic curve operations. The usage patterns are VT-safe:

| Class | Pattern | Thread Safety |
|-------|---------|---------------|
| `BDHKEUtils` | `MessageDigest.getInstance()` per call | Safe - new instance each call |
| `BDHKEUtils` | Static `SecP256K1Curve` field | Safe - read-only operations |
| `DLEQUtils` | Static `SecureRandom` field | Safe - internally synchronized, short operations |
| `Schnorr` | `SecureRandom.getInstanceStrong()` | Safe - new instance each call |
| `KeysUtils` | Provider registration in static block | Safe - one-time initialization |

### CPU-Bound Operations

The cryptographic operations in cashu-lib are CPU-bound (elliptic curve math, hashing). These operations:
- Do not perform blocking I/O
- Complete in microseconds to low milliseconds
- Will not cause significant carrier thread monopolization

For high-throughput scenarios with many concurrent signing operations, consider:
1. Monitoring carrier thread utilization via JFR
2. If needed, offloading to a dedicated platform thread pool

## Recommendations for Consumers

### cashu-mint and cashu-wallet

When using cashu-lib with Virtual Threads enabled:

1. **No special configuration needed** - cashu-lib APIs can be called directly from Virtual Threads
2. **Batch operations are safe** - Multiple concurrent `BDHKEUtils.verify()` or signing calls work correctly
3. **No pinning detection alerts expected** - The library won't trigger `-Djdk.tracePinnedThreads` warnings

### Testing with Virtual Threads

To verify thread-safety in your integration:

```java
@Test
void concurrentSigningOperations() throws Exception {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<byte[]>> futures = IntStream.range(0, 100)
            .mapToObj(i -> executor.submit(() -> {
                byte[] secret = ("secret" + i).getBytes(StandardCharsets.UTF_8);
                return BDHKEUtils.hashToCurve(secret);
            }))
            .toList();

        // All operations should complete without error
        for (var future : futures) {
            assertNotNull(future.get(5, TimeUnit.SECONDS));
        }
    }
}
```

### Pinning Detection in CI

Add this to your test configuration to detect any future regressions:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Djdk.tracePinnedThreads=full</argLine>
    </configuration>
</plugin>
```

## Version History

| Version | Status | Notes |
|---------|--------|-------|
| 0.12.0+ | VT Compatible | @ThreadSafe annotations, CI pinning detection |
| 0.11.1+ | VT Compatible | Initial VT compatibility audit completed |
