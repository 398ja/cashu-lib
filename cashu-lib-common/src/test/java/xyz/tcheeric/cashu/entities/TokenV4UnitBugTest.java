package xyz.tcheeric.cashu.entities;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TokenV4UnitBugTest {

    @Test
    /**
     * Ensures clickable serialization preserves the unit field.
     */
    void shouldPreserveUnitWhenSerializedAsClickable() {
        // Arrange
        TokenV4 token = new TokenV4();
        token.setMintUrl("https://mint.example");
        token.setUnit("sat");

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("test-secret-12345");
        proof.setSignature(Utils.hexStringToBytes("0244538319de485d55bed3b29a642bee5879375ab9e7a620e11e48ba482421f3cf"));

        TokenV4.TokenData tokenData = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ffd48b8f5ecf80"),
                new ArrayList<>(List.of(proof))
        );
        token.setTokenDataList(new ArrayList<>(List.of(tokenData)));

        // Act
        String serializedToken = token.serialize(true);
        TokenV4 deserialized = TokenV4.deserialize(serializedToken);

        // Assert
        assertEquals("https://mint.example", deserialized.getMintUrl());
        assertNotNull(deserialized.getUnit());
        assertEquals("sat", deserialized.getUnit());
    }

    @Test
    /**
     * Ensures non-clickable serialization also preserves the unit field.
     */
    void shouldPreserveUnitWhenSerializedWithoutClickablePrefix() {
        // Arrange
        TokenV4 token = new TokenV4();
        token.setMintUrl("https://mint.example");
        token.setUnit("sat");

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("test-secret-12345");
        proof.setSignature(Utils.hexStringToBytes("0244538319de485d55bed3b29a642bee5879375ab9e7a620e11e48ba482421f3cf"));

        TokenV4.TokenData tokenData = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ffd48b8f5ecf80"),
                new ArrayList<>(List.of(proof))
        );
        token.setTokenDataList(new ArrayList<>(List.of(tokenData)));

        // Act
        String serializedToken = token.serialize(false);
        TokenV4 deserialized = TokenV4.deserialize(serializedToken);

        // Assert
        assertEquals("https://mint.example", deserialized.getMintUrl());
        assertNotNull(deserialized.getUnit());
        assertEquals("sat", deserialized.getUnit());
    }
}
