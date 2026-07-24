# Definite-length V4 CBOR Encoding — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Make `cashu-lib` encode V4 Cashu tokens as **definite-length** CBOR (NUT-00 layout) so cashu-ts and every standard wallet can decode them.

**Architecture:** Add a small hand-rolled `TokenV4CborEncoder` in `cashu-lib-common` that writes definite-length CBOR primitives; point `TokenV4.serialize()` at it instead of Jackson's `CBOR_MAPPER.writeValueAsBytes`. Decode (`deserialize` via Jackson `readValue`) is unchanged — it already reads both. Roll out via BOM bump to cashu-mint/gateways.

**Tech Stack:** Java 21, Maven (multi-module), JUnit 5 + AssertJ, Jackson CBOR (decode only). Repo: `cashu-lib` (module `cashu-lib-common`, v0.19.0).

## Global Constraints

- Only the **encode** path changes. `TokenV4.deserialize()` (Jackson `readValue`) stays as-is.
- Output must be **definite-length** CBOR — no `0xbf` (indefinite map), `0x9f` (indefinite array), or `0xff` (break) bytes anywhere.
- Exact V4 structure: `TokenV4{ "t":[ TokenData{ "i":bstr, "p":[ TokenProof{ "a":uint, "s":tstr, "c":bstr, "d"?:DLEQ{"e":bstr,"s":bstr,"r":bstr}, "w"?:tstr } ] } ], "m":tstr, "u":tstr, "d"?:tstr }`.
- Byte strings (major 2): `i`, `c`, DLEQ `e`/`s`/`r`. Text strings (major 3): `m`, `u`, `d`, `s`, `w`. Unsigned int (major 0, minimal header): `a`.
- **Optional keys omitted entirely when null** (`d` memo, proof `d` dleq, `w` witness) — map length counts only present keys. Mirrors cashu-ts (absent, not null).
- Amounts use minimal-length uint headers (0..23 inline; 24→1 byte; 256→2 bytes; etc.).
- `TokenUtil.serialize` framing (`cashuB` + unpadded url-safe base64) is unchanged — already cashu-ts-compatible.
- Test module: `cashu-lib-common`; tests under `src/test/java/xyz/tcheeric/cashu/entities/`. Run a single test from repo root: `mvn -pl cashu-lib-common -am -Dtest=<Class> test`.

---

### Task 1: `TokenV4CborEncoder` — CBOR primitive writers

**Files:**
- Create: `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/TokenV4CborEncoder.java`
- Test: `cashu-lib-common/src/test/java/xyz/tcheeric/cashu/entities/TokenV4CborEncoderPrimitivesTest.java`

**Interfaces:**
- Produces: package-visible static methods on `TokenV4CborEncoder` for unit-testing the primitives: `static byte[] majorHeader(int major, long n)`, `static void writeUint(ByteArrayOutputStream o, long n)`, `static void writeByteString(ByteArrayOutputStream o, byte[] b)`, `static void writeTextString(ByteArrayOutputStream o, String s)`, `static void writeMapHeader(ByteArrayOutputStream o, int n)`, `static void writeArrayHeader(ByteArrayOutputStream o, int n)`. (Full-token `encode` lands in Task 2.)

- [ ] **Step 1: Write the failing test** — assert CBOR header/primitive bytes against known-good CBOR (RFC 8949 examples):
```java
package xyz.tcheeric.cashu.entities;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4CborEncoder;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.assertThat;

class TokenV4CborEncoderPrimitivesTest {
    private static byte[] cap(java.util.function.Consumer<ByteArrayOutputStream> w) {
        var o = new ByteArrayOutputStream(); w.accept(o); return o.toByteArray();
    }
    @Test void uintMinimalHeaders() {
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 0))).containsExactly(0x00);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 23))).containsExactly(0x17);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 24))).containsExactly(0x18, 0x18);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 255))).containsExactly(0x18, 0xFF);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 256))).containsExactly(0x19, 0x01, 0x00);
        assertThat(cap(o -> TokenV4CborEncoder.writeUint(o, 1000000))).containsExactly(0x1A, 0x00, 0x0F, 0x42, 0x40);
    }
    @Test void byteStringHeaderAndBody() {
        // h'01020304' -> 0x44 01 02 03 04
        assertThat(cap(o -> TokenV4CborEncoder.writeByteString(o, new byte[]{1,2,3,4})))
            .containsExactly(0x44, 0x01, 0x02, 0x03, 0x04);
    }
    @Test void textStringHeaderAndBody() {
        // "sat" -> 0x63 73 61 74
        assertThat(cap(o -> TokenV4CborEncoder.writeTextString(o, "sat")))
            .containsExactly(0x63, 0x73, 0x61, 0x74);
    }
    @Test void mapAndArrayHeadersAreDefiniteLength() {
        assertThat(cap(o -> TokenV4CborEncoder.writeMapHeader(o, 4))).containsExactly(0xA4);   // NOT 0xBF
        assertThat(cap(o -> TokenV4CborEncoder.writeArrayHeader(o, 2))).containsExactly(0x82); // NOT 0x9F
    }
}
```

