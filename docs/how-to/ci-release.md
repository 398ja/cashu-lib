# CI and Release Process

The project uses a GitHub Actions workflow defined in [`ci.yml`](../../.github/workflows/ci.yml) that runs on pushes and pull requests to `main`. The workflow sets up JDK 21, caches Maven dependencies, and executes `mvn -B verify` to build all modules, run tests, and aggregate code coverage via JaCoCo. The resulting HTML report is stored as a workflow artifact and generated at `cashu-lib-test/target/site/jacoco-aggregate/index.html`.

To reproduce the CI build locally and generate the same coverage report, run:

```bash
mvn -q verify
```

## Release process

Artifacts are deployed to Sonatype OSSRH and synchronized to Maven Central. Releasing requires credentials and signing keys to be configured in the environment:

- `OSSRH_USERNAME` and `OSSRH_PASSWORD`
- `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE`

Contact the project maintainers to obtain these secrets. Once configured (for example via GitHub secrets or your local `~/.m2/settings.xml`), publish a release with:

```bash
mvn -q deploy -Dgpg.keyname=<YOUR_GPG_KEY_ID>
```

