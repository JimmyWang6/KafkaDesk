package com.kafkadesk.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TopicInfoTest {

    @Test
    void testTopicCreation() {
        TopicInfo topic = new TopicInfo("test-topic", 3, (short) 2);
        
        assertEquals("test-topic", topic.getName());
        assertEquals(3, topic.getPartitions());
        assertEquals(2, topic.getReplicationFactor());
    }

    @Test
    void testTopicEquality() {
        TopicInfo topic1 = new TopicInfo("test-topic", 3, (short) 2);
        TopicInfo topic2 = new TopicInfo("test-topic", 5, (short) 1);
        TopicInfo topic3 = new TopicInfo("other-topic", 3, (short) 2);
        
        assertEquals(topic1, topic2); // Same name
        assertNotEquals(topic1, topic3); // Different name
    }

    @Test
    void testToString() {
        TopicInfo topic = new TopicInfo("test-topic", 3, (short) 2);
        assertEquals("test-topic", topic.toString());
    }
}
