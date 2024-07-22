package cashu.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Getter
public class Mint {

    @JsonProperty
    private final String id;

    @JsonProperty
    private final Set<KeySet> keySets;

    public Mint() {
        this(UUID.randomUUID().toString());
    }

    public Mint(@NonNull String id) {
        this.id = id;
        this.keySets = new HashSet<>();
    }

    public boolean addKeySet(@NonNull KeySet keySet) {
        return keySets.add(keySet);
    }

    public boolean removeKeySet(@NonNull KeySet keySet) {
        return keySets.remove(keySet);
    }


}
