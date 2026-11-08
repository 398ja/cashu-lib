package xyz.tcheeric.cashu.common.voucher;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.common.TokenV4CborEncoder;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the optional {@code v} map of voucher documents on cashuB tokens (cashu-voucher#64).
 */
class TokenV4VoucherDocumentsTest {

    private static final byte[] DOC_A = "voucher-document-a".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DOC_B = "voucher-document-b".getBytes(StandardCharsets.UTF_8);

    private static TokenV4 token(int proofCount) {
        TokenV4 token = new TokenV4();
        token.setMintUrl("https://mint.example");
        token.setUnit("sat");
        List<TokenV4.TokenData.TokenProof> proofs = new ArrayList<>();
        for (int i = 0; i < proofCount; i++) {
            TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
            proof.setAmount(1 << i);
            proof.setSecret("s" + i);
            proof.setSignature(HexFormat.of().parseHex("02" + "ab".repeat(32)));
            proofs.add(proof);
        }
        token.setTokenDataList(new ArrayList<>(List.of(
                new TokenV4.TokenData(HexFormat.of().parseHex("00e3372e61d05605"), proofs))));
        return token;
    }

    /** Raw CBOR of {@code token} with a hand-built {@code v} map appended, bypassing the helpers. */
    private static String withRawDocuments(TokenV4 token, Map<String, byte[]> documents) {
        byte[] body = TokenV4CborEncoder.encode(token);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(body[0] + 1);
        out.write(body, 1, body.length - 1);
        TokenV4CborEncoder.writeTextString(out, "v");
        TokenV4CborEncoder.writeMapHeader(out, documents.size());
        documents.forEach((key, value) -> {
            TokenV4CborEncoder.writeTextString(out, key);
            TokenV4CborEncoder.writeByteString(out, value);
        });
        return "cashuB" + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
    }

    /** A token without documents does not gain a {@code v} key. */
    @Test
    void shouldOmitVoucherMapWhenTokenHasNoDocuments() {
        TokenV4 token = token(2);

        byte[] cbor = TokenV4CborEncoder.encode(token);

        assertThat(cbor[0]).isEqualTo((byte) 0xA3);
    }

    /** Documents survive a serialize and deserialize round trip, written after {@code u}. */
    @Test
    void shouldRoundTripDocumentsWhenTokenCarriesThem() {
        TokenV4 token = token(2);
        String keyA = token.putVoucherDocument(DOC_A);
        String keyB = token.putVoucherDocument(DOC_B);

        TokenV4 decoded = TokenV4.deserialize(token.serialize(false));

        assertThat(decoded.getVoucherDocuments()).containsOnlyKeys(keyA, keyB);
        assertThat(decoded.getVoucherDocuments().get(keyA)).isEqualTo(DOC_A);
        assertThat(decoded).isEqualTo(token);
        assertThat(decoded.serialize(false)).isEqualTo(token.serialize(false));
    }

    /** The {@code v} key is the last top-level entry. */
    @Test
    void shouldWriteVoucherMapLastWhenEncoding() {
        TokenV4 token = token(1);
        token.putVoucherDocument(DOC_A);
        byte[] cbor = TokenV4CborEncoder.encode(token);
        byte[] unitThenV = new byte[] {0x61, 'u', 0x63, 's', 'a', 't', 0x61, 'v', (byte) 0xA1};

        int index = indexOf(cbor, unitThenV);

        assertThat(cbor[0]).isEqualTo((byte) 0xA4);
        assertThat(index).isPositive();
    }

    /** The plain Jackson CBOR mapper also reads {@code v}, so tokens decoded either way agree. */
    @Test
    void shouldPreserveDocumentsWhenDecodedWithModuleFreeCborMapper() throws Exception {
        TokenV4 token = token(1);
        String key = token.putVoucherDocument(DOC_A);

        TokenV4 decoded = JsonUtils.CBOR_MAPPER.readValue(TokenV4CborEncoder.encode(token), TokenV4.class);

        assertThat(decoded.getVoucherDocuments().get(key)).isEqualTo(DOC_A);
    }

    /** Callers cannot mutate a token through the returned map or arrays. */
    @Test
    void shouldReturnDefensiveCopiesWhenReadingDocuments() {
        TokenV4 token = token(1);
        String key = token.putVoucherDocument(DOC_A);

        token.getVoucherDocuments().get(key)[0] = 0;

        assertThat(token.getVoucherDocuments().get(key)).isEqualTo(DOC_A);
        assertThatThrownBy(() -> token.getVoucherDocuments().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /** Unreferenced documents are dropped before re-encoding. */
    @Test
    void shouldDropUnreferencedDocumentsWhenRetaining() {
        TokenV4 token = token(2);
        String keyA = token.putVoucherDocument(DOC_A);
        token.putVoucherDocument(DOC_B);

        token.retainReferencedDocuments(Set.of(keyA));

        assertThat(token.getVoucherDocuments()).containsOnlyKeys(keyA);
    }

    /** Tokens holding the same documents are equal; different documents are not. */
    @Test
    void shouldCompareDocumentsByContentWhenCheckingEquality() {
        TokenV4 first = token(1);
        TokenV4 second = token(1);
        first.putVoucherDocument(DOC_A);
        second.putVoucherDocument(DOC_A.clone());
        TokenV4 third = token(1);
        third.putVoucherDocument(DOC_B);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(third);
    }

    /** A key that is not 64 lowercase hex is refused. */
    @Test
    void shouldRefuseTokenWhenKeyIsNotLowercaseHex() {
        String encoded = withRawDocuments(token(1), Map.of(VoucherDocuments.key(DOC_A).toUpperCase(), DOC_A));

        assertThatThrownBy(() -> TokenV4.deserialize(encoded))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64 lowercase hex");
    }

    /**
     * A {@code v} map that repeats a key is refused rather than read last-wins, so Java and the
     * TypeScript wallet agree that such a token is invalid. Both entries hash correctly, so only
     * the duplicate check can refuse it.
     */
    @Test
    void shouldRefuseTokenWhenDocumentKeyIsRepeated() {
        byte[] body = TokenV4CborEncoder.encode(token(2));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(body[0] + 1);
        out.write(body, 1, body.length - 1);
        TokenV4CborEncoder.writeTextString(out, "v");
        TokenV4CborEncoder.writeMapHeader(out, 2);
        for (int i = 0; i < 2; i++) {
            TokenV4CborEncoder.writeTextString(out, VoucherDocuments.key(DOC_A));
            TokenV4CborEncoder.writeByteString(out, DOC_A);
        }
        String encoded = "cashuB" + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());

        assertThatThrownBy(() -> TokenV4.deserialize(encoded))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid token")
                .hasMessageContaining("Duplicate field");
    }

    /** A key that does not hash its value is refused. */
    @Test
    void shouldRefuseTokenWhenKeyDoesNotMatchDocumentHash() {
        String encoded = withRawDocuments(token(1), Map.of(VoucherDocuments.key(DOC_B), DOC_A));

        assertThatThrownBy(() -> TokenV4.deserialize(encoded))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match SHA-256");
    }

    /** An empty document is refused. */
    @Test
    void shouldRefuseTokenWhenDocumentIsEmpty() {
        String encoded = withRawDocuments(token(1), Map.of(VoucherDocuments.key(new byte[0]), new byte[0]));

        assertThatThrownBy(() -> TokenV4.deserialize(encoded))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    /** A document over 65,536 bytes is refused. */
    @Test
    void shouldRefuseTokenWhenDocumentIsTooLarge() {
        byte[] huge = new byte[VoucherDocuments.MAX_DOCUMENT_BYTES + 1];
        Arrays.fill(huge, (byte) 7);
        String encoded = withRawDocuments(token(1), Map.of(VoucherDocuments.key(huge), huge));

        assertThatThrownBy(() -> TokenV4.deserialize(encoded))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds 65536 bytes");
    }

    /** More documents than proofs is refused. */
    @Test
    void shouldRefuseTokenWhenDocumentsOutnumberProofs() {
        String encoded = withRawDocuments(token(1),
                Map.of(VoucherDocuments.key(DOC_A), DOC_A, VoucherDocuments.key(DOC_B), DOC_B));

        assertThatThrownBy(() -> TokenV4.deserialize(encoded))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("count exceeds proof count");
    }

    /** A document at exactly the size limit is accepted. */
    @Test
    void shouldAcceptDocumentWhenAtSizeLimit() {
        TokenV4 token = token(1);
        byte[] maximal = new byte[VoucherDocuments.MAX_DOCUMENT_BYTES];

        String key = token.putVoucherDocument(maximal);

        assertThat(TokenV4.deserialize(token.serialize(false)).getVoucherDocuments()).containsOnlyKeys(key);
    }

    /** Putting an empty document is refused immediately. */
    @Test
    void shouldRefusePutWhenDocumentIsEmpty() {
        TokenV4 token = token(1);

        assertThatThrownBy(() -> token.putVoucherDocument(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Unknown top-level keys: the CBOR mapper does not ignore unknown properties, so a token with a
     * key this library does not model is refused today. That is why {@code v} must be a known key.
     */
    @Test
    void shouldRefuseTokenWhenTopLevelKeyIsUnknown() {
        byte[] body = TokenV4CborEncoder.encode(token(1));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(body[0] + 1);
        out.write(body, 1, body.length - 1);
        TokenV4CborEncoder.writeTextString(out, "z");
        TokenV4CborEncoder.writeTextString(out, "unknown");
        String encoded = "cashuB" + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());

        assertThatThrownBy(() -> TokenV4.deserialize(encoded)).isInstanceOf(RuntimeException.class);
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
