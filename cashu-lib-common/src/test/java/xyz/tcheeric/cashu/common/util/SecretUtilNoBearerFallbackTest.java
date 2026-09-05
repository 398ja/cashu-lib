package xyz.tcheeric.cashu.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut11.MalformedP2PKSecretException;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A secret that declares a NUT-10 kind must never come back as an unlocked bearer secret.
 *
 * <p>The parse path in {@link SecretUtil#toSecret(Object)} is how a mint reads a proof off the
 * wire. It used to fall through to {@link RandomStringSecret} whenever building the structured
 * secret threw anything other than {@link MalformedP2PKSecretException}. A NUT-00 random string
 * carries no spending condition at all, and {@code Y} is derived from the same wire bytes either
 * way, so the mint would accept a swap of a proof whose lock it had silently discarded. The
 * sender, who knows the secret string, could then spend it without a witness.
 *
 * <p>These tests pin the boundary: declared kind means structured, and a structured secret that
 * cannot be built is refused rather than downgraded.
 */
@DisplayName("SecretUtil never downgrades a locked secret to a bearer secret")
class SecretUtilNoBearerFallbackTest {

    /** A real point on secp256k1; the generator. */
    private static final String PUBKEY =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    @Nested
    @DisplayName("a declared kind that cannot be built is refused")
    class DeclaredKindIsRefused {

        @Test
        @DisplayName("HTLC, a valid Kind with no implementation, is refused not unlocked")
        void htlcIsRefused() {
            String wire = "[\"HTLC\",{\"nonce\":\"" + "aa".repeat(16)
                    + "\",\"data\":\"" + "02" + "bb".repeat(32) + "\",\"tags\":[]}]";

            assertThatThrownBy(() -> SecretUtil.toSecret(wire))
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("HTLC");
        }

        @Test
        @DisplayName("an unknown kind is refused not unlocked")
        void unknownKindIsRefused() {
            String wire = "[\"NOT_A_KIND\",{\"nonce\":\"" + "aa".repeat(16)
                    + "\",\"data\":\"" + "02" + "bb".repeat(32) + "\",\"tags\":[]}]";

            assertThatThrownBy(() -> SecretUtil.toSecret(wire))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("a lowercase kind is refused, not silently treated as a bearer string")
        void lowercaseKindIsRefused() {
            String wire = "[\"p2pk\",{\"nonce\":\"" + "aa".repeat(16)
                    + "\",\"data\":\"" + "02" + "bb".repeat(32) + "\",\"tags\":[]}]";

            assertThatThrownBy(() -> SecretUtil.toSecret(wire))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }
    }

    @Nested
    @DisplayName("an unrecognised sigflag reaches validate() as a typed rejection")
    class SigFlagIsValidatedNotSwallowed {

        @Test
        @DisplayName("lowercase sig_all is refused, not turned into a bearer secret")
        void lowercaseSigFlagIsRefused() {
            String wire = "[\"P2PK\",{\"nonce\":\"" + "aa".repeat(16)
                    + "\",\"data\":\"02" + "bb".repeat(32) + "\","
                    + "\"tags\":[[\"sigflag\",\"sig_all\"]]}]";

            // Previously: SignatureFlag.valueOf threw a bare IllegalArgumentException during tag
            // conversion, before validate() ran, and the caller downgraded the whole secret.
            assertThatThrownBy(() -> SecretUtil.toSecret(wire))
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("a well-formed SIG_ALL still parses")
        void wellFormedSigFlagStillParses() {
            String wire = "[\"P2PK\",{\"nonce\":\"" + "aa".repeat(16)
                    + "\",\"data\":\"" + PUBKEY + "\","
                    + "\"tags\":[[\"sigflag\",\"SIG_ALL\"]]}]";

            Secret parsed = SecretUtil.toSecret(wire);

            assertThat(parsed).isInstanceOf(P2PKSecret.class);
            assertThat(((P2PKSecret) parsed).getSigFlag()).isEqualTo("SIG_ALL");
        }
    }

    @Nested
    @DisplayName("genuine bearer secrets are unaffected")
    class BearerSecretsStillParse {

        @Test
        @DisplayName("a plain hex string is still a RandomStringSecret")
        void plainHexIsBearer() {
            String hex = "ab".repeat(32);

            Secret parsed = SecretUtil.toSecret(hex);

            assertThat(parsed).isInstanceOf(RandomStringSecret.class);
        }

        @Test
        @DisplayName("a bracketed string that is not JSON is still a bearer secret")
        void bracketedNonJsonIsBearer() {
            // Starts with '[' and ends with ']' but never claimed a kind.
            String notJson = "[not json at all]";

            assertThatCode(() -> SecretUtil.toSecret(notJson)).doesNotThrowAnyException();

            Secret parsed = SecretUtil.toSecret(notJson);

            assertThat(parsed).isInstanceOf(RandomStringSecret.class);
        }

        @Test
        @DisplayName("a JSON array whose first element is not a string is a bearer secret")
        void numericFirstElementIsBearer() {
            String wire = "[1,2,3]";

            Secret parsed = SecretUtil.toSecret(wire);

            assertThat(parsed).isInstanceOf(RandomStringSecret.class);
        }
    }
}
