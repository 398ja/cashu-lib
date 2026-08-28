package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import xyz.tcheeric.cashu.common.nut00.CashuErrorCode;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the NUT-00 &sect; 0.2 error body: {@code {"detail": <str>, "code": <int>}}.
 */
class ErrorResponseTest {

    /** The wire shape is exactly the two spec fields, detail first, with code as a JSON number. */
    @Test
    void shouldEmitSpecShapeWhenSerialized() {
        // Arrange
        ErrorResponse response = new ErrorResponse(CashuErrorCode.proofs_already_spent);

        // Act
        String json = response.toJson();

        // Assert
        assertThat(json).isEqualTo("{\"detail\":\"Proofs already spent\",\"code\":11001}");
    }

    /**
     * The exit criterion: whatever the mint serializes is exactly what a client parses back,
     * field for field, with no lossy round trip.
     */
    @Test
    void shouldRoundTripUnchangedWhenClientParsesMintOutput() {
        // Arrange
        ErrorResponse emittedByMint = new ErrorResponse(
                CashuErrorCode.transaction_not_balanced,
                "Inputs 21 do not cover outputs 32");

        // Act
        String onTheWire = emittedByMint.toJson();
        ErrorResponse parsedByClient = ErrorResponse.fromJson(onTheWire);

        // Assert
        assertThat(parsedByClient).isEqualTo(emittedByMint);
        assertThat(parsedByClient.detail()).isEqualTo("Inputs 21 do not cover outputs 32");
        assertThat(parsedByClient.code()).isEqualTo(11005);
        assertThat(parsedByClient.toJson()).isEqualTo(onTheWire);
    }

    /**
     * A detail containing quotes and backslashes must stay valid JSON, which is the bug that
     * String.format-built payloads had.
     */
    @Test
    void shouldStayValidJsonWhenDetailContainsQuotesAndBackslashes() {
        // Arrange
        String hostileDetail = "unknown unit \"sat\\btc\", got a \\\" mess\nand a newline";
        ErrorResponse response = new ErrorResponse(CashuErrorCode.unit_not_supported, hostileDetail);

        // Act
        ErrorResponse parsed = ErrorResponse.fromJson(response.toJson());

        // Assert
        assertThat(parsed.detail()).isEqualTo(hostileDetail);
        assertThat(parsed.code()).isEqualTo(11013);
    }

    /** A client parsing a raw spec payload gets the same record the mint would have built. */
    @Test
    void shouldParseSpecExampleWhenGivenRawPayload() {
        // Arrange
        String specExample = "{\"detail\": \"oops\", \"code\": 1337}";

        // Act
        ErrorResponse parsed = ErrorResponse.fromJson(specExample);

        // Assert
        assertThat(parsed).isEqualTo(new ErrorResponse("oops", 1337));
    }

    /** A code outside our registry still parses, and reports no enum and a 500 default status. */
    @Test
    void shouldReportUnknownCodeWhenCodeIsNotRegistered() {
        // Arrange
        ErrorResponse response = new ErrorResponse("something new", 99999);

        // Act & Assert
        assertThat(response.errorCode()).isEmpty();
        assertThat(response.httpStatus()).isEqualTo(500);
    }

    /** Every enum constant produces a body whose two fields survive a serialization round trip. */
    @ParameterizedTest
    @EnumSource(CashuErrorCode.class)
    void shouldRoundTripEveryErrorCode(CashuErrorCode errorCode) {
        // Arrange
        ErrorResponse response = new ErrorResponse(errorCode);

        // Act
        ErrorResponse parsed = ErrorResponse.fromJson(response.toJson());

        // Assert
        assertThat(parsed).isEqualTo(response);
        assertThat(parsed.code()).isEqualTo(errorCode.getCode());
    }

    /** No extra fields leak onto the wire, since third-party wallets parse strictly. */
    @Test
    void shouldEmitOnlyDetailAndCodeWhenSerialized() throws Exception {
        // Arrange
        ObjectMapper mapper = JsonUtils.JSON_MAPPER;

        // Act
        JsonNode node = mapper.readTree(new ErrorResponse(CashuErrorCode.quote_expired).toJson());

        // Assert
        assertThat(node.properties()).extracting(java.util.Map.Entry::getKey)
                .containsExactly("detail", "code");
        assertThat(node.get("code").isNumber()).isTrue();
    }

    /** A payload that is not an error body is rejected with the offending input in the message. */
    @Test
    void shouldRejectMalformedPayloadWhenParsing() {
        // Arrange
        String notAnError = "not json at all";

        // Act & Assert
        assertThatThrownBy(() -> ErrorResponse.fromJson(notAnError))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not json at all");
    }
}
