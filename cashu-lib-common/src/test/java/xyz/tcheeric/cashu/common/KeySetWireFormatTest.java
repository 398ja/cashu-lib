package xyz.tcheeric.cashu.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The wire shape of a keyset in {@code GET /v1/keys}.
 *
 * <p>NUT-01 lists {@code active} among the fields of every keyset in that
 * response. This mint omitted it, and the consequence was not a missing
 * field: wallets model it as required, so the entire response failed to
 * deserialise with a pydantic {@code ValidationError}. A current Nutshell
 * wallet could therefore obtain no keyset at all and reported
 *
 * <pre>
 *   KeysetNotFoundError: no active keysets found for unit sat
 *   (or they are unsupported by this wallet)
 * </pre>
 *
 * <p>which reads like the mint has no keys rather than like a schema defect.
 * Nothing in either repository asserted the wire shape of this endpoint, so
 * the omission was invisible to every test while being fatal to every modern
 * wallet.
 */
@DisplayName("NUT-01 — a keyset on the wire carries every field the spec lists")
class KeySetWireFormatTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static KeySet sampleKeySet() {
        Keys keys = new Keys();
        keys.put(BigInteger.ONE,
                PublicKey.fromString(
                        "0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798"));
        return KeySet.builder()
                .id("004cf8cba2f93266")
                .unit("sat")
                .keys(keys)
                .partPerThousand(0)
                .build();
    }

    @Test
    @DisplayName("active is serialised, and defaults to true")
    void activeIsPresentOnTheWire() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(sampleKeySet()));

        assertThat(json.has("active"))
                .as("""
                        A wallet that models `active` as required cannot deserialise a keyset \
                        without it, and fails the whole GET /v1/keys response rather than the \
                        one field. The mint then looks like it has no keysets at all.""")
                .isTrue();
        assertThat(json.get("active").asBoolean())
                .as("GET /v1/keys returns only active keysets (NUT-01), so true is the "
                        + "correct default for anything appearing there")
                .isTrue();
    }

    @Test
    @DisplayName("every field NUT-01 lists for a keyset is present")
    void everySpecifiedFieldIsPresent() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(sampleKeySet()));

        // final_expiry is optional and legitimately omitted when null.
        assertThat(json.fieldNames()).toIterable()
                .as("the spec's example response carries id, unit, active, input_fee_ppk "
                        + "and keys")
                .contains("id", "unit", "active", "input_fee_ppk", "keys");
    }

    /**
     * A keyset explicitly marked inactive must say so, or
     * {@code GET /v1/keys/{id}} — which may serve an inactive keyset — would
     * silently advertise it as signable.
     */
    @Test
    @DisplayName("an inactive keyset is not silently reported as active")
    void inactiveIsCarriedFaithfully() throws Exception {
        KeySet inactive = sampleKeySet();
        inactive.setActive(false);

        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(inactive));

        assertThat(json.get("active").asBoolean())
                .as("the default must not override an explicitly inactive keyset")
                .isFalse();
    }

    @Test
    @DisplayName("a keyset round-trips through the wire format")
    void roundTripsThroughJson() throws Exception {
        KeySet original = sampleKeySet();

        KeySet parsed = MAPPER.readValue(MAPPER.writeValueAsString(original), KeySet.class);

        assertThat(parsed.getId()).isEqualTo(original.getId());
        assertThat(parsed.getUnit()).isEqualTo(original.getUnit());
        assertThat(parsed.isActive()).isEqualTo(original.isActive());
        assertThat(parsed.getPartPerThousand()).isEqualTo(original.getPartPerThousand());
    }
}
