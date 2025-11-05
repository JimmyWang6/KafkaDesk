# Changelog

All notable changes to KafkaDesk will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-05

### Added
- Initial release of KafkaDesk
- Connection management for multiple Kafka clusters
  - Add, edit, and delete connections
  - Test connections before saving
  - Persistent storage of connections in `~/.kafkadesk/connections.json`
  - Support for SASL authentication (PLAIN mechanism)
- Topic management features
  - List all topics with partition and replication factor information
  - Create new topics with custom partitions and replication factor
  - Delete topics
  - View topic configurations
  - Update topic configurations
- Message browsing and sending
  - Browse recent messages from topics (configurable limit)
  - Browse messages from specific partitions with offset control
  - Search messages by key or value pattern
  - Send test messages with optional keys
  - Send messages with custom headers
  - View message metadata (partition, offset, timestamp, headers)
- Consumer group monitoring
  - List all consumer groups in cluster
  - View consumer group state and member count
  - Track total consumer lag per group
  - View per-partition consumer lag
  - View consumer group offsets
  - Delete consumer groups
  - Reset consumer group offsets
- Performance monitoring
  - View partition-level metrics (leader, replicas, ISR count)
  - Monitor message counts per partition
  - Track cluster node information
  - View cluster ID and controller node
- JavaFX-based graphical user interface
  - Tabbed interface for different management functions
  - Connection list with connect/disconnect actions
  - Table views for topics, messages, and consumer groups
  - Dialog-based forms for creating/editing resources
  - Status bar for operation feedback
- Comprehensive documentation
  - README with feature overview and usage instructions
  - Quick Start guide for new users
  - Contributing guidelines for developers
  - Helper scripts for running the application (Linux/Mac/Windows)
- Unit tests for core models and utilities
- Maven build configuration with shade plugin for uber JAR
- Logging configuration with Logback

### Technical Details
- Built with Java 11
- JavaFX 17.0.2 for UI
- Apache Kafka Clients 3.4.0
- Maven project structure
- Layered architecture (Model, Service, Controller, Util)

[1.0.0]: https://github.com/JimmyWang6/KafkaDesk/releases/tag/v1.0.0
