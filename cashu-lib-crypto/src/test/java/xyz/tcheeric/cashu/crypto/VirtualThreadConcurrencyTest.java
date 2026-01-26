package xyz.tcheeric.cashu.crypto;

import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.crypto.util.KeysUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Virtual Thread concurrency tests for crypto operations.
 *
 * These tests verify thread-safety of BDHKE and related crypto operations
 * when executed concurrently on 100+ Virtual Threads. Run with
 * {@code -Djdk.tracePinnedThreads=full} to detect any pinning issues.
 */
class VirtualThreadConcurrencyTest {

    private static final int CONCURRENT_OPERATIONS = 100;
    private static final SecP256K1Curve CURVE = new SecP256K1Curve();

    /**
     * Verifies hashToCurve is thread-safe under concurrent Virtual Thread execution.
     * Each VT computes a hash for a unique secret; all should complete without error.
     */
    @Test
    void hashToCurveShouldBeThreadSafeUnderConcurrentVirtualThreads() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<byte[]>> futures = IntStream.range(0, CONCURRENT_OPERATIONS)
                    .mapToObj(i -> executor.submit(() -> {
                        // Use byte[] overload to avoid hex parsing issues
                        byte[] secret = ("concurrent_secret_" + i).getBytes(StandardCharsets.UTF_8);
                        ECPoint result = BDHKEUtils.hashToCurve(secret);
                        return result.getEncoded(true);
                    }))
                    .toList();

