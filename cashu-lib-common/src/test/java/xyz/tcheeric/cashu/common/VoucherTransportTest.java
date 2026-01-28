package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut18.VoucherTransport;
import xyz.tcheeric.cashu.common.nut18.VoucherTransportType;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("VoucherTransport")
class VoucherTransportTest {

    private static final ObjectMapper MAPPER = JsonUtils.JSON_MAPPER;

    @Nested
    @DisplayName("VoucherTransportType")
    class VoucherTransportTypeTests {

        @Test
        void shouldSerializeNostrType() throws Exception {
            VoucherTransportType type = VoucherTransportType.NOSTR;
            String json = MAPPER.writeValueAsString(type);
            assertThat(json).isEqualTo("\"nostr\"");
        }

        @Test
        void shouldSerializePostType() throws Exception {
            VoucherTransportType type = VoucherTransportType.POST;
            String json = MAPPER.writeValueAsString(type);
            assertThat(json).isEqualTo("\"post\"");
        }

        @Test
        void shouldSerializeMerchantType() throws Exception {
            VoucherTransportType type = VoucherTransportType.MERCHANT;
            String json = MAPPER.writeValueAsString(type);
            assertThat(json).isEqualTo("\"merchant\"");
        }

        @Test
        void shouldDeserializeNostrType() throws Exception {
            VoucherTransportType type = MAPPER.readValue("\"nostr\"", VoucherTransportType.class);
            assertThat(type).isEqualTo(VoucherTransportType.NOSTR);
        }

        @Test
        void shouldDeserializePostType() throws Exception {
            VoucherTransportType type = MAPPER.readValue("\"post\"", VoucherTransportType.class);
            assertThat(type).isEqualTo(VoucherTransportType.POST);
        }

        @Test
        void shouldDeserializeMerchantType() throws Exception {
            VoucherTransportType type = MAPPER.readValue("\"merchant\"", VoucherTransportType.class);
            assertThat(type).isEqualTo(VoucherTransportType.MERCHANT);
        }

        @Test
        void shouldDeserializeCaseInsensitive() throws Exception {
            VoucherTransportType type = MAPPER.readValue("\"MERCHANT\"", VoucherTransportType.class);
            assertThat(type).isEqualTo(VoucherTransportType.MERCHANT);
        }

        @Test
        void shouldRejectUnknownType() {
            assertThrows(Exception.class, () ->
                MAPPER.readValue("\"unknown\"", VoucherTransportType.class));
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        void shouldCreateNostrNip17Transport() {
            String nprofile = "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p";

            VoucherTransport transport = VoucherTransport.nostrNip17(nprofile);

            assertThat(transport.getType()).isEqualTo(VoucherTransportType.NOSTR);
            assertThat(transport.getTarget()).isEqualTo(nprofile);
            assertThat(transport.getTagValue("n")).isEqualTo("17");
            assertThat(transport.isNostr()).isTrue();
            assertThat(transport.isHttpPost()).isFalse();
            assertThat(transport.isMerchant()).isFalse();
        }

        @Test
        void shouldCreateHttpPostTransport() {
            String url = "https://api.example.com/pay/callback";

            VoucherTransport transport = VoucherTransport.httpPost(url);

            assertThat(transport.getType()).isEqualTo(VoucherTransportType.POST);
            assertThat(transport.getTarget()).isEqualTo(url);
            assertThat(transport.getTags()).isEmpty();
            assertThat(transport.isHttpPost()).isTrue();
            assertThat(transport.isNostr()).isFalse();
            assertThat(transport.isMerchant()).isFalse();
        }

        @Test
        void shouldCreateMerchantTransport() {
            String merchantEndpoint = "https://merchant.example.com/redeem";

            VoucherTransport transport = VoucherTransport.merchant(merchantEndpoint);

            assertThat(transport.getType()).isEqualTo(VoucherTransportType.MERCHANT);
            assertThat(transport.getTarget()).isEqualTo(merchantEndpoint);
            assertThat(transport.getTags()).isEmpty();
            assertThat(transport.isMerchant()).isTrue();
            assertThat(transport.isNostr()).isFalse();
            assertThat(transport.isHttpPost()).isFalse();
        }

        @Test
        void shouldCreateMerchantTransportWithId() {
            String merchantEndpoint = "https://merchant.example.com/redeem";
            String merchantId = "merchant-123";

            VoucherTransport transport = VoucherTransport.merchant(merchantEndpoint, merchantId);

            assertThat(transport.getType()).isEqualTo(VoucherTransportType.MERCHANT);
            assertThat(transport.getTarget()).isEqualTo(merchantEndpoint);
            assertThat(transport.getTagValue("merchant_id")).isEqualTo(merchantId);
            assertThat(transport.isMerchant()).isTrue();
        }

        @Test
        void shouldIgnoreBlankMerchantId() {
            String merchantEndpoint = "https://merchant.example.com/redeem";

            VoucherTransport transport = VoucherTransport.merchant(merchantEndpoint, "   ");

            assertThat(transport.getTagValue("merchant_id")).isNull();
        }

        @Test
        void shouldIgnoreNullMerchantId() {
            String merchantEndpoint = "https://merchant.example.com/redeem";

            VoucherTransport transport = VoucherTransport.merchant(merchantEndpoint, null);

            assertThat(transport.getTagValue("merchant_id")).isNull();
        }
    }

