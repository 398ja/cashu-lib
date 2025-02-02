package xyz.tcheeric.cashu.common.model;

import lombok.NonNull;

public interface Secret {

    String getData();

    void setData(@NonNull String data);

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
