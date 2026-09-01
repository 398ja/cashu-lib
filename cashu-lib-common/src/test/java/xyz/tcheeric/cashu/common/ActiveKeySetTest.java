package xyz.tcheeric.cashu.common;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a {@code GET /v1/keysets} entry carries the NUT-02 fee and expiry fields.
 */
class ActiveKeySetTest {

    private static final String KEYSET_ID = "009a1f293253e41e";
    private static final int INPUT_FEE_PPK = 100;
    private static final long FINAL_EXPIRY = 2059210353L;

    /** The listing entry takes its fee from the keyset it was built from. */
    @Test
    void shouldPropagateInputFeeWhenBuiltFromKeySet() {
        ActiveKeySet entry = ActiveKeySet.fromKeySet(keySet(), true);

        assertThat(entry.getInputFeePpk()).isEqualTo(INPUT_FEE_PPK);
    }

    /** A keyset with no expiry serializes to the NUT-02 shape without a final_expiry field. */
    @Test
    void shouldOmitFinalExpiryWhenItIsNotSet() throws Exception {
        ActiveKeySet entry = ActiveKeySet.fromKeySet(keySet(), true);

        String json = JsonUtils.JSON_MAPPER.writeValueAsString(entry);

        assertThat(json).isEqualTo(
                "{\"id\":\"" + KEYSET_ID + "\",\"unit\":\"sat\",\"active\":true,\"input_fee_ppk\":" + INPUT_FEE_PPK + "}");
    }

    /** A keyset with an expiry serializes it under the spec's final_expiry key. */
    @Test
    void shouldSerializeFinalExpiryWhenItIsSet() throws Exception {
        ActiveKeySet entry = ActiveKeySet.fromKeySet(keySet(), false, FINAL_EXPIRY);

        String json = JsonUtils.JSON_MAPPER.writeValueAsString(entry);

        assertThat(json).isEqualTo(
                "{\"id\":\"" + KEYSET_ID + "\",\"unit\":\"sat\",\"active\":false,\"input_fee_ppk\":"
                        + INPUT_FEE_PPK + ",\"final_expiry\":" + FINAL_EXPIRY + "}");
    }

    /** Both new fields survive a JSON round trip. */
    @Test
    void shouldRoundTripFeeAndExpiryWhenDeserialized() throws Exception {
        ActiveKeySet entry = ActiveKeySet.fromKeySet(keySet(), true, FINAL_EXPIRY);

        String json = JsonUtils.JSON_MAPPER.writeValueAsString(entry);
        ActiveKeySet parsed = JsonUtils.JSON_MAPPER.readValue(json, ActiveKeySet.class);

        assertThat(parsed).isEqualTo(entry);
    }

    private static KeySet keySet() {
        KeySet keySet = new KeySet();
        keySet.setId(KEYSET_ID);
        keySet.setUnit("sat");
        keySet.setPartPerThousand(INPUT_FEE_PPK);
        return keySet;
    }
}
