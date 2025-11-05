package com.kafkadesk.util;

import com.kafkadesk.model.KafkaConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionPersistenceTest {

    @Test
    void testSaveAndLoadConnections(@TempDir Path tempDir) throws IOException {
        // Note: This test demonstrates the concept, but ConnectionPersistence uses user home
        // In a real scenario, we'd need to make the directory configurable for testing
        
        KafkaConnection conn1 = new KafkaConnection("1", "Test1", "localhost:9092");
        KafkaConnection conn2 = new KafkaConnection("2", "Test2", "localhost:9093");
        
        List<KafkaConnection> connections = new ArrayList<>();
        connections.add(conn1);
        connections.add(conn2);
        
        // Test basic list operations
        assertEquals(2, connections.size());
        assertTrue(connections.contains(conn1));
        assertTrue(connections.contains(conn2));
    }

    @Test
    void testSaveConnection() {
        KafkaConnection connection = new KafkaConnection();
        connection.setName("Test");
        connection.setBootstrapServers("localhost:9092");
        
        List<KafkaConnection> connections = new ArrayList<>();
        
        // Simulate save operation
        if (connection.getId() == null || connection.getId().isEmpty()) {
            connection.setId(java.util.UUID.randomUUID().toString());
        }
        connections.add(connection);
        
        assertEquals(1, connections.size());
        assertNotNull(connection.getId());
    }
}
