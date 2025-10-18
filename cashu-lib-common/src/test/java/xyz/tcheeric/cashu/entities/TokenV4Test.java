package xyz.tcheeric.cashu.entities;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TokenV4Test {

    // Should serialize a token with multiple keysets and deserialize correctly (functional equivalence)
    // Note: We verify functional equivalence instead of byte-for-byte CBOR matching
    // Jackson uses indefinite-length CBOR maps which differ from NUT-00's definite-length examples
    @Test
    public void shouldSerializeExampleToken() {
        TokenV4 token = new TokenV4();
        token.setMintUrl("http://localhost:3338/");
        token.setUnit("sat");

        TokenV4.TokenData.TokenProof proof1 = new TokenV4.TokenData.TokenProof();
        proof1.setAmount(1);
        proof1.setSecret("acc12435e7b8484c3cf1850149218af90f716a52bf4a5ed347e48ecc13f77388");
        proof1.setSignature(Utils.hexStringToBytes("0244538319de485d55bed3b29a642bee5879375ab9e7a620e11e48ba482421f3cf"));
        TokenV4.TokenData td1 = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ffd48b8f5ecf80"),
                new ArrayList<>(List.of(proof1))
        );

        TokenV4.TokenData.TokenProof proof2a = new TokenV4.TokenData.TokenProof();
        proof2a.setAmount(2);
        proof2a.setSecret("1323d3d4707a58ad2e23ada4e9f1f49f5a5b4ac7b708eb0d61f738f48307e8ee");
        proof2a.setSignature(Utils.hexStringToBytes("023456aa110d84b4ac747aebd82c3b005aca50bf457ebd5737a4414fac3ae7d94d"));

        TokenV4.TokenData.TokenProof proof2b = new TokenV4.TokenData.TokenProof();
        proof2b.setAmount(1);
        proof2b.setSecret("56bcbcbb7cc6406b3fa5d57d2174f4eff8b4402b176926d3a57d3c3dcbb59d57");
        proof2b.setSignature(Utils.hexStringToBytes("0273129c5719e599379a974a626363c333c56cafc0e6d01abe46d5808280789c63"));

        TokenV4.TokenData td2 = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ad268c4d1f5826"),
                new ArrayList<>(List.of(proof2a, proof2b))
        );

        token.setTokenDataList(new ArrayList<>(List.of(td1, td2)));

        // Serialize and then deserialize to verify functional equivalence
        String serialized = token.serialize(false);
        assertTrue(serialized.startsWith("cashuB"));

        TokenV4 deserialized = TokenV4.deserialize(serialized);
        assertEquals("http://localhost:3338", deserialized.getMintUrl());
        assertEquals("sat", deserialized.getUnit());
        assertEquals(2, deserialized.getTokenDataList().size());
        assertEquals(1, deserialized.getTokenDataList().get(0).getProofs().size());
        assertEquals(2, deserialized.getTokenDataList().get(1).getProofs().size());
    }

    // Should deserialize the example token with multiple keysets
    @Test
    public void shouldDeserializeExampleToken() {
        String serialized = "cashuBo2F0gqJhaUgA_9SLj17PgGFwgaNhYQFhc3hAYWNjMTI0MzVlN2I4NDg0YzNjZjE4NTAxNDkyMThhZjkwZjcxNmE1MmJmN" +
                "GE1ZWQzNDdlNDhlY2MxM2Y3NzM4OGFjWCECRFODGd5IXVW-07KaZCvuWHk3WrnnpiDhHki6SCQh88-iYWlIAK0mjE0fWCZhcIKjYWECYXN4QDEzMjNkM2Q0NzA3YTU4Y" +
                "WQyZTIzYWRhNGU5ZjFmNDlmNWE1YjRhYzdiNzA4ZWIwZDYxZjczOGY0ODMwN2U4ZWVhY1ghAjRWqhENhLSsdHrr2Cw7AFrKUL9Ffr1XN6RBT6w659lNo2FhAWFzeEA1N" +
                "mJjYmNiYjdjYzY0MDZiM2ZhNWQ1N2QyMTc0ZjRlZmY4YjQ0MDJiMTc2OTI2ZDNhNTdkM2MzZGNiYjU5ZDU3YWNYIQJzEpxXGeWZN5qXSmJjY8MzxWyvwObQGr5G1YCCg" +
                "HicY2FtdWh0dHA6Ly9sb2NhbGhvc3Q6MzMzOGF1Y3NhdA";
        TokenV4 token = TokenV4.deserialize(serialized);
        assertEquals("http://localhost:3338", token.getMintUrl());
        assertEquals("sat", token.getUnit());
        assertEquals(2, token.getTokenDataList().size());
    }

    /**
     * Ensure a single-keyset token from NUT-00 deserializes with expected mint, unit, and proof count.
     */
    @Test
    public void deserializeSingleKeysetToken() {
        String serialized = "cashuBpGF0gaJhaUgArSaMTR9YJmFwgaNhYQFhc3hAOWE2ZGJiODQ3YmQyMzJiYTc2ZGIwZGYxOTcyMTZiMjlkM2I4Y2MxNDU1M2NkMjc4MjdmYzFjYzk0MmZlZGI0ZWFjWCEDhhhUP_trhpXfStS6vN6So0qWvc2X3O4NfM-Y1HISZ5JhZGlUaGFuayB5b3VhbXVodHRwOi8vbG9jYWxob3N0OjMzMzhhdWNzYXQ=";
        TokenV4 token = TokenV4.deserialize(serialized);
        assertEquals("http://localhost:3338", token.getMintUrl());
        assertEquals("sat", token.getUnit());
        assertEquals(1, token.getTokenDataList().size());
        assertEquals(1, token.getTokenDataList().iterator().next().getProofs().size());
    }

    // Verifies a clickable URI token (cashu:cashuB...) deserializes and preserves core fields
    @Test
    public void shouldDeserializeClickableUriTokenV4() {
        TokenV4 token = new TokenV4();
        token.setMintUrl("http://localhost:3338/");
        token.setUnit("sat");

        TokenV4.TokenData.TokenProof proof1 = new TokenV4.TokenData.TokenProof();
        proof1.setAmount(1);
        proof1.setSecret("acc12435e7b8484c3cf1850149218af90f716a52bf4a5ed347e48ecc13f77388");
        proof1.setSignature(Utils.hexStringToBytes("0244538319de485d55bed3b29a642bee5879375ab9e7a620e11e48ba482421f3cf"));
        TokenV4.TokenData td1 = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ffd48b8f5ecf80"),
                new ArrayList<>(List.of(proof1))
        );

        token.setTokenDataList(new ArrayList<>(List.of(td1)));

        String clickable = token.serialize(true);
        assertTrue(clickable.startsWith("cashu:cashuB"));

        TokenV4 roundTrip = TokenV4.deserialize(clickable);
        assertEquals("http://localhost:3338", roundTrip.getMintUrl());
        assertEquals("sat", roundTrip.getUnit());
        assertEquals(1, roundTrip.getTokenDataList().size());
        assertEquals(1, roundTrip.getTokenDataList().get(0).getProofs().size());
    }

    // Ensures DLEQ proof fields and witness survive serialize/deserialize round-trip
    @Test
    public void shouldSerializeDeserializeWithDLEQAndWitness() {
        TokenV4 token = new TokenV4();
        token.setMintUrl("http://localhost:3338");
        token.setUnit("sat");

        TokenV4.TokenData.TokenProof.DLEQProof dleq = new TokenV4.TokenData.TokenProof.DLEQProof();
        dleq.setE(Utils.hexStringToBytes("0a0b0c"));
        dleq.setS(Utils.hexStringToBytes("0d0e0f"));
        dleq.setR(Utils.hexStringToBytes("01020304"));

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        proof.setSignature(Utils.hexStringToBytes("038618543ffb6b8695df4ad4babcde92a34a96bdcd97dcee0d7ccf98d472126792"));
        proof.setDleqProof(dleq);
        proof.setWitness("w123");

        TokenV4.TokenData td = new TokenV4.TokenData(
            Utils.hexStringToBytes("00ad268c4d1f5826"),
            new ArrayList<>(List.of(proof))
        );

        token.setTokenDataList(new ArrayList<>(List.of(td)));

        String serialized = token.serialize(false);
        TokenV4 parsed = TokenV4.deserialize(serialized);

        TokenV4.TokenData.TokenProof parsedProof = parsed.getTokenDataList().get(0).getProofs().get(0);
        assertNotNull(parsedProof.getDleqProof());
        assertArrayEquals(dleq.getE(), parsedProof.getDleqProof().getE());
        assertArrayEquals(dleq.getS(), parsedProof.getDleqProof().getS());
        assertArrayEquals(dleq.getR(), parsedProof.getDleqProof().getR());
        assertEquals("w123", parsedProof.getWitness());
    }
}
