package xyz.tcheeric.cashu.common.nut17;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NUT-17 JSON-RPC Serialization")
class JsonRpcSerializationTest {

    private static final ObjectMapper MAPPER = JsonUtils.JSON_MAPPER;

    @Nested
    @DisplayName("SubscriptionKind")
    class SubscriptionKindTests {

        @Test
        void shouldSerializeBolt11MintQuote() throws Exception {
            String json = MAPPER.writeValueAsString(SubscriptionKind.bolt11_mint_quote);
            assertThat(json).isEqualTo("\"bolt11_mint_quote\"");
        }

        @Test
        void shouldSerializeBolt11MeltQuote() throws Exception {
            String json = MAPPER.writeValueAsString(SubscriptionKind.bolt11_melt_quote);
            assertThat(json).isEqualTo("\"bolt11_melt_quote\"");
        }

        @Test
        void shouldSerializeProofState() throws Exception {
            String json = MAPPER.writeValueAsString(SubscriptionKind.proof_state);
            assertThat(json).isEqualTo("\"proof_state\"");
        }

        @Test
        void shouldDeserializeBolt11MintQuote() throws Exception {
            SubscriptionKind kind = MAPPER.readValue("\"bolt11_mint_quote\"", SubscriptionKind.class);
            assertThat(kind).isEqualTo(SubscriptionKind.bolt11_mint_quote);
        }

        @Test
        void shouldDeserializeBolt11MeltQuote() throws Exception {
            SubscriptionKind kind = MAPPER.readValue("\"bolt11_melt_quote\"", SubscriptionKind.class);
            assertThat(kind).isEqualTo(SubscriptionKind.bolt11_melt_quote);
        }

        @Test
        void shouldDeserializeProofState() throws Exception {
            SubscriptionKind kind = MAPPER.readValue("\"proof_state\"", SubscriptionKind.class);
            assertThat(kind).isEqualTo(SubscriptionKind.proof_state);
        }
    }

    @Nested
    @DisplayName("JsonRpcRequest")
    class JsonRpcRequestTests {

        @Test
        void shouldDefaultJsonrpcTo2_0() throws Exception {
            JsonRpcRequest request = new JsonRpcRequest();

            String json = MAPPER.writeValueAsString(request);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("jsonrpc").asText()).isEqualTo("2.0");
        }

