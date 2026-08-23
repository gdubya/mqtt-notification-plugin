# Project Rules

## Git Commit Conventions
- Use **Conventional Commits** syntax (`<type>(<scope>): <description>`) for all git commits.
- Allowed types include:
  - `feat`: New feature or user-facing change
  - `fix`: Bug fix
  - `docs`: Documentation changes
  - `style`: Formatting, missing semicolons, etc. (no code change)
  - `refactor`: Refactoring production code
  - `test`: Adding or refactoring tests
  - `chore`: Updating build tasks, dependencies, package configs, etc.
  - `ci`: CI/CD configuration changes (e.g., GitHub Actions, Jenkinsfile)

## Development Environment
- Use **WSL** (`Ubuntu-22.04` with zsh / SDKMAN) for development tooling (e.g., `mvn`, `java`).
- Execute commands via WSL targeting the project directory:
  `wsl -d Ubuntu-22.04 --cd /mnt/c/Users/garet/Projects/mqtt-notification-plugin -e zsh -ic 'source ~/.zshrc && <command>'`

## Branching & Pull Requests
- Never commit or push directly to `main` or `master`.
- Always make code and documentation changes on a dedicated branch and submit a Pull Request.

