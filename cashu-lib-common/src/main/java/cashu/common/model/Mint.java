package cashu.common.model;

import cashu.common.protocol.Actor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@Builder
@Getter
public class Mint implements Actor, Archivable {

    private final PrivateKey privateKey;
    private final Set<KeySet> keySets;

    public Mint() {
        this(PrivateKey.generateRandom());
    }

    public Mint(@NonNull PrivateKey privateKey) {
        this.privateKey = privateKey;
        this.keySets = new HashSet<>();
    }

    public boolean addKeySet(@NonNull KeySet keySet) {
        return keySets.add(keySet);
    }

    public boolean removeKeySet(@NonNull KeySet keySet) {
        return keySets.remove(keySet);
    }


}