    @Nested
    @DisplayName("Tag Handling")
    class TagHandlingTests {

        @Test
        void shouldAddAndRetrieveTags() {
            VoucherTransport transport = new VoucherTransport();
            transport.addTag("key1", "value1");
            transport.addTag("key2", "value2");

            assertThat(transport.getTagValue("key1")).isEqualTo("value1");
            assertThat(transport.getTagValue("key2")).isEqualTo("value2");
            assertThat(transport.getTagValue("nonexistent")).isNull();
        }

        @Test
        void shouldReturnFirstMatchingTag() {
            VoucherTransport transport = new VoucherTransport();
            transport.addTag("key", "first");
            transport.addTag("key", "second");

            assertThat(transport.getTagValue("key")).isEqualTo("first");
        }

        @Test
        void shouldHandleNullTags() {
            VoucherTransport transport = new VoucherTransport();
            transport.setTags(null);

            assertThat(transport.getTagValue("any")).isNull();
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerializationTests {

        @Test
        void shouldSerializeTransportWithCorrectKeys() throws Exception {
            VoucherTransport transport = VoucherTransport.builder()
                    .typeValue(VoucherTransportType.MERCHANT.getValue())
                    .target("https://merchant.example.com/redeem")
                    .tags(List.of(List.of("merchant_id", "m123")))
                    .build();

            String json = MAPPER.writeValueAsString(transport);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("t").asText()).isEqualTo("merchant");
            assertThat(node.get("a").asText()).isEqualTo("https://merchant.example.com/redeem");
            assertThat(node.get("g").isArray()).isTrue();
            assertThat(node.get("g").get(0).get(0).asText()).isEqualTo("merchant_id");
            assertThat(node.get("g").get(0).get(1).asText()).isEqualTo("m123");
        }

        @Test
        void shouldDeserializeTransport() throws Exception {
            String json = "{\"t\":\"merchant\",\"a\":\"https://merchant.com/redeem\",\"g\":[[\"merchant_id\",\"m456\"]]}";

            VoucherTransport transport = MAPPER.readValue(json, VoucherTransport.class);

            assertThat(transport.getType()).isEqualTo(VoucherTransportType.MERCHANT);
            assertThat(transport.getTarget()).isEqualTo("https://merchant.com/redeem");
            assertThat(transport.getTagValue("merchant_id")).isEqualTo("m456");
        }

        @Test
        void shouldOmitNullTags() throws Exception {
            VoucherTransport transport = VoucherTransport.builder()
                    .typeValue(VoucherTransportType.POST.getValue())
                    .target("https://example.com")
                    .tags(null)
                    .build();

            String json = MAPPER.writeValueAsString(transport);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.has("g")).isFalse();
        }

        @Test
        void shouldRoundTripTransport() throws Exception {
            VoucherTransport original = VoucherTransport.merchant("https://merchant.com", "m789");
            original.addTag("extra", "data");

            String json = MAPPER.writeValueAsString(original);
            VoucherTransport restored = MAPPER.readValue(json, VoucherTransport.class);

            assertThat(restored.getType()).isEqualTo(original.getType());
            assertThat(restored.getTarget()).isEqualTo(original.getTarget());
            assertThat(restored.getTagValue("merchant_id")).isEqualTo("m789");
            assertThat(restored.getTagValue("extra")).isEqualTo("data");
        }
    }

    @Nested
    @DisplayName("CBOR Serialization")
    class CborSerializationTests {

        @Test
        void shouldRoundTripViaCbor() throws Exception {
            VoucherTransport original = VoucherTransport.merchant("https://merchant.com/redeem", "m123");
            original.addTag("custom", "value");

            byte[] cbor = JsonUtils.CBOR_MAPPER.writeValueAsBytes(original);
            VoucherTransport restored = JsonUtils.CBOR_MAPPER.readValue(cbor, VoucherTransport.class);

            assertThat(restored.getType()).isEqualTo(VoucherTransportType.MERCHANT);
            assertThat(restored.getTarget()).isEqualTo("https://merchant.com/redeem");
            assertThat(restored.getTagValue("merchant_id")).isEqualTo("m123");
            assertThat(restored.getTagValue("custom")).isEqualTo("value");
        }
    }
}
