# KafkaDesk Quick Start Guide

This guide will help you get started with KafkaDesk in minutes.

## Installation

### Prerequisites

1. **Java 11 or higher** - Check with:
   ```bash
   java -version
   ```

2. **Maven 3.6 or higher** (for building from source) - Check with:
   ```bash
   mvn -version
   ```

### Build from Source

```bash
# Clone the repository
git clone https://github.com/JimmyWang6/KafkaDesk.git
cd KafkaDesk

# Build the application
mvn clean package

# Run the application
./run.sh          # Linux/Mac
run.bat           # Windows
```

Or use Maven directly:
```bash
mvn javafx:run
```

## First Steps

### 1. Add a Kafka Connection

1. Click the **"Add"** button in the Connections panel
2. Fill in the connection details:
   - **Name**: A friendly name (e.g., "Local Kafka")
   - **Bootstrap Servers**: Your Kafka broker address (e.g., `localhost:9092`)
   - **Description**: Optional description
3. Click **"Save"**

### 2. Connect to Kafka

1. Select your connection from the list
2. Click **"Connect"**
3. Wait for the connection to establish
4. Topics and consumer groups will load automatically

### 3. View Topics

1. Navigate to the **"Topics"** tab
2. All topics in your cluster will be displayed
3. Click **"Refresh"** to reload the list

### 4. Browse Messages

1. Select a topic from the Topics tab
2. Navigate to the **"Messages"** tab
3. Click **"Browse Messages"** to view recent messages
4. Use the search box to find specific messages

### 5. Send a Test Message

1. Select a topic from the Topics tab
2. Navigate to the **"Messages"** tab
3. Click **"Send Message"**
4. Enter optional key and message value
5. Click **"Send"**

### 6. Monitor Consumer Groups

1. Navigate to the **"Consumer Groups"** tab
2. View all consumer groups and their status
3. Check consumer lag for each group
4. Click **"Refresh"** to update information

## Common Tasks

### Create a New Topic

1. Connect to a cluster
2. Go to the **"Topics"** tab
3. Click **"Create Topic"**
4. Enter:
   - Topic name
   - Number of partitions
   - Replication factor
5. Click **"Create"**

### Delete a Topic

1. Connect to a cluster
2. Go to the **"Topics"** tab
3. Select the topic to delete
4. Click **"Delete Topic"**
5. Confirm the deletion

### Search Messages

1. Select a topic
2. Go to the **"Messages"** tab
3. Enter search term in the search box
4. Click **"Search"**
5. Matching messages will be displayed

## Tips

- **Connection Persistence**: Your connections are automatically saved to `~/.kafkadesk/connections.json`
- **Multiple Clusters**: You can add and switch between multiple Kafka clusters
- **Keyboard Navigation**: Use arrow keys to navigate between items in lists and tables
- **Refresh Data**: Click refresh buttons to update data from the cluster

## Troubleshooting

### Cannot Connect to Kafka

- Verify Kafka is running: `kafka-topics.sh --bootstrap-server localhost:9092 --list`
- Check the bootstrap servers address is correct
- Ensure no firewall is blocking the connection
- Check if SASL authentication is required

### Application Won't Start

- Verify Java 11+ is installed
- Check JavaFX is available (should be included in the build)
- Look at logs in `logs/kafkadesk.log`

### No Topics Showing

- Click the "Refresh" button
- Verify you have permission to list topics
- Check the connection is still active

## Next Steps

- Explore the **Monitoring** tab for cluster metrics
- Configure topic settings
- Monitor consumer group lag
- Try different search patterns for messages

## Getting Help

- Check the [README](README.md) for detailed documentation
- Review [CONTRIBUTING](CONTRIBUTING.md) for development setup
- Open an issue on GitHub for bugs or feature requests

## Example: Complete Workflow

Here's a typical workflow for testing message flow:

1. **Connect** to your Kafka cluster
2. **Create** a test topic: `my-test-topic` (3 partitions, RF=1)
3. **Send** a few test messages to the topic
4. **Browse** messages to verify they were sent
5. **Search** for specific message content
6. **Monitor** the topic metrics in the Monitoring tab
7. Check **Consumer Groups** for any active consumers

Happy Kafka management! 🎉
