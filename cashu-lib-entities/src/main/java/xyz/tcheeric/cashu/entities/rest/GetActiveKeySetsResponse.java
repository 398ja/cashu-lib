package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.ActiveKeySet;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetActiveKeySetsResponse {

    // Accept both "keysets" (NUT-02) and legacy "activeKeySets"
    @JsonProperty("keysets")
    @JsonAlias({"activeKeySets"})
    private List<ActiveKeySet> activeKeySets = new ArrayList<>();

    public void addActiveKeySet(@NonNull ActiveKeySet activeKeySet) {
        activeKeySets.add(activeKeySet);
    }
}
