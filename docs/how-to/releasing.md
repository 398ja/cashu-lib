# Releasing

Artifacts are deployed to Sonatype OSSRH and synchronized to Maven Central.

## Prerequisites

Releasing requires credentials and signing keys configured in the environment:

- `OSSRH_USERNAME` and `OSSRH_PASSWORD`
- `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE`

Contact the project maintainers to obtain these secrets. Once configured (for example via GitHub secrets or your local `~/.m2/settings.xml`), publish a release with:

```bash
mvn -q deploy -Dgpg.keyname=<YOUR_GPG_KEY_ID>
```
