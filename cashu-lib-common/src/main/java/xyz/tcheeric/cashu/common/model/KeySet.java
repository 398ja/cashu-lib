package xyz.tcheeric.cashu.common.model;

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
@JsonPropertyOrder({"id", "unit", "keys", "input_fee_ppk"})
public class KeySet {

    @JsonProperty
    private String id;

    @JsonProperty
    private String unit;

    @JsonProperty
    private Keys keys;

    @JsonProperty("input_fee_ppk")
    private int partPerThousand;
}
