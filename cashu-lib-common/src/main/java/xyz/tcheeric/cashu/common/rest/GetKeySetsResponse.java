package xyz.tcheeric.cashu.common.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.entities.KeySet;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetKeySetsResponse {

    private List<KeySet> keySets;

    public void addActiveKeySet(@NonNull KeySet keySet) {
        keySets.add(keySet);
    }

}
