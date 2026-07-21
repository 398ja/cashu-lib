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
}
