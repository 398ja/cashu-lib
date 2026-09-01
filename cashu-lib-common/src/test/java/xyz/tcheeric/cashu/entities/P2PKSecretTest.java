package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class P2PKSecretTest {

    // Real compressed secp256k1 points (G, 2G, 3G, 4G). These fixtures previously used
    // "deadbeef" as the lock and "pk1"/"refund1" as keys, which NUT-11 rejects — the
    // constructors and key setters now validate, so the placeholders no longer parse.
    private static final String KEY_LOCK =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String KEY_2 =
            "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";
    private static final String KEY_3 =
            "02f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9";
    private static final String KEY_4 =
            "02e493dbf1c10d80f3581e4904930b1404cc6c13900ee0758474fa94abe8c4cd13";

    private static P2PKSecret lock() {
        return new P2PKSecret(Hex.decode(KEY_LOCK));
    }

    /**
     * Ensures P2PK secrets round-trip through JSON serialization with all metadata intact.
     */
    @Test
    void shouldRoundTripSerializeP2PKSecret() throws Exception {
        // Arrange
        P2PKSecret secret = lock();
        secret.setNSigs(2); // lock key + one extra = 2 keys in the main pathway
        secret.setSigFlag(P2PKSecret.SignatureFlag.SIG_ALL);
        secret.addPubKey(KEY_2);
        secret.setLockTime(42);
        secret.addRefund(KEY_3);
        ObjectMapper mapper = new ObjectMapper();

        // Act
        String json = mapper.writeValueAsString(secret);
        WellKnownSecret deserialized = mapper.readValue(json, WellKnownSecret.class);

        // Assert
        assertThat(deserialized).isEqualTo(secret);
    }

    /**
     * NUT-11 refund-path signature threshold defaults to 1 when the n_sigs_refund
     * tag is absent, preserving current 1-of-N refund behavior for existing escrows.
     */
    @Test
    void shouldDefaultNSigsRefundToOneWhenUnset() {
        // Arrange
        P2PKSecret secret = lock();

        // Act
        int nSigsRefund = secret.getNSigsRefund();

        // Assert
        assertThat(nSigsRefund).isEqualTo(1);
    }

    /**
     * Ensures setNSigsRefund persists the refund-path threshold and getNSigsRefund
     * reads it back once explicitly set.
     */
    @Test
    void shouldSetAndGetNSigsRefund() {
        // Arrange
        P2PKSecret secret = lock();

        // Act
        secret.setNSigsRefund(2);

        // Assert
        assertThat(secret.getNSigsRefund()).isEqualTo(2);
    }

    /**
     * A malformed secret whose {@code n_sigs_refund} tag carries no value (a key-only tag array,
     * which the deserializers accept) must fall back to the default of 1 rather than throwing
     * {@link IndexOutOfBoundsException} — otherwise a crafted secret could DoS verification.
     */
    @Test
    void shouldDefaultNSigsRefundToOneWhenTagHasNoValue() {
        // Arrange
        P2PKSecret secret = lock();
        secret.setTag(P2PKSecret.P2PKTag.n_sigs_refund.name(), List.of()); // present but empty

        // Act / Assert
        assertThat(secret.getNSigsRefund()).isEqualTo(1);
    }

    /**
     * Ensures the n_sigs_refund tag round-trips through JSON serialization the
     * same way the existing n_sigs tag does.
     *
     * <p>Three refund keys, not one: NUT-11 makes a threshold exceeding its pathway's key count
     * malformed, so an n_sigs_refund of 3 against a single refund key no longer deserializes.
     */
    @Test
    void shouldRoundTripSerializeNSigsRefund() throws Exception {
        // Arrange
        P2PKSecret secret = lock();
        secret.setNSigs(2);
        secret.setSigFlag(P2PKSecret.SignatureFlag.SIG_ALL);
        secret.addPubKey(KEY_2);
        secret.setLockTime(42);
        secret.setRefund(List.of(KEY_2, KEY_3, KEY_4));
        secret.setNSigsRefund(3);
        ObjectMapper mapper = new ObjectMapper();

        // Act
        String json = mapper.writeValueAsString(secret);
        WellKnownSecret deserialized = mapper.readValue(json, WellKnownSecret.class);

        // Assert
        assertThat(deserialized).isEqualTo(secret);
        assertThat(((P2PKSecret) deserialized).getNSigsRefund()).isEqualTo(3);
    }
}
