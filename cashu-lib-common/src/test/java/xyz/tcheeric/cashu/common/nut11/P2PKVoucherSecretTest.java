package xyz.tcheeric.cashu.common.nut11;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.nut10.Nut10Option;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.nut18.VoucherSecret;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link P2PKVoucherSecret} is a voucher that is also P2PK-locked.
 *
 * <p>The properties worth pinning are the ones that motivated a distinct kind: the lock is a
 * real NUT-11 lock and is validated as one, the voucher metadata survives a wire round trip,
 * and the type cannot be mistaken for a plain voucher by a mint dispatching on kind.
 */
class P2PKVoucherSecretTest {

    /** secp256k1 generator, even-y. */
    private static final String SPENDING_KEY =
            "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    private static P2PKVoucherSecret sample() {
        P2PKVoucherSecret secret = new P2PKVoucherSecret(Hex.decode(SPENDING_KEY));
        secret.setNonce("859d4935c4907062a6297cf4e663e2835d90d97ecdd510745d32f6816323a41f");
        secret.setVoucherId("0f5d9e0e-3a0a-4a4f-9f6f-6f4a3b2c1d0e");
        secret.setIssuerId("acme");
        secret.setUnit("sat");
        secret.setFaceValue(5000L);
        secret.setExpiresAt(4_102_444_800L); // 2100-01-01, comfortably future
        secret.setIssuerPublicKey(SPENDING_KEY);
        secret.setIssuerSignature("de".repeat(32));
        return secret;
    }

    @Nested
    @DisplayName("kind")
    class KindTests {

        @Test
        @DisplayName("is P2PK_VOUCHER, not P2PK, so a mint can dispatch on it")
        void kindIsDistinct() {
            assertThat(sample().getKind()).isEqualTo(WellKnownSecret.Kind.P2PK_VOUCHER);
        }

        @Test
        @DisplayName("the enum name is the wire string")
        void kindNameIsWireString() {
            // WellKnownSecretSerializer writes getKind().name() and the deserializer reads
            // Kind.valueOf(...), so this name is a public format and not just an identifier.
            assertThat(WellKnownSecret.Kind.P2PK_VOUCHER.name()).isEqualTo("P2PK_VOUCHER");
        }

        @Test
        @DisplayName("is a P2PKSecret, so existing lock enforcement applies unchanged")
        void isAP2PKSecret() {
            assertThat(sample()).isInstanceOf(P2PKSecret.class);
        }

        @Test
        @DisplayName("is not a VoucherSecret, so a voucher-only branch cannot swallow it")
        void isNotAVoucherSecret() {
            // The trap this kind exists to avoid: a mint whose voucher branch matched this
            // type would run the issuer and expiry checks and never check the witness, which
            // is exactly the advisory-lock failure a distinct kind prevents.
            assertThat(sample()).isNotInstanceOf(VoucherSecret.class);
        }
    }

    @Nested
    @DisplayName("the lock")
    class LockTests {

        @Test
        @DisplayName("lives in data, where NUT-11 puts it")
        void spendingKeyIsInData() {
            assertThat(Hex.toHexString(sample().getData())).isEqualTo(SPENDING_KEY);
        }

        /** Prefix 02 with x = 0: a well-formed encoding whose x is not on the curve. */
        private static final String NOT_A_POINT = "02" + "00".repeat(32);

        @Test
        @DisplayName("rejects a spending key that is not a curve point")
        void rejectsNonPointSpendingKey() {
            // Inherited from P2PKSecret: a construction-side mistake surfaces where it is made.
            // Note the encoding is the right length and prefix -- what fails is the curve check,
            // which is the check that matters.
            assertThatThrownBy(() -> new P2PKVoucherSecret(Hex.decode(NOT_A_POINT)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("is validated by NUT-11's malformed-secret rules")
        void inheritsNut11Validation() {
            P2PKVoucherSecret secret = sample();
            secret.setNSigs(5); // more signatures than there are keys

            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class);
        }

        @Test
        @DisplayName("a well-formed secret validates")
        void wellFormedValidates() {
            assertThatCode(() -> sample().validate()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("voucher metadata")
    class MetadataTests {

        @Test
        @DisplayName("the voucher id is a tag, because data holds the lock")
        void voucherIdIsATag() {
            P2PKVoucherSecret secret = sample();

            assertThat(secret.getVoucherId()).isEqualTo("0f5d9e0e-3a0a-4a4f-9f6f-6f4a3b2c1d0e");
            assertThat(secret.getTag(xyz.tcheeric.cashu.common.nut18.VoucherTags.VOUCHER_ID)).isNotNull();
        }

        @Test
        @DisplayName("uses the same tag keys as an ordinary voucher")
        void sharesVoucherTagVocabulary() {
            // One vocabulary regardless of which kind carries it, so an issuer and a mint do
            // not need two readers.
            P2PKVoucherSecret secret = sample();

            assertThat(secret.getTag("issuer")).isNotNull();
            assertThat(secret.getTag("unit")).isNotNull();
            assertThat(secret.getTag("face_value")).isNotNull();
            assertThat(secret.getTag("expires_at")).isNotNull();
            assertThat(secret.getTag("issuer_sig")).isNotNull();
            assertThat(secret.getTag("issuer_pubkey")).isNotNull();
        }

        @Test
        @DisplayName("reads back what was written")
        void accessorsRoundTrip() {
            P2PKVoucherSecret secret = sample();

            assertThat(secret.getIssuerId()).isEqualTo("acme");
            assertThat(secret.getUnit()).isEqualTo("sat");
            assertThat(secret.getFaceValue()).isEqualTo(5000L);
            assertThat(secret.getExpiresAt()).isEqualTo(4_102_444_800L);
            assertThat(secret.getIssuerPublicKey()).isEqualTo(SPENDING_KEY);
        }

        @Test
        @DisplayName("absent optional metadata reads as null rather than throwing")
        void absentMetadataIsNull() {
            P2PKVoucherSecret bare = new P2PKVoucherSecret(Hex.decode(SPENDING_KEY));

            assertThat(bare.getIssuerId()).isNull();
            assertThat(bare.getFaceValue()).isNull();
            assertThat(bare.getExpiresAt()).isNull();
            assertThat(bare.getMerchantMetadata()).isNull();
        }

        @Test
        @DisplayName("isSigned reports presence of both issuer fields")
        void isSignedIsPresenceOnly() {
            assertThat(sample().isSigned()).isTrue();
            assertThat(new P2PKVoucherSecret(Hex.decode(SPENDING_KEY)).isSigned()).isFalse();
        }

        @Test
        @DisplayName("isExpired compares against the expiry tag")
        void isExpired() {
            P2PKVoucherSecret secret = sample();
            assertThat(secret.isExpired()).isFalse();

            secret.setExpiresAt(1L); // 1970
            assertThat(secret.isExpired()).isTrue();
        }

        @Test
        @DisplayName("a voucher with no expiry never expires")
        void noExpiryNeverExpires() {
            assertThat(new P2PKVoucherSecret(Hex.decode(SPENDING_KEY)).isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("wire round trip")
    class WireTests {

        @Test
        @DisplayName("serializes to a P2PK_VOUCHER secret and parses back equal")
        void roundTrips() throws Exception {
            P2PKVoucherSecret original = sample();

            String json = JsonUtils.JSON_MAPPER.writeValueAsString(original);
            assertThat(json).contains("P2PK_VOUCHER");

            WellKnownSecret parsed =
                    JsonUtils.JSON_MAPPER.readValue(json, WellKnownSecret.class);

            // The deserializer must pick the right class from the kind alone.
            assertThat(parsed).isInstanceOf(P2PKVoucherSecret.class);

            P2PKVoucherSecret back = (P2PKVoucherSecret) parsed;
            assertThat(back.getKind()).isEqualTo(WellKnownSecret.Kind.P2PK_VOUCHER);
            assertThat(Hex.toHexString(back.getData())).isEqualTo(SPENDING_KEY);
            assertThat(back.getIssuerId()).isEqualTo("acme");
            assertThat(back.getFaceValue()).isEqualTo(5000L);
            assertThat(back.getVoucherId()).isEqualTo(original.getVoucherId());
        }

        @Test
        @DisplayName("a parsed secret equals an identically constructed one")
        void parsedEqualsConstructed() throws Exception {
            // Tag values are normalised to strings on parse; without that a constructed secret
            // and a parsed one would differ by boxed type alone.
            P2PKVoucherSecret original = sample();
            String json = JsonUtils.JSON_MAPPER.writeValueAsString(original);
            WellKnownSecret parsed =
                    JsonUtils.JSON_MAPPER.readValue(json, WellKnownSecret.class);

            assertThat(JsonUtils.JSON_MAPPER.writeValueAsString(parsed)).isEqualTo(json);
        }

        @Test
        @DisplayName("a malformed lock is rejected at parse, not later")
        void malformedLockRejectedAtParse() {
            // Deserialization is the enforcement point: a malformed secret must never become a
            // live object. Inherited from the P2PKSecret path.
            String json = "[\"P2PK_VOUCHER\",{\"nonce\":\"aa\",\"data\":\"02"
                    + "00".repeat(32) + "\",\"tags\":[]}]";

            assertThatThrownBy(
                    () -> JsonUtils.JSON_MAPPER.readValue(json, WellKnownSecret.class))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("Nut10Option")
    class OptionTests {

        @Test
        @DisplayName("forP2PKVoucher builds the kind with the spending key as data")
        void buildsOption() {
            Nut10Option option = Nut10Option.forP2PKVoucher(SPENDING_KEY);

            assertThat(option.getKind()).isEqualTo(WellKnownSecret.Kind.P2PK_VOUCHER);
            assertThat(option.getData()).isEqualTo(SPENDING_KEY);
        }

        @Test
        @DisplayName("isVoucher stays false, so existing branches keep their meaning")
        void isVoucherIsNarrow() {
            Nut10Option option = Nut10Option.forP2PKVoucher(SPENDING_KEY);

            assertThat(option.isVoucher()).isFalse();
            assertThat(option.isP2PKVoucher()).isTrue();
            assertThat(option.carriesVoucherMetadata()).isTrue();
        }

        @Test
        @DisplayName("an ordinary voucher carries metadata but is not P2PK-locked")
        void ordinaryVoucherUnchanged() {
            Nut10Option option = Nut10Option.forVoucher("abcd");

            assertThat(option.isVoucher()).isTrue();
            assertThat(option.isP2PKVoucher()).isFalse();
            assertThat(option.carriesVoucherMetadata()).isTrue();
        }
    }

    /**
     * Issue 398ja/cashu-mint#406: a locktime without refund keys unlocks the proof.
     *
     * <p>NUT-11 makes a refund-less proof spendable with no witness once its locktime passes.
     * For a plain P2PK secret that is intended. For a voucher it is the one guarantee this kind
     * exists to provide, retracted silently and on a timer: the voucher behaves correctly until
     * the locktime passes, then quietly stops being locked.
     *
     * <p>Refused at construction rather than at spending, so the mint's NUT-11 behaviour is
     * unchanged for proofs it did not issue.
     */
    @Nested
    @DisplayName("locktime without refund keys")
    class LocktimeWithoutRefund {

        @Test
        @DisplayName("is refused, because it would unlock the voucher once the locktime passed")
        void refusesALocktimeWithNoRefundPath() {
            P2PKVoucherSecret secret = sample();
            secret.setLockTime(1);

            assertThatThrownBy(secret::validate)
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("locktime")
                    .hasMessageContaining("refund");
        }

        @Test
        @DisplayName("is allowed with refund keys, which are a real signature requirement")
        void allowsALocktimeWithARefundPath() {
            P2PKVoucherSecret secret = sample();
            secret.setLockTime(1);
            secret.setRefund(java.util.List.of(SPENDING_KEY));

            assertThatCode(secret::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("does not restrict a voucher with no locktime at all")
        void allowsNoLocktime() {
            // The common case, and the one that would break loudly if the rule were written as
            // "refund keys are mandatory" rather than "mandatory when a locktime is present".
            assertThatCode(sample()::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("is refused when it arrives from the wire, not only when built locally")
        void refusesItOnDeserialization() {
            // The check that decides whether this rule is real. A secret an attacker sends is
            // deserialised, never constructed through the setters, so a rule enforced only in
            // validate() would be dead if nothing on that path called it. The deserialiser
            // does, for every P2PKSecret and therefore for this subclass.
            String wire = "[\"P2PK_VOUCHER\",{\"nonce\":\"n\",\"data\":\"" + SPENDING_KEY
                    + "\",\"tags\":[[\"locktime\",\"1\"],[\"voucher_id\",\"v-1\"]]}]";

            // Thrown straight out of the deserialiser rather than wrapped, so the caller sees
            // the reason rather than a generic mapping failure.
            assertThatThrownBy(() -> JsonUtils.JSON_MAPPER.readValue(wire, WellKnownSecret.class))
                    .isInstanceOf(MalformedP2PKSecretException.class)
                    .hasMessageContaining("refund");
        }

        @Test
        @DisplayName("does not change the rule for a plain P2PK secret")
        void leavesPlainP2PKAlone() {
            // The deliberate NUT-11 behaviour this must not disturb: an escrow whose locktime
            // passes with no refund path falls open, which is what makes it recoverable.
            P2PKSecret plain = new P2PKSecret(Hex.decode(SPENDING_KEY));
            plain.setLockTime(1);

            assertThatCode(plain::validate).doesNotThrowAnyException();
        }
    }

    /**
     * The two voucher-carrying kinds must READ the same.
     *
     * They carry identical tags and differ only in where the spending key
     * lives, so a caller that gets a different answer depending on which kind
     * it holds would have the lock silently change the money. These four
     * accessors were missing from this class entirely, which meant a locked
     * voucher could be constructed and then not be fully readable — the gap
     * that blocked SignedVoucher from accepting one.
     */
    @Nested
    @DisplayName("parity with the plain VOUCHER kind")
    class ParityWithVoucherSecret {

        @Test
        @DisplayName("the same tags read back the same values")
        void readsMatch() {
            P2PKVoucherSecret locked = new P2PKVoucherSecret(Hex.decode(SPENDING_KEY));
            locked.setMemo("two coffees");
            locked.setFaceDecimals(2);
            locked.setBackingStrategy("PROPORTIONAL");
            locked.setIssuanceRatio(0.75d);

            VoucherSecret plain = new VoucherSecret();
            plain.setMemo("two coffees");
            plain.setFaceDecimals(2);
            plain.setBackingStrategy("PROPORTIONAL");
            plain.setIssuanceRatio(0.75d);

            assertThat(locked.getMemo()).isEqualTo(plain.getMemo());
            assertThat(locked.getFaceDecimals()).isEqualTo(plain.getFaceDecimals());
            assertThat(locked.getBackingStrategy()).isEqualTo(plain.getBackingStrategy());
            assertThat(locked.getIssuanceRatio()).isEqualTo(plain.getIssuanceRatio());
        }

        @Test
        @DisplayName("unset tags default the same way")
        void defaultsMatch() {
            P2PKVoucherSecret locked = new P2PKVoucherSecret(Hex.decode(SPENDING_KEY));
            VoucherSecret plain = new VoucherSecret();

            // The defaults are load-bearing: a voucher with no explicit ratio
            // is 1:1, and one defaulting to 0 would value every coupon at
            // nothing.
            assertThat(locked.getFaceDecimals()).isEqualTo(plain.getFaceDecimals());
            assertThat(locked.getBackingStrategy()).isEqualTo(plain.getBackingStrategy());
            assertThat(locked.getIssuanceRatio()).isEqualTo(plain.getIssuanceRatio());
            assertThat(locked.getMemo()).isNull();
        }

        @Test
        @DisplayName("a malformed ratio falls back rather than throwing")
        void malformedRatioFallsBack() {
            // Read from a proof someone else minted, so it must not be trusted
            // to parse. Throwing here would make an unreadable tag an
            // unspendable proof.
            P2PKVoucherSecret locked = new P2PKVoucherSecret(Hex.decode(SPENDING_KEY));
            locked.setTag("issuance_ratio", java.util.List.of("not-a-number"));

            assertThat(locked.getIssuanceRatio()).isEqualTo(1.0d);
        }

        @Test
        @DisplayName("the spending key still owns data")
        void dataIsStillTheKey() {
            // The whole point of the kind. If a payload ever displaced this,
            // P2PKSecret.validate() would reject the proof outright.
            P2PKVoucherSecret locked = new P2PKVoucherSecret(Hex.decode(SPENDING_KEY));
            locked.setMemo("two coffees");

            assertThat(Hex.toHexString(locked.getData())).isEqualTo(SPENDING_KEY);
        }
    }
}
