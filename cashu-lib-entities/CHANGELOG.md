# Changelog

All notable changes to cashu-lib-entities will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.15.0] - 2026-01-28

### Changed

- **Package Reorganization**: REST entities reorganized into NUT-specific packages.
  - `rest/nut03/`: PostSwapRequest, PostSwapResponse
  - `rest/nut04/`: PostMintQuoteRequest, PostMintQuoteResponse, PostMintQuoteBolt11Request, PostMintQuoteFiatRequest, PostMintRequest, PostMintResponse
  - `rest/nut05/`: PostMeltQuoteRequest, PostMeltQuoteResponse, PostMeltQuoteBolt11Request, PostMeltQuoteBolt11Response, PostMeltRequest, PostMeltResponse, PostMeltBolt11Request, PostMeltBolt11Response, PostMeltQuoteMockRequest, PostMeltQuoteMockResponse, PostMeltQuoteTestRequest, PostMeltQuoteTestResponse
  - `rest/nut07/`: PostCheckStateRequest, PostCheckStateResponse
  - `rest/nut09/`: PostRestoreRequest, PostRestoreResponse
- Updated cashu-lib-common dependency to 0.15.0

**Migration Note**: Update import statements to use new package paths.

---

## [0.14.0] - 2026-01-28

### Changed

- Updated cashu-lib-common dependency to 0.14.0

---

## [0.12.0] - 2026-01-21

### Changed

- Updated cashu-lib-crypto dependency to 0.12.0
- Updated cashu-lib-common dependency to 0.12.0

---

## [0.11.0] - 2026-01-10

### Changed

- Updated cashu-lib-common dependency to 0.11.0

---

## [0.10.0] and earlier

See git history for earlier changes.
