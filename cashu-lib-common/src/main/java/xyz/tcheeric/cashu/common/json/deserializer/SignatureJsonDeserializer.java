package xyz.tcheeric.cashu.common.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import xyz.tcheeric.cashu.common.Signature;

/**
 * Accepts signature values in multiple forms and normalizes to compressed hex (66 chars, 0x02/0x03).
 * - 66-char hex starting with 02/03: keep as-is
 * - 64-char hex (x-only): prepend 02
 * - 128-char hex Schnorr signature: take the first 64 as X, prepend 02
 * - otherwise: left-pad to 64 and prepend 02
 */
public class SignatureJsonDeserializer extends JsonDeserializer<Signature> {
    @Override
    public Signature deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_STRING) {
            String text = p.getValueAsString();
            String norm = normalizeCompressedHex(text);
            return Signature.fromString(norm);
        }
        // Fallback: try to read as Object and normalize if string
        Object codecRead = p.readValueAs(Object.class);
        if (codecRead instanceof String s) {
            String norm = normalizeCompressedHex(s);
            return Signature.fromString(norm);
        }
        return null;
    }

    private static String normalizeCompressedHex(String hex) {
        if (hex == null) return null;
        String h = hex.trim().toLowerCase();
        if (h.length() == 66 && (h.startsWith("02") || h.startsWith("03"))) return h;
        if (h.length() == 64 && h.matches("[0-9a-f]{64}")) return "02" + h;
        if (h.length() > 66) {
            if (h.matches("[0-9a-f]{128}")) {
                return "02" + h.substring(0, 64);
            }
            throw new IllegalArgumentException("Unsupported signature length: " + h.length());
        }
        StringBuilder sb = new StringBuilder(66);
        sb.append("02");
        for (int i = h.length(); i < 64; i++) sb.append('0');
        sb.append(h);
        return sb.toString();
    }
}

