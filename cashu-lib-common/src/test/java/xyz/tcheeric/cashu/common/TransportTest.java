package xyz.tcheeric.cashu.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.nut18.Transport;
import xyz.tcheeric.cashu.common.nut18.TransportType;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Transport")
class TransportTest {

    private static final ObjectMapper MAPPER = JsonUtils.JSON_MAPPER;

    @Nested
    @DisplayName("TransportType")
    class TransportTypeTests {

        @Test
        void shouldSerializeNostrType() throws Exception {
            TransportType type = TransportType.NOSTR;
            String json = MAPPER.writeValueAsString(type);
            assertThat(json).isEqualTo("\"nostr\"");
        }

        @Test
        void shouldSerializePostType() throws Exception {
            TransportType type = TransportType.POST;
            String json = MAPPER.writeValueAsString(type);
            assertThat(json).isEqualTo("\"post\"");
        }

        @Test
        void shouldDeserializeNostrType() throws Exception {
            TransportType type = MAPPER.readValue("\"nostr\"", TransportType.class);
            assertThat(type).isEqualTo(TransportType.NOSTR);
        }

        @Test
        void shouldDeserializePostType() throws Exception {
            TransportType type = MAPPER.readValue("\"post\"", TransportType.class);
            assertThat(type).isEqualTo(TransportType.POST);
        }

        @Test
        void shouldDeserializeCaseInsensitive() throws Exception {
            TransportType type = MAPPER.readValue("\"NOSTR\"", TransportType.class);
            assertThat(type).isEqualTo(TransportType.NOSTR);
        }

        @Test
        void shouldRejectUnknownType() {
            assertThrows(Exception.class, () ->
                MAPPER.readValue("\"unknown\"", TransportType.class));
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        void shouldCreateNostrNip17Transport() {
            String nprofile = "nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p";

            Transport transport = Transport.nostrNip17(nprofile);

            assertThat(transport.getType()).isEqualTo(TransportType.NOSTR);
            assertThat(transport.getTarget()).isEqualTo(nprofile);
            assertThat(transport.getTagValue("n")).isEqualTo("17");
            assertThat(transport.isNostr()).isTrue();
            assertThat(transport.isHttpPost()).isFalse();
        }

        @Test
        void shouldCreateHttpPostTransport() {
            String url = "https://api.example.com/pay/callback";

            Transport transport = Transport.httpPost(url);

            assertThat(transport.getType()).isEqualTo(TransportType.POST);
            assertThat(transport.getTarget()).isEqualTo(url);
            assertThat(transport.getTags()).isEmpty();
            assertThat(transport.isHttpPost()).isTrue();
            assertThat(transport.isNostr()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tag Handling")
    class TagHandlingTests {

        @Test
        void shouldAddAndRetrieveTags() {
            Transport transport = new Transport();
            transport.addTag("key1", "value1");
            transport.addTag("key2", "value2");

            assertThat(transport.getTagValue("key1")).isEqualTo("value1");
            assertThat(transport.getTagValue("key2")).isEqualTo("value2");
            assertThat(transport.getTagValue("nonexistent")).isNull();
        }

        @Test
        void shouldReturnFirstMatchingTag() {
            Transport transport = new Transport();
            transport.addTag("key", "first");
            transport.addTag("key", "second");

            assertThat(transport.getTagValue("key")).isEqualTo("first");
        }

        @Test
        void shouldHandleNullTags() {
            Transport transport = new Transport();
            transport.setTags(null);

            assertThat(transport.getTagValue("any")).isNull();
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerializationTests {

        @Test
        void shouldSerializeTransportWithCorrectKeys() throws Exception {
            Transport transport = Transport.builder()
                    .typeValue(TransportType.POST.getValue())
                    .target("https://example.com/callback")
                    .tags(List.of(List.of("custom", "value")))
                    .build();

            String json = MAPPER.writeValueAsString(transport);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.get("t").asText()).isEqualTo("post");
            assertThat(node.get("a").asText()).isEqualTo("https://example.com/callback");
            assertThat(node.get("g").isArray()).isTrue();
            assertThat(node.get("g").get(0).get(0).asText()).isEqualTo("custom");
            assertThat(node.get("g").get(0).get(1).asText()).isEqualTo("value");
        }

        @Test
        void shouldDeserializeTransport() throws Exception {
            String json = "{\"t\":\"nostr\",\"a\":\"nprofile123\",\"g\":[[\"n\",\"17\"]]}";

            Transport transport = MAPPER.readValue(json, Transport.class);

            assertThat(transport.getType()).isEqualTo(TransportType.NOSTR);
            assertThat(transport.getTarget()).isEqualTo("nprofile123");
            assertThat(transport.getTagValue("n")).isEqualTo("17");
        }

        @Test
        void shouldOmitNullTags() throws Exception {
            Transport transport = Transport.builder()
                    .typeValue(TransportType.POST.getValue())
                    .target("https://example.com")
                    .tags(null)
                    .build();

            String json = MAPPER.writeValueAsString(transport);
            JsonNode node = MAPPER.readTree(json);

            assertThat(node.has("g")).isFalse();
        }

        @Test
        void shouldRoundTripTransport() throws Exception {
            Transport original = Transport.nostrNip17("nprofile1abc");
            original.addTag("extra", "data");

            String json = MAPPER.writeValueAsString(original);
            Transport restored = MAPPER.readValue(json, Transport.class);

            assertThat(restored.getType()).isEqualTo(original.getType());
            assertThat(restored.getTarget()).isEqualTo(original.getTarget());
            assertThat(restored.getTagValue("n")).isEqualTo("17");
            assertThat(restored.getTagValue("extra")).isEqualTo("data");
        }
    }
}
