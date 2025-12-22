package xyz.tcheeric.cashu.entities.rest;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.KeySet;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetKeySetsResponse {

    @JsonProperty("keysets")
    @JsonAlias({"keySets"})
    private List<KeySet> keySets = new ArrayList<>();

    public void addActiveKeySet(@NonNull KeySet keySet) {
        keySets.add(keySet);
    }

}
