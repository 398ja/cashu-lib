# Repo Guidelines

## NUTs

- https://github.com/cashubtc/nuts/blob/main/00.md
- https://github.com/cashubtc/nuts/blob/main/01.md
- https://github.com/cashubtc/nuts/blob/main/02.md
- https://github.com/cashubtc/nuts/blob/main/03.md
- https://github.com/cashubtc/nuts/blob/main/04.md
- https://github.com/cashubtc/nuts/blob/main/05.md
- https://github.com/cashubtc/nuts/blob/main/06.md
- https://github.com/cashubtc/nuts/blob/main/07.md
- https://github.com/cashubtc/nuts/blob/main/08.md
- https://github.com/cashubtc/nuts/blob/main/09.md
- https://github.com/cashubtc/nuts/blob/main/10.md
- https://github.com/cashubtc/nuts/blob/main/11.md
- https://github.com/cashubtc/nuts/blob/main/12.md
- https://github.com/cashubtc/nuts/blob/main/13.md
- https://github.com/cashubtc/nuts/blob/main/14.md
- https://github.com/cashubtc/nuts/blob/main/15.md
- https://github.com/cashubtc/nuts/blob/main/16.md
- https://github.com/cashubtc/nuts/blob/main/17.md
- https://github.com/cashubtc/nuts/blob/main/18.md
- https://github.com/cashubtc/nuts/blob/main/19.md
- https://github.com/cashubtc/nuts/blob/main/20.md
- https://github.com/cashubtc/nuts/blob/main/21.md
- https://github.com/cashubtc/nuts/blob/main/22.md
- https://github.com/cashubtc/nuts/blob/main/23.md
- https://github.com/cashubtc/nuts/blob/main/24.md

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
- Maintain the versions in the configuration section of the pom.xml files.

## Pull Requests

- Always follow the repository's PR submission guidelines and use the PR template.
- Summarize the changes made and describe how they were tested.
- Include any limitations or known issues in the description.
- Add a "Network Access" section summarizing blocked domains if network requests were denied.
- Ensure all new features, modules, or dependencies are properly documented in the `README.md` file.
