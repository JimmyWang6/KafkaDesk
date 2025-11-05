package com.kafkadesk.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Cluster configuration model
 */
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

    public ClusterConfig() {
        this.id = UUID.randomUUID().toString();
        this.properties = new HashMap<>();
        this.autoConnect = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ClusterConfig(String name, String bootstrapServers) {
        this();
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
        this.updatedAt = LocalDateTime.now();
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
        this.updatedAt = LocalDateTime.now();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusterConfig that = (ClusterConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ClusterConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", bootstrapServers='" + bootstrapServers + '\'' +
                ", securityProtocol='" + securityProtocol + '\'' +
                '}';
    }
}
