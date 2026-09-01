package xyz.tcheeric.cashu.common;

import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whether a TokenV4 can carry a NUT-02 version 2 keyset id.
 *
 * <p>A v1 id is 8 bytes and a v2 id is 33. The {@code i} field is a CBOR byte string, so length is
 * carried rather than assumed, but that is worth proving rather than believing: a token that
 * silently truncates a v2 id would produce proofs attributed to a keyset that does not exist.
 */
class TokenV4KeysetIdV2Test {

    private static final String V2_KEYSET_ID =
            "015ba18a8adcd02e715a58358eb618da4a4b3791151a4bee5e968bb88406ccf76a";

    private static TokenV4 tokenWithKeysetId(String keysetIdHex) {
        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("9a1f293253e41e9a1f293253e41e9a1f293253e41e9a1f293253e41e9a1f2932");
        proof.setSignature(Hex.decode(
                "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"));

        TokenV4.TokenData data = new TokenV4.TokenData();
        data.setKeySetId(Hex.decode(keysetIdHex));
        data.addProofs(proof);

        TokenV4 token = new TokenV4();
        token.setMintUrl("https://mint.example.com");
        token.setUnit("sat");
        token.setTokenDataList(List.of(data));
        return token;
    }

    /**
     * Ensures a 33-byte version 2 keyset id survives encoding intact, rather than being truncated
     * to the 8 bytes a version 1 id occupies.
     */
    @Test
    void shouldCarryAVersionTwoKeysetIdWithoutTruncatingIt() {
        // Arrange
        TokenV4 token = tokenWithKeysetId(V2_KEYSET_ID);

        // Act
        byte[] encoded = TokenV4CborEncoder.encode(token);

        // Assert
        assertThat(encoded).containsSequence(Hex.decode(V2_KEYSET_ID));
    }

    /**
     * Ensures the encoder distinguishes the two id lengths, so a v2 id is not quietly encoded as
     * though it were a v1 one.
     *
     * <p>The 33-byte id is 25 bytes longer than the 8-byte one, and costs one further byte because
     * CBOR encodes a length above 23 in a following byte rather than in the head itself.
     */
    @Test
    void shouldEncodeVersionOneAndVersionTwoIdsDifferently() {
        // Arrange
        TokenV4 v1 = tokenWithKeysetId("009a1f293253e41e");
        TokenV4 v2 = tokenWithKeysetId(V2_KEYSET_ID);

        // Act
        byte[] encodedV1 = TokenV4CborEncoder.encode(v1);
        byte[] encodedV2 = TokenV4CborEncoder.encode(v2);

        // Assert
        assertThat(encodedV2.length - encodedV1.length).isEqualTo(26);
    }
}
