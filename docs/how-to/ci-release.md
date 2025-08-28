# CI and Release Process

The project uses a GitHub Actions workflow defined in [`ci.yml`](../../.github/workflows/ci.yml) that runs after the “Format” workflow completes. It sets up JDK 21, caches Maven dependencies, executes `mvn -B verify` to build all modules and run tests, aggregates coverage via JaCoCo, and uploads the results to Codecov. The resulting HTML report is stored as a workflow artifact at `cashu-lib-test/target/site/jacoco-aggregate/index.html`.

To reproduce the CI build locally and generate the same coverage report, run:

```bash
mvn -q verify
```

## Release process

Automated releases are orchestrated by [`release-please.yml`](../../.github/workflows/release-please.yml), which manages versioning and publishes artifacts to Sonatype OSSRH and Maven Central.

Manual releases are only necessary when maintainers need to deploy outside this workflow. Configure the following credentials and signing keys in your environment:

- `OSSRH_USERNAME` and `OSSRH_PASSWORD`
- `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE`

After configuring the secrets (for example via GitHub secrets or your local `~/.m2/settings.xml`), publish a manual release with:

```bash
mvn -q deploy -Dgpg.keyname=<YOUR_GPG_KEY_ID>
```

