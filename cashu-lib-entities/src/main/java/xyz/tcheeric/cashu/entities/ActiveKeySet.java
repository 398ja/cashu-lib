package xyz.tcheeric.cashu.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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

    public static ActiveKeySet fromKeySet(@NonNull KeySet keySet, boolean active) {
        ActiveKeySet activeKeySet = new ActiveKeySet();
        activeKeySet.setId(keySet.getId());
        activeKeySet.setUnit(keySet.getUnit());
        activeKeySet.setActive(active);
        return activeKeySet;
    }
}
