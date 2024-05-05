package cashu.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"id", "unit", "active"})
public class ActiveKeySet {

    @JsonProperty
    private String id;

    @JsonProperty
    private String unit;

    @JsonProperty
    private boolean active;
}