- [ ] **Step 2: Run — verify it fails.** `mvn -pl cashu-lib-common -am -Dtest=TokenV4CborEncoderPrimitivesTest test` → FAIL (class missing).

- [ ] **Step 3: Implement the primitives:**
```java
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
    static byte[] majorHeader(int major, long n) { var o = new ByteArrayOutputStream(); writeHead(o, major, n); return o.toByteArray(); }
    static void writeUint(ByteArrayOutputStream o, long n) { writeHead(o, 0, n); }
    static void writeByteString(ByteArrayOutputStream o, byte[] b) { writeHead(o, 2, b.length); o.writeBytes(b); }
    static void writeTextString(ByteArrayOutputStream o, String s) { byte[] b = s.getBytes(StandardCharsets.UTF_8); writeHead(o, 3, b.length); o.writeBytes(b); }
    static void writeMapHeader(ByteArrayOutputStream o, int n) { writeHead(o, 5, n); }
    static void writeArrayHeader(ByteArrayOutputStream o, int n) { writeHead(o, 4, n); }
}
```

- [ ] **Step 4: Run — verify it passes.** Same command → PASS.

- [ ] **Step 5: Commit.**
```bash
git add cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/TokenV4CborEncoder.java \
        cashu-lib-common/src/test/java/xyz/tcheeric/cashu/entities/TokenV4CborEncoderPrimitivesTest.java
git commit -m "feat(cbor): definite-length CBOR primitive writers for V4 tokens"
```

---

### Task 2: `encode(TokenV4)` — full token tree

**Files:**
- Modify: `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/TokenV4CborEncoder.java`
- Test: `cashu-lib-common/src/test/java/xyz/tcheeric/cashu/entities/TokenV4CborEncoderTest.java`

**Interfaces:**
- Consumes: the primitive writers (Task 1) + `TokenV4`/`TokenData`/`TokenProof`/`DLEQProof` getters (`getMintUrl`, `getUnit`, `getMemo`, `getTokenDataList`; `getKeySetId`, `getProofs`; `getAmount`, `getSecret`, `getSignature`, `getDleqProof`, `getWitness`; `getE/getS/getR`).
- Produces: `public static byte[] encode(TokenV4 token)`.

- [ ] **Step 1: Write the failing test** — build a token with one keyset, two proofs (one WITH dleq, one WITHOUT), memo present; assert top-level + structural bytes are definite-length and optional-omission is correct:
```java
package xyz.tcheeric.cashu.entities;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import xyz.tcheeric.cashu.common.TokenV4CborEncoder;
import static org.assertj.core.api.Assertions.assertThat;

class TokenV4CborEncoderTest {
    private static TokenV4 sample(boolean withMemo) {
        // Build via TokenV4's public API/setters as used elsewhere in the codebase.
        // proof0: amount=1, secret="s0", C=h'aa', dleq={e:h'01',s:h'02',r:h'03'}
        // proof1: amount=256, secret="s1", C=h'bb', NO dleq, NO witness
        // keyset i = h'00e3372e61d05605'; m="https://mint.x"; u="sat"; d = withMemo? "hi": null
        // ... (use the same construction the existing TokenV4Test uses) ...
        return /* constructed TokenV4 */ null; // implementer fills using existing builders/setters
    }
    @Test void topLevelMapIsDefiniteLength() {
        byte[] b = TokenV4CborEncoder.encode(sample(true));
        assertThat(b[0] & 0xFF).isNotEqualTo(0xBF);          // not indefinite map
        assertThat(b[0] & 0xE0).isEqualTo(0xA0);             // major type 5 (map), definite
        assertThat((b[0] & 0xFF)).isEqualTo(0xA4);           // 4 keys: t,m,u,d
    }
    @Test void noIndefiniteOrBreakBytesAnywhere() {
        byte[] b = TokenV4CborEncoder.encode(sample(true));
        for (byte x : b) {
            int u = x & 0xFF;
            assertThat(u).isNotEqualTo(0xBF); // indefinite map
            assertThat(u).isNotEqualTo(0x9F); // indefinite array
            // 0xFF as a *break* only occurs after an indefinite head; since none exist, none should appear as a header.
        }
    }
    @Test void memoOmittedWhenNull_topMapHasThreeKeys() {
        byte[] b = TokenV4CborEncoder.encode(sample(false));
        assertThat(b[0] & 0xFF).isEqualTo(0xA3);             // t,m,u only
    }
}
```
(Implementer: construct `sample()` using the same `TokenV4`/`TokenData`/`TokenProof` construction path the existing `TokenV4Test.java` uses — read it first.)

