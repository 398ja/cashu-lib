package xyz.tcheeric.cashu.entities.rest.nut04;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NUT-23 mint quote response shape.
 */
class PostMintQuoteResponseTest {

    /**
     * Ensures a mint quote serializes every field NUT-23 requires, with the published names.
     */
    @Test
    @SneakyThrows
    void shouldSerializeAllRequiredFieldsWhenQuoteIsComplete() {
        // Arrange
        PostMintQuoteResponse quote = PostMintQuoteResponse.builder()
                .quoteId("9d745270-1405-46de-b5c5-e2762b4f5e00")
                .request("lnbc100n1p...")
                .amount(10)
                .unit("sat")
                .amountPaid(10)
                .amountIssued(0)
                .updatedAt(1701704757)
                .state("PAID")
                .expiry(1701704757)
                .build();

        // Act
        JsonNode json = JsonUtils.JSON_MAPPER.valueToTree(quote);

        // Assert
        assertThat(json.get("method").asText()).isEqualTo("bolt11");
        assertThat(json.get("amount_paid").asLong()).isEqualTo(10);
        assertThat(json.get("amount_issued").asLong()).isZero();
        assertThat(json.get("updated_at").asLong()).isEqualTo(1701704757);
    }

    /**
     * Ensures the deprecated positional constructor still builds a quote with the default method.
     */
    @Test
    void shouldDefaultToBolt11WhenBuiltFromLegacyConstructor() {
        // Act
        PostMintQuoteResponse quote =
                new PostMintQuoteResponse("quote-id", "lnbc100n1p...", true, 1701704757);

        // Assert
        assertThat(quote.getMethod()).isEqualTo("bolt11");
        assertThat(quote.isPaid()).isTrue();
    }
}
