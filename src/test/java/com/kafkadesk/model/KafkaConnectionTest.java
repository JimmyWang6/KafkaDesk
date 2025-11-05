package com.kafkadesk.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KafkaConnectionTest {

    @Test
    void testConnectionCreation() {
        KafkaConnection connection = new KafkaConnection("1", "Test Connection", "localhost:9092");
        
        assertEquals("1", connection.getId());
        assertEquals("Test Connection", connection.getName());
        assertEquals("localhost:9092", connection.getBootstrapServers());
    }

    @Test
    void testConnectionEquality() {
        KafkaConnection conn1 = new KafkaConnection("1", "Test1", "localhost:9092");
        KafkaConnection conn2 = new KafkaConnection("1", "Test2", "localhost:9093");
        KafkaConnection conn3 = new KafkaConnection("2", "Test3", "localhost:9092");
        
        assertEquals(conn1, conn2); // Same ID
        assertNotEquals(conn1, conn3); // Different ID
    }

    @Test
    void testToString() {
        KafkaConnection connection = new KafkaConnection("1", "Test", "localhost:9092");
        String result = connection.toString();
        
        assertTrue(result.contains("Test"));
        assertTrue(result.contains("localhost:9092"));
    }
}
