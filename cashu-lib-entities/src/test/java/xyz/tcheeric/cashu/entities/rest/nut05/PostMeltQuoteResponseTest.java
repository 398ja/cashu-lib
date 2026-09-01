package xyz.tcheeric.cashu.entities.rest.nut05;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NUT-05 / NUT-23 melt quote response shape and state handling.
 */
class PostMeltQuoteResponseTest {

    /**
     * Ensures a melt quote serializes every field NUT-23 requires, with the published names.
     */
    @Test
    void shouldSerializeAllRequiredFieldsWhenQuoteIsComplete() {
        // Arrange
        PostMeltQuoteResponse quote = PostMeltQuoteResponse.builder()
                .quoteId("TRmjduhIsPxd...")
                .request("lnbc100n1p...")
                .amount(10)
                .unit("sat")
                .feeReserve(2)
                .state(MeltQuoteState.PAID)
                .expiry(1701704757)
                .paymentPreimage("c5a1ae1f639e1...")
                .build();

        // Act
        JsonNode json = JsonUtils.JSON_MAPPER.valueToTree(quote);

        // Assert
        assertThat(json.get("request").asText()).isEqualTo("lnbc100n1p...");
        assertThat(json.get("unit").asText()).isEqualTo("sat");
        assertThat(json.get("method").asText()).isEqualTo("bolt11");
        assertThat(json.get("state").asText()).isEqualTo("PAID");
        assertThat(json.get("payment_preimage").asText()).isEqualTo("c5a1ae1f639e1...");
    }

    /**
     * Ensures an in-flight payment is reported as PENDING, which the deprecated boolean cannot say.
     */
    @Test
    void shouldReportPendingWhenPaymentIsInFlight() {
        // Arrange
        PostMeltQuoteResponse quote = new PostMeltQuoteResponse();

        // Act
        quote.setState(MeltQuoteState.PENDING);

        // Assert
        assertThat(quote.getState()).isEqualTo(MeltQuoteState.PENDING);
        assertThat(quote.isPaid()).isFalse();
    }

    /**
     * Ensures a pending quote is not downgraded to UNPAID by the deprecated boolean setter.
     */
    @Test
    void shouldKeepPendingWhenDeprecatedPaidFlagIsSetFalse() {
        // Arrange
        PostMeltQuoteResponse quote = new PostMeltQuoteResponse();
        quote.setState(MeltQuoteState.PENDING);

        // Act
        quote.setPaid(false);

        // Assert
        assertThat(quote.getState()).isEqualTo(MeltQuoteState.PENDING);
    }

    /**
     * Ensures the deprecated positional constructor keeps the state and the boolean consistent.
     */
    @Test
    void shouldDeriveStateWhenBuiltFromLegacyConstructor() {
        // Act
        PostMeltQuoteResponse quote = new PostMeltQuoteResponse("quote-id", 10, 2, true, 1701704757);

        // Assert
        assertThat(quote.getState()).isEqualTo(MeltQuoteState.PAID);
        assertThat(quote.getMethod()).isEqualTo("bolt11");
    }

    /**
     * Ensures a melt quote published by another implementation parses back into the state enum.
     */
    @Test
    void shouldDeserializeWhenStateIsPending() throws Exception {
        // Arrange
        String json = """
                {"quote":"q","request":"lnbc100n1p...","amount":10,"unit":"sat",\
                "method":"bolt11","fee_reserve":2,"state":"PENDING","expiry":1701704757,\
                "payment_preimage":null}""";

        // Act
        PostMeltQuoteResponse quote =
                JsonUtils.JSON_MAPPER.readValue(json, PostMeltQuoteResponse.class);

        // Assert
        assertThat(quote.getState()).isEqualTo(MeltQuoteState.PENDING);
        assertThat(quote.isPaid()).isFalse();
    }
}
