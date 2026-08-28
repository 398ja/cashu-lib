package xyz.tcheeric.cashu.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.BaseKey;
import xyz.tcheeric.cashu.common.PrivateKey;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.RSSProof;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.Signature;
import xyz.tcheeric.cashu.common.TokenV3;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compliance checks for NUT-00 token serialization and hash-to-curve behaviour.
 */
class NUT00Tests {

    private static final List<Map.Entry<String, String>> HASH_TO_CURVE_VECTORS = List.of(
            Map.entry(
                    "0000000000000000000000000000000000000000000000000000000000000000",
                    "024cce997d3b518f739663b757deaec95bcd9473c30a14ac2fd04023a739d1a725"
            ),
            Map.entry(
                    "0000000000000000000000000000000000000000000000000000000000000001",
                    "022e7158e11c9506f1aa4248bf531298daa7febd6194f003edcd9b93ade6253acf"
            ),
            Map.entry(
                    "0000000000000000000000000000000000000000000000000000000000000002",
                    "026cdbe15362df59cd1dd3c9c11de8aedac2106eca69236ecd9fbe117af897be4f"
            )
    );

    private record BlindedSignatureVector(String privateKeyHex, String blindedPointHex, String expectedSignatureHex) {}

    private static final List<BlindedSignatureVector> BLINDED_SIGNATURE_VECTORS = List.of(
            new BlindedSignatureVector(
                    "0000000000000000000000000000000000000000000000000000000000000001",
                    "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2",
                    "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2"
            ),
            new BlindedSignatureVector(
                    "7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f",
                    "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2",
                    "0398bc70ce8184d27ba89834d19f5199c84443c31131e48d3c1214db24247d005d"
            ),
            new BlindedSignatureVector(
                    "811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b",
                    "03a633b63d81df8522946d09c19548561493de05b81dfc2bb2f308d3328d973537",
                    "03b890b660bf5d5c3ad7651c7afe3a08793be57c150ad8d0646441e68aa77ef830"
            )
    );

    private static final String COMMON_PUBLIC_KEY =
            "02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2";

    /**
     * Ensures BDHKE hash-to-curve matches the expected points for known inputs.
     *
     * <p>The NUT-00 vectors give their messages as raw byte arrays printed as hex, so they are
     * hex-decoded here rather than being read as secret strings.
     */
    @Test
    void shouldHashMessagesToExpectedCurvePoints() {
        // Arrange
        List<Map.Entry<String, String>> vectors = HASH_TO_CURVE_VECTORS;

        // Act & Assert
        for (Map.Entry<String, String> vector : vectors) {
            byte[] message = Hex.decode(vector.getKey());
            String actual = BDHKEUtils.pointToHex(BDHKEUtils.hashToCurve(message));
            assertEquals(vector.getValue(), actual);
        }
    }

    /**
     * Ensures blinded signature derivation reproduces known Schnorr signatures.
     */
    @Test
    void shouldProduceExpectedBlindedSignatures() {
        // Act & Assert
        for (BlindedSignatureVector vector : BLINDED_SIGNATURE_VECTORS) {
            BaseKey privateKey = PrivateKey.fromString(vector.privateKeyHex());
            BaseKey blindedPoint = PublicKey.fromString(vector.blindedPointHex());
            byte[] blindedSignature = BDHKEUtils.signBlindedMessage(blindedPoint.toBytes(), privateKey.toBytes());
            Signature parsedSignature = Signature.fromString(Hex.toHexString(blindedSignature));
            assertEquals(Signature.fromString(vector.expectedSignatureHex()), parsedSignature);
        }
    }

