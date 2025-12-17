# Releasing

Artifacts are deployed to the `reposilite-releases` repository at `https://maven.398ja.xyz/releases`.

## Automation

- [`release-please`](../../.github/workflows/release-please.yml) manages version bumps and changelog PRs on `main`.
- GitHub releases trigger [`publish.yml`](../../.github/workflows/publish.yml), which imports the signing key and runs `./mvnw -q deploy`.

## Prerequisites

Releasing requires credentials and signing keys configured in the environment:

- `MAVEN_USERNAME` and `MAVEN_PASSWORD` for `reposilite-releases`
- `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE`
- `gpg.keyname` passed via `-Dgpg.keyname=<YOUR_GPG_KEY_ID>`

Contact the project maintainers to obtain these secrets. Once configured (for example via GitHub secrets or your local `~/.m2/settings.xml`), publish a release with:

```bash
./mvnw -q deploy -Dgpg.keyname=<YOUR_GPG_KEY_ID>
```
