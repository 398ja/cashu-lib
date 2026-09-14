package xyz.tcheeric.cashu.crypto;

import java.util.concurrent.atomic.LongAdder;

/**
 * Counts how often a proof verifies <em>only</em> under the legacy secret encoding.
 *
 * <h2>Why this exists</h2>
 *
 * <p>{@link SecretEncoding#LEGACY_HEX} is a compatibility path for proofs issued before the
 * NUT-00 secret encoding was corrected. It widens the set of secrets that verify — a secret is
 * tried under two encodings, so two distinct byte strings map to two distinct curve points that
 * both belong to one proof. That is the deliberate price of not invalidating proofs already in
 * circulation, and {@code SpentProofKey} checks the spent-proof table under both keys so it
 * cannot become a double-spend.
 *
 * <p>The class comment on {@code SecretEncoding} says it "should not be the price forever" and
 * points operators at the switch that turns it off. But nothing told an operator <em>when</em>
 * turning it off was safe: flipping it while pre-migration proofs are still unspent invalidates
 * real money, and there was no way to know whether any remained. The sunset was therefore
 * untakeable in practice — a documented plan with no trigger (AppSec finding L-3, issue #264).
 *
 * <p>This counter is that trigger. When it stays at zero across a period comfortably longer than
 * the longest-lived proof an operator expects, no circulating proof depends on the legacy
 * encoding and {@code cashu.secret.legacy-encoding.enabled=false} is safe.
 *
 * <h2>Why a counter rather than a metric</h2>
 *
 * <p>{@code cashu-lib} has no metrics dependency and should not gain one: it is a library, and
 * choosing a metrics backend is the application's decision. Callers that have one — the mint has
 * Micrometer — can publish {@link #legacyOnlyVerifications()} as a gauge. Callers that do not can
 * still read it.
 *
 * <p>{@link LongAdder} rather than {@code AtomicLong} because verification is concurrent and this
 * is write-heavy, read-rare: exactly the access pattern {@code LongAdder} is built for.
 */
public final class LegacyEncodingUsage {

    private static final LongAdder LEGACY_ONLY_VERIFICATIONS = new LongAdder();

    private LegacyEncodingUsage() {
    }

    /**
     * Records a verification that succeeded under {@link SecretEncoding#LEGACY_HEX} after the
     * spec encoding had already failed.
     *
     * <p>Only counted when the spec encoding did <em>not</em> match, so a proof that verifies
     * under both — which most do, since the encodings agree for NUT-10 well-known secrets — does
     * not inflate the number. The question being answered is "would this proof stop verifying if
     * the legacy path were removed", and only a legacy-only match answers yes.
     */
    public static void recordLegacyOnlyVerification() {
        LEGACY_ONLY_VERIFICATIONS.increment();
    }

    /**
     * How many proofs have verified only under the legacy encoding since this process started.
     *
     * <p>Zero over a long enough window is the signal that the legacy path can be switched off.
     * Non-zero means pre-migration proofs are still circulating and switching it off would
     * invalidate them.
     *
     * @return the count since JVM start
     */
    public static long legacyOnlyVerifications() {
        return LEGACY_ONLY_VERIFICATIONS.sum();
    }

    /** Resets the counter. Intended for tests; a running mint has no reason to call this. */
    public static void reset() {
        LEGACY_ONLY_VERIFICATIONS.reset();
    }
}
