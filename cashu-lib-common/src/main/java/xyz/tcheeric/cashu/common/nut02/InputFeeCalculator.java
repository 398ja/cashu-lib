package xyz.tcheeric.cashu.common.nut02;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.tcheeric.cashu.common.KeySet;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;

import java.util.Collection;

/**
 * Computes the NUT-02 input fee for a set of proofs.
 *
 * <p>The fee of each proof is the {@code input_fee_ppk} of the keyset that issued it, and the
 * total is the sum rounded up to whole units:
 *
 * <pre>
 * fee = (sum(input_fee_ppk of each input's keyset) + 999) / 1000
 * </pre>
 *
 * <p>Inputs may span several keysets with different fees, so every proof is priced against its
 * own keyset id.
 */
@RequiredArgsConstructor
public class InputFeeCalculator {

    /**
     * Fees are quoted per thousand inputs, so the sum is divided by this to yield whole units.
     */
    private static final int PARTS_PER_THOUSAND = 1000;

    private static final int ROUND_UP_OFFSET = PARTS_PER_THOUSAND - 1;

    @NonNull
    private final KeySetResolver keySetResolver;

    /**
     * Returns the total fee owed for spending the given proofs.
     *
     * @throws UnknownKeySetException when a proof names a keyset the resolver does not know
     */
    public int calculateFee(@NonNull Collection<? extends Proof<? extends Secret>> proofs)
            throws UnknownKeySetException {

        int summedPartsPerThousand = 0;
        for (Proof<? extends Secret> proof : proofs) {
            summedPartsPerThousand += feePartsPerThousandOf(proof);
        }
        return Math.floorDiv(summedPartsPerThousand + ROUND_UP_OFFSET, PARTS_PER_THOUSAND);
    }

    private int feePartsPerThousandOf(@NonNull Proof<? extends Secret> proof)
            throws UnknownKeySetException {

        String keySetId = proof.getKeySetId();
        if (keySetId == null) {
            throw new IllegalArgumentException("Proof must carry a keyset id. Got: null");
        }
        KeySet keySet = keySetResolver.findById(keySetId)
                .orElseThrow(() -> new UnknownKeySetException(keySetId));
        return keySet.getPartPerThousand();
    }
}