    /**
     * Ensures TokenV3 serializes deterministically and deserializes with the same fields.
     */
    @Test
    void shouldSerializeTokenV3ToStableJson() throws JsonProcessingException {
        // Arrange
        TokenV3<RandomStringSecret> token = new TokenV3<>();
        token.setMemo("Thank you.");
        token.setUnit("sat");

        Set<TokenV3.MintProof<RandomStringSecret>> mintProofs = new LinkedHashSet<>();
        TokenV3.MintProof<RandomStringSecret> mintProof = new TokenV3.MintProof<>();
        mintProof.setMint("https://8333.space:3338");

        Set<Proof<RandomStringSecret>> proofs = new LinkedHashSet<>();
        proofs.add(createProof(
                "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea",
                2,
                "407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837"
        ));
        proofs.add(createProof(
                "029e8e5050b890a7d6c0968db16bc1d5d5fa040ea1de284f6ec69d61299f671059",
                8,
                "fe15109314e61d7756b0f8ee0f23a624acaa3f4e042f61433c728c7057b931be"
        ));

        mintProof.setProofs(proofs);
        mintProofs.add(mintProof);
        token.setMintProofs(mintProofs);

        // Act
        String json = JsonUtils.JSON_MAPPER.writeValueAsString(token);
        TokenV3<?> parsed = JsonUtils.JSON_MAPPER.readValue(json, TokenV3.class);

        // Assert
        assertEquals("sat", parsed.getUnit());
        assertEquals("Thank you.", parsed.getMemo());
        assertEquals(1, parsed.getMintProofs().size());
        assertEquals(2, parsed.getMintProofs().iterator().next().getProofs().size());
    }

    /**
     * Ensures clickable serialization adds the prefix and deserialization maintains memo and unit.
     */
    @Test
    void shouldSerializeTokenV3ToClickableUri() {
        // Arrange
        TokenV3<RandomStringSecret> token = new TokenV3<>();
        token.setMemo("Thank you.");
        token.setUnit("sat");

        Set<TokenV3.MintProof<RandomStringSecret>> mintProofs = new LinkedHashSet<>();
        TokenV3.MintProof<RandomStringSecret> mintProof = new TokenV3.MintProof<>();
        mintProof.setMint("https://8333.space:3338");

        Set<Proof<RandomStringSecret>> proofs = new LinkedHashSet<>();
        proofs.add(createProof(
                "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea",
                2,
                "407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837"
        ));
        mintProof.setProofs(proofs);
        mintProofs.add(mintProof);
        token.setMintProofs(mintProofs);

        // Act
        String serialized = token.serialize(true);
        TokenV3<?> parsed = TokenV3.deserialize(serialized);

        // Assert
        assertTrue(serialized.startsWith("cashu:cashuA"));
        assertEquals("sat", parsed.getUnit());
        assertEquals("Thank you.", parsed.getMemo());
    }

    /**
     * Ensures TokenV4 maintains fields through serialization and deserialization.
     */
    @Test
    void shouldPreserveTokenV4FieldsDuringSerialization() {
        // Arrange
        TokenV4 token = new TokenV4();
        token.setMemo("Thank you");
        token.setUnit("sat");
        token.setMintUrl("http://localhost:3338");

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("9a6dbb847bd232ba76db0df197216b29d3b8cc14553cd27827fc1cc942fedb4e");
        proof.setSignature(Utils.hexStringToBytes("038618543ffb6b8695df4ad4babcde92a34a96bdcd97dcee0d7ccf98d472126792"));

        token.setTokenDataList(List.of(
                new TokenV4.TokenData(
                        Utils.hexStringToBytes("00ad268c4d1f5826"),
                        List.of(proof)
                )
        ));

        // Act
        String serialized = token.serialize(false);
        TokenV4 deserialized = TokenV4.deserialize(serialized);

        // Assert
        assertTrue(serialized.startsWith("cashuB"));
        assertEquals("http://localhost:3338", deserialized.getMintUrl());
        assertEquals("sat", deserialized.getUnit());
        assertEquals("Thank you", deserialized.getMemo());
        assertEquals(1, deserialized.getTokenDataList().size());
    }

    private static RSSProof createProof(String signatureHex, int amount, String secretHex) {
        RSSProof proof = new RSSProof();
        proof.setUnblindedSignature(Signature.fromString(signatureHex));
        proof.setAmount(amount);
        proof.setSecret(RandomStringSecret.fromString(secretHex));
        proof.setKeySetId("009a1f293253e41e");
        return proof;
    }
}
