package xyz.tcheeric.cashu.common.rest;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import xyz.tcheeric.cashu.entities.KeySet;

import java.util.List;

@Data
@NoArgsConstructor
public class KeySetResponse {

    private List<KeySet> keysets;

    public KeySetResponse(@NonNull List<KeySet> keysets) {
        this.keysets = keysets;
    }
}