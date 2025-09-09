# Repo Guidelines

The Cashu protocol is defined in the NUT specifications maintained at [cashubtc/nuts](https://github.com/cashubtc/nuts):
- When implementing features, consult the NUT specifications:

- [NUT-00](https://github.com/cashubtc/nuts/blob/main/00.md)
- [NUT-01](https://github.com/cashubtc/nuts/blob/main/01.md)
- [NUT-02](https://github.com/cashubtc/nuts/blob/main/02.md)
- [NUT-03](https://github.com/cashubtc/nuts/blob/main/03.md)
- [NUT-04](https://github.com/cashubtc/nuts/blob/main/04.md)
- [NUT-05](https://github.com/cashubtc/nuts/blob/main/05.md)
- [NUT-06](https://github.com/cashubtc/nuts/blob/main/06.md)
- [NUT-07](https://github.com/cashubtc/nuts/blob/main/07.md)
- [NUT-08](https://github.com/cashubtc/nuts/blob/main/08.md)
- [NUT-09](https://github.com/cashubtc/nuts/blob/main/09.md)
- [NUT-10](https://github.com/cashubtc/nuts/blob/main/10.md)
- [NUT-11](https://github.com/cashubtc/nuts/blob/main/11.md)
- [NUT-12](https://github.com/cashubtc/nuts/blob/main/12.md)
- [NUT-13](https://github.com/cashubtc/nuts/blob/main/13.md)
- [NUT-14](https://github.com/cashubtc/nuts/blob/main/14.md)
- [NUT-15](https://github.com/cashubtc/nuts/blob/main/15.md)
- [NUT-16](https://github.com/cashubtc/nuts/blob/main/16.md)
- [NUT-17](https://github.com/cashubtc/nuts/blob/main/17.md)
- [NUT-18](https://github.com/cashubtc/nuts/blob/main/18.md)
- [NUT-19](https://github.com/cashubtc/nuts/blob/main/19.md)
- [NUT-20](https://github.com/cashubtc/nuts/blob/main/20.md)
- [NUT-21](https://github.com/cashubtc/nuts/blob/main/21.md)
- [NUT-22](https://github.com/cashubtc/nuts/blob/main/22.md)
- [NUT-23](https://github.com/cashubtc/nuts/blob/main/23.md)
- [NUT-24](https://github.com/cashubtc/nuts/blob/main/24.md)

## Project
- Maintain the versions in the configuration section of the parent pom.xml file.

## Coding
- When writing code, follow the "Clean Code" principles:
    - [Clean Code](https://dev.398ja.xyz/books/Clean_Architecture.pdf)
        - Relevant chapters: 2, 3, 4, 7, 10, 17
    - [Clean Architecture](https://dev.398ja.xyz/books/Clean_Code.pdf)
        - Relevant chapters: All chapters in part III and IV, 7-14.
    - [Design Patterns](https://github.com/iluwatar/java-design-patterns)
        - Follow design patterns as described in the book, whenever possible.
- When commiting code, follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification.
- When adding new features, ensure they are compliant with the Cashu specification (NUTs) provided above.

## Documentation

- When generating documentation:
    - Follow the Diátaxis framework and classify each document as a tutorial, how-to guide, reference, or explanation.
    - Place new Markdown files under `docs/<section>` matching the chosen category.
    - Start each document with a top-level `#` heading and a short introduction that states the purpose.
    - Link the document from `docs/README.md` in the corresponding section.
    - Use relative links to reference other documents and keep code snippets minimal and tested.
    - Consult the following resources on Diátaxis for guidance:
        - https://github.blog/developer-skills/documentation-done-right-a-developers-guide/
        - https://diataxis.fr/
        - https://diataxis.fr/start-here/
        - https://diataxis.fr/how-to-use-diataxis/
        - https://diataxis.fr/tutorials/
        - https://diataxis.fr/how-to-guides/
        - https://diataxis.fr/tutorials-how-to/
        - https://diataxis.fr/quality/
        - https://diataxis.fr/complex-hierarchies/
        - https://diataxis.fr/compass/

## Testing

- Always run `mvn -q verify` from the repository root before committing your changes.
- Include the command's output in the PR description.
- If tests fail due to dependency or network issues, mention this in the PR.
- Update the documentation files if you add or modify features.
- Update the `pom.xml` file for new modules or dependencies, ensuring compatibility with Java 21.
- Verify new Dockerfiles or `docker-compose.yml` files by running `docker-compose build`.
- Document new REST endpoints in the API documentation and ensure they are tested.
- Add unit tests for new functionality, covering edge cases. Follow "Clean Code" principles on unit tests, as described in the "Clean Code" book (Chapter 9).
- Ensure modifications to existing code do not break functionality and pass all tests.
- Add integration tests for new features to verify end-to-end functionality.
- Ensure new dependencies or configurations do not introduce security vulnerabilities.
- Add a comment on top of every test method to describe the test in plain English.

## Pull Requests

- Always follow the repository's PR submission guidelines and use the PR template located at `.github/pull_request_template.md`.
- Summarize the changes made and describe how they were tested.
- Include any limitations or known issues in the description.
- Ensure all new features are compliant with the Cashu specification (NUTs) provided above.
