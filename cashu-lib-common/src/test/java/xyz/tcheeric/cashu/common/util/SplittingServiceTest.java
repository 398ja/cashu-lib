package xyz.tcheeric.cashu.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SplittingServiceTest {

    private final SplittingService splittingService = new SplittingService();

    /**
     * Zero amount should require no denominations.
     */
    @Test
    void shouldReturnEmptyListForZeroAmount() {
        assertThat(splittingService.split(0, List.of(1, 2, 4))).isEmpty();
    }

    /**
     * Splitting should select the largest denominations first.
     */
    @Test
    void shouldUseLargestDenominations() {
        List<Integer> result = splittingService.split(5000, List.of(8, 16, 128, 256, 512, 4096));
        assertThat(result).containsExactly(4096, 512, 256, 128, 8);
    }

    /**
     * Splitting should throw an exception when it cannot represent the amount.
     */
    @Test
    void shouldFailWhenDenominationsMissing() {
        assertThatIllegalStateException()
                .isThrownBy(() -> splittingService.split(3, List.of(4, 8)));
    }

    /**
     * The service should refuse negative amounts.
     */
    @Test
    void shouldRejectNegativeAmounts() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> splittingService.split(-1, List.of(1, 2)));
    }

    /**
     * Non-positive denominations should be ignored.
     */
    @Test
    void shouldIgnoreNonPositiveDenominations() {
        List<Integer> result = splittingService.split(3, List.of(0, -1, 1, 2));
        assertThat(result).containsExactly(2, 1);
    }

    /**
     * Splitting should fall back to multiple smaller denominations when the expected high value is absent.
     */
    @Test
    void shouldUseMultipleSmallerDenominationsWhenHigherMissing() {
         List<Integer> available = List.of(512, 256, 128, 64, 32, 16, 8, 4, 2, 1);
         List<Integer> result = splittingService.split(2000, available);
         assertThat(result).containsExactly(512, 512, 512, 256, 128, 64, 16);
     }
}
