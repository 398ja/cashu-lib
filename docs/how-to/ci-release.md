# CI and Release Process

The main CI workflow ([`ci.yml`](../../.github/workflows/ci.yml)) runs on pushes and pull requests targeting `main` or `develop`. It:

- installs Temurin 21 with Maven caching
- executes `./mvnw -q verify |& tee build.log`
- uploads Surefire reports and JaCoCo exec files as artifacts
- publishes coverage to Codecov from `**/target/site/jacoco/jacoco.xml`
- writes aggregated HTML coverage to `target/site/jacoco-aggregate/index.html`
- opens an issue with the tail of the failing log when CI fails on `develop`

```bash
./mvnw -q verify
```

Use `./mvnw -q -pl <module> -am verify` if you need to run CI steps for a single module.

## Release process

Releases are automated via [`release-please.yml`](../../.github/workflows/release-please.yml), which creates tags and changelog PRs after successful CI on `main` or on manual dispatch. Published GitHub releases trigger the [`publish`](../../.github/workflows/publish.yml) workflow that deploys artifacts with `./mvnw -q deploy` to the `reposilite-releases` repository at `https://maven.398ja.xyz/releases`.

Publishing requires these secrets (configured in GitHub Actions or your local `~/.m2/settings.xml`):

- `MAVEN_USERNAME` / `MAVEN_PASSWORD`
- `GPG_PRIVATE_KEY` / `GPG_PASSPHRASE`
- `gpg.keyname` set via `-Dgpg.keyname=...` when deploying locally

To publish manually outside of CI, import your GPG key, configure the credentials above, and run:

```bash
./mvnw -q deploy -Dgpg.keyname=<YOUR_GPG_KEY_ID>
```
