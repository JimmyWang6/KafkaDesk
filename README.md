# KafkaDesk

A desktop client tool for Kafka, offering visualized functions for Kafka cluster management and operations.

## Features

### 🔌 Connection Management
- Easily connect and manage multiple Kafka clusters
- Save and load connection configurations
- Support for SASL authentication
- Test connections before saving

### 📊 Topic Management
- View all topics in a cluster
- Create new topics with custom partitions and replication factor
- Delete existing topics
- View topic configurations and metadata

### 💬 Message Browsing
- Real-time message viewing from topics
- Browse messages from specific partitions
- Search messages by key or value
- View message metadata (partition, offset, timestamp, headers)

### 📤 Message Sending
- Send test messages to any topic
- Support for message keys
- Optional message headers
- Immediate feedback on message delivery

### 👥 Consumer Group Monitoring
- View all consumer groups in a cluster
- Monitor consumer group state and member count
- Track consumer lag per group
- View detailed offset information per partition

### 📈 Performance Monitoring
- Real-time monitoring of topic and partition metrics
- View partition leader, replica count, and ISR status
- Monitor message counts per partition
- Track cluster node information

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Access to a Kafka cluster (for testing)

## Building the Application

```bash
# Clone the repository
git clone https://github.com/JimmyWang6/KafkaDesk.git
cd KafkaDesk

# Build with Maven
mvn clean package

# Run the application
mvn javafx:run
```

## Running the Application

After building, you can run the application using:

```bash
java -jar target/kafkadesk-1.0.0.jar
```

Or use Maven:

```bash
mvn javafx:run
```

## Usage

### Adding a Connection

1. Click the "Add" button in the Connections panel
2. Enter connection details:
   - **Name**: A friendly name for the connection
   - **Bootstrap Servers**: Kafka broker addresses (e.g., `localhost:9092`)
   - **Description**: Optional description
3. Click "Save"

### Connecting to a Cluster

1. Select a connection from the list
2. Click "Connect"
3. Wait for the connection to establish
4. Topics and consumer groups will be loaded automatically

### Managing Topics

1. Connect to a cluster
2. Navigate to the "Topics" tab
3. Click "Create Topic" to create a new topic
4. Select a topic and click "Delete Topic" to remove it
5. Click "Refresh" to reload the topic list

### Browsing Messages

1. Connect to a cluster
2. Navigate to the "Messages" tab
3. Select a topic from the Topics tab
4. Click "Browse Messages" to view recent messages
5. Use the search box to find specific messages

### Sending Messages

1. Connect to a cluster
2. Select a topic from the Topics tab
3. Navigate to the "Messages" tab
4. Click "Send Message"
5. Enter key (optional) and value
6. Click "Send"

### Monitoring Consumer Groups

1. Connect to a cluster
2. Navigate to the "Consumer Groups" tab
3. View all consumer groups, their state, and lag
4. Click "Refresh" to update the information

## Configuration

Connections are automatically saved in your home directory:
- Linux/Mac: `~/.kafkadesk/connections.json`
- Windows: `%USERPROFILE%\.kafkadesk\connections.json`

## Testing

Run the test suite:

```bash
mvn test
```

## Dependencies

- **JavaFX 17**: UI framework
- **Apache Kafka Clients 3.4.0**: Kafka integration
- **Gson**: JSON serialization
- **Logback**: Logging framework
- **JUnit 5**: Testing framework

## Architecture

The application follows a layered architecture:

- **Model Layer**: Data models (KafkaConnection, TopicInfo, KafkaMessage, etc.)
- **Service Layer**: Business logic for Kafka operations
  - KafkaConnectionService
  - TopicService
  - MessageService
  - ConsumerGroupService
  - MonitoringService
- **Controller Layer**: JavaFX UI controllers
- **Util Layer**: Utility classes for persistence and helper functions

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

## License

See the [LICENSE](LICENSE) file for details.

## Troubleshooting

### Connection Issues
- Verify that the Kafka broker addresses are correct
- Check if the Kafka cluster is running
- Ensure network connectivity to the Kafka brokers
- If using SASL, verify authentication credentials

### Performance
- For large topics, browse messages in smaller batches
- Use search functionality to find specific messages
- Monitor consumer lag to identify performance issues

## Support

For issues and questions, please open an issue on the GitHub repository.
