package xyz.tcheeric.cashu.common;

import lombok.NonNull;

public interface Secret {

    byte[] getData();

    void setData(@NonNull byte[] data);

    byte[] toBytes();

    @Deprecated(forRemoval = true)
    static Secret fromString(@NonNull String secret, @NonNull Class<?> type) {
        return switch (type.getSimpleName()) {
            case "P2PKSecret" -> P2PKSecret.fromString(secret);
            case "RandomStringSecret" -> RandomStringSecret.fromString(secret);
            default -> throw new IllegalArgumentException("Unknown secret type: " + type.getSimpleName());
        };
    }
}
