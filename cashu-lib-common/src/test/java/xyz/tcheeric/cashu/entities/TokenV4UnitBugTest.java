package xyz.tcheeric.cashu.entities;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to reproduce the "token missing unit" bug
 */
public class TokenV4UnitBugTest {

    @Test
    public void testUnitFieldPreservedAfterSerialization() {
        // Create a token with unit field set
        TokenV4 original = new TokenV4();
        original.setMintUrl("https://mint.example");
        original.setUnit("sat");

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("test-secret-12345");
        proof.setSignature(Utils.hexStringToBytes("0244538319de485d55bed3b29a642bee5879375ab9e7a620e11e48ba482421f3cf"));

        TokenV4.TokenData td = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ffd48b8f5ecf80"),
                new ArrayList<>(List.of(proof))
        );

        original.setTokenDataList(new ArrayList<>(List.of(td)));

        // Serialize with clickable=true (as mentioned in the bug report)
        String serialized = original.serialize(true);
        System.out.println("Serialized token: " + serialized);

        // Deserialize
        TokenV4 deserialized = TokenV4.deserialize(serialized);

        // Verify all fields are preserved
        assertEquals("https://mint.example", deserialized.getMintUrl(), "Mint URL should be preserved");
        assertNotNull(deserialized.getUnit(), "Unit should not be null after deserialization");
        assertEquals("sat", deserialized.getUnit(), "Unit should be 'sat' after deserialization");
    }

    @Test
    public void testUnitFieldWithNonClickable() {
        // Create a token with unit field set
        TokenV4 original = new TokenV4();
        original.setMintUrl("https://mint.example");
        original.setUnit("sat");

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("test-secret-12345");
        proof.setSignature(Utils.hexStringToBytes("0244538319de485d55bed3b29a642bee5879375ab9e7a620e11e48ba482421f3cf"));

        TokenV4.TokenData td = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ffd48b8f5ecf80"),
                new ArrayList<>(List.of(proof))
        );

        original.setTokenDataList(new ArrayList<>(List.of(td)));

        // Serialize with clickable=false
        String serialized = original.serialize(false);
        System.out.println("Serialized token (non-clickable): " + serialized);

        // Deserialize
        TokenV4 deserialized = TokenV4.deserialize(serialized);

        // Verify all fields are preserved
        assertEquals("https://mint.example", deserialized.getMintUrl(), "Mint URL should be preserved");
        assertNotNull(deserialized.getUnit(), "Unit should not be null after deserialization");
        assertEquals("sat", deserialized.getUnit(), "Unit should be 'sat' after deserialization");
    }
}
