package xyz.tcheeric.cashu.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Assertions;
import xyz.tcheeric.cashu.common.BaseKey;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.PrivateKey;
import xyz.tcheeric.cashu.common.PublicKey;
import xyz.tcheeric.cashu.common.RSSProof;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.RandomStringSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.Signature;
import xyz.tcheeric.cashu.common.TokenV3;
import xyz.tcheeric.cashu.common.TokenV4;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.cashu.crypto.BDHKEUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class NUT00Tests {

    @Test
    public void hashToCurveFunction() {
        Secret message = RandomStringSecret.fromString("0000000000000000000000000000000000000000000000000000000000000000");
        String expected = "024cce997d3b518f739663b757deaec95bcd9473c30a14ac2fd04023a739d1a725";
        var result = BDHKEUtils.hashToCurve(((RandomStringSecret) message).toBytes());
        Assertions.assertEquals(expected, BDHKEUtils.pointToHex(result));

        message = RandomStringSecret.fromString("0000000000000000000000000000000000000000000000000000000000000001");
        expected = "022e7158e11c9506f1aa4248bf531298daa7febd6194f003edcd9b93ade6253acf";
        result = BDHKEUtils.hashToCurve(((RandomStringSecret) message).toBytes());
        Assertions.assertEquals(expected, BDHKEUtils.pointToHex(result));

        message = RandomStringSecret.fromString("0000000000000000000000000000000000000000000000000000000000000002");
        expected = "026cdbe15362df59cd1dd3c9c11de8aedac2106eca69236ecd9fbe117af897be4f";
        result = BDHKEUtils.hashToCurve(((RandomStringSecret) message).toBytes());
        Assertions.assertEquals(expected, BDHKEUtils.pointToHex(result));
    }


    @Test
    public void blindedSignatures() {
        // Test 1
        BaseKey k = PrivateKey.fromString("0000000000000000000000000000000000000000000000000000000000000001");
        BaseKey B_ = PublicKey.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2");
        Signature C_ = Signature.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2");

        byte[] result = BDHKEUtils.signBlindedMessage(B_.toBytes(), k.toBytes());
        Assertions.assertEquals(C_, Signature.fromString(Hex.toHexString(result)));

        // Test 2
        k = PrivateKey.fromString("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f");
        B_ = PublicKey.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2");
        C_ = Signature.fromString("0398bc70ce8184d27ba89834d19f5199c84443c31131e48d3c1214db24247d005d");

        result = BDHKEUtils.signBlindedMessage(B_.toBytes(), k.toBytes());
        Assertions.assertEquals(C_, Signature.fromString(Hex.toHexString(result)));

        // Test 3, mine
        k = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        B_ = PublicKey.fromString("03a633b63d81df8522946d09c19548561493de05b81dfc2bb2f308d3328d973537");
        C_ = Signature.fromString("03b890b660bf5d5c3ad7651c7afe3a08793be57c150ad8d0646441e68aa77ef830");

        result = BDHKEUtils.signBlindedMessage(B_.toBytes(), k.toBytes());
        Assertions.assertEquals(C_, Signature.fromString(Hex.toHexString(result)));
    }


    @Test
    public void serializeV3() throws JsonProcessingException {
        TokenV3<RandomStringSecret> tokenV3 = new TokenV3();

        tokenV3.setMemo("Thank you.");
        tokenV3.setUnit("sat");

        Set<TokenV3.MintProof<RandomStringSecret>> mintProofs = new LinkedHashSet<>();
        TokenV3.MintProof<RandomStringSecret> mintProof = new TokenV3.MintProof<>();
        mintProof.setMint("https://8333.space:3338");

        Set<Proof<RandomStringSecret>> proofs = new LinkedHashSet<>();
        RSSProof proof = new RSSProof();
        proof.setUnblindedSignature(Signature.fromString("02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea"));
        proof.setAmount(2);
        proof.setSecret(RandomStringSecret.fromString("407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837"));
        proof.setKeySetId("009a1f293253e41e");
        proofs.add(proof);

        proof = new RSSProof();
        proof.setUnblindedSignature(Signature.fromString("029e8e5050b890a7d6c0968db16bc1d5d5fa040ea1de284f6ec69d61299f671059"));
        proof.setAmount(8);
        proof.setSecret(RandomStringSecret.fromString("fe15109314e61d7756b0f8ee0f23a624acaa3f4e042f61433c728c7057b931be"));
        proof.setKeySetId("009a1f293253e41e");
        proofs.add(proof);

        mintProof.setProofs(proofs);
        mintProofs.add(mintProof);
        tokenV3.setMintProofs(mintProofs);

        log.debug(JsonUtils.JSON_MAPPER.writeValueAsString(tokenV3));

        Assertions.assertEquals("cashuAeyJ0b2tlbiI6W3sibWludCI6Imh0dHBzOi8vODMzMy5zcGFjZTozMzM4IiwicHJvb2ZzIjpbeyJhbW91bnQiOjIsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6IjQwNzkxNWJjMjEyYmU2MWE3N2UzZTZkMmFlYjRjNzI3OTgwYmRhNTFjZDA2YTZhZmMyOWUyODYxNzY4YTc4MzciLCJDIjoiMDJiYzkwOTc5OTdkODFhZmIyY2M3MzQ2YjVlNDM0NWE5MzQ2YmQyYTUwNmViNzk1ODU5OGE3MmYwY2Y4NTE2M2VhIn0seyJhbW91bnQiOjgsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6ImZlMTUxMDkzMTRlNjFkNzc1NmIwZjhlZTBmMjNhNjI0YWNhYTNmNGUwNDJmNjE0MzNjNzI4YzcwNTdiOTMxYmUiLCJDIjoiMDI5ZThlNTA1MGI4OTBhN2Q2YzA5NjhkYjE2YmMxZDVkNWZhMDQwZWExZGUyODRmNmVjNjlkNjEyOTlmNjcxMDU5In1dfV0sInVuaXQiOiJzYXQiLCJtZW1vIjoiVGhhbmsgeW91LiJ9", tokenV3.serialize(false));
    }

    @Test
    public void deserializationOfIncorrectPrefixTokenV3() {
        String incorrectPrefixToken = "ca$huAeyJ0b2tlbiI6W3sibWludCI6Imh0dHBzOi8vODMzMy5zcGFjZTozMzM4IiwicHJvb2ZzIjpbeyJhbW91bnQiOjIsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6IjQwNzkxNWJjMjEyYmU2MWE3N2UzZTZkMmFlYjRjNzI3OTgwYmRhNTFjZDA2YTZhZmMyOWUyODYxNzY4YTc4MzciLCJDIjoiMDJiYzkwOTc5OTdkODFhZmIyY2M3MzQ2YjVlNDM0NWE5MzQ2YmQyYTUwNmViNzk1ODU5OGE3MmYwY2Y4NTE2M2VhIn0seyJhbW91bnQiOjgsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6ImZlMTUxMDkzMTRlNjFkNzc1NmIwZjhlZTBmMjNhNjI0YWNhYTNmNGUwNDJmNjE0MzNjNzI4YzcwNTdiOTMxYmUiLCJDIjoiMDI5ZThlNTA1MGI4OTBhN2Q2YzA5NjhkYjE2YmMxZDVkNWZhMDQwZWExZGUyODRmNmVjNjlkNjEyOTlmNjcxMDU5In1dfV0sInVuaXQiOiJzYXQiLCJtZW1vIjoiVGhhbmsgeW91LiJ9";
        Assertions.assertThrows(IllegalArgumentException.class, () -> TokenV3.deserialize(incorrectPrefixToken));
    }

    @Test
    public void deserializationOfNoPrefixTokenV3() {
        String noPrefixToken = "eyJ0b2tlbiI6W3sibWludCI6Imh0dHBzOi8vODMzMy5zcGFjZTozMzM4IiwicHJvb2ZzIjpbeyJhbW91bnQiOjIsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6IjQwNzkxNWJjMjEyYmU2MWE3N2UzZTZkMmFlYjRjNzI3OTgwYmRhNTFjZDA2YTZhZmMyOWUyODYxNzY4YTc4MzciLCJDIjoiMDJiYzkwOTc5OTdkODFhZmIyY2M3MzQ2YjVlNDM0NWE5MzQ2YmQyYTUwNmViNzk1ODU5OGE3MmYwY2Y4NTE2M2VhIn0seyJhbW91bnQiOjgsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6ImZlMTUxMDkzMTRlNjFkNzc1NmIwZjhlZTBmMjNhNjI0YWNhYTNmNGUwNDJmNjE0MzNjNzI4YzcwNTdiOTMxYmUiLCJDIjoiMDI5ZThlNTA1MGI4OTBhN2Q2YzA5NjhkYjE2YmMxZDVkNWZhMDQwZWExZGUyODRmNmVjNjlkNjEyOTlmNjcxMDU5In1dfV0sInVuaXQiOiJzYXQiLCJtZW1vIjoiVGhhbmsgeW91LiJ9";
        Assertions.assertThrows(IllegalArgumentException.class, () -> TokenV3.deserialize(noPrefixToken));
    }

    // Ensure TokenV3 deserializes from the clickable URI form
    @Test
    public void deserializationOfClickableUriTokenV3() {
        TokenV3<RandomStringSecret> tokenV3 = new TokenV3<>();
        tokenV3.setMemo("Thank you.");
        tokenV3.setUnit("sat");

        Set<TokenV3.MintProof<RandomStringSecret>> mintProofs = new LinkedHashSet<>();
        TokenV3.MintProof<RandomStringSecret> mintProof = new TokenV3.MintProof<>();
        mintProof.setMint("https://8333.space:3338");

        Set<Proof<RandomStringSecret>> proofs = new LinkedHashSet<>();
        RSSProof proof = new RSSProof();
        proof.setUnblindedSignature(Signature.fromString("02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea"));
        proof.setAmount(2);
        proof.setSecret(RandomStringSecret.fromString("407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837"));
        proof.setKeySetId("009a1f293253e41e");
        proofs.add(proof);
        mintProof.setProofs(proofs);
        mintProofs.add(mintProof);
        tokenV3.setMintProofs(mintProofs);

        String clickable = tokenV3.serialize(true);
        Assertions.assertTrue(clickable.startsWith("cashu:cashuA"));

        TokenV3<?> parsed = TokenV3.deserialize(clickable);
        Assertions.assertEquals("sat", parsed.getUnit());
        Assertions.assertEquals("Thank you.", parsed.getMemo());
    }

    // Ensure TokenV4 serialization/deserialization works correctly (functional equivalence)
    // Note: Jackson uses indefinite-length CBOR which differs from NUT-00's definite-length examples
    @Test
    public void serializationOfTokenV4SingleKeyset() {
        TokenV4 tokenV4 = new TokenV4();

        tokenV4.setMemo("Thank you");
        tokenV4.setUnit("sat");
        tokenV4.setMintUrl("http://localhost:3338");

        TokenV4.TokenData.TokenProof tokenProof = new TokenV4.TokenData.TokenProof();
        tokenProof.setAmount(1);
        tokenProof.setSecret("9a6dbb847bd232ba76db0df197216b29d3b8cc14553cd27827fc1cc942fedb4e");
        tokenProof.setSignature(Utils.hexStringToBytes("038618543ffb6b8695df4ad4babcde92a34a96bdcd97dcee0d7ccf98d472126792"));

        tokenV4.setTokenDataList(List.of(
                new TokenV4.TokenData(
                        Utils.hexStringToBytes("00ad268c4d1f5826"),
                        List.of(tokenProof)
                )
        ));

        // Verify serialization and deserialization preserve all fields
        String serialized = tokenV4.serialize(false);
        Assertions.assertTrue(serialized.startsWith("cashuB"));

        TokenV4 deserialized = TokenV4.deserialize(serialized);
        Assertions.assertEquals("http://localhost:3338", deserialized.getMintUrl());
        Assertions.assertEquals("sat", deserialized.getUnit());
        Assertions.assertEquals("Thank you", deserialized.getMemo());
        Assertions.assertEquals(1, deserialized.getTokenDataList().size());
    }
}
