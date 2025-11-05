# KafkaDesk Design Document v1.0

## 1. Project Overview

### 1.1 Project Introduction
KafkaDesk is a Kafka desktop client developed based on Java and JavaFX, designed to provide developers and operations personnel with a visualized Kafka cluster management and operation interface. Through an intuitive graphical interface, users can easily manage Kafka clusters, topics, consumer groups and other resources, and perform message production and consumption operations.

### 1.2 Core Objectives
- Provide a friendly graphical interface to lower the barrier to using Kafka
- Support multi-cluster management for easy switching between development and production environments
- Implement visualized message production and consumption
- Provide cluster monitoring and performance analysis functions
- Cross-platform support (Windows, macOS, Linux)

### 1.3 Target Users
- Kafka developers
- System operations personnel
- Data engineers
- Testers

## 2. Technical Architecture

### 2.1 Technology Stack
- **Programming Language**: Java 17+
- **UI Framework**: JavaFX 17+
- **Build Tool**: Maven
- **Kafka Client**: Apache Kafka Clients 3.6+
- **JSON Processing**: Jackson
- **Logging Framework**: SLF4J + Logback
- **Unit Testing**: JUnit 5

### 2.2 Architecture Design

#### 2.2.1 Overall Architecture
```
┌─────────────────────────────────────────────────┐
│              Presentation Layer                  │
│  (JavaFX Views, Controllers, FXML)              │
├─────────────────────────────────────────────────┤
│              Business Logic Layer                │
│  (Service Layer, Business Models)               │
├─────────────────────────────────────────────────┤
│              Data Access Layer                   │
│  (Kafka Client Wrapper, Configuration Manager)  │
├─────────────────────────────────────────────────┤
│              External Dependencies               │
│  (Kafka Cluster, Local Storage)                 │
└─────────────────────────────────────────────────┘
```

#### 2.2.2 Module Division
1. **UI Module** (kafkadesk-ui)
   - Main window and layout management
   - Cluster management interface
   - Topic management interface
   - Consumer group management interface
   - Message producer interface
   - Message query interface
   - Settings interface

2. **Core Service Module** (kafkadesk-core)
   - Cluster connection service
   - Topic management service
   - Consumer group management service
   - Message producer service
   - Message consumer service
   - Configuration management service

3. **Data Model Module** (kafkadesk-model)
   - Cluster configuration model
   - Topic information model
   - Consumer group model
   - Message model

4. **Utility Module** (kafkadesk-utils)
   - Serialization/deserialization utilities
   - Date and time utilities
   - String processing utilities

## 3. Core Function Design

### 3.1 Cluster Management

#### 3.1.1 Function Description
- Add/edit/delete cluster connection configurations
- Test cluster connection
- View cluster information (version, nodes, controller, etc.)
- Support multi-cluster management and quick switching

