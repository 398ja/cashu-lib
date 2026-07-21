# V4 definite-length CBOR — cashu-ts interop golden vector

## What this is

`v4-definite-token.txt` is a single-line, pinned `cashuB...` V4 token string produced by
`TokenV4.serialize(false)` (definite-length CBOR, via `TokenV4CborEncoder`), built from the
fixed sample in `TokenV4GoldenTest` (`cashu-lib-common/src/test/java/xyz/tcheeric/cashu/entities/TokenV4GoldenTest.java`):

- keyset id: `00e3372e61d05605`
- proof 0: `a=1`, `s` = a VOUCHER-style NUT-10 secret (`["VOUCHER","bf69fa9c...","nonce",[["unit","sat"]]]`),
  `c` = a 33-byte compressed-point-shaped value, `dleq=null`
- proof 1: `a=256`, `s="s1"`, `c` = a different 33-byte value, `dleq=null`
- mint: `https://mint.staging.398ja.xyz`
- unit: `sat`
- memo: absent (not set)

`TokenV4GoldenTest.serializeMatchesPinnedCashuTsInteropVector` asserts `serialize(false)` for this
fixed input still equals the committed file — so any regression in the encoder (e.g. reverting to
indefinite-length CBOR) breaks this test first, before it reaches a downstream JS consumer.

## Why it exists (the anchor)

Tasks 1–3 changed `TokenV4CborEncoder` to emit **definite-length** CBOR maps/arrays instead of
indefinite-length ones. The old indefinite-length output could round-trip inside this Java repo
(Jackson's CBOR codec tolerates both), but **cashu-ts (the JS reference implementation used by
the wallet frontends) could not decode it** — `getDecodedToken()` threw `Unsupported length: 31`
on the indefinite-length markers (`0xbf` map / `0x9f` array / `0xff` break).

This golden vector + the `verify-cashu-ts.mjs` script below are the proof that the fix actually
closes that gap: a real cashu-ts install can decode the Java-produced token end-to-end.

## How to re-verify with cashu-ts

Any `@cashu/cashu-ts` v4.x install works. The command below uses the copy already vendored in the
sibling `imani-apps` repo (`packages/offline-wallet`), which is what `verify-cashu-ts.mjs`
imports by absolute path today:

```bash
cd cashu-lib-common/src/test/resources/golden
node verify-cashu-ts.mjs
```

`verify-cashu-ts.mjs`:

```js
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { getDecodedToken } from '/home/eric/IdeaProjects/imani-apps/packages/offline-wallet/node_modules/@cashu/cashu-ts/lib/cashu-ts.es.js';

const tokenPath = fileURLToPath(new URL('./v4-definite-token.txt', import.meta.url));
const t = readFileSync(tokenPath, 'utf8').trim();

// cashu-ts 4.x's getDecodedToken(tokenString, keysetIds) takes the set of known keyset ids for
// the mint being decoded against.
const KNOWN_KEYSET_IDS = ['00e3372e61d05605'];

const d = getDecodedToken(t, KNOWN_KEYSET_IDS);
console.log('DECODED_OK', d.mint, d.unit, d.proofs.length);
```

If you have a standalone `@cashu/cashu-ts` install elsewhere (e.g. `npm i @cashu/cashu-ts` in a
scratch dir), swap the absolute import path for a plain `import { getDecodedToken } from
'@cashu/cashu-ts';` — the logic is unaffected.

**Note (cashu-ts 4.7.2 API detail):** `getDecodedToken` takes a mandatory second argument,
`keysetIds: readonly string[]` — the known keyset ids for the mint, used to disambiguate keyset-id
encodings. Omitting it throws inside cashu-ts's internal keyset-id-format detection (`Cannot read
properties of undefined (reading 'map')`), which is unrelated to this repo's CBOR encoding and is
NOT the `Unsupported length` failure this vector guards against — the fix above is just to pass
the keyset id.

## Verification evidence

Run 2026-07-21, node v20.18.1, `@cashu/cashu-ts@4.7.2` (vendored under
`imani-apps/packages/offline-wallet/node_modules/@cashu/cashu-ts`):

```
$ cd cashu-lib-common/src/test/resources/golden && node verify-cashu-ts.mjs
DECODED_OK https://mint.staging.398ja.xyz sat 2
```

No `Unsupported length` error, no indefinite-length CBOR rejection. `getDecodedToken` returned a
`Token` with `mint === "https://mint.staging.398ja.xyz"`, `unit === "sat"`, and `proofs.length ===
2` — matching the fixed sample exactly. This confirms the definite-length CBOR output from
`TokenV4CborEncoder` is cashu-ts-decodable.

## If the encoder changes

1. Update `TokenV4GoldenTest.sample()` only if the *fixture* needs to change (keyset id, proof
   shapes, mint URL, etc.) — do not hand-edit the pinned `.txt` file.
2. Temporarily add a print statement (or a scratch test) to capture the new
   `serialize(false)` output, overwrite `v4-definite-token.txt` with it (single line, no
   trailing newline content beyond what `.trim()` tolerates).
3. Re-run `node verify-cashu-ts.mjs` and confirm `DECODED_OK ...` with no error.
4. Update the "Verification evidence" section above with the new run's output.
5. Commit the updated `.txt` + this README together with the encoder change.
