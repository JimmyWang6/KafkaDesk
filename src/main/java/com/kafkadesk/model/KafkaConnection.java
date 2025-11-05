package com.kafkadesk.model;

import java.util.Objects;

/**
 * Represents a Kafka cluster connection configuration
 */
public class KafkaConnection {
    private String id;
    private String name;
    private String bootstrapServers;
    private String description;
    private boolean useSasl;
    private String saslMechanism;
    private String saslUsername;
    private String saslPassword;

    public KafkaConnection() {
    }

    public KafkaConnection(String id, String name, String bootstrapServers) {
        this.id = id;
        this.name = name;
        this.bootstrapServers = bootstrapServers;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isUseSasl() {
        return useSasl;
    }

    public void setUseSasl(boolean useSasl) {
        this.useSasl = useSasl;
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public String getSaslUsername() {
        return saslUsername;
    }

    public void setSaslUsername(String saslUsername) {
        this.saslUsername = saslUsername;
    }

    public String getSaslPassword() {
        return saslPassword;
    }

    public void setSaslPassword(String saslPassword) {
        this.saslPassword = saslPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConnection that = (KafkaConnection) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + bootstrapServers + ")";
    }
}
