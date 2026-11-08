package xyz.tcheeric.cashu.common.voucher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;

import java.util.Arrays;

/**
 * Immutable voucher document bytes with content-based equality, so containers holding documents
 * keep value semantics in {@code equals} and {@code hashCode}.
 */
public final class VoucherDocument {

    private final byte[] bytes;

    private VoucherDocument(byte[] bytes) {
        this.bytes = bytes;
    }

    /** Wraps a defensive copy of the given document bytes. */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VoucherDocument of(@NonNull byte[] bytes) {
        return new VoucherDocument(bytes.clone());
    }

    /** Returns a defensive copy of the document bytes. */
    @JsonValue
    public byte[] toBytes() {
        return bytes.clone();
    }

    /** Returns this document's container key, {@code hex(SHA256(bytes))}. */
    public String key() {
        return VoucherDocuments.key(bytes);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VoucherDocument that && Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "VoucherDocument[" + bytes.length + " bytes]";
    }
}
