package xyz.tcheeric.cashu.common.nut11;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import xyz.tcheeric.cashu.common.nut10.WellKnownSecret;
import xyz.tcheeric.cashu.common.util.JsonUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
public class P2PKSecret extends WellKnownSecret {

    public enum P2PKTag {
        sigflag,
        n_sigs,
        pubkeys,
        locktime,
        refund,
        n_sigs_refund
    }

    public enum SignatureFlag { // IMPORTANT: Do not change the order!!!
        SIG_INPUTS,
        SIG_ALL
    }

    public P2PKSecret() {
        super(Kind.P2PK);
    }

    public P2PKSecret(@NonNull byte[] data) {
        super(Kind.P2PK, P2PKPublicKeys.requireValid(data, "data").getBytes());
        this.setNSigs(1);
        this.setSigFlag(SignatureFlag.SIG_INPUTS);
    }

    public P2PKSecret(@NonNull byte[] data, int nSigs, @NonNull SignatureFlag sigFlag) {
        super(Kind.P2PK, P2PKPublicKeys.requireValid(data, "data").getBytes());
        this.setNSigs(nSigs);
        this.setSigFlag(sigFlag);
    }

    public void addTag(@NonNull P2PKTag tag, @NonNull List<Object> values) {
        super.addTag(tag.name(), values);
    }

    public void setSigFlag(@NonNull SignatureFlag sigFlag) {
        super.setTag(P2PKTag.sigflag.name(), List.of(sigFlag));
    }

    public void setNSigs(@NonNull Integer nSigs) {
        super.setTag(P2PKTag.n_sigs.name(), List.of(nSigs));
    }

    public void setNSigsRefund(@NonNull Integer nSigsRefund) {
        super.setTag(P2PKTag.n_sigs_refund.name(), List.of(nSigsRefund));
    }

    public void setPubKeys(@NonNull List<String> pubKeys) {
        requireValidKeys(pubKeys, P2PKTag.pubkeys.name());
        super.setTag(P2PKTag.pubkeys.name(), new ArrayList<>(pubKeys.stream().toList()));
    }

    public void addPubKey(@NonNull String pubKey) {
        List<String> pubKeys = getPubKeys();
        pubKeys.add(pubKey);
        this.setPubKeys(pubKeys);
    }

    public void setLockTime(@NonNull Integer lockTime) {
        super.setTag(P2PKTag.locktime.name(), List.of(lockTime));
    }

    public void setRefund(@NonNull List<String> refund) {
        requireValidKeys(refund, P2PKTag.refund.name());
        super.setTag(P2PKTag.refund.name(), new ArrayList<>(refund.stream().toList()));
    }

    /**
     * Validates every key a caller supplies, so a construction-side mistake surfaces where it is
     * made rather than at serialization. {@code addPubKey} / {@code addRefund} inherit this by
     * routing through their setters.
     */
    private static void requireValidKeys(List<String> keys, String tagName) {
        for (int i = 0; i < keys.size(); i++) {
            P2PKPublicKeys.requireValid(keys.get(i), tagName + "[" + i + "]");
        }
    }

    public void addRefund(@NonNull String refund) {
        List<String> refunds = getRefund();
        refunds.add(refund);
        this.setRefund(refunds);
    }

    public int getNSigs() {
        return intValue(P2PKTag.n_sigs, -1);
    }

    /**
     * NUT-11 refund-path signature threshold. Defaults to {@code 1} when the
     * {@code n_sigs_refund} tag is absent <em>or carries no value</em> — per
     * NUT-11 the refund path requires a single signature by default, and
     * existing escrows minted before this tag existed must keep their current
     * 1-of-N refund behavior unchanged. Guarding the empty-values case also
     * stops a malformed secret (key-only tag array) from throwing
     * {@link IndexOutOfBoundsException} here.
     */
    public int getNSigsRefund() {
        return intValue(P2PKTag.n_sigs_refund, 1);
    }

    /**
     * The signature flag, or {@code null} when unset.
     *
     * <p>Tolerates a raw string value: a hand-built or partially-coerced secret can hold one, and
     * rejecting an unrecognised flag is {@link #validate()}'s job — the getter must not crash
     * first.
     */
    public String getSigFlag() {
        Object raw = firstValue(P2PKTag.sigflag);
        if (raw == null) {
            return null;
        }
        return raw instanceof SignatureFlag ? ((SignatureFlag) raw).name() : String.valueOf(raw);
    }

    public List<String> getPubKeys() {
        return stringValues(P2PKTag.pubkeys);
    }

    public int getLockTime() {
        return intValue(P2PKTag.locktime, 0);
    }

    public List<String> getRefund() {
        return stringValues(P2PKTag.refund);
    }

