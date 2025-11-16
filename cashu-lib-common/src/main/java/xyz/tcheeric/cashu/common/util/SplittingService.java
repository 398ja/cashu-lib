package xyz.tcheeric.cashu.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for splitting amounts into denominations that are actually stored
 * in a mint keyset.
 */
public final class SplittingService {

    public SplittingService() {
    }

    /**
     * Splits {@code amount} into the smallest set of denominations that are
     * present in {@code availableDenominations}. Denominations subject to the
     * addition must be positive.
     *
     * @param amount             amount to split
     * @param availableDenominations denominations published by the mint (must not be empty)
     * @return sorted list of denominations that sum to {@code amount}
     * @throws IllegalArgumentException if {@code amount} is negative
     * @throws IllegalStateException    if the amount cannot be represented
     */
    public List<Integer> split(long amount, Collection<Integer> availableDenominations) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (amount == 0) {
            return List.of();
        }
        Set<Integer> positiveDenoms = availableDenominations.stream()
                .filter(value -> value != null && value > 0)
                .collect(Collectors.toSet());
        if (positiveDenoms.isEmpty()) {
            throw new IllegalStateException("No positive denominations available");
        }
        List<Integer> sorted = positiveDenoms.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        List<Integer> parts = new ArrayList<>();
        long remaining = amount;
        while (remaining > 0) {
            Integer next = null;
            for (int denom : sorted) {
                if (denom <= remaining) {
                    next = denom;
                    break;
                }
            }
            if (next == null) {
                throw new IllegalStateException("Cannot represent amount " + amount + " with denominations " + sorted);
            }
            parts.add(next);
            remaining -= next;
        }
        return parts;
    }
}
