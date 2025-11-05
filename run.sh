#!/bin/bash
# KafkaDesk launcher script

echo "Starting KafkaDesk..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 11 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "Error: Java 11 or higher is required"
    echo "Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

# Build if necessary
if [ ! -f "target/kafkadesk-1.0.0.jar" ]; then
    echo "Building KafkaDesk..."
    mvn clean package
    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi
fi

# Run the application
java -jar target/kafkadesk-1.0.0.jar

exit 0
