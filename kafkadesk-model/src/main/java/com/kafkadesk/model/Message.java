package com.kafkadesk.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Message model
 */
public class Message {
    private String topic;
    private Integer partition;
    private Long offset;
    private String key;
    private String value;
    private Map<String, String> headers;
    private Long timestamp;
    private MessageFormat format;

    public Message() {
        this.headers = new HashMap<>();
        this.format = MessageFormat.TEXT;
    }

    public Message(String topic, String value) {
        this();
        this.topic = topic;
        this.value = value;
    }

    // Getters and Setters
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public MessageFormat getFormat() {
        return format;
    }

    public void setFormat(MessageFormat format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "Message{" +
                "topic='" + topic + '\'' +
                ", partition=" + partition +
                ", offset=" + offset +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Message format enumeration
     */
    public enum MessageFormat {
        JSON("JSON"),
        TEXT("文本"),
        AVRO("Avro"),
        XML("XML");

        private final String displayName;

        MessageFormat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
