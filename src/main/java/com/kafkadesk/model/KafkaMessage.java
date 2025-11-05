package com.kafkadesk.model;

import java.time.Instant;

/**
 * Represents a Kafka message
 */
public class KafkaMessage {
    private String topic;
    private int partition;
    private long offset;
    private Instant timestamp;
    private String key;
    private String value;
    private String headers;

    public KafkaMessage() {
    }

    public KafkaMessage(String topic, int partition, long offset, String key, String value) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.key = key;
        this.value = value;
    }

    // Getters and Setters
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "Message[partition=" + partition + ", offset=" + offset + "]";
    }
}
