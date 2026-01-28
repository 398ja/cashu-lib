package xyz.tcheeric.cashu.common;

import lombok.NonNull;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;
import xyz.tcheeric.cashu.common.nut13.DeterministicSecret;

public interface Secret {

    byte[] getData();

    void setData(@NonNull byte[] data);

    byte[] toBytes();

    @Deprecated(forRemoval = true)
    static Secret fromString(@NonNull String secret, @NonNull Class<?> type) {
        return switch (type.getSimpleName()) {
            case "P2PKSecret" -> P2PKSecret.fromString(secret);
            case "RandomStringSecret" -> RandomStringSecret.fromString(secret);
            case "DeterministicSecret" -> DeterministicSecret.fromString(secret);
            default -> throw new IllegalArgumentException("Unknown secret type: " + type.getSimpleName());
        };
    }
}
