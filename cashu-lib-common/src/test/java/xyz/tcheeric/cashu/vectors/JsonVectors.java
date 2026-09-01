package xyz.tcheeric.cashu.vectors;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the JSON blocks of the vector documents.
 *
 * <p>Several documents use a flat JSON object purely as a labelled bag of values (for example the
 * NUT-13 {@code secret_0} … {@code secret_4} block), which this flattens to a map.
 */
final class JsonVectors {

    private JsonVectors() {
        // Utility class - prevent instantiation
    }

    /**
     * The fields of a flat JSON object, as text values.
     *
     * @param json a JSON object whose values are scalars
     * @throws IllegalArgumentException if the JSON is not an object
     */
    @SneakyThrows
    static Map<String, String> flatFields(@NonNull String json) {
        JsonNode node = JsonUtils.JSON_MAPPER.readTree(json);
        if (!node.isObject()) {
            throw new IllegalArgumentException("Expected a JSON object, got: " + node.getNodeType());
        }
        Map<String, String> fields = new LinkedHashMap<>();
        node.fields().forEachRemaining(field -> fields.put(field.getKey(), field.getValue().asText()));
        return fields;
    }
}
