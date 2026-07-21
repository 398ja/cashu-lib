// Interop anchor check: proves cashu-ts's getDecodedToken() can decode the definite-length
// CBOR TokenV4 emitted by xyz.tcheeric.cashu.common.TokenV4CborEncoder (pinned in
// v4-definite-token.txt, produced by TokenV4GoldenTest).
//
// Run with a real @cashu/cashu-ts install. In this repo's sibling project there is already one
// vendored under imani-apps/packages/offline-wallet/node_modules; that's what the documented
// command below uses. Any @cashu/cashu-ts >=4.x install works equally well -- e.g.
// `npm i @cashu/cashu-ts` in a scratch dir and importing '@cashu/cashu-ts' directly.
//
// Usage (from this directory):
//   node verify-cashu-ts.mjs
//
// Expected stdout (no thrown error, especially no "Unsupported length"):
//   DECODED_OK https://mint.staging.398ja.xyz sat 2

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { getDecodedToken } from '/home/eric/IdeaProjects/imani-apps/packages/offline-wallet/node_modules/@cashu/cashu-ts/lib/cashu-ts.es.js';

const tokenPath = fileURLToPath(new URL('./v4-definite-token.txt', import.meta.url));
const t = readFileSync(tokenPath, 'utf8').trim();

// cashu-ts 4.x's getDecodedToken(tokenString, keysetIds) takes the set of known keyset ids for
// the mint being decoded against (used to disambiguate hex vs. other keyset-id encodings). The
// golden vector uses keyset id "00e3372e61d05605" (see TokenV4GoldenTest).
const KNOWN_KEYSET_IDS = ['00e3372e61d05605'];

const d = getDecodedToken(t, KNOWN_KEYSET_IDS);
console.log('DECODED_OK', d.mint, d.unit, d.proofs.length);
