package xyz.tcheeric.cashu.common.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.ThreadSafe;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;
import xyz.tcheeric.cashu.common.Token;
import xyz.tcheeric.cashu.common.TokenV3;
import xyz.tcheeric.cashu.common.TokenV4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes collision-resistant fingerprints for serialized Cashu tokens.
 * <p>
 * This utility parses {@code cashuA} (V3/JSON) and {@code cashuB} (V4/CBOR) tokens,
 * extracts the proof secrets, and computes a canonical fingerprint using
 * {@link ProofFingerprint}.
 * </p>
 * <p>
 * The fingerprint is deterministic: the same token will always produce the same
 * fingerprint regardless of whitespace, encoding variations, or proof ordering.
 * </p>
 *
 * @see ProofFingerprint for the underlying fingerprint computation
 */
@Slf4j
@ThreadSafe
public final class TokenFingerprint {

    private TokenFingerprint() {
        // Not instantiable
    }

    /**
     * Computes a fingerprint from a serialized token string.
     * <p>
     * Supports both token formats:
     * <ul>
     *   <li>{@code cashuA...} - V3 JSON-based tokens</li>
     *   <li>{@code cashuB...} - V4 CBOR-based tokens</li>
     * </ul>
     * Also accepts the clickable URI format ({@code cashu:cashuA...}).
     * </p>
     * <p>
     * If token parsing fails, falls back to computing the fingerprint
     * from the entire token string.
     * </p>
     *
     * @param serializedToken the serialized token string
     * @return hex-encoded SHA-256 fingerprint (64 characters)
     */
    public static String compute(@NonNull String serializedToken) {
        String trimmed = serializedToken.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute fingerprint from empty token");
        }

        try {
            // Determine token version and parse accordingly
            String tokenPayload = stripUriScheme(trimmed);

            if (tokenPayload.startsWith(Token.TOKEN_PREFIX + Token.Version.V3.getCode())) {
                return computeFromV3(tokenPayload);
            } else if (tokenPayload.startsWith(Token.TOKEN_PREFIX + Token.Version.V4.getCode())) {
                return computeFromV4(tokenPayload);
            } else {
                log.debug("token_fingerprint unknown_format using_fallback prefix={}",
                        tokenPayload.length() > 10 ? tokenPayload.substring(0, 10) : tokenPayload);
                return ProofFingerprint.computeFromString(trimmed);
            }

        } catch (Exception e) {
            log.warn("token_fingerprint parse_failed using_fallback error={}", e.getMessage());
            return ProofFingerprint.computeFromString(trimmed);
        }
    }

    /**
     * Computes fingerprint from a V3 token.
     *
     * @param proofs  the proofs from the token
     * @param mintUrl the mint URL
     * @return hex-encoded fingerprint
     */
    public static <T extends Secret> String computeFromProofs(
            @NonNull Collection<Proof<T>> proofs,
            @NonNull String mintUrl) {
        return ProofFingerprint.compute(proofs, mintUrl);
    }

    /**
     * Computes fingerprint from a V3 (JSON) token.
     */
    @SuppressWarnings("unchecked")
    private static String computeFromV3(String tokenPayload) {
        TokenV3<Secret> token = TokenV3.deserialize(tokenPayload);
        Set<TokenV3.MintProof<Secret>> mintProofs = token.getMintProofs();

        if (mintProofs == null || mintProofs.isEmpty()) {
            throw new IllegalArgumentException("Token contains no mint proofs");
        }

        // Collect all secrets and mint URLs
        List<String> allSecrets = new ArrayList<>();
        String mintUrl = "";

        for (TokenV3.MintProof<Secret> mintProof : mintProofs) {
            if (mintUrl.isEmpty() && mintProof.getMint() != null) {
                mintUrl = mintProof.getMint();
            }

            Set<Proof<Secret>> proofs = mintProof.getProofs();
            if (proofs != null) {
                for (Proof<Secret> proof : proofs) {
                    if (proof.getSecret() != null) {
                        allSecrets.add(proof.getSecret().toString());
                    }
                }
            }
        }

        if (allSecrets.isEmpty()) {
            throw new IllegalArgumentException("No secrets found in V3 token");
        }

        String fingerprint = ProofFingerprint.computeFromSecrets(allSecrets, mintUrl);
        log.debug("token_fingerprint v3 secrets_count={} mint={} fingerprint={}",
                allSecrets.size(),
                mintUrl.length() > 30 ? mintUrl.substring(0, 30) + "..." : mintUrl,
                fingerprint.substring(0, 16) + "...");

        return fingerprint;
    }

    /**
     * Computes fingerprint from a V4 (CBOR) token.
     */
    private static String computeFromV4(String tokenPayload) {
        TokenV4 token = TokenV4.deserialize(tokenPayload);
        List<TokenV4.TokenData> tokenDataList = token.getTokenDataList();

        if (tokenDataList == null || tokenDataList.isEmpty()) {
            throw new IllegalArgumentException("Token contains no token data");
        }

        String mintUrl = token.getMintUrl() != null ? token.getMintUrl() : "";

        // Collect all secrets from token proofs
        List<String> allSecrets = tokenDataList.stream()
                .filter(td -> td.getProofs() != null)
                .flatMap(td -> td.getProofs().stream())
                .map(TokenV4.TokenData.TokenProof::getSecret)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());

        if (allSecrets.isEmpty()) {
            throw new IllegalArgumentException("No secrets found in V4 token");
        }

        String fingerprint = ProofFingerprint.computeFromSecrets(allSecrets, mintUrl);
        log.debug("token_fingerprint v4 secrets_count={} mint={} fingerprint={}",
                allSecrets.size(),
                mintUrl.length() > 30 ? mintUrl.substring(0, 30) + "..." : mintUrl,
                fingerprint.substring(0, 16) + "...");

        return fingerprint;
    }

    /**
     * Strips the cashu: URI scheme if present.
     */
    private static String stripUriScheme(String token) {
        if (token.startsWith(Token.URI_SCHEME)) {
            return token.substring(Token.URI_SCHEME.length());
        }
        return token;
    }
}
