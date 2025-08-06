package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SecretUtilTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testP2PKSecretSigInputs() throws Exception {
        String json = "[\"P2PK\", {\"nonce\": \"859d4935c4907062a6297cf4e663e2835d90d97ecdd510745d32f6816323a41f\", " +
                "\"data\": \"0249098aa8b9d2fbec49ff8598feb17b592b986e62319a4fa488a3dc36387157a7\", " +
                "\"tags\": [[\"sigflag\",\"SIG_INPUTS\"]]}]";
        List<?> raw = mapper.readValue(json, new TypeReference<List<?>>() {});
        Secret secret = SecretUtil.<Secret>toSecret(raw);
        assertThat(secret).isInstanceOf(P2PKSecret.class);
        P2PKSecret p2pk = (P2PKSecret) secret;
        assertThat(p2pk.getSigFlag()).isEqualTo(P2PKSecret.SignatureFlag.SIG_INPUTS.name());
    }
}
