package xyz.tcheeric.cashu.common.voucher;

import lombok.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Shared rules for voucher documents carried by reference (cashu-voucher#64).
 *
 * <p>A voucher document {@code D} is stored once per token, request or payload, keyed by
 * {@code hex(SHA256(D))}. Each proof that refers to it carries only the 32-byte hash. This class
 * only checks the container rules (key format, key binding, size, count). It does not verify the
 * voucher itself; that belongs to cashu-voucher.
 */
public final class VoucherDocuments {

    /** Largest document a container may carry. */
    public static final int MAX_DOCUMENT_BYTES = 65_536;

    /**
     * Largest total of document bytes one container may carry. With up to 1000 proofs or inputs
     * per container, the per-document bound alone would allow 64 MiB, so the total is bounded too.
     */
    public static final int MAX_TOTAL_DOCUMENT_BYTES = 1_048_576;

    /** Length of a document reference (a SHA-256 digest) in bytes. */
    public static final int REFERENCE_BYTES = 32;

    private static final Pattern KEY_PATTERN = Pattern.compile("[0-9a-f]{64}");
    private static final HexFormat HEX = HexFormat.of();

    private VoucherDocuments() {
    }

    /** Returns the container key of a document: lowercase hex of its SHA-256 digest. */
    public static String key(@NonNull byte[] document) {
        return HEX.formatHex(sha256(document));
    }

    /** Whether some voucher data is a by-reference hash rather than an inline document. */
    public static boolean isReference(@NonNull byte[] data) {
        return data.length == REFERENCE_BYTES;
    }

    /** Whether a string is a well-formed container key (64 lowercase hex characters). */
    public static boolean isWellFormedKey(String key) {
        return key != null && KEY_PATTERN.matcher(key).matches();
    }

    /**
     * Checks a document map against the container rules.
     *
     * @param documents  key to document bytes
     * @param maxEntries the number of proofs or inputs in the container
     * @throws IllegalArgumentException when any rule is broken; the message names the offending entry
     */
    public static void validate(@NonNull Map<String, byte[]> documents, int maxEntries) {
        if (documents.size() > maxEntries) {
            throw new IllegalArgumentException("Voucher document count exceeds proof count. Got: "
                    + documents.size() + " documents for " + maxEntries + " proofs");
        }
        // Sizes before hashes, so an oversized container costs no hashing.
        long total = 0;
        for (byte[] document : documents.values()) {
            total += document == null ? 0 : document.length;
        }
        if (total > MAX_TOTAL_DOCUMENT_BYTES) {
            throw new IllegalArgumentException("Voucher documents exceed " + MAX_TOTAL_DOCUMENT_BYTES
                    + " bytes in total. Got: " + total + " bytes");
        }
        documents.forEach(VoucherDocuments::validateEntry);
    }

    /**
     * Decodes a hex document map (the JSON wire form) and checks it.
     *
     * @return a sorted, unmodifiable map of key to document bytes
     * @throws IllegalArgumentException when a value is not hex or any container rule is broken
     */
    public static SortedMap<String, byte[]> fromHex(@NonNull Map<String, String> hexDocuments, int maxEntries) {
        if (hexDocuments.size() > maxEntries) {
            throw new IllegalArgumentException("Voucher document count exceeds proof count. Got: "
                    + hexDocuments.size() + " documents for " + maxEntries + " proofs");
        }
        // Bound the hex text before decoding any of it: two hex characters per byte.
        long totalHexChars = 0;
        for (String value : hexDocuments.values()) {
            totalHexChars += value == null ? 0 : value.length();
        }
        if (totalHexChars > 2L * MAX_TOTAL_DOCUMENT_BYTES) {
            throw new IllegalArgumentException("Voucher documents exceed " + MAX_TOTAL_DOCUMENT_BYTES
                    + " bytes in total. Got: " + (totalHexChars / 2) + " bytes");
        }
        SortedMap<String, byte[]> decoded = new TreeMap<>();
        hexDocuments.forEach((key, value) -> decoded.put(key, decodeHexValue(key, value)));
        validate(decoded, maxEntries);
        return Collections.unmodifiableSortedMap(decoded);
    }

    /** Encodes a document map into its JSON wire form, sorted by key. */
    public static Map<String, String> toHex(@NonNull Map<String, byte[]> documents) {
        Map<String, String> encoded = new LinkedHashMap<>();
        new TreeMap<>(documents).forEach((key, value) -> encoded.put(key, HEX.formatHex(value)));
        return encoded;
    }

    private static void validateEntry(String key, byte[] document) {
        if (!isWellFormedKey(key)) {
            throw new IllegalArgumentException("Voucher document key must be 64 lowercase hex characters. Got: " + key);
        }
        if (document == null || document.length == 0) {
            throw new IllegalArgumentException("Voucher document must not be empty. Key: " + key);
        }
        if (document.length > MAX_DOCUMENT_BYTES) {
            throw new IllegalArgumentException("Voucher document exceeds " + MAX_DOCUMENT_BYTES
                    + " bytes. Got: " + document.length + " bytes for key " + key);
        }
        if (!key.equals(key(document))) {
            throw new IllegalArgumentException("Voucher document key does not match SHA-256 of the document. Key: " + key);
        }
    }

    private static byte[] decodeHexValue(String key, String value) {
        if (value == null) {
            throw new IllegalArgumentException("Voucher document must not be empty. Key: " + key);
        }
        try {
            return HEX.parseHex(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Voucher document is not valid hex. Key: " + key, e);
        }
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable on this JVM", e);
        }
    }
}
