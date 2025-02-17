package xyz.tcheeric.cashu.common.rest;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.entities.ActiveKeySet;

import java.util.List;

@Data
@NoArgsConstructor
public class ActiveKeySetResponse {
    private List<ActiveKeySet> keysets;

    public ActiveKeySetResponse(@NonNull List<ActiveKeySet> keysets) {
        this.keysets = keysets;
    }
}
