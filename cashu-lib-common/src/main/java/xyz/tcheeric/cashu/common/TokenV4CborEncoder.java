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
    public static void writeByteString(ByteArrayOutputStream o, byte[] b) { writeHead(o, 2, b.length); o.writeBytes(b); }
    public static void writeTextString(ByteArrayOutputStream o, String s) { byte[] b = s.getBytes(StandardCharsets.UTF_8); writeHead(o, 3, b.length); o.writeBytes(b); }
    public static void writeMapHeader(ByteArrayOutputStream o, int n) { writeHead(o, 5, n); }
    public static void writeArrayHeader(ByteArrayOutputStream o, int n) { writeHead(o, 4, n); }

    /** Encodes a full V4 token tree as definite-length RFC-8949 CBOR (optional keys omitted when null). */
    public static byte[] encode(TokenV4 token) {
        var o = new ByteArrayOutputStream();
        // top map: t, m, u, and d only if memo != null
        int keys = 3 + (token.getMemo() != null ? 1 : 0);
        writeMapHeader(o, keys);
        // "t" -> array of TokenData
        writeTextString(o, "t");
        var tds = token.getTokenDataList();
        writeArrayHeader(o, tds.size());
        for (var td : tds) {
            writeMapHeader(o, 2);                 // i, p
            writeTextString(o, "i"); writeByteString(o, td.getKeySetId());
            writeTextString(o, "p");
            var ps = td.getProofs();
            writeArrayHeader(o, ps.size());
            for (var p : ps) {
                int pk = 3 + (p.getDleqProof() != null ? 1 : 0) + (p.getWitness() != null ? 1 : 0);
                writeMapHeader(o, pk);            // a, s, c [, d][, w]
                writeTextString(o, "a"); writeUint(o, p.getAmount());
                writeTextString(o, "s"); writeTextString(o, p.getSecret());
                writeTextString(o, "c"); writeByteString(o, p.getSignature());
                if (p.getDleqProof() != null) {
                    var dq = p.getDleqProof();
                    writeTextString(o, "d");
                    writeMapHeader(o, 3);         // e, s, r
                    writeTextString(o, "e"); writeByteString(o, dq.getE());
                    writeTextString(o, "s"); writeByteString(o, dq.getS());
                    writeTextString(o, "r"); writeByteString(o, dq.getR());
                }
                if (p.getWitness() != null) { writeTextString(o, "w"); writeTextString(o, p.getWitness()); }
            }
        }
        writeTextString(o, "m"); writeTextString(o, token.getMintUrl());
        writeTextString(o, "u"); writeTextString(o, token.getUnit());
        if (token.getMemo() != null) { writeTextString(o, "d"); writeTextString(o, token.getMemo()); }
        return o.toByteArray();
    }
}
