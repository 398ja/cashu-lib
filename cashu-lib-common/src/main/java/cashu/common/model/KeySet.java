package cashu.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"id", "unit", "keys"})
public class KeySet implements Archivable {

    @JsonProperty
    private String id;

    @JsonProperty
    private String unit;

    @JsonProperty
    private Keys keys;
}
