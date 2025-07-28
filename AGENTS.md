# Repo Guidelines

## Testing

- Always run `mvn -q verify` from the repository root before committing your changes.
- Include the command's output in the PR description.
- If tests fail due to dependency or network issues, mention this in the PR.
- Update the `README.md` file if you add or modify features.
- Update the `pom.xml` file for new modules or dependencies, ensuring compatibility with Java 21.
- Verify new Dockerfiles or `docker-compose.yml` files by running `docker-compose build`.
- Document new REST endpoints in the API documentation and ensure they are tested.
- Add unit tests for new functionality, covering edge cases.
- Ensure modifications to existing code do not break functionality and pass all tests.
- Add integration tests for new features to verify end-to-end functionality.
- Ensure new dependencies or configurations do not introduce security vulnerabilities.
- Always configure the versions in the pom.xml files. Versions are maintained in the configuration file `versions.properties` in the root directory.

## Pull Requests

- Summarize the changes made and describe how they were tested.
- Include any limitations or known issues in the description.
- Add a "Network Access" section summarizing blocked domains if network requests were denied.
- Ensure all new features, modules, or dependencies are properly documented in the `README.md` file.