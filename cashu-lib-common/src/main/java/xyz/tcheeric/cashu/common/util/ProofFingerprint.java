package xyz.tcheeric.cashu.common.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.crypto.util.Utils;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Computes collision-resistant fingerprints for Cashu proofs.
 * <p>
 * Fingerprints are derived from the SHA-256 hash of sorted proof secrets,
 * ensuring deterministic output regardless of proof ordering. This prevents
 * token duplication attacks where the same proofs could be submitted in
 * different orders.
 * </p>
 * <p>
 * The fingerprint format is: SHA-256(secret1|secret2|...|secretN||mintUrl)
 * where secrets are sorted lexicographically.
 * </p>
 *
 * @see TokenFingerprint for computing fingerprints from serialized tokens
 */
@Slf4j
@ThreadSafe
public final class ProofFingerprint {

    private static final String SECRET_SEPARATOR = "|";
    private static final String MINT_SEPARATOR = "||";

    private ProofFingerprint() {
        // Not instantiable
    }

    /**
     * Computes a fingerprint from a collection of proofs.
     * <p>
     * The fingerprint is computed by:
     * <ol>
     *   <li>Extracting the string representation of each proof's secret</li>
     *   <li>Sorting secrets lexicographically for deterministic ordering</li>
     *   <li>Joining with {@code |} separator</li>
     *   <li>Appending {@code ||} and the mint URL</li>
     *   <li>Computing SHA-256 hash of the result</li>
     * </ol>
     *
     * @param proofs  the proofs to fingerprint
     * @param mintUrl the mint URL for additional uniqueness
     * @return hex-encoded SHA-256 fingerprint (64 characters)
     * @throws IllegalArgumentException if proofs is empty
     */
    public static <T extends Secret> String compute(
            @NonNull Collection<Proof<T>> proofs,
            @NonNull String mintUrl) {

        if (proofs.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute fingerprint from empty proof collection");
        }

        List<String> secrets = proofs.stream()
                .map(Proof::getSecret)
                .filter(secret -> secret != null)
                .map(Secret::toString)
                .sorted()
                .collect(Collectors.toList());

        if (secrets.isEmpty()) {
            throw new IllegalArgumentException("No valid secrets found in proofs");
        }

        String input = String.join(SECRET_SEPARATOR, secrets) + MINT_SEPARATOR + mintUrl;
        return computeHash(input);
    }

    /**
     * Computes a fingerprint from raw secret strings.
     * <p>
     * Use this method when you have already extracted secret strings from proofs.
     *
     * @param secrets the secret strings to fingerprint
     * @param mintUrl the mint URL for additional uniqueness
     * @return hex-encoded SHA-256 fingerprint (64 characters)
     * @throws IllegalArgumentException if secrets is empty
     */
    public static String computeFromSecrets(
            @NonNull Collection<String> secrets,
            @NonNull String mintUrl) {

        if (secrets.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute fingerprint from empty secrets collection");
        }

        List<String> sortedSecrets = secrets.stream()
                .filter(s -> s != null && !s.isBlank())
                .sorted()
                .collect(Collectors.toList());

        if (sortedSecrets.isEmpty()) {
            throw new IllegalArgumentException("No valid secrets provided");
        }

        String input = String.join(SECRET_SEPARATOR, sortedSecrets) + MINT_SEPARATOR + mintUrl;
        return computeHash(input);
    }

    /**
     * Computes a fingerprint from a single string input.
     * <p>
     * This is the fallback method when proof extraction fails, computing
     * the hash of the entire input string.
     *
     * @param input the string to hash
     * @return hex-encoded SHA-256 fingerprint (64 characters)
     */
    public static String computeFromString(@NonNull String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute fingerprint from empty string");
        }
        return computeHash(trimmed);
    }

    /**
     * Computes SHA-256 hash of the input string.
     *
     * @param input the string to hash
     * @return hex-encoded hash (64 characters)
     */
    private static String computeHash(String input) {
        try {
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
            byte[] hash = Utils.sha256(inputBytes);
            String fingerprint = Utils.bytesToHexString(hash);
            log.debug("proof_fingerprint compute input_length={} fingerprint={}",
                    input.length(), fingerprint.substring(0, 16) + "...");
            return fingerprint;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in compliant JVMs
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
