# KafkaDesk

[![Java CI with Maven](https://github.com/JimmyWang6/KafkaDesk/actions/workflows/maven.yml/badge.svg)](https://github.com/JimmyWang6/KafkaDesk/actions/workflows/maven.yml)

A cross-platform desktop client tool for Apache Kafka, offering visualized functions for Kafka cluster management and operations.

## Features

- **Cluster Management**: Support for multi-cluster configuration and management
- **Topic Management**: View, create, and delete Kafka topics
- **Message Producer**: Visual interface for sending messages to Kafka
- **Message Query**: Advanced query functionality with filters for topic, partition, and offset
- **Consumer Group Management**: Monitor consumer groups, members, and lag information
- **Internationalization**: Full support for English and Chinese languages
- **Cross-Platform**: Support for Windows, macOS, and Linux

## Tech Stack

- Java 17+
- JavaFX 17+
- Apache Kafka Clients 3.6.0
- Maven

## Build and Run

### Prerequisites

- JDK 17 or higher
- Maven 3.6 or higher

### Build Project

```bash
mvn clean package
```

### Run Application

```bash
cd kafkadesk-ui
mvn javafx:run
```

Or run the packaged JAR:

```bash
java -jar kafkadesk-ui/target/kafkadesk-ui-1.0.0-SNAPSHOT.jar
```

## Quick Start

1. Launch the application
2. Click "Add Cluster" button
3. Enter cluster name and Bootstrap Servers (e.g., localhost:9092)
4. Select a cluster from the tree on the left to connect
5. Use Topic Management, Message Producer, Message Query, and Consumer Groups tabs

## Project Structure

```
KafkaDesk/
├── kafkadesk-model/     # Data models
├── kafkadesk-utils/     # Utility classes
├── kafkadesk-core/      # Core services
└── kafkadesk-ui/        # JavaFX user interface
```

## Configuration

Configuration file location: `~/.kafkadesk/config.json`

## Documentation

- [Design Document](DESIGN.md) - Comprehensive design specification
- [Quick Start Guide](QUICKSTART.md) - Detailed usage guide
- [Changelog](CHANGELOG.md) - Version history and feature documentation

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

Apache License 2.0
