package xyz.tcheeric.cashu.common.nut10;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NUT-11 specifies {@code locktime} and {@code n_sigs} as numbers.
 *
 * <p>{@link TagSerializer} tested for {@code Integer} specifically, so when locktime widened to
 * {@code Long} to stop 32-bit truncation it fell through to the {@code String.valueOf} default and
 * was emitted as {@code ["locktime","1893456000"]}. The widening fix walked straight past the gap
 * it was standing on.
 *
 * <p>This is not only a spec deviation. A secret re-serialised through Jackson rather than through
 * the canonical {@code toString()} produces different bytes, so it hashes to a different {@code Y}
 * and becomes a different proof. Nothing derives {@code Y} through Jackson today, which is the
 * only reason this was contained.
 */
@DisplayName("NUT-10 tag serialization")
class TagSerializerNumericTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static WellKnownSecret.Tag tag(String key, Object... values) {
        WellKnownSecret.Tag tag = new WellKnownSecret.Tag(key);
        tag.setValues(new java.util.ArrayList<>(java.util.List.of(values)));
        return tag;
    }

    @Test
    @DisplayName("a Long tag value is emitted as a JSON number")
    void longIsEmittedAsNumber() throws Exception {
        String json = mapper.writeValueAsString(
                tag("locktime", 1893456000L));

        assertEquals("[\"locktime\",1893456000]", json,
                "locktime is a Long since the truncation fix, and was being quoted");
    }

    @Test
    @DisplayName("a locktime beyond 2038 survives as a number")
    void largeLocktimeIsNotTruncatedOrQuoted() throws Exception {
        // Past Integer.MAX_VALUE: the value that motivated widening to Long in the first place.
        long beyond2038 = 4102444800L;

        String json = mapper.writeValueAsString(
                tag("locktime", beyond2038));

        assertEquals("[\"locktime\",4102444800]", json);
        assertTrue(!json.contains("\"4102444800\""), "must not be quoted");
    }

    @Test
    @DisplayName("an Integer tag value is still a number")
    void integerIsStillANumber() throws Exception {
        assertEquals("[\"n_sigs\",2]",
                mapper.writeValueAsString(tag("n_sigs", 2)));
    }

    @Test
    @DisplayName("a BigInteger tag value is a number")
    void bigIntegerIsANumber() throws Exception {
        assertEquals("[\"locktime\",1893456000]",
                mapper.writeValueAsString(tag("locktime", java.math.BigInteger.valueOf(1893456000L))));
    }

    @Test
    @DisplayName("string tag values stay quoted")
    void stringsStayStrings() throws Exception {
        // Pubkeys and refund keys are strings and must not be mistaken for numbers.
        String json = mapper.writeValueAsString(tag("pubkey",
                "02a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90"));

        assertTrue(json.contains("\"02a1b2c3"), "a pubkey must remain a JSON string");
    }

    @Test
    @DisplayName("a numeric string stays a string")
    void numericStringIsNotCoerced() throws Exception {
        // The inverse mistake: a value that happens to look numeric must not become a number,
        // or round-tripping changes the bytes just as surely.
        assertEquals("[\"custom\",\"123\"]",
                mapper.writeValueAsString(tag("custom", "123")));
    }
}
