package xyz.tcheeric.cashu.common.nut10;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import xyz.tcheeric.cashu.common.nut11.P2PKSecret;

import java.io.IOException;

/**
 * Serialises a NUT-10 tag as a JSON array.
 *
 * <p>Numeric tag values are emitted as JSON numbers. This used to test for {@code Integer}
 * specifically, so when locktime widened to {@code Long} to stop 32-bit truncation it fell through
 * to {@code writeString(String.valueOf(v))} and was emitted as {@code "locktime":"1893456000"}.
 * NUT-11 specifies a number, and {@code n_sigs} had the same gap before that.
 *
 * <p>It mattered more than a formatting nit: a secret re-serialised through Jackson rather than
 * through {@code toString()} produces different bytes, so it hashes to a different {@code Y} and
 * is a different proof. Nothing derived {@code Y} through this path, since
 * {@code SecretUtil.getY} uses the canonical {@code toString()} encoder, so it was one refactor
 * away from splitting proof identity rather than actually doing it.
 */
public class TagSerializer extends JsonSerializer<WellKnownSecret.Tag> {
    @Override
    public void serialize(WellKnownSecret.Tag value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        gen.writeString(value.getKey());
        if (value.getValues() != null) {
            for (Object v : value.getValues()) {
                if (v instanceof String s) {
                    gen.writeString(s);
                } else if (v instanceof Integer i) {
                    gen.writeNumber(i);
                } else if (v instanceof Long l) {
                    // locktime is a Long since the 32-bit truncation fix; without this it was
                    // written as a quoted string.
                    gen.writeNumber(l);
                } else if (v instanceof Short || v instanceof Byte) {
                    gen.writeNumber(((Number) v).longValue());
                } else if (v instanceof java.math.BigInteger b) {
                    gen.writeNumber(b);
                } else if (v instanceof P2PKSecret.SignatureFlag f) {
                    gen.writeString(f.name());
                } else {
                    gen.writeString(String.valueOf(v));
                }
            }
        }
        gen.writeEndArray();
    }
}
