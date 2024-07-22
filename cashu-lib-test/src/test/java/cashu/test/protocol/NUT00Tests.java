package cashu.test.protocol;

import cashu.common.model.CryptoElement;
import cashu.common.model.PrivateKey;
import cashu.common.model.PublicKey;
import cashu.common.model.Secret;
import cashu.common.model.Signature;
import cashu.common.model.Token;
import cashu.crypto.BDHKEUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NUT00Tests {

    @Test
    public void hashToCurveFunction() {
        Secret message = Secret.fromString("0000000000000000000000000000000000000000000000000000000000000000");
        String expected = "024cce997d3b518f739663b757deaec95bcd9473c30a14ac2fd04023a739d1a725";
        var result = BDHKEUtils.hashToCurve(message.toBytes());
        assertEquals(expected, BDHKEUtils.pointToHex(result).toString());

        message = Secret.fromString("0000000000000000000000000000000000000000000000000000000000000001");
        expected = "022e7158e11c9506f1aa4248bf531298daa7febd6194f003edcd9b93ade6253acf";
        result = BDHKEUtils.hashToCurve(message.toBytes());
        assertEquals(expected, BDHKEUtils.pointToHex(result).toString());

        message = Secret.fromString("0000000000000000000000000000000000000000000000000000000000000002");
        expected = "026cdbe15362df59cd1dd3c9c11de8aedac2106eca69236ecd9fbe117af897be4f";
        result = BDHKEUtils.hashToCurve(message.toBytes());
        assertEquals(expected, BDHKEUtils.pointToHex(result).toString());
    }


    @Test
    public void blindedSignatures() {
        // Test 1
        CryptoElement k = PrivateKey.fromString("0000000000000000000000000000000000000000000000000000000000000001");
        CryptoElement B_ = PublicKey.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2");
        CryptoElement C_ = Signature.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2");

        var result = BDHKEUtils.signBlindedMessage(B_.toBytes(), k.toBytes());
        assertEquals(C_, Signature.fromBytes(result));

        // Test 2
        k = PrivateKey.fromString("7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f");
        B_ = PublicKey.fromString("02a9acc1e48c25eeeb9289b5031cc57da9fe72f3fe2861d264bdc074209b107ba2");
        C_ = Signature.fromString("0398bc70ce8184d27ba89834d19f5199c84443c31131e48d3c1214db24247d005d");

        result = BDHKEUtils.signBlindedMessage(B_.toBytes(), k.toBytes());
        assertEquals(C_, Signature.fromBytes(result));

        // Test 3, mine
        k = PrivateKey.fromString("811d912719d64d21444862da82fe802223509c684825ed8d6ec569ebbb681f9b");
        B_ = PublicKey.fromString("03a633b63d81df8522946d09c19548561493de05b81dfc2bb2f308d3328d973537");
        C_ = Signature.fromString("03b890b660bf5d5c3ad7651c7afe3a08793be57c150ad8d0646441e68aa77ef830");

        result = BDHKEUtils.signBlindedMessage(B_.toBytes(), k.toBytes());
        assertEquals(C_, Signature.fromBytes(result));
    }


    @Test
    public void serializationOfTokenV3() throws JsonProcessingException {

        String strToken = """
                {
                  "token": [
                    {
                      "mint": "https://8333.space:3338",
                      "proofs": [
                        {
                          "amount": 2,
                          "id": "009a1f293253e41e",
                          "secret": "407915bc212be61a77e3e6d2aeb4c727980bda51cd06a6afc29e2861768a7837",
                          "C": "02bc9097997d81afb2cc7346b5e4345a9346bd2a506eb7958598a72f0cf85163ea"
                        },
                        {
                          "amount": 8,
                          "id": "009a1f293253e41e",
                          "secret": "fe15109314e61d7756b0f8ee0f23a624acaa3f4e042f61433c728c7057b931be",
                          "C": "029e8e5050b890a7d6c0968db16bc1d5d5fa040ea1de284f6ec69d61299f671059"
                        }
                      ]
                    }
                  ],
                  "unit": "sat",
                  "memo": "Thank you."
                }""";

        String expected = "cashuAeyJ0b2tlbiI6W3sibWludCI6Imh0dHBzOi8vODMzMy5zcGFjZTozMzM4IiwicHJvb2ZzIjpbeyJhbW91bnQiOjIsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6IjQwNzkxNWJjMjEyYmU2MWE3N2UzZTZkMmFlYjRjNzI3OTgwYmRhNTFjZDA2YTZhZmMyOWUyODYxNzY4YTc4MzciLCJDIjoiMDJiYzkwOTc5OTdkODFhZmIyY2M3MzQ2YjVlNDM0NWE5MzQ2YmQyYTUwNmViNzk1ODU5OGE3MmYwY2Y4NTE2M2VhIn0seyJhbW91bnQiOjgsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6ImZlMTUxMDkzMTRlNjFkNzc1NmIwZjhlZTBmMjNhNjI0YWNhYTNmNGUwNDJmNjE0MzNjNzI4YzcwNTdiOTMxYmUiLCJDIjoiMDI5ZThlNTA1MGI4OTBhN2Q2YzA5NjhkYjE2YmMxZDVkNWZhMDQwZWExZGUyODRmNmVjNjlkNjEyOTlmNjcxMDU5In1dfV0sInVuaXQiOiJzYXQiLCJtZW1vIjoiVGhhbmsgeW91LiJ9";
        assertEquals(expected, Token.serialize(strToken, false));

        ObjectMapper mapper = new ObjectMapper();
        Token token = mapper.readValue(strToken, Token.class);
        assertEquals(token, Token.deserialize(expected));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializationOfIncorrectPrefixTokenV3 () {
        String incorrectPrefixToken = "casshuAeyJ0b2tlbiI6W3sibWludCI6Imh0dHBzOi8vODMzMy5zcGFjZTozMzM4IiwicHJvb2ZzIjpbeyJhbW91bnQiOjIsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6IjQwNzkxNWJjMjEyYmU2MWE3N2UzZTZkMmFlYjRjNzI3OTgwYmRhNTFjZDA2YTZhZmMyOWUyODYxNzY4YTc4MzciLCJDIjoiMDJiYzkwOTc5OTdkODFhZmIyY2M3MzQ2YjVlNDM0NWE5MzQ2YmQyYTUwNmViNzk1ODU5OGE3MmYwY2Y4NTE2M2VhIn0seyJhbW91bnQiOjgsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6ImZlMTUxMDkzMTRlNjFkNzc1NmIwZjhlZTBmMjNhNjI0YWNhYTNmNGUwNDJmNjE0MzNjNzI4YzcwNTdiOTMxYmUiLCJDIjoiMDI5ZThlNTA1MGI4OTBhN2Q2YzA5NjhkYjE2YmMxZDVkNWZhMDQwZWExZGUyODRmNmVjNjlkNjEyOTlmNjcxMDU5In1dfV0sInVuaXQiOiJzYXQiLCJtZW1vIjoiVGhhbmsgeW91LiJ9";
        Token.deserialize(incorrectPrefixToken);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deserializationOfNoPrefixTokenV3 () {
        String noPrefixToken  = "eyJ0b2tlbiI6W3sibWludCI6Imh0dHBzOi8vODMzMy5zcGFjZTozMzM4IiwicHJvb2ZzIjpbeyJhbW91bnQiOjIsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6IjQwNzkxNWJjMjEyYmU2MWE3N2UzZTZkMmFlYjRjNzI3OTgwYmRhNTFjZDA2YTZhZmMyOWUyODYxNzY4YTc4MzciLCJDIjoiMDJiYzkwOTc5OTdkODFhZmIyY2M3MzQ2YjVlNDM0NWE5MzQ2YmQyYTUwNmViNzk1ODU5OGE3MmYwY2Y4NTE2M2VhIn0seyJhbW91bnQiOjgsImlkIjoiMDA5YTFmMjkzMjUzZTQxZSIsInNlY3JldCI6ImZlMTUxMDkzMTRlNjFkNzc1NmIwZjhlZTBmMjNhNjI0YWNhYTNmNGUwNDJmNjE0MzNjNzI4YzcwNTdiOTMxYmUiLCJDIjoiMDI5ZThlNTA1MGI4OTBhN2Q2YzA5NjhkYjE2YmMxZDVkNWZhMDQwZWExZGUyODRmNmVjNjlkNjEyOTlmNjcxMDU5In1dfV0sInVuaXQiOiJzYXQiLCJtZW1vIjoiVGhhbmsgeW91LiJ9";
        Token.deserialize(noPrefixToken);
    }
}
