package xyz.tcheeric.cashu.entities.rest.nut05;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The wire shape of {@code POST /v1/melt/{method}}.
 *
 * <p>NUT-05 defines the melt response as the melt <em>quote</em> response plus
 * the method-specific proof of payment. This mint returned only
 * {@code {paid, payment_preimage, change}} — the pre-NUT-23 shape — and a
 * current wallet rejected it with eight missing fields.
 *
 * <p>That failure is worse than a refusal. By the time the response is
 * written the melt has happened: the invoice is paid and the customer's
 * inputs are spent. A wallet that cannot parse the answer sees something
 * indistinguishable from a failure, and if it retries it has already lost the
 * proofs. Nothing asserted this endpoint's shape, so the drift was invisible
 * here while being fatal in every modern wallet.
 */
@DisplayName("NUT-05 — the melt response carries the quote fields, not just `paid`")
class PostMeltResponseWireFormatTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static PostMeltResponse paidMelt() {
        PostMeltResponse response = new PostMeltResponse();
        response.setQuoteId("019e6d5a-2347-7000-89e2-35fe79f92c0e");
        response.setRequest("lnbc100n1p3kdrv5sp5...");
        response.setAmount(10);
        response.setUnit("sat");
        response.setMethod("bolt11");
        response.setFeeReserve(2);
        response.setState(MeltQuoteState.PAID);
        response.setExpiry(1701704757);
        response.setPaymentPreimage("c5a1ae1f639e1f4a3872e81500fd028bece7bedc1152f740cba5c3417b748c1b");
        response.setPaid(true);
        return response;
    }

    /**
     * The exact field set a current wallet requires. Listed explicitly rather
     * than looped, so a future removal names which one went.
     */
    @Test
    @DisplayName("every field NUT-05 lists is present on the wire")
    void everySpecifiedFieldIsPresent() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(paidMelt()));

        assertThat(json.fieldNames()).toIterable()
                .as("""
                        These are the eight a current Nutshell wallet reported missing. It \
                        models them as required, so their absence fails the whole response to \
                        deserialise rather than losing one field.""")
                .contains("quote", "amount", "unit", "method", "request",
                        "fee_reserve", "state", "expiry");
    }

    @Test
    @DisplayName("state is the spec enum, serialised as its string name")
    void stateIsSerialisedAsTheSpecString() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(paidMelt()));

        assertThat(json.get("state").asText())
                .as("NUT-05 state is one of UNPAID, PENDING, PAID")
                .isEqualTo("PAID");
    }

    /**
     * The deprecated boolean stays until nothing reads it. Removing it to
     * satisfy new wallets would break every existing one, which is the
     * opposite of the problem being fixed.
     */
    @Test
    @DisplayName("the legacy `paid` boolean is retained alongside `state`")
    void legacyPaidIsRetained() throws Exception {
        JsonNode json = MAPPER.readTree(MAPPER.writeValueAsString(paidMelt()));

        assertThat(json.has("paid"))
                .as("wallets still reading `paid` must keep working through the migration")
                .isTrue();
        assertThat(json.get("paid").asBoolean()).isTrue();
    }

    /**
     * {@code paid} and {@code state} carry the same fact in two vocabularies.
     * If they can disagree, one of them is lying to whichever wallet reads it.
     */
    @Test
    @DisplayName("paid and state agree")
    void paidAndStateAgree() throws Exception {
        JsonNode paid = MAPPER.readTree(MAPPER.writeValueAsString(paidMelt()));

        assertThat(paid.get("paid").asBoolean())
                .as("PAID state with paid=false would be a contradiction on the wire")
                .isEqualTo(MeltQuoteState.PAID.name().equals(paid.get("state").asText()));
    }

    @Test
    @DisplayName("the response round-trips")
    void roundTrips() throws Exception {
        PostMeltResponse original = paidMelt();

        PostMeltResponse parsed =
                MAPPER.readValue(MAPPER.writeValueAsString(original), PostMeltResponse.class);

        assertThat(parsed.getQuoteId()).isEqualTo(original.getQuoteId());
        assertThat(parsed.getState()).isEqualTo(original.getState());
        assertThat(parsed.getUnit()).isEqualTo(original.getUnit());
        assertThat(parsed.getAmount()).isEqualTo(original.getAmount());
        assertThat(parsed.getPaymentPreimage()).isEqualTo(original.getPaymentPreimage());
    }
}