            for (int i = 0; i < futures.size(); i++) {
                byte[] result = futures.get(i).get(10, TimeUnit.SECONDS);
                assertNotNull(result, "hashToCurve result should not be null for index " + i);
                assertEquals(33, result.length, "Compressed point should be 33 bytes");
            }
        }
    }

    /**
     * Verifies blind message creation is thread-safe under concurrent Virtual Thread execution.
     */
    @Test
    void blindMessageShouldBeThreadSafeUnderConcurrentVirtualThreads() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<byte[][]>> futures = IntStream.range(0, CONCURRENT_OPERATIONS)
                    .mapToObj(i -> executor.submit(() -> {
                        byte[] secret = ("blind_secret_" + i).getBytes(StandardCharsets.UTF_8);
                        return BDHKEUtils.blindMessage(secret);
                    }))
                    .toList();

            for (int i = 0; i < futures.size(); i++) {
                byte[][] result = futures.get(i).get(10, TimeUnit.SECONDS);
                assertNotNull(result, "blindMessage result should not be null for index " + i);
                assertEquals(2, result.length, "blindMessage should return [B_, r]");
                assertEquals(64, result[0].length, "B_ should be 64 bytes (uncompressed without prefix)");
                assertEquals(32, result[1].length, "r should be 32 bytes");
            }
        }
    }

    /**
     * Verifies sign and verify roundtrip is thread-safe under concurrent Virtual Thread execution.
     */
    @Test
    void signAndVerifyShouldBeThreadSafeUnderConcurrentVirtualThreads() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> futures = IntStream.range(0, CONCURRENT_OPERATIONS)
                    .mapToObj(i -> executor.submit(() -> {
                        // Generate unique key and secret for each thread
                        byte[] privateKey = KeysUtils.generatePrivateKey();
                        // Use NUT-10 JSON format (starts with '[') to avoid hex parsing
                        String secret = "[\"P2PK\",{\"nonce\":\"" + i + "\",\"data\":\"test\"}]";

                        // Compute Y = hash_to_curve(secret)
                        byte[] Y = BDHKEUtils.hashToCurve(secret);

                        // Sign: C = k * Y
                        byte[] C = BDHKEUtils.signBlindedMessage(Y, privateKey);

                        // Verify: C == k * hash_to_curve(secret)
                        return BDHKEUtils.verify(secret, privateKey, C);
                    }))
                    .toList();

            for (int i = 0; i < futures.size(); i++) {
                Boolean verified = futures.get(i).get(10, TimeUnit.SECONDS);
                assertTrue(verified, "Verification should succeed for index " + i);
            }
        }
    }

    /**
     * Verifies full BDHKE blind signing protocol is thread-safe under concurrent Virtual Thread execution.
     */
    @Test
    void fullBlindSigningProtocolShouldBeThreadSafeUnderConcurrentVirtualThreads() throws Exception {
        // Mint's private key (shared across all operations, simulating real usage)
        byte[] mintPrivateKey = KeysUtils.generatePrivateKey();
        byte[] mintPublicKey = KeysUtils.derivePublicKey(mintPrivateKey);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> futures = IntStream.range(0, CONCURRENT_OPERATIONS)
                    .mapToObj(i -> executor.submit(() -> {
                        // Client: create secret using NUT-10 JSON format
                        String secretStr = "[\"P2PK\",{\"nonce\":\"full_" + i + "\",\"data\":\"protocol\"}]";
                        byte[] secret = secretStr.getBytes(StandardCharsets.UTF_8);
                        byte[][] blindResult = BDHKEUtils.blindMessage(secret);
                        byte[] B_ = blindResult[0];
                        byte[] r = blindResult[1];

                        // Need to add 0x04 prefix for uncompressed point
                        byte[] B_withPrefix = new byte[65];
                        B_withPrefix[0] = 0x04;
                        System.arraycopy(B_, 0, B_withPrefix, 1, 64);

                        // Mint: sign the blinded message
                        byte[] C_ = BDHKEUtils.signBlindedMessage(B_withPrefix, mintPrivateKey);

                        // Client: unblind the signature
                        byte[] C = BDHKEUtils.unblindSignature(C_, r, mintPublicKey);

                        // Verify the unblinded signature
                        return BDHKEUtils.verify(secretStr, mintPrivateKey, C);
                    }))
                    .toList();

            for (int i = 0; i < futures.size(); i++) {
                Boolean verified = futures.get(i).get(10, TimeUnit.SECONDS);
                assertTrue(verified, "Full protocol verification should succeed for index " + i);
            }
        }
    }

    /**
     * Verifies DLEQ proof generation and verification is thread-safe under concurrent Virtual Thread execution.
     */
    @Test
    void dleqProofShouldBeThreadSafeUnderConcurrentVirtualThreads() throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> futures = IntStream.range(0, CONCURRENT_OPERATIONS)
                    .mapToObj(i -> executor.submit(() -> {
                        byte[] privateKeyBytes = KeysUtils.generatePrivateKey();
                        BigInteger privateKey = Utils.bigIntFromBytes(privateKeyBytes);
                        byte[] secret = ("dleq_secret_" + i).getBytes(StandardCharsets.UTF_8);

                        // Create blinded message
                        byte[][] blindResult = BDHKEUtils.blindMessage(secret);
                        byte[] B_ = blindResult[0];

                        // Add prefix for decoding as EC point
                        byte[] B_withPrefix = new byte[65];
                        B_withPrefix[0] = 0x04;
                        System.arraycopy(B_, 0, B_withPrefix, 1, 64);
                        ECPoint blindedMessage = CURVE.decodePoint(B_withPrefix);

                        // Sign
                        byte[] C_bytes = BDHKEUtils.signBlindedMessage(B_withPrefix, privateKeyBytes);
                        ECPoint blindSignature = CURVE.decodePoint(C_bytes);

                        // Generate DLEQ proof
                        DLEQUtils.DLEQProofResult proof = DLEQUtils.generateProof(privateKey, blindedMessage, blindSignature);

                        // Derive public key for verification
                        byte[] pubKeyBytes = KeysUtils.derivePublicKey(privateKeyBytes);
                        ECPoint publicKey = CURVE.decodePoint(pubKeyBytes);

                        // Verify DLEQ proof
                        return DLEQUtils.verifyProof(proof.e(), proof.s(), blindedMessage, blindSignature, publicKey);
                    }))
                    .toList();

            for (int i = 0; i < futures.size(); i++) {
                Boolean verified = futures.get(i).get(10, TimeUnit.SECONDS);
                assertTrue(verified, "DLEQ proof verification should succeed for index " + i);
            }
        }
    }
}
