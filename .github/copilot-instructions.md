# Copilot Instructions

These guidelines help contributors and GitHub Copilot work effectively in this repository.

## Contributor Guidelines

- Follow the directives in [AGENTS.md](../AGENTS.md) when modifying code or documentation.
- Run `mvn -q verify` before pushing changes to ensure the build and tests pass.
- Keep commits focused and include descriptive messages.

## Protocol References

- Cashu protocol NUT specifications:
  - [NUT-00: Notation, Utilization, and Terminology](https://github.com/cashubtc/nuts/blob/main/00.md)
  - [NUT-01: Mint public key exchange](https://github.com/cashubtc/nuts/blob/main/01.md)
  - [NUT-02: Keysets and fees](https://github.com/cashubtc/nuts/blob/main/02.md)
  - [NUT-03: Swap tokens](https://github.com/cashubtc/nuts/blob/main/03.md)
  - [NUT-04: Mint tokens](https://github.com/cashubtc/nuts/blob/main/04.md)
  - [NUT-05: Melting tokens](https://github.com/cashubtc/nuts/blob/main/05.md)
  - [NUT-06: Mint information](https://github.com/cashubtc/nuts/blob/main/06.md)
  - [NUT-07: Token state check](https://github.com/cashubtc/nuts/blob/main/07.md)
  - [NUT-08: Lightning fee return](https://github.com/cashubtc/nuts/blob/main/08.md)
  - [NUT-09: Restore signatures](https://github.com/cashubtc/nuts/blob/main/09.md)
  - [NUT-10: Spending conditions](https://github.com/cashubtc/nuts/blob/main/10.md)
  - [NUT-11: Pay to Public Key (P2PK)](https://github.com/cashubtc/nuts/blob/main/11.md)
  - [NUT-12: Offline ecash signature validation](https://github.com/cashubtc/nuts/blob/main/12.md)
  - [NUT-13: Deterministic Secrets](https://github.com/cashubtc/nuts/blob/main/13.md)
  - [NUT-14: Hashed Timelock Contracts (HTLCs)](https://github.com/cashubtc/nuts/blob/main/14.md)
  - [NUT-15: Partial multi-path payments](https://github.com/cashubtc/nuts/blob/main/15.md)
  - [NUT-16: Animated QR codes](https://github.com/cashubtc/nuts/blob/main/16.md)
  - [NUT-17: WebSockets](https://github.com/cashubtc/nuts/blob/main/17.md)
  - [NUT-18: Payment Requests](https://github.com/cashubtc/nuts/blob/main/18.md)
  - [NUT-19: Cached Responses](https://github.com/cashubtc/nuts/blob/main/19.md)
  - [NUT-20: Signature on Mint Quote](https://github.com/cashubtc/nuts/blob/main/20.md)
  - [NUT-21: Clear Authentication](https://github.com/cashubtc/nuts/blob/main/21.md)
  - [NUT-22: Blind Authentication](https://github.com/cashubtc/nuts/blob/main/22.md)
  - [NUT-23: BOLT11](https://github.com/cashubtc/nuts/blob/main/23.md)
  - [NUT-24: HTTP 402 Payment Required](https://github.com/cashubtc/nuts/blob/main/24.md)
- Lightning invoice handling should conform to [BOLT 11](https://github.com/lightning/bolts/blob/master/11-payment-encoding.md).