- [ ] **Step 2: Run — verify it fails.** `mvn -pl cashu-lib-common -am -Dtest=TokenV4CborEncoderTest test` → FAIL (`encode` missing).

- [ ] **Step 3: Implement `encode`:**
```java
public static byte[] encode(TokenV4 token) {
    var o = new java.io.ByteArrayOutputStream();
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
```
(Verify the getter names against `TokenV4.java` — adjust if the amount getter returns `Integer` (unbox to `long`) or the C field getter is `getSignature`/`getC`.)

- [ ] **Step 4: Run — verify it passes.** Same command → PASS.

- [ ] **Step 5: Commit.**
```bash
git add cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/TokenV4CborEncoder.java \
        cashu-lib-common/src/test/java/xyz/tcheeric/cashu/entities/TokenV4CborEncoderTest.java
git commit -m "feat(cbor): encode full V4 token tree as definite-length CBOR"
```

---

### Task 3: Wire `TokenV4.serialize()` to the new encoder + round-trip

**Files:**
- Modify: `cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/TokenV4.java:109`
- Test: `cashu-lib-common/src/test/java/xyz/tcheeric/cashu/entities/TokenV4RoundTripTest.java`

**Interfaces:** Consumes `TokenV4CborEncoder.encode` (Task 2).

- [ ] **Step 1: Write the failing test** — the serialized token must (a) decode back via `deserialize` to an equal token, and (b) its raw CBOR must not start with `0xbf`:
```java
package xyz.tcheeric.cashu.entities;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.cashu.common.TokenV4;
import java.util.Base64;
import static org.assertj.core.api.Assertions.assertThat;

class TokenV4RoundTripTest {
    @Test void serializeProducesDefiniteLengthAndRoundTrips() {
        TokenV4 t = /* same sample construction as TokenV4CborEncoderTest */;
        String s = t.serialize(false);
        // strip "cashuB" prefix, url-decode, check first byte
        String b64 = s.substring("cashuB".length());
        byte[] cbor = Base64.getUrlDecoder().decode(b64);
        assertThat(cbor[0] & 0xFF).isNotEqualTo(0xBF);
        assertThat(cbor[0] & 0xE0).isEqualTo(0xA0); // definite map
        // round-trip
        TokenV4 back = TokenV4.deserialize(s);
        assertThat(back.getMintUrl()).isEqualTo(t.getMintUrl());
        assertThat(back.getUnit()).isEqualTo(t.getUnit());
        assertThat(back.getTokenDataList().size()).isEqualTo(t.getTokenDataList().size());
        // spot-check amounts/secrets survive
    }
}
```

- [ ] **Step 2: Run — verify it fails.** `mvn -pl cashu-lib-common -am -Dtest=TokenV4RoundTripTest test` → FAIL (still `0xbf` from Jackson).

- [ ] **Step 3: Implement — swap the encode call** at `TokenV4.java:109`:
```java
// was: byte[] cborToken = JsonUtils.CBOR_MAPPER.writeValueAsBytes(this);
byte[] cborToken = TokenV4CborEncoder.encode(this);
```
Leave `deserialize()` and the imports for `JsonUtils` used elsewhere intact (only remove the CBOR_MAPPER encode use if now unused — check).

- [ ] **Step 4: Run — verify it passes.** Same command → PASS.

- [ ] **Step 5: Run the whole module** to catch any test that asserted the old bytes: `mvn -pl cashu-lib-common -am test`. If `TokenV4CBORInspectTest` / `TokenV4UnitBugTest` / `TokenV4Test` assert indefinite-length bytes or Jackson-specific output, update those assertions to the definite-length expectation (they should still round-trip). Expected: green.