    /**
     * Checks this secret against NUT-11's malformed-secret rules.
     *
     * <p>NUT-11 names four conditions that make a secret malformed — a repeated tag, a signature
     * threshold that is not a positive integer or exceeds its pathway's key count, an unrecognised
     * sigflag, and a key duplicated within one pathway — each closing "the Proof MUST be rejected
     * as unspendable". A fifth is enforced here and is <em>not</em> in the spec: that the public
     * keys are valid compressed secp256k1 points. See the design note; an upstream clarification
     * is pending.
     *
     * @throws MalformedP2PKSecretException if the Proof must be rejected as unspendable
     */
    public void validate() {
        requireEachTagAtMostOnce();

        P2PKPublicKeys.requireValid(getData(), "data");

        List<String> pubKeys = stringValues(P2PKTag.pubkeys);
        for (int i = 0; i < pubKeys.size(); i++) {
            P2PKPublicKeys.requireValid(pubKeys.get(i), "pubkeys[" + i + "]");
        }

        List<String> refunds = stringValues(P2PKTag.refund);
        for (int i = 0; i < refunds.size(); i++) {
            P2PKPublicKeys.requireValid(refunds.get(i), "refund[" + i + "]");
        }

        // The main pathway is `data` plus the `pubkeys` tag; the refund pathway is the `refund`
        // tag. A key may appear in both, but not twice within one.
        List<String> mainPathway = new ArrayList<>();
        mainPathway.add(Hex.toHexString(getData()));
        mainPathway.addAll(pubKeys);
        requireNoDuplicateKeys(mainPathway, "main");
        requireNoDuplicateKeys(refunds, "refund");

        requireThresholdInRange(P2PKTag.n_sigs, mainPathway.size());
        if (!refunds.isEmpty()) {
            // With no refund pathway there is nothing to threshold, and the default of 1 must not
            // be read as "1 exceeds 0 keys" and condemn every ordinary secret.
            requireThresholdInRange(P2PKTag.n_sigs_refund, refunds.size());
        }

        requireKnownSigFlag();
    }

    private void requireEachTagAtMostOnce() {
        List<Tag> tags = getTags();
        if (tags == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (Tag tag : tags) {
            if (tag.getKey() != null && !seen.add(tag.getKey())) {
                throw new MalformedP2PKSecretException(
                        "tag '" + tag.getKey() + "' appears more than once");
            }
        }
    }

    /**
     * NUT-11 compares keys on the lowercase x-coordinate with the parity prefix ignored, so
     * {@code 02||x} and {@code 03||x} are the same key and duplicate each other.
     */
    private void requireNoDuplicateKeys(List<String> pathway, String pathwayName) {
        Set<String> seen = new HashSet<>();
        for (String key : pathway) {
            if (!seen.add(P2PKPublicKeys.toComparisonForm(key))) {
                throw new MalformedP2PKSecretException(
                        pathwayName + " pathway contains a duplicate public key");
            }
        }
    }

    private void requireThresholdInRange(P2PKTag tag, int keyCount) {
        Object raw = firstValue(tag);
        if (raw == null) {
            return; // absent, or present with no value: the NUT-11 default of 1 applies
        }
        long threshold = asThreshold(tag, raw);
        if (threshold < 1) {
            throw new MalformedP2PKSecretException(
                    tag.name() + " must be a positive integer, got " + threshold);
        }
        if (threshold > keyCount) {
            throw new MalformedP2PKSecretException(tag.name() + " of " + threshold
                    + " exceeds the " + keyCount + " key(s) in its pathway");
        }
    }

    /**
     * NUT-11 writes every tag value as a JSON string, so {@code "n_sigs": "2"} is the ordinary wire
     * form and only a value that is not an integer at all is malformed.
     */
    private static long asThreshold(P2PKTag tag, Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw new MalformedP2PKSecretException(
                    tag.name() + " is not an integer, got " + raw, e);
        }
    }

    private void requireKnownSigFlag() {
        Object raw = firstValue(P2PKTag.sigflag);
        if (raw == null) {
            return; // absent: SIG_INPUTS applies
        }
        if (raw instanceof SignatureFlag) {
            return;
        }
        try {
            SignatureFlag.valueOf(String.valueOf(raw));
        } catch (IllegalArgumentException e) {
            throw new MalformedP2PKSecretException("unrecognised sigflag value", e);
        }
    }

    /**
     * First tag value as an int, falling back to {@code defaultValue}.
     *
     * <p>Accepts any {@link Number}, not just {@code Integer}: {@code deserializeNut10Format}
     * stores integral JSON as {@code longValue()}, so a wire secret carrying {@code n_sigs} or
     * {@code locktime} would otherwise fail the cast.
     */
    private int intValue(P2PKTag tag, int defaultValue) {
        Object raw = firstValue(tag);
        if (raw == null) {
            return defaultValue;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            // Non-numeric is malformed; validate() rejects it. A getter must not throw.
            return defaultValue;
        }
    }

    private Object firstValue(P2PKTag tag) {
        Tag t = super.getTag(tag.name());
        if (t == null) {
            return null;
        }
        List<Object> values = t.getValues();
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    /**
     * Tag values as strings, without the unchecked cast that a raw {@code (List<String>)} would
     * need — a numeric tag value degrades to a validation failure rather than a
     * {@link ClassCastException} at an arbitrary call site.
     */
    private List<String> stringValues(P2PKTag tag) {
        Tag t = super.getTag(tag.name());
        if (t == null || t.getValues() == null) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>(t.getValues().size());
        for (Object value : t.getValues()) {
            result.add(value == null ? null : String.valueOf(value));
        }
        return result;
    }

    @SneakyThrows
    @Deprecated(forRemoval = true)
    public static P2PKSecret fromString(@NonNull String secret) {
        return JsonUtils.JSON_MAPPER.readValue(secret, P2PKSecret.class);
    }
}
