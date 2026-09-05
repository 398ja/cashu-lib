package xyz.tcheeric.cashu.common.nut11;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A locktime is a Unix timestamp, so it must survive past 2038.
 *
 * <p>{@code getLockTime()} used to return {@code int}. Narrowing {@code 4102444800}
 * (2100-01-01) to 32 bits yields {@code -192522496}, and every caller phrases the check as
 * "locktime set and now past it", so a negative value read as long expired. A proof that looked
 * locked until 2100 was immediately claimable through the refund path, or by anyone when no
 * refund keys were present.
 */
@DisplayName("P2PK locktime survives 32-bit overflow")
class P2PKSecretLockTimeOverflowTest {

    /** A real point on secp256k1; the generator. */
    private static final String PUBKEY =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    private static String wireWithLockTime(String locktimeJson) {
        return "[\"P2PK\",{\"nonce\":\"" + "aa".repeat(16)
                + "\",\"data\":\"" + PUBKEY + "\","
                + "\"tags\":[[\"locktime\"," + locktimeJson + "]]}]";
    }

    @Test
    @DisplayName("a year-2100 locktime is read at full width, not wrapped negative")
    void year2100SurvivesOffTheWire() {
        Secret parsed = SecretUtil.toSecret(wireWithLockTime("4102444800"));

        assertThat(parsed).isInstanceOf(P2PKSecret.class);
        assertThat(((P2PKSecret) parsed).getLockTime())
                .as("a 2100 timestamp must not narrow to a negative int")
                .isEqualTo(4102444800L);
    }

    @Test
    @DisplayName("a year-2100 locktime set directly is also full width")
    void year2100SetDirectly() {
        P2PKSecret secret = new P2PKSecret();
        secret.setTag(P2PKSecret.P2PKTag.locktime.name(), List.of(4102444800L));

        assertThat(secret.getLockTime()).isEqualTo(4102444800L);
    }

    @Test
    @DisplayName("a locktime past 2038 is still in the future, so the lock holds")
    void postEpochLockIsNotTreatedAsExpired() {
        P2PKSecret secret = new P2PKSecret();
        secret.setTag(P2PKSecret.P2PKTag.locktime.name(), List.of(4102444800L));

        long now = System.currentTimeMillis() / 1000;
        boolean hasPassed = secret.getLockTime() > 0 && secret.getLockTime() < now;

        assertThat(hasPassed)
                .as("2100 has not passed; the proof must remain locked")
                .isFalse();
    }

    @Test
    @DisplayName("a negative locktime is malformed, not silently accepted")
    void negativeLockTimeIsRejected() {
        assertThatThrownBy(() -> SecretUtil.toSecret(wireWithLockTime("-1")))
                .isInstanceOf(MalformedP2PKSecretException.class)
                .hasMessageContaining("locktime");
    }

    @Test
    @DisplayName("a non-numeric locktime is malformed, not defaulted to 0")
    void nonNumericLockTimeIsRejected() {
        assertThatThrownBy(() -> SecretUtil.toSecret(wireWithLockTime("\"abc\"")))
                .isInstanceOf(MalformedP2PKSecretException.class)
                .hasMessageContaining("locktime");
    }

    @Test
    @DisplayName("an ordinary locktime still parses")
    void ordinaryLockTimeStillParses() {
        Secret parsed = SecretUtil.toSecret(wireWithLockTime("1893456000"));

        assertThat(((P2PKSecret) parsed).getLockTime()).isEqualTo(1893456000L);
    }

    @Test
    @DisplayName("an absent locktime reads as 0")
    void absentLockTimeIsZero() {
        P2PKSecret secret = new P2PKSecret();

        assertThat(secret.getLockTime()).isZero();
    }
}
