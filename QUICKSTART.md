# KafkaDesk Quick Start Guide

## Project Introduction

KafkaDesk is a Kafka desktop client built with Java 17 and JavaFX 17, providing a user-friendly graphical interface for managing and operating Kafka clusters.

## Project Structure

```
KafkaDesk/
├── pom.xml                    # Maven parent configuration
├── DESIGN.md                  # Detailed design document
├── README.md                  # Project description
├── QUICKSTART.md             # This file
│
├── kafkadesk-model/          # Data model module
│   ├── ClusterConfig.java    # Cluster configuration
│   ├── TopicInfo.java        # Topic information
│   ├── Message.java          # Message model
│   └── ConsumerGroupInfo.java # Consumer group information
│
├── kafkadesk-utils/          # Utility module
│   ├── JsonUtil.java         # JSON utilities
│   ├── DateTimeUtil.java     # Date/time utilities
│   └── StringUtil.java       # String utilities
│
├── kafkadesk-core/           # Core service module
│   ├── service/
│   │   ├── ClusterService.java      # Cluster service
│   │   ├── TopicService.java        # Topic service
│   │   ├── ProducerService.java     # Producer service
│   │   ├── ConsumerService.java     # Consumer service
│   │   └── ConsumerGroupService.java # Consumer group service
│   └── config/
│       └── ConfigManager.java       # Configuration manager
│
└── kafkadesk-ui/             # JavaFX UI module
    ├── KafkaDeskApplication.java    # Main application
    ├── controller/
    │   └── MainController.java      # Main controller
    ├── util/
    │   └── I18nUtil.java            # Internationalization utility
    └── resources/
        ├── fxml/main.fxml           # Main UI layout
        ├── css/light-theme.css      # Light theme styles
        └── i18n/                    # Language resources
            ├── messages_en.properties
            └── messages_zh_CN.properties
```

## System Requirements

- **JDK**: 17 or higher
- **Maven**: 3.6 or higher
- **Kafka**: 2.8+ (for testing connections)

## Quick Start

### 1. Clone the Project

```bash
git clone https://github.com/JimmyWang6/KafkaDesk.git
cd KafkaDesk
```

### 2. Compile the Project

```bash
mvn clean compile
```

### 3. Package the Project

```bash
mvn clean package -DskipTests
```

### 4. Run the Application

**Method 1: Using Maven Plugin**
```bash
cd kafkadesk-ui
mvn javafx:run
```

**Method 2: Running the JAR Directly**
```bash
java -jar kafkadesk-ui/target/kafkadesk-ui-1.0.0-SNAPSHOT.jar
```

## Usage Instructions

### Adding a Kafka Cluster

1. Click "File" menu -> "Add Cluster"
2. Enter cluster name (e.g., Local Development)
3. Enter Bootstrap Servers (e.g., localhost:9092)
4. Click OK to save

### Connecting to a Cluster

1. Select a cluster from the tree on the left side
2. The application will automatically attempt to connect
3. Upon successful connection, the status bar will display "Connected to cluster: xxx"

### Viewing Topic List

1. After connecting to a cluster, switch to the "Topic Management" tab
2. The topic list will load automatically
3. Click on a topic to view detailed information (partitions, replicas, configuration, etc.)

### Sending Messages

1. Switch to the "Message Producer" tab
2. Enter the topic name
3. (Optional) Enter message Key
4. Enter message content
5. Click "Send Message" button

### Querying Messages

1. Switch to the "Message Query" tab
2. Select the topic to query
3. (Optional) Select specific partition or "All Partitions"
4. Set offset range (from/to)
5. Set maximum number of records
6. Click "Search" button
7. Messages will be displayed in the table in real-time

### Managing Consumer Groups

1. Switch to the "Consumer Groups" tab
2. The list of consumer groups will load automatically
3. Click on a consumer group to view:
   - Group members (Member ID, Client ID, Host, Assignments)
   - Lag information (Topic, Partition, Current Offset, Lag value)

### Settings

1. Click "Tools" menu -> "Settings"
2. Select language (English or 中文)
3. Click OK to save
4. Restart the application for language changes to take effect

## Configuration Files

Application configuration file location:
```
~/.kafkadesk/config.json
```

Configuration includes:
- Cluster connection configurations
- User preference settings
- Window size and position
- Recently used clusters

## Log Files

Application log location:
```
~/.kafkadesk/logs/kafkadesk.log
```

## Feature Highlights

### Implemented Features ✅

- Multi-cluster management
- Cluster connection testing
- Topic list viewing
- Topic detailed information
- Message production (with Key/Value support)
- Message query (with filters)
- Consumer group management
- Lag monitoring
- Configuration persistence
- Multi-language support (English/Chinese)
- User-friendly UI interface

### Planned Features 🚧

- Topic creation and deletion
- Topic configuration modification
- Message filtering and searching
- Message export functionality
- Dark theme
- Performance monitoring
- ACL management

## Development Guide

### Project Dependencies

- **JavaFX 17.0.8**: UI framework
- **Kafka Clients 3.6.0**: Kafka client library
- **Jackson 2.15.3**: JSON processing
- **SLF4J + Logback**: Logging framework

### Building Individual Modules

```bash
# Build only the model module
mvn clean compile -pl kafkadesk-model

# Build only the core module with dependencies
mvn clean compile -pl kafkadesk-core -am
```

### Running Tests

```bash
mvn test
```

### Generating Project Documentation

```bash
mvn javadoc:javadoc
```

## Troubleshooting

### Issue: Cannot connect to Kafka cluster

**Solution**:
1. Ensure Kafka service is running
2. Verify Bootstrap Servers address is correct
3. Check network connection and firewall settings
4. Review log files for detailed error information

### Issue: JavaFX runtime errors

**Solution**:
1. Ensure JDK version is 17 or higher
2. Use `mvn javafx:run` instead of running JAR directly
3. Check if JavaFX runtime libraries are present

### Issue: Compilation errors

**Solution**:
1. Clean and rebuild: `mvn clean install`
2. Ensure Maven version >= 3.6
3. Delete cache in `~/.m2/repository` and re-download dependencies

## Contributing

Contributions are welcome! Please feel free to submit Issues and Pull Requests.

Development workflow:
1. Fork the project
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

Apache License 2.0

## Contact

- GitHub: https://github.com/JimmyWang6/KafkaDesk
- Issues: https://github.com/JimmyWang6/KafkaDesk/issues

---

**Enjoy using KafkaDesk!** 🎉
