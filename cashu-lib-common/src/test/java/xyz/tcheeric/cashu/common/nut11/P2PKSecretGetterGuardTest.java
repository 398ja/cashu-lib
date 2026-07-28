package xyz.tcheeric.cashu.common.nut11;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * A tag that is present but carries no value is accepted by both deserializers, so a crafted
 * secret can reach these getters. {@code getNSigsRefund} already guards this case and documents
 * why — a malformed secret must not be able to throw {@link IndexOutOfBoundsException} out of a
 * getter and DoS verification. Its three siblings never received the same guard.
 */
class P2PKSecretGetterGuardTest {

    private static final String KEY =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    private static P2PKSecret secret() {
        return new P2PKSecret(Hex.decode(KEY));
    }

    @Nested
    @DisplayName("tag present but empty falls back to the absent-tag default")
    class EmptyValues {

        @Test
        @DisplayName("getNSigs returns -1 rather than throwing")
        void nSigs() {
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.n_sigs.name(), List.of());
            assertThat(s.getNSigs()).isEqualTo(-1);
        }

        @Test
        @DisplayName("getSigFlag returns null rather than throwing")
        void sigFlag() {
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.sigflag.name(), List.of());
            assertThat(s.getSigFlag()).isNull();
        }

        @Test
        @DisplayName("getLockTime returns 0 rather than throwing")
        void lockTime() {
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.locktime.name(), List.of());
            assertThat(s.getLockTime()).isZero();
        }

        @Test
        @DisplayName("getNSigsRefund keeps its existing behaviour")
        void nSigsRefundUnchanged() {
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.n_sigs_refund.name(), List.of());
            assertThat(s.getNSigsRefund()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("numeric tag values do not blow up on the cast")
    class NumericValues {

        @Test
        @DisplayName("getNSigs reads a Long, as the NUT-10 deserializer produces")
        void nSigsFromLong() {
            // deserializeNut10Format stores integral JSON as longValue().
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.n_sigs.name(), List.of(2L));
            assertThat(s.getNSigs()).isEqualTo(2);
        }

        @Test
        @DisplayName("getLockTime reads a Long")
        void lockTimeFromLong() {
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.locktime.name(), List.of(1893456000L));
            assertThat(s.getLockTime()).isEqualTo(1893456000);
        }
    }

    @Nested
    @DisplayName("unchecked list casts")
    class ListCasts {

        @Test
        @DisplayName("getPubKeys degrades a numeric entry to a string instead of ClassCastException")
        void pubKeys() {
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.pubkeys.name(), List.of(42L));
            assertThatCode(() -> assertThat(s.getPubKeys()).containsExactly("42"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("getRefund degrades a numeric entry to a string instead of ClassCastException")
        void refund() {
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.refund.name(), List.of(42L));
            assertThatCode(() -> assertThat(s.getRefund()).containsExactly("42"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("sigflag arriving as a raw string")
    class StringSigFlag {

        @Test
        @DisplayName("getSigFlag tolerates a String value instead of ClassCastException")
        void tolerated() {
            // A hand-built or partially-coerced secret can hold the raw string. Rejecting an
            // unknown flag is validate()'s job; the getter must not crash first.
            P2PKSecret s = secret();
            s.setTag(P2PKSecret.P2PKTag.sigflag.name(), List.of("SIG_ALL"));
            assertThat(s.getSigFlag()).isEqualTo("SIG_ALL");
        }

        @Test
        @DisplayName("getSigFlag still reads an enum value")
        void enumStillWorks() {
            P2PKSecret s = secret();
            s.setSigFlag(P2PKSecret.SignatureFlag.SIG_ALL);
            assertThat(s.getSigFlag()).isEqualTo("SIG_ALL");
        }
    }
}
