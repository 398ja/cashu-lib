package xyz.tcheeric.cashu.common.nut02;

import lombok.NonNull;
import xyz.tcheeric.cashu.common.KeySet;

import java.util.Map;
import java.util.Optional;

/**
 * Looks up a keyset by its NUT-02 keyset id.
 *
 * <p>A transaction may spend inputs issued under several keysets, because NUT-02 keeps proofs of
 * inactive keysets spendable. Fee resolution therefore needs to price each proof against its own
 * keyset rather than against one keyset chosen by the caller.
 */
@FunctionalInterface
public interface KeySetResolver {

    /**
     * Returns the keyset with the given id, or an empty optional when the id is not known.
     *
     * @param keySetId the NUT-02 keyset id carried by a proof
     */
    Optional<KeySet> findById(@NonNull String keySetId);

    /**
     * Creates a resolver backed by an in-memory map of keyset id to keyset.
     */
    static KeySetResolver of(@NonNull Map<String, KeySet> keySetsById) {
        return keySetId -> Optional.ofNullable(keySetsById.get(keySetId));
    }
}
