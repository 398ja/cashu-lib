# Repo Guidelines

## Testing

- Always run `mvn -q verify` from the repository root before committing your changes.
- Capture the command's output and include it in the PR description.
- If tests fail because dependencies cannot be installed or the network is unreachable, call this out in the PR.

## Pull Requests

- Summarize the changes you made.
- Describe how you tested them, referencing any limitations.
- Add a "Network access" section summarizing blocked domains if any network requests were denied.