#### 3.1.2 Data Model
```java
public class ClusterConfig {
    private String id;
    private String name;
    private String bootstrapServers;
    private String saslMechanism;
    private String securityProtocol;
    private Map<String, String> properties;
    private boolean autoConnect;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 3.2 Topic Management

#### 3.2.1 Function Description
- View topic list
- Create new topics (configure partitions, replicas, etc.)
- Delete topics
- View topic detailed information (partitions, replicas, configuration)
- Modify topic configuration
- Increase partition count

#### 3.2.2 Data Model
```java
public class TopicInfo {
    private String name;
    private int partitions;
    private int replicationFactor;
    private Map<String, String> config;
    private List<PartitionInfo> partitionDetails;
}
```

### 3.3 Message Production

#### 3.3.1 Function Description
- Select target topic and partition
- Support manual message content input
- Support batch message sending
- Support multiple message formats (JSON, text, Avro)
- Support custom Key and Headers
- Send result feedback

### 3.4 Message Query

#### 3.4.1 Function Description
- Select topic and partition
- Support querying from specific offset range
- Message filtering and searching
- Export messages to file
- Support multiple formatting displays

#### 3.4.2 Data Model
```java
public class Message {
    private String topic;
    private Integer partition;
    private Long offset;
    private String key;
    private String value;
    private Map<String, String> headers;
    private Long timestamp;
}
```

### 3.5 Consumer Group Management

#### 3.5.1 Function Description
- View all consumer groups
- View consumer group details (members, subscribed topics)
- View consumer group consumption progress (lag)
- Reset consumer group offset
- Delete consumer group

#### 3.5.2 Data Model
```java
public class ConsumerGroupInfo {
    private String groupId;
    private String state;
    private String protocolType;
    private List<MemberInfo> members;
    private Map<TopicPartition, OffsetAndMetadata> offsets;
    private Map<TopicPartition, Long> lag;
}
```

## 4. User Interface Design

### 4.1 Main Window Layout
```
┌────────────────────────────────────────────────────┐
│  Menu Bar: File | Edit | View | Tools | Help       │
├────────────────────────────────────────────────────┤
│  Toolbar: [Connect] [Refresh] [Settings] [About]   │
├──────────┬─────────────────────────────────────────┤
│          │                                         │
│  Cluster │         Main Work Area                  │
│  Tree    │   (Tab switching for different modules) │
│          │                                         │
│  - Cluster1│   ┌─────────────────────────────────┐│
│    - Topics│   │ Topic Mgmt | Producer | Query  ││
│    - Groups│   └─────────────────────────────────┘│
│  - Cluster2│                                       │
│          │                                         │
├──────────┴─────────────────────────────────────────┤
│  Status Bar: Connection Status | Current Cluster   │
└────────────────────────────────────────────────────┘
```

### 4.2 UI Design Principles
- **Simplicity**: Clean and clear interface, avoiding information overload
- **Consistency**: Maintain consistent operations and visual style
- **Responsiveness**: Provide timely operation feedback
- **Usability**: Conform to user habits, reducing learning costs
- **Aesthetics**: Adopt modern design style

### 4.3 Themes and Styles
- Support light and dark theme switching
- Use JavaFX CSS for style customization
- Adopt unified color scheme and icon library

## 5. Data Storage Design

### 5.1 Configuration Storage
- Use JSON format to store configuration files
- Configuration file location: `~/.kafkadesk/config.json`
- Storage content:
  - Cluster connection configurations
  - User preference settings
  - Window position and size
  - Recently used clusters

### 5.2 Configuration File Structure
```json
{
  "version": "1.0",
  "clusters": [
    {
      "id": "cluster-1",
      "name": "Development Environment",
      "bootstrapServers": "localhost:9092",
      "properties": {}
    }
  ],
  "preferences": {
    "theme": "light",
    "language": "zh_CN",
    "autoConnect": true,
    "lastSelectedCluster": "cluster-1"
  },
  "window": {
    "width": 1200,
    "height": 800,
    "maximized": false
  }
}
```

## 6. Security Design

### 6.1 Authentication Support
- Support SASL/PLAIN
- Support SASL/SCRAM
- Support SSL/TLS encrypted connections

### 6.2 Sensitive Information Handling
- Encrypted password storage
- Support for local password encryption
- Prompt users not to use plain text passwords in production environment

## 7. Performance Optimization

### 7.1 UI Responsiveness
- All Kafka operations executed in background threads
- Use JavaFX Task and Service for asynchronous processing
- Avoid UI thread blocking

### 7.2 Data Loading Optimization
- Topic and message lists support pagination
- Use virtual scrolling for large data display
- Message consumption supports streaming processing

### 7.3 Memory Management
- Limit message cache quantity
- Promptly release unused resources
- Monitor memory usage

## 8. Error Handling

### 8.1 Exception Handling Strategy
- All Kafka operations wrapped in try-catch blocks
- Friendly error message prompts
- Record detailed error logs

### 8.2 Common Error Handling
- Connection failure: Prompt user to check network and configuration
- Insufficient permissions: Prompt user to check authentication information
- Timeout: Provide retry options
- Resource does not exist: Friendly prompt and refresh list

## 9. Development Plan

### 9.1 Phase 1 (MVP - Minimum Viable Product)
**Goal**: Implement basic cluster connection and topic management functions

**Tasks**:
1. Set up project basic architecture (Maven configuration, module division)
2. Implement main window framework and basic layout
3. Implement cluster connection management
4. Implement topic list viewing
5. Implement simple message production function
6. Implement simple message query function

**Estimated Time**: 2-3 weeks

### 9.2 Phase 2 (Feature Enhancement)
**Goal**: Improve core functions and enhance user experience

**Tasks**:
1. Improve topic management (create, delete, configuration modification)
2. Enhance message production (support multiple formats, batch sending)
3. Enhance message query (filtering, searching, exporting)
4. Implement consumer group management
5. Add configuration persistence
6. Optimize UI interaction and styles

**Estimated Time**: 3-4 weeks

### 9.3 Phase 3 (Advanced Features)
**Goal**: Add advanced features and monitoring functions

**Tasks**:
1. Implement cluster monitoring functions
2. Add performance statistics and chart display
3. Support multiple languages (Chinese, English)
4. Implement light and dark theme switching
5. Add shortcut key support
6. Improve documentation and help system

**Estimated Time**: 2-3 weeks

### 9.4 Phase 4 (Testing and Release)
**Goal**: Comprehensive testing and official release

**Tasks**:
1. Unit testing and integration testing
2. Cross-platform testing (Windows, macOS, Linux)
3. Performance testing and optimization
4. User documentation writing
5. Packaging and release

**Estimated Time**: 1-2 weeks

## 10. Project Structure

```
KafkaDesk/
├── pom.xml                          # Maven main configuration file
├── README.md                        # Project description
├── DESIGN.md                        # Design document
├── LICENSE                          # License
├── .gitignore                       # Git ignore configuration
│
├── kafkadesk-model/                 # Data model module
│   ├── pom.xml
│   └── src/
│       └── main/java/com/kafkadesk/model/
│           ├── ClusterConfig.java
│           ├── TopicInfo.java
│           ├── ConsumerGroupInfo.java
│           └── Message.java
│
├── kafkadesk-core/                  # Core service module
│   ├── pom.xml
│   └── src/
│       └── main/java/com/kafkadesk/core/
│           ├── service/
│           │   ├── ClusterService.java
│           │   ├── TopicService.java
│           │   ├── ProducerService.java
│           │   ├── ConsumerService.java
│           │   └── ConsumerGroupService.java
│           └── config/
│               └── ConfigManager.java
│
├── kafkadesk-ui/                    # UI module
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/kafkadesk/ui/
│       │   ├── KafkaDeskApplication.java
│       │   ├── controller/
│       │   │   └── MainController.java
│       │   └── util/
│       │       ├── FXMLUtil.java
│       │       └── I18nUtil.java
│       └── resources/
│           ├── fxml/
│           │   └── main.fxml
│           ├── css/
│           │   ├── light-theme.css
│           │   └── dark-theme.css
│           ├── i18n/
│           │   ├── messages_en.properties
│           │   └── messages_zh_CN.properties
│           └── images/
│               └── icons/
│
└── kafkadesk-utils/                 # Utility module
    ├── pom.xml
    └── src/
        └── main/java/com/kafkadesk/utils/
            ├── JsonUtil.java
            ├── DateTimeUtil.java
            └── StringUtil.java
