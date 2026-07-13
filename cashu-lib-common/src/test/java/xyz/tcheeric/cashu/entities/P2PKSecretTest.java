package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class P2PKSecretTest {

    @Test
    /**
     * Ensures P2PK secrets round-trip through JSON serialization with all metadata intact.
     */
    void shouldRoundTripSerializeP2PKSecret() throws Exception {
        // Arrange
        byte[] secretData = Hex.decode("deadbeef");
        P2PKSecret secret = new P2PKSecret(secretData);
        secret.setNSigs(2);
        secret.setSigFlag(P2PKSecret.SignatureFlag.SIG_ALL);
        secret.addPubKey("pk1");
        secret.setLockTime(42);
        secret.addRefund("refund1");
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
        byte[] secretData = Hex.decode("deadbeef");
        P2PKSecret secret = new P2PKSecret(secretData);

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
        byte[] secretData = Hex.decode("deadbeef");
        P2PKSecret secret = new P2PKSecret(secretData);

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
        byte[] secretData = Hex.decode("deadbeef");
        P2PKSecret secret = new P2PKSecret(secretData);
        secret.setTag(P2PKSecret.P2PKTag.n_sigs_refund.name(), List.of()); // present but empty

        // Act / Assert
        assertThat(secret.getNSigsRefund()).isEqualTo(1);
    }

    /**
     * Ensures the n_sigs_refund tag round-trips through JSON serialization the
     * same way the existing n_sigs tag does.
     */
    @Test
    void shouldRoundTripSerializeNSigsRefund() throws Exception {
        // Arrange
        byte[] secretData = Hex.decode("deadbeef");
        P2PKSecret secret = new P2PKSecret(secretData);
        secret.setNSigs(2);
        secret.setNSigsRefund(3);
        secret.setSigFlag(P2PKSecret.SignatureFlag.SIG_ALL);
        secret.addPubKey("pk1");
        secret.setLockTime(42);
        secret.addRefund("refund1");
        ObjectMapper mapper = new ObjectMapper();

        // Act
        String json = mapper.writeValueAsString(secret);
        WellKnownSecret deserialized = mapper.readValue(json, WellKnownSecret.class);

        // Assert
        assertThat(deserialized).isEqualTo(secret);
        assertThat(((P2PKSecret) deserialized).getNSigsRefund()).isEqualTo(3);
    }
}