- [ ] **Step 6: Commit.**
```bash
git add cashu-lib-common/src/main/java/xyz/tcheeric/cashu/common/TokenV4.java \
        cashu-lib-common/src/test/java/xyz/tcheeric/cashu/entities/TokenV4RoundTripTest.java
git commit -m "feat(cbor): serialize V4 tokens as definite-length CBOR (cashu-ts interop)"
```

---

### Task 4: cashu-ts interop golden vector

**Files:**
- Create: `cashu-lib-common/src/test/resources/golden/v4-definite-token.txt` (the Java-encoded token string)
- Create: `cashu-lib-common/src/test/resources/golden/README.md` (how to verify with cashu-ts)
- Test: extend `TokenV4RoundTripTest` (or a new `TokenV4GoldenTest`) to emit + pin the vector.

**Purpose:** prove — and keep proving — that the Java output is cashu-ts-decodable.

- [ ] **Step 1: Emit the vector.** Add a test that builds a FIXED sample token (fixed keyset id, 2 proofs incl. a VOUCHER-style long secret + a no-dleq proof, fixed mint URL `https://mint.staging.398ja.xyz`, unit `sat`), `serialize(false)`, and writes the exact string to `target/` (and asserts it equals the committed `v4-definite-token.txt` once pinned). Run it once, copy the produced string into the resource file, commit.

- [ ] **Step 2: cashu-ts check (documented + optional CI).** Write `golden/README.md` with a runnable node snippet:
```js
// npm i @cashu/cashu-ts  &&  node this
const { getDecodedToken } = require('@cashu/cashu-ts');
const t = require('fs').readFileSync('v4-definite-token.txt','utf8').trim();
const d = getDecodedToken(t);
console.log('OK', d.mint, d.unit, d.proofs.length);
```
Run it once locally (node) to confirm `getDecodedToken` succeeds on the committed vector; paste the output into the README as evidence. (If a JS step in Java CI is desired, add a tiny `maven-exec`/`frontend-maven-plugin` step — otherwise this is a documented one-time gate re-run on encoder changes.)

- [ ] **Step 3: Commit** the vector + README + the pinning test.
```bash
git add cashu-lib-common/src/test/resources/golden/ cashu-lib-common/src/test/java/xyz/tcheeric/cashu/entities/
git commit -m "test(cbor): cashu-ts interop golden vector for definite-length V4 tokens"
```

---

### Task 5: Release cashu-lib + BOM + rebuild/redeploy issuer

**Files:** poms (version), `imani-bom`, then consuming repos.

- [ ] **Step 1:** `/bumpup` cashu-lib a minor version (new feature). `mvn -q -DskipTests install`.
- [ ] **Step 2:** Bump the cashu-lib version in `imani-bom` (via `/bumpup` there); `mvn -q -DskipTests install`.
- [ ] **Step 3: Identify issuers to rebuild.** Grep `cashu-mint` + `imani-wallet-lib`/gateway repos for `TokenV4` / `.serialize(` usage on token types (open-Q2). Any service that **encodes** a token to hand to a client must consume the new cashu-lib via the BOM bump and be rebuilt. At minimum the token-issuing gateway (`customer-wallet`) — confirm whether cashu-mint also serializes tokens it returns.
- [ ] **Step 4:** Rebuild the issuer image(s) (jib — **fully-qualified goal** `com.google.cloud.tools:jib-maven-plugin:3.4.0:build`, the `jib:build` prefix fails at the reactor) and redeploy to staging (recreate the `customer-wallet` / affected service).
- [ ] **Step 5: Verify end-to-end.** Issue a fresh voucher on staging; in a browser console on the offline wallet run `window.OfflineWallet.decodeStrict(token, [])` — it must now DECODE (no `Unsupported length: 31`). (The next blocker after this is DLEQ — a separate spec.)

---

## Self-Review notes
- Spec coverage: hand-rolled encoder (T1+T2) ✓; wire serialize + round-trip (T3) ✓; cashu-ts golden vector interop anchor (T4) ✓; rollout/BOM/redeploy (T5) ✓; definite-length + optional-omission + minimal-int constraints enforced in T1/T2 tests ✓.
- Open-Q3 (unpadded url-safe base64) resolved during planning: `TokenUtil.serialize` already uses `Base64.getUrlEncoder().withoutPadding()` — no change needed.
- Open-Q1/Q2 (other encode callers / does cashu-mint encode) are resolved in T5 Step 3 before release.
- Out of scope: DLEQ emission (next spec), migrating existing tokens.
