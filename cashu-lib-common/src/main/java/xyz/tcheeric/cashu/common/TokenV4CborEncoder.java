package xyz.tcheeric.cashu.common;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Minimal RFC-8949 definite-length CBOR writer for the V4 token structure. */
public final class TokenV4CborEncoder {
    private TokenV4CborEncoder() {}

    // major: 0=uint,2=bstr,3=tstr,4=array,5=map. Writes the minimal-length head with the count/length n.
    static void writeHead(ByteArrayOutputStream o, int major, long n) {
        int mt = major << 5;
        if (n < 24)            { o.write(mt | (int) n); }
        else if (n < 0x100L)   { o.write(mt | 24); o.write((int) n); }
        else if (n < 0x10000L) { o.write(mt | 25); o.write((int)(n >> 8)); o.write((int) n); }
        else if (n < 0x100000000L) { o.write(mt | 26); o.write((int)(n>>24)); o.write((int)(n>>16)); o.write((int)(n>>8)); o.write((int) n); }
        else { o.write(mt | 27); for (int s = 56; s >= 0; s -= 8) o.write((int)(n >> s)); }
    }
    public static byte[] majorHeader(int major, long n) { var o = new ByteArrayOutputStream(); writeHead(o, major, n); return o.toByteArray(); }
    public static void writeUint(ByteArrayOutputStream o, long n) { writeHead(o, 0, n); }
    /**
     * Signed CBOR integer: non-negative → major 0 (uint); negative → major 1 with head value {@code -1-n}
     * (RFC 8949 §3.1). Amounts are normally positive, but an invalid/negative amount must round-trip as a
     * CBOR negative integer (mirroring Jackson) so downstream decode validation rejects it — rather than
     * being mangled by the unsigned writer into a corrupt header byte.
     */
    public static void writeInt(ByteArrayOutputStream o, long n) {
        if (n >= 0) writeHead(o, 0, n);
        else writeHead(o, 1, -1 - n);
    }
    public static void writeByteString(ByteArrayOutputStream o, byte[] b) { writeHead(o, 2, b.length); o.writeBytes(b); }
    public static void writeTextString(ByteArrayOutputStream o, String s) { byte[] b = s.getBytes(StandardCharsets.UTF_8); writeHead(o, 3, b.length); o.writeBytes(b); }
    public static void writeMapHeader(ByteArrayOutputStream o, int n) { writeHead(o, 5, n); }
    public static void writeArrayHeader(ByteArrayOutputStream o, int n) { writeHead(o, 4, n); }

    /**
     * Encodes a full V4 token tree as definite-length RFC-8949 CBOR.
     *
     * <p>Every model class is annotated {@code @JsonInclude(NON_NULL)}, so this encoder
     * mirrors that exactly: <b>any null field is omitted entirely</b> and each map header
     * counts only the fields actually present. This matches the Jackson output byte-for-byte
     * for the field set, and prevents NPEs on optional/nullable fields (e.g. the NUT-12 DLEQ
     * blinding factor {@code r}, which is optional; or a proof with no signature in a
     * partially-built/negative-test token).
     */
    public static byte[] encode(TokenV4 token) {
        var o = new ByteArrayOutputStream();

        // ---- top-level token map in NUT-00 order: t, d(memo), m, u — each only when non-null ----
        var tds = token.getTokenDataList();
        int topKeys = (tds != null ? 1 : 0)
                + (token.getMintUrl() != null ? 1 : 0)
                + (token.getUnit() != null ? 1 : 0)
                + (token.getMemo() != null ? 1 : 0);
        writeMapHeader(o, topKeys);
        if (tds != null) {
            writeTextString(o, "t");
            writeArrayHeader(o, tds.size());
            for (var td : tds) {
                // ---- TokenData map: i (if non-null), p (if non-null) ----
                var ps = td.getProofs();
                int tdKeys = (td.getKeySetId() != null ? 1 : 0) + (ps != null ? 1 : 0);
                writeMapHeader(o, tdKeys);
                if (td.getKeySetId() != null) { writeTextString(o, "i"); writeByteString(o, td.getKeySetId()); }
                if (ps != null) {
                    writeTextString(o, "p");
                    writeArrayHeader(o, ps.size());
                    for (var p : ps) {
                        // ---- TokenProof map: a, s, c, d(dleq), w(witness) — each only when non-null ----
                        int pk = (p.getAmount() != null ? 1 : 0)
                                + (p.getSecret() != null ? 1 : 0)
                                + (p.getSignature() != null ? 1 : 0)
                                + (p.getDleqProof() != null ? 1 : 0)
                                + (p.getWitness() != null ? 1 : 0);
                        writeMapHeader(o, pk);
                        if (p.getAmount() != null)    { writeTextString(o, "a"); writeInt(o, p.getAmount().longValue()); }
                        if (p.getSecret() != null)    { writeTextString(o, "s"); writeTextString(o, p.getSecret()); }
                        if (p.getSignature() != null) { writeTextString(o, "c"); writeByteString(o, p.getSignature()); }
                        if (p.getDleqProof() != null) {
                            var dq = p.getDleqProof();
                            // ---- DLEQ map: e, s, r — r (blinding factor) is optional ----
                            int dk = (dq.getE() != null ? 1 : 0) + (dq.getS() != null ? 1 : 0) + (dq.getR() != null ? 1 : 0);
                            writeTextString(o, "d");
                            writeMapHeader(o, dk);
                            if (dq.getE() != null) { writeTextString(o, "e"); writeByteString(o, dq.getE()); }
                            if (dq.getS() != null) { writeTextString(o, "s"); writeByteString(o, dq.getS()); }
                            if (dq.getR() != null) { writeTextString(o, "r"); writeByteString(o, dq.getR()); }
                        }
                        if (p.getWitness() != null) { writeTextString(o, "w"); writeTextString(o, p.getWitness()); }
                    }
                }
            }
        }
        if (token.getMemo() != null)    { writeTextString(o, "d"); writeTextString(o, token.getMemo()); }
        if (token.getMintUrl() != null) { writeTextString(o, "m"); writeTextString(o, token.getMintUrl()); }
        if (token.getUnit() != null)    { writeTextString(o, "u"); writeTextString(o, token.getUnit()); }
        return o.toByteArray();
    }
}
