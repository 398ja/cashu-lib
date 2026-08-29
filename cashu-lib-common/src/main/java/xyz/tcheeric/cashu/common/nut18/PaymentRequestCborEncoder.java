package xyz.tcheeric.cashu.common.nut18;

import lombok.NonNull;
import xyz.tcheeric.cashu.common.TokenV4CborEncoder;
import xyz.tcheeric.cashu.common.nut10.Nut10Option;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a NUT-18 payment request as definite-length RFC-8949 CBOR.
 *
 * <p>Jackson writes maps in the indefinite-length form ({@code bf ... ff}) and the spec's vectors
 * use the definite-length form ({@code a5}). The two decode identically and encode to entirely
 * different byte strings, so a request encoded by Jackson is one no other implementation
 * reproduces. A payment request is quoted, stored and compared as a string, so that difference is
 * the whole substance of the format.
 *
 * <p>Jackson also serialized every public getter, which added {@code type}, {@code nostr} and
 * {@code httpPost} fields to each transport from methods that exist for callers rather than for the
 * wire. Writing the fields explicitly is what keeps the encoding to what NUT-18 defines.
 *
 * @see <a href="https://github.com/cashubtc/nuts/blob/main/18.md">NUT-18</a>
 */
final class PaymentRequestCborEncoder {

    private PaymentRequestCborEncoder() {
    }

    /**
     * Encodes a payment request.
     *
     * <p>Absent fields are omitted rather than written as null, and the map header counts only what
     * is present. An empty {@code mints} or {@code transports} list counts as absent: NUT-18 omits
     * the field entirely in that case, and an absent transport list means the payer chooses, which
     * is not the same statement as an empty one.
     */
    static byte[] encode(@NonNull PaymentRequest request) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Runnable> fields = new ArrayList<>();

        addIfPresent(fields, out, "i", request.getPaymentId());
        addIfPresent(fields, out, "a", request.getAmount());
        addIfPresent(fields, out, "u", request.getUnit());
        addIfPresent(fields, out, "s", request.getSingleUse());
        addTextListIfPresent(fields, out, "m", request.getMints());
        addIfPresent(fields, out, "d", request.getDescription());
        addTransportsIfPresent(fields, out, request.getTransports());
        addNut10IfPresent(fields, out, request.getNut10Option());

        TokenV4CborEncoder.writeMapHeader(out, fields.size());
        fields.forEach(Runnable::run);
        return out.toByteArray();
    }

    private static void addIfPresent(List<Runnable> fields, ByteArrayOutputStream out,
                                     String key, Object value) {
        if (value == null) {
            return;
        }
        fields.add(() -> {
            TokenV4CborEncoder.writeTextString(out, key);
            writeValue(out, value);
        });
    }

    private static void writeValue(ByteArrayOutputStream out, Object value) {
        if (value instanceof String text) {
            TokenV4CborEncoder.writeTextString(out, text);
        } else if (value instanceof Integer number) {
            TokenV4CborEncoder.writeInt(out, number);
        } else if (value instanceof Boolean flag) {
            // RFC 8949 §3.3: simple values 20 and 21 are false and true.
            out.write(flag ? 0xf5 : 0xf4);
        } else {
            throw new IllegalArgumentException("Unsupported payment request field type: " + value.getClass());
        }
    }

    private static void addTextListIfPresent(List<Runnable> fields, ByteArrayOutputStream out,
                                             String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        fields.add(() -> {
            TokenV4CborEncoder.writeTextString(out, key);
            TokenV4CborEncoder.writeArrayHeader(out, values.size());
            values.forEach(value -> TokenV4CborEncoder.writeTextString(out, value));
        });
    }

    private static void addTransportsIfPresent(List<Runnable> fields, ByteArrayOutputStream out,
                                               List<Transport> transports) {
        if (transports == null || transports.isEmpty()) {
            return;
        }
        fields.add(() -> {
            TokenV4CborEncoder.writeTextString(out, "t");
            TokenV4CborEncoder.writeArrayHeader(out, transports.size());
            transports.forEach(transport -> writeTransport(out, transport));
        });
    }

    private static void writeTransport(ByteArrayOutputStream out, Transport transport) {
        boolean hasTags = transport.getTags() != null && !transport.getTags().isEmpty();
        TokenV4CborEncoder.writeMapHeader(out, hasTags ? 3 : 2);
        TokenV4CborEncoder.writeTextString(out, "t");
        TokenV4CborEncoder.writeTextString(out, transport.getTypeValue());
        TokenV4CborEncoder.writeTextString(out, "a");
        TokenV4CborEncoder.writeTextString(out, transport.getTarget());
        if (hasTags) {
            TokenV4CborEncoder.writeTextString(out, "g");
            writeTagList(out, transport.getTags());
        }
    }

    private static void addNut10IfPresent(List<Runnable> fields, ByteArrayOutputStream out,
                                          Nut10Option nut10Option) {
        if (nut10Option == null) {
            return;
        }
        fields.add(() -> {
            TokenV4CborEncoder.writeTextString(out, "nut10");
            boolean hasTags = nut10Option.getTags() != null && !nut10Option.getTags().isEmpty();
            TokenV4CborEncoder.writeMapHeader(out, hasTags ? 3 : 2);
            TokenV4CborEncoder.writeTextString(out, "k");
            TokenV4CborEncoder.writeTextString(out, nut10Option.getKind().name());
            TokenV4CborEncoder.writeTextString(out, "d");
            TokenV4CborEncoder.writeTextString(out, nut10Option.getData());
            if (hasTags) {
                TokenV4CborEncoder.writeTextString(out, "t");
                writeTagList(out, nut10Option.getTags());
            }
        });
    }

    private static void writeTagList(ByteArrayOutputStream out, List<List<String>> tags) {
        TokenV4CborEncoder.writeArrayHeader(out, tags.size());
        for (List<String> tag : tags) {
            TokenV4CborEncoder.writeArrayHeader(out, tag.size());
            tag.forEach(value -> TokenV4CborEncoder.writeTextString(out, value));
        }
    }
}
