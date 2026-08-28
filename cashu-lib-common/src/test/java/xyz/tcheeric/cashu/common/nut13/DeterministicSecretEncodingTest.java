package xyz.tcheeric.cashu.common.nut13;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.KeysetId;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The agreement between what a deterministic secret hashes to and what it is transmitted as.
 *
 * <p>Every other test in this repository mints and verifies within one process, using the same
 * bytes on both sides, so all of them pass whichever bytes {@code getData()} returns. The
 * question that matters is the one a third party asks: given the secret string on the wire,
 * which curve point does it hash to? These tests ask that question.
 */
class DeterministicSecretEncodingTest {

    private static final byte[] DERIVED_BYTES = new byte[]{
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
            (byte) 0xee, (byte) 0xff,
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd,
            (byte) 0xee, (byte) 0xff};

    private static DeterministicSecret secret() {
        return DeterministicSecret.create(DERIVED_BYTES, KeysetId.fromString("009a1f293253e41e"), 0);
    }

    /**
     * Ensures the bytes hashed to the curve are the UTF-8 bytes of the string the mint receives,
     * so a wallet blinds against the same curve point it later asks the mint about. When these
     * disagreed, a spent proof was reported as spendable balance and NUT-13 restore recovered
     * nothing, both silently.
     */
    @Test
    void shouldHashTheBytesOfTheStringItIsTransmittedAs() {
        // Arrange
        DeterministicSecret secret = secret();

        // Act
        byte[] hashedBytes = secret.getData();

        // Assert
        assertThat(hashedBytes).isEqualTo(secret.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Ensures the raw derivation output stays reachable, since NUT-13 recovery needs the derived
     * bytes themselves rather than the encoded form.
     */
    @Test
    void shouldKeepTheDerivedBytesReachable() {
        // Arrange
        DeterministicSecret secret = secret();

        // Act
        byte[] derivedBytes = secret.getDerivedBytes();

        // Assert
        assertThat(derivedBytes).isEqualTo(DERIVED_BYTES);
    }

    /**
     * Ensures the hashed bytes are the 64 hex characters rather than the 32 bytes they decode to,
     * which is the exact confusion that made the wallet and the mint disagree.
     */
    @Test
    void shouldHashTheHexCharactersRatherThanTheBytesTheyDecodeTo() {
        // Arrange
        DeterministicSecret secret = secret();

        // Act
        byte[] hashedBytes = secret.getData();

        // Assert
        assertThat(hashedBytes).hasSize(64);
        assertThat(secret.getDerivedBytes()).hasSize(32);
    }
}
