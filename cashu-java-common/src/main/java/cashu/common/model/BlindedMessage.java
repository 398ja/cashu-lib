package cashu.common.model;

import cashu.common.json.deserializer.BlindedMessageDeserializer;
import cashu.common.json.serializer.BlindedMessageSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"amount", "id", "B_"})
public class BlindedMessage {

    @JsonProperty
    private int amount;

    @JsonProperty("id")
    private String keySetId;

    @JsonProperty("B_")
    private PrivateKey blindedMessage;
}
