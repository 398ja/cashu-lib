package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.P2PKSecret;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.util.SecretUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecretUtilTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Ensures the secret utility converts a P2PK payload with a SIG_INPUTS flag into a P2PKSecret instance.
     */
    @Test
    void shouldCreateP2PKSecretWithSigInputsFlag() throws Exception {
        // Arrange
        String json = "[\"P2PK\", {\"nonce\": \"859d4935c4907062a6297cf4e663e2835d90d97ecdd510745d32f6816323a41f\", " +
                "\"data\": \"0249098aa8b9d2fbec49ff8598feb17b592b986e62319a4fa488a3dc36387157a7\", " +
                "\"tags\": [[\"sigflag\",\"SIG_INPUTS\"]]}]";
        List<?> rawSecret = mapper.readValue(json, new TypeReference<List<?>>() {});

        // Act
        Secret secret = SecretUtil.<Secret>toSecret(rawSecret);

        // Assert
        assertThat(secret).isInstanceOf(P2PKSecret.class);
        P2PKSecret p2pkSecret = (P2PKSecret) secret;
        assertThat(p2pkSecret.getSigFlag()).isEqualTo(P2PKSecret.SignatureFlag.SIG_INPUTS.name());
    }
}
