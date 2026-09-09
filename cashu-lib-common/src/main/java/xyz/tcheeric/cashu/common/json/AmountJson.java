package xyz.tcheeric.cashu.common.json;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Strict reader for the {@code amount} field of a proof, blinded message or blind signature.
 *
 * <h2>What was wrong with {@code asInt()}</h2>
 *
 * <p>{@code JsonNode.asInt()} is a coercion, not a parse. It answers {@code 0} for a string that
 * is not a number, for {@code null}, for a boolean, and for an object; and it silently truncates
 * a value that does not fit in an {@code int} (audit M-8). Every one of those is a malformed
 * amount being read as a valid one, and {@code 0} in particular is a value the rest of the
 * protocol treats as meaningful.
 *
 * <p>An amount is money. It has to be an integer that was actually written as one, it has to be
 * non-negative, and it has to fit. Anything else is a malformed message and should be refused at
 * the edge rather than turned into a plausible-looking number that flows onward.
 *
 * <p>The upper bound is the largest power of two an {@code int} can hold. Cashu amounts are
 * powers of two by construction, so this rejects nothing legitimate while leaving room for the
 * caller to sum many of them into a {@code long} without overflow.
 */
public final class AmountJson {

    /**
     * Largest permitted single amount: {@code 2^30}. Cashu denominations are powers of two, and
     * this is the largest one that leaves headroom for summing thousands of inputs in a
     * {@code long}.
     */
    public static final int MAX_AMOUNT = 1 << 30;

    private AmountJson() {
    }

    /**
     * Reads a required amount field.
     *
     * @param parent    the enclosing JSON object
     * @param fieldName the field to read, normally {@code "amount"}
     * @return the amount
     * @throws IllegalArgumentException if the field is absent, not an integer, negative, or
     *                                  larger than {@link #MAX_AMOUNT}
     */
    public static int require(JsonNode parent, String fieldName) {
        if (parent == null) {
            throw new IllegalArgumentException("Missing object for '" + fieldName + "'");
        }
        JsonNode node = parent.get(fieldName);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing required field '" + fieldName + "'");
        }
        if (!node.isIntegralNumber()) {
            // Covers strings, booleans, objects, arrays and fractional numbers. asInt() would
            // have answered 0 for most of these.
            throw new IllegalArgumentException(
                    "Field '" + fieldName + "' must be an integer, got: " + node.getNodeType());
        }
        if (!node.canConvertToInt()) {
            throw new IllegalArgumentException(
                    "Field '" + fieldName + "' does not fit in an int: " + node.asText());
        }
        int value = node.intValue();
        if (value < 0) {
            throw new IllegalArgumentException(
                    "Field '" + fieldName + "' must not be negative, got " + value);
        }
        if (value > MAX_AMOUNT) {
            throw new IllegalArgumentException(
                    "Field '" + fieldName + "' exceeds the maximum amount " + MAX_AMOUNT
                            + ", got " + value);
        }
        return value;
    }
}
