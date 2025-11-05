# Contributing to KafkaDesk

Thank you for your interest in contributing to KafkaDesk! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/KafkaDesk.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Test your changes thoroughly
6. Commit with a clear message: `git commit -m "Add feature: description"`
7. Push to your fork: `git push origin feature/your-feature-name`
8. Create a Pull Request

## Development Setup

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- An IDE (IntelliJ IDEA, Eclipse, or VS Code recommended)

### Building the Project

```bash
mvn clean compile
```

### Running Tests

```bash
mvn test
```

### Running the Application

```bash
mvn javafx:run
```

Or using the helper scripts:
```bash
./run.sh       # Linux/Mac
run.bat        # Windows
```

## Code Style

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public methods
- Keep methods focused and concise
- Write unit tests for new functionality

## Project Structure

```
src/main/java/com/kafkadesk/
├── model/              # Data models
├── service/            # Business logic
├── controller/         # UI controllers
└── util/               # Utility classes

src/main/resources/
├── fxml/               # JavaFX layouts
└── logback.xml         # Logging configuration

src/test/java/          # Unit tests
```

## Adding New Features

When adding a new feature:

1. **Model Layer**: Create or update model classes if needed
2. **Service Layer**: Implement business logic in service classes
3. **Controller Layer**: Update UI controllers to use the new service
4. **UI Layer**: Update FXML files if UI changes are needed
5. **Tests**: Add unit tests for new functionality
6. **Documentation**: Update README.md if user-facing features are added

## Testing Guidelines

- Write unit tests for all new service methods
- Test edge cases and error conditions
- Use meaningful test names that describe what is being tested
- Mock external dependencies (Kafka connections) when appropriate

## Commit Message Guidelines

Use clear and descriptive commit messages:

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests after the first line

Example:
```
Add consumer group lag monitoring

- Implement lag calculation per partition
- Add UI display for total lag
- Update monitoring service with lag tracking

Fixes #123
```

## Pull Request Process

1. Ensure all tests pass
2. Update documentation if needed
3. Add a clear description of the changes
4. Reference any related issues
5. Wait for code review and address feedback

## Code Review

All submissions require review. We use GitHub pull requests for this purpose.

Reviewers will check for:
- Code quality and style
- Test coverage
- Documentation updates
- Security considerations
- Performance implications

## Reporting Bugs

When reporting bugs, please include:

- KafkaDesk version
- Java version
- Operating system
- Steps to reproduce
- Expected behavior
- Actual behavior
- Relevant logs or screenshots

## Feature Requests

We welcome feature requests! Please:

- Check if the feature has already been requested
- Provide a clear description of the feature
- Explain the use case and benefits
- Consider contributing the implementation

## Questions?

Feel free to open an issue for questions or join our discussions.

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see LICENSE file).
