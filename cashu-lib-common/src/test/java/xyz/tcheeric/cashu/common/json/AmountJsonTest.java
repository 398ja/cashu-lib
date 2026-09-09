package xyz.tcheeric.cashu.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An amount is money, so reading one has to be a parse, not a coercion.
 *
 * <p>The deserializers used {@code JsonNode.asInt()} (audit M-8). That answers {@code 0} for a
 * non-numeric string, for {@code null}, for a boolean and for an object, and it truncates values
 * that do not fit. Every one of those turns a malformed message into a plausible amount, and
 * {@code 0} is a value the protocol treats as meaningful rather than as an error.
 */
@DisplayName("Amount fields are parsed strictly")
class AmountJsonTest {

    private static com.fasterxml.jackson.databind.JsonNode json(String s) throws Exception {
        return new ObjectMapper().readTree(s);
    }

    @Test
    @DisplayName("a well-formed amount is read")
    void wellFormedAmountIsRead() throws Exception {
        assertThat(AmountJson.require(json("{\"amount\": 64}"), "amount")).isEqualTo(64);
    }

    @Test
    @DisplayName("zero is a legitimate amount and is preserved")
    void zeroIsAllowed() throws Exception {
        assertThat(AmountJson.require(json("{\"amount\": 0}"), "amount")).isZero();
    }

    @Test
    @DisplayName("a non-numeric string is rejected, not read as zero")
    void nonNumericStringIsRejected() throws Exception {
        // asInt() answered 0 here, which is indistinguishable from a genuine zero amount.
        assertThatThrownBy(() -> AmountJson.require(json("{\"amount\": \"abc\"}"), "amount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be an integer");
    }

    @Test
    @DisplayName("a numeric string is rejected: JSON numbers are not strings")
    void numericStringIsRejected() throws Exception {
        assertThatThrownBy(() -> AmountJson.require(json("{\"amount\": \"64\"}"), "amount"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a fractional amount is rejected")
    void fractionalIsRejected() throws Exception {
        assertThatThrownBy(() -> AmountJson.require(json("{\"amount\": 1.5}"), "amount"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a negative amount is rejected")
    void negativeIsRejected() throws Exception {
        assertThatThrownBy(() -> AmountJson.require(json("{\"amount\": -1}"), "amount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    @DisplayName("an amount that does not fit in an int is rejected, not truncated")
    void oversizedIsRejected() throws Exception {
        assertThatThrownBy(() -> AmountJson.require(json("{\"amount\": 99999999999999}"), "amount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not fit");
    }

    @Test
    @DisplayName("an amount above the protocol maximum is rejected")
    void aboveMaximumIsRejected() throws Exception {
        String big = "{\"amount\": " + (AmountJson.MAX_AMOUNT + 1) + "}";

        assertThatThrownBy(() -> AmountJson.require(json(big), "amount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    @DisplayName("the protocol maximum itself is accepted")
    void maximumIsAccepted() throws Exception {
        String atMax = "{\"amount\": " + AmountJson.MAX_AMOUNT + "}";

        assertThat(AmountJson.require(json(atMax), "amount")).isEqualTo(AmountJson.MAX_AMOUNT);
    }

    @Test
    @DisplayName("a missing amount is rejected")
    void missingIsRejected() throws Exception {
        assertThatThrownBy(() -> AmountJson.require(json("{}"), "amount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing");
    }

    @Test
    @DisplayName("a null amount is rejected")
    void nullIsRejected() throws Exception {
        assertThatThrownBy(() -> AmountJson.require(json("{\"amount\": null}"), "amount"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a boolean amount is rejected")
    void booleanIsRejected() throws Exception {
        assertThatThrownBy(() -> AmountJson.require(json("{\"amount\": true}"), "amount"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
