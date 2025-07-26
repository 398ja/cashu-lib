package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CryptoElementJsonCreatorTest {

    @Test
    public void deserializePrivateKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String value = PrivateKey.generateRandom().toString();
        PrivateKey key = mapper.readValue("\"" + value + "\"", PrivateKey.class);
        assertEquals(PrivateKey.fromString(value), key);
    }

    @Test
    public void deserializePublicKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String value = PrivateKey.derivePublicKey(PrivateKey.generateRandom()).toString();
        PublicKey key = mapper.readValue("\"" + value + "\"", PublicKey.class);
        assertEquals(PublicKey.fromString(value), key);
    }

    @Test
    public void deserializeSignature() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String value = "029e8e5050b890a7d6c0968db16bc1d5d5fa040ea1de284f6ec69d61299f671059";
        Signature sig = mapper.readValue("\"" + value + "\"", Signature.class);
        assertEquals(Signature.fromString(value), sig);
    }
}
