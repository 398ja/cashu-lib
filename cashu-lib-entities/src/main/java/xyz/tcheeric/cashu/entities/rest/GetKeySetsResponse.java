package xyz.tcheeric.cashu.entities.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.common.KeySet;

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
