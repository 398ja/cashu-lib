package xyz.tcheeric.cashu.common.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import xyz.tcheeric.cashu.common.Signature;

/**
 * Ensures Signature values serialize as 33-byte compressed points (66 hex chars),
 * prefixed with 0x02 or 0x03. Falls back to 0x02 if parity is unknown.
 */
public class SignatureJsonSerializer extends JsonSerializer<Signature> {
    @Override
    public void serialize(Signature value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        String hex = value.toString();
        if (hex == null) {
            gen.writeNull();
            return;
        }
        String normalized = normalizeCompressedHex(hex);
        gen.writeString(normalized);
    }

    private static String normalizeCompressedHex(String hex) {
        String h = hex.trim().toLowerCase();
        // If already 66 chars and starts with 02/03, keep it
        if (h.length() == 66 && (h.startsWith("02") || h.startsWith("03"))) {
            return h;
        }
        // If 64 chars (x-only), prepend even prefix 02
        if (h.length() == 64 && h.matches("[0-9a-f]{64}")) {
            return "02" + h;
        }
        // If longer, keep the first 32 bytes for X and prepend 02
        if (h.length() > 66) {
            return "02" + h.substring(h.length() - 64);
        }
        // Otherwise, left-pad to 64 and prepend 02
        StringBuilder sb = new StringBuilder(66);
        sb.append("02");
        for (int i = h.length(); i < 64; i++) sb.append('0');
        sb.append(h);
        return sb.toString();
    }
}

