package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CryptoElementJsonCreatorTest {

    @Test
    public void deserializePrivateKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String value = "0a498f8f0e00c235f5959c878abb9cf5533323971d729836423076a92aa2e03a";
        PrivateKey key = mapper.readValue("\"" + value + "\"", PrivateKey.class);
        assertEquals(PrivateKey.fromString(value), key);
    }

    @Test
    public void deserializePublicKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String value = "fc1f245f363dac26acaeb57cc457264cf6c9ccf512fdf2684a4645a3b0f59d44";
        PublicKey key = mapper.readValue("\"" + value + "\"", PublicKey.class);
        assertEquals(PublicKey.fromString(value), key);
    }

    @Test
    public void deserializeSignature() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String value = "991562096c11c1d8798a7557a278b81e6fed29afa020c73f3e090489687afc06d22566fe0d43795d2c39aca75fc0717a28e7c3fee2af212287f0804dbe5e9f22";
        Signature sig = mapper.readValue("\"" + value + "\"", Signature.class);
        assertEquals(Signature.fromString(value), sig);
    }
}
