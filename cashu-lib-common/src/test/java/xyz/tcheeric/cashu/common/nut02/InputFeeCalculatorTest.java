package xyz.tcheeric.cashu.common.nut02;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.KeySet;
import xyz.tcheeric.cashu.common.nut00.CashuErrorCode;
import xyz.tcheeric.cashu.common.LiteralSecret;
import xyz.tcheeric.cashu.common.Proof;
import xyz.tcheeric.cashu.common.Secret;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies NUT-02 input fee resolution: every proof is priced against its own keyset and the
 * total is rounded up to whole units.
 */
class InputFeeCalculatorTest {

    private static final String FIRST_KEYSET_ID = "009a1f293253e41e";
    private static final String SECOND_KEYSET_ID = "0042ade98b2a370a";

    /** A single-keyset input set is priced with that keyset's fee, rounded up. */
    @Test
    void shouldPriceInputsWithTheirOwnKeysetWhenAllShareOneKeyset() throws Exception {
        InputFeeCalculator calculator = calculatorFor(Map.of(FIRST_KEYSET_ID, keySet(FIRST_KEYSET_ID, 100)));

        int fee = calculator.calculateFee(proofs(FIRST_KEYSET_ID, 3));

        assertThat(fee).isEqualTo(1);
    }

    /** Inputs spanning two keysets with different fees sum each proof's own keyset fee. */
    @Test
    void shouldSumPerKeysetFeesWhenInputsSpanTwoKeysets() throws Exception {
        Map<String, KeySet> keySets = new LinkedHashMap<>();
        keySets.put(FIRST_KEYSET_ID, keySet(FIRST_KEYSET_ID, 100));
        keySets.put(SECOND_KEYSET_ID, keySet(SECOND_KEYSET_ID, 500));
        InputFeeCalculator calculator = calculatorFor(keySets);

        List<Proof<? extends Secret>> inputs = new ArrayList<>(proofs(FIRST_KEYSET_ID, 5));
        inputs.addAll(proofs(SECOND_KEYSET_ID, 3));

        // 5*100 + 3*500 = 2000 ppk, which is exactly 2 units.
        assertThat(calculator.calculateFee(inputs)).isEqualTo(2);
    }

    /** Ten inputs at 100 ppk land exactly on the rounding boundary and cost one unit. */
    @Test
    void shouldChargeOneUnitWhenFeesSumToExactlyOneThousandPartsPerThousand() throws Exception {
        InputFeeCalculator calculator = calculatorFor(Map.of(FIRST_KEYSET_ID, keySet(FIRST_KEYSET_ID, 100)));

        assertThat(calculator.calculateFee(proofs(FIRST_KEYSET_ID, 10))).isEqualTo(1);
    }

    /** One input past the boundary rounds up to the next whole unit. */
    @Test
    void shouldRoundUpWhenFeesExceedTheRoundingBoundary() throws Exception {
        InputFeeCalculator calculator = calculatorFor(Map.of(FIRST_KEYSET_ID, keySet(FIRST_KEYSET_ID, 100)));

        assertThat(calculator.calculateFee(proofs(FIRST_KEYSET_ID, 11))).isEqualTo(2);
    }

    /** A zero-fee keyset costs nothing, however many inputs are spent. */
    @Test
    void shouldChargeNothingWhenKeysetHasZeroFee() throws Exception {
        InputFeeCalculator calculator = calculatorFor(Map.of(FIRST_KEYSET_ID, keySet(FIRST_KEYSET_ID, 0)));

        assertThat(calculator.calculateFee(proofs(FIRST_KEYSET_ID, 42))).isZero();
    }

    /** An empty input set owes no fee. */
    @Test
    void shouldChargeNothingWhenThereAreNoInputs() throws Exception {
        InputFeeCalculator calculator = calculatorFor(Map.of());

        assertThat(calculator.calculateFee(List.of())).isZero();
    }

    /** An input from an unresolvable keyset raises the typed 12001 error rather than an assertion. */
    @Test
    void shouldRaiseUnknownKeysetErrorWhenKeysetIdIsNotResolvable() {
        InputFeeCalculator calculator = calculatorFor(Map.of(FIRST_KEYSET_ID, keySet(FIRST_KEYSET_ID, 100)));

        assertThatThrownBy(() -> calculator.calculateFee(proofs(SECOND_KEYSET_ID, 1)))
                .isInstanceOf(UnknownKeySetException.class)
                .hasMessageContaining(SECOND_KEYSET_ID)
                .extracting(thrown -> ((UnknownKeySetException) thrown).getErrorCode())
                .isEqualTo(CashuErrorCode.keyset_not_known);
    }

    private static InputFeeCalculator calculatorFor(Map<String, KeySet> keySetsById) {
        return new InputFeeCalculator(KeySetResolver.of(keySetsById));
    }

    private static KeySet keySet(String id, int inputFeePpk) {
        KeySet keySet = new KeySet();
        keySet.setId(id);
        keySet.setUnit("sat");
        keySet.setPartPerThousand(inputFeePpk);
        return keySet;
    }

    private static List<Proof<? extends Secret>> proofs(String keySetId, int count) {
        List<Proof<? extends Secret>> proofs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            Proof<LiteralSecret> proof = new Proof<>();
            proof.setAmount(1);
            proof.setKeySetId(keySetId);
            proofs.add(proof);
        }
        return proofs;
    }
}
