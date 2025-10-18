package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.common.util.JsonUtils;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Detailed CBOR inspection to understand the serialization bug
 */
public class TokenV4CBORInspectTest {

    @Test
    public void inspectCBORBytes() throws Exception {
        // Create a token with unit field set
        TokenV4 original = new TokenV4();
        original.setMintUrl("https://mint");
        original.setUnit("sat");

        TokenV4.TokenData.TokenProof proof = new TokenV4.TokenData.TokenProof();
        proof.setAmount(1);
        proof.setSecret("test-secret");
        proof.setSignature(Utils.hexStringToBytes("0244538319de485d55bed3b29a642bee5879375ab9e7a620e11e48ba482421f3cf"));

        TokenV4.TokenData td = new TokenV4.TokenData(
                Utils.hexStringToBytes("00ffd48b8f5ecf80"),
                new ArrayList<>(List.of(proof))
        );

        original.setTokenDataList(new ArrayList<>(List.of(td)));

        // Serialize manually
        String serializedManual = original.serialize(false);
        System.out.println("Manual serialization: " + serializedManual);

        // Extract CBOR bytes from manual serialization
        String tokenWithoutPrefix = serializedManual.substring("cashuB".length());
        byte[] cborBytesManual = Base64.getUrlDecoder().decode(tokenWithoutPrefix);
        System.out.println("\nManual CBOR bytes (" + cborBytesManual.length + " bytes):");
        System.out.println(bytesToHex(cborBytesManual));

        // Try deserializing with Jackson directly
        ObjectMapper mapper = JsonUtils.CBOR_MAPPER;
        TokenV4 deserializedManual = mapper.readValue(cborBytesManual, TokenV4.class);
        System.out.println("\nDeserialized from manual:");
        System.out.println("  mintUrl: " + deserializedManual.getMintUrl());
        System.out.println("  unit: " + deserializedManual.getUnit());
        System.out.println("  memo: " + deserializedManual.getMemo());

        // Compare with Jackson-only serialization
        byte[] cborBytesJackson = mapper.writeValueAsBytes(original);
        System.out.println("\nJackson CBOR bytes (" + cborBytesJackson.length + " bytes):");
        System.out.println(bytesToHex(cborBytesJackson));

        TokenV4 deserializedJackson = mapper.readValue(cborBytesJackson, TokenV4.class);
        System.out.println("\nDeserialized from Jackson:");
        System.out.println("  mintUrl: " + deserializedJackson.getMintUrl());
        System.out.println("  unit: " + deserializedJackson.getUnit());
        System.out.println("  memo: " + deserializedJackson.getMemo());

        // Assertions
        assertNotNull(deserializedJackson.getUnit(), "Jackson deserialization should preserve unit");
        assertEquals("sat", deserializedJackson.getUnit());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % 16 == 0) sb.append("\n");
            sb.append(String.format("%02x ", bytes[i]));
        }
        return sb.toString();
    }
}
