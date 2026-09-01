# Vendored cashubtc/nuts test vectors

These files are copied verbatim from [cashubtc/nuts](https://github.com/cashubtc/nuts) at pinned
commit
[`49a909ce4d0739824b3859d4b3da21e6c1abdaeb`](https://github.com/cashubtc/nuts/tree/49a909ce4d0739824b3859d4b3da21e6c1abdaeb),
directory `tests/`.

| File | Specification | Driven by |
| --- | --- | --- |
| `00-tests.md` | NUT-00 hash-to-curve, BDHKE, token serialization | `Nut00VectorTest` |
| `01-tests.md` | NUT-01 keyset key validation | `Nut01VectorTest` |
| `02-tests.md` | NUT-02 keyset identifiers | `Nut02VectorTest` |
| `11-test.md` | NUT-11 P2PK spending conditions | `Nut11VectorTest` |
| `12-tests.md` | NUT-12 DLEQ proofs | `Nut12VectorTest` |
| `13-tests.md` | NUT-13 deterministic secrets | `Nut13VectorTest` |
| `18-tests.md` | NUT-18 payment requests | `Nut18VectorTest` |
| `20-test.md` | NUT-20 signed mint quotes | `Nut20VectorTest` |

Upstream also publishes `26-test.md`, `27-test.md`, `28-tests.md` and `29-tests.md`. They are
deliberately not vendored: NUT-26, 27, 28 and 29 are unimplemented, and vectors for a specification
we do not implement only restate that fact.

## Refreshing

Copy the upstream `tests/` files over these, unchanged, and update the commit above. Do not edit
the vendored content: the tests parse upstream's Markdown so that a refresh diff is reviewable
against the specification repository.

## What the vectors cannot pin down

See [`docs/reference/nut-test-vector-coverage.md`](../../../../../../docs/reference/nut-test-vector-coverage.md).