```

## 11. Dependency Management

### 11.1 Main Dependencies
```xml
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>17.0.8</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>17.0.8</version>
    </dependency>
    
    <!-- Kafka Client -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>3.6.0</version>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.3</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>2.0.9</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.11</version>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## 12. Packaging and Release

### 12.1 Packaging Method
- Use Maven for building
- Use jpackage to create native installation packages
- Support creating executable JAR files

### 12.2 Release Channels
- GitHub Releases
- Official website download
- Possible package managers (Homebrew, Chocolatey)

## 13. Future Extension Plans

### 13.1 Feature Extensions
- Schema Registry integration
- Kafka Connect management
- Kafka Streams monitoring
- ACL permission management
- Cluster performance metrics visualization
- Message tracing

### 13.2 Technical Optimizations
- Support plugin architecture
- Provide REST API interface
- Increase automated test coverage
- Continuous integration and continuous deployment (CI/CD)

## 14. Summary

KafkaDesk aims to become a powerful and easy-to-use Kafka desktop client tool. Through Java and JavaFX technology stack, we will provide a cross-platform graphical interface to help users manage and operate Kafka clusters more efficiently.

This design document is the first version of the project design and will be continuously improved and optimized based on development progress and user feedback.

---

**Document Version**: 1.0  
**Creation Date**: 2025-11-05  
**Author**: KafkaDesk Team  
**Status**: Active Development
