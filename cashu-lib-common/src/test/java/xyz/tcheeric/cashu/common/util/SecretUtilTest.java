package xyz.tcheeric.cashu.common.util;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;
import xyz.tcheeric.cashu.common.nut11.P2PKVoucherSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherSecret;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretUtilTest {

    private static final String HEX_SECRET =
            "0000000000000000000000000000000000000000000000000000000000000001";

    /** Y for the 32 raw bytes the secret string hex-decodes to; the pre-migration commitment. */
    private static final String LEGACY_Y_FOR_HEX_SECRET =
            "022e7158e11c9506f1aa4248bf531298daa7febd6194f003edcd9b93ade6253acf";

    /**
     * Ensures a plain secret is UTF-8 encoded before hash_to_curve, as NUT-00 requires, and no
     * longer hex-decoded into the different point the library used to commit to.
     */
    @Test
    void shouldComputeYFromHexSecretUsingUtf8Encoding() {
        RandomStringSecret secret = RandomStringSecret.fromString(HEX_SECRET);

        String actualY = SecretUtil.toY(secret);
        String expectedY = PublicKey.fromBytes(
                BDHKEUtils.hashToCurve(HEX_SECRET.getBytes(StandardCharsets.UTF_8)).getEncoded(true)).toString();

        assertThat(actualY).isEqualTo(expectedY);
        assertThat(actualY).isNotEqualTo(LEGACY_Y_FOR_HEX_SECRET);
    }

    /**
     * Ensures NUT-10 secrets use UTF-8 string hashing rather than hex decoding.
     */
    @Test
    void shouldComputeYFromNut10SecretUsingUtf8Encoding() {
        VoucherSecret secret = new VoucherSecret();
        secret.setData(Hex.decode("deadbeef"));
        secret.setNonce("nonce-123");

        String secretString = secret.toString();
        String expectedY = PublicKey.fromBytes(BDHKEUtils.hashToCurve(secretString)).toString();

        assertThat(SecretUtil.toY(secret)).isEqualTo(expectedY);
    }

    /**
     * Ensures toYFromString and toY agree for the same secret representation.
     */
    @Test
    void shouldMatchToYFromStringWithToY() {
        RandomStringSecret secret = RandomStringSecret.fromString(HEX_SECRET);

        String fromSecret = SecretUtil.toY(secret);
        String fromString = SecretUtil.toYFromString(secret.toString());

        assertThat(fromString).isEqualTo(fromSecret);
    }

    /**
     * Regression test: ensures SecretUtil.toSecret() preserves null nonce from NUT-10 JSON.
     * This is critical for BDHKE verification to work correctly.
     */
    @Test
    void shouldPreserveNullNonceFromNut10JsonArray() {
        // Given: NUT-10 JSON with null nonce
        String json = "[\"VOUCHER\",\"746573742d64617461\",null,[]]";

        // When: Parse to Secret
        Secret secret = SecretUtil.toSecret(json);

        // Then: Nonce should be Java null, not the string "null"
        assertThat(secret).isInstanceOf(WellKnownSecret.class);
        WellKnownSecret wks = (WellKnownSecret) secret;
        assertThat(wks.getNonce())
                .withFailMessage("Nonce should be Java null, not string 'null'")
                .isNull();

        // And: Re-serialized JSON should contain JSON null, not string "null"
        String output = secret.toString();
        assertThat(output)
                .withFailMessage("Output should contain JSON null: %s", output)
                .contains(",null,");
        assertThat(output)
                .withFailMessage("Output should NOT contain string 'null': %s", output)
                .doesNotContain(",\"null\",");
    }

    /**
     * Ensures that a string nonce is preserved correctly.
     */
    @Test
    void shouldPreserveStringNonceFromNut10JsonArray() {
        // Given: NUT-10 JSON with string nonce
        String json = "[\"VOUCHER\",\"746573742d64617461\",\"my-nonce-123\",[]]";

        // When: Parse to Secret
        Secret secret = SecretUtil.toSecret(json);

        // Then: Nonce should be preserved
        assertThat(secret).isInstanceOf(WellKnownSecret.class);
        WellKnownSecret wks = (WellKnownSecret) secret;
        assertThat(wks.getNonce()).isEqualTo("my-nonce-123");
    }

    /**
     * A P2PK_VOUCHER read off the wire must come back as a LOCKED secret.
     *
     * <p>This is the mint's parse path. Before this case existed it threw
     * "Unsupported secret kind: P2PK_VOUCHER", and the caller fell back to a
     * spending condition that checks no lock — so a locked proof was accepted
     * and spent with {@code witness=null}. Observed against a real mint, which
     * returned 200 for exactly that request.</p>
     *
     * <p>The bug was invisible from inside cashu-lib: every unit test built its
     * secrets with {@code new P2PKVoucherSecret(...)} rather than parsing one,
     * so the one route a mint actually uses was the one route untested.</p>
     */
    @Test
    void parsesAP2pkVoucherAsALockedSecret() {
        String spendingKey =
                "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
        String json = "[\"P2PK_VOUCHER\",{\"nonce\":\"" + "ab".repeat(16)
                + "\",\"data\":\"" + spendingKey
                + "\",\"tags\":[[\"issuer\",\"test-issuer\"],[\"unit\",\"sat\"]]}]";

        Secret parsed = SecretUtil.toSecret(json);

        // A P2PKSecret, so VerifyProofsTask routes it to the condition that
        // checks the witness rather than to the voucher-only one.
        assertThat(parsed).isInstanceOf(P2PKVoucherSecret.class);

        P2PKVoucherSecret locked = (P2PKVoucherSecret) parsed;
        assertThat(locked.getKind()).isEqualTo(WellKnownSecret.Kind.P2PK_VOUCHER);
        // The lock survives the parse. Losing it here would leave a secret that
        // looks locked and enforces nothing.
        assertThat(Hex.toHexString(locked.getData())).isEqualTo(spendingKey);
        // And so do the voucher fields, which the voucher half of the condition
        // needs.
        assertThat(locked.getIssuerId()).isEqualTo("test-issuer");
        assertThat(locked.getUnit()).isEqualTo("sat");
    }

    @Test
    void refusesAP2pkVoucherWhoseLockIsNotAKey() {
        // `validated()` runs P2PKSecret.validate() on this path, so a malformed
        // lock is refused at parse rather than becoming an unspendable proof.
        String json = "[\"P2PK_VOUCHER\",{\"nonce\":\"" + "ab".repeat(16)
                + "\",\"data\":\"" + "00".repeat(33) + "\",\"tags\":[]}]";

        assertThatThrownBy(() -> SecretUtil.toSecret(json))
                .isInstanceOf(RuntimeException.class);
    }
    /**
     * A P2PK_VOUCHER's spending conditions must read the same as a P2PK's.
     *
     * <p>{@code addTagsToSecret} calls {@code convertP2PKTagValues} only when
     * {@code kind == P2PK}, so a P2PK_VOUCHER keeps its tag values as the raw
     * JSON types: {@code sigflag} stays a String rather than becoming a
     * SignatureFlag, and {@code locktime}/{@code n_sigs} stay Double rather
     * than Integer. P2PKVoucherSecret extends P2PKSecret and inherits every
     * rule that reads them, so a divergence here would mean one kind enforcing
     * a locktime the other ignores.
     *
     * <p>The accessors happen to normalise both forms today
     * ({@code intValue} parses a String, {@code getSigFlag} handles either), so
     * this is currently latent rather than exploitable. Asserted so it stays
     * that way: anything that starts reading a tag value directly would break
     * silently, and silently is how the parse gap that preceded this test
     * behaved.
     */
    @Test
    void aP2pkVoucherReportsTheSameSpendingConditionsAsAP2pk() {
        String spendingKey =
                "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
        // n_sigs=1 with one key: a threshold above the key count is correctly
        // rejected by validate(), which this test is not about.
        // locktime needs refund keys, or P2PKVoucherSecret refuses the secret
        // outright — a past locktime with no refund path silently unlocks it.
        String refundKey = "02c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";
        String tags = "[[\"sigflag\",\"SIG_ALL\"],[\"n_sigs\",1],[\"locktime\",1700000000],"
                + "[\"refund\",\"" + refundKey + "\"],"
                + "[\"issuer\",\"test-issuer\"],[\"unit\",\"sat\"]]";
        String voucherJson = "[\"P2PK_VOUCHER\",{\"nonce\":\"" + "ab".repeat(16)
                + "\",\"data\":\"" + spendingKey + "\",\"tags\":" + tags + "}]";
        String p2pkJson = "[\"P2PK\",{\"nonce\":\"" + "ab".repeat(16)
                + "\",\"data\":\"" + spendingKey + "\",\"tags\":" + tags + "}]";

        P2PKSecret voucher = (P2PKSecret) SecretUtil.toSecret(voucherJson);
        P2PKSecret p2pk = (P2PKSecret) SecretUtil.toSecret(p2pkJson);

        assertThat(voucher.getSigFlag())
                .as("a locked voucher must not ignore a sigflag the plain kind honours")
                .isEqualTo(p2pk.getSigFlag());
        assertThat(voucher.getNSigs())
                .as("nor a signature threshold")
                .isEqualTo(p2pk.getNSigs());
        assertThat(voucher.getLockTime())
                .as("nor a locktime, which decides when the lock stops applying")
                .isEqualTo(p2pk.getLockTime());
    }
}