        @Test
        void shouldSerializeCompleteRequest() throws Exception {
            SubscriptionFilter filter = new SubscriptionFilter(List.of("quote-123", "quote-456"));
            SubscriptionParams params = new SubscriptionParams(
                    SubscriptionKind.bolt11_mint_quote,
                    List.of(filter),
                    null
            );
            JsonRpcRequest request = new JsonRpcRequest("2.0", "subscribe", params, "req-1");

            String json = MAPPER.writeValueAsString(request);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("jsonrpc").asText()).isEqualTo("2.0");
            assertThat(node.get("method").asText()).isEqualTo("subscribe");
            assertThat(node.get("id").asText()).isEqualTo("req-1");
            assertThat(node.get("params").get("kind").asText()).isEqualTo("bolt11_mint_quote");
            assertThat(node.get("params").get("filters").get(0).get("ids").get(0).asText()).isEqualTo("quote-123");
        }

        @Test
        void shouldDeserializeRequest() throws Exception {
            String json = """
                {
                    "jsonrpc": "2.0",
                    "method": "subscribe",
                    "params": {
                        "kind": "proof_state",
                        "filters": [{"ids": ["Y1", "Y2"]}]
                    },
                    "id": "abc-123"
                }
                """;

            JsonRpcRequest request = MAPPER.readValue(json, JsonRpcRequest.class);

            assertThat(request.getJsonrpc()).isEqualTo("2.0");
            assertThat(request.getMethod()).isEqualTo("subscribe");
            assertThat(request.getId()).isEqualTo("abc-123");
            assertThat(request.getParams().getKind()).isEqualTo(SubscriptionKind.proof_state);
            assertThat(request.getParams().getFilters()).hasSize(1);
            assertThat(request.getParams().getFilters().get(0).getIds()).containsExactly("Y1", "Y2");
        }

        @Test
        void shouldRoundTripRequest() throws Exception {
            SubscriptionFilter filter = new SubscriptionFilter(List.of("id1", "id2"));
            SubscriptionParams params = new SubscriptionParams(
                    SubscriptionKind.bolt11_melt_quote,
                    List.of(filter),
                    null
            );
            JsonRpcRequest original = new JsonRpcRequest("2.0", "subscribe", params, "test-id");

            String json = MAPPER.writeValueAsString(original);
            JsonRpcRequest restored = MAPPER.readValue(json, JsonRpcRequest.class);

            assertThat(restored.getJsonrpc()).isEqualTo(original.getJsonrpc());
            assertThat(restored.getMethod()).isEqualTo(original.getMethod());
            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getParams().getKind()).isEqualTo(original.getParams().getKind());
        }
    }

    @Nested
    @DisplayName("JsonRpcResponse")
    class JsonRpcResponseTests {

        @Test
        void shouldDefaultJsonrpcTo2_0() throws Exception {
            JsonRpcResponse response = new JsonRpcResponse();

            String json = MAPPER.writeValueAsString(response);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("jsonrpc").asText()).isEqualTo("2.0");
        }

        @Test
        void shouldSerializeSuccessResponse() throws Exception {
            SubscriptionResult result = SubscriptionResult.ok("sub-123");
            JsonRpcResponse response = JsonRpcResponse.success("req-1", result);

            String json = MAPPER.writeValueAsString(response);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("jsonrpc").asText()).isEqualTo("2.0");
            assertThat(node.get("id").asText()).isEqualTo("req-1");
            assertThat(node.get("result").get("status").asText()).isEqualTo("OK");
            assertThat(node.get("result").get("subId").asText()).isEqualTo("sub-123");
            assertThat(node.has("error")).isFalse();
        }

        @Test
        void shouldSerializeErrorResponse() throws Exception {
            JsonRpcResponse response = JsonRpcResponse.error("req-2", JsonRpcError.INVALID_PARAMS, "Invalid kind");

            String json = MAPPER.writeValueAsString(response);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("jsonrpc").asText()).isEqualTo("2.0");
            assertThat(node.get("id").asText()).isEqualTo("req-2");
            assertThat(node.get("error").get("code").asInt()).isEqualTo(-32602);
            assertThat(node.get("error").get("message").asText()).isEqualTo("Invalid kind");
            assertThat(node.has("result")).isFalse();
        }

        @Test
        void shouldOmitNullFields() throws Exception {
            JsonRpcResponse response = JsonRpcResponse.success("id-1", "simple result");

            String json = MAPPER.writeValueAsString(response);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.has("error")).isFalse();
        }

        @Test
        void shouldDeserializeSuccessResponse() throws Exception {
            String json = """
                {
                    "jsonrpc": "2.0",
                    "result": {"status": "OK", "subId": "sub-abc"},
                    "id": "req-123"
                }
                """;

            JsonRpcResponse response = MAPPER.readValue(json, JsonRpcResponse.class);

            assertThat(response.getJsonrpc()).isEqualTo("2.0");
            assertThat(response.getId()).isEqualTo("req-123");
            assertThat(response.getError()).isNull();
        }

        @Test
        void shouldDeserializeErrorResponse() throws Exception {
            String json = """
                {
                    "jsonrpc": "2.0",
                    "error": {"code": -32601, "message": "Method not found"},
                    "id": "req-456"
                }
                """;

            JsonRpcResponse response = MAPPER.readValue(json, JsonRpcResponse.class);

            assertThat(response.getJsonrpc()).isEqualTo("2.0");
            assertThat(response.getId()).isEqualTo("req-456");
            assertThat(response.getError().getCode()).isEqualTo(-32601);
            assertThat(response.getError().getMessage()).isEqualTo("Method not found");
        }
    }

    @Nested
    @DisplayName("JsonRpcNotification")
    class JsonRpcNotificationTests {

        @Test
        void shouldDefaultJsonrpcTo2_0() throws Exception {
            JsonRpcNotification notification = new JsonRpcNotification();

            String json = MAPPER.writeValueAsString(notification);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("jsonrpc").asText()).isEqualTo("2.0");
        }

        @Test
        void shouldDefaultMethodToNotification() throws Exception {
            JsonRpcNotification notification = new JsonRpcNotification();

            String json = MAPPER.writeValueAsString(notification);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("method").asText()).isEqualTo("notification");
        }

        @Test
        void shouldSerializeNotificationWithParams() throws Exception {
            ProofStatePayload payload = new ProofStatePayload("02abc123", "SPENT", null);
            JsonRpcNotification notification = JsonRpcNotification.of("sub-1", payload);

            String json = MAPPER.writeValueAsString(notification);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("jsonrpc").asText()).isEqualTo("2.0");
            assertThat(node.get("method").asText()).isEqualTo("notification");
            assertThat(node.get("params").get("subId").asText()).isEqualTo("sub-1");
        }

        @Test
        void shouldDeserializeNotification() throws Exception {
            String json = """
                {
                    "jsonrpc": "2.0",
                    "method": "notification",
                    "params": {
                        "subId": "sub-xyz",
                        "payload": {"Y": "02def456", "state": "UNSPENT"}
                    }
                }
                """;

            JsonRpcNotification notification = MAPPER.readValue(json, JsonRpcNotification.class);

            assertThat(notification.getJsonrpc()).isEqualTo("2.0");
            assertThat(notification.getMethod()).isEqualTo("notification");
            assertThat(notification.getParams().getSubId()).isEqualTo("sub-xyz");
        }

        @Test
        void shouldRoundTripNotification() throws Exception {
            JsonRpcNotification original = JsonRpcNotification.of("test-sub", "simple payload");

            String json = MAPPER.writeValueAsString(original);
            JsonRpcNotification restored = MAPPER.readValue(json, JsonRpcNotification.class);

            assertThat(restored.getJsonrpc()).isEqualTo(original.getJsonrpc());
            assertThat(restored.getMethod()).isEqualTo(original.getMethod());
            assertThat(restored.getParams().getSubId()).isEqualTo(original.getParams().getSubId());
        }
    }

    @Nested
    @DisplayName("SubscriptionParams")
    class SubscriptionParamsTests {

        @Test
        void shouldSerializeSubIdWithCorrectFieldName() throws Exception {
            SubscriptionParams params = new SubscriptionParams(null, null, "my-sub-id");

            String json = MAPPER.writeValueAsString(params);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("subId").asText()).isEqualTo("my-sub-id");
            assertThat(node.has("sub_id")).isFalse();
        }

        @Test
        void shouldDeserializeSubIdFromCamelCase() throws Exception {
            String json = """
                {
                    "kind": "bolt11_mint_quote",
                    "subId": "existing-sub"
                }
                """;

            SubscriptionParams params = MAPPER.readValue(json, SubscriptionParams.class);

            assertThat(params.getSubId()).isEqualTo("existing-sub");
            assertThat(params.getKind()).isEqualTo(SubscriptionKind.bolt11_mint_quote);
        }

        @Test
        void shouldRoundTripParams() throws Exception {
            SubscriptionFilter filter = new SubscriptionFilter(List.of("quote-1"));
            SubscriptionParams original = new SubscriptionParams(
                    SubscriptionKind.bolt11_melt_quote,
                    List.of(filter),
                    "sub-abc"
            );

            String json = MAPPER.writeValueAsString(original);
            SubscriptionParams restored = MAPPER.readValue(json, SubscriptionParams.class);

            assertThat(restored.getKind()).isEqualTo(original.getKind());
            assertThat(restored.getSubId()).isEqualTo(original.getSubId());
            assertThat(restored.getFilters()).hasSize(1);
            assertThat(restored.getFilters().get(0).getIds()).containsExactly("quote-1");
        }
    }

    @Nested
    @DisplayName("NotificationParams")
    class NotificationParamsTests {

        @Test
        void shouldSerializeSubIdWithCorrectFieldName() throws Exception {
            NotificationParams params = new NotificationParams("notif-sub", "test-payload");

            String json = MAPPER.writeValueAsString(params);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("subId").asText()).isEqualTo("notif-sub");
        }

        @Test
        void shouldDeserializeSubId() throws Exception {
            String json = """
                {
                    "subId": "my-notification-sub",
                    "payload": {"data": "value"}
                }
                """;

            NotificationParams params = MAPPER.readValue(json, NotificationParams.class);

            assertThat(params.getSubId()).isEqualTo("my-notification-sub");
        }
    }

    @Nested
    @DisplayName("SubscriptionResult")
    class SubscriptionResultTests {

        @Test
        void shouldSerializeSubIdWithCorrectFieldName() throws Exception {
            SubscriptionResult result = SubscriptionResult.ok("result-sub-123");

            String json = MAPPER.writeValueAsString(result);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("subId").asText()).isEqualTo("result-sub-123");
            assertThat(node.get("status").asText()).isEqualTo("OK");
        }

        @Test
        void shouldDeserializeSubId() throws Exception {
            String json = """
                {
                    "status": "OK",
                    "subId": "deserialized-sub"
                }
                """;

            SubscriptionResult result = MAPPER.readValue(json, SubscriptionResult.class);

            assertThat(result.getSubId()).isEqualTo("deserialized-sub");
            assertThat(result.getStatus()).isEqualTo("OK");
        }

        @Test
        void shouldRoundTripResult() throws Exception {
            SubscriptionResult original = SubscriptionResult.ok("round-trip-sub");

            String json = MAPPER.writeValueAsString(original);
            SubscriptionResult restored = MAPPER.readValue(json, SubscriptionResult.class);

            assertThat(restored.getStatus()).isEqualTo(original.getStatus());
            assertThat(restored.getSubId()).isEqualTo(original.getSubId());
        }
    }

    @Nested
    @DisplayName("ProofStatePayload")
    class ProofStatePayloadTests {

        @Test
        void shouldSerializeYWithUppercaseFieldName() throws Exception {
            ProofStatePayload payload = new ProofStatePayload("02abcdef", "SPENT", null);

            String json = MAPPER.writeValueAsString(payload);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("Y").asText()).isEqualTo("02abcdef");
            assertThat(node.has("y")).isFalse();
        }

        @Test
        void shouldOmitNullWitness() throws Exception {
            ProofStatePayload payload = new ProofStatePayload("02abcdef", "UNSPENT", null);

            String json = MAPPER.writeValueAsString(payload);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.has("witness")).isFalse();
        }

        @Test
        void shouldIncludeWitnessWhenPresent() throws Exception {
            ProofStatePayload payload = new ProofStatePayload("02abcdef", "SPENT", "{\"signatures\":[]}");

            String json = MAPPER.writeValueAsString(payload);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("witness").asText()).isEqualTo("{\"signatures\":[]}");
        }

        @Test
        void shouldDeserializeFromUppercaseY() throws Exception {
            String json = """
                {
                    "Y": "03fedcba",
                    "state": "PENDING"
                }
                """;

            ProofStatePayload payload = MAPPER.readValue(json, ProofStatePayload.class);

            assertThat(payload.getY()).isEqualTo("03fedcba");
            assertThat(payload.getState()).isEqualTo("PENDING");
        }

        @Test
        void shouldRoundTripPayload() throws Exception {
            ProofStatePayload original = new ProofStatePayload("02123456", "UNSPENT", "witness-data");

            String json = MAPPER.writeValueAsString(original);
            ProofStatePayload restored = MAPPER.readValue(json, ProofStatePayload.class);

            assertThat(restored.getY()).isEqualTo(original.getY());
            assertThat(restored.getState()).isEqualTo(original.getState());
            assertThat(restored.getWitness()).isEqualTo(original.getWitness());
        }
    }

    @Nested
    @DisplayName("QuoteStatePayload")
    class QuoteStatePayloadTests {

        @Test
        void shouldSerializeQuoteIdAsQuoteField() throws Exception {
            QuoteStatePayload payload = new QuoteStatePayload();
            payload.setQuoteId("quote-xyz");
            payload.setState("PAID");

            String json = MAPPER.writeValueAsString(payload);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("quote").asText()).isEqualTo("quote-xyz");
            assertThat(node.has("quoteId")).isFalse();
        }

        @Test
        void shouldSerializeFeeReserveWithSnakeCase() throws Exception {
            QuoteStatePayload payload = new QuoteStatePayload();
            payload.setFeeReserve(1000L);

            String json = MAPPER.writeValueAsString(payload);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("fee_reserve").asLong()).isEqualTo(1000L);
        }

        @Test
        void shouldSerializePaymentPreimageWithSnakeCase() throws Exception {
            QuoteStatePayload payload = new QuoteStatePayload();
            payload.setPaymentPreimage("preimage123");

            String json = MAPPER.writeValueAsString(payload);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("payment_preimage").asText()).isEqualTo("preimage123");
        }

        @Test
        void shouldOmitNullFields() throws Exception {
            QuoteStatePayload payload = new QuoteStatePayload();
            payload.setQuoteId("q1");
            payload.setState("UNPAID");

            String json = MAPPER.writeValueAsString(payload);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.has("request")).isFalse();
            assertThat(node.has("paid")).isFalse();
            assertThat(node.has("expiry")).isFalse();
            assertThat(node.has("amount")).isFalse();
            assertThat(node.has("fee_reserve")).isFalse();
            assertThat(node.has("payment_preimage")).isFalse();
            assertThat(node.has("change")).isFalse();
        }

        @Test
        void shouldDeserializeFromSnakeCaseFields() throws Exception {
            String json = """
                {
                    "quote": "quote-abc",
                    "state": "ISSUED",
                    "fee_reserve": 500,
                    "payment_preimage": "preimg-xyz"
                }
                """;

            QuoteStatePayload payload = MAPPER.readValue(json, QuoteStatePayload.class);

            assertThat(payload.getQuoteId()).isEqualTo("quote-abc");
            assertThat(payload.getState()).isEqualTo("ISSUED");
            assertThat(payload.getFeeReserve()).isEqualTo(500L);
            assertThat(payload.getPaymentPreimage()).isEqualTo("preimg-xyz");
        }

        @Test
        void shouldRoundTripFullPayload() throws Exception {
            QuoteStatePayload original = new QuoteStatePayload();
            original.setQuoteId("full-quote");
            original.setRequest("lnbc...");
            original.setPaid(true);
            original.setState("PAID");
            original.setExpiry(1700000000L);
            original.setAmount(10000L);
            original.setFeeReserve(100L);
            original.setPaymentPreimage("abc123");

            String json = MAPPER.writeValueAsString(original);
            QuoteStatePayload restored = MAPPER.readValue(json, QuoteStatePayload.class);

            assertThat(restored.getQuoteId()).isEqualTo(original.getQuoteId());
            assertThat(restored.getRequest()).isEqualTo(original.getRequest());
            assertThat(restored.getPaid()).isEqualTo(original.getPaid());
            assertThat(restored.getState()).isEqualTo(original.getState());
            assertThat(restored.getExpiry()).isEqualTo(original.getExpiry());
            assertThat(restored.getAmount()).isEqualTo(original.getAmount());
            assertThat(restored.getFeeReserve()).isEqualTo(original.getFeeReserve());
            assertThat(restored.getPaymentPreimage()).isEqualTo(original.getPaymentPreimage());
        }
    }

    @Nested
    @DisplayName("JsonRpcError")
    class JsonRpcErrorTests {

        @Test
        void shouldSerializeError() throws Exception {
            JsonRpcError error = new JsonRpcError(JsonRpcError.PARSE_ERROR, "Parse error");

            String json = MAPPER.writeValueAsString(error);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("code").asInt()).isEqualTo(-32700);
            assertThat(node.get("message").asText()).isEqualTo("Parse error");
        }

        @Test
        void shouldDeserializeError() throws Exception {
            String json = """
                {
                    "code": -32603,
                    "message": "Internal error"
                }
                """;

            JsonRpcError error = MAPPER.readValue(json, JsonRpcError.class);

            assertThat(error.getCode()).isEqualTo(JsonRpcError.INTERNAL_ERROR);
            assertThat(error.getMessage()).isEqualTo("Internal error");
        }

        @Test
        void shouldHaveCorrectErrorCodeConstants() {
            assertThat(JsonRpcError.PARSE_ERROR).isEqualTo(-32700);
            assertThat(JsonRpcError.INVALID_REQUEST).isEqualTo(-32600);
            assertThat(JsonRpcError.METHOD_NOT_FOUND).isEqualTo(-32601);
            assertThat(JsonRpcError.INVALID_PARAMS).isEqualTo(-32602);
            assertThat(JsonRpcError.INTERNAL_ERROR).isEqualTo(-32603);
        }
    }

    @Nested
    @DisplayName("SubscriptionFilter")
    class SubscriptionFilterTests {

        @Test
        void shouldSerializeFilter() throws Exception {
            SubscriptionFilter filter = new SubscriptionFilter(List.of("id1", "id2", "id3"));

            String json = MAPPER.writeValueAsString(filter);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("ids").isArray()).isTrue();
            assertThat(node.get("ids").size()).isEqualTo(3);
            assertThat(node.get("ids").get(0).asText()).isEqualTo("id1");
        }

        @Test
        void shouldDeserializeFilter() throws Exception {
            String json = """
                {
                    "ids": ["filter1", "filter2"]
                }
                """;

            SubscriptionFilter filter = MAPPER.readValue(json, SubscriptionFilter.class);

            assertThat(filter.getIds()).containsExactly("filter1", "filter2");
        }

        @Test
        void shouldRoundTripFilter() throws Exception {
            SubscriptionFilter original = new SubscriptionFilter(List.of("a", "b", "c"));

            String json = MAPPER.writeValueAsString(original);
            SubscriptionFilter restored = MAPPER.readValue(json, SubscriptionFilter.class);

            assertThat(restored.getIds()).isEqualTo(original.getIds());
        }
    }
}
