# Changelog

## 1.0.0 (2025-08-30)


### ⚠ BREAKING CHANGES

* **crypto:** The CryptoElement class has been renamed to BaseKey.

### Features

* add CompressedPublicKey class for handling compressed public keys ([1712461](https://github.com/398ja/cashu-lib/commit/17124612bc759bfcd68b4e86aa1412b963d51edb))
* add UnCompressedPublicKey class for handling uncompressed keys ([01ca7ab](https://github.com/398ja/cashu-lib/commit/01ca7ab8947ae76ad81a6e560bcef97ae3b28ae5))
* generic secret util ([ca1436e](https://github.com/398ja/cashu-lib/commit/ca1436ef4aaaecbb6214a81210f7f31d8578743f))
* generic secret util ([10fa9d2](https://github.com/398ja/cashu-lib/commit/10fa9d27e6f78d4809c035484a8f5394314be5eb))
* introduce keyset id value object ([40047f7](https://github.com/398ja/cashu-lib/commit/40047f757af69c46fd725e5c2aad6fb41d847c88))
* introduce keyset id value object ([37e5210](https://github.com/398ja/cashu-lib/commit/37e521062f994b10d64a3e9c7765de620b730180))
* **serializer:** add TagSerializer for WellKnownSecret.Tag ([8fc7d2d](https://github.com/398ja/cashu-lib/commit/8fc7d2d362543e208c49497a6b8415ab363b9e37))


### Bug Fixes

* align TokenV4 with NUT-00; add clickable URI support for V3/V4; add tests and docs ([e1911e9](https://github.com/398ja/cashu-lib/commit/e1911e9baeee5034274b4001d64b380900b0df6a))
* align TokenV4 with NUT-00; add clickable URI support for V3/V4; add tests and docs ([eaf548d](https://github.com/398ja/cashu-lib/commit/eaf548d018251b3d108dc0c53f2f08fc1ee81a7e))
* canonicalize TokenV4 serialization ([e5c9925](https://github.com/398ja/cashu-lib/commit/e5c9925ed7fc3bf14277566d82558477fc39e78a))
* canonicalize TokenV4 serialization ([e896f59](https://github.com/398ja/cashu-lib/commit/e896f596a2d82327c292c03a752ab9c6b5e719d3))
* correct schnorr signature and key lengths ([11e0a47](https://github.com/398ja/cashu-lib/commit/11e0a476bc3e58d067c01f8c4072c80ac0b0f1a3))
* ensure TokenV4 serialization matches NUT-00 example ([b869b33](https://github.com/398ja/cashu-lib/commit/b869b33b7922ba58de7e3821bfbefd757f332b23))
* handle schnorr signature and key lengths ([884a116](https://github.com/398ja/cashu-lib/commit/884a1166d16e4c9d94a6afef793f3945877c9274))


### Code Refactoring

* **crypto:** rename CryptoElement to BaseKey and simplify structure ([868bac0](https://github.com/398ja/cashu-lib/commit/868bac0b2289a3c965f884da396d473f50790f37))
